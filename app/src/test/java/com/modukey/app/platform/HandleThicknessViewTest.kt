package com.modukey.app.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.modukey.app.core.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36], qualifiers = "xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HandleThicknessViewTest {
    @Test fun `native handles use density and clamp drawing without changing saved thickness`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals(2f, context.resources.displayMetrics.density)
        for (edge in Edge.entries) for (legacy in listOf(false, true)) {
            val view: View = if (legacy) EdgeTriggerView(context) {} else HandleTriggerView(context, {})
            fun draw(thicknessDp: Float, touchDepthPx: Int): Int {
                val settings = TriggerSettings(indicatorVisible = true, indicatorThicknessDp = thicknessDp, indicatorOpacity = 1f, cornerRadiusDp = 0f)
                if (edge == Edge.BOTTOM) view.layout(0, 0, 240, touchDepthPx) else view.layout(0, 0, touchDepthPx, 240)
                if (view is EdgeTriggerView) view.configure(settings, edge, false) else (view as HandleTriggerView).configure(settings, edge, false)
                val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                view.draw(Canvas(bitmap))
                val visible = if (edge == Edge.BOTTOM) (0 until view.height).count { bitmap.getPixel(view.width / 2, it) != 0 }
                    else (0 until view.width).count { bitmap.getPixel(it, view.height / 2) != 0 }
                bitmap.recycle()
                return visible
            }
            assertEquals(8, draw(4f, 32))
            assertEquals(13, draw(6.5f, 32))
            assertEquals(16, draw(18f, 16))
            assertEquals(36, draw(18f, 64))
            assertEquals(0, draw(0f, 32))
        }
    }
}
