package com.modukey.app.core

import org.junit.Assert.*
import org.junit.Test

class WheelAutofitTest {
    @Test fun fittedWheelsContainAllButtonsAndTouchTargetsAcrossOrientationsAndDensities() {
        for (edge in Edge.entries) for (density in listOf(1f, 3f)) for (count in 1..6) {
            val screen = Bounds(12f, 24f, 400f * density, 800f * density)
            val settings = requireNotNull(WheelAutofit.fit(WheelLayoutSettings(), screen, density, count, edge))
            val g = WheelGeometry.calculate(screen, density, PanelLayout(), settings, count, screen, edge)
            assertEquals("$edge $density $count $settings", count, g.count)
            assertEquals(settings.innerRadiusDp!! * density, g.innerRadius, .001f)
            assertTrue(g.expansion in 1f..1.1f)
            assertTrue(g.bounds.x >= screen.x && g.bounds.y >= screen.y)
            assertTrue(g.bounds.x + g.bounds.width <= screen.x + screen.width + .001f)
            assertTrue(g.bounds.y + g.bounds.height <= screen.y + screen.height + .001f)
            for (selected in 0 until count) g.slices(selected).forEach { slice ->
                val target = 48f * density
                assertTrue(slice.size >= target)
                assertTrue((g.innerRadius + slice.size / 2) * kotlin.math.sin(
                    (slice.end - slice.start - g.angularGap(slice)) / 2) + .001f >= target / 2)
            }
        }
    }
    @Test fun smallestInnerRadiusWinsBeforeExpansionAndOuterRadius() {
        val screen = Bounds(0f, 0f, 800f, 900f)
        val one = requireNotNull(WheelAutofit.fit(WheelLayoutSettings(), screen, 1f, 1, Edge.LEFT))
        assertEquals(1f, one.innerRadiusDp!!)
        assertEquals(1.1f, one.selectedScale)
        assertEquals(336f, one.outerRadiusDp!!)
        val six = requireNotNull(WheelAutofit.fit(WheelLayoutSettings(), screen, 1f, 6, Edge.LEFT))
        val inner = six.innerRadiusDp!!
        if (inner > WheelLayoutSettings.MIN_INNER_RADIUS_DP) for (band in 48..335) for (percent in 100..110) {
            val candidate = six.copy(innerRadiusDp = inner - 1f, outerRadiusDp = inner - 1f + band,
                selectedScale = percent / 100f)
            if (candidate.outerRadiusDp!! > 336f) continue
            val g = WheelGeometry.calculate(screen, 1f, PanelLayout(), candidate, 6, screen, Edge.LEFT)
            assertTrue("A smaller inner radius must not fit: $candidate", g.count < 6)
        }
        for (percent in ((six.selectedScale * 100).toInt() + 1)..110) for (outer in (inner.toInt() + 48)..336) {
            val candidate = six.copy(selectedScale = percent / 100f, outerRadiusDp = outer.toFloat())
            assertTrue(WheelGeometry.calculate(screen, 1f, PanelLayout(), candidate, 6, screen, Edge.LEFT).count < 6)
        }
        for (outer in (six.outerRadiusDp!!.toInt() + 1)..336) {
            val candidate = six.copy(outerRadiusDp = outer.toFloat())
            assertTrue(WheelGeometry.calculate(screen, 1f, PanelLayout(), candidate, 6, screen, Edge.LEFT).count < 6)
        }
    }
    @Test fun disabledAutofitRetainsSizesAndSmallScreensDoNotClaimCapacity() {
        val settings = WheelLayoutSettings(innerRadiusDp = 64f, outerRadiusDp = 180f, autoFit = false)
        assertSame(settings, WheelAutofit.fit(settings, Bounds(0f, 0f, 400f, 800f), 1f, 6, Edge.LEFT))
        assertNull(WheelAutofit.fit(settings.copy(autoFit = true), Bounds(0f, 0f, 40f, 40f), 1f, 1, Edge.BOTTOM))
        val fitted = requireNotNull(WheelAutofit.fit(settings.copy(autoFit = true), Bounds(0f, 0f, 400f, 800f), 1f, 3, Edge.RIGHT))
        val disabled = fitted.copy(autoFit = false)
        assertEquals(fitted.radii(56f), disabled.radii(56f))
        assertSame(disabled, WheelAutofit.fit(disabled, Bounds(0f, 0f, 800f, 400f), 1f, 6, Edge.BOTTOM))
    }
    @Test fun manualSizeChangesDisableAutofitAndOtherEditsPreserveIt() {
        val layout = PanelLayout(mode = PanelMode.WHEEL)
        val edits: List<(PanelLayout) -> PanelLayout> = listOf(
            { it.copy(buttonSizeDp = 64f) }, { it.copy(wheel = it.wheel.copy(radiusDp = 140f)) },
            { it.copy(wheel = it.wheel.copy(innerRadiusDp = 20f)) }, { it.copy(wheel = it.wheel.copy(outerRadiusDp = 180f)) },
            { it.copy(wheel = it.wheel.copy(expandSelection = false)) }, { it.copy(wheel = it.wheel.copy(selectedScale = 1.2f)) })
        edits.forEach { assertFalse(layout.editSize(it).wheel.autoFit) }
        assertTrue(layout.editSize { it }.wheel.autoFit)
        assertTrue(layout.editSize { it.copy(x = .8f, offsetYDp = 12f) }.wheel.autoFit)
        assertTrue(layout.editSize { it.copy(wheel = it.wheel.copy(animationsEnabled = false, focusDurationMs = 280)) }.wheel.autoFit)
    }
}
