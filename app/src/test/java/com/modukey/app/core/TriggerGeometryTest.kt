package com.modukey.app.core

import org.junit.Assert.*
import org.junit.Test

class TriggerGeometryTest {
    @Test fun `right edge is bounded by display safe area and density`() {
        val bounds = TriggerGeometry.bounds(1080f, 2400f, 3f, Insets(0f, 90f, 0f, 90f), TriggerSettings(), false)
        assertEquals(1032f, bounds.x, 0.01f)
        assertEquals(48f, bounds.width, 0.01f)
        assertEquals(360f, bounds.height, 0.01f)
        assertEquals(90f + 2220f * 0.65f - 360f / 2, bounds.y, 0.01f)
    }

    @Test fun `bottom placement stays above mandatory Home region and uses landscape setting`() {
        val config = TriggerSettings(landscape = Placement(Edge.BOTTOM, 1f), landscapeEnabled = true, landscapeOverrideEnabled = true)
        val bounds = TriggerGeometry.bounds(2400f, 1080f, 3f, Insets(90f, 0f, 0f, 80f), config, true)
        assertEquals(Bounds(2040f, 952f, 360f, 48f), bounds)
    }

    @Test fun `tiny display clamps touch and visual geometry`() {
        val config = TriggerSettings(portrait = Placement(Edge.LEFT, 1f))
        val bounds = TriggerGeometry.bounds(10f, 30f, 3f, Insets(), config, false)
        assertEquals(Bounds(0f, 0f, 10f, 30f), bounds)
        val visible = TriggerGeometry.indicator(bounds.width, bounds.height, 3f, config, Edge.LEFT)
        assertEquals(Bounds(0f, 5f, 10f, 20f), visible)
    }

    @Test fun `corrupt or outdated preferences cannot make invalid touch windows`() {
        val config = TriggerSettings(lengthDp = -4f, depthDp = Float.NaN, portrait = Placement(Edge.RIGHT, Float.POSITIVE_INFINITY)).normalized()
        assertEquals(48f, config.lengthDp)
        assertEquals(16f, config.depthDp)
        assertEquals(0.65f, config.portrait.position)
    }

    @Test fun `resizing preserves the chosen center and clamps at either corner`() {
        for (edge in Edge.entries) {
            for (position in listOf(0f, 0.5f, 1f)) {
                val s = TriggerSettings(portrait = Placement(edge, position))
                val a = TriggerGeometry.bounds(1000f, 2000f, 2f, Insets(20f, 40f, 60f, 80f), s, false)
                val b = TriggerGeometry.bounds(1000f, 2000f, 2f, Insets(20f, 40f, 60f, 80f), s.copy(lengthDp = 200f), false)
                val horizontal = edge == Edge.BOTTOM
                fun start(r: Bounds) = if (horizontal) r.x else r.y
                fun length(r: Bounds) = if (horizontal) r.width else r.height
                if (position == 0.5f) assertEquals(start(a) + length(a) / 2, start(b) + length(b) / 2, 0.001f)
                if (position == 0f) assertEquals(start(a), start(b), 0.001f)
                if (position == 1f) assertEquals(start(a) + length(a), start(b) + length(b), 0.001f)
            }
        }
    }

    @Test fun `drag uses the same center and preserves off-center and clamped grabs`() {
        for (position in listOf(0f, 0.5f, 1f)) {
            val start = TriggerGeometry.start(position, 120f, 800f)
            val pointer = start + 20f
            val grab = pointer - position * 800f
            assertEquals(position, TriggerGeometry.draggedPosition(pointer, grab, 800f), 0.0001f)
            assertEquals((position + 0.1f).coerceAtMost(1f), TriggerGeometry.draggedPosition(pointer + 80f, grab, 800f), 0.0001f)
        }
    }

    @Test fun `landscape inherits logical bottom until explicitly overridden and retains override values`() {
        val s = TriggerSettings(portrait = Placement(Edge.BOTTOM, 0.3f), landscape = Placement(Edge.LEFT, 0.8f), landscapeEnabled = true)
        for (insets in listOf(Insets(90f, 0f, 0f, 80f), Insets(0f, 0f, 90f, 80f))) {
            val b = TriggerGeometry.bounds(2400f, 1080f, 3f, insets, s, true)
            assertEquals(952f, b.y, 0.001f)
            assertEquals(360f, b.width, 0.001f)
        }
        assertEquals(s.portrait, s.placement(true))
        assertEquals(s.landscape, s.copy(landscapeOverrideEnabled = true).placement(true))
        val disabled = s.copy(landscapeOverrideEnabled = true).copy(landscapeOverrideEnabled = false)
        assertEquals(Placement(Edge.LEFT, 0.8f), disabled.landscape)
        assertEquals(s.portrait, disabled.placement(true))
    }

    @Test fun `appearance settings are independent and constrained only when rendered`() {
        val s = TriggerSettings(lengthDp = 200f, depthDp = 32f, indicatorLengthFraction = 0.73f, indicatorThicknessDp = 30f, cornerRadiusDp = 15f)
        val small = s.copy(lengthDp = 48f, depthDp = 8f).normalized()
        assertEquals(0.73f, small.indicatorLengthFraction)
        assertEquals(30f, small.indicatorThicknessDp)
        assertEquals(15f, small.cornerRadiusDp)
        val rendered = TriggerGeometry.indicator(8f, 48f, 1f, small, Edge.RIGHT)
        assertEquals(48 * 0.73f, rendered.height, 0.001f)
        assertEquals(8f, rendered.width)
        val restored = small.copy(lengthDp = 200f, depthDp = 32f).normalized()
        assertEquals(s, restored)
    }
}
