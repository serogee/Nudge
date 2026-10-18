package com.modukey.app.platform

import android.app.KeyguardManager
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.os.Looper
import android.os.PowerManager
import android.view.*
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
class RotationPreviewRuntimeTest {
    private val app get() = ApplicationProvider.getApplicationContext<NudgeApplication>()
    private var destroy: () -> Unit = {}
    @After fun cleanup() { destroy(); app.onTerminate() }
    private fun idle() = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
    private fun await(condition: () -> Boolean) {
        repeat(300) { idle(); if (condition()) return; Thread.sleep(5) }
        assertTrue(app.diagnostics.details.value, condition())
    }
    private fun metrics(w: Int, h: Int) = WindowMetrics(Rect(0, 0, w, h), WindowInsets.Builder()
        .setInsetsIgnoringVisibility(WindowInsets.Type.systemBars(), android.graphics.Insets.of(4, 20, 6, 24)).build())
    private class Windows(delegate: WindowManager, var metrics: WindowMetrics) : WindowManager by delegate {
        val registered = linkedMapOf<View, WindowManager.LayoutParams>()
        override fun getCurrentWindowMetrics() = metrics
        override fun addView(view: View, params: ViewGroup.LayoutParams) {
            check(view !in registered); registered[view] = params as WindowManager.LayoutParams
            view.layoutParams = params; view.layout(0, 0, params.width, params.height)
        }
        override fun updateViewLayout(view: View, params: ViewGroup.LayoutParams) {
            check(view in registered); registered[view] = params as WindowManager.LayoutParams
            view.layoutParams = params; view.layout(0, 0, params.width, params.height)
        }
        override fun removeViewImmediate(view: View) { check(registered.remove(view) != null) }
    }
    private class Host : PanelHost {
        var spec: PanelSpec? = null
        override fun show(spec: PanelSpec) { this.spec = spec }
        override fun hide(animate: Boolean) { spec = null }
        override fun refreshSound() = Unit
    }
    @Test fun `draft overlays update live but normal windows use approved settings`() {
        val controller = Robolectric.buildService(EdgeAccessibilityService::class.java).create()
        val service = controller.get(); destroy = { controller.destroy() }
        shadowOf(app).grantPermissions("${app.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
        shadowOf(service.getSystemService(PowerManager::class.java)).setIsInteractive(true)
        shadowOf(service.getSystemService(KeyguardManager::class.java)).setKeyguardLocked(false)
        await { app.configuration.state.value != null }
        val handle = HandleConfig(enabled = true)
        val panel = SavedPanel(settings = PanelConfig(buttons = listOf(PanelButton())))
        app.configuration.update { AppConfiguration(enabled = true, handles = listOf(handle), panels = listOf(panel)) }
        ReflectionHelpers.callInstanceMethod<Void>(service, "onServiceConnected")
        await { app.status.state.value.attachedHandleIds == setOf(handle.id) }
        val windows = Windows(service.getSystemService(WindowManager::class.java), metrics(400, 800))
        ReflectionHelpers.setField(service, "manager", windows)
        service.onConfigurationChanged(service.resources.configuration)
        await { windows.registered.size == 1 }
        ReflectionHelpers.getField<PanelHost>(service, "panelHost").hide(false)
        val host = Host(); ReflectionHelpers.setField(service, "panelHost", host)
        val saved = app.configuration.state.value
        val token = Any()
        app.configuration.openHandle(handle.id)
        app.editor.begin(token, EditorSelection(handle.id))
        app.configuration.editHandle(handle.id) { it.copy(trigger = it.trigger.copy(lengthDp = 220f)) }
        await { windows.registered.values.single().height == 220 }
        assertEquals(saved, app.configuration.state.value)
        // Backgrounding hides the guide but keeps the draft for the returning editor.
        app.editor.end(token)
        await { windows.registered.values.single().height == 120 }
        assertTrue(app.configuration.draft.value!!.changed)
        app.configuration.cancelDraft(); app.configuration.openPanel(panel.id)
        app.editor.begin(token, EditorSelection(handle.id, panel.id))
        app.configuration.editPanel(panel.id) { it.copy(settings = it.settings.copy(opacity = .4f)) }
        await { host.spec?.panel?.opacity == .4f && host.spec?.editing == true }
        app.editor.test(PanelTestRequest(panel.id, handle.id))
        await { host.spec?.editing == false && host.spec?.panel?.opacity == .4f }
        assertEquals(saved, app.configuration.state.value)
        app.configuration.cancelDraft()
        await { host.spec?.panel?.opacity == panel.settings.opacity }
        app.editor.end(token)
        await { host.spec == null && windows.registered.values.single().height == 120 }
        assertEquals(saved, app.configuration.state.value)
    }
    @Test fun `live landscape guides map to the left and runtime bottom survives both rotations without duplicates`() {
        val controller = Robolectric.buildService(EdgeAccessibilityService::class.java).create()
        val service = controller.get(); destroy = { controller.destroy() }
        shadowOf(app).grantPermissions("${app.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
        shadowOf(service.getSystemService(PowerManager::class.java)).setIsInteractive(true)
        shadowOf(service.getSystemService(KeyguardManager::class.java)).setKeyguardLocked(false)
        await { app.configuration.state.value != null }
        val handle = HandleConfig(enabled = true, trigger = TriggerSettings(landscapeEnabled = true, landscapeOverrideEnabled = true,
            landscape = Placement(Edge.BOTTOM, .65f)))
        val panel = SavedPanel(settings = PanelConfig(buttons = listOf(PanelButton(), PanelButton(action = ButtonAction.LOCK))))
        app.configuration.update { AppConfiguration(enabled = true, handles = listOf(handle), panels = listOf(panel)) }
        ReflectionHelpers.callInstanceMethod<Void>(service, "onServiceConnected")
        await { app.status.state.value.attachedHandleIds == setOf(handle.id) }
        val windows = Windows(service.getSystemService(WindowManager::class.java), metrics(400, 800))
        ReflectionHelpers.setField(service, "manager", windows)
        service.onConfigurationChanged(service.resources.configuration)
        await { windows.registered.size == 1 }
        ReflectionHelpers.getField<PanelHost>(service, "panelHost").hide(false)
        val host = Host(); ReflectionHelpers.setField(service, "panelHost", host)
        val saved = app.configuration.state.value
        val token = Any()
        app.editor.begin(token, EditorSelection(handle.id, panel.id, landscape = true))
        await { host.spec?.previewQuarterTurns == 1 && windows.registered.size == 1 }
        val guide = windows.registered.entries.single()
        assertEquals(4, guide.value.x); assertEquals(16, guide.value.width); assertEquals(120, guide.value.height)
        assertTrue(guide.value.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE != 0)
        assertEquals(1, ReflectionHelpers.getField<Int>(guide.key, "previewQuarterTurns"))
        assertTrue(host.spec!!.editing)
        val bounds = host.spec!!.geometry.bounds
        assertTrue(bounds.width > bounds.height)
        assertTrue(bounds.x >= 4f && bounds.y >= 20f && bounds.x + bounds.width <= 394f && bounds.y + bounds.height <= 776f)
        app.editor.begin(token, EditorSelection(handle.id, panel.id, landscape = false))
        await { host.spec?.previewQuarterTurns == 0 && windows.registered.values.single().x > 300 }
        assertEquals(saved, app.configuration.state.value)
        app.editor.end(token)
        await { app.status.state.value.attachedHandleIds == setOf(handle.id) }
        val display = service.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)
        val listener = ReflectionHelpers.getField<DisplayManager.DisplayListener>(service, "displayListener")
        for (rotation in listOf(Surface.ROTATION_90, Surface.ROTATION_270, Surface.ROTATION_90, Surface.ROTATION_270)) {
            val previous = windows.registered.keys.single()
            windows.metrics = metrics(800, 400)
            shadowOf(display).setRotation(rotation)
            listener.onDisplayChanged(Display.DEFAULT_DISPLAY)
            await { windows.registered.size == 1 && windows.registered.keys.single() !== previous }
            val runtime = windows.registered.entries.single()
            assertEquals(360, runtime.value.y); assertEquals(16, runtime.value.height)
            assertEquals(0, ReflectionHelpers.getField<Int>(runtime.key, "previewQuarterTurns"))
            assertNull(app.status.state.value.error)
        }
        controller.destroy(); destroy = {}
        assertTrue(windows.registered.isEmpty())
    }
}
