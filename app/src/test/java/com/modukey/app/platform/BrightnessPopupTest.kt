package com.modukey.app.platform

import android.app.Activity
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.test.core.app.ApplicationProvider
import com.modukey.app.NudgeApplication
import com.modukey.app.core.*
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
class BrightnessPopupTest {
    private val app get() = ApplicationProvider.getApplicationContext<NudgeApplication>()
    @After fun cleanup() { app.onTerminate() }
    private fun event(action: Int, y: Float) = MotionEvent.obtain(0, 20, action, 30f, y, 0)
    @Test fun previewDoesNotConsumeTouchesOrChangeBrightness() {
        val writes = mutableListOf<Float>()
        val view = BrightnessPopupView(app, writes::add, {}, {})
        view.layout(0, 0, 64, 240); view.configure(BrightnessPopupSettings(), true, .5f)
        event(MotionEvent.ACTION_DOWN, 30f).let { assertFalse(view.onTouchEvent(it)); it.recycle() }
        event(MotionEvent.ACTION_UP, 50f).let { assertFalse(view.onTouchEvent(it)); it.recycle() }
        assertTrue(writes.isEmpty())
    }
    @Test fun finalReleaseWritesLatestLevelAndCancelledInputDoesNotFlush() {
        val writes = mutableListOf<Float>()
        val view = BrightnessPopupView(app, writes::add, {}, {})
        view.layout(0, 0, 64, 240); view.configure(BrightnessPopupSettings(), false, .5f)
        listOf(MotionEvent.ACTION_DOWN to 200f, MotionEvent.ACTION_MOVE to 150f, MotionEvent.ACTION_UP to 6f).forEach { (action, y) -> event(action, y).let { view.onTouchEvent(it); it.recycle() } }
        assertEquals(1, writes.size); assertEquals(1f, writes.single(), .01f)
        writes.clear()
        listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_CANCEL).forEach { action -> event(action, 150f).let { view.onTouchEvent(it); it.recycle() } }
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100)); assertTrue(writes.isEmpty())
    }
    @Test fun hostReusesOneWindowAndStopsItsTimeoutAfterHiding() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        val context = controller.get(); val wm = context.getSystemService(WindowManager::class.java)
        var dismissed = 0
        lateinit var host: BrightnessPopupHost
        host = BrightnessPopupHost(context, wm, app.brightness, { dismissed++; host.hide() }, { fail("Window failed: $it") })
        val screen = ScreenSpace(400f, 800f, 1f, Insets(), false)
        try {
            assertTrue(host.show(BrightnessPopupSettings(timeoutMs = 1_000), screen, false))
            assertTrue(host.show(BrightnessPopupSettings(widthDp = 88f, timeoutMs = 1_000), screen, false))
            assertTrue(host.visible)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1_001)); assertEquals(1, dismissed); assertFalse(host.visible)
            host.show(BrightnessPopupSettings(), screen, true)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(20)); assertTrue(host.visible); assertEquals(1, dismissed)
            host.hide(); host.hide()
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(20)); assertEquals(1, dismissed)
        } finally { host.hide(); controller.pause().stop().destroy() }
    }
    @Test fun oldPreviewOwnerCannotClearReplacementSession() {
        val preview = BrightnessPreview(); val old = Any(); val current = Any()
        preview.begin(old); preview.begin(current); preview.end(old); assertTrue(preview.state.value)
        preview.end(current); assertFalse(preview.state.value)
    }
}
