package com.modukey.app.platform

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.modukey.app.core.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IconRepositoryTest {
    @Test fun `manual static resources are cached and missing packs fall back safely`()=runTest {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val repository=IconRepository(context)
        val button=PanelButton(icon=IconReference(context.packageName,"ic_modukey"))
        val first=repository.icon(button,"",48)
        assertNotNull(first); assertEquals(48,first!!.width)
        assertSame(first,repository.icon(button,"",48))
        assertNull(repository.icon(PanelButton(icon=IconReference("missing.pack","missing")),"",48))
        assertTrue(repository.names("missing.pack").isEmpty())
        assertFalse(repository.available("missing.pack"))
    }
    @Test fun `unlock requests are one shot and a later request cancels the previous one`() {
        val requests=UnlockRequests(); val value=PendingLaunch("handle",PanelButton(),false,false)
        val first=requests.put(value); assertTrue(requests.contains(first))
        val second=requests.put(value); assertFalse(requests.contains(first))
        assertEquals(value,requests.take(second)); assertNull(requests.take(second))
    }
}
