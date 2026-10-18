package com.modukey.app.platform

import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
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
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowAccessibilityService
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36], shadows = [PanelDismissalRuntimeTest.ServiceShadow::class])
@Suppress("DEPRECATION")
class PanelDismissalRuntimeTest {
    @Implements(AccessibilityService::class)
    class ServiceShadow : ShadowAccessibilityService() {
        private var info = AccessibilityServiceInfo()
        @Implementation fun getServiceInfo(): AccessibilityServiceInfo = info
        @Implementation fun setServiceInfo(value: AccessibilityServiceInfo) { info = value }
    }
    private val app get() = ApplicationProvider.getApplicationContext<NudgeApplication>()
    private var destroy: () -> Unit = {}
    @After fun cleanup() { destroy(); app.onTerminate() }
    private fun idle(ms: Long = 32) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
    private fun await(condition: () -> Boolean) {
        repeat(300) { idle(16); if (condition()) return; Thread.sleep(5) }
        assertTrue(app.diagnostics.details.value, condition())
    }
    private class Host : PanelHost {
        var spec: PanelSpec? = null; var soundUpdates = 0
        override fun show(spec: PanelSpec) { this.spec = spec }
        override fun hide(animate: Boolean) { spec = null }
        override fun refreshSound() { soundUpdates++ }
    }
    private data class Fixture(val service: EdgeAccessibilityService, val handle: HandleConfig, val panel: SavedPanel, val host: Host,
        val close: (PanelDismissReason) -> Unit)
    private fun fixture(timed: Boolean = false): Fixture {
        val controller = Robolectric.buildService(EdgeAccessibilityService::class.java).create()
        val service = controller.get(); destroy = { controller.destroy() }
        shadowOf(app).grantPermissions("${app.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
        shadowOf(service.getSystemService(PowerManager::class.java)).setIsInteractive(true)
        shadowOf(service.getSystemService(KeyguardManager::class.java)).setKeyguardLocked(false)
        await { app.configuration.state.value != null }
        val buttons = listOf(PanelButton(action = ButtonAction.VOLUME_UP, showVolumePopup = true),
            PanelButton(action = ButtonAction.VOLUME_DOWN), PanelButton(action = ButtonAction.MEDIA_MUTE))
        val panel = SavedPanel(settings = PanelConfig(buttons = buttons, autoCloseEnabled = timed, autoCloseMs = 1_000))
        val second = SavedPanel()
        val handle = HandleConfig(enabled = true, trigger = TriggerSettings(landscapeEnabled = true), bindings = mapOf(
            GestureSlot.SWIPE_1 to GestureTarget.panel(panel.id, true), GestureSlot.SWIPE_2 to GestureTarget.panel(second.id)))
        app.configuration.update { AppConfiguration(enabled = true, handles = listOf(handle), panels = listOf(panel, second)) }
        service.serviceInfo = AccessibilityServiceInfo()
        ReflectionHelpers.callInstanceMethod<Void>(service, "onServiceConnected")
        await { app.status.state.value.attachedHandleIds == setOf(handle.id) }
        val original = ReflectionHelpers.getField<AccessibilityPanelHost>(service, "panelHost")
        val close = ReflectionHelpers.getField<(PanelDismissReason) -> Unit>(original, "onDismiss")
        val host = Host(); ReflectionHelpers.setField(service, "panelHost", host)
        activity("test.browser"); activity("test.camera"); activity("test.volume", launchable = false)
        return Fixture(service, handle, panel, host, close)
    }
    private fun activity(pkg: String, name: String = "$pkg.Main", launchable: Boolean = true) {
        val info = ActivityInfo().apply {
            packageName = pkg; this.name = name; enabled = true
            applicationInfo = ApplicationInfo().apply { packageName = pkg; enabled = true }
        }
        shadowOf(app.packageManager).addOrUpdateActivity(info)
        if (launchable) shadowOf(app.packageManager).addResolveInfoForIntent(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(pkg), ResolveInfo().apply { activityInfo = info })
    }
    private fun window(f: Fixture, pkg: String, name: String = "$pkg.Main", time: Long = SystemClock.uptimeMillis(), changes: Int = 0) {
        val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED).apply {
            packageName = pkg; className = name; eventTime = time; contentChangeTypes = changes
            text.add("PRIVATE SCREEN TEXT")
        }
        try { f.service.onAccessibilityEvent(event) } finally { event.recycle() }
        idle()
    }
    private fun swipe(f: Fixture) = ReflectionHelpers.callInstanceMethod<Void>(f.service, "activate",
        ClassParameter.from(String::class.java, f.handle.id), ClassParameter.from(GestureKind::class.java, GestureKind.SWIPE))

    @Test fun `first observed activity preserves the panel and real app switch closes it`() {
        val f = fixture(); swipe(f)
        window(f, "test.browser")
        assertEquals(f.panel.id, app.status.state.value.panelId)
        window(f, "test.camera")
        assertNull(app.status.state.value.panelId); assertNull(f.host.spec)
        swipe(f); assertEquals(f.panel.id, app.status.state.value.panelId)
        val log = app.diagnostics.details.value
        assertTrue(log, log.contains("reason=app_changed"))
        assertTrue(log, log.contains("previousPackage=test.browser"))
        assertTrue(log, log.contains("incomingPackage=test.camera"))
        assertTrue(log, log.contains("eventClass=test.camera.Main"))
        assertTrue(log, log.contains("ageMs=")); assertFalse(log.contains("PRIVATE SCREEN TEXT"))
    }
    @Test fun `track activities before opening panels and stop unneeded events`() {
        val f = fixture()
        assertEquals(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, f.service.serviceInfo.eventTypes)
        window(f, "test.browser"); swipe(f); window(f, "test.camera")
        assertNull(app.status.state.value.panelId)
        app.configuration.update { it.editHandle(f.handle.id) { h -> h.copy(bindings = mapOf(GestureSlot.SWIPE_1 to GestureTarget.VOLUME)) } }
        await { f.service.serviceInfo.eventTypes == 0 }
    }
    @Test fun `popup dialog pane own overlay and queued activity events keep panels open`() {
        val f = fixture(); window(f, "test.browser")
        val stale = SystemClock.uptimeMillis(); idle(100); swipe(f)
        window(f, "test.camera", time = stale)
        assertEquals(f.panel.id, app.status.state.value.panelId)
        for ((pkg, name) in listOf("test.volume" to "test.volume.Main", "test.camera" to "android.app.Dialog",
            "test.unknown" to "test.unknown.Main", "com.android.systemui" to "com.android.systemui.volume.VolumeDialog",
            "android" to "android.widget.FrameLayout", app.packageName to PanelRoot::class.java.name)) {
            window(f, pkg, name)
            assertEquals("$pkg/$name", f.panel.id, app.status.state.value.panelId)
        }
        window(f, "test.camera", changes = AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_APPEARED)
        assertEquals(f.panel.id, app.status.state.value.panelId)
        window(f, "test.browser"); assertEquals(f.panel.id, app.status.state.value.panelId)
        assertFalse(app.diagnostics.details.value.contains("Panel dismissed"))
    }
    @Test fun `sound actions and refresh do not close panels or extend timeout without interaction`() {
        val f = fixture(timed = true); window(f, "test.browser"); swipe(f)
        f.panel.settings.buttons.forEach { button ->
            ReflectionHelpers.callInstanceMethod<Void>(f.service, "runButton", ClassParameter.from(PanelButton::class.java, button))
            window(f, "test.volume")
            assertEquals(f.panel.id, app.status.state.value.panelId)
        }
        assertEquals(3, f.host.soundUpdates)
        idle(500); f.service.onInterrupt(); window(f, "test.browser")
        assertEquals(f.panel.id, app.status.state.value.panelId)
        idle(600)
        assertNull(app.status.state.value.panelId)
        assertTrue(app.diagnostics.details.value.contains("reason=timeout"))
        swipe(f); assertEquals(f.panel.id, app.status.state.value.panelId)
    }
    @Test fun `Back outside touch screen off and service loss have distinct bounded diagnostics`() {
        val f = fixture(); window(f, "test.browser")
        for (reason in listOf(PanelDismissReason.BACK, PanelDismissReason.OUTSIDE_TOUCH, PanelDismissReason.WINDOW_DETACHED)) {
            swipe(f); f.close(reason)
            assertNull(app.status.state.value.panelId)
            assertTrue(app.diagnostics.details.value.contains("reason=${reason.name.lowercase()}"))
        }
        swipe(f)
        val receiver = ReflectionHelpers.getField<android.content.BroadcastReceiver>(f.service, "screenReceiver")
        shadowOf(f.service.getSystemService(PowerManager::class.java)).setIsInteractive(false)
        receiver.onReceive(f.service, Intent(Intent.ACTION_SCREEN_OFF))
        assertTrue(app.diagnostics.details.value.contains("reason=screen_state"))
        shadowOf(f.service.getSystemService(PowerManager::class.java)).setIsInteractive(true)
        idle(); swipe(f); destroy(); destroy = {}
        val log = app.diagnostics.details.value
        assertTrue(log.contains("reason=service_stopped"))
        assertTrue(log.length <= 32_768); assertTrue(log.lines().size <= 64)
    }
}
