package com.modukey.app.core

import org.junit.Assert.*
import org.junit.Test

class TriggerGeometryTest {
    @Test fun `right edge is bounded by display safe area and density`() {
        val bounds = TriggerGeometry.bounds(1080f, 2400f, 3f, Insets(0f, 90f, 0f, 90f), TriggerSettings(), false)
        assertEquals(1032f, bounds.x, 0.01f)
        assertEquals(48f, bounds.width, 0.01f)
        assertEquals(360f, bounds.height, 0.01f)
        assertEquals(90f + (2220f - 360f) * 0.65f, bounds.y, 0.01f)
    }

    @Test fun `bottom placement stays above mandatory Home region and uses landscape setting`() {
        val config = TriggerSettings(landscape = Placement(Edge.BOTTOM, 1f), landscapeEnabled = true)
        val bounds = TriggerGeometry.bounds(2400f, 1080f, 3f, Insets(90f, 0f, 0f, 80f), config, true)
        assertEquals(Bounds(2040f, 952f, 360f, 48f), bounds)
    }

    @Test fun `tiny display clamps touch and visual geometry`() {
        val config = TriggerSettings(portrait = Placement(Edge.LEFT, 1f))
        val bounds = TriggerGeometry.bounds(10f, 30f, 3f, Insets(), config, false)
        assertEquals(Bounds(0f, 0f, 10f, 30f), bounds)
        val visible = TriggerGeometry.indicator(bounds.width, bounds.height, 3f, config, Edge.LEFT)
        assertEquals(bounds, visible)
    }

    @Test fun `corrupt or outdated preferences cannot make invalid touch windows`() {
        val config = TriggerSettings(lengthDp = -4f, depthDp = Float.NaN, portrait = Placement(Edge.RIGHT, Float.POSITIVE_INFINITY)).normalized()
        assertEquals(48f, config.lengthDp)
        assertEquals(16f, config.depthDp)
        assertEquals(0.65f, config.portrait.position)
    }
}
