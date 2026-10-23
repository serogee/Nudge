package com.modukey.app.core

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Slider fractions are perceptual; Settings.System stores linear backlight values.
 * Default conversion follows AOSP SettingsLib BrightnessUtils (Apache 2.0).
 * Calibration samples describe evenly spaced native slider positions: five manual or 17 automatic points.
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
            val position = f * points.lastIndex
            val index = position.toInt().coerceAtMost(points.lastIndex - 1)
            return (points[index] + (points[index + 1] - points[index]) * (position - index)).roundToInt()
        }
        val linear = if (f <= .5f) (f / .5f) * (f / .5f) else exp((f - .55991073f) / .17883277f) + .28466892f
        return (minimum + (maximum - minimum) * (linear / 12f).coerceIn(0f, 1f)).roundToInt()
    }
    fun fraction(level: Int): Float {
        val points = samples
        if (points != null) {
            val value = level.coerceIn(points.first(), points.last())
            if (value == points.first()) return 0f
            if (value == points.last()) return 1f
            // A dense native curve can contain equal adjacent hardware levels. Use the plateau midpoint.
            val exact = points.indexOf(value)
            if (exact >= 0) return (exact + points.lastIndexOf(value)) / (2f * points.lastIndex)
            val index = (0 until points.lastIndex).first { value < points[it + 1] }
            return (index + (value - points[index]).toFloat() / (points[index + 1] - points[index])) / points.lastIndex
        }
        if (level <= minimum) return 0f
        if (level >= maximum) return 1f
        val linear = ((level - minimum).toFloat() / (maximum - minimum)).coerceIn(0f, 1f) * 12f
        return (if (linear <= 1f) sqrt(linear) * .5f else .17883277f * ln(linear - .28466892f) + .55991073f).coerceIn(0f, 1f)
    }
    companion object {
        const val AUTOMATIC_SAMPLE_COUNT = 17
        fun validSamples(values: List<Int>) = values.all { it in 0..65535 } && when (values.size) {
            5 -> values.zipWithNext().all { (a, b) -> a < b }
            AUTOMATIC_SAMPLE_COUNT -> values.first() < values.last() && values.zipWithNext().all { (a, b) -> a <= b }
            else -> false
        }
    }
    fun quantum(level: Int) = maxOf(fraction(level + 1) - fraction(level), fraction(level) - fraction(level - 1)).coerceAtLeast(.000001f)
}
