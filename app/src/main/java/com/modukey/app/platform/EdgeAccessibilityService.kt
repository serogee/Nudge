package com.modukey.app.platform

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.*
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.PowerManager
import android.view.*
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.modukey.app.NudgeApplication
import com.modukey.app.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filterNotNull
import kotlin.math.roundToInt

private data class OverlaySpec(val bounds: Bounds, val settings: TriggerSettings, val edge: Edge, val editing: Boolean)

class EdgeAccessibilityService : AccessibilityService() {
    private val app get() = application as NudgeApplication
    private var scope: CoroutineScope? = null
    private lateinit var manager: WindowManager
    private var view: EdgeTriggerView? = null
    private var lastParams: WindowManager.LayoutParams? = null
    private var settings: TriggerSettings? = null
    private var foregroundPackage: String? = null
    private var registered = false
    private var overlay: SingleOverlay<OverlaySpec>? = null
    private var frames: FrameCoalescer? = null
    private var windowFailed = false
    private val frameCallback = Choreographer.FrameCallback { frames?.onFrame() }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            view?.cancelGesture()
            // Screen transitions must hide immediately, even before the next display frame.
            refresh()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val cleanupError = stopSession()
        if (cleanupError != null) {
            app.status.report(ServiceState(connected = true, error = cleanupError))
            return
        }
        windowFailed = false
        app.diagnostics.record("Accessibility connected")
        foregroundPackage = null
        manager = getSystemService(WindowManager::class.java)
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val choreographer = Choreographer.getInstance()
        frames = FrameCoalescer({ choreographer.postFrameCallback(frameCallback) },
            { choreographer.removeFrameCallback(frameCallback) }, ::refresh)
        overlay = SingleOverlay(object : OverlayHost<OverlaySpec> {
            override fun attach(value: OverlaySpec) {
                val candidate = EdgeTriggerView(this@EdgeAccessibilityService) { activate() }
                candidate.configure(value.settings, value.edge, value.editing)
                val layout = params(value.bounds, value.editing)
                view = candidate
                try {
                    manager.addView(candidate, layout)
                    lastParams = layout
                } catch (error: RuntimeException) {
                    try { detach() } catch (cleanup: RuntimeException) { app.diagnostics.record("Attach cleanup failed", cleanup) }
                    throw error
                }
            }
            override fun update(value: OverlaySpec) {
                val target = requireNotNull(view)
                target.configure(value.settings, value.edge, value.editing)
                val layout = params(value.bounds, value.editing)
                val previous = lastParams
                if (previous == null || previous.x != layout.x || previous.y != layout.y ||
                    previous.width != layout.width || previous.height != layout.height || previous.flags != layout.flags) {
                    target.cancelGesture()
                    manager.updateViewLayout(target, layout)
                    lastParams = layout
                }
            }
            override fun detach() {
                // Clear ownership first. A removal exception cannot lead to recursive cleanup.
                val candidate = view
                view = null
                lastParams = null
                candidate?.cancelGesture()
                if (candidate?.isAttachedToWindow == true) manager.removeViewImmediate(candidate)
            }
        })
        try {
            ContextCompat.registerReceiver(this, screenReceiver, IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }, ContextCompat.RECEIVER_NOT_EXPORTED)
            registered = true
            app.status.report(ServiceState(connected = true))
            scope?.launch {
                app.settings.state.filterNotNull().collect {
                    settings = it
                    try { updateWindowEventSubscription(it) }
                    catch (error: RuntimeException) { app.diagnostics.record("Window-event subscription failed", error) }
                    frames?.request()
                }
            }
            scope?.launch {
                app.customization.active.collect {
                    view?.cancelGesture()
                    if (it) frames?.request() else refresh()
                }
            }
        } catch (error: RuntimeException) {
            app.diagnostics.record("Accessibility setup failed", error)
            stopSession()
            app.status.report(ServiceState(connected = true, error = "Could not initialize the trigger. Re-enable Nudge in Accessibility."))
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (settings?.enabled != true || settings?.excludedPackages.isNullOrEmpty()) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == "com.android.systemui" || pkg == "android" || pkg == foregroundPackage) return
        foregroundPackage = pkg
        frames?.request()
    }

    private fun updateWindowEventSubscription(config: TriggerSettings) {
        val requested = if (config.enabled && config.excludedPackages.isNotEmpty()) AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED else 0
        val info = serviceInfo ?: return
        if (info.eventTypes == requested) return
        foregroundPackage = null
        info.eventTypes = requested
        serviceInfo = info
    }

    override fun onConfigurationChanged(newConfig: Configuration) { super.onConfigurationChanged(newConfig); view?.cancelGesture(); frames?.request() }
    override fun onInterrupt() {
        app.diagnostics.record("Accessibility interrupted")
        view?.cancelGesture()
        refresh()
    }
    override fun onUnbind(intent: Intent?): Boolean {
        app.diagnostics.record("Accessibility unbound")
        stopSession()
        return super.onUnbind(intent)
    }
    override fun onDestroy() {
        app.diagnostics.record("Accessibility destroyed")
        stopSession()
        super.onDestroy()
    }
    override fun onTaskRemoved(rootIntent: Intent?) {
        app.diagnostics.record("Task removed; accessibility lifetime remains system-managed")
        super.onTaskRemoved(rootIntent)
    }

    private fun eligible(config: TriggerSettings): Boolean = TriggerVisibility.allowed(
        config, getSystemService(PowerManager::class.java).isInteractive,
        getSystemService(KeyguardManager::class.java).isKeyguardLocked,
        resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE, foregroundPackage,
    )

    private fun activate() {
        val config = app.settings.state.value ?: return
        if (app.customization.active.value || !eligible(config)) { refresh(); return }
        when (val result = app.volume.show()) {
            VolumeResult.Requested -> if (config.haptics) view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM) else Unit
            is VolumeResult.Failed -> {
                app.diagnostics.record("Native volume request failed: ${result.message}")
                app.status.report(app.status.state.value.copy(error = result.message))
            }
        }
    }

    private fun refresh() {
        if (scope == null || windowFailed) return
        val config = app.settings.state.value ?: return
        val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val editing = app.customization.active.value && TriggerVisibility.editingAllowed(
            getSystemService(PowerManager::class.java).isInteractive, getSystemService(KeyguardManager::class.java).isKeyguardLocked)
        val show = editing || eligible(config)
        try {
            if (show) {
                val metrics = manager.currentWindowMetrics
                val safe = metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                val mandatory = metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.mandatorySystemGestures())
                val insets = Insets(safe.left.toFloat(), safe.top.toFloat(), safe.right.toFloat(), maxOf(safe.bottom, mandatory.bottom).toFloat())
                val bounds = TriggerGeometry.bounds(metrics.bounds.width().toFloat(), metrics.bounds.height().toFloat(), resources.displayMetrics.density, insets, config, landscape)
                if (bounds.width <= 0f || bounds.height <= 0f) { hideSafely(); app.status.report(ServiceState(connected = true)); return }
                overlay?.show(OverlaySpec(bounds, config, config.placement(landscape).edge, editing))
            } else {
                val error = hideSafely()
                app.status.report(ServiceState(connected = true, error = error))
                return
            }
            app.status.report(ServiceState(connected = true, triggerAttached = true))
        } catch (error: RuntimeException) {
            windowFailed = true
            app.diagnostics.record("Overlay update failed", error)
            hideSafely()
            app.status.report(ServiceState(connected = true, error = "Could not display the edge trigger. Re-enable Nudge in Accessibility and check Help. Details are in local diagnostics."))
        }
    }

    private fun hideSafely(): String? = try { overlay?.hide(); null }
        catch (error: RuntimeException) {
            windowFailed = true
            app.diagnostics.record("Overlay removal failed", error)
            "Could not remove the trigger window. Re-enable Nudge in Accessibility."
        }

    @SuppressLint("RtlHardcoded") // Physical display coordinates, independent of language direction.
    private fun params(bounds: Bounds, editing: Boolean) = WindowManager.LayoutParams(
        bounds.width.roundToInt().coerceAtLeast(1), bounds.height.roundToInt().coerceAtLeast(1),
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or (if (editing) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else 0),
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.LEFT
        x = bounds.x.roundToInt()
        y = bounds.y.roundToInt()
        setFitInsetsTypes(0)
        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        title = "Nudge edge shortcut"
    }

    private fun stopSession(): String? {
        frames?.stop()
        frames = null
        scope?.cancel()
        scope = null
        val removalError = hideSafely()
        overlay = null
        settings = null
        if (registered) {
            registered = false
            try { unregisterReceiver(screenReceiver) }
            catch (error: RuntimeException) { app.diagnostics.record("Receiver cleanup failed", error) }
        }
        app.status.report(ServiceState(error = removalError))
        return removalError
    }
}
