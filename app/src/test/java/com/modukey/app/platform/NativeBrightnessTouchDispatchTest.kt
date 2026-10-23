package com.modukey.app.platform

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.modukey.app.NudgeApplication
import com.modukey.app.core.Bounds
import kotlinx.coroutines.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36])
class NativeBrightnessTouchDispatchTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val owner = Robolectric.buildService(BrightnessCalibrationService::class.java).create()
    private val screen = Bounds(0f, 0f, 1080f, 2400f)
    private val control = NativeBrightnessControl("native", 0f, 2047f, 1024f, true, Bounds(100f, 200f, 120f, 600f), true)
    @After fun cleanup() { scope.cancel(); owner.destroy(); ApplicationProvider.getApplicationContext<NudgeApplication>().onTerminate() }
    @Test fun releaseIsFiniteAndCancellationCannotRestoreBeforeTheTouchEnds() {
        lateinit var gesture: GestureDescription
        lateinit var callback: AccessibilityService.GestureResultCallback
        val completion = NativeBrightnessTouchCompletion(owner.get()) { g, c -> gesture = g; callback = c; true }
        var ended = false
        scope.launch { try { completion.release(control, screen) } finally { ended = true } }
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(completion.active); assertEquals(1, gesture.strokeCount)
        assertEquals(50L, gesture.getStroke(0).duration)
        scope.cancel(); assertFalse(ended); assertTrue(completion.active)
        callback.onCompleted(gesture)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(ended); assertFalse(completion.active)
    }
    @Test fun refusalTimeoutAndLateCallbacksNeverLeaveAnActiveTouch() {
        var result: Boolean? = null
        val refused = NativeBrightnessTouchCompletion(owner.get()) { _, _ -> false }
        scope.launch { result = refused.release(control, screen) }
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(false, result); assertFalse(refused.active)
        lateinit var callback: AccessibilityService.GestureResultCallback
        lateinit var gesture: GestureDescription
        val timeout = NativeBrightnessTouchCompletion(owner.get()) { g, c -> gesture = g; callback = c; true }
        scope.launch { result = timeout.release(control, screen) }
        shadowOf(Looper.getMainLooper()).idle()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1_001))
        assertEquals(false, result); assertFalse(timeout.active)
        callback.onCompleted(gesture)
        assertEquals(false, result)
    }
    @Test fun platformCancellationEndsTheTouchWithoutReportingACommit() {
        lateinit var gesture: GestureDescription
        lateinit var callback: AccessibilityService.GestureResultCallback
        var result: Boolean? = null
        val completion = NativeBrightnessTouchCompletion(owner.get()) { g, c -> gesture = g; callback = c; true }
        scope.launch { result = completion.release(control, screen) }
        shadowOf(Looper.getMainLooper()).idle()
        callback.onCancelled(gesture)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(false, result); assertFalse(completion.active)
        callback.onCompleted(gesture)
        assertEquals(false, result)
    }
}
