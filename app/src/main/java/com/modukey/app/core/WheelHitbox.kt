package com.modukey.app.core

import kotlin.math.hypot

enum class WheelHitboxMode { AUTOMATIC, BUTTON_SIZE, MANUAL }

data class WheelHitboxSettings(val mode: WheelHitboxMode = WheelHitboxMode.AUTOMATIC,
    val innerRadiusDp: Float = 32f, val outerRadiusDp: Float = 240f) {
    fun normalized(): WheelHitboxSettings {
        val inner = (innerRadiusDp.takeIf(Float::isFinite) ?: 32f).coerceIn(0f, 592f)
        val outer = (outerRadiusDp.takeIf(Float::isFinite) ?: 240f).coerceIn(inner + 8f, 600f)
        return copy(innerRadiusDp = inner, outerRadiusDp = outer)
    }
}

/** Drag regions use completed logical geometry. Normal taps always retain the visible button bounds. */
internal fun WheelGeometryResult.pickHit(x: Float, y: Float, pick: WheelPickSettings, density: Float,
    hitbox: WheelHitboxSettings, selected: Int?): Int? {
    if (hitbox.mode == WheelHitboxMode.AUTOMATIC)
        return hit(x, y, pick.minimumDistanceDp * density, pick.maximumDistanceDp * density, selected)
    if (!x.isFinite() || !y.isFinite()) return null
    val distance = hypot(x - center.x, y - center.y)
    if (distance == 0f) return null // The exact center has no direction.
    val index = angularHit(x, y, selected) ?: return null
    val (inner, outer) = when (hitbox.mode) {
        WheelHitboxMode.BUTTON_SIZE -> slices(selected)[index].let { it.innerRadius to it.outerRadius }
        WheelHitboxMode.MANUAL -> hitbox.normalized().let { it.innerRadiusDp * density to it.outerRadiusDp * density }
        WheelHitboxMode.AUTOMATIC -> error("Automatic bounds were handled above")
    }
    return index.takeIf { distance in inner..outer }
}
