package com.modukey.app.core

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

data class PanelGeometryResult(
    val bounds: Bounds, val crossCount: Int, val lines: Int, val visibleCount: Int,
    val cell: Float, val gap: Float, val padding: Float,
) {
    fun cellPosition(index: Int, direction: PanelDirection): Pair<Int, Int> =
        if (direction == PanelDirection.VERTICAL) index % crossCount to index / crossCount
        else index / crossCount to index % crossCount
}
object PanelGeometry {
    fun calculate(screen: Bounds, density: Float, layout: PanelLayout, count: Int, handle: Bounds, edge: Edge): PanelGeometryResult {
        val s = layout.normalized()
        val vertical = s.direction == PanelDirection.VERTICAL
        val padding = 8f * density
        val gap = 8f * density
        val cell = s.buttonSizeDp * density
        val availableCross = if (vertical) screen.width else screen.height
        val availableMain = if (vertical) screen.height else screen.width
        val cross = min(s.crossCount, floor((availableCross - padding * 2 + gap) / (cell + gap)).toInt().coerceAtLeast(1))
        val screenLines = floor((availableMain - padding * 2 + gap) / (cell + gap)).toInt().coerceAtLeast(1)
        val needed = ceil(count.coerceAtLeast(1).toFloat() / cross).toInt()
        val lines = min(needed, min(s.maximumLines, screenLines)).coerceAtLeast(1)
        val visible = if (s.mode == PanelMode.FIXED) min(count, cross * lines) else count
        val crossLength = min(availableCross, padding * 2 + cross * cell + (cross - 1) * gap)
        val mainLength = if (s.mode == PanelMode.FIXED) min(availableMain, padding * 2 + lines * cell + (lines - 1) * gap)
            else (availableMain * s.extentFraction).coerceIn(min(availableMain, cell + padding * 2), availableMain)
        val width = if (vertical) crossLength else mainLength
        val height = if (vertical) mainLength else crossLength
        var x = screen.x + s.x * screen.width - width / 2
        var y = screen.y + s.y * screen.height - height / 2
        if (s.followHandle) {
            when (edge) {
                Edge.LEFT -> { x = handle.x + handle.width + 8 * density; y = handle.y + handle.height / 2 - height / 2 }
                Edge.RIGHT -> { x = handle.x - width - 8 * density; y = handle.y + handle.height / 2 - height / 2 }
                Edge.BOTTOM -> { x = handle.x + handle.width / 2 - width / 2; y = handle.y - height - 8 * density }
            }
            x += s.offsetXDp * density
            y -= s.offsetYDp * density
        }
        x = x.coerceIn(screen.x, screen.x + max(0f, screen.width - width))
        y = y.coerceIn(screen.y, screen.y + max(0f, screen.height - height))
        return PanelGeometryResult(Bounds(x, y, width, height), cross, lines, visible, cell, gap, padding)
    }
}
fun Bounds.contains(x: Float, y: Float) = x >= this.x && x < this.x + width && y >= this.y && y < this.y + height
fun Bounds.overlaps(other: Bounds) = x < other.x + other.width && x + width > other.x && y < other.y + other.height && y + height > other.y
