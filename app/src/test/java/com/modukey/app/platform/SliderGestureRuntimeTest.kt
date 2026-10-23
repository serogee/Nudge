package com.modukey.app.platform

import android.app.KeyguardManager
import android.media.AudioManager
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.MotionEvent
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
import org.robolectric.util.ReflectionHelpers
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36], qualifiers = "mdpi", shadows = [BrightnessControllerTest.Writes::class])
@Suppress("DEPRECATION")
class SliderGestureRuntimeTest {
    private val app get() = ApplicationProvider.getApplicationContext<NudgeApplication>()
    private var destroy: () -> Unit = {}
    @After fun cleanup() { destroy(); BrightnessControllerTest.Writes.allowed = false; app.onTerminate() }
    private fun idle(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
    private fun await(attempts: Int = 300, condition: () -> Boolean) {
        repeat(attempts) { idle(16); if (condition()) return; Thread.sleep(5) }
        assertTrue(app.diagnostics.details.value, condition())
    }
    private fun fixture(slot: GestureSlot = GestureSlot.SWIPE_1, activation: SliderActivation = SliderActivation.AFTER_HOLD,
        holdMs: Long = 200, kind: SliderKind = SliderKind.BRIGHTNESS): Pair<EdgeAccessibilityService, HandleTriggerView> {
        val controller = Robolectric.buildService(EdgeAccessibilityService::class.java).create(); destroy = { controller.destroy() }
        val service = controller.get()
        shadowOf(app).grantPermissions("${app.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
        shadowOf(app.getSystemService(PowerManager::class.java)).setIsInteractive(true)
        shadowOf(app.getSystemService(KeyguardManager::class.java)).setKeyguardLocked(false)
        // Disk-backed configuration can load slowly during the full native-rendering suite.
        await(attempts = 2_000) { app.configuration.state.value != null }
        BrightnessControllerTest.Writes.allowed = true
        Settings.System.putInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS, BrightnessScale().level(.5f))
        Settings.System.putInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, 0)
        val audio = app.getSystemService(AudioManager::class.java)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2, 0)
        val handle = HandleConfig(id = "slider", enabled = true, trigger = TriggerSettings(portrait = Placement(Edge.LEFT), landscapeEnabled = true),
            bindings = mapOf(slot to GestureTarget(brightness = kind == SliderKind.BRIGHTNESS, volume = kind == SliderKind.VOLUME,
                slideToAdjust = true, sliderActivation = activation, sliderHoldMs = holdMs)), brightnessTravelDp = 240f)
        app.configuration.update { AppConfiguration(enabled = true, handles = listOf(handle), brightnessPopup = BrightnessPopupSettings(timeoutMs = 1000)) }
        ReflectionHelpers.callInstanceMethod<Void>(service, "onServiceConnected")
        await { app.status.state.value.attachedHandleIds.contains(handle.id) }
        val view = ReflectionHelpers.getField<Map<String, HandleTriggerView>>(service, "views").getValue(handle.id)
        return service to view
    }
    private fun host(service: EdgeAccessibilityService) = ReflectionHelpers.getField<BrightnessPopupHost>(service, "brightnessHost")
    private fun touch(view: HandleTriggerView, action: Int, time: Long, x: Float = 1f, y: Float = 200f) {
        val event = MotionEvent.obtain(0, time, action, x, y, 0)
        try { assertTrue(view.onTouchEvent(event)) } finally { event.recycle() }
    }
    @Test fun swipeCapturesOutsideTheHandleWithoutAnInitialJumpAndHoldsPopupUntilRelease() {
        val (service, view) = fixture(activation = SliderActivation.IMMEDIATE)
        val before = app.brightness.fraction()
        touch(view, MotionEvent.ACTION_DOWN, 100)
        touch(view, MotionEvent.ACTION_MOVE, 200, x = 31f)
        assertTrue(host(service).visible); assertEquals(before, app.brightness.fraction(), .001f)
        idle(1500); assertTrue(host(service).visible)
        touch(view, MotionEvent.ACTION_MOVE, 1800, x = -700f, y = 140f); idle(32)
        assertEquals(before + .25f, app.brightness.fraction(), .012f)
        touch(view, MotionEvent.ACTION_UP, 1900, x = -700f, y = 140f)
        val released = app.brightness.fraction()
        idle(800); assertTrue(host(service).visible)
        idle(300); assertFalse(host(service).visible)
        assertEquals(released, app.brightness.fraction(), .001f)
    }
    @Test fun guardedBrightnessOpensImmediatelyThenWaitsForStillnessAndExcludesInitialMovement() {
        val (service, view) = fixture(holdMs = 350)
        val before = app.brightness.fraction()
        touch(view, MotionEvent.ACTION_DOWN, 100)
        touch(view, MotionEvent.ACTION_MOVE, 200, x = 31f)
        assertTrue(host(service).visible)
        idle(150)
        touch(view, MotionEvent.ACTION_MOVE, 350, x = 100f, y = 180f)
        idle(349)
        assertEquals(before, app.brightness.fraction(), .001f)
        touch(view, MotionEvent.ACTION_MOVE, 699, x = 101f, y = 179f)
        idle(1)
        touch(view, MotionEvent.ACTION_MOVE, 710, x = 101f, y = 171f)
        idle(32); assertEquals(before, app.brightness.fraction(), .001f)
        touch(view, MotionEvent.ACTION_MOVE, 750, x = -700f, y = 111f)
        idle(32); assertEquals(before + .25f, app.brightness.fraction(), .012f)
        touch(view, MotionEvent.ACTION_UP, 800, x = -700f, y = 87f)
        assertEquals(before + .35f, app.brightness.fraction(), .012f)
        idle(1100); assertFalse(host(service).visible)
    }
    @Test fun guardedVolumeKeepsTheValueOnEarlyReleaseAndAdjustsWithoutLiftingAfterTheHold() {
        val (_, view) = fixture(holdMs = 100, kind = SliderKind.VOLUME)
        val before = app.volume.read()!!
        touch(view, MotionEvent.ACTION_DOWN, 100)
        touch(view, MotionEvent.ACTION_MOVE, 200, x = 31f)
        idle(99)
        touch(view, MotionEvent.ACTION_MOVE, 299, x = 80f, y = 100f)
        idle(99)
        touch(view, MotionEvent.ACTION_UP, 398, x = 80f, y = 0f)
        idle(300); assertEquals(before.level, app.volume.read()!!.level)
        touch(view, MotionEvent.ACTION_DOWN, 800)
        touch(view, MotionEvent.ACTION_MOVE, 900, x = 31f)
        idle(100)
        touch(view, MotionEvent.ACTION_MOVE, 1010, x = 31f, y = -48f)
        idle(32); assertEquals(before.maximum, app.volume.read()!!.level)
        touch(view, MotionEvent.ACTION_UP, 1100, x = 31f, y = -48f)
        idle(1100); assertEquals(before.maximum, app.volume.read()!!.level)
    }
    @Test fun holdOverridesReleaseTimingAndCancellationDropsQueuedChanges() {
        val (service, view) = fixture(GestureSlot.HOLD)
        touch(view, MotionEvent.ACTION_DOWN, 100); idle(500)
        assertTrue(host(service).visible)
        val before = app.brightness.fraction()
        touch(view, MotionEvent.ACTION_MOVE, 700, y = 100f)
        touch(view, MotionEvent.ACTION_POINTER_DOWN, 710, y = 100f)
        idle(32); assertEquals(before, app.brightness.fraction(), .001f)
        assertFalse(view.isTouching)
        touch(view, MotionEvent.ACTION_UP, 800, y = 0f)
        assertEquals(before, app.brightness.fraction(), .001f)
        idle(1100); assertFalse(host(service).visible)
    }
    @Test fun handleRemovalAndRotationEndCapturedInput() {
        val (service, view) = fixture()
        touch(view, MotionEvent.ACTION_DOWN, 100); touch(view, MotionEvent.ACTION_MOVE, 200, x = 31f)
        ReflectionHelpers.callInstanceMethod<Void>(service, "rebuildHandles")
        assertFalse(view.isTouching)
        touch(view, MotionEvent.ACTION_MOVE, 250, x = 31f, y = 0f); idle(32)
        assertEquals(.5f, app.brightness.fraction(), .012f)
        app.configuration.update { it.copy(handles = emptyList()) }
        await { !host(service).visible }
    }
    @Test fun travelGuideExistsOnlyInTheEditorAndDoesNotWriteValues() {
        val (service, _) = fixture()
        val before = app.brightness.fraction()
        val token = Any(); val editor = Any()
        val guide = ReflectionHelpers.getField<SliderGuideHost>(service, "sliderGuideHost")
        fun guideView() = ReflectionHelpers.getField<android.view.View?>(guide, "view")
        app.sliderGuidePreview.begin(token, SliderGuideRequest("slider", SliderKind.BRIGHTNESS, 600f))
        idle(32); assertNull(guideView())
        app.editor.begin(editor, EditorSelection(handleId = "slider"))
        await { guideView() != null }
        val flags = (guideView()!!.layoutParams as android.view.WindowManager.LayoutParams).flags
        assertTrue(flags and android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE != 0)
        assertEquals(before, app.brightness.fraction(), .001f)
        app.editor.end(editor); await { guideView() == null }
        app.sliderGuidePreview.end(token)
        assertEquals(before, app.brightness.fraction(), .001f)
    }
    @Test fun releaseKeepsTheSwipeSequenceResetAfterItsPanelCloses() {
        val (service, view) = fixture()
        val panel = SavedPanel(settings = PanelConfig(autoCloseEnabled = true, autoCloseMs = 1000))
        val target = GestureTarget.panel(panel.id).copy(brightness = true, slideToAdjust = true)
        app.configuration.update { it.copy(panels = listOf(panel)).editHandle("slider") { h -> h.copy(bindings = mapOf(
            GestureSlot.SWIPE_1 to target, GestureSlot.SWIPE_2 to GestureTarget.VOLUME)) } }
        await { app.configuration.state.value?.handle("slider")?.target(GestureSlot.SWIPE_1) == target }
        touch(view, MotionEvent.ACTION_DOWN, 100); touch(view, MotionEvent.ACTION_MOVE, 200, x = 31f)
        assertNotNull(ReflectionHelpers.getField<Any?>(service, "open"))
        idle(1500)
        assertNull(ReflectionHelpers.getField<Any?>(service, "open")); assertTrue(view.isTouching)
        assertTrue(host(service).visible)
        touch(view, MotionEvent.ACTION_UP, 1800, x = 31f)
        val sessions = ReflectionHelpers.getField<GestureSessions>(service, "sessions")
        assertEquals(0, sessions.next(app.configuration.state.value!!.handle("slider")!!, GestureKind.SWIPE, android.os.SystemClock.elapsedRealtime()).second)
    }
    @Test fun releaseDoesNotRestoreASwipeStageForAClosePanelsAction() {
        val (service, view) = fixture()
        val target = GestureTarget(brightness = true, slideToAdjust = true, action = PanelButton(action = ButtonAction.CLOSE_PANELS))
        app.configuration.update { it.editHandle("slider") { h -> h.copy(bindings = mapOf(
            GestureSlot.SWIPE_1 to target, GestureSlot.SWIPE_2 to GestureTarget.VOLUME)) } }
        await { app.configuration.state.value?.handle("slider")?.target(GestureSlot.SWIPE_1) == target }
        touch(view, MotionEvent.ACTION_DOWN, 100); touch(view, MotionEvent.ACTION_MOVE, 200, x = 31f)
        assertTrue(host(service).visible)
        touch(view, MotionEvent.ACTION_UP, 300, x = 31f)
        val sessions = ReflectionHelpers.getField<GestureSessions>(service, "sessions")
        assertEquals(0, sessions.next(app.configuration.state.value!!.handle("slider")!!, GestureKind.SWIPE, android.os.SystemClock.elapsedRealtime()).second)
    }
}
