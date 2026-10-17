package com.modukey.app.platform

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

object AccessibilityPermission {
    fun enabled(context: Context): Boolean = contains(
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
        ComponentName(context, EdgeAccessibilityService::class.java),
    )

    internal fun contains(enabledComponents: String?, expected: ComponentName): Boolean =
        enabledComponents?.split(':')?.any { ComponentName.unflattenFromString(it) == expected } == true
}
