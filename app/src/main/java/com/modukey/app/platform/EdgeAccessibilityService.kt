package com.modukey.app.platform

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.*
import android.content.res.Configuration
import android.hardware.display.DisplayManager
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

private data class OpenPanel(val panelId: String, val handleId: String? = null, val testing: Boolean = false)
private enum class PanelCloseReason { APP_CHANGED, BACK, OUTSIDE_TOUCH, WINDOW_DETACHED, TIMEOUT, SCREEN_STATE, EDITOR_CHANGED,
    TEST_REPLACED, GESTURE_REPLACED, HANDLE_UNAVAILABLE, PANEL_REMOVED, TEST_ENDED, ACTION_COMPLETED, REFRESH_ERROR, WINDOW_ERROR, SERVICE_STOPPED, WHEEL_RELEASED }
internal data class ScreenSpace(val width: Float, val height: Float, val density: Float, val insets: Insets, val landscape: Boolean, val rotation: Int = 0) {
    val usable get() = Bounds(insets.left, insets.top, (width-insets.left-insets.right).coerceAtLeast(1f), (height-insets.top-insets.bottom).coerceAtLeast(1f))
}

class EdgeAccessibilityService : AccessibilityService() {
    private val app get() = application as NudgeApplication
    private var scope: CoroutineScope? = null
    private lateinit var manager: WindowManager
    private val views = linkedMapOf<String, HandleTriggerView>()
    private var displays: DisplayManager? = null
    private var displayRotation = 0
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(id: Int) = Unit
        override fun onDisplayRemoved(id: Int) = Unit
        override fun onDisplayChanged(id: Int) {
            if (scope == null || id != Display.DEFAULT_DISPLAY) return
            val rotation = displays?.getDisplay(id)?.rotation ?: return
            if (rotation != displayRotation) { displayRotation = rotation; rebuildHandles() }
        }
    }
    private var registry: OverlayRegistry<String, HandleWindowSpec>? = null
    private var frames: FrameCoalescer? = null
    private var sliderGuideHost: SliderGuideHost? = null
    private var brightnessHost: BrightnessPopupHost? = null
    private var brightnessOpen = false
    private var brightnessOwner: String? = null
    private var panelHost: PanelHost? = null
    private var panelTimeout: PanelTimeout? = null
    private var open: OpenPanel? = null
    private var attachedPanel: Pair<OpenPanel, Boolean>? = null
    private var raiseHandles = false
    private var foregroundTracker: ForegroundTracker? = null
    private val foreground get() = foregroundTracker?.packageName
    private var panelOpenedAt: Long? = null
    private var softHideForeground: String? = null
    private var registered = false
    private var handleBounds = emptyMap<String, Bounds>()
    private var actions: ActionExecutor? = null
    private var screenshots: NativeScreenshotRequest? = null
    private var systemPanels: NativeSystemPanelRequest? = null
    private val sessions = GestureSessions()
    private data class SliderBinding(val handleId: String, val slot: GestureSlot, val target: GestureTarget)
    private var sliderBinding: SliderBinding? = null
    private data class WheelBinding(val handle: HandleConfig, val slot: GestureSlot, val target: GestureTarget, val stage: Int?,
        val panel: SavedPanel, val session: WheelPickSession, var armJob: Job? = null)
    private var wheelBinding: WheelBinding? = null
    private val haptics by lazy { GestureHaptics(this) }
    private val errors = mutableMapOf<String, String>()
    private val frame = Choreographer.FrameCallback { frames?.onFrame() }
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            screenshots?.cancel()
            systemPanels?.cancel()
            hideBrightness(); views.values.forEach { it.cancelGesture() }; sessions.clear(); dismissPanel(PanelCloseReason.SCREEN_STATE)
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
        screenshots = NativeScreenshotRequest(choreographer,
            { scope != null && getSystemService(PowerManager::class.java).isInteractive },
            { hideBrightness(); systemPanels?.cancel(); dismissPanel(PanelCloseReason.ACTION_COMPLETED); clearPanelWindows() },
            { performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT) },
            { app.diagnostics.record("Screenshot request failed"); Toast.makeText(this, R.string.screenshot_failed, Toast.LENGTH_SHORT).show() })
        systemPanels = NativeSystemPanelRequest(choreographer,
            { scope != null && getSystemService(PowerManager::class.java).isInteractive },
            { hideBrightness(); screenshots?.cancel(); dismissPanel(PanelCloseReason.ACTION_COMPLETED); clearPanelWindows() },
            { action ->
                val opened = performGlobalAction(action)
                if (opened && action == GLOBAL_ACTION_QUICK_SETTINGS && HyperOsSettings.isXiaomiDevice())
                    Toast.makeText(this, R.string.native_control_center_manual_help, Toast.LENGTH_LONG).show()
                opened
            },
            { app.diagnostics.record("System panel request failed"); Toast.makeText(this, R.string.native_panel_failed, Toast.LENGTH_SHORT).show() })
        sliderGuideHost = SliderGuideHost(this, manager)
        brightnessHost = BrightnessPopupHost(this, manager, app.brightness, { hideBrightness(animate = true); refresh() }, {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }, changed = { panelHost?.refreshSound() })
        actions = PanelActions(this, screenshot = { screenshots?.request() == true }, systemPanel = { systemPanels?.request(it) == true }, brightnessPopup = ::showBrightness)
        panelTimeout = PanelTimeout(requireNotNull(scope)) { dismissPanel(PanelCloseReason.TIMEOUT); refresh() }
        panelHost = AccessibilityPanelHost(this, manager, actions!!, { reason -> dismissPanel(when (reason) {
            PanelDismissReason.BACK -> PanelCloseReason.BACK
            PanelDismissReason.OUTSIDE_TOUCH -> PanelCloseReason.OUTSIDE_TOUCH
            PanelDismissReason.WINDOW_DETACHED -> PanelCloseReason.WINDOW_DETACHED
        }); refresh() },
            { x, y -> handleBounds.values.any { it.contains(x, y) } }, ::runButton, { panelTimeout?.touch(it) }, { error ->
                app.diagnostics.record("Panel cleanup failed", error); errors["panel"] = getString(R.string.overlay_error); frames?.request()
            })
        registry = OverlayRegistry { id -> HandleWindowHost(this, manager, { activate(id, it) }, { sessions.reset(id) }, { frames?.request() }, { error ->
            app.diagnostics.record("Handle handoff cleanup failed", error)
        }) { view ->
            if (view == null) views.remove(id) else { view.gesturePolicy = { gesturePolicy(id) }; views[id] = view }
        } }
        try {
            displays = getSystemService(DisplayManager::class.java)
            displayRotation = displays?.getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: 0
            displays?.registerDisplayListener(displayListener, null)
            ContextCompat.registerReceiver(this, screenReceiver, IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_USER_PRESENT)
            }, ContextCompat.RECEIVER_NOT_EXPORTED)
            registered = true
            app.shortcuts.bind(this, ::runShortcut)
            app.accessibility.bind(this) { disableSelf(); stopSession() }
            app.status.report(ServiceState(connected = true)); app.diagnostics.record("Accessibility connected")
            scope?.launch { app.configuration.preview.filterNotNull().collect { frames?.request() } }
            scope?.launch { app.configuration.state.filterNotNull().collect { app.handlePauses.reconcile(it); frames?.request(); panelHost?.refreshSound() } }
            scope?.launch { app.sliderGuidePreview.state.collect { frames?.request() } }
            scope?.launch { app.brightnessPreview.state.collect { frames?.request() } }
            scope?.launch { app.automaticCalibration.state.map { it.phase }.distinctUntilChanged().collect { phase ->
                if (phase == AutomaticCalibrationPhase.RUNNING) {
                    hideBrightness(); screenshots?.cancel(); systemPanels?.cancel(); views.values.forEach { it.cancelGesture() }
                    sessions.clear(); dismissPanel(PanelCloseReason.ACTION_COMPLETED); clearPanelWindows()
                }
                frames?.request(); panelHost?.refreshSound()
            } }
            scope?.launch { app.handlePauses.state.collect { frames?.request(); panelHost?.refreshSound() } }
            scope?.launch { app.editor.state.collect { hideBrightness(); screenshots?.cancel(); systemPanels?.cancel(); views.values.forEach { it.cancelGesture() }; sessions.clear(); dismissPanel(PanelCloseReason.EDITOR_CHANGED); clearPanelWindows(); refresh() } }
            scope?.launch { app.editor.tests.collect { request ->
                val selection = app.editor.state.value
                if (selection?.preview == true && selection.matches(request.handleId, request.panelId) && app.configuration.preview.value?.panel(request.panelId) != null) {
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
    override fun onConfigurationChanged(newConfig: Configuration) { super.onConfigurationChanged(newConfig); rebuildHandles() }
    private fun rebuildHandles() {
        sliderGuideHost?.hide()
        views.values.forEach { it.cancelGesture(); it.resetSoftHide() }
        registry?.clear()?.values?.forEach { app.diagnostics.record("Handle rotation cleanup failed", it) }
        frames?.request()
    }
    override fun onInterrupt() { views.values.forEach { it.cancelGesture() }; refresh() }
    override fun onUnbind(intent: Intent?): Boolean { app.diagnostics.record("Accessibility unbound"); stopSession(); return super.onUnbind(intent) }
    override fun onDestroy() { stopSession(); super.onDestroy() }
    override fun onTaskRemoved(rootIntent: Intent?) { app.diagnostics.record("Task removed; service remains system managed"); super.onTaskRemoved(rootIntent) }

    private fun reason(config: AppConfiguration, h: HandleConfig, landscape: Boolean = screen().landscape, ignorePause: Boolean = false): HandleAvailability {
        if (app.automaticCalibration.state.value.phase == AutomaticCalibrationPhase.RUNNING) return HandleAvailability.PAUSED
        val base = HandleEligibility.reason(config, h,
        getSystemService(PowerManager::class.java).isInteractive, getSystemService(KeyguardManager::class.java).isKeyguardLocked,
        landscape, foreground)
        return if (base == HandleAvailability.ACTIVE && !ignorePause && h.id in app.handlePauses.state.value) HandleAvailability.PAUSED else base
    }
    private fun screen(): ScreenSpace {
        val m = manager.currentWindowMetrics
        val safe = m.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
        val nav = m.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.mandatorySystemGestures())
        return ScreenSpace(m.bounds.width().toFloat(), m.bounds.height().toFloat(), resources.displayMetrics.density,
            Insets(safe.left.toFloat(), safe.top.toFloat(), safe.right.toFloat(), maxOf(safe.bottom, nav.bottom).toFloat()), m.bounds.width() > m.bounds.height(),
            displays?.getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: 0)
    }
    private fun activate(id: String, kind: GestureKind) {
        val config = app.configuration.state.value ?: return
        val handle = config.handle(id)?.effectiveGestures(config, screen().landscape) ?: return
        if (app.editor.state.value != null || reason(config, handle) != HandleAvailability.ACTIVE) { refresh(); return }
        haptics.perform(handle.trigger.feedback(kind))
        val now = SystemClock.elapsedRealtime(); val (target, stage) = sessions.next(handle, kind, now)
        if (target.isNone) return
        // One gesture can request both windows. Android owns the native popup's lifetime.
        replacePanel(target.panelId?.takeIf { config.panel(it) != null }?.let { OpenPanel(it, id) }, id)
        val slot = when (kind) {
            GestureKind.SWIPE -> GestureSlot.entries[requireNotNull(stage)]
            GestureKind.SWIPE_HOLD -> GestureSlot.SWIPE_HOLD
            GestureKind.TAP -> GestureSlot.TAP
            GestureKind.HOLD -> GestureSlot.HOLD
        }
        val continuous = target.slideToAdjust && (target.volume xor target.brightness) && views[id]?.isTouching == true
        if (continuous) { cancelSlider(); if (target.brightness) brightnessHost?.holdFromHandle(true) }
        var submitted = target.panelId != null
        var volumeSubmitted = false
        if (target.volume && target.action?.action !in listOf(ButtonAction.VOLUME_UP, ButtonAction.VOLUME_DOWN)) {
            when (val result = if (continuous) app.volume.showMedia() else app.volume.show()) {
                VolumeResult.Requested -> { submitted = true; volumeSubmitted = true }
                is VolumeResult.Failed -> {
                    app.diagnostics.record("Volume request failed: ${result.message}")
                    Toast.makeText(this, R.string.volume_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
        target.action?.let { action ->
            // Remove the previous panel before an app launch or the system lock action.
            refresh()
            actions?.execute(id, if (action.action in listOf(ButtonAction.VOLUME_UP, ButtonAction.VOLUME_DOWN)) action.copy(showVolumePopup = target.volume) else action, testing = false, gestureSlot = slot)?.let { result ->
                submitted = submitted || result.succeeded
                if (target.volume && action.action in listOf(ButtonAction.VOLUME_UP, ButtonAction.VOLUME_DOWN)) volumeSubmitted = result.succeeded
                result.message?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
            }
        }
        if (target.brightness) {
            if (continuous) brightnessHost?.holdFromHandle(true)
            actions?.showBrightness(id, slot)?.let { result ->
                submitted = submitted || result.succeeded
                result.message?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
            }
        }
        val sliding = continuous && ((target.volume && volumeSubmitted) ||
            (target.brightness && brightnessHost?.visible == true && app.brightness.canWrite)) && beginSlider(handle, slot, target, stage)
        if (target.dragsWheel(config, screen().landscape)) cancelSlider()
        val picking = target.dragsWheel(config, screen().landscape) && beginWheel(handle, slot, target, stage)
        if (target.dragsWheel(config, screen().landscape) && !picking) dismissPanel(PanelCloseReason.WHEEL_RELEASED)
        if (continuous && target.brightness && !sliding) brightnessHost?.holdFromHandle(false)
        if (submitted) {
            sessions.submitted(handle, stage, target, now)
            if ((sliding || picking) && stage != null) sessions.keepWhileTouch(id)
        } else sessions.reset(id)
        if (target.action?.action in listOf(ButtonAction.CLOSE_PANELS, ButtonAction.SCREENSHOT)) sessions.reset(id)
        if (target.dragsWheel(config, screen().landscape) && !picking) sessions.reset(id)
        refresh()
    }
    private fun gesturePolicy(id: String): HandleGesturePolicy {
        val config = app.configuration.state.value ?: return HandleGesturePolicy()
        val landscape = screen().landscape
        val h = config.handle(id)?.effectiveGestures(config, landscape) ?: return HandleGesturePolicy()
        val next = sessions.next(h, GestureKind.SWIPE, SystemClock.elapsedRealtime()).first.normalized()
        fun captures(t: GestureTarget) = t.slideToAdjust || t.dragsWheel(config, landscape)
        return HandleGesturePolicy(captures(next), captures(h.target(GestureSlot.HOLD).normalized()),
            captures(h.runtimeTarget(GestureSlot.SWIPE_HOLD).normalized()), !h.hasContinuousSwipe &&
                (0..2).none { h.target(GestureSlot.entries[it]).dragsWheel(config, landscape) })
    }
    private fun beginWheel(handle: HandleConfig, slot: GestureSlot, target: GestureTarget, stage: Int?): Boolean {
        val config = app.configuration.state.value ?: return false
        val original = config.handle(handle.id) ?: return false
        val panel = config.panel(target.panelId) ?: return false
        val view = views[handle.id] ?: return false
        if (!view.isTouching || app.editor.state.value != null || reason(config, original) != HandleAvailability.ACTIVE) return false
        val space = screen()
        val bounds = TriggerGeometry.bounds(space.width, space.height, space.density, space.insets, original.trigger, space.landscape)
        val geometry = PanelGeometry.calculate(space.usable, space.density, panel.settings.layout(space.landscape), panel.settings.buttons.size,
            bounds, original.trigger.placement(space.landscape).edge).wheel ?: return false
        val driver = WheelPickSession(geometry, target.wheelPick, space.density, view.touchX, view.touchY, SystemClock.uptimeMillis(), slot.ordinal < 3)
        val binding = WheelBinding(original, slot, target, stage, panel, driver)
        val captured = view.captureInteraction({ x, y, moving ->
            if (moving && wheelBinding === binding) {
                val old = driver.selected
                driver.move(x, y, SystemClock.uptimeMillis())
                if (old != driver.selected) {
                    if (driver.selected != null) haptics.perform(original.trigger.feedback(when (slot) {
                        GestureSlot.HOLD -> GestureKind.HOLD; GestureSlot.SWIPE_HOLD -> GestureKind.SWIPE_HOLD; else -> GestureKind.SWIPE }))
                    frames?.request()
                }
            }
        }) { cancelled -> finishWheel(binding, view.touchX, view.touchY, cancelled) }
        if (!captured) return false
        wheelBinding = binding
        driver.deadline?.let { deadline -> binding.armJob = scope?.launch {
            delay((deadline - SystemClock.uptimeMillis()).coerceAtLeast(0))
            if (wheelBinding === binding) driver.arm(SystemClock.uptimeMillis())
        } }
        panelTimeout?.touch(true)
        return true
    }
    private fun wheelValid(binding: WheelBinding): Boolean {
        val config = app.configuration.state.value ?: return false
        return app.editor.state.value == null && config.handle(binding.handle.id) == binding.handle && config.panel(binding.panel.id) == binding.panel &&
            reason(config, binding.handle) == HandleAvailability.ACTIVE && open?.let { it.panelId == binding.panel.id && it.handleId == binding.handle.id } == true
    }
    private fun finishWheel(binding: WheelBinding, x: Float, y: Float, cancelled: Boolean) {
        if (wheelBinding !== binding) return
        val valid = wheelValid(binding)
        val result = binding.session.finish(x, y, SystemClock.uptimeMillis(), cancelled || !valid)
        val button = result.index?.let { binding.panel.settings.buttons.getOrNull(it) }?.takeIf { it.action != ButtonAction.EMPTY }
        wheelBinding = null; binding.armJob?.cancel()
        dismissPanel(PanelCloseReason.WHEEL_RELEASED)
        clearPanelWindows()
        if (valid && result.kind == WheelFinishKind.NEXT_SWIPE) {
            sessions.submitted(binding.handle, binding.stage, binding.target.copy(panelId = null), SystemClock.elapsedRealtime())
        } else if (valid && result.kind == WheelFinishKind.PICK && button != null) {
            actions?.execute(binding.handle.id, button, testing = false)?.message?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        }
        frames?.request()
    }
    private fun cancelWheel() {
        val binding = wheelBinding ?: return
        wheelBinding = null; binding.armJob?.cancel()
        views[binding.handle.id]?.let { view ->
            binding.session.finish(view.touchX, view.touchY, SystemClock.uptimeMillis(), cancelled = true)
            view.cancelGesture()
        }
        sessions.reset(binding.handle.id)
    }
    private fun cancelSlider() {
        val old = sliderBinding ?: return
        sliderBinding = null
        views[old.handleId]?.cancelGesture()
        sessions.reset(old.handleId)
    }
    private fun beginSlider(handle: HandleConfig, slot: GestureSlot, target: GestureTarget, stage: Int?): Boolean {
        val view = views[handle.id] ?: return false
        val config = app.configuration.state.value ?: return false
        if (!view.isTouching || app.editor.state.value != null || reason(config, handle) != HandleAvailability.ACTIVE) return false
        val initial = if (target.volume) app.volume.read()?.fraction else app.brightness.fraction()
        if (initial == null) { Toast.makeText(this, R.string.volume_failed, Toast.LENGTH_SHORT).show(); return false }
        if (target.brightness) brightnessHost?.holdFromHandle(true)
        val binding = SliderBinding(handle.id, slot, target)
        val travel = (if (target.volume) handle.volumeTravelDp else handle.brightnessTravelDp) * resources.displayMetrics.density
        val guarded = slot in listOf(GestureSlot.SWIPE_1, GestureSlot.SWIPE_2, GestureSlot.SWIPE_3) && target.sliderActivation == SliderActivation.AFTER_HOLD
        val driver = SliderDragSession(view, initial, view.touchY, travel, write = { fraction ->
            if (target.volume) app.volume.setFraction(fraction) else {
                when (app.brightness.setFraction(fraction)) {
                    BrightnessResult.CHANGED, BrightnessResult.ALREADY_SET -> {
                        panelHost?.refreshSound()
                        val scale = app.brightness.calibration.scale
                        SliderApplied(app.brightness.fraction(), scale.quantum(scale.level(fraction)))
                    }
                    else -> null
                }
            }
        }, keepOpen = if (target.volume) ({ app.volume.read() != null && app.volume.showMedia() == VolumeResult.Requested }) else null, failed = {
            cancelSlider()
            if (target.brightness) hideBrightness()
            Toast.makeText(this, if (target.volume) R.string.volume_failed else if (!app.brightness.canWrite)
                R.string.brightness_access_required else R.string.brightness_failed, Toast.LENGTH_SHORT).show()
        }, startX = view.touchX, holdMs = if (guarded) target.sliderHoldMs else 0, slopPx = 8f * resources.displayMetrics.density,
            readOnArm = { if (target.volume) app.volume.read()?.fraction else app.brightness.fraction() },
            onArmed = { if (handle.trigger.feedback(GestureKind.SWIPE).enabled) view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK) })
        val captured = view.captureInteraction(driver::move) { cancelled ->
            val completed = driver.finish(cancelled)
            val owns = sliderBinding === binding
            if (owns) sliderBinding = null
            if (target.brightness) brightnessHost?.holdFromHandle(false)
            val current = app.configuration.state.value?.handle(handle.id)
            val panelOwned = target.panelId == null || open?.let { it.handleId == handle.id && it.panelId == target.panelId } == true
            val resetsSequence = target.action?.action in listOf(ButtonAction.CLOSE_PANELS, ButtonAction.SCREENSHOT)
            if (!completed || !owns || current?.target(slot) != target || !panelOwned || resetsSequence) sessions.reset(handle.id)
            else sessions.submitted(current, stage, target, SystemClock.elapsedRealtime())
        }
        if (!captured) { driver.finish(true); return false }
        sliderBinding = binding; driver.start()
        return true
    }
    private fun replacePanel(next: OpenPanel?, keepSession: String) {
        cancelWheel()
        panelTimeout?.stop(); open?.handleId?.takeIf { it != keepSession }?.let(sessions::reset)
        if (open != next) recordDismissal(PanelCloseReason.GESTURE_REPLACED)
        open = next; panelOpenedAt = next?.let { SystemClock.uptimeMillis() }
    }
    private fun dismissPanel(reason: PanelCloseReason, event: AccessibilityEvent? = null, previous: String? = foreground) {
        cancelWheel()
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
        if (wheelBinding != null) return
        val owner = open ?: return
        val config = (if (owner.testing) app.configuration.preview.value else app.configuration.state.value) ?: return
        val h = config.handle(owner.handleId)
        if (!owner.testing && (h == null || reason(config, h, ignorePause = true) != HandleAvailability.ACTIVE)) { dismissPanel(PanelCloseReason.HANDLE_UNAVAILABLE); refresh(); return }
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
        val selected = app.editor.state.value
        val config = (if (selected != null) app.configuration.preview.value else app.configuration.state.value) ?: return
        wheelBinding?.let { if (!wheelValid(it)) dismissPanel(PanelCloseReason.HANDLE_UNAVAILABLE) }
        sliderBinding?.let { binding ->
            val h = config.handle(binding.handleId)
            if (selected != null || h == null || h.target(binding.slot) != binding.target || reason(config, h) != HandleAvailability.ACTIVE) cancelSlider()
        }
        if (softHideForeground != foreground) { views.values.forEach { it.resetSoftHide() }; softHideForeground = foreground }
        val s = screen()
        val awake = getSystemService(PowerManager::class.java).isInteractive
        val locked = getSystemService(KeyguardManager::class.java).isKeyguardLocked
        val guideRequest = app.sliderGuidePreview.state.value
        val guideHandle = config.handle(guideRequest?.handleId)
        if (guideRequest != null && guideHandle != null && selected?.handleId == guideRequest.handleId && awake && !locked) {
            val fraction = if (guideRequest.kind == SliderKind.VOLUME) app.volume.read()?.fraction else app.brightness.fraction()
            if (fraction != null) sliderGuideHost?.show(guideRequest, guideHandle, s, fraction) else sliderGuideHost?.hide()
        } else sliderGuideHost?.hide()
        if (app.brightnessPreview.state.value && app.automaticCalibration.state.value.phase != AutomaticCalibrationPhase.RUNNING && !brightnessOpen && selected == null && awake && !locked) {
            brightnessOpen = false; brightnessOwner = null
            brightnessHost?.show(config.brightnessPopup, s, editing = true)
        } else if (brightnessOpen && awake && selected == null &&
            (brightnessOwner?.let { config.handle(it)?.let { h -> reason(config, h, ignorePause = true) == HandleAvailability.ACTIVE } } ?: !locked)) {
            if (brightnessHost?.show(config.brightnessPopup, s, editing = false) != true) hideBrightness()
        } else hideBrightness(animate = awake && !locked && selected == null)
        val preview = selected?.landscape?.takeIf { open?.testing != true }?.let { PreviewGeometry.calculate(s.width, s.height, s.insets, s.rotation, it) }
        val guideScreen = preview?.let { ScreenSpace(it.width, it.height, s.density, it.insets, it.landscape) } ?: s
        val editing = selected != null && getSystemService(PowerManager::class.java).isInteractive && !getSystemService(KeyguardManager::class.java).isKeyguardLocked
        val reasons = config.handles.associate { it.id to reason(config, it, s.landscape) }.toMutableMap()
        sessions.retain(reasons.filterValues { it == HandleAvailability.ACTIVE }.keys)
        val desired = linkedMapOf<String, HandleWindowSpec>()
        config.handles.asReversed().forEach { h ->
            val guide = editing && selected?.preview == true && selected.handleId == h.id
            if (guide || (selected == null && reasons[h.id] == HandleAvailability.ACTIVE)) {
                val space = if (guide) guideScreen else s
                val b = TriggerGeometry.bounds(space.width, space.height, space.density, space.insets, h.trigger, space.landscape)
                desired[h.id] = HandleWindowSpec(if (guide) preview?.toDisplay(b) ?: b else b, h.trigger, h.trigger.placement(space.landscape).edge, guide, if (guide) preview?.quarterTurns ?: 0 else 0,
                    hideIndicator = h.indicatorHidden && !guide, animationsEnabled = config.handleAnimationsEnabled)
            }
            if (selected != null) reasons[h.id] = HandleAvailability.EDITING
        }
        var wanted = open
        val closeReason = wanted?.let {
            when {
                config.panel(it.panelId) == null -> PanelCloseReason.PANEL_REMOVED
                it.testing && (!editing || selected?.preview != true || !selected.matches(it.handleId, it.panelId)) -> PanelCloseReason.TEST_ENDED
                !it.testing && selected != null -> PanelCloseReason.EDITOR_CHANGED
                !it.testing && config.handle(it.handleId)?.let { h -> reason(config, h, s.landscape, ignorePause = true) } != HandleAvailability.ACTIVE -> PanelCloseReason.HANDLE_UNAVAILABLE
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
                val animateExit = if (previous?.testing == true) editing && selected?.preview == true && selected.matches(previous.handleId, previous.panelId)
                    else selected == null && config.handle(previous?.handleId)?.let { reason(config, it, ignorePause = true) == HandleAvailability.ACTIVE } == true
                panelHost?.hide(animate = animateExit)
            }
            if (wanted != null) {
                val trigger = config.handle(wanted.handleId)?.trigger ?: TriggerSettings()
                val p = config.panel(wanted.panelId)!!.settings
                val space = if (panelEditing) guideScreen else s
                val edge = trigger.placement(space.landscape).edge
                val b = TriggerGeometry.bounds(space.width, space.height, space.density, space.insets, trigger, space.landscape)
                val geometry = PanelGeometry.calculate(space.usable, space.density, p.layout(space.landscape), p.buttons.size, b, edge)
                panelHost?.show(PanelSpec(p, p.layout(space.landscape), if (panelEditing && preview != null) geometry.copy(bounds = preview.toDisplay(geometry.bounds)) else geometry,
                    panelEditing, edge, if (panelEditing) preview?.quarterTurns ?: 0 else 0, wanted.handleId,
                    wheelBinding?.session?.selected ?: if (panelEditing && geometry.wheel != null && p.layout(space.landscape).wheel.expandSelection && geometry.visibleCount > 0) geometry.visibleCount / 2 else null,
                    wheelBinding != null))
            }
            attachedPanel = identity
            if (wanted != null) errors.remove("panel")
        } catch (e: RuntimeException) {
            app.diagnostics.record("Panel window failed", e); dismissPanel(PanelCloseReason.WINDOW_ERROR); attachedPanel = null
            try { panelHost?.hide(animate = false) } catch (_: RuntimeException) { }; errors["panel"] = getString(R.string.overlay_error)
        }
        val timedPanel = open?.let { config.panel(it.panelId)?.settings }
        panelTimeout?.configure(if (timedPanel?.autoCloseEnabled == true) timedPanel.autoCloseMs else 0)
        if (wheelBinding != null) panelTimeout?.touch(true)
        handleBounds = desired.filterValues { !it.editing }.mapValues { it.value.bounds }
        if (wanted == null) raiseHandles = false else if (changed) raiseHandles = true
        // Keep captured input and its return animation until the handle is idle.
        val raise = raiseHandles && views.values.none { it.hasInteraction }
        val failures = registry?.reconcile(desired, raise = raise).orEmpty()
        if (raise) raiseHandles = false
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
        val need = open != null || (config.enabled && config.handles.any { h -> h.enabled && h.id !in app.handlePauses.state.value &&
            (h.trigger.excludedPackages.isNotEmpty() || h.trigger.softHideEnabled || h.bindings.values.any { config.panel(it.panelId) != null }) })
        foregroundTracker?.track(need, SystemClock.uptimeMillis())
        val events = if (need) AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED else 0
        val info = serviceInfo ?: return
        if (info.eventTypes != events) { info.eventTypes = events; serviceInfo = info }
    }
    private fun runShortcut(id: String): Boolean {
        if (scope == null || !getSystemService(PowerManager::class.java).isInteractive || getSystemService(KeyguardManager::class.java).isKeyguardLocked) return false
        val config = app.configuration.state.value ?: return false
        val shortcut = config.shortcuts.firstOrNull { it.id == id } ?: return false
        if (!shortcut.available(config)) return false
        hideBrightness(); dismissPanel(PanelCloseReason.ACTION_COMPLETED); clearPanelWindows()
        val result = actions?.executeShortcut(shortcut) ?: return false
        result.message?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        refresh()
        return true
    }
    private fun showBrightness(handleId: String): Boolean {
        val config = app.configuration.state.value ?: return false
        val handle = config.handle(handleId) ?: return false
        if (app.editor.state.value != null || reason(config, handle, ignorePause = true) != HandleAvailability.ACTIVE) return false
        brightnessOpen = true; brightnessOwner = handleId
        val shown = brightnessHost?.show(config.brightnessPopup, screen(), editing = false) == true
        if (shown) brightnessHost?.restartTimeout() else hideBrightness()
        return shown
    }
    private fun hideBrightness(animate: Boolean = false) {
        if (sliderBinding?.target?.brightness == true) cancelSlider()
        brightnessOpen = false; brightnessOwner?.let(sessions::reset); brightnessOwner = null
        brightnessHost?.hide(animate)
    }
    private fun stopSession() {
        cancelSlider()
        raiseHandles = false
        sliderGuideHost?.hide(); sliderGuideHost = null
        app.shortcuts.unbind(this)
        app.accessibility.unbind(this)
        hideBrightness(); brightnessHost = null
        screenshots?.cancel(); screenshots = null
        systemPanels?.cancel(); systemPanels = null
        dismissPanel(PanelCloseReason.SERVICE_STOPPED)
        panelTimeout?.stop(); panelTimeout = null
        frames?.stop(); frames = null; scope?.cancel(); scope = null
        attachedPanel = null; sessions.clear(); app.unlocks.clear(); handleBounds = emptyMap(); foregroundTracker = null; softHideForeground = null
        try { panelHost?.hide(animate = false) } catch (e: RuntimeException) { app.diagnostics.record("Panel cleanup failed", e) }; panelHost = null
        registry?.clear()?.values?.forEach { app.diagnostics.record("Handle cleanup failed", it) }; registry = null
        views.clear(); errors.clear(); actions = null
        displays?.let { try { it.unregisterDisplayListener(displayListener) } catch (_: RuntimeException) { } }; displays = null
        if (registered) { registered = false; try { unregisterReceiver(screenReceiver) } catch (_: RuntimeException) { } }
        app.status.report(ServiceState())
    }
}
