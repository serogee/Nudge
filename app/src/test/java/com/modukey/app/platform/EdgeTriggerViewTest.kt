package com.modukey.app.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.core.app.ApplicationProvider
import com.modukey.app.core.Edge
import com.modukey.app.core.TriggerSettings
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EdgeTriggerViewTest {
    @Test fun `invisible trigger draws zero pixels while setup highlight draws only its bounds`() {
        val view = EdgeTriggerView(ApplicationProvider.getApplicationContext<Context>()) {}
        view.layout(0, 0, 16, 120)
        fun renderedPixels(): IntArray {
            val bitmap = Bitmap.createBitmap(16, 120, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            return IntArray(16 * 120).also { bitmap.getPixels(it, 0, 16, 0, 0, 16, 120) }
        }
        view.configure(TriggerSettings(), Edge.RIGHT, false)
        assertTrue(renderedPixels().all { it == 0 })
        view.configure(TriggerSettings(), Edge.RIGHT, true)
        assertTrue(renderedPixels().any { it != 0 })
        view.configure(TriggerSettings(), Edge.RIGHT, false)
        assertTrue(renderedPixels().all { it == 0 })
    }
}
