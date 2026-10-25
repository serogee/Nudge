package com.modukey.app.ui

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import com.modukey.app.core.Edge
import com.modukey.app.core.WheelGeometryResult
import com.modukey.app.core.WheelSlice
import kotlin.math.PI

/** Round only the four joins. A generic corner path effect also flattens circular arcs. */
internal fun wheelShape(geometry: WheelGeometryResult, slice: WheelSlice, corner: Float): Path {
    val inner = geometry.visibleInnerRadius(slice)
    val outer = slice.outerRadius
    val start = slice.start + geometry.sideInset(slice, outer, true)
    val end = slice.end - geometry.sideInset(slice, outer, false)
    val innerStart = slice.start + geometry.sideInset(slice, inner, true)
    val innerEnd = slice.end - geometry.sideInset(slice, inner, false)
    // The short inner arc must not reduce rounding at the independent outer corners.
    val requested = corner.takeIf(Float::isFinite)?.coerceAtLeast(0f) ?: 0f
    val outerRound = minOf(requested, (outer - inner) / 2f, outer * (end - start) / 4f).coerceAtLeast(0f)
    val innerRound = minOf(requested, (outer - inner) / 2f, inner * (innerEnd - innerStart) / 4f).coerceAtLeast(0f)
    val outerTrim = outerRound / outer.coerceAtLeast(.0001f)
    val innerTrim = innerRound / inner.coerceAtLeast(.0001f)
    val cx = geometry.center.x - geometry.bounds.x
    val cy = geometry.center.y - geometry.bounds.y
    val direction = if (geometry.edge == Edge.RIGHT) -1f else 1f
    fun degrees(angle: Float): Float {
        val degrees = angle * 180f / PI.toFloat()
        return when (geometry.edge) { Edge.LEFT -> degrees; Edge.RIGHT -> 180f - degrees; Edge.BOTTOM -> degrees - 90f }
    }
    fun rect(radius: Float) = Rect(cx - radius, cy - radius, cx + radius, cy + radius)
    return Path().apply {
        fun side(angle: Float, radius: Float, boundary: Float, distance: Float): com.modukey.app.core.WheelPoint {
            val p = geometry.point(angle, radius)
            val origin = geometry.point(boundary, 0f); val unit = geometry.point(boundary, 1f)
            return com.modukey.app.core.WheelPoint(p.x + (unit.x - origin.x) * distance, p.y + (unit.y - origin.y) * distance)
        }
        fun line(p: com.modukey.app.core.WheelPoint) = lineTo(p.x - geometry.bounds.x, p.y - geometry.bounds.y)
        fun join(angle: Float, radius: Float, to: com.modukey.app.core.WheelPoint) {
            val control = geometry.point(angle, radius)
            quadraticTo(control.x - geometry.bounds.x, control.y - geometry.bounds.y,
                to.x - geometry.bounds.x, to.y - geometry.bounds.y)
        }
        arcTo(rect(outer), degrees(start + outerTrim), (end - start - outerTrim * 2f) * 180f / PI.toFloat() * direction, true)
        join(end, outer, side(end, outer, slice.end, -outerRound))
        line(side(innerEnd, inner, slice.end, innerRound))
        join(innerEnd, inner, geometry.point(innerEnd - innerTrim, inner))
        if (innerEnd - innerStart > innerTrim * 2f + .00001f)
            arcTo(rect(inner), degrees(innerEnd - innerTrim), -(innerEnd - innerStart - innerTrim * 2f) * 180f / PI.toFloat() * direction, false)
        join(innerStart, inner, side(innerStart, inner, slice.start, innerRound))
        line(side(start, outer, slice.start, -outerRound))
        join(start, outer, geometry.point(start + outerTrim, outer))
        close()
    }
}
