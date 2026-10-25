package com.modukey.app.core

import org.junit.Assert.*
import org.junit.Test

class WheelHitboxTest {
    @Test fun enteringManualCopiesTheEffectivePreviousModeInsteadOfOldManualValues() {
        for (edge in Edge.entries) for (d in listOf(1f, 3f)) for (fitted in listOf(false, true)) {
            val g = geometry(edge, d).copy(autoFit = fitted)
            for (mode in listOf(WheelHitboxMode.AUTOMATIC, WheelHitboxMode.BUTTON_SIZE)) {
                val before = WheelHitboxSettings(mode, 16f, 420f)
                val bounds = g.hitboxRadii(WheelPickSettings(), d, before)
                val manual = before.withMode(WheelHitboxMode.MANUAL, bounds.first / d to bounds.second / d)
                assertEquals(bounds.first, manual.innerRadiusDp * d, .001f)
                assertEquals(bounds.second, manual.outerRadiusDp * d, .001f)
                assertEquals(manual, manual.withMode(WheelHitboxMode.MANUAL, 0f to 600f))
            }
        }
    }

    @Test fun fittedAutomaticBoundsKeepThirtyTwoDpInsideAndFollowEachOuterEdge() {
        for (edge in Edge.entries) for (d in listOf(1f, 3f)) {
            val screen = Bounds(0f, 0f, 1000f * d, 1000f * d)
            val settings = requireNotNull(WheelAutofit.fit(WheelLayoutSettings(), screen, d, 6, edge))
            val g = WheelGeometry.calculate(screen, d, PanelLayout(), settings, 6, screen, edge)
            val pick = WheelPickSettings(minimumDistanceDp = 16f, maximumDistanceDp = 600f)
            val normal = g.slices()[2]
            val angle = (normal.start + normal.end) / 2f
            val insideHole = g.point(angle, 40f * d)
            assertEquals(2, g.pickHit(insideHole.x, insideHole.y, pick, d, WheelHitboxSettings(), null))
            assertNull(g.tapHit(insideHole.x, insideHole.y))
            for (selected in listOf<Int?>(null, 2)) {
                val edgeRadius = g.slices(selected)[2].outerRadius
                val bounds = g.hitboxRadii(pick, d, WheelHitboxSettings(), selected, 2)
                assertEquals(32f * d, bounds.first, .001f)
                assertEquals(edgeRadius, bounds.second, .001f)
                for (radius in listOf(31f * d, edgeRadius + d)) {
                    val p = g.point(angle, radius)
                    assertNull(g.pickHit(p.x, p.y, pick, d, WheelHitboxSettings(), selected))
                }
                val p = g.point(angle, edgeRadius - .01f * d)
                assertEquals(2, g.pickHit(p.x, p.y, pick, d, WheelHitboxSettings(), selected))
            }
        }
    }

    private fun geometry(edge: Edge, density: Float) = WheelGeometry.calculate(Bounds(10f, 20f, 800f * density, 900f * density),
        density, PanelLayout(), WheelLayoutSettings(innerRadiusDp = 100f, outerRadiusDp = 156f, selectedScale = 1.3f, autoFit = false),
        3, Bounds(10f, 300f, 16f, 120f), edge)
    @Test fun automaticKeepsTheExistingDistancesAndInwardMargin() {
        val pick = WheelPickSettings(minimumDistanceDp = 32f, maximumDistanceDp = 240f)
        for (edge in Edge.entries) for (d in listOf(1f, 3f)) {
            val g = geometry(edge, d)
            for (radius in listOf(0f, 32f, 87f, 88f, 99f, 100f, 128f, 156f, 240f, 241f, 300f)) {
                val p = g.point(0f, radius * d)
                for (selected in listOf(null, 1)) assertEquals(g.hit(p.x, p.y, 32f * d, 240f * d, selected),
                    g.pickHit(p.x, p.y, pick, d, WheelHitboxSettings(innerRadiusDp = 0f, outerRadiusDp = 600f), selected))
            }
        }
    }
    @Test fun buttonSizeUsesTheLogicalButtonRadiiAndIgnoresAutomaticDistances() {
        val pick = WheelPickSettings(minimumDistanceDp = 160f, maximumDistanceDp = 168f)
        val hitbox = WheelHitboxSettings(WheelHitboxMode.BUTTON_SIZE)
        for (edge in Edge.entries) for (d in listOf(1f, 3f)) {
            val g = geometry(edge, d)
            for (selected in listOf(null, 1)) {
                val slice = g.slices(selected)[1]
                for (radius in listOf(slice.innerRadius, (slice.innerRadius + slice.outerRadius) / 2, slice.outerRadius - .01f * d)) {
                    val p = g.point(0f, radius)
                    assertEquals(1, g.pickHit(p.x, p.y, pick, d, hitbox, selected))
                }
                for (radius in listOf(slice.innerRadius - d, slice.outerRadius + d)) {
                    val p = g.point(0f, radius)
                    assertNull(g.pickHit(p.x, p.y, pick, d, hitbox, selected))
                }
            }
        }
    }
    @Test fun manualRadiiCanReachInsideAndOutsideTheButtonsWithoutChangingTaps() {
        val pick = WheelPickSettings(minimumDistanceDp = 160f, maximumDistanceDp = 168f)
        val hitbox = WheelHitboxSettings(WheelHitboxMode.MANUAL, 16f, 300f)
        for (edge in Edge.entries) for (d in listOf(1f, 3f)) {
            val g = geometry(edge, d)
            for (radius in listOf(16f, 40f, 128f, 299f, 300f)) {
                val p = g.point(0f, radius * d)
                assertEquals(1, g.pickHit(p.x, p.y, pick, d, hitbox, null))
                if (radius < 100f || radius > 156f) assertNull(g.tapHit(p.x, p.y))
            }
            for (radius in listOf(15f, 301f)) {
                val p = g.point(0f, radius * d)
                assertNull(g.pickHit(p.x, p.y, pick, d, hitbox, null))
            }
            assertNull(g.pickHit(Float.NaN, 0f, pick, d, hitbox, null))
            assertNull(g.pickHit(g.center.x, g.center.y, pick, d, hitbox.copy(innerRadiusDp = 0f), 1))
            val behind = g.point(kotlin.math.PI.toFloat(), 128f * d)
            assertNull(g.pickHit(behind.x, behind.y, pick, d, hitbox, null))
        }
    }
    @Test fun manualReleaseUsesTheSameBoundsAndCannotCreateANewHighlight() {
        val g = geometry(Edge.LEFT, 1f)
        val hitbox = WheelHitboxSettings(WheelHitboxMode.MANUAL, 0f, 60f)
        val inside = g.point(0f, 40f); val outside = g.point(0f, 61f)
        fun session() = WheelPickSession(g, WheelPickSettings(), 1f, g.center.x, g.center.y, 0, false, hitbox)
        val picked = session().apply { move(inside.x, inside.y, 1) }
        assertEquals(1, picked.selected)
        assertEquals(WheelFinish(WheelFinishKind.PICK, 1), picked.finish(inside.x, inside.y, 2))
        val stale = session().apply { move(inside.x, inside.y, 1) }
        assertEquals(WheelFinishKind.CANCEL, stale.finish(outside.x, outside.y, 2).kind)
        val cleared = session().apply { move(inside.x, inside.y, 1); move(outside.x, outside.y, 2) }
        assertNull(cleared.selected)
        assertEquals(WheelFinishKind.CANCEL, cleared.finish(inside.x, inside.y, 3).kind)
    }
    @Test fun everyModeKeepsStickyAngularBoundaries() {
        val g = geometry(Edge.LEFT, 1f)
        val normal = g.slices()[1]; val expanded = g.slices(1)[1]
        val p = g.point((normal.end + expanded.end) / 2f)
        for (mode in WheelHitboxMode.entries) {
            val hitbox = WheelHitboxSettings(mode, 16f, 300f)
            assertEquals(2, g.pickHit(p.x, p.y, WheelPickSettings(), 1f, hitbox, null))
            assertEquals(1, g.pickHit(p.x, p.y, WheelPickSettings(), 1f, hitbox, 1))
        }
    }
    @Test fun manualRadiiRemainFiniteOrderedAndIndependentOfAutofit() {
        assertEquals(WheelHitboxSettings(), WheelHitboxSettings(innerRadiusDp = Float.NaN, outerRadiusDp = Float.POSITIVE_INFINITY).normalized())
        assertEquals(0f, WheelHitboxSettings(innerRadiusDp = -1f).normalized().innerRadiusDp)
        val limits = WheelHitboxSettings(innerRadiusDp = 900f, outerRadiusDp = -1f).normalized()
        assertEquals(592f, limits.innerRadiusDp); assertEquals(600f, limits.outerRadiusDp)
        val hitbox = WheelHitboxSettings(WheelHitboxMode.MANUAL, 16f, 300f)
        val layout = PanelLayout(mode = PanelMode.WHEEL)
        assertTrue(layout.editSize { it.copy(wheel = it.wheel.copy(hitbox = hitbox)) }.wheel.autoFit)
        assertEquals(hitbox, WheelAutofit.fit(layout.wheel.copy(hitbox = hitbox), Bounds(0f, 0f, 800f, 900f), 1f, 6, Edge.LEFT)!!.hitbox)
    }
}
