package com.modukey.app.platform

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.KeyguardManager
import android.content.*
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

/** Separately enabled for an explicit run. Normal handles never receive window-content access. */
class BrightnessCalibrationService : AccessibilityService(), CalibrationServicePort {
    private val coordinator get() = (application as NudgeApplication).automaticCalibration
    private var running = false
    private var stopping = false
    private var opened = false
    private var registered = false
    private var rotation = 0
    private var overlay: View? = null
    private var status: TextView? = null
    private val manager get() = getSystemService(WindowManager::class.java)
    private val screenOff = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { if (!stopping) coordinator.cancel(AutomaticCalibrationError.LOCKED) }
    }
    override fun onServiceConnected() {
        super.onServiceConnected()
        restrict()
        if (!coordinator.connect(this)) close()
    }
    private fun restrict() {
        serviceInfo = (serviceInfo ?: AccessibilityServiceInfo()).apply {
            eventTypes = 0; flags = 0; packageNames = arrayOf(NativeBrightnessNode.SYSTEM_UI)
        }
    }
    override fun open(): Boolean {
        if (stopping || running) return false
        running = true
        rotation = getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: 0
        serviceInfo = (serviceInfo ?: AccessibilityServiceInfo()).apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOWS_CHANGED
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            packageNames = arrayOf(NativeBrightnessNode.SYSTEM_UI)
        }
        ContextCompat.registerReceiver(this, screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_NOT_EXPORTED)
        registered = true
        showProgress()
        opened = performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
        return opened
    }
    private fun roots(): List<AccessibilityNodeInfo> {
        if (!running || stopping) return emptyList()
        // Do not request application-window roots, including Nudge's own cancel overlay.
        return windows.filter { it.type == AccessibilityWindowInfo.TYPE_SYSTEM }.mapNotNull { it.root }
    }
    override fun control(): NativeBrightnessControl? = NativeBrightnessNode.read(roots())
    override fun move(expected: NativeBrightnessControl, fraction: Float): Boolean = NativeBrightnessNode.move(roots(), expected, fraction)
    override fun progress(percent: Int) { status?.text = getString(R.string.auto_calibration_progress, percent) }
    private fun showProgress() {
        val density = resources.displayMetrics.density
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((16 * density).toInt(), (12 * density).toInt(), (16 * density).toInt(), (8 * density).toInt())
            background = GradientDrawable().apply { setColor(Color.rgb(32, 32, 32)); cornerRadius = 16 * density }
        }
        status = TextView(this).apply { text = getString(R.string.auto_calibration_opening); setTextColor(Color.WHITE); textSize = 16f }
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
    override fun close() {
        if (stopping) return
        stopping = true; running = false
        if (registered) { runCatching { unregisterReceiver(screenOff) }; registered = false }
        overlay?.let { runCatching { manager.removeViewImmediate(it) } }; overlay = null; status = null
        runCatching { restrict() }
        if (opened && getSystemService(PowerManager::class.java).isInteractive && !getSystemService(KeyguardManager::class.java).isKeyguardLocked)
            runCatching { performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE) }
        opened = false
        disableSelf()
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() { coordinator.disconnected(this); close() }
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val current = getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: 0
        if (running && current != rotation) coordinator.cancel(AutomaticCalibrationError.CONTROL_CHANGED)
    }
    override fun onUnbind(intent: Intent?): Boolean { coordinator.disconnected(this); close(); return super.onUnbind(intent) }
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!stopping) { coordinator.cancel(); close() }
        super.onTaskRemoved(rootIntent)
    }
    override fun onDestroy() { coordinator.disconnected(this); close(); super.onDestroy() }
}
