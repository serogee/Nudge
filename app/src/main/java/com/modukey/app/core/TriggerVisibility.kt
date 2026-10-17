package com.modukey.app.core

object TriggerVisibility {
    fun allowed(settings: TriggerSettings, interactive: Boolean, locked: Boolean, landscape: Boolean, foregroundPackage: String?): Boolean =
        settings.enabled && interactive && (!locked || settings.lockScreenEnabled) && (!landscape || settings.landscapeEnabled) &&
            (locked || settings.excludedPackages.isEmpty() || (foregroundPackage != null && foregroundPackage !in settings.excludedPackages))

    fun editingAllowed(interactive: Boolean, locked: Boolean): Boolean = interactive && !locked
}
