package com.modukey.app.core

import org.junit.Assert.*
import org.junit.Test

class WheelPickerTest {
    private fun geometry(edge: Edge = Edge.LEFT, radius: Float = 200f, count: Int = 9, density: Float = 1f) =
        WheelGeometry.calculate(Bounds(10f, 20f, 800f * density, 900f * density), density, PanelLayout(),
            WheelLayoutSettings(radius), count, Bounds(10f, 300f, 16f, 120f), edge)
    @Test fun sixIsTheMaximumAndSmallWheelsRetainFewerSlots() {
        assertEquals(6, geometry().capacity)
        assertEquals(6, geometry().count)
        assertTrue(geometry(radius = 96f).capacity < 6)
        assertEquals(0, geometry(count = 0).count)
    }
    @Test fun placementAndOrderingMatchEveryEdgeAndDensity() {
        for (edge in Edge.entries) for (density in listOf(1f, 3f)) {
            val g = geometry(edge, density = density)
            assertTrue(g.bounds.x >= 10f && g.bounds.y >= 20f)
            assertTrue(g.bounds.x + g.bounds.width <= 10f + 800f * density + .01f)
            assertTrue(g.bounds.y + g.bounds.height <= 20f + 900f * density + .01f)
            val slices = g.slices()
            slices.forEachIndexed { i, s ->
                assertEquals(i, g.hit(s.point.x, s.point.y, 0f, 1000f * density))
                assertTrue(s.size >= 48f * density)
            }
            if (edge == Edge.BOTTOM) assertTrue(slices.first().point.x < slices.last().point.x)
            else assertTrue(slices.first().point.y < slices.last().point.y)
        }
    }
    @Test fun expansionRetainsTheOldSelectionUntilItsExpandedBoundaryIsCrossed() {
        val g = geometry(count = 6)
        val normal = g.slices()[2]
        val expanded = g.slices(2)[2]
        assertTrue(expanded.size > normal.size)
        assertTrue(expanded.end > normal.end)
        val sticky = g.point((normal.end + expanded.end) / 2f)
        assertEquals(3, g.hit(sticky.x, sticky.y, 0f, 500f))
        assertEquals(2, g.hit(sticky.x, sticky.y, 0f, 500f, 2))
        val next = g.point(expanded.end + .01f)
        assertEquals(3, g.hit(next.x, next.y, 0f, 500f, 2))
        assertEquals(3, g.hit(next.x, next.y, 0f, 500f, 3))
    }
    @Test fun neutralOutsideAndMaximumDistanceClearSelectionAndPermitReentry() {
        val g = geometry(count = 3)
        val p = g.slices()[1].point
        val session = WheelPickSession(g, WheelPickSettings(enabled = true), 1f, g.center.x, g.center.y, 0, false)
        session.move(p.x, p.y, 10); assertEquals(1, session.selected)
        session.move(g.center.x, g.center.y, 20); assertNull(session.selected)
        session.move(p.x, p.y, 30); assertEquals(1, session.selected)
        val far = g.point(0f, 241f)
        session.move(far.x, far.y, 40); assertNull(session.selected)
        session.move(p.x, p.y, 50); assertEquals(1, session.selected)
        assertEquals(WheelFinish(WheelFinishKind.PICK, 1), session.finish(p.x, p.y, 60))
        assertEquals(WheelFinishKind.CANCEL, session.finish(p.x, p.y, 61).kind)
    }
    @Test fun openingAndReleaseCannotCreateTheFirstHighlight() {
        val g = geometry(count = 3); val p = g.slices()[1].point
        val session = WheelPickSession(g, WheelPickSettings(), 1f, p.x, p.y, 0, false)
        session.move(p.x + 8f, p.y, 10); assertNull(session.selected)
        assertEquals(WheelFinishKind.CANCEL, session.finish(p.x + 30f, p.y, 20).kind)
    }
    @Test fun earlyReleaseAndTheExactDeadlineHaveDifferentResults() {
        val g = geometry(count = 3); val p = g.slices()[1].point
        fun session() = WheelPickSession(g, WheelPickSettings(allowLaterSwipes = true), 1f, p.x, p.y, 100, true)
        assertEquals(WheelFinishKind.NEXT_SWIPE, session().finish(p.x, p.y, 299).kind)
        assertEquals(WheelFinishKind.CANCEL, session().finish(p.x, p.y, 300).kind)
        val armed = session()
        armed.move(p.x, p.y, 200); armed.arm(300)
        armed.move(p.x + 9f, p.y, 301); assertEquals(1, armed.selected)
        assertEquals(WheelFinishKind.PICK, armed.finish(p.x + 9f, p.y, 302).kind)
    }
    @Test fun finalCoordinatesAndCancellationCannotConfirmStaleHighlights() {
        val g = geometry(count = 3); val p = g.slices()[1].point
        fun session() = WheelPickSession(g, WheelPickSettings(), 1f, g.center.x, g.center.y, 0, false).apply { move(p.x, p.y, 10) }
        assertEquals(WheelFinishKind.CANCEL, session().finish(g.center.x, g.center.y, 20).kind)
        assertEquals(WheelFinishKind.CANCEL, session().finish(p.x, p.y, 20, true).kind)
        assertEquals(WheelFinishKind.CANCEL, session().finish(Float.NaN, p.y, 20).kind)
    }
    @Test fun invalidSettingsUseFiniteDefaultsAndOrderedDistances() {
        val layout = WheelLayoutSettings(Float.NaN, selectedScale = Float.POSITIVE_INFINITY).normalized()
        assertEquals(WheelLayoutSettings(), layout)
        val pick = WheelPickSettings(minimumDistanceDp = 160f, maximumDistanceDp = -1f, releaseWindowMs = 1).normalized()
        assertEquals(168f, pick.maximumDistanceDp); assertEquals(100, pick.releaseWindowMs.toInt())
    }
    @Test fun capacityPreventsSquareTargetOverlapInEveryExpansionState() {
        for (edge in Edge.entries) for (cell in listOf(48f, 56f, 96f)) for (radius in listOf(96f, 128f, 200f, 240f)) for (scale in listOf(1f, 1.25f, 1.5f)) {
            val g = WheelGeometry.calculate(Bounds(0f, 0f, 900f, 900f), 1f, PanelLayout(buttonSizeDp = cell),
                WheelLayoutSettings(radius, selectedScale = scale), 6, Bounds(0f, 300f, 16f, 120f), edge)
            for (selected in listOf<Int?>(null) + (0 until g.count).toList()) {
                val slices = g.slices(selected)
                slices.forEach { assertTrue(it.size >= 48f) }
                for (a in slices.indices) for (b in a + 1 until slices.size) {
                    val spacing = (slices[a].size + slices[b].size) / 2f + 8f
                    assertTrue(kotlin.math.abs(slices[a].point.x - slices[b].point.x) >= spacing || kotlin.math.abs(slices[a].point.y - slices[b].point.y) >= spacing)
                }
            }
        }
    }
}
