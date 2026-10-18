package com.modukey.app.core

import org.junit.Assert.*
import org.junit.Test

class PanelGeometryTest {
    @Test fun `follow offsets use density and retain absolute placement through switches and clamping`() {
        val space = Bounds(20f, 40f, 900f, 1400f)
        val saved = PanelLayout(x = .3f, y = .7f, offsetXDp = -10f, offsetYDp = 15f)
        for (edge in Edge.entries) {
            val area = when(edge) {
                Edge.LEFT -> Bounds(20f, 500f, 32f, 240f)
                Edge.RIGHT -> Bounds(888f, 500f, 32f, 240f)
                Edge.BOTTOM -> Bounds(300f, 1408f, 240f, 32f)
            }
            val base = PanelGeometry.calculate(space, 2f, saved.copy(offsetXDp = 0f, offsetYDp = 0f), 1, area, edge).bounds
            val shifted = PanelGeometry.calculate(space, 2f, saved, 1, area, edge).bounds
            assertEquals(base.x - 20f, shifted.x, .001f)
            assertEquals(base.y - 30f, shifted.y, .001f)
            val absolute = PanelGeometry.calculate(space, 2f, saved.copy(followHandle = false), 1, area, edge).bounds
            val absoluteWithoutOffsets = PanelGeometry.calculate(space, 2f, saved.copy(followHandle = false, offsetXDp = 0f, offsetYDp = 0f), 1, area, edge).bounds
            assertEquals(absoluteWithoutOffsets, absolute)
            val clamped = PanelGeometry.calculate(space, 2f, saved.copy(offsetXDp = 240f, offsetYDp = 240f), 1, area, edge).bounds
            assertTrue(clamped.x >= space.x && clamped.x + clamped.width <= space.x + space.width)
            assertTrue(clamped.y >= space.y && clamped.y + clamped.height <= space.y + space.height)
        }
        assertEquals(saved, saved.copy(followHandle = false).copy(followHandle = true))
    }
    private val screen = Bounds(0f, 24f, 400f, 800f)
    private val handle = Bounds(384f, 450f, 16f, 120f)
    @Test fun `fixed overflow keeps ordered capacity and never expands past maximum lines`() {
        val g = PanelGeometry.calculate(screen, 1f, PanelLayout(crossCount = 2, maximumLines = 3), 12, handle, Edge.RIGHT)
        assertEquals(6, g.visibleCount); assertEquals(3, g.lines); assertEquals(2, g.crossCount)
        assertEquals(0 to 0, g.cellPosition(0, PanelDirection.VERTICAL))
        assertEquals(1 to 0, g.cellPosition(1, PanelDirection.VERTICAL))
        assertEquals(0 to 1, g.cellPosition(2, PanelDirection.VERTICAL))
    }
    @Test fun `horizontal grid is transposed and fills down then right`() {
        val g = PanelGeometry.calculate(screen, 1f, PanelLayout(direction = PanelDirection.HORIZONTAL, crossCount = 2, maximumLines = 3), 12, handle, Edge.RIGHT)
        assertEquals(6, g.visibleCount)
        assertEquals(0 to 1, g.cellPosition(1, PanelDirection.HORIZONTAL))
        assertEquals(1 to 0, g.cellPosition(2, PanelDirection.HORIZONTAL))
    }
    @Test fun `scrolling exposes all buttons and switching back retains limits`() {
        val layout = PanelLayout(crossCount = 2, maximumLines = 3)
        assertEquals(100, PanelGeometry.calculate(screen, 1f, layout.copy(mode = PanelMode.SCROLLABLE), 100, handle, Edge.LEFT).visibleCount)
        assertEquals(6, PanelGeometry.calculate(screen, 1f, layout, 100, handle, Edge.LEFT).visibleCount)
    }
    @Test fun `physical limits reduce capacity without mutating saved values`() {
        val layout = PanelLayout(crossCount = 12, maximumLines = 20)
        val g = PanelGeometry.calculate(Bounds(0f, 0f, 200f, 150f), 1f, layout, 100, handle, Edge.BOTTOM)
        assertTrue(g.bounds.width <= 200); assertTrue(g.bounds.height <= 150)
        assertTrue(g.visibleCount < 240); assertEquals(12, layout.crossCount); assertEquals(20, layout.maximumLines)
    }
    @Test fun `custom placement preserves center during resizing and clamps corners`() {
        val layout = PanelLayout(followHandle = false, x = 0.5f, y = 0.5f)
        for (size in listOf(48f, 56f, 80f)) {
            val b = PanelGeometry.calculate(screen, 1f, layout.copy(buttonSizeDp = size), 2, handle, Edge.RIGHT).bounds
            assertEquals(200f, b.x + b.width / 2); assertEquals(424f, b.y + b.height / 2)
        }
        val b = PanelGeometry.calculate(screen, 1f, layout.copy(x = 0f, y = 1f), 2, handle, Edge.RIGHT).bounds
        assertEquals(0f, b.x); assertEquals(824f, b.y + b.height)
    }
}
