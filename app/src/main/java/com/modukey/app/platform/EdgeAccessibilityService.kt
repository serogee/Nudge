package com.modukey.app.platform

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.*
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.PowerManager
import android.os.SystemClock
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.modukey.app.NudgeApplication
import com.modukey.app.MainActivity
import com.modukey.app.R
import com.modukey.app.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filterNotNull
import kotlin.math.roundToInt

private data class HandleSpec(val bounds: Bounds, val settings: TriggerSettings, val edge: Edge, val editing: Boolean)
private data class OpenPanel(val handleId: String, val index: Int, val testing: Boolean = false)
internal data class ScreenSpace(val width: Float, val height: Float, val density: Float, val insets: Insets, val landscape: Boolean) {
    val usable get() = Bounds(insets.left, insets.top, (width-insets.left-insets.right).coerceAtLeast(1f), (height-insets.top-insets.bottom).coerceAtLeast(1f))
}

class EdgeAccessibilityService : AccessibilityService() {
    private val app get() = application as NudgeApplication
    private var scope: CoroutineScope? = null
    private lateinit var manager: WindowManager
    private val views = linkedMapOf<String, HandleTriggerView>()
    private val layouts = mutableMapOf<String, WindowManager.LayoutParams>()
    private var registry: OverlayRegistry<String, HandleSpec>? = null
    private var frames: FrameCoalescer? = null
    private var panelHost: PanelHost? = null
    private var open: OpenPanel? = null
    private var attachedPanel: Pair<OpenPanel, Boolean>? = null
    private var foreground: String? = null
    private var registered = false
    private var handleBounds = emptyMap<String, Bounds>()
    private var actions: ActionExecutor? = null
    private val sessions = GestureSessions()
    private val errors = mutableMapOf<String, String>()
    private val frame = Choreographer.FrameCallback { frames?.onFrame() }
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            views.values.forEach { it.cancelGesture() }; sessions.clear(); open = null
            if (intent?.action == Intent.ACTION_SCREEN_OFF) app.unlocks.clear()
            refresh()
        }
    }
    override fun onServiceConnected() {
        super.onServiceConnected(); stopSession()
        manager = getSystemService(WindowManager::class.java)
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val choreographer = Choreographer.getInstance()
        frames = FrameCoalescer({ choreographer.postFrameCallback(frame) }, { choreographer.removeFrameCallback(frame) }, ::refresh)
        actions = PanelActions(this)
        panelHost = AccessibilityPanelHost(this, manager, actions!!, { dismissPanel(); refresh() },
            { x, y -> handleBounds.values.any { it.contains(x, y) } }, ::runButton)
        registry = OverlayRegistry { id -> object : OverlayHost<HandleSpec> {
            override fun attach(value: HandleSpec) {
                val view = HandleTriggerView(this@EdgeAccessibilityService) { activate(id, it) }
                view.configure(value.settings, value.edge, value.editing)
                val p = params(value); views[id] = view
                try { manager.addView(view, p); layouts[id] = p } catch (e: RuntimeException) { detach(); throw e }
            }
            override fun update(value: HandleSpec) {
                val view = requireNotNull(views[id]); val p = params(value); val previous = layouts[id]
                view.configure(value.settings, value.edge, value.editing)
                if (previous?.x != p.x || previous.y != p.y || previous.width != p.width || previous.height != p.height || previous.flags != p.flags) {
                    view.cancelGesture(); manager.updateViewLayout(view, p); layouts[id] = p
                }
            }
            override fun detach() {
                val view = views.remove(id); layouts.remove(id); view?.cancelGesture()
                if (view?.isAttachedToWindow == true) manager.removeViewImmediate(view)
            }
        } }
        try {
            ContextCompat.registerReceiver(this, screenReceiver, IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_USER_PRESENT)
            }, ContextCompat.RECEIVER_NOT_EXPORTED)
            registered = true
            app.status.report(ServiceState(connected = true)); app.diagnostics.record("Accessibility connected")
            scope?.launch { app.configuration.state.filterNotNull().collect { frames?.request() } }
            scope?.launch { app.editor.state.collect { views.values.forEach { it.cancelGesture() }; sessions.clear(); dismissPanel(); refresh() } }
            scope?.launch { app.editor.tests.collect { request ->
                if (app.editor.state.value?.handleId == request.handleId) { open = OpenPanel(request.handleId, request.panelIndex, testing = true); refresh() }
            } }
        } catch (e: RuntimeException) { app.diagnostics.record("Service setup failed", e); stopSession(); app.status.report(ServiceState(connected = true, error = getString(R.string.overlay_error))) }
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) {
            // Service-owned windows also use our package. Only an activity event changes the foreground app.
            if (event.className == MainActivity::class.java.name) {
                if (pkg != foreground && open?.testing == false) dismissPanel()
                foreground = pkg
            }
            frames?.request(); return
        }
        if (pkg == "android" || pkg == "com.android.systemui") { frames?.request(); return }
        if (pkg != foreground) { foreground = pkg; if (open?.testing == false) dismissPanel(); frames?.request() }
    }
    override fun onConfigurationChanged(newConfig: Configuration) { super.onConfigurationChanged(newConfig); views.values.forEach { it.cancelGesture() }; frames?.request() }
    override fun onInterrupt() { views.values.forEach { it.cancelGesture() }; refresh() }
    override fun onUnbind(intent: Intent?): Boolean { app.diagnostics.record("Accessibility unbound"); stopSession(); return super.onUnbind(intent) }
    override fun onDestroy() { stopSession(); super.onDestroy() }
    override fun onTaskRemoved(rootIntent: Intent?) { app.diagnostics.record("Task removed; service remains system managed"); super.onTaskRemoved(rootIntent) }

    private fun reason(config: AppConfiguration, h: HandleConfig) = HandleEligibility.reason(config, h,
        getSystemService(PowerManager::class.java).isInteractive, getSystemService(KeyguardManager::class.java).isKeyguardLocked,
        resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE, foreground)
    private fun screen(): ScreenSpace {
        val m = manager.currentWindowMetrics
        val safe = m.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
        val nav = m.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.mandatorySystemGestures())
        return ScreenSpace(m.bounds.width().toFloat(), m.bounds.height().toFloat(), resources.displayMetrics.density,
            Insets(safe.left.toFloat(), safe.top.toFloat(), safe.right.toFloat(), maxOf(safe.bottom, nav.bottom).toFloat()), resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
    }
    private fun activate(id: String, kind: GestureKind) {
        val config = app.configuration.state.value ?: return
        val handle = config.handle(id) ?: return
        if (app.editor.state.value != null || reason(config, handle) != HandleAvailability.ACTIVE) { refresh(); return }
        val now = SystemClock.elapsedRealtime(); val (target, stage) = sessions.next(handle, kind, now)
        if (target == GestureTarget.NONE) return
        if (target == GestureTarget.VOLUME) {
            replacePanel(null, id); refresh()
            when (val result = app.volume.show()) {
                VolumeResult.Requested -> sessions.submitted(handle, stage, target, now)
                is VolumeResult.Failed -> { sessions.reset(id); app.diagnostics.record("Volume request failed: ${result.message}"); Toast.makeText(this, R.string.volume_failed, Toast.LENGTH_SHORT).show() }
            }
        } else { replacePanel(OpenPanel(id, target.panelIndex!!), id); sessions.submitted(handle, stage, target, now); refresh() }
        if (handle.trigger.haptics) views[id]?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }
    private fun replacePanel(next: OpenPanel?, keepSession: String) { open?.takeIf { it.handleId != keepSession }?.let { sessions.reset(it.handleId) }; open = next }
    private fun dismissPanel() { open?.let { sessions.reset(it.handleId) }; open = null }
    private fun runButton(button: PanelButton) {
        val owner = open ?: return; val config = app.configuration.state.value ?: return; val h = config.handle(owner.handleId) ?: return
        if (!owner.testing && reason(config, h) != HandleAvailability.ACTIVE) { dismissPanel(); refresh(); return }
        val p = h.panels[owner.index]; val s = screen()
        val b = TriggerGeometry.bounds(s.width, s.height, s.density, s.insets, h.trigger, s.landscape)
        val geometry = PanelGeometry.calculate(s.usable, s.density, p.layout(s.landscape), p.buttons.size, b, h.trigger.placement(s.landscape).edge)
        if (button !in p.buttons.take(geometry.visibleCount)) return
        val result = actions?.execute(h.id, button, owner.testing) ?: return
        result.message?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        if (result.close) { dismissPanel(); refresh() } else panelHost?.refreshSound()
    }
    private fun refresh() {
        if (scope == null) return
        try { render() } catch (e: RuntimeException) {
            app.diagnostics.record("Overlay refresh failed", e)
            dismissPanel(); sessions.clear()
            try { panelHost?.hide() } catch (_: RuntimeException) { }
            registry?.clear()
            app.status.report(ServiceState(connected = true, error = getString(R.string.overlay_error)))
        }
    }
    private fun render() {
        if (scope == null) return
        val config = app.configuration.state.value ?: return
        val s = screen(); val selected = app.editor.state.value
        val editing = selected != null && getSystemService(PowerManager::class.java).isInteractive && !getSystemService(KeyguardManager::class.java).isKeyguardLocked
        val reasons = config.handles.associate { it.id to reason(config, it) }.toMutableMap()
        sessions.retain(reasons.filterValues { it == HandleAvailability.ACTIVE }.keys)
        val desired = linkedMapOf<String, HandleSpec>()
        config.handles.asReversed().forEach { h ->
            val guide = editing && selected?.handleId == h.id
            if (guide || (selected == null && reasons[h.id] == HandleAvailability.ACTIVE)) {
                val b = TriggerGeometry.bounds(s.width, s.height, s.density, s.insets, h.trigger, s.landscape)
                desired[h.id] = HandleSpec(b, h.trigger, h.trigger.placement(s.landscape).edge, guide)
            }
            if (selected != null) reasons[h.id] = HandleAvailability.EDITING
        }
        var wanted = open
        if (wanted != null && (config.handle(wanted.handleId) == null || (wanted.testing && (!editing || selected?.handleId != wanted.handleId)) ||
                (!wanted.testing && (selected != null || reasons[wanted.handleId] != HandleAvailability.ACTIVE)))) { dismissPanel(); wanted = null }
        var panelEditing = false
        if (wanted == null && editing && selected?.panelIndex != null && config.handle(selected.handleId) != null) { wanted = OpenPanel(selected.handleId, selected.panelIndex); panelEditing = true }
        val identity = wanted?.let { it to panelEditing }; val changed = attachedPanel != identity
        try {
            if (changed) panelHost?.hide()
            if (wanted != null) {
                val h = config.handle(wanted.handleId)!!; val p = h.panels[wanted.index]
                val b = TriggerGeometry.bounds(s.width, s.height, s.density, s.insets, h.trigger, s.landscape)
                panelHost?.show(PanelSpec(p, p.layout(s.landscape), PanelGeometry.calculate(s.usable, s.density, p.layout(s.landscape), p.buttons.size, b, h.trigger.placement(s.landscape).edge), panelEditing))
            }
            attachedPanel = identity
            if (wanted != null) errors.remove("panel")
        } catch (e: RuntimeException) {
            app.diagnostics.record("Panel window failed", e); dismissPanel(); attachedPanel = null
            try { panelHost?.hide() } catch (_: RuntimeException) { }; errors["panel"] = getString(R.string.overlay_error)
        }
        handleBounds = desired.filterValues { !it.editing }.mapValues { it.value.bounds }
        val failures = registry?.reconcile(desired, raise = changed && wanted != null).orEmpty()
        errors.keys.filter { it != "panel" && it !in desired }.toList().forEach(errors::remove)
        failures.forEach { (id, error) -> errors[id] = getString(R.string.overlay_error); app.diagnostics.record("Handle window failed", error); reasons[id] = HandleAvailability.ERROR }
        desired.keys.filter { it in registry?.attached.orEmpty() }.forEach { errors.remove(it) }
        errors.keys.filter { it in desired }.forEach { reasons[it] = HandleAvailability.ERROR }
        try { updateEvents(config) } catch (e: RuntimeException) { app.diagnostics.record("Event subscription failed", e) }
        val active = registry?.attached.orEmpty().filterTo(mutableSetOf()) { selected == null }
        app.status.report(ServiceState(true, active.isNotEmpty(), errors.values.firstOrNull(), active, reasons, open?.handleId))
    }
    private fun updateEvents(config: AppConfiguration) {
        val need = open != null || (config.enabled && config.handles.any { it.enabled && it.trigger.excludedPackages.isNotEmpty() })
        val events = if (need) AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED else 0
        val info = serviceInfo ?: return
        if (info.eventTypes != events) { info.eventTypes = events; serviceInfo = info }
    }
    @SuppressLint("RtlHardcoded")
    private fun params(spec: HandleSpec) = WindowManager.LayoutParams(spec.bounds.width.roundToInt().coerceAtLeast(1), spec.bounds.height.roundToInt().coerceAtLeast(1),
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or (if (spec.editing) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else 0), PixelFormat.TRANSLUCENT).apply {
        gravity = Gravity.TOP or Gravity.LEFT; x = spec.bounds.x.roundToInt(); y = spec.bounds.y.roundToInt()
        setFitInsetsTypes(0); layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS; title = "Nudge handle"
    }
    private fun stopSession() {
        frames?.stop(); frames = null; scope?.cancel(); scope = null
        open = null; attachedPanel = null; sessions.clear(); app.unlocks.clear(); handleBounds = emptyMap(); foreground = null
        try { panelHost?.hide() } catch (e: RuntimeException) { app.diagnostics.record("Panel cleanup failed", e) }; panelHost = null
        registry?.clear()?.values?.forEach { app.diagnostics.record("Handle cleanup failed", it) }; registry = null
        views.clear(); layouts.clear(); errors.clear(); actions = null
        if (registered) { registered = false; try { unregisterReceiver(screenReceiver) } catch (_: RuntimeException) { } }
        app.status.report(ServiceState())
    }
}
