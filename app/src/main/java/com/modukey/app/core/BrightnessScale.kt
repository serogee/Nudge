package com.modukey.app.core

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Slider fractions are perceptual; Settings.System stores linear backlight values.
 * Default conversion follows AOSP SettingsLib BrightnessUtils (Apache 2.0).
 * Calibration samples describe the native slider at 0, 25, 50, 75 and 100 percent.
 */
class BrightnessScale(val samples: List<Int>? = null, val minimum: Int = 1, val maximum: Int = 255) {
    init {
        require(maximum > minimum)
        require(samples == null || validSamples(samples))
    }
    fun level(fraction: Float): Int {
        val f = (if (fraction.isFinite()) fraction else .5f).coerceIn(0f, 1f)
        val points = samples
        if (points != null) {
            val position = f * 4
            val index = position.toInt().coerceAtMost(3)
            return (points[index] + (points[index + 1] - points[index]) * (position - index)).roundToInt()
        }
        val linear = if (f <= .5f) (f / .5f) * (f / .5f) else exp((f - .55991073f) / .17883277f) + .28466892f
        return (minimum + (maximum - minimum) * (linear / 12f).coerceIn(0f, 1f)).roundToInt()
    }
    fun fraction(level: Int): Float {
        val points = samples
        if (points != null) {
            val value = level.coerceIn(points.first(), points.last())
            val index = (0..3).first { value <= points[it + 1] }
            return (index + (value - points[index]).toFloat() / (points[index + 1] - points[index])) / 4f
        }
        if (level <= minimum) return 0f
        if (level >= maximum) return 1f
        val linear = ((level - minimum).toFloat() / (maximum - minimum)).coerceIn(0f, 1f) * 12f
        return (if (linear <= 1f) sqrt(linear) * .5f else .17883277f * ln(linear - .28466892f) + .55991073f).coerceIn(0f, 1f)
    }
    companion object {
        fun validSamples(values: List<Int>) = values.size == 5 && values.all { it in 0..65535 } && values.zipWithNext().all { (a, b) -> a < b }
    }
    fun quantum(level: Int) = maxOf(fraction(level + 1) - fraction(level), fraction(level) - fraction(level - 1)).coerceAtLeast(.000001f)
}
