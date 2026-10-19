package com.modukey.app.core

enum class BrightnessPreset { MAXIMUM, MINIMUM }
data class BrightnessValue(val level: Int, val automatic: Boolean)
data class BrightnessChange(val preset: BrightnessPreset, val before: BrightnessValue, val after: BrightnessValue, val restoring: Boolean)

/** Only Nudge's own changes can retain restore values. Any observed external change clears them. */
class BrightnessSession {
    private val previous = mutableMapOf<BrightnessPreset, BrightnessChange>()
    private var expected: BrightnessValue? = null
    val tracking get() = previous.isNotEmpty()

    fun observe(value: BrightnessValue) {
        if (expected != null && value != expected) clear()
    }
    fun active(preset: BrightnessPreset, value: BrightnessValue): Boolean {
        observe(value)
        return previous[preset]?.after == value
    }
    fun change(preset: BrightnessPreset, current: BrightnessValue, minimum: Int, maximum: Int): BrightnessChange? {
        observe(current)
        val owned = previous[preset]?.takeIf { it.after == current }
        if (owned != null) return BrightnessChange(preset, current, owned.before, restoring = true)
        val after = BrightnessValue(if (preset == BrightnessPreset.MAXIMUM) maximum else minimum, automatic = false)
        return BrightnessChange(preset, current, after, restoring = false)
    }
    fun applied(change: BrightnessChange) {
        if (change.restoring) previous.remove(change.preset) else previous[change.preset] = change
        expected = change.after.takeIf { tracking }
    }
    fun clear() { previous.clear(); expected = null }
}
