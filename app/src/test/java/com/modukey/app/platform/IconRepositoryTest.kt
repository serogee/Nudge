package com.modukey.app.platform

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.modukey.app.core.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File

@RunWith(RobolectricTestRunner::class)
class IconRepositoryTest {
    @Test fun `manual static resources are cached and missing packs fall back safely`()=runTest {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val repository=IconRepository(context)
        val button=PanelButton(icon=IconReference(context.packageName,"ic_modukey"))
        val first=repository.icon(button,"",48)
        assertNotNull(first); assertEquals(48,first!!.width)
        assertSame(first,repository.icon(button,"",48))
        assertSame(first,repository.cached(button,"",48))
        assertNull(repository.icon(PanelButton(icon=IconReference("missing.pack","missing")),"",48))
        assertTrue(repository.names("missing.pack").isEmpty())
        assertFalse(repository.available("missing.pack"))
    }
    @Test fun `cached artwork is available immediately across nearby sizes and button IDs`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = IconRepository(context)
        val button = PanelButton(icon = IconReference(context.packageName, "ic_modukey"))
        assertNull(repository.cached(button, "", 64))
        val large = repository.icon(button, "", 144)
        assertNotNull(large)
        assertSame(large, repository.cached(button.copy(id = "another-button"), "", 64))
        assertSame(large, repository.icon(button, "", 64))
        val bucket = repository.icon(button, "", 145)
        assertSame(bucket, repository.icon(button, "", 160))
        assertNull(repository.cached(button.copy(icon = IconReference(context.packageName, "missing")), "", 64))
    }
    @Test fun `disk artwork survives a repository restart and package updates replace it`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val button = PanelButton(icon = IconReference(context.packageName, "ic_modukey"))
        val first = IconRepository(context).icon(button, "", 48)!!
        val directory = File(context.cacheDir, "button-icons-v1")
        assertTrue(directory.listFiles()!!.any { it.extension == "png" })
        val restarted = IconRepository(context)
        val restored = restarted.icon(button, "", 48)!!
        assertNotSame(first, restored)
        assertTrue(first.sameAs(restored))
        @Suppress("DEPRECATION") val info = context.packageManager.getPackageInfo(context.packageName, 0)
        info.lastUpdateTime += 1_000
        shadowOf(context.packageManager).installPackage(info)
        val updated = restarted.icon(button, "", 48)!!
        assertNotSame(restored, updated)
        restarted.invalidate(context.packageName)
        assertNull(restarted.cached(button, "", 48))
    }
    @Test fun `preloading includes saved buttons and corrupt disk entries recover`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val button = PanelButton(icon = IconReference(context.packageName, "ic_modukey"))
        val repository = IconRepository(context)
        repository.preload(AppConfiguration(panels = listOf(SavedPanel(settings = PanelConfig(buttons = listOf(button))))))
        assertNotNull(repository.cached(button, "", 64))
        val directory = File(context.cacheDir, "button-icons-v1")
        directory.listFiles()!!.filter { it.extension == "png" }.forEach { it.writeText("interrupted cache write") }
        assertNotNull(IconRepository(context).icon(button, "", 192))
    }
    @Test fun `unlock requests are one shot and a later request cancels the previous one`() {
        val requests=UnlockRequests(); val value=PendingLaunch("handle",PanelButton(),false,false)
        val first=requests.put(value); assertTrue(requests.contains(first))
        val second=requests.put(value); assertFalse(requests.contains(first))
        assertEquals(value,requests.take(second)); assertNull(requests.take(second))
    }
}
