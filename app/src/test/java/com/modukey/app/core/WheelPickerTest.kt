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
    @Test fun customBoundsControlTheHoleAndBandIndependently() {
        val settings = WheelLayoutSettings(innerRadiusDp = 60f, outerRadiusDp = 160f)
        val g = WheelGeometry.calculate(Bounds(0f, 0f, 800f, 900f), 1f, PanelLayout(), settings, 6,
            Bounds(0f, 300f, 16f, 120f), Edge.LEFT)
        assertEquals(60f, g.innerRadius)
        g.slices().forEach { assertEquals(60f, it.innerRadius); assertEquals(160f, it.outerRadius) }
        assertNull(g.tapHit(g.point(0f, 59f).x, g.point(0f, 59f).y))
        assertTrue(WheelLayoutSettings(innerRadiusDp = 300f, outerRadiusDp = 100f).radii(56f).let { it.second - it.first >= 48f })
        assertTrue(WheelLayoutSettings(radiusDp = 240f, outerRadiusDp = 60f).radii(56f).let { it.second - it.first >= 48f })
    }
    @Test fun oneDpInnerRadiusKeepsAHoleAndUsableTargetsAtEveryEdge() {
        val settings = WheelLayoutSettings(innerRadiusDp = 1f, outerRadiusDp = 49f)
        assertEquals(1f to 49f, settings.radii(56f))
        assertEquals(1f, settings.copy(innerRadiusDp = 0f).normalized().innerRadiusDp!!)
        assertEquals(49f, settings.copy(outerRadiusDp = 0f).normalized().outerRadiusDp!!)
        for (edge in Edge.entries) for (density in listOf(1f, 3f)) {
            val g = WheelGeometry.calculate(Bounds(0f, 0f, 800f * density, 900f * density), density,
                PanelLayout(), settings, 6, Bounds(0f, 300f * density, 16f * density, 120f * density), edge)
            assertEquals(density, g.innerRadius, .001f)
            assertEquals(1, g.capacity)
            val hole = g.point(0f, density / 2f)
            assertNull(g.tapHit(hole.x, hole.y))
            for (selected in listOf<Int?>(null, 0)) {
                val slice = g.slices(selected).single()
                assertTrue(slice.size >= 48f * density)
                assertEquals(0, g.tapHit(slice.point.x, slice.point.y, selected))
            }
        }
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
    @Test fun everyWedgeContainsItsTouchTargetAndStaysSeparatedDuringExpansion() {
        for (edge in Edge.entries) for (cell in listOf(48f, 56f, 96f)) for (radius in listOf(40f, 64f, 96f, 128f, 200f, 240f)) for (scale in listOf(1f, 1.25f, 1.5f)) {
            val g = WheelGeometry.calculate(Bounds(0f, 0f, 900f, 900f), 1f, PanelLayout(buttonSizeDp = cell),
                WheelLayoutSettings(radius, selectedScale = scale), 6, Bounds(0f, 300f, 16f, 120f), edge)
            for (selected in listOf<Int?>(null) + (0 until g.count).toList()) {
                val slices = g.slices(selected)
                slices.forEach { slice ->
                    val target = maxOf(48f, slice.iconSize)
                    assertTrue(slice.size >= target)
                    val distanceToSide = (slice.innerRadius + slice.size / 2f) *
                        kotlin.math.sin((slice.end - slice.start - g.angularGap(slice)) / 2f)
                    assertTrue(distanceToSide + .001f >= target / 2f)
                    assertTrue(slice.innerRadius >= 12f)
                    assertTrue(slice.outerRadius <= g.outerRadius + .001f)
                }
                slices.zipWithNext().forEach { (a, b) -> assertEquals(a.end, b.start, .0001f) }
            }
        }
    }
    @Test fun compactRadiiKeepTheCenterEmptyAndReduceCapacity() {
        val compact = geometry(radius = 40f)
        assertEquals(40f, WheelLayoutSettings(1f).normalized().radiusDp)
        assertTrue(compact.bounds.width < geometry(radius = 96f).bounds.width)
        assertTrue(compact.capacity in 1..2)
        val center = compact.point(0f, compact.innerRadius / 2f)
        assertNull(compact.hit(center.x, center.y, 0f, 500f))
        assertNull(compact.tapHit(center.x, center.y))
        val empty = geometry(count = 0)
        assertNull(empty.tapHit(empty.center.x, empty.center.y))
    }
    @Test fun normalTapsUseTheWedgeAndExcludeItsGapsAndOuterSpace() {
        val g = geometry(count = 3)
        g.slices().forEachIndexed { index, slice -> assertEquals(index, g.tapHit(slice.point.x, slice.point.y)) }
        val gap = g.point(g.slices()[0].end)
        assertNull(g.tapHit(gap.x, gap.y))
        val beyond = g.point(0f, g.outerRadius + 1f)
        assertNull(g.tapHit(beyond.x, beyond.y))
    }
    @Test fun theNewWedgeAndIconGrowWhileTheirNeighborsShrink() {
        val g = geometry(count = 6)
        val normal = g.slices(); val focused = g.slices(2)
        assertTrue(focused[2].outerRadius > normal[2].outerRadius)
        assertTrue(focused[2].iconSize > normal[2].iconSize)
        assertTrue(focused[1].end - focused[1].start < normal[1].end - normal[1].start)
        assertTrue(focused[1].iconSize < normal[1].iconSize)
        assertEquals(normal[2].innerRadius, focused[2].innerRadius)
        val point = g.point(g.slices(2)[2].end + .01f)
        assertEquals(3, g.hit(point.x, point.y, 0f, 500f, 2))
        assertEquals(3, g.hit(point.x, point.y, 0f, 500f, 3))
    }
    @Test fun animationCannotChangeLogicalSelectionUnderAStationaryFinger() {
        val g = geometry(count = 6)
        val point = g.slices()[2].point
        val session = WheelPickSession(g, WheelPickSettings(), 1f, g.center.x, g.center.y, 0, false)
        session.move(point.x, point.y, 1)
        for (frame in 0..20) {
            val slices = g.slices(List(g.count) { if (it == 2) frame / 20f else 0f })
            assertEquals(2, session.selected)
            assertEquals(2, g.hit(point.x, point.y, 0f, 500f, session.selected))
            assertTrue(slices.all { it.end > it.start })
        }
        assertEquals(WheelFinish(WheelFinishKind.PICK, 2), session.finish(point.x, point.y, 200))
    }
    @Test fun crossingEitherExpandedBoundaryTransfersTheHitboxWithoutSwitchingBack() {
        for (edge in Edge.entries) for (count in 2..6) {
            val g = geometry(edge, count = count)
            val normal = g.slices()
            for (selected in 0 until count) for (direction in listOf(-1, 1)) {
                val next = selected + direction
                if (next !in 0 until count) continue
                val current = g.slices(selected)[selected]
                val boundary = if (direction < 0) current.start else current.end
                val inside = g.point(boundary - direction * .001f)
                assertEquals(selected, g.hit(inside.x, inside.y, 0f, 500f, selected))
                val outside = g.point(boundary + direction * .001f)
                assertEquals(next, g.hit(outside.x, outside.y, 0f, 500f, selected))
                assertEquals(next, g.hit(outside.x, outside.y, 0f, 500f, next))
                val changed = g.slices(next)
                assertTrue(changed[next].end - changed[next].start > normal[next].end - normal[next].start)
                assertTrue(changed[selected].end - changed[selected].start < normal[selected].end - normal[selected].start)
            }
        }
    }
    @Test fun safeBoundsContainEveryExpandedButtonAtExtremePlacements() {
        for (edge in Edge.entries) for (screen in listOf(Bounds(12f, 28f, 320f, 640f), Bounds(28f, 12f, 640f, 260f))) {
            for (follow in listOf(true, false)) for (position in listOf(0f, 1f)) {
                val layout = PanelLayout(followHandle = follow, x = position, y = position, offsetXDp = 240f, offsetYDp = -240f)
                val g = WheelGeometry.calculate(screen, 1f, layout, WheelLayoutSettings(240f, selectedScale = 1.5f), 9,
                    Bounds(screen.x, screen.y, 16f, 100f), edge)
                assertTrue(g.capacity in 1..6)
                for (selected in listOf<Int?>(null) + (0 until g.count).toList()) g.slices(selected).forEach { slice ->
                    assertTrue(slice.point.x - slice.size / 2f >= screen.x)
                    assertTrue(slice.point.y - slice.size / 2f >= screen.y)
                    assertTrue(slice.point.x + slice.size / 2f <= screen.x + screen.width)
                    assertTrue(slice.point.y + slice.size / 2f <= screen.y + screen.height)
                }
            }
        }
    }
    @Test fun screensTooSmallForTouchTargetsKeepTheCenterVisibleAndHaveNoActions() {
        for (edge in Edge.entries) {
            val screen = Bounds(10f, 20f, 40f, 40f)
            val g = WheelGeometry.calculate(screen, 1f, PanelLayout(), WheelLayoutSettings(), 6, screen, edge)
            assertEquals(0, g.capacity); assertEquals(0, g.count)
            assertTrue(g.center.x in screen.x..(screen.x + screen.width))
            assertTrue(g.center.y in screen.y..(screen.y + screen.height))
            assertTrue(g.outerRadius * 2f <= screen.height)
            assertTrue(g.outerRadius * 2f <= screen.width)
            assertNull(g.hit(g.center.x, g.center.y, 0f, 240f))
        }
    }
    @Test fun currentSelectionOwnsASharedBoundaryUntilTheFingerCrossesIt() {
        val g = geometry(count = 2).copy(expansion = 1f)
        val boundary = g.point(0f)
        assertEquals(0, g.hit(boundary.x, boundary.y, 32f, 240f))
        assertEquals(0, g.hit(boundary.x, boundary.y, 32f, 240f, 0))
        assertEquals(1, g.hit(boundary.x, boundary.y, 32f, 240f, 1))
        val crossed = g.point(-.01f)
        assertEquals(0, g.hit(crossed.x, crossed.y, 32f, 240f, 1))
    }
    @Test fun dragPickingReachesInsideTheHoleWithoutChangingTapsOrDistanceLimits() {
        for (edge in Edge.entries) for (density in listOf(1f, 3f)) {
            val g = geometry(edge, density = density, count = 3)
            val early = g.point(0f, g.innerRadius - 10f * density)
            assertEquals(1, g.hit(early.x, early.y, 16f * density, 1000f * density))
            assertNull(g.tapHit(early.x, early.y))
            val neutral = g.point(0f, g.innerRadius - 13f * density)
            assertNull(g.hit(neutral.x, neutral.y, 0f, 1000f * density))
            assertNull(g.hit(early.x, early.y, g.innerRadius, 1000f * density))
            assertNull(g.hit(early.x, early.y, 0f, g.innerRadius - 11f * density))
            val session = WheelPickSession(g, WheelPickSettings(minimumDistanceDp = 16f), density, g.center.x, g.center.y, 0, false)
            session.move(early.x, early.y, 10)
            assertEquals(1, session.selected)
            assertEquals(WheelFinish(WheelFinishKind.PICK, 1), session.finish(early.x, early.y, 20))
        }
        val tiny = geometry(radius = 40f, count = 1)
        val deep = tiny.point(0f, tiny.innerRadius * .7f)
        assertNull(tiny.hit(deep.x, deep.y, 0f, 500f))
    }
}
