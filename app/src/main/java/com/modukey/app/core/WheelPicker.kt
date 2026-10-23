package com.modukey.app.core

import kotlin.math.*

data class WheelLayoutSettings(val radiusDp: Float = 128f, val expandSelection: Boolean = true, val selectedScale: Float = 1.25f) {
    fun normalized() = copy(radiusDp = radiusDp.finite(128f).coerceIn(96f, 240f), selectedScale = selectedScale.finite(1.25f).coerceIn(1f, 1.5f))
}

data class WheelPickSettings(val enabled: Boolean = false, val minimumDistanceDp: Float = 32f, val maximumDistanceDp: Float = 240f,
    val allowLaterSwipes: Boolean = false, val releaseWindowMs: Long = 200) {
    fun normalized(): WheelPickSettings {
        val minimum = minimumDistanceDp.finite(32f).coerceIn(16f, 160f)
        return copy(minimumDistanceDp = minimum, maximumDistanceDp = maximumDistanceDp.finite(240f).coerceIn(maxOf(48f, minimum + 8f), 600f),
            releaseWindowMs = releaseWindowMs.coerceIn(100, 1_000))
    }
}

data class WheelPoint(val x: Float, val y: Float)
data class WheelSlice(val start: Float, val end: Float, val point: WheelPoint, val size: Float)

/** Angles increase from top to bottom on side handles and left to right on bottom handles. */
data class WheelGeometryResult(val bounds: Bounds, val center: WheelPoint, val edge: Edge, val radius: Float,
    val cell: Float, val capacity: Int, val count: Int, val expansion: Float, val minimumCell: Float) {
    fun point(angle: Float, distance: Float = radius): WheelPoint = when (edge) {
        Edge.LEFT -> WheelPoint(center.x + cos(angle) * distance, center.y + sin(angle) * distance)
        Edge.RIGHT -> WheelPoint(center.x - cos(angle) * distance, center.y + sin(angle) * distance)
        Edge.BOTTOM -> WheelPoint(center.x + sin(angle) * distance, center.y - cos(angle) * distance)
    }
    fun angle(x: Float, y: Float): Float = when (edge) {
        Edge.LEFT -> atan2(y - center.y, x - center.x)
        Edge.RIGHT -> atan2(y - center.y, center.x - x)
        Edge.BOTTOM -> atan2(x - center.x, center.y - y)
    }
    fun slices(selected: Int? = null): List<WheelSlice> {
        if (count == 0) return emptyList()
        val step = PI.toFloat() / count
        val widths = FloatArray(count) { step }
        if (selected != null && selected in widths.indices && count > 1) {
            val neighbors = listOf(selected - 1, selected + 1).filter { it in widths.indices }
            val extra = step * (expansion - 1f)
            widths[selected] += extra
            neighbors.forEach { widths[it] -= extra / neighbors.size }
        }
        var start = -PI.toFloat() / 2f
        return widths.mapIndexed { index, width ->
            val end = start + width
            WheelSlice(start, end, point((start + end) / 2f),
                if (index == selected) cell * expansion else (cell * (width / step)).coerceAtLeast(minimumCell)).also { start = end }
        }
    }
    fun hit(x: Float, y: Float, minimum: Float, maximum: Float, selected: Int? = null): Int? {
        if (!x.isFinite() || !y.isFinite()) return null
        val distance = hypot(x - center.x, y - center.y)
        if (distance < minimum || distance > maximum) return null
        val angle = angle(x, y)
        if (angle < -PI.toFloat() / 2f || angle > PI.toFloat() / 2f) return null
        return slices(selected).indexOfFirst { angle >= it.start && angle <= it.end }.takeIf { it >= 0 }
    }
}

object WheelGeometry {
    fun calculate(screen: Bounds, density: Float, layout: PanelLayout, settings: WheelLayoutSettings, count: Int, handle: Bounds, edge: Edge): WheelGeometryResult {
        val s = settings.normalized()
        val d = density.coerceAtLeast(.01f)
        val cell = layout.normalized().buttonSizeDp * d
        val expansion = if (s.expandSelection) s.selectedScale else 1f
        val margin = cell * expansion / 2f + 8f * d
        val side = edge != Edge.BOTTOM
        val radius = minOf(s.radiusDp * d, (if (side) screen.height / 2f else screen.width / 2f) - margin,
            (if (side) screen.width else screen.height) - margin * 2f).coerceAtLeast(0f)
        // Use the narrowest neighbor region at maximum expansion, even before a button is selected.
        val capacity = (6 downTo 1).firstOrNull { n ->
            val step = PI.toFloat() / n
            val narrow = if (n > 1) step * (2f - expansion) else step
            val normalSpace = 2f * radius * sin(step / 2f)
            val smallSpace = 2f * radius * sin(narrow / 2f)
            radius > 0f && (n == 1 || (normalSpace >= cell + 8f * d && smallSpace >= 56f * d))
        } ?: 0
        val width = if (side) radius + margin * 2f else (radius + margin) * 2f
        val height = if (side) (radius + margin) * 2f else radius + margin * 2f
        val localX = when (edge) { Edge.LEFT -> margin; Edge.RIGHT -> radius + margin; Edge.BOTTOM -> radius + margin }
        val localY = if (side) radius + margin else radius + margin
        var cx = screen.x + layout.x * screen.width
        var cy = screen.y + layout.y * screen.height
        if (layout.followHandle) {
            cx = when (edge) { Edge.LEFT -> handle.x + handle.width; Edge.RIGHT -> handle.x; Edge.BOTTOM -> handle.x + handle.width / 2f } + layout.offsetXDp * d
            cy = (if (side) handle.y + handle.height / 2f else handle.y) - layout.offsetYDp * d
        }
        val x = (cx - localX).coerceIn(screen.x, screen.x + (screen.width - width).coerceAtLeast(0f))
        val y = (cy - localY).coerceIn(screen.y, screen.y + (screen.height - height).coerceAtLeast(0f))
        return WheelGeometryResult(Bounds(x, y, width.coerceAtMost(screen.width), height.coerceAtMost(screen.height)),
            WheelPoint(x + localX, y + localY), edge, radius, cell, capacity, count.coerceIn(0, capacity), expansion, 48f * d)
    }
}

enum class WheelFinishKind { PICK, NEXT_SWIPE, CANCEL }
data class WheelFinish(val kind: WheelFinishKind, val index: Int? = null)

/** One touch owns one picker. Release may clear a highlight, but cannot create one. */
class WheelPickSession(val geometry: WheelGeometryResult, settings: WheelPickSettings, private val density: Float,
    startX: Float, startY: Float, now: Long, allowQuickRelease: Boolean) {
    private val settings = settings.normalized()
    val deadline: Long? = if (allowQuickRelease && this.settings.allowLaterSwipes) now + this.settings.releaseWindowMs else null
    private var anchor = WheelPoint(startX, startY)
    private var latest = anchor
    private var armed = deadline == null
    private var moved = false
    private var active = true
    var selected: Int? = null; private set
    fun arm(now: Long) {
        if (active && !armed && now >= requireNotNull(deadline)) { armed = true; anchor = latest }
    }
    fun move(x: Float, y: Float, now: Long) {
        if (!active || !x.isFinite() || !y.isFinite()) return
        latest = WheelPoint(x, y)
        arm(now)
        if (!armed) return
        if (!moved && hypot(x - anchor.x, y - anchor.y) <= 8f * density) return
        moved = true
        selected = geometry.hit(x, y, settings.minimumDistanceDp * density, settings.maximumDistanceDp * density, selected)
    }
    fun finish(x: Float, y: Float, now: Long, cancelled: Boolean = false): WheelFinish {
        if (!active) return WheelFinish(WheelFinishKind.CANCEL)
        active = false
        if (cancelled) return WheelFinish(WheelFinishKind.CANCEL)
        if (deadline != null && now < deadline) return WheelFinish(WheelFinishKind.NEXT_SWIPE)
        val pick = selected?.takeIf { geometry.hit(x, y, settings.minimumDistanceDp * density, settings.maximumDistanceDp * density, it) == it }
        return if (pick != null) WheelFinish(WheelFinishKind.PICK, pick) else WheelFinish(WheelFinishKind.CANCEL)
    }
}

private fun Float.finite(default: Float) = if (isFinite()) this else default
