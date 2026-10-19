package com.modukey.app.core

/** Saved per action. Previous values are runtime-only and never enter backups. */
data class BrightnessReturnSettings(val usePrevious: Boolean = true, val fraction: Float = .5f) {
    fun normalized() = copy(fraction = (if (fraction.isFinite()) fraction else .5f).coerceIn(0f, 1f))
}
