package com.modukey.app.platform

import android.app.KeyguardManager
import android.os.Looper
import android.os.PowerManager
import android.os.VibratorManager
import android.view.MotionEvent
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
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36], qualifiers = "mdpi")
class DisabledGestureRuntimeTest {
    private val app get() = ApplicationProvider.getApplicationContext<NudgeApplication>()
    private var controller: ServiceController<EdgeAccessibilityService>? = null
    @After fun cleanup() { controller?.destroy(); app.onTerminate() }
    private fun idle(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
    private fun await(condition: () -> Boolean) {
        repeat(300) { idle(16); if (condition()) return; Thread.sleep(5) }
        assertTrue("Service did not reach the expected state", condition())
    }
    private class Host : PanelHost {
        var latest: PanelSpec? = null
        override fun show(spec: PanelSpec) { latest = spec }
        override fun hide(animate: Boolean) { latest = null }
        override fun refreshSound() {}
    }
    @Suppress("DEPRECATION")
    private fun start(bindings: Map<GestureSlot, GestureTarget>, panels: List<SavedPanel> = emptyList()): EdgeAccessibilityService {
        val service = Robolectric.buildService(EdgeAccessibilityService::class.java).create().also { controller = it }.get()
        shadowOf(app).grantPermissions(android.Manifest.permission.VIBRATE, "${app.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
        shadowOf(service.getSystemService(PowerManager::class.java)).setIsInteractive(true)
        shadowOf(service.getSystemService(KeyguardManager::class.java)).setKeyguardLocked(false)
        await { app.configuration.state.value != null }
        val feedback = GestureFeedback(true)
        val handle = HandleConfig(id = "handle", enabled = true, bindings = bindings, trigger = TriggerSettings(
            portrait = Placement(Edge.LEFT), landscapeEnabled = true, indicatorVisible = true, swipeTimeoutMs = 1200,
            holdExecution = HoldExecution.AFTER_TIME, swipeHoldExecution = HoldExecution.AFTER_TIME,
            swipeFeedback = feedback, tapFeedback = feedback, holdFeedback = feedback, swipeHoldFeedback = feedback))
        app.configuration.update { AppConfiguration(enabled = true, handles = listOf(handle), panels = panels, handleAnimationsEnabled = false) }
        ReflectionHelpers.callInstanceMethod<Void>(service, "onServiceConnected")
        await { views(service).containsKey("handle") }
        shadowOf(service.getSystemService(VibratorManager::class.java).defaultVibrator).setHasVibrator(true)
        return service
    }
    private fun views(service: EdgeAccessibilityService) = ReflectionHelpers.getField<Map<String, HandleTriggerView>>(service, "views")
    private fun touch(view: HandleTriggerView, action: Int, time: Long, x: Float = 1f): Boolean {
        val event = MotionEvent.obtain(100, time, action, x, 20f, 0)
        return try { view.onTouchEvent(event) } finally { event.recycle() }
    }
    @Test fun allEmptyAssignmentsMakeTheVisibleHandleUntouchableAndIgnoreFeedback() {
        val service = start(mapOf(GestureSlot.TAP to GestureTarget.direct(PanelButton(action = ButtonAction.EMPTY))))
        val view = views(service).getValue("handle")
        assertTrue((view.layoutParams as WindowManager.LayoutParams).flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE != 0)
        assertFalse(view.isClickable); assertTrue(view.systemGestureExclusionRects.isEmpty())
        assertFalse(touch(view, MotionEvent.ACTION_DOWN, 100)); assertFalse(view.performClick())
        assertFalse(view.hasInteraction)
        for (kind in GestureKind.entries) ReflectionHelpers.callInstanceMethod<Void>(service, "activate",
            ClassParameter.from(String::class.java, "handle"), ClassParameter.from(GestureKind::class.java, kind))
        assertFalse(shadowOf(service.getSystemService(VibratorManager::class.java).defaultVibrator).isVibrating)
        for (target in listOf(GestureTarget(volume = true), GestureTarget(brightness = true))) {
            app.configuration.update { it.editHandle("handle") { h -> h.copy(bindings = mapOf(GestureSlot.TAP to target)) } }
            await { (views(service).getValue("handle").layoutParams as WindowManager.LayoutParams).flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE == 0 }
            assertSame(view, views(service).getValue("handle")); assertTrue(view.isClickable)
            app.configuration.update { it.editHandle("handle") { h -> h.copy(bindings = emptyMap()) } }
            await { (view.layoutParams as WindowManager.LayoutParams).flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE != 0 }
        }
    }
    @Test fun emptyGesturesDoNotVibrateOrResetTheNextAssignedSwipe() {
        val first = SavedPanel(name = "First"); val second = SavedPanel(name = "Second")
        val service = start(mapOf(GestureSlot.SWIPE_1 to GestureTarget.panel(first.id),
            GestureSlot.SWIPE_2 to GestureTarget.panel(second.id)), listOf(first, second))
        val host = Host(); ReflectionHelpers.setField(service, "panelHost", host)
        val handle = app.configuration.state.value!!.handle("handle")!!
        ReflectionHelpers.getField<GestureSessions>(service, "sessions").submitted(handle, 0, handle.target(GestureSlot.SWIPE_1), 0)
        val view = views(service).getValue("handle")
        assertFalse(view.performClick())
        assertTrue(touch(view, MotionEvent.ACTION_DOWN, 100)); assertTrue(touch(view, MotionEvent.ACTION_UP, 200))
        for (kind in listOf(GestureKind.TAP, GestureKind.HOLD, GestureKind.SWIPE_HOLD))
            ReflectionHelpers.callInstanceMethod<Void>(service, "activate",
                ClassParameter.from(String::class.java, "handle"), ClassParameter.from(GestureKind::class.java, kind))
        assertNull(host.latest)
        val vibrator = shadowOf(service.getSystemService(VibratorManager::class.java).defaultVibrator)
        assertFalse(vibrator.isVibrating)
        assertTrue(touch(view, MotionEvent.ACTION_DOWN, 1000))
        assertTrue(touch(view, MotionEvent.ACTION_MOVE, 1100, 31f))
        assertTrue(touch(view, MotionEvent.ACTION_UP, 1200, 31f))
        assertEquals(second.id, app.status.state.value.panelId)
        assertEquals(second.settings, host.latest!!.panel)
        assertTrue(vibrator.isVibrating)
    }
    @Test fun anUnassignedImmediateHoldDoesNotConsumeALaterSwipe() {
        val panel = SavedPanel(name = "Tools")
        val service = start(mapOf(GestureSlot.SWIPE_1 to GestureTarget.panel(panel.id)), listOf(panel))
        val host = Host(); ReflectionHelpers.setField(service, "panelHost", host)
        val view = views(service).getValue("handle")
        assertTrue(touch(view, MotionEvent.ACTION_DOWN, 100))
        idle(600)
        assertFalse(ReflectionHelpers.getField<HandleGesture>(view, "gesture").emitted)
        assertNull(host.latest)
        assertFalse(shadowOf(service.getSystemService(VibratorManager::class.java).defaultVibrator).isVibrating)
        assertTrue(touch(view, MotionEvent.ACTION_MOVE, 700, 31f))
        assertTrue(touch(view, MotionEvent.ACTION_UP, 800, 31f))
        assertEquals(panel.settings, host.latest!!.panel)
    }
}
