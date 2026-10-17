package com.modukey.app.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
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
    @Test fun `invisible trigger draws zero pixels and continuous editor restores invisible pixels`() {
        val view = EdgeTriggerView(ApplicationProvider.getApplicationContext<Context>()) {}
        view.layout(0, 0, 16, 120)
        fun renderedPixels(): IntArray {
            val bitmap = Bitmap.createBitmap(16, 120, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            return IntArray(16 * 120).also { bitmap.getPixels(it, 0, 16, 0, 0, 16, 120) }
        }
        view.configure(TriggerSettings(), Edge.RIGHT, false)
        assertTrue(renderedPixels().all { it == 0 })
        val zeroThickness = TriggerSettings(indicatorVisible = true, indicatorThicknessFraction = 0f)
        view.configure(zeroThickness, Edge.RIGHT, false)
        assertTrue(renderedPixels().all { it == 0 })
        view.configure(zeroThickness, Edge.RIGHT, true)
        assertTrue(renderedPixels().any { it != 0 })
        view.configure(TriggerSettings(), Edge.RIGHT, true)
        assertTrue(renderedPixels().any { it != 0 })
        assertFalse(view.isClickable)
        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO, view.importantForAccessibility)
        assertTrue(view.systemGestureExclusionRects.isEmpty())
        view.configure(TriggerSettings(), Edge.RIGHT, false)
        assertTrue(renderedPixels().all { it == 0 })
    }

    @Test fun `editing cannot activate and appearance updates do not reset a gesture`() {
        var activations = 0
        val view = EdgeTriggerView(ApplicationProvider.getApplicationContext<Context>()) { activations++ }
        val s = TriggerSettings()
        view.layout(0, 0, 16, 120)
        view.configure(s, Edge.RIGHT, true)
        assertFalse(view.performClick())
        assertEquals(0, activations)
        view.configure(s, Edge.RIGHT, false)
        fun event(action: Int, x: Float, time: Long) {
            val e = MotionEvent.obtain(0, time, action, x, 20f, 0)
            try { view.onTouchEvent(e) } finally { e.recycle() }
        }
        event(MotionEvent.ACTION_DOWN, 80f, 0)
        view.configure(s.copy(indicatorOpacity = 0.2f), Edge.RIGHT, false)
        event(MotionEvent.ACTION_MOVE, 40f, 100)
        event(MotionEvent.ACTION_UP, 40f, 120)
        assertEquals(1, activations)
        event(MotionEvent.ACTION_DOWN, 80f, 200)
        view.cancelGesture()
        event(MotionEvent.ACTION_UP, 40f, 300)
        assertEquals(1, activations)
    }
}
