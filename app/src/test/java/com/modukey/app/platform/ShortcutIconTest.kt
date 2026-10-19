package com.modukey.app.platform

import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.modukey.app.NudgeApplication
import com.modukey.app.core.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ShortcutIconTest {
    private val app get() = ApplicationProvider.getApplicationContext<NudgeApplication>()
    @After fun cleanup() { app.onTerminate() }
    @Test fun everyStaticActionAndLibraryIconProducesPaddedArtwork() = runBlocking {
        val buttons = ButtonAction.entries.filter { it != ButtonAction.EMPTY }.map { PanelButton(action = it) } +
            BuiltInIcon.entries.map { PanelButton(icon = IconReference(builtIn = it), iconContainer = IconContainer.STANDALONE) }
        for (button in buttons) {
            val bitmap = ShortcutIconRenderer.render(app, app.icons, button)
            assertEquals(Color.TRANSPARENT, bitmap.getPixel(0, 0))
            val pixels = IntArray(bitmap.width * bitmap.height); bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            assertTrue(button.toString(), pixels.any { Color.alpha(it) > 0 })
            if (button.icon.builtIn != null || button.action != ButtonAction.NUDGE_SETTINGS)
                assertTrue(button.toString(), pixels.any { Color.red(it) > 220 && Color.green(it) > 220 && Color.alpha(it) > 200 })
            bitmap.recycle()
        }
    }
}
