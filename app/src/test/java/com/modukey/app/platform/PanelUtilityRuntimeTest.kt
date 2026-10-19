package com.modukey.app.platform

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.os.Looper
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import com.modukey.app.NudgeApplication
import com.modukey.app.R
import com.modukey.app.core.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowAccessibilityService
import org.robolectric.shadows.ShadowToast
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36], shadows = [PanelUtilityRuntimeTest.Actions::class])
@Suppress("DEPRECATION")
class PanelUtilityRuntimeTest {
    @Implements(AccessibilityService::class)
    class Actions : ShadowAccessibilityService() {
        val requests = mutableListOf<Int>()
        var supported = true
        var beforeCapture: () -> Unit = {}
        @Implementation override fun performGlobalAction(action: Int): Boolean {
            requests += action
            if (action == AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT) beforeCapture()
            return supported
        }
    }
    private val app get() = ApplicationProvider.getApplicationContext<NudgeApplication>()
    private var destroy: () -> Unit = {}
    @After fun cleanup() { destroy(); app.onTerminate() }
    private fun idle(ms: Long = 100) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
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
    private data class Fixture(val service: EdgeAccessibilityService, val owner: HandleConfig, val other: HandleConfig,
        val panel: SavedPanel, val host: Host, val actions: Actions)
    private fun fixture(): Fixture {
        val controller = Robolectric.buildService(EdgeAccessibilityService::class.java).create()
        val service = controller.get(); destroy = { controller.destroy() }
        shadowOf(app).grantPermissions("${app.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
        shadowOf(service.getSystemService(PowerManager::class.java)).setIsInteractive(true)
        shadowOf(service.getSystemService(KeyguardManager::class.java)).setKeyguardLocked(false)
        await { app.configuration.state.value != null }
        val panel = SavedPanel(settings = PanelConfig(buttons = listOf(PanelButton(action = ButtonAction.CLOSE_PANELS), PanelButton(action = ButtonAction.SCREENSHOT))))
        val next = SavedPanel()
        val owner = HandleConfig(enabled = true, bindings = mapOf(GestureSlot.SWIPE_1 to GestureTarget.panel(panel.id),
            GestureSlot.SWIPE_2 to GestureTarget.direct(PanelButton(action = ButtonAction.CLOSE_PANELS)), GestureSlot.SWIPE_3 to GestureTarget.panel(next.id),
            GestureSlot.HOLD to GestureTarget.direct(PanelButton(action = ButtonAction.SCREENSHOT))))
        val other = HandleConfig(enabled = true, bindings = mapOf(GestureSlot.TAP to GestureTarget.direct(PanelButton(action = ButtonAction.CLOSE_PANELS))))
        app.configuration.update { AppConfiguration(enabled = true, handles = listOf(owner, other), panels = listOf(panel, next)) }
        ReflectionHelpers.callInstanceMethod<Void>(service, "onServiceConnected")
        await { app.status.state.value.attachedHandleIds.size == 2 }
        val original = ReflectionHelpers.getField<PanelHost>(service, "panelHost"); original.hide(false)
        val host = Host(); ReflectionHelpers.setField(service, "panelHost", host)
        return Fixture(service, owner, other, panel, host, Shadow.extract(service))
    }
    private fun gesture(f: Fixture, id: String = f.owner.id, kind: GestureKind = GestureKind.SWIPE) {
        ReflectionHelpers.callInstanceMethod<Void>(f.service, "activate", ClassParameter.from(String::class.java, id), ClassParameter.from(GestureKind::class.java, kind))
    }
    private fun button(f: Fixture, action: ButtonAction) = ReflectionHelpers.callInstanceMethod<Void>(f.service, "runButton",
        ClassParameter.from(PanelButton::class.java, f.panel.settings.buttons.single { it.action == action }))

    @Test fun `close works from a button and either handle and resets the owning swipe sequence`() {
        val f = fixture()
        gesture(f); assertEquals(f.panel.id, app.status.state.value.panelId)
        button(f, ButtonAction.CLOSE_PANELS); assertNull(f.host.spec); assertNull(app.status.state.value.panelId)
        gesture(f); assertEquals(f.panel.id, app.status.state.value.panelId)
        gesture(f); assertNull(f.host.spec) // Swipe 2 closes; Swipe 3 must not become the next stage.
        gesture(f); assertEquals(f.panel.id, app.status.state.value.panelId)
        gesture(f, f.other.id, GestureKind.TAP); assertNull(f.host.spec)
        gesture(f); assertEquals(f.panel.id, app.status.state.value.panelId)
        gesture(f, f.other.id, GestureKind.TAP); gesture(f, f.other.id, GestureKind.TAP)
        assertNull(f.host.spec); assertTrue(f.actions.requests.isEmpty())
    }
    @Test fun `panel and direct screenshot actions remove panels before one native request`() {
        val f = fixture()
        f.actions.beforeCapture = { assertNull(f.host.spec); assertNull(app.status.state.value.panelId) }
        gesture(f); button(f, ButtonAction.SCREENSHOT)
        assertNull(f.host.spec); assertTrue(f.actions.requests.isEmpty())
        idle(); assertEquals(listOf(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT), f.actions.requests)
        gesture(f); assertEquals(f.panel.id, app.status.state.value.panelId)
        gesture(f, kind = GestureKind.HOLD)
        assertNull(f.host.spec); assertEquals(1, f.actions.requests.size)
        idle(); assertEquals(2, f.actions.requests.size)
        assertEquals(setOf(f.owner.id, f.other.id), app.status.state.value.attachedHandleIds)
    }
    @Test fun `service loss cancels capture and rejected captures show an error`() {
        val f = fixture()
        f.actions.supported = false
        gesture(f, kind = GestureKind.HOLD); idle()
        assertEquals(app.getString(R.string.screenshot_failed), ShadowToast.getTextOfLatestToast())
        assertEquals(1, f.actions.requests.size)
        f.actions.supported = true
        gesture(f, kind = GestureKind.HOLD)
        f.service.onDestroy(); idle()
        assertEquals(1, f.actions.requests.size)
        assertFalse(app.status.state.value.connected)
    }
}
