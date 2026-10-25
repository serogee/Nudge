package com.modukey.app.platform

import android.app.KeyguardManager
import android.content.Intent
import android.os.Looper
import android.os.PowerManager
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
@Config(sdk = [31, 36], qualifiers = "mdpi")
@Suppress("DEPRECATION")
class WheelGestureRuntimeTest {
    private val app get() = ApplicationProvider.getApplicationContext<NudgeApplication>()
    private var destroy: () -> Unit = {}
    @After fun cleanup() { destroy(); app.onTerminate() }
    private fun idle(ms: Long = 32) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
    private fun await(condition: () -> Boolean) {
        repeat(300) { idle(16); if (condition()) return; Thread.sleep(5) }
        assertTrue(app.diagnostics.details.value, condition())
    }
    private class Host : PanelHost {
        var spec: PanelSpec? = null
        override fun show(spec: PanelSpec) { this.spec = spec }
        override fun hide(animate: Boolean) { spec = null }
        override fun refreshSound() = Unit
    }
    private data class Fixture(val service: EdgeAccessibilityService, val view: HandleTriggerView, val host: Host,
        val dismiss: (PanelDismissReason) -> Unit)
    private fun fixture(allowLater: Boolean = false, slot: GestureSlot = GestureSlot.SWIPE_1, drag: Boolean = true,
        hitbox: WheelHitboxSettings = WheelHitboxSettings(), innerRadius: Float? = null): Fixture {
        val controller = Robolectric.buildService(EdgeAccessibilityService::class.java).create(); destroy = { controller.destroy() }
        val service = controller.get()
        shadowOf(app).grantPermissions("${app.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
        shadowOf(app.getSystemService(PowerManager::class.java)).setIsInteractive(true)
        shadowOf(app.getSystemService(KeyguardManager::class.java)).setKeyguardLocked(false)
        await { app.configuration.state.value != null }
        val panel = SavedPanel(id = "wheel", settings = PanelConfig(buttons = listOf(PanelButton(action = ButtonAction.TOGGLE_HANDLE_VISIBILITY)),
            portrait = PanelLayout(mode = PanelMode.WHEEL, wheel = WheelLayoutSettings(hitbox = hitbox, innerRadiusDp = innerRadius)), autoCloseEnabled = true, autoCloseMs = 1000))
        val target = GestureTarget.panel(panel.id).copy(wheelPick = WheelPickSettings(drag, allowLaterSwipes = allowLater))
        val handle = HandleConfig(id = "handle", enabled = true, trigger = TriggerSettings(portrait = Placement(Edge.LEFT), landscapeEnabled = true),
            bindings = mapOf(slot to target, GestureSlot.SWIPE_2 to GestureTarget.VOLUME))
        app.configuration.update { AppConfiguration(enabled = true, handles = listOf(handle), panels = listOf(panel)) }
        ReflectionHelpers.callInstanceMethod<Void>(service, "onServiceConnected")
        await { app.status.state.value.attachedHandleIds.contains(handle.id) }
        val originalHost = ReflectionHelpers.getField<AccessibilityPanelHost>(service, "panelHost")
        val dismiss = ReflectionHelpers.getField<(PanelDismissReason) -> Unit>(originalHost, "onDismiss")
        val host = Host(); ReflectionHelpers.setField(service, "panelHost", host)
        val view = ReflectionHelpers.getField<Map<String, HandleTriggerView>>(service, "views").getValue(handle.id)
        return Fixture(service, view, host, dismiss)
    }
    private fun touch(view: HandleTriggerView, action: Int, x: Float = 1f, y: Float = 200f) {
        val now = android.os.SystemClock.uptimeMillis()
        val event = MotionEvent.obtain(now, now, action, x, y, 0)
        try { assertTrue(view.onTouchEvent(event)) } finally { event.recycle() }
    }
    private fun open(f: Fixture) { touch(f.view, MotionEvent.ACTION_DOWN); idle(20); touch(f.view, MotionEvent.ACTION_MOVE, 31f); idle() }
    private fun stage(f: Fixture) = ReflectionHelpers.getField<GestureSessions>(f.service, "sessions")
        .next(app.configuration.state.value!!.handle("handle")!!.effectiveGestures(app.configuration.state.value!!, false), GestureKind.SWIPE,
            android.os.SystemClock.elapsedRealtime()).second
    @Test fun releaseExecutesOnceAndClosesBeforeTheAction() {
        val f = fixture(); open(f)
        assertTrue(f.host.spec!!.wheelDragging); assertNull(f.host.spec!!.wheelSelection)
        idle(1500); assertNotNull(f.host.spec)
        val p = f.host.spec!!.geometry.wheel!!.slices().single().point
        touch(f.view, MotionEvent.ACTION_MOVE, p.x, p.y); idle()
        assertEquals(0, f.host.spec!!.wheelSelection)
        touch(f.view, MotionEvent.ACTION_UP, p.x, p.y); idle()
        assertNull(f.host.spec)
        assertTrue(app.configuration.state.value!!.handle("handle")!!.indicatorHidden)
        touch(f.view, MotionEvent.ACTION_UP, p.x, p.y); idle()
        assertTrue(app.configuration.state.value!!.handle("handle")!!.indicatorHidden)
        assertEquals(0, stage(f))
    }
    @Test fun manualHitboxesCanPickInsideTheVisibleHole() {
        val f = fixture(hitbox = WheelHitboxSettings(WheelHitboxMode.MANUAL, 16f, 80f), innerRadius = 100f); open(f)
        val g = f.host.spec!!.geometry.wheel!!
        val p = g.point(0f, 40f)
        assertNull(g.tapHit(p.x, p.y))
        touch(f.view, MotionEvent.ACTION_MOVE, p.x, p.y); idle()
        assertEquals(0, f.host.spec!!.wheelSelection)
        touch(f.view, MotionEvent.ACTION_UP, p.x, p.y); idle()
        assertTrue(app.configuration.state.value!!.handle("handle")!!.indicatorHidden)
        assertNull(f.host.spec)
    }
    @Test fun buttonSizeRejectsTheInnerMarginAndRechecksOuterBoundsOnRelease() {
        val f = fixture(hitbox = WheelHitboxSettings(WheelHitboxMode.BUTTON_SIZE)); open(f)
        val g = f.host.spec!!.geometry.wheel!!
        val near = g.point(0f, g.innerRadius - 1f)
        touch(f.view, MotionEvent.ACTION_MOVE, near.x, near.y); idle()
        assertNull(f.host.spec!!.wheelSelection)
        val p = g.slices().single().point
        touch(f.view, MotionEvent.ACTION_MOVE, p.x, p.y); idle()
        assertEquals(0, f.host.spec!!.wheelSelection)
        val far = g.point(0f, g.slices(0).single().outerRadius + 1f)
        touch(f.view, MotionEvent.ACTION_UP, far.x, far.y); idle()
        assertFalse(app.configuration.state.value!!.handle("handle")!!.indicatorHidden)
        assertNull(f.host.spec)
    }
    @Test fun earlyReleasePreservesNextStageAndTheSequenceTimeout() {
        val f = fixture(allowLater = true); open(f)
        touch(f.view, MotionEvent.ACTION_UP, 200f); idle()
        assertNull(f.host.spec); assertEquals(1, stage(f))
        assertFalse(app.configuration.state.value!!.handle("handle")!!.indicatorHidden)
        idle(3001); assertEquals(0, stage(f))
    }
    @Test fun holdWindowRequiresFreshMovementAndCannotActivateFromRelease() {
        val f = fixture(allowLater = true); open(f)
        val p = f.host.spec!!.geometry.wheel!!.slices().single().point
        touch(f.view, MotionEvent.ACTION_MOVE, p.x, p.y)
        idle(200); assertNull(f.host.spec!!.wheelSelection)
        touch(f.view, MotionEvent.ACTION_UP, p.x, p.y); idle()
        assertFalse(app.configuration.state.value!!.handle("handle")!!.indicatorHidden)
        assertNull(f.host.spec); assertEquals(0, stage(f))
    }
    @Test fun outsideDistanceUnhighlightsAndReentryRestoresSelection() {
        val f = fixture(); open(f)
        val g = f.host.spec!!.geometry.wheel!!; val p = g.slices().single().point
        touch(f.view, MotionEvent.ACTION_MOVE, p.x, p.y); idle(); assertEquals(0, f.host.spec!!.wheelSelection)
        val far = g.point(0f, 300f)
        touch(f.view, MotionEvent.ACTION_MOVE, far.x, far.y); idle(); assertNull(f.host.spec!!.wheelSelection)
        touch(f.view, MotionEvent.ACTION_MOVE, p.x, p.y); idle(); assertEquals(0, f.host.spec!!.wheelSelection)
        touch(f.view, MotionEvent.ACTION_UP, far.x, far.y); idle()
        assertFalse(app.configuration.state.value!!.handle("handle")!!.indicatorHidden)
        assertNull(f.host.spec)
    }
    @Test fun cancellationMultitouchRotationAndConfigurationChangesNeverExecute() {
        val f = fixture()
        for (ending in 0..3) {
            open(f); val p = f.host.spec!!.geometry.wheel!!.slices().single().point
            touch(f.view, MotionEvent.ACTION_MOVE, p.x, p.y); idle()
            when (ending) {
                0 -> touch(f.view, MotionEvent.ACTION_CANCEL, p.x, p.y)
                1 -> touch(f.view, MotionEvent.ACTION_POINTER_DOWN, p.x, p.y)
                2 -> app.configuration.update { it.editPanel("wheel") { panel -> panel.copy(name = "Changed") } }
                else -> ReflectionHelpers.callInstanceMethod<Void>(f.service, "rebuildHandles")
            }
            idle(); assertNull(f.host.spec)
            if (ending != 3) touch(f.view, MotionEvent.ACTION_UP, p.x, p.y)
            idle()
            assertFalse(app.configuration.state.value!!.handle("handle")!!.indicatorHidden)
        }
    }
    @Test fun holdAndSwipeHoldOpenAtTheirDeadlinesWithoutChangingSavedTiming() {
        val f = fixture(slot = GestureSlot.HOLD)
        touch(f.view, MotionEvent.ACTION_DOWN); idle(500)
        assertTrue(f.host.spec!!.wheelDragging)
        assertEquals(HoldExecution.ON_RELEASE, app.configuration.state.value!!.handle("handle")!!.trigger.holdExecution)
        touch(f.view, MotionEvent.ACTION_CANCEL); idle(); assertNull(f.host.spec)
    }
    @Test fun backScreenOffInterruptionAndServiceLossCancelHighlightedPicks() {
        val f = fixture()
        for (ending in 0..3) {
            open(f)
            val p = f.host.spec!!.geometry.wheel!!.slices().single().point
            touch(f.view, MotionEvent.ACTION_MOVE, p.x, p.y); idle()
            assertEquals(0, f.host.spec!!.wheelSelection)
            when (ending) {
                0 -> f.dismiss(PanelDismissReason.BACK)
                1 -> app.sendBroadcast(Intent(Intent.ACTION_SCREEN_OFF))
                2 -> f.service.onInterrupt()
                else -> f.service.onUnbind(Intent())
            }
            idle()
            assertNull(f.host.spec)
            assertNull(ReflectionHelpers.getField<Any?>(f.service, "wheelBinding"))
            touch(f.view, MotionEvent.ACTION_UP, p.x, p.y); idle()
            assertFalse(app.configuration.state.value!!.handle("handle")!!.indicatorHidden)
        }
    }
    @Test fun swipeHoldCapturesAtItsDeadlineAndKeepsReleaseExecutionSaved() {
        val f = fixture(slot = GestureSlot.SWIPE_HOLD)
        touch(f.view, MotionEvent.ACTION_DOWN); idle(20); touch(f.view, MotionEvent.ACTION_MOVE, 31f)
        assertNull(f.host.spec); idle(500)
        assertTrue(f.host.spec!!.wheelDragging)
        assertEquals(HoldExecution.ON_RELEASE, app.configuration.state.value!!.handle("handle")!!.trigger.swipeHoldExecution)
        touch(f.view, MotionEvent.ACTION_CANCEL); idle(); assertNull(f.host.spec)
    }
    @Test fun pickedActionsKeepPanelUnlockContextAndSeeNoOpenWheel() {
        val f = fixture(); var executions = 0
        ReflectionHelpers.setField(f.service, "actions", object : ActionExecutor {
            override fun snapshot() = SoundState()
            override fun execute(handleId: String, button: PanelButton, testing: Boolean, gestureSlot: GestureSlot?): ActionResult {
                executions++
                assertEquals("handle", handleId); assertNull(gestureSlot); assertNull(f.host.spec)
                assertNull(ReflectionHelpers.getField<Any?>(f.service, "open"))
                return ActionResult()
            }
        })
        open(f); val p = f.host.spec!!.geometry.wheel!!.slices().single().point
        touch(f.view, MotionEvent.ACTION_MOVE, p.x, p.y)
        touch(f.view, MotionEvent.ACTION_UP, p.x, p.y); idle()
        assertEquals(1, executions)
    }
    @Test fun normalWheelOpensOnReleaseAndKeepsNormalButtonActions() {
        val f = fixture(drag = false); open(f); assertNull(f.host.spec)
        touch(f.view, MotionEvent.ACTION_UP, 31f); idle()
        assertFalse(f.host.spec!!.wheelDragging)
        ReflectionHelpers.callInstanceMethod<Void>(f.service, "runButton", ReflectionHelpers.ClassParameter.from(PanelButton::class.java,
            app.configuration.state.value!!.panel("wheel")!!.settings.buttons.single()))
        idle(); assertTrue(app.configuration.state.value!!.handle("handle")!!.indicatorHidden)
        assertNotNull(f.host.spec)
    }
}
