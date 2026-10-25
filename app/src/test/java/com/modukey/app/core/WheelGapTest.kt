package com.modukey.app.core

import kotlin.math.*
import org.junit.Assert.*
import org.junit.Test

class WheelGapTest {
    @Test fun sharedEdgesKeepTheSamePerpendicularGapAcrossTheBandAndHighlight() {
        for (edge in Edge.entries) for (d in listOf(1f, 3f)) for (gap in listOf(0f, 4f, 24f)) {
            val g = WheelGeometry.calculate(Bounds(0f, 0f, 1000f * d, 1000f * d), d, PanelLayout(),
                WheelLayoutSettings(innerRadiusDp = 100f, outerRadiusDp = 240f, gapDp = gap), 6,
                Bounds(0f, 300f * d, 16f * d, 120f * d), edge)
            for (selected in listOf(null, 2)) {
                val slices = g.slices(selected)
                for (i in 0 until slices.lastIndex) {
                    val left = slices[i]; val right = slices[i + 1]
                    for (radius in listOf(g.innerRadius + 24f * d, g.radius, minOf(left.outerRadius, right.outerRadius) - 24f * d)) {
                        val a = g.point(left.end - g.sideInset(left, radius, false), radius)
                        val b = g.point(right.start + g.sideInset(right, radius, true), radius)
                        assertEquals("$edge $gap $selected $radius", gap * d, hypot(a.x - b.x, a.y - b.y), .002f * d)
                        if (gap > 0f) {
                            val empty = g.point(left.end, radius)
                            assertNull(g.tapHit(empty.x, empty.y, selected))
                        }
                    }
                }
            }
        }
    }

    @Test fun gapLimitsAndDensityDoNotChangeTheAutomaticInwardMargin() {
        assertEquals(4f, WheelLayoutSettings(gapDp = Float.NaN).normalized().gapDp)
        assertEquals(0f, WheelLayoutSettings(gapDp = -8f).normalized().gapDp)
        assertEquals(24f, WheelLayoutSettings(gapDp = 900f).normalized().gapDp)
        val screen = Bounds(0f, 0f, 800f, 900f)
        val a = WheelGeometry.calculate(screen, 1f, PanelLayout(), WheelLayoutSettings(innerRadiusDp = 100f, gapDp = 0f), 3, screen, Edge.LEFT)
        val b = a.copy(gap = 24f)
        for (radius in listOf(87f, 88f, 100f)) {
            val p = a.point(0f, radius)
            assertEquals(a.hit(p.x, p.y, 32f, 240f), b.hit(p.x, p.y, 32f, 240f))
        }
    }
}
