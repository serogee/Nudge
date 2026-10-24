package com.modukey.app.core

enum class BrightnessSide { OPPOSITE_VOLUME, LEFT, RIGHT, CUSTOM }
enum class VolumeSide { LEFT, RIGHT }
enum class BrightnessTouchBehavior { JUMP_TO_TOUCH, SLIDE_TO_ADJUST }

/** System UI does not expose the volume window's bounds. These are adjustable starting dimensions. */
data class BrightnessPopupSettings(
    val side: BrightnessSide = BrightnessSide.OPPOSITE_VOLUME,
    val volumeSide: VolumeSide = VolumeSide.RIGHT,
    val widthDp: Float = 64f, val heightDp: Float = 240f,
    val x: Float = .5f, val y: Float = .65f,
    val background: Long = 0xFF202122, val track: Long = 0xFF3C3C3E,
    val fill: Long = 0xFF999A9C, val icon: Long = 0xFFECECEE,
    val backgroundOpacity: Float = 1f, val trackOpacity: Float = 1f,
    val fillOpacity: Float = 1f, val iconOpacity: Float = 1f,
    val timeoutMs: Long = 3_000,
    val cornerRadiusDp: Float = 32f, val showBackground: Boolean = true, val showAutomaticToggle: Boolean = true,
    val touchBehavior: BrightnessTouchBehavior = BrightnessTouchBehavior.SLIDE_TO_ADJUST,
    val animation: PanelAnimation = PanelAnimation.NONE,
    val closeAnimation: PanelAnimation = PanelAnimation.NONE,
) {
    fun normalized() = copy(touchBehavior = BrightnessTouchBehavior.SLIDE_TO_ADJUST, cornerRadiusDp = cornerRadiusDp.finite(32f).coerceIn(0f, 64f), widthDp = widthDp.finite(64f).coerceIn(48f, 128f), heightDp = heightDp.finite(240f).coerceIn(120f, 480f),
        x = x.finite(.5f).coerceIn(0f, 1f), y = y.finite(.65f).coerceIn(0f, 1f),
        background = background or 0xFF000000, track = track or 0xFF000000, fill = fill or 0xFF000000, icon = icon or 0xFF000000,
        backgroundOpacity = backgroundOpacity.finite(1f).coerceIn(0f, 1f), trackOpacity = trackOpacity.finite(1f).coerceIn(0f, 1f),
        fillOpacity = fillOpacity.finite(1f).coerceIn(0f, 1f), iconOpacity = iconOpacity.finite(1f).coerceIn(0f, 1f), timeoutMs = timeoutMs.coerceIn(1_000, 10_000))
}
object BrightnessPopupGeometry {
    fun automaticSpace(height: Float, density: Float, enabled: Boolean): Float =
        if (enabled) (56f * density).coerceAtMost((height - density).coerceAtLeast(0f)) else 0f

    fun calculate(usable: Bounds, density: Float, settings: BrightnessPopupSettings): Bounds {
        val s = settings.normalized()
        val w = (s.widthDp * density).coerceAtMost(usable.width)
        val h = (s.heightDp * density + (if (s.showAutomaticToggle) 56f * density else 0f)).coerceAtMost(usable.height)
        val sliderHeight = h - automaticSpace(h, density, s.showAutomaticToggle)
        val gap = (12f * density).coerceAtMost((usable.width - w).coerceAtLeast(0f) / 2f)
        val left = when (s.side) {
            BrightnessSide.LEFT -> true
            BrightnessSide.RIGHT -> false
            BrightnessSide.OPPOSITE_VOLUME -> s.volumeSide == VolumeSide.RIGHT
            BrightnessSide.CUSTOM -> null
        }
        val x = when (left) { true -> usable.x + gap; false -> usable.x + usable.width - w - gap
            null -> usable.x + s.x * usable.width - w / 2f }
        return Bounds(x.coerceIn(usable.x, usable.x + usable.width - w),
            (usable.y + (1f - s.y) * usable.height - sliderHeight / 2f).coerceIn(usable.y, usable.y + usable.height - h), w, h)
    }
}
private fun Float.finite(default: Float) = if (isFinite()) this else default
