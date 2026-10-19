package com.modukey.app.platform

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.MotionEvent
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
@Config(sdk = [31, 36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HandleVisibilityViewTest {
    private val app get() = ApplicationProvider.getApplicationContext<NudgeApplication>()
    @After fun cleanup() { app.onTerminate() }
    private fun visiblePixels(view: HandleTriggerView): Int {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val pixels = IntArray(view.width * view.height)
        bitmap.getPixels(pixels, 0, view.width, 0, 0, view.width, view.height)
        bitmap.recycle()
        return pixels.count { it != 0 }
    }
    @Test fun hidingDrawsNothingRetainsTouchAndRestoresOnlySavedVisibleBars() {
        var actions = 0
        val view = HandleTriggerView(app, { actions++ })
        val settings = TriggerSettings(indicatorVisible = true, indicatorOpacity = 1f)
        view.layout(0, 0, 16, 120)
        view.configure(settings, Edge.RIGHT, false)
        assertTrue(visiblePixels(view) > 0)
        view.configure(settings, Edge.RIGHT, false, hideIndicator = true)
        assertEquals(0, visiblePixels(view)); assertTrue(view.performClick()); assertEquals(1, actions)
        view.configure(settings, Edge.RIGHT, false)
        assertTrue(visiblePixels(view) > 0)
        view.configure(settings.copy(indicatorVisible = false), Edge.RIGHT, false)
        assertEquals(0, visiblePixels(view))
    }
    @Test fun editingPreviewStaysVisibleAndChangingVisibilityDoesNotCancelATap() {
        var actions = 0
        val view = HandleTriggerView(app, { actions++ })
        val settings = TriggerSettings(indicatorVisible = true)
        view.layout(0, 0, 16, 120)
        view.configure(settings, Edge.RIGHT, true, hideIndicator = true)
        assertTrue(visiblePixels(view) > 0); assertFalse(view.performClick())
        view.configure(settings, Edge.RIGHT, false)
        MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 8f, 60f, 0).let { view.onTouchEvent(it); it.recycle() }
        view.configure(settings, Edge.RIGHT, false, hideIndicator = true)
        MotionEvent.obtain(0, 100, MotionEvent.ACTION_UP, 8f, 60f, 0).let { view.onTouchEvent(it); it.recycle() }
        assertEquals(1, actions); assertEquals(0, visiblePixels(view))
    }
}
