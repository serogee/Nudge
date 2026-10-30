package com.modukey.app.platform

import android.app.KeyguardManager
import android.os.Looper
import android.os.PowerManager
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
import org.robolectric.util.ReflectionHelpers.ClassParameter
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36])
class HandlePauseRuntimeTest {
    private val app get() = ApplicationProvider.getApplicationContext<NudgeApplication>()
    private var destroy: () -> Unit = {}
    @After fun cleanup() { destroy(); app.onTerminate() }
    private fun await(condition: () -> Boolean) {
        repeat(1_000) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(16)); if (condition()) return; Thread.sleep(10) }
        assertTrue("${app.diagnostics.details.value}\nConfiguration error: ${app.configuration.error.value}", condition())
    }
    private class Host : PanelHost {
        var spec: PanelSpec? = null
        override fun show(spec: PanelSpec) { this.spec = spec }
        override fun hide(animate: Boolean) { spec = null }
        override fun refreshSound() = Unit
    }
    private data class Fixture(val service: EdgeAccessibilityService, val owner: HandleConfig, val panel: SavedPanel, val host: Host)
    private fun fixture(scope: HandleToggleScope = HandleToggleScope.ALL, savedPause: Boolean = false): Fixture {
        val controller = Robolectric.buildService(EdgeAccessibilityService::class.java).create()
        val service = controller.get(); destroy = { controller.destroy() }
        shadowOf(app).grantPermissions("${app.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
        shadowOf(service.getSystemService(PowerManager::class.java)).setIsInteractive(true)
        shadowOf(service.getSystemService(KeyguardManager::class.java)).setKeyguardLocked(false)
        await { app.configuration.state.value != null }
        val toggle = PanelButton(action = ButtonAction.TOGGLE_HANDLES, handleScope = scope)
        val panel = SavedPanel(settings = PanelConfig(buttons = listOf(toggle)))
        val owner = HandleConfig(enabled = true, bindings = mapOf(GestureSlot.SWIPE_1 to GestureTarget.panel(panel.id),
            GestureSlot.HOLD to GestureTarget.direct(toggle.copy(id = newId()))))
        app.configuration.update { AppConfiguration(enabled = true, handles = listOf(owner, HandleConfig(id = "other", enabled = true)), panels = listOf(panel),
            pausedHandleIds = if (savedPause) setOf("other") else emptySet()) }
        ReflectionHelpers.callInstanceMethod<Void>(service, "onServiceConnected")
        await { app.status.state.value.attachedHandleIds == if (savedPause) setOf(owner.id) else setOf(owner.id, "other") }
        ReflectionHelpers.getField<PanelHost>(service, "panelHost").hide(false)
        val host = Host(); ReflectionHelpers.setField(service, "panelHost", host)
        return Fixture(service, owner, panel, host)
    }
    private fun gesture(f: Fixture, kind: GestureKind = GestureKind.SWIPE) = ReflectionHelpers.callInstanceMethod<Void>(f.service, "activate",
        ClassParameter.from(String::class.java, f.owner.id), ClassParameter.from(GestureKind::class.java, kind))
    private fun button(f: Fixture) = ReflectionHelpers.callInstanceMethod<Void>(f.service, "runButton",
        ClassParameter.from(PanelButton::class.java, f.panel.settings.buttons.single()))

    @Test fun savedPausePreventsAttachmentAndSurvivesMasterAndEnableChanges() {
        val f=fixture(savedPause=true)
        assertEquals(setOf("other"),app.handlePauses.state.value)
        app.configuration.update { it.copy(enabled=false).editHandle("other") { h -> h.copy(enabled=false) } }
        await { app.status.state.value.attachedHandleIds.isEmpty() }
        assertEquals(setOf("other"),app.handlePauses.state.value)
        app.configuration.update { it.copy(enabled=true).editHandle("other") { h -> h.copy(enabled=true) } }
        await { app.status.state.value.attachedHandleIds == setOf(f.owner.id) }
        f.service.onUnbind(null)
        ReflectionHelpers.callInstanceMethod<Void>(f.service,"onServiceConnected")
        await { app.status.state.value.attachedHandleIds == setOf(f.owner.id) }
        assertEquals(setOf("other"),app.configuration.state.value!!.pausedHandleIds)
        app.configuration.update { it.copy(handles=it.handles.filterNot { h -> h.id == "other" }) }
        assertTrue(app.handlePauses.state.value.isEmpty())
    }
    @Test fun combinedButtonRepairsMismatchesAndRestoresBothGroupsTogether() {
        val f=fixture()
        val combined=PanelButton(action=ButtonAction.TOGGLE_AND_HIDE_HANDLES,toggleScope=HandleGroupScope.OTHERS,visibilityScope=HandleGroupScope.ALL)
        val actions=PanelActions(f.service)
        app.configuration.update { it.hideIndicators(true) }
        assertTrue(actions.execute(f.owner.id,combined,false).succeeded)
        assertEquals(setOf("other"),app.configuration.state.value!!.pausedHandleIds)
        assertTrue(app.configuration.state.value!!.hideHandleIndicators)
        assertTrue(actions.execute(f.owner.id, PanelButton(action = ButtonAction.TOGGLE_HANDLE_VISIBILITY), false).succeeded)
        assertTrue(app.configuration.state.value!!.hiddenIndicatorIds.isEmpty())
        assertEquals(setOf("other"), app.configuration.state.value!!.pausedHandleIds)
        assertEquals(setOf("other"), actions.snapshot().pausedHandleIds)
        assertTrue(actions.execute(f.owner.id,combined,false).succeeded)
        assertTrue(app.configuration.state.value!!.pausedHandleIds.isEmpty())
        assertTrue(app.configuration.state.value!!.hiddenIndicatorIds.isEmpty())
        assertTrue(actions.execute(f.owner.id,combined,false).succeeded)
        assertEquals(setOf("other"),app.configuration.state.value!!.pausedHandleIds)
        assertTrue(app.configuration.state.value!!.hideHandleIndicators)
    }

    @Test fun combinedButtonKeepsItsPanelUsableAndTogglesSeparateGroups() {
        val f = fixture()
        val combined = f.panel.settings.buttons.single().copy(action = ButtonAction.TOGGLE_AND_HIDE_HANDLES,
            toggleScope = HandleGroupScope.CURRENT, visibilityScope = HandleGroupScope.OTHERS)
        app.configuration.update { it.editPanel(f.panel.id) { p -> p.copy(settings = p.settings.copy(buttons = listOf(combined))) } }
        gesture(f); await { app.status.state.value.panelId == f.panel.id }
        fun press() = ReflectionHelpers.callInstanceMethod<Void>(f.service, "runButton", ClassParameter.from(PanelButton::class.java, combined))
        press()
        await { app.handlePauses.state.value == setOf(f.owner.id) && app.configuration.state.value!!.hiddenIndicatorIds == setOf("other") }
        assertEquals(f.panel.id, app.status.state.value.panelId)
        press()
        await { app.handlePauses.state.value.isEmpty() && app.configuration.state.value!!.hiddenIndicatorIds.isEmpty() }
        assertEquals(f.panel.id, app.status.state.value.panelId)
        val invalid = combined.copy(toggleScope = HandleGroupScope.CURRENT, visibilityScope = HandleGroupScope.ALL)
        assertFalse(PanelActions(f.service).execute("", invalid, false).succeeded)
        assertTrue(app.configuration.state.value!!.hiddenIndicatorIds.isEmpty()); assertTrue(app.handlePauses.state.value.isEmpty())
    }

    @Test fun visibilityActionKeepsWindowsAndGesturesWhilePreservingSavedAppearance() {
        val f = fixture()
        app.configuration.update { config -> config.copy(handles = config.handles.map { h ->
            h.copy(trigger = h.trigger.copy(indicatorVisible = true), bindings = h.bindings + (GestureSlot.TAP to GestureTarget.panel(f.panel.id)))
        }) }
        val views = ReflectionHelpers.getField<MutableMap<String, HandleTriggerView>>(f.service, "views")
        val original = views.toMap()
        val saved = app.configuration.state.value!!.handles
        val actions = PanelActions(f.service)
        val toggle = PanelButton(action = ButtonAction.TOGGLE_HANDLE_VISIBILITY)
        assertFalse(actions.execute(f.owner.id, toggle, false).close)
        await { views.values.all { ReflectionHelpers.getField<Boolean>(it, "hideIndicator") } }
        assertTrue(app.configuration.state.value!!.hideHandleIndicators)
        assertEquals(saved.map { it.id }.toSet(), actions.snapshot().hiddenIndicatorIds)
        assertEquals(saved.map { it.copy(indicatorHidden = true) }, app.configuration.state.value!!.handles)
        assertEquals(original.keys, views.keys)
        original.forEach { (id, view) -> assertSame(view, views[id]) }
        views.getValue(f.owner.id).performClick()
        await { app.status.state.value.panelId == f.panel.id }
        actions.execute(f.owner.id, toggle, false)
        await { views.values.none { ReflectionHelpers.getField<Boolean>(it, "hideIndicator") } }
        assertFalse(app.configuration.state.value!!.hideHandleIndicators)
        assertEquals(saved, app.configuration.state.value!!.handles)
        assertEquals(f.panel.id, app.status.state.value.panelId)
    }

    @Test fun `an open panel can resume all paused handles and saved disabling still closes it`() {
        val f = fixture(); gesture(f)
        assertEquals(f.owner.id, f.host.spec!!.handleId)
        button(f); await { app.status.state.value.attachedHandleIds.isEmpty() }
        assertEquals(f.panel.id, app.status.state.value.panelId); assertNotNull(f.host.spec)
        assertTrue(app.configuration.state.value!!.handles.all { it.enabled })
        assertEquals(HandleAvailability.PAUSED, app.status.state.value.handles[f.owner.id])
        button(f); await { app.status.state.value.attachedHandleIds.size == 2 }
        assertEquals(f.panel.id, app.status.state.value.panelId)
        button(f); await { app.status.state.value.attachedHandleIds.isEmpty() }
        app.configuration.update { it.editHandle(f.owner.id) { h -> h.copy(enabled = false) } }
        await { app.status.state.value.panelId == null }; assertNull(f.host.spec)
    }
    @Test fun `direct actions can pause other handles without disabling their saved switches`() {
        val f = fixture(HandleToggleScope.OTHERS)
        gesture(f, GestureKind.HOLD); await { app.status.state.value.attachedHandleIds == setOf(f.owner.id) }
        assertEquals(setOf("other"), app.handlePauses.state.value)
        gesture(f, GestureKind.HOLD); await { app.status.state.value.attachedHandleIds.size == 2 }
        app.handlePauses.toggle(HandleToggleScope.ALL, f.owner.id, app.configuration.state.value!!)
        await { app.status.state.value.attachedHandleIds.isEmpty() }
        assertEquals(0, f.service.serviceInfo?.eventTypes ?: 0)
        app.handlePauses.clear(); await { app.status.state.value.attachedHandleIds.size == 2 }
    }
}
