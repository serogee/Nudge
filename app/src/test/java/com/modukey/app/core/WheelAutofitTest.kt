package com.modukey.app.core

import org.junit.Assert.*
import org.junit.Test

class WheelAutofitTest {
    @Test fun fittedWheelsContainAllButtonsAndTouchTargetsAcrossOrientationsAndDensities() {
        for (edge in Edge.entries) for (density in listOf(1f, 3f)) for (count in 1..6) for (scale in listOf(1f, 1.1f, 1.25f, 1.5f)) {
            val screen = Bounds(12f, 24f, 800f * density, 1000f * density)
            val settings = requireNotNull(WheelAutofit.fit(WheelLayoutSettings(selectedScale = scale), screen, density, count, edge))
            val g = WheelGeometry.calculate(screen, density, PanelLayout(), settings, count, screen, edge)
            assertEquals("$edge $density $count $settings", count, g.count)
            assertEquals(settings.innerRadiusDp!! * density, g.innerRadius, .001f)
            assertTrue(settings.innerRadiusDp!! >= 32f)
            assertEquals(.55f, settings.iconPosition)
            assertEquals(scale, settings.selectedScale)
            assertEquals(scale, g.expansion)
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
    @Test fun smallestOverallOuterRadiusWinsAndKeepsThePreferredHighlight() {
        val screen = Bounds(0f, 0f, 800f, 900f)
        val one = requireNotNull(WheelAutofit.fit(WheelLayoutSettings(), screen, 1f, 1, Edge.LEFT))
        assertEquals(32f, one.innerRadiusDp!!)
        assertEquals(1.25f, one.selectedScale)
        assertEquals(80f, one.outerRadiusDp!!)
        val six = requireNotNull(WheelAutofit.fit(WheelLayoutSettings(), screen, 1f, 6, Edge.LEFT))
        assertTrue(six.innerRadiusDp!! > one.innerRadiusDp!!)
        for (scale in listOf(1f, 1.1f, 1.25f, 1.5f)) for (gap in listOf(0f, 4f, 24f)) for (count in 1..6) {
            val fitted = requireNotNull(WheelAutofit.fit(WheelLayoutSettings(selectedScale = scale, gapDp = gap, iconPosition = .2f), screen, 1f, count, Edge.LEFT))
            assertEquals(scale, fitted.selectedScale)
            assertEquals(.55f, fitted.iconPosition)
            val smallerOuter = fitted.outerRadiusDp!! - 1f
            for (inner in 32..(smallerOuter.toInt() - 48)) {
                assertFalse("A smaller outer edge must not fit: $count $gap $inner $smallerOuter",
                    WheelGeometry.fitsTargets(inner.toFloat(), smallerOuter - inner, fitted.selectedScale, count, gap))
            }
        }
    }
    @Test fun insufficientSpaceDoesNotReplaceTheSelectedSizeWithASmallerHighlight() {
        val screen = Bounds(0f, 0f, 400f, 180f)
        assertNotNull(WheelAutofit.fit(WheelLayoutSettings(selectedScale = 1f), screen, 1f, 1, Edge.LEFT))
        assertNull(WheelAutofit.fit(WheelLayoutSettings(selectedScale = 1.25f), screen, 1f, 1, Edge.LEFT))
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
            { it.copy(wheel = it.wheel.copy(expandSelection = false)) })
        edits.forEach { assertFalse(layout.editSize(it).wheel.autoFit) }
        assertTrue(layout.editSize { it }.wheel.autoFit)
        assertTrue(layout.editSize { it.copy(wheel = it.wheel.copy(selectedScale = 1.4f)) }.wheel.autoFit)
        assertTrue(layout.editSize { it.copy(x = .8f, offsetYDp = 12f) }.wheel.autoFit)
        assertTrue(layout.editSize { it.copy(wheel = it.wheel.copy(animationsEnabled = false, focusDurationMs = 280)) }.wheel.autoFit)
    }
}
