package com.modukey.app.platform

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.PowerManager
import android.os.SystemClock
import android.view.Gravity
import android.view.WindowInsets
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.modukey.app.NudgeApplication
import com.modukey.app.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt

private data class OverlaySpec(val bounds: Bounds, val settings: TriggerSettings, val edge: Edge, val highlight: Boolean)

class EdgeAccessibilityService : AccessibilityService() {
    private val app get() = application as NudgeApplication
    private var scope: CoroutineScope? = null
    private lateinit var manager: WindowManager
    private var view: EdgeTriggerView? = null
    private var settings: TriggerSettings? = null
    private var foregroundPackage: String? = null
    private var registered = false
    private var overlay: SingleOverlay<OverlaySpec>? = null
    private var highlightJob: Job? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { refresh() }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Also handles a repeated bind callback without collecting twice or adding another window.
        stopSession()
        foregroundPackage = null
        manager = getSystemService(WindowManager::class.java)
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        overlay = SingleOverlay(object : OverlayHost<OverlaySpec> {
            override fun attach(value: OverlaySpec) {
                val candidate = EdgeTriggerView(this@EdgeAccessibilityService) { activate() }
                candidate.configure(value.settings, value.edge, value.highlight)
                manager.addView(candidate, params(value.bounds))
                view = candidate
            }
            override fun update(value: OverlaySpec) {
                view?.configure(value.settings, value.edge, value.highlight)
                manager.updateViewLayout(requireNotNull(view), params(value.bounds))
            }
            override fun detach() {
                view?.let { if (it.isAttachedToWindow) manager.removeViewImmediate(it) }
                view = null
            }
        })
        ContextCompat.registerReceiver(this, screenReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }, ContextCompat.RECEIVER_NOT_EXPORTED)
        registered = true
        app.status.report(ServiceState(connected = true))
        scope?.launch {
            try {
                app.settings.settings.collect { settings = it; refresh() }
            } catch (_: java.io.IOException) {
                overlay?.hide()
                app.status.report(ServiceState(connected = true, error = "Could not read saved settings. Reopen Nudge and try again."))
            }
        }
        highlightJob = scope?.launch {
            app.status.highlightUntil.collectLatest { until ->
                refresh()
                val remaining = until - SystemClock.elapsedRealtime()
                if (remaining > 0) {
                    delay(remaining)
                    refresh()
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        // System panels are not foreground app switches. Never inspect the window's view tree.
        val pkg = event.packageName?.toString() ?: return
        if (pkg != "com.android.systemui" && pkg != "android") foregroundPackage = pkg
        refresh()
    }

    override fun onConfigurationChanged(newConfig: Configuration) { super.onConfigurationChanged(newConfig); refresh() }
    override fun onInterrupt() { overlay?.hide(); app.status.report(ServiceState(connected = true)) }
    override fun onUnbind(intent: Intent?): Boolean { stopSession(); return super.onUnbind(intent) }
    override fun onDestroy() { stopSession(); super.onDestroy() }

    private fun activate() {
        // Recheck lock state even if a DOWN began just before the screen locked.
        if (!isAvailable()) { refresh(); return }
        when (val result = app.volume.show()) {
            VolumeResult.Requested -> Unit
            is VolumeResult.Failed -> app.status.report(app.status.state.value.copy(error = result.message))
        }
    }

    private fun isAvailable(): Boolean =
        getSystemService(PowerManager::class.java).isInteractive && !getSystemService(KeyguardManager::class.java).isKeyguardLocked

    private fun refresh() {
        val config = settings ?: return
        val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val show = TriggerVisibility.allowed(config, getSystemService(PowerManager::class.java).isInteractive,
            getSystemService(KeyguardManager::class.java).isKeyguardLocked, landscape, foregroundPackage)
        try {
            if (show) {
                val metrics = manager.currentWindowMetrics
                val safe = metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                val mandatory = metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.mandatorySystemGestures())
                val insets = Insets(safe.left.toFloat(), safe.top.toFloat(), safe.right.toFloat(), maxOf(safe.bottom, mandatory.bottom).toFloat())
                val bounds = TriggerGeometry.bounds(metrics.bounds.width().toFloat(), metrics.bounds.height().toFloat(), resources.displayMetrics.density, insets, config, landscape)
                overlay?.show(OverlaySpec(bounds, config, config.placement(landscape).edge, app.status.highlightUntil.value > SystemClock.elapsedRealtime()))
            } else overlay?.hide()
            app.status.report(ServiceState(connected = true, triggerAttached = show))
        } catch (_: RuntimeException) {
            overlay?.hide()
            app.status.report(ServiceState(connected = true, error = "HyperOS could not attach the edge trigger. Re-enable Nudge in Accessibility, then check Help."))
        }
    }

    private fun params(bounds: Bounds) = WindowManager.LayoutParams(
        bounds.width.roundToInt().coerceAtLeast(1), bounds.height.roundToInt().coerceAtLeast(1),
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.LEFT
        x = bounds.x.roundToInt()
        y = bounds.y.roundToInt()
        // Geometry already applies the full display's safe insets; avoid a second inset by WMS.
        setFitInsetsTypes(0)
        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        title = "Nudge edge shortcut"
    }

    private fun stopSession() {
        scope?.cancel()
        scope = null
        overlay?.hide()
        overlay = null
        if (registered) { unregisterReceiver(screenReceiver); registered = false }
        app.status.report(ServiceState())
    }
}
