package com.modukey.app.core

enum class BrightnessPreset { MAXIMUM, MINIMUM }
data class BrightnessValue(val level: Int, val automatic: Boolean)
data class BrightnessChange(val preset: BrightnessPreset, val before: BrightnessValue, val after: BrightnessValue, val restoring: Boolean)

/** Only Nudge's own changes can retain restore values. Any observed external change clears them. */
class BrightnessSession {
    private var owned: BrightnessChange? = null
    val tracking get() = owned != null

    fun observe(value: BrightnessValue) {
        if (owned != null && value != owned?.after) clear()
    }
    fun active(preset: BrightnessPreset, value: BrightnessValue): Boolean {
        observe(value)
        return owned?.preset == preset && owned?.after == value
    }
    fun change(preset: BrightnessPreset, current: BrightnessValue, minimum: Int, maximum: Int,
        settings: BrightnessReturnSettings = BrightnessReturnSettings(),
        returnLevel: Int = BrightnessScale(minimum = minimum, maximum = maximum).level(settings.normalized().fraction)): BrightnessChange {
        observe(current)
        val target = if (preset == BrightnessPreset.MAXIMUM) maximum else minimum
        val previous = owned?.takeIf { it.preset == preset && it.after == current }?.before
        if (previous != null || (preset == BrightnessPreset.MAXIMUM && !current.automatic && current.level == maximum)) {
            val options = settings.normalized()
            val available = previous != null && previous.level in minimum until maximum && previous.level != target
            val after = if (options.usePrevious && available) checkNotNull(previous)
                else BrightnessValue(returnLevel, automatic = false)
            return BrightnessChange(preset, current, after, restoring = true)
        }
        val after = BrightnessValue(target, automatic = false)
        return BrightnessChange(preset, current, after, restoring = false)
    }
    fun applied(change: BrightnessChange) {
        owned = change.takeUnless { it.restoring }
    }
    fun clear() { owned = null }
}
