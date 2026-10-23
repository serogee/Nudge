package com.modukey.app.platform

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.KeyguardManager
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.os.PowerManager
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.modukey.app.NudgeApplication
import com.modukey.app.R

/** Reads controls only during an explicit run, including when the user keeps the service enabled. */
class BrightnessCalibrationService : AccessibilityService(), CalibrationServicePort {
    private val coordinator get() = (application as NudgeApplication).automaticCalibration
    private var running = false
    private var stopping = false
    private var panelSession = false
    private var registered = false
    private var rotation = 0
    private var overlay: View? = null
    private var status: TextView? = null
    private val owners by lazy {
        buildSet {
            add(NativeBrightnessNode.SYSTEM_UI)
            // Plugin contexts can own Xiaomi controls. Do not accept a user-installed namesake.
            @Suppress("DEPRECATION")
            val plugin = try { packageManager.getApplicationInfo(NativeBrightnessNode.MIUI_PLUGIN, 0) }
                catch (_: PackageManager.NameNotFoundException) { null }
            if (plugin != null && plugin.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0)
                add(NativeBrightnessNode.MIUI_PLUGIN)
        }
    }
    private var lastDiscovery: String? = null
    private var discoveryReports = 0
    private var brightnessLabels: Set<String> = emptySet()
    private val manager get() = getSystemService(WindowManager::class.java)
    private val screenOff = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { if (!stopping) coordinator.cancel(AutomaticCalibrationError.LOCKED) }
    }
    override fun onServiceConnected() {
        super.onServiceConnected()
        restrict()
        if (!coordinator.connect(this)) disable()
    }
    private fun restrict() {
        serviceInfo = (serviceInfo ?: AccessibilityServiceInfo()).apply {
            eventTypes = 0; flags = 0; packageNames = owners.toTypedArray()
        }
    }
    @SuppressLint("DiscouragedApi") // The label belongs to the installed System UI package, not Nudge resources.
    private fun nativeBrightnessLabels(): Set<String> = owners.mapNotNull { owner ->
        try {
            val nativeResources = packageManager.getResourcesForApplication(owner)
            val id = nativeResources.getIdentifier("accessibility_brightness", "string", owner)
            if (id == 0) null else nativeResources.getString(id).trim().takeIf { it.isNotEmpty() }
        } catch (_: PackageManager.NameNotFoundException) { null }
        catch (_: android.content.res.Resources.NotFoundException) { null }
    }.toSet()
    override fun open(): Boolean {
        if (stopping || running) return false
        running = true
        lastDiscovery = null; discoveryReports = 0
        brightnessLabels = nativeBrightnessLabels()
        rotation = getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: 0
        serviceInfo = (serviceInfo ?: AccessibilityServiceInfo()).apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOWS_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            // Brightness identity can be on a non-interactive container around a generic SeekBar.
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            packageNames = owners.toTypedArray()
        }
        (application as NudgeApplication).diagnostics.record("Brightness calibration owners: ${owners.joinToString()}; nativeLabels=${brightnessLabels.size}")
        ContextCompat.registerReceiver(this, screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_NOT_EXPORTED)
        registered = true
        showProgress()
        panelSession = true
        // Xiaomi can route this global action to notifications. Let the user open either panel style.
        if (!HyperOsSettings.isXiaomiDevice()) performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
        return true
    }
    private fun roots(): List<AccessibilityNodeInfo> {
        if (!running || stopping) return emptyList()
        val roots = mutableListOf<AccessibilityNodeInfo>()
        @Suppress("DEPRECATION")
        fun accept(root: AccessibilityNodeInfo?, active: Boolean = false) {
            if (root == null) return
            // Some System UI panels are application windows. Inspect ownership before traversing a root.
            val duplicate = if (root.windowId >= 0) roots.any { it.windowId == root.windowId } else active && roots.isNotEmpty()
            if (root.packageName?.toString() in owners && !duplicate) roots += root else root.recycle()
        }
        windows.filter { it.type == AccessibilityWindowInfo.TYPE_SYSTEM || it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
            .forEach { accept(it.root) }
        // Older System UI builds can expose only the active root rather than interactive windows.
        accept(rootInActiveWindow, active = true)
        return roots
    }
    override fun control(): NativeBrightnessControl? {
        if (!running || stopping) return null
        return NativeBrightnessNode.read(roots(), owners, brightnessLabels) { detail ->
            if (detail != lastDiscovery) {
                lastDiscovery = detail
                // Polling is bounded, and only the final report plus a few changes need storage.
                if (discoveryReports++ < 4) (application as NudgeApplication).diagnostics.record("Brightness discovery: $detail")
            }
        }
    }
    override fun move(expected: NativeBrightnessControl, fraction: Float): Boolean = NativeBrightnessNode.move(roots(), expected, fraction, owners, brightnessLabels)
    override fun waitingForPanel() { status?.setText(R.string.auto_calibration_waiting_panel) }
    override fun progress(percent: Int) { status?.text = getString(R.string.auto_calibration_progress, percent) }
    private fun showProgress() {
        val density = resources.displayMetrics.density
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((16 * density).toInt(), (12 * density).toInt(), (16 * density).toInt(), (8 * density).toInt())
            background = GradientDrawable().apply { setColor(Color.rgb(32, 32, 32)); cornerRadius = 16 * density }
        }
        status = TextView(this).apply {
            text = getString(R.string.auto_calibration_opening); setTextColor(Color.WHITE); textSize = 16f
            maxWidth = (resources.displayMetrics.widthPixels - 64 * density).toInt().coerceAtLeast((200 * density).toInt())
        }
        layout.addView(status)
        layout.addView(Button(this).apply { setText(R.string.cancel); contentDescription = getString(R.string.cancel_auto_calibration)
            setOnClickListener { coordinator.cancel() } })
        val params = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL; y = (24 * density).toInt()
        }
        overlay = layout; manager.addView(layout, params)
    }
    private fun endSession(): Boolean {
        if (running) lastDiscovery?.let { (application as NudgeApplication).diagnostics.record("Brightness discovery final: $it") }
        running = false
        brightnessLabels = emptySet()
        if (registered) { runCatching { unregisterReceiver(screenOff) }; registered = false }
        overlay?.let { runCatching { manager.removeViewImmediate(it) } }; overlay = null; status = null
        val restricted = runCatching { restrict() }.isSuccess
        if (panelSession && getSystemService(PowerManager::class.java).isInteractive && !getSystemService(KeyguardManager::class.java).isKeyguardLocked)
            runCatching { performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE) }
        panelSession = false
        return restricted
    }
    override fun close() {
        if (stopping) return
        val restricted = endSession()
        if (!(application as NudgeApplication).calibrationOptions.keepServiceEnabled.value || !restricted) {
            stopping = true
            disableSelf()
        }
    }
    override fun disable() {
        if (stopping) return
        endSession()
        stopping = true
        disableSelf()
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() { coordinator.interrupted(this); close() }
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val current = getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: 0
        if (running && current != rotation) coordinator.cancel(AutomaticCalibrationError.CONTROL_CHANGED)
    }
    override fun onUnbind(intent: Intent?): Boolean { coordinator.disconnected(this); disable(); return super.onUnbind(intent) }
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!stopping) { if (coordinator.state.value.busy) coordinator.cancel(); close() }
        super.onTaskRemoved(rootIntent)
    }
    override fun onDestroy() { coordinator.disconnected(this); disable(); super.onDestroy() }
}
