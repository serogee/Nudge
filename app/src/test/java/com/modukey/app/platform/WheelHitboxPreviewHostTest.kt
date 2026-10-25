package com.modukey.app.platform

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.test.core.app.ApplicationProvider
import com.modukey.app.NudgeApplication
import com.modukey.app.core.*
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
class WheelHitboxPreviewHostTest {
    private val app get() = ApplicationProvider.getApplicationContext<NudgeApplication>()
    @After fun cleanup() { app.onTerminate() }
    private class Windows(delegate: WindowManager) : WindowManager by delegate {
        val registered = linkedMapOf<View, WindowManager.LayoutParams>()
        override fun addView(view: View, params: ViewGroup.LayoutParams) {
            registered[view] = params as WindowManager.LayoutParams
            view.layoutParams = params; view.layout(0, 0, params.width, params.height)
        }
        override fun updateViewLayout(view: View, params: ViewGroup.LayoutParams) { registered[view] = params as WindowManager.LayoutParams }
        override fun removeViewImmediate(view: View) { registered.remove(view) }
    }

    @Test fun referenceCoversBoundsOutsideTheWheelOnEveryRotationWithoutOwningInput() {
        for (turns in 0..3) for (edge in Edge.entries) {
            val manager = Windows(app.getSystemService(WindowManager::class.java))
            val host = WheelHitboxPreviewHost(app, manager)
            val screen = ScreenSpace(800f, 1000f, 1f, Insets(), false)
            val odd = turns % 2 != 0
            val preview = PreviewGeometry(if (odd) 1000f else 800f, if (odd) 800f else 1000f, Insets(), odd, turns)
            val logical = Bounds(0f, 0f, preview.width, preview.height)
            val g = WheelGeometry.calculate(logical, 1f, PanelLayout(followHandle = false),
                WheelLayoutSettings(innerRadiusDp = 88f, outerRadiusDp = 136f), 6, logical, edge)
            val bitmap = Bitmap.createBitmap(800, 1000, Bitmap.Config.ARGB_8888)
            try {
                host.show(g, 20f to 300f, screen, preview)
                host.show(g, 20f to 300f, screen, preview)
                val (view, params) = manager.registered.entries.single()
                assertEquals(800, params.width); assertEquals(1000, params.height)
                assertTrue(params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE != 0)
                assertTrue(params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0)
                view.draw(Canvas(bitmap))
                for ((radius, filled) in listOf(10f to false, 40f to true, 200f to true, 310f to false)) {
                    val p = g.point(0f, radius)
                    val mapped = preview.toDisplay(Bounds(p.x, p.y, 0f, 0f))
                    assertEquals("$edge $turns $radius", filled, bitmap.getPixel(mapped.x.toInt(), mapped.y.toInt()) != 0)
                }
                host.hide(); host.hide()
                assertTrue(manager.registered.isEmpty())
            } finally { host.hide(); bitmap.recycle() }
        }
    }
}
