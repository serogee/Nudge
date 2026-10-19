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
        repeat(300) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(16)); if (condition()) return; Thread.sleep(5) }
        assertTrue(app.diagnostics.details.value, condition())
    }
    private class Host : PanelHost {
        var spec: PanelSpec? = null
        override fun show(spec: PanelSpec) { this.spec = spec }
        override fun hide(animate: Boolean) { spec = null }
        override fun refreshSound() = Unit
    }
    private data class Fixture(val service: EdgeAccessibilityService, val owner: HandleConfig, val panel: SavedPanel, val host: Host)
    private fun fixture(scope: HandleToggleScope = HandleToggleScope.ALL): Fixture {
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
        app.configuration.update { AppConfiguration(enabled = true, handles = listOf(owner, HandleConfig(id = "other", enabled = true)), panels = listOf(panel)) }
        ReflectionHelpers.callInstanceMethod<Void>(service, "onServiceConnected")
        await { app.status.state.value.attachedHandleIds.size == 2 }
        ReflectionHelpers.getField<PanelHost>(service, "panelHost").hide(false)
        val host = Host(); ReflectionHelpers.setField(service, "panelHost", host)
        return Fixture(service, owner, panel, host)
    }
    private fun gesture(f: Fixture, kind: GestureKind = GestureKind.SWIPE) = ReflectionHelpers.callInstanceMethod<Void>(f.service, "activate",
        ClassParameter.from(String::class.java, f.owner.id), ClassParameter.from(GestureKind::class.java, kind))
    private fun button(f: Fixture) = ReflectionHelpers.callInstanceMethod<Void>(f.service, "runButton",
        ClassParameter.from(PanelButton::class.java, f.panel.settings.buttons.single()))

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
