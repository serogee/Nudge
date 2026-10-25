package com.modukey.app.core

import kotlin.math.*

enum class WheelFocusAnimation { NONE, SMOOTH, SPRING }

data class WheelLayoutSettings(val radiusDp: Float = 128f, val expandSelection: Boolean = true, val selectedScale: Float = 1.25f,
    val animationsEnabled: Boolean = true, val innerRadiusDp: Float? = null, val outerRadiusDp: Float? = null,
    val focusAnimation: WheelFocusAnimation = WheelFocusAnimation.SMOOTH, val focusDurationMs: Int = 140,
    val autoFit: Boolean = true, val adaptIconSize: Boolean = true, val iconSizeDp: Float = 32f,
    val iconPosition: Float = .5f) {
    companion object { const val MIN_INNER_RADIUS_DP = 1f }
    fun normalized(): WheelLayoutSettings {
        val inner = innerRadiusDp?.finite(100f)?.coerceIn(MIN_INNER_RADIUS_DP, 288f)
        val outer = outerRadiusDp?.finite(156f)?.coerceIn((inner ?: MIN_INNER_RADIUS_DP) + 48f, 336f)
        return copy(radiusDp = radiusDp.finite(128f).coerceIn(40f, 240f), selectedScale = selectedScale.finite(1.25f).coerceIn(1f, 1.5f),
            innerRadiusDp = inner?.coerceAtMost((outer ?: 336f) - 48f), outerRadiusDp = outer, focusDurationMs = focusDurationMs.coerceIn(80, 400),
            iconSizeDp = iconSizeDp.finite(32f).coerceIn(12f, 128f), iconPosition = iconPosition.finite(.5f).coerceIn(0f, 1f))
    }
    /** Missing bounds retain the old center-radius and button-size geometry on import. */
    fun radii(buttonSizeDp: Float): Pair<Float, Float> {
        val s = normalized()
        val inner = (s.innerRadiusDp ?: maxOf(12f, s.radiusDp - buttonSizeDp / 2f)).coerceAtMost((s.outerRadiusDp ?: 336f) - 48f)
        return inner to (s.outerRadiusDp ?: (inner + buttonSizeDp))
    }
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
data class WheelSlice(val start: Float, val end: Float, val point: WheelPoint, val size: Float,
    val innerRadius: Float, val outerRadius: Float, val iconSize: Float)

/** Angles increase from top to bottom on side handles and left to right on bottom handles. */
data class WheelGeometryResult(val bounds: Bounds, val center: WheelPoint, val edge: Edge, val radius: Float,
    val cell: Float, val capacity: Int, val count: Int, val expansion: Float, val minimumCell: Float,
    val innerRadius: Float = (radius - cell / 2f).coerceAtLeast(0f), val gap: Float = 4f) {
    val outerRadius get() = innerRadius + cell * expansion
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
    fun slices(selected: Int? = null): List<WheelSlice> = slices(List(count) { if (it == selected) 1f else 0f })

    /** Animation uses the same geometry, but selection always uses the completed target regions. */
    fun slices(focus: List<Float>): List<WheelSlice> {
        if (count == 0) return emptyList()
        val step = PI.toFloat() / count
        val widths = FloatArray(count) { step }
        val weights = List(count) { focus.getOrElse(it) { 0f }.finite(0f).coerceIn(0f, 1f) }
        val totalFocus = weights.sum().coerceAtMost(1f)
        if (count > 1) for (index in widths.indices) {
            val neighbors = listOf(index - 1, index + 1).filter { it in widths.indices }
            val extra = step * (expansion - 1f) * weights[index]
            widths[index] += extra
            neighbors.forEach { widths[it] -= extra / neighbors.size }
        }
        var start = -PI.toFloat() / 2f
        return widths.mapIndexed { index, width ->
            val end = start + width
            val scale = 1f + (expansion - 1f) * weights[index] - minOf(.2f, expansion - 1f) * (totalFocus - weights[index])
            val depth = (cell * scale).coerceAtLeast(minimumCell)
            WheelSlice(start, end, point((start + end) / 2f, innerRadius + depth / 2f), depth,
                innerRadius, innerRadius + depth, cell * .55f * scale).also { start = end }
        }
    }
    fun angularGap(slice: WheelSlice) = (gap / (slice.innerRadius + slice.size / 2f).coerceAtLeast(1f)).coerceAtMost((slice.end - slice.start) / 4f)
    fun segmentPoints(slice: WheelSlice): List<WheelPoint> {
        val inset = angularGap(slice) / 2f
        val start = slice.start + inset; val end = slice.end - inset
        return (0..48).map { point(start + (end - start) * it / 48f, slice.outerRadius) } +
            (48 downTo 0).map { point(start + (end - start) * it / 48f, slice.innerRadius) }
    }
    fun tapHit(x: Float, y: Float, selected: Int? = null): Int? {
        val index = hit(x, y, innerRadius, outerRadius, selected) ?: return null
        val slice = slices(selected)[index]; val inset = angularGap(slice) / 2f
        val angle = angle(x, y)
        return index.takeIf { hypot(x - center.x, y - center.y) <= slice.outerRadius &&
            angle >= slice.start + inset && angle <= slice.end - inset }
    }
    fun hit(x: Float, y: Float, minimum: Float, maximum: Float, selected: Int? = null): Int? {
        if (!x.isFinite() || !y.isFinite()) return null
        val distance = hypot(x - center.x, y - center.y)
        // Drag picking reaches slightly inside the hollow center. Normal taps still pass innerRadius as their minimum.
        val dragInner = innerRadius - minOf(innerRadius * .2f, gap * 3f)
        if (distance < maxOf(minimum, dragInner) || distance > maximum) return null
        val angle = angle(x, y)
        if (angle < -PI.toFloat() / 2f || angle > PI.toFloat() / 2f) return null
        val regions = slices(selected)
        if (selected != null && selected in regions.indices && angle >= regions[selected].start && angle <= regions[selected].end) return selected
        return regions.indexOfFirst { angle >= it.start && angle <= it.end }.takeIf { it >= 0 }
    }
}

object WheelGeometry {
    internal fun maximumOuterDp(screen: Bounds, density: Float, edge: Edge): Float {
        val d = density.coerceAtLeast(.01f)
        val side = edge != Edge.BOTTOM
        return minOf((if (side) screen.height / 2f else screen.width / 2f) / d - 8f,
            (if (side) screen.width else screen.height) / d - 16f).coerceAtLeast(0f)
    }
    internal fun fitsTargets(inner: Float, cell: Float, expansion: Float, count: Int): Boolean {
        val candidate = WheelGeometryResult(Bounds(0f, 0f, 0f, 0f), WheelPoint(0f, 0f), Edge.LEFT,
            inner + cell / 2f, cell, count, count, expansion, 48f, inner, 4f)
        return (listOf<Int?>(null) + (0 until count).toList()).all { selected ->
            candidate.slices(selected).all { slice ->
                val target = 48f
                slice.size >= target && (inner + slice.size / 2f) *
                    sin((slice.end - slice.start - candidate.angularGap(slice)) / 2f) >= target / 2f
            }
        }
    }
    fun calculate(screen: Bounds, density: Float, layout: PanelLayout, settings: WheelLayoutSettings, count: Int, handle: Bounds, edge: Edge): WheelGeometryResult {
        val s = settings.normalized()
        val d = density.coerceAtLeast(.01f)
        val (innerDp, outerDp) = s.radii(layout.normalized().buttonSizeDp)
        val expansion = if (s.expandSelection) s.selectedScale else 1f
        val margin = 8f * d
        val side = edge != Edge.BOTTOM
        val maximumOuter = maximumOuterDp(screen, d, edge) * d
        val cell = minOf((outerDp - innerDp) * d, (maximumOuter - minOf(innerDp, 12f) * d).coerceAtLeast(0f) / expansion)
        val inner = minOf(innerDp * d, (maximumOuter - cell * expansion).coerceAtLeast(0f))
        val radius = inner + cell / 2f
        val outer = inner + cell * expansion
        // Capacity depends on 48 dp touch targets. Artwork is fitted separately without changing selection.
        val capacity = (6 downTo 1).firstOrNull { n ->
            cell >= 48f * d && screen.width >= margin * 2f && screen.height >= margin * 2f &&
                fitsTargets(inner / d, cell / d, expansion, n)
        } ?: 0
        val width = if (side) outer + margin * 2f else (outer + margin) * 2f
        val height = if (side) (outer + margin) * 2f else outer + margin * 2f
        val localX = when (edge) { Edge.LEFT -> margin; Edge.RIGHT -> outer + margin; Edge.BOTTOM -> outer + margin }
        val localY = outer + margin
        var cx = screen.x + layout.x * screen.width
        var cy = screen.y + layout.y * screen.height
        if (layout.followHandle) {
            cx = when (edge) { Edge.LEFT -> handle.x + handle.width; Edge.RIGHT -> handle.x; Edge.BOTTOM -> handle.x + handle.width / 2f } + layout.offsetXDp * d
            cy = (if (side) handle.y + handle.height / 2f else handle.y) - layout.offsetYDp * d
        }
        val x = (cx - localX).coerceIn(screen.x, screen.x + (screen.width - width).coerceAtLeast(0f))
        val y = (cy - localY).coerceIn(screen.y, screen.y + (screen.height - height).coerceAtLeast(0f))
        val bounds = Bounds(x, y, width.coerceAtMost(screen.width), height.coerceAtMost(screen.height))
        val renderedInner = inner.coerceAtMost(outer)
        val renderedCell = if (capacity == 0) ((outer - renderedInner) / expansion).coerceAtLeast(0f) else cell
        return WheelGeometryResult(bounds, WheelPoint(x + localX.coerceAtMost(bounds.width), y + localY.coerceAtMost(bounds.height)),
            edge, radius, renderedCell, capacity, count.coerceIn(0, capacity), expansion, 48f * d, renderedInner, 4f * d)
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
