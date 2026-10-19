package com.modukey.app.platform

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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36], qualifiers = "mdpi")
class HoldTimingViewTest {
    private val app get() = ApplicationProvider.getApplicationContext<NudgeApplication>()
    @After fun cleanup() { app.onTerminate() }
    private fun touch(view: HandleTriggerView, action: Int, time: Long, x: Float = 1f) {
        val event = MotionEvent.obtain(0, time, action, x, 20f, 0)
        try { assertTrue(view.onTouchEvent(event)) } finally { event.recycle() }
    }
    @Test fun `native view uses separate delays and fires once on release`() {
        val actions = mutableListOf<GestureKind>()
        val view = HandleTriggerView(app, { actions += it })
        view.layout(0, 0, 48, 180)
        view.configure(TriggerSettings(holdTimeMs = 1_200, swipeHoldTimeMs = 900), Edge.LEFT, false)
        touch(view, MotionEvent.ACTION_DOWN, 100); touch(view, MotionEvent.ACTION_MOVE, 1_300)
        assertTrue(actions.isEmpty())
        touch(view, MotionEvent.ACTION_UP, 1_300)
        assertEquals(listOf(GestureKind.HOLD), actions)
        touch(view, MotionEvent.ACTION_UP, 1_301)
        assertEquals(1, actions.size)
        touch(view, MotionEvent.ACTION_DOWN, 2_000); touch(view, MotionEvent.ACTION_MOVE, 2_100, 31f)
        touch(view, MotionEvent.ACTION_UP, 3_000, 31f)
        assertEquals(listOf(GestureKind.HOLD, GestureKind.SWIPE_HOLD), actions)
    }
    @Test fun `changing a delay cancels captured input and the next gesture uses its new delay`() {
        val actions = mutableListOf<GestureKind>()
        val view = HandleTriggerView(app, { actions += it })
        view.layout(0, 0, 48, 180)
        val settings = TriggerSettings(swipeHoldTimeMs = 900)
        view.configure(settings, Edge.LEFT, false)
        touch(view, MotionEvent.ACTION_DOWN, 0)
        view.configure(settings.copy(holdTimeMs = 1_200), Edge.LEFT, false)
        touch(view, MotionEvent.ACTION_UP, 1_200); assertTrue(actions.isEmpty())
        touch(view, MotionEvent.ACTION_DOWN, 2_000); touch(view, MotionEvent.ACTION_UP, 2_500)
        assertTrue(actions.isEmpty())
        touch(view, MotionEvent.ACTION_DOWN, 3_000); touch(view, MotionEvent.ACTION_UP, 4_200)
        assertEquals(listOf(GestureKind.HOLD), actions)
        touch(view, MotionEvent.ACTION_DOWN, 5_000); touch(view, MotionEvent.ACTION_MOVE, 5_100, 31f)
        view.configure(settings.copy(holdTimeMs = 1_200, swipeHoldTimeMs = 1_500), Edge.LEFT, false)
        touch(view, MotionEvent.ACTION_UP, 6_600, 31f); assertEquals(1, actions.size)
    }
}
