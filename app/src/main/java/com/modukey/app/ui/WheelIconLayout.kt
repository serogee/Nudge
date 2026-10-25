package com.modukey.app.ui

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import com.modukey.app.core.WheelGeometryResult
import com.modukey.app.core.WheelLayoutSettings
import com.modukey.app.core.WheelPoint
import com.modukey.app.core.WheelSlice
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sqrt

internal data class WheelIconLayout(val point: WheelPoint, val size: Float)

/** Fit a square's enclosing circle inside the actual rounded outline, with four dp of padding. */
internal fun wheelIconLayout(geometry: WheelGeometryResult, slice: WheelSlice, settings: WheelLayoutSettings,
    outline: Path, density: Float): WheelIconLayout {
    val s = settings.normalized()
    val d = density.coerceAtLeast(.01f)
    val tolerance = .1f * d
    val border = outline.asAndroidPath().approximate(tolerance)
    val angle = (slice.start + slice.end) / 2f
    val middle = (slice.innerRadius + slice.outerRadius) / 2f
    fun available(radius: Float): Float {
        val p = geometry.point(angle, radius)
        val x = p.x - geometry.bounds.x; val y = p.y - geometry.bounds.y
        var distance = Float.POSITIVE_INFINITY
        for (index in 3 until border.size step 3) {
            val ax = border[index - 2]; val ay = border[index - 1]
            val bx = border[index + 1]; val by = border[index + 2]
            val dx = bx - ax; val dy = by - ay
            val length = dx * dx + dy * dy
            val t = if (length == 0f) 0f else ((x - ax) * dx + (y - ay) * dy).div(length).coerceIn(0f, 1f)
            distance = min(distance, hypot(x - ax - t * dx, y - ay - t * dy))
        }
        return ((distance - 4f * d - tolerance * 2f) * sqrt(2f)).coerceAtLeast(0f)
    }
    // The endpoints retain a readable icon instead of putting its center on a boundary.
    val minimum = min(12f * d, available(middle))
    fun limit(inner: Boolean): Float {
        var low = if (inner) slice.innerRadius else middle
        var high = if (inner) middle else slice.outerRadius
        repeat(18) {
            val candidate = (low + high) / 2f
            if ((available(candidate) >= minimum) == inner) high = candidate else low = candidate
        }
        return if (inner) high else low
    }
    val radius = if (s.iconPosition <= .5f) {
        val near = limit(true)
        near + (middle - near) * s.iconPosition * 2f
    } else middle + (limit(false) - middle) * (s.iconPosition - .5f) * 2f
    val scale = slice.iconSize / (geometry.cell * .55f).coerceAtLeast(.01f)
    val requested = if (s.adaptIconSize) slice.iconSize else s.iconSizeDp * d * scale
    return WheelIconLayout(geometry.point(angle, radius), min(requested, available(radius)))
}
