package com.modukey.app.platform

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent

object HyperOsSettings {
    /** Xiaomi's optional OEM component is not part of the standard Android settings API. */
    fun openAutostart(context: Context): Boolean {
        val intent = Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"))
            .putExtra("package_name", context.packageName).putExtra("packageName", context.packageName)
        return try { context.startActivity(intent); true }
        catch (_: ActivityNotFoundException) { false }
        catch (_: SecurityException) { false }
    }
}
