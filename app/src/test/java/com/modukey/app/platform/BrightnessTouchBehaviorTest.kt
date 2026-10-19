package com.modukey.app.platform

import android.os.Looper
import android.view.MotionEvent
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.modukey.app.NudgeApplication
import com.modukey.app.core.BrightnessPopupSettings
import com.modukey.app.core.BrightnessTouchBehavior
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36])
class BrightnessTouchBehaviorTest {
    private val app get() = ApplicationProvider.getApplicationContext<NudgeApplication>()
    @After fun cleanup() { app.onTerminate() }
    private fun view(writes: MutableList<Float>, mode: BrightnessTouchBehavior, level: Float = .5f) =
        BrightnessPopupView(app, writes::add, {}, {}).apply {
            configure(BrightnessPopupSettings(showBackground = false, showAutomaticToggle = false, touchBehavior = mode), false, level)
            measure(View.MeasureSpec.makeMeasureSpec(64, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(240, View.MeasureSpec.EXACTLY))
            layout(0, 0, 64, 240)
        }
    private fun touch(view: View, vararg events: Pair<Int, Float>) {
        events.forEach { (action, y) -> MotionEvent.obtain(0, 20, action, 30f, y, 0).let { view.dispatchTouchEvent(it); it.recycle() } }
    }

    @Test fun jumpModeUsesTheTouchPosition() {
        val writes = mutableListOf<Float>()
        val view = view(writes, BrightnessTouchBehavior.JUMP_TO_TOUCH)
        touch(view, MotionEvent.ACTION_DOWN to 180f, MotionEvent.ACTION_UP to 180f)
        assertEquals(.25f, writes.single(), .001f)
    }

    @Test fun slideModeStartsAtTheCurrentLevelAndIgnoresStationaryTaps() {
        val writes = mutableListOf<Float>()
        val view = view(writes, BrightnessTouchBehavior.SLIDE_TO_ADJUST, .4f)
        touch(view, MotionEvent.ACTION_DOWN to 200f, MotionEvent.ACTION_UP to 200f)
        assertTrue(writes.isEmpty())
        touch(view, MotionEvent.ACTION_DOWN to 200f, MotionEvent.ACTION_MOVE to 176f, MotionEvent.ACTION_UP to 176f)
        assertEquals(.5f, writes.single(), .001f)
    }

    @Test fun slideModeClampsAtLimitsAndRespondsImmediatelyWhenDirectionReverses() {
        val writes = mutableListOf<Float>()
        val view = view(writes, BrightnessTouchBehavior.SLIDE_TO_ADJUST)
        touch(view, MotionEvent.ACTION_DOWN to 200f, MotionEvent.ACTION_MOVE to 80f,
            MotionEvent.ACTION_MOVE to 56f, MotionEvent.ACTION_UP to 80f)
        assertEquals(.9f, writes.single(), .001f)
        writes.clear()
        touch(view, MotionEvent.ACTION_DOWN to 10f, MotionEvent.ACTION_MOVE to 250f, MotionEvent.ACTION_UP to 226f)
        assertEquals(.1f, writes.single(), .001f)
    }

    @Test fun cancellationAndModeChangesDropPendingRelativeWrites() {
        val writes = mutableListOf<Float>()
        val view = view(writes, BrightnessTouchBehavior.SLIDE_TO_ADJUST)
        touch(view, MotionEvent.ACTION_DOWN to 200f, MotionEvent.ACTION_MOVE to 176f, MotionEvent.ACTION_CANCEL to 176f)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        assertTrue(writes.isEmpty())
        touch(view, MotionEvent.ACTION_DOWN to 200f, MotionEvent.ACTION_MOVE to 176f)
        view.configure(BrightnessPopupSettings(showBackground = false, showAutomaticToggle = false), false, .5f)
        touch(view, MotionEvent.ACTION_UP to 176f)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        assertTrue(writes.isEmpty())
    }
}
