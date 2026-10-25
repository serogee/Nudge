package com.modukey.app.core

import kotlin.math.hypot

enum class WheelHitboxMode { AUTOMATIC, BUTTON_SIZE, MANUAL }

data class WheelHitboxSettings(val mode: WheelHitboxMode = WheelHitboxMode.AUTOMATIC,
    val innerRadiusDp: Float = 32f, val outerRadiusDp: Float = 240f) {
    fun withMode(next: WheelHitboxMode, reference: Pair<Float, Float>): WheelHitboxSettings =
        if (next == WheelHitboxMode.MANUAL && mode != next)
            copy(mode = next, innerRadiusDp = reference.first, outerRadiusDp = reference.second).normalized()
        else copy(mode = next)
    fun normalized(): WheelHitboxSettings {
        val inner = (innerRadiusDp.takeIf(Float::isFinite) ?: 32f).coerceIn(0f, 592f)
        val outer = (outerRadiusDp.takeIf(Float::isFinite) ?: 240f).coerceIn(inner + 8f, 600f)
        return copy(innerRadiusDp = inner, outerRadiusDp = outer)
    }
}

/** The radial selection fan has no visual item gaps. These points are in logical screen coordinates. */
fun WheelGeometryResult.hitboxPoints(radii: Pair<Float, Float>): List<WheelPoint> {
    val inner = radii.first.coerceAtLeast(0f); val outer = radii.second.coerceAtLeast(inner)
    val half = kotlin.math.PI.toFloat() / 2f
    return (0..64).map { point(-half + 2f * half * it / 64f, outer) } +
        (64 downTo 0).map { point(-half + 2f * half * it / 64f, inner) }
}

/** Fitted Wheels keep a short neutral center even when fitting enlarges the visual hole. */
fun WheelGeometryResult.hitboxRadii(pick: WheelPickSettings, density: Float, hitbox: WheelHitboxSettings,
    selected: Int? = null, index: Int? = selected): Pair<Float, Float> = when (hitbox.mode) {
    WheelHitboxMode.AUTOMATIC -> if (autoFit) WheelLayoutSettings.MIN_AUTOFIT_INNER_RADIUS_DP * density to
        (slices(selected).getOrNull(index ?: -1)?.outerRadius ?: (innerRadius + cell))
        else maxOf(pick.minimumDistanceDp * density, innerRadius - minOf(innerRadius * .2f, 12f * density)) to pick.maximumDistanceDp * density
    WheelHitboxMode.BUTTON_SIZE -> innerRadius to (slices(selected).getOrNull(index ?: -1)?.outerRadius ?: (innerRadius + cell))
    WheelHitboxMode.MANUAL -> hitbox.normalized().let { it.innerRadiusDp * density to it.outerRadiusDp * density }
}

/** Drag regions use completed logical geometry. Normal taps always retain the visible button bounds. */
internal fun WheelGeometryResult.pickHit(x: Float, y: Float, pick: WheelPickSettings, density: Float,
    hitbox: WheelHitboxSettings, selected: Int?): Int? {
    if (hitbox.mode == WheelHitboxMode.AUTOMATIC && !autoFit)
        return hit(x, y, pick.minimumDistanceDp * density, pick.maximumDistanceDp * density, selected)
    if (!x.isFinite() || !y.isFinite()) return null
    val distance = hypot(x - center.x, y - center.y)
    if (distance == 0f) return null // The exact center has no direction.
    val index = angularHit(x, y, selected) ?: return null
    val (inner, outer) = hitboxRadii(pick, density, hitbox, selected, index)
    return index.takeIf { distance in inner..outer }
}
