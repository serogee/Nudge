package com.modukey.app.core

enum class HandleAvailability { ACTIVE, DISABLED, PAUSED, SCREEN_OFF, LOCKED, LANDSCAPE, EXCLUDED, EDITING, ERROR }
object HandleEligibility {
    fun reason(config: AppConfiguration, handle: HandleConfig, interactive: Boolean, locked: Boolean, landscape: Boolean, foreground: String?): HandleAvailability = when {
        !config.enabled -> HandleAvailability.PAUSED
        !handle.enabled -> HandleAvailability.DISABLED
        !interactive -> HandleAvailability.SCREEN_OFF
        locked && !handle.trigger.lockScreenEnabled -> HandleAvailability.LOCKED
        landscape && !handle.trigger.landscapeEnabled -> HandleAvailability.LANDSCAPE
        !locked && foreground in handle.trigger.excludedPackages -> HandleAvailability.EXCLUDED
        else -> HandleAvailability.ACTIVE
    }
}
