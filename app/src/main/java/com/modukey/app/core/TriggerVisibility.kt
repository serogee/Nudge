package com.modukey.app.core

object TriggerVisibility {
    fun allowed(settings: TriggerSettings, interactive: Boolean, locked: Boolean, landscape: Boolean, foregroundPackage: String?): Boolean =
        settings.enabled && interactive && !locked && (!landscape || settings.landscapeEnabled) &&
            (settings.excludedPackages.isEmpty() || (foregroundPackage != null && foregroundPackage !in settings.excludedPackages))
}
