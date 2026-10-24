package com.modukey.app.ui

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import com.modukey.app.core.Edge
import com.modukey.app.core.WheelGeometryResult
import com.modukey.app.core.WheelSlice
import kotlin.math.PI

/** Round only the four joins. A generic corner path effect also flattens circular arcs. */
internal fun wheelShape(geometry: WheelGeometryResult, slice: WheelSlice, corner: Float): Path {
    val inset = geometry.angularGap(slice) / 2f
    val start = slice.start + inset
    val end = slice.end - inset
    val inner = slice.innerRadius
    val outer = slice.outerRadius
    val rounding = corner.coerceIn(0f, minOf((outer - inner) / 2f, inner * (end - start) / 4f))
    val outerTrim = rounding / outer.coerceAtLeast(1f)
    val innerTrim = rounding / inner.coerceAtLeast(1f)
    val cx = geometry.center.x - geometry.bounds.x
    val cy = geometry.center.y - geometry.bounds.y
    val direction = if (geometry.edge == Edge.RIGHT) -1f else 1f
    fun degrees(angle: Float): Float {
        val degrees = angle * 180f / PI.toFloat()
        return when (geometry.edge) { Edge.LEFT -> degrees; Edge.RIGHT -> 180f - degrees; Edge.BOTTOM -> degrees - 90f }
    }
    fun rect(radius: Float) = Rect(cx - radius, cy - radius, cx + radius, cy + radius)
    return Path().apply {
        fun line(angle: Float, radius: Float) {
            val p = geometry.point(angle, radius)
            lineTo(p.x - geometry.bounds.x, p.y - geometry.bounds.y)
        }
        fun join(angle: Float, radius: Float, toAngle: Float, toRadius: Float) {
            val control = geometry.point(angle, radius)
            val to = geometry.point(toAngle, toRadius)
            quadraticTo(control.x - geometry.bounds.x, control.y - geometry.bounds.y,
                to.x - geometry.bounds.x, to.y - geometry.bounds.y)
        }
        arcTo(rect(outer), degrees(start + outerTrim), (end - start - outerTrim * 2f) * 180f / PI.toFloat() * direction, true)
        join(end, outer, end, outer - rounding)
        line(end, inner + rounding)
        join(end, inner, end - innerTrim, inner)
        arcTo(rect(inner), degrees(end - innerTrim), -(end - start - innerTrim * 2f) * 180f / PI.toFloat() * direction, false)
        join(start, inner, start, inner + rounding)
        line(start, outer - rounding)
        join(start, outer, start + outerTrim, outer)
        close()
    }
}
