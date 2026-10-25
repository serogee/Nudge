package com.modukey.app.platform

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings

/** Only the live Nudge handles service can disable itself. Enabling always needs Android settings. */
class AccessibilityControl(private val context: Context) {
    private var owner: Any? = null
    private var stop: (() -> Unit)? = null
    val enabled get() = stop != null || AccessibilityPermission.enabled(context)
    fun bind(token: Any, disable: () -> Unit) { owner = token; stop = disable }
    fun unbind(token: Any) { if (owner === token) { owner = null; stop = null } }
    fun toggle(): Boolean = setEnabled(!enabled)
    fun setEnabled(value: Boolean): Boolean {
        if (value) return openSettings()
        val disable = stop
        if (disable != null) {
            try { disable(); return true } catch (_: RuntimeException) { return openSettings() }
        }
        return !AccessibilityPermission.enabled(context) || openSettings()
    }
    private fun openSettings(): Boolean = try {
        val component = ComponentName(context, EdgeAccessibilityService::class.java)
        val key = ":settings:fragment_args_key"
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .putExtra(Intent.EXTRA_COMPONENT_NAME, component)
            .putExtra(key, component.flattenToString())
            .putExtra(":settings:show_fragment_args", Bundle().apply { putString(key, component.flattenToString()) })
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: android.content.ActivityNotFoundException) { false }
      catch (_: SecurityException) { false }
}
