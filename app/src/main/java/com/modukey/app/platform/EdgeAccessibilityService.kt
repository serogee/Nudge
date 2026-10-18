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
import com.modukey.app.R
import com.modukey.app.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filterNotNull
import kotlin.math.roundToInt

private data class HandleSpec(val bounds: Bounds, val settings: TriggerSettings, val edge: Edge, val editing: Boolean)
private data class OpenPanel(val panelId: String, val handleId: String? = null, val testing: Boolean = false)
private enum class PanelCloseReason { APP_CHANGED, BACK, OUTSIDE_TOUCH, WINDOW_DETACHED, TIMEOUT, SCREEN_STATE, EDITOR_CHANGED,
    TEST_REPLACED, GESTURE_REPLACED, HANDLE_UNAVAILABLE, PANEL_REMOVED, TEST_ENDED, ACTION_COMPLETED, REFRESH_ERROR, WINDOW_ERROR, SERVICE_STOPPED }
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
    private var panelTimeout: PanelTimeout? = null
    private var open: OpenPanel? = null
    private var attachedPanel: Pair<OpenPanel, Boolean>? = null
    private var foregroundTracker: ForegroundTracker? = null
    private val foreground get() = foregroundTracker?.packageName
    private var panelOpenedAt: Long? = null
    private var softHideForeground: String? = null
    private var registered = false
    private var handleBounds = emptyMap<String, Bounds>()
    private var actions: ActionExecutor? = null
    private val sessions = GestureSessions()
    private val errors = mutableMapOf<String, String>()
    private val frame = Choreographer.FrameCallback { frames?.onFrame() }
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            views.values.forEach { it.cancelGesture() }; sessions.clear(); dismissPanel(PanelCloseReason.SCREEN_STATE)
            clearPanelWindows()
            if (intent?.action == Intent.ACTION_SCREEN_OFF) app.unlocks.clear()
            refresh()
        }
    }
    override fun onServiceConnected() {
        super.onServiceConnected(); stopSession()
        manager = getSystemService(WindowManager::class.java)
        foregroundTracker = ForegroundTracker(packageManager, packageName)
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val choreographer = Choreographer.getInstance()
        frames = FrameCoalescer({ choreographer.postFrameCallback(frame) }, { choreographer.removeFrameCallback(frame) }, ::refresh)
        actions = PanelActions(this)
        panelTimeout = PanelTimeout(requireNotNull(scope)) { dismissPanel(PanelCloseReason.TIMEOUT); refresh() }
        panelHost = AccessibilityPanelHost(this, manager, actions!!, { reason -> dismissPanel(when (reason) {
            PanelDismissReason.BACK -> PanelCloseReason.BACK
            PanelDismissReason.OUTSIDE_TOUCH -> PanelCloseReason.OUTSIDE_TOUCH
            PanelDismissReason.WINDOW_DETACHED -> PanelCloseReason.WINDOW_DETACHED
        }); refresh() },
            { x, y -> handleBounds.values.any { it.contains(x, y) } }, ::runButton, { panelTimeout?.touch(it) }, { error ->
                app.diagnostics.record("Panel cleanup failed", error); errors["panel"] = getString(R.string.overlay_error); frames?.request()
            })
        registry = OverlayRegistry { id -> object : OverlayHost<HandleSpec> {
            override fun attach(value: HandleSpec) {
                val view = HandleTriggerView(this@EdgeAccessibilityService, { activate(id, it) }, { sessions.reset(id) })
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
            scope?.launch { app.editor.state.collect { views.values.forEach { it.cancelGesture() }; sessions.clear(); dismissPanel(PanelCloseReason.EDITOR_CHANGED); clearPanelWindows(); refresh() } }
            scope?.launch { app.editor.tests.collect { request ->
                if (app.editor.state.value == EditorSelection(request.handleId, request.panelId) && app.configuration.state.value?.panel(request.panelId) != null) {
                    dismissPanel(PanelCloseReason.TEST_REPLACED); open = OpenPanel(request.panelId, request.handleId, testing = true)
                    panelOpenedAt = SystemClock.uptimeMillis(); refresh()
                }
            } }
        } catch (e: RuntimeException) { app.diagnostics.record("Service setup failed", e); stopSession(); app.status.report(ServiceState(connected = true, error = getString(R.string.overlay_error))) }
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (!getSystemService(PowerManager::class.java).isInteractive || getSystemService(KeyguardManager::class.java).isKeyguardLocked) return
        val observation = foregroundTracker?.observe(event, SystemClock.uptimeMillis(), panelOpenedAt) ?: return
        if (observation.switched && open?.testing == false) dismissPanel(PanelCloseReason.APP_CHANGED, event, observation.previous)
        if (observation.previous != observation.current) frames?.request()
    }
    override fun onConfigurationChanged(newConfig: Configuration) { super.onConfigurationChanged(newConfig); views.values.forEach { it.cancelGesture(); it.resetSoftHide() }; frames?.request() }
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
        if (target.isNone) return
        // One gesture can request both windows. Android owns the native popup's lifetime.
        replacePanel(target.panelId?.takeIf { config.panel(it) != null }?.let { OpenPanel(it, id) }, id)
        var submitted = target.panelId != null
        if (target.volume && target.action?.action !in listOf(ButtonAction.VOLUME_UP, ButtonAction.VOLUME_DOWN)) {
            when (val result = app.volume.show()) {
                VolumeResult.Requested -> submitted = true
                is VolumeResult.Failed -> {
                    app.diagnostics.record("Volume request failed: ${result.message}")
                    Toast.makeText(this, R.string.volume_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
        target.action?.let { action ->
            val slot = when (kind) {
                GestureKind.SWIPE -> GestureSlot.entries[requireNotNull(stage)]
                GestureKind.SWIPE_HOLD -> GestureSlot.SWIPE_HOLD
                GestureKind.TAP -> GestureSlot.TAP
                GestureKind.HOLD -> GestureSlot.HOLD
            }
            // Remove the previous panel before an app launch or the system lock action.
            refresh()
            actions?.execute(id, if (action.action in listOf(ButtonAction.VOLUME_UP, ButtonAction.VOLUME_DOWN)) action.copy(showVolumePopup = target.volume) else action, testing = false, gestureSlot = slot)?.let { result ->
                submitted = submitted || result.succeeded
                result.message?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
            }
        }
        if (submitted) sessions.submitted(handle, stage, target, now) else sessions.reset(id)
        refresh()
        if (handle.trigger.haptics) views[id]?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }
    private fun replacePanel(next: OpenPanel?, keepSession: String) {
        panelTimeout?.stop(); open?.handleId?.takeIf { it != keepSession }?.let(sessions::reset)
        if (open != next) recordDismissal(PanelCloseReason.GESTURE_REPLACED)
        open = next; panelOpenedAt = next?.let { SystemClock.uptimeMillis() }
    }
    private fun dismissPanel(reason: PanelCloseReason, event: AccessibilityEvent? = null, previous: String? = foreground) {
        recordDismissal(reason, event, previous)
        panelTimeout?.stop(); open?.handleId?.let(sessions::reset); open = null; panelOpenedAt = null
    }
    private fun recordDismissal(reason: PanelCloseReason, event: AccessibilityEvent? = null, previous: String? = foreground) {
        val panel = open ?: return
        fun field(value: CharSequence?) = value?.toString()?.replace(Regex("[\\s\\p{Cntrl}]"), " ")?.take(160) ?: "unknown"
        val age = (SystemClock.uptimeMillis() - (panelOpenedAt ?: SystemClock.uptimeMillis())).coerceAtLeast(0)
        app.diagnostics.record("Panel dismissed: reason=${reason.name.lowercase(java.util.Locale.ROOT)}; panel=${field(panel.panelId)}; " +
            "handle=${field(panel.handleId)}; ageMs=$age; previousPackage=${field(previous)}; " +
            "incomingPackage=${field(event?.packageName)}; eventClass=${field(event?.className)}")
    }
    private fun clearPanelWindows() {
        attachedPanel = null
        try { panelHost?.hide(animate = false) } catch (e: RuntimeException) {
            app.diagnostics.record("Panel cleanup failed", e); errors["panel"] = getString(R.string.overlay_error)
        }
    }
    private fun runButton(button: PanelButton) {
        val owner = open ?: return
        val config = app.configuration.state.value ?: return
        val h = config.handle(owner.handleId)
        if (!owner.testing && (h == null || reason(config, h) != HandleAvailability.ACTIVE)) { dismissPanel(PanelCloseReason.HANDLE_UNAVAILABLE); refresh(); return }
        val p = config.panel(owner.panelId)?.settings ?: return
        val trigger = h?.trigger ?: TriggerSettings()
        val s = screen()
        val b = TriggerGeometry.bounds(s.width, s.height, s.density, s.insets, trigger, s.landscape)
        val geometry = PanelGeometry.calculate(s.usable, s.density, p.layout(s.landscape), p.buttons.size, b, trigger.placement(s.landscape).edge)
        if (button !in p.buttons.take(geometry.visibleCount)) return
        val result = actions?.execute(h?.id.orEmpty(), button, owner.testing) ?: return
        result.message?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        if (result.close) { dismissPanel(PanelCloseReason.ACTION_COMPLETED); refresh() } else { panelTimeout?.restart(); panelHost?.refreshSound() }
    }
    private fun refresh() {
        if (scope == null) return
        try { render() } catch (e: RuntimeException) {
            app.diagnostics.record("Overlay refresh failed", e)
            dismissPanel(PanelCloseReason.REFRESH_ERROR); sessions.clear()
            try { panelHost?.hide(animate = false) } catch (_: RuntimeException) { }
            registry?.clear()
            app.status.report(ServiceState(connected = true, error = getString(R.string.overlay_error)))
        }
    }
    private fun render() {
        if (scope == null) return
        val config = app.configuration.state.value ?: return
        if (softHideForeground != foreground) { views.values.forEach { it.resetSoftHide() }; softHideForeground = foreground }
        val s = screen(); val selected = app.editor.state.value
        val editing = selected != null && getSystemService(PowerManager::class.java).isInteractive && !getSystemService(KeyguardManager::class.java).isKeyguardLocked
        val reasons = config.handles.associate { it.id to reason(config, it) }.toMutableMap()
        sessions.retain(reasons.filterValues { it == HandleAvailability.ACTIVE }.keys)
        val desired = linkedMapOf<String, HandleSpec>()
        config.handles.asReversed().forEach { h ->
            val guide = editing && selected?.preview == true && selected.handleId == h.id
            if (guide || (selected == null && reasons[h.id] == HandleAvailability.ACTIVE)) {
                val b = TriggerGeometry.bounds(s.width, s.height, s.density, s.insets, h.trigger, s.landscape)
                desired[h.id] = HandleSpec(b, h.trigger, h.trigger.placement(s.landscape).edge, guide)
            }
            if (selected != null) reasons[h.id] = HandleAvailability.EDITING
        }
        var wanted = open
        val closeReason = wanted?.let {
            when {
                config.panel(it.panelId) == null -> PanelCloseReason.PANEL_REMOVED
                it.testing && (!editing || selected != EditorSelection(it.handleId, it.panelId)) -> PanelCloseReason.TEST_ENDED
                !it.testing && selected != null -> PanelCloseReason.EDITOR_CHANGED
                !it.testing && reasons[it.handleId] != HandleAvailability.ACTIVE -> PanelCloseReason.HANDLE_UNAVAILABLE
                else -> null
            }
        }
        if (closeReason != null) { dismissPanel(closeReason); wanted = null }
        var panelEditing = false
        if (wanted == null && editing && selected?.preview == true && selected.panelId != null && config.panel(selected.panelId) != null) {
            wanted = OpenPanel(selected.panelId, selected.handleId); panelEditing = true
        }
        val identity = wanted?.let { it to panelEditing }; val changed = attachedPanel != identity
        try {
            if (changed) {
                val previous = attachedPanel?.takeIf { !it.second }?.first
                val animateExit = if (previous?.testing == true) editing && selected == EditorSelection(previous.handleId, previous.panelId)
                    else selected == null && config.handle(previous?.handleId)?.let { reason(config, it) == HandleAvailability.ACTIVE } == true
                panelHost?.hide(animate = animateExit)
            }
            if (wanted != null) {
                val trigger = config.handle(wanted.handleId)?.trigger ?: TriggerSettings()
                val p = config.panel(wanted.panelId)!!.settings
                val b = TriggerGeometry.bounds(s.width, s.height, s.density, s.insets, trigger, s.landscape)
                panelHost?.show(PanelSpec(p, p.layout(s.landscape), PanelGeometry.calculate(s.usable, s.density, p.layout(s.landscape), p.buttons.size, b, trigger.placement(s.landscape).edge), panelEditing, trigger.placement(s.landscape).edge))
            }
            attachedPanel = identity
            if (wanted != null) errors.remove("panel")
        } catch (e: RuntimeException) {
            app.diagnostics.record("Panel window failed", e); dismissPanel(PanelCloseReason.WINDOW_ERROR); attachedPanel = null
            try { panelHost?.hide(animate = false) } catch (_: RuntimeException) { }; errors["panel"] = getString(R.string.overlay_error)
        }
        val timedPanel = open?.let { config.panel(it.panelId)?.settings }
        panelTimeout?.configure(if (timedPanel?.autoCloseEnabled == true) timedPanel.autoCloseMs else 0)
        handleBounds = desired.filterValues { !it.editing }.mapValues { it.value.bounds }
        val failures = registry?.reconcile(desired, raise = changed && wanted != null).orEmpty()
        views.forEach { (id, view) -> view.setPanelOpen(open?.handleId == id) }
        errors.keys.filter { it != "panel" && it !in desired }.toList().forEach(errors::remove)
        failures.forEach { (id, error) -> errors[id] = getString(R.string.overlay_error); app.diagnostics.record("Handle window failed", error); reasons[id] = HandleAvailability.ERROR }
        desired.keys.filter { it in registry?.attached.orEmpty() }.forEach { errors.remove(it) }
        errors.keys.filter { it in desired }.forEach { reasons[it] = HandleAvailability.ERROR }
        try { updateEvents(config) } catch (e: RuntimeException) { app.diagnostics.record("Event subscription failed", e) }
        val active = registry?.attached.orEmpty().filterTo(mutableSetOf()) { selected == null }
        app.status.report(ServiceState(true, active.isNotEmpty(), errors.values.firstOrNull(), active, reasons, open?.handleId, open?.panelId))
    }
    private fun updateEvents(config: AppConfiguration) {
        val need = open != null || (config.enabled && config.handles.any { h -> h.enabled &&
            (h.trigger.excludedPackages.isNotEmpty() || h.trigger.softHideEnabled || h.bindings.values.any { config.panel(it.panelId) != null }) })
        foregroundTracker?.track(need, SystemClock.uptimeMillis())
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
        dismissPanel(PanelCloseReason.SERVICE_STOPPED)
        panelTimeout?.stop(); panelTimeout = null
        frames?.stop(); frames = null; scope?.cancel(); scope = null
        attachedPanel = null; sessions.clear(); app.unlocks.clear(); handleBounds = emptyMap(); foregroundTracker = null; softHideForeground = null
        try { panelHost?.hide(animate = false) } catch (e: RuntimeException) { app.diagnostics.record("Panel cleanup failed", e) }; panelHost = null
        registry?.clear()?.values?.forEach { app.diagnostics.record("Handle cleanup failed", it) }; registry = null
        views.clear(); layouts.clear(); errors.clear(); actions = null
        if (registered) { registered = false; try { unregisterReceiver(screenReceiver) } catch (_: RuntimeException) { } }
        app.status.report(ServiceState())
    }
}
