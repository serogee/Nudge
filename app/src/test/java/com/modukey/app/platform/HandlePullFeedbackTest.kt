package com.modukey.app.platform

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.MotionEvent
import androidx.test.core.app.ApplicationProvider
import com.modukey.app.NudgeApplication
import com.modukey.app.core.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36], qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HandlePullFeedbackTest {
    private val app get() = ApplicationProvider.getApplicationContext<NudgeApplication>()
    private fun scale(value: Float) = ReflectionHelpers.callStaticMethod<Void>(ValueAnimator::class.java,
        "setDurationScale", ClassParameter.from(Float::class.javaPrimitiveType, value))
    @After fun cleanup() { scale(1f); app.onTerminate() }
    private fun idle(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
    private fun pixels(view: HandleTriggerView): IntArray {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val result = IntArray(view.width * view.height)
        bitmap.getPixels(result, 0, view.width, 0, 0, view.width, view.height)
        bitmap.recycle()
        return result
    }
    private fun touch(view: HandleTriggerView, edge: Edge, action: Int, time: Long, distance: Float = 0f) {
        val x = when (edge) { Edge.LEFT -> 1f + distance; Edge.RIGHT -> 47f - distance; Edge.BOTTOM -> 90f }
        val y = if (edge == Edge.BOTTOM) 47f - distance else 90f
        val event = MotionEvent.obtain(0, time, action, x, y, 0)
        try { assertTrue(view.onTouchEvent(event)) } finally { event.recycle() }
    }
    @Test fun visibleBarsBendWhileHeldAndReturnAfterBothSwipeTypesOnEveryEdge() {
        scale(1f)
        for (edge in Edge.entries) {
            val actions = mutableListOf<GestureKind>()
            val view = HandleTriggerView(app, actions::add)
            view.layout(0, 0, if (edge == Edge.BOTTOM) 180 else 48, if (edge == Edge.BOTTOM) 48 else 180)
            view.configure(TriggerSettings(indicatorVisible = true, indicatorOpacity = 1f), edge, false)
            val rest = pixels(view)
            assertTrue(rest.any { it != 0 })
            touch(view, edge, MotionEvent.ACTION_DOWN, 100)
            touch(view, edge, MotionEvent.ACTION_MOVE, 200, 40f)
            val pulled = pixels(view)
            assertFalse("The visible bar must bend on $edge", rest.contentEquals(pulled))
            idle(700)
            assertArrayEquals("The bar must stay pulled while held", pulled, pixels(view))
            assertTrue(actions.isEmpty())
            touch(view, edge, MotionEvent.ACTION_UP, 900, 40f)
            assertEquals(listOf(GestureKind.SWIPE_HOLD), actions)
            assertTrue(view.hasInteraction)
            idle(250)
            assertFalse(view.hasInteraction)
            assertArrayEquals(rest, pixels(view))
            touch(view, edge, MotionEvent.ACTION_DOWN, 1000)
            touch(view, edge, MotionEvent.ACTION_MOVE, 1100, 40f)
            touch(view, edge, MotionEvent.ACTION_UP, 1200, 40f)
            idle(250)
            assertEquals(listOf(GestureKind.SWIPE_HOLD, GestureKind.SWIPE), actions)
            assertArrayEquals(rest, pixels(view))
        }
    }
    @Test fun cancellationHiddenBarsAndDisabledAnimationsLeaveNoFeedbackRunning() {
        scale(1f)
        val view = HandleTriggerView(app, {})
        view.layout(0, 0, 48, 180)
        val settings = TriggerSettings(indicatorVisible = true)
        view.configure(settings, Edge.LEFT, false)
        val rest = pixels(view)
        touch(view, Edge.LEFT, MotionEvent.ACTION_DOWN, 100)
        touch(view, Edge.LEFT, MotionEvent.ACTION_MOVE, 200, 40f)
        view.cancelGesture()
        assertFalse(view.hasInteraction); assertArrayEquals(rest, pixels(view))
        scale(0f)
        touch(view, Edge.LEFT, MotionEvent.ACTION_DOWN, 1000)
        touch(view, Edge.LEFT, MotionEvent.ACTION_MOVE, 1100, 40f)
        assertArrayEquals(rest, pixels(view))
        touch(view, Edge.LEFT, MotionEvent.ACTION_UP, 1200, 40f)
        assertFalse(view.hasInteraction)
        scale(1f)
        view.configure(settings, Edge.LEFT, false, hideIndicator = true)
        touch(view, Edge.LEFT, MotionEvent.ACTION_DOWN, 2000)
        touch(view, Edge.LEFT, MotionEvent.ACTION_MOVE, 2100, 40f)
        assertTrue(pixels(view).all { it == 0 })
        touch(view, Edge.LEFT, MotionEvent.ACTION_UP, 2200, 40f)
        assertFalse(view.hasInteraction)
    }
}
