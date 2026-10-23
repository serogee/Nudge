package com.modukey.app.platform

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build

object HyperOsSettings {
    internal fun isXiaomiDevice(manufacturer: String = Build.MANUFACTURER, brand: String = Build.BRAND) =
        listOf(manufacturer, brand).any { value -> listOf("xiaomi", "redmi", "poco").any { value.equals(it, ignoreCase = true) } }

    /** Xiaomi's optional OEM component is not part of the standard Android settings API. */
    fun openAutostart(context: Context): Boolean {
        val intent = Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"))
            .putExtra("package_name", context.packageName).putExtra("packageName", context.packageName)
        return try { context.startActivity(intent); true }
        catch (_: ActivityNotFoundException) { false }
        catch (_: SecurityException) { false }
    }
}
