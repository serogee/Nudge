package com.modukey.app.core

import org.junit.Assert.*
import org.junit.Test

class PreviewGeometryTest {
    @Test fun `landscape bottom maps to physical left when editing on an upright phone`() {
        val p = PreviewGeometry.calculate(400f, 800f, Insets(2f, 30f, 4f, 24f), 0, true)
        assertEquals(800f, p.width); assertEquals(400f, p.height)
        assertEquals(Insets(30f, 4f, 24f, 2f), p.insets)
        assertEquals(1, p.quarterTurns)
        assertEquals(Bounds(0f, 200f, 16f, 120f), p.toDisplay(Bounds(200f, 384f, 120f, 16f)))
        assertEquals(Bounds(180f, 0f, 120f, 16f), p.toDisplay(Bounds(0f, 100f, 16f, 120f)))
        assertEquals(Bounds(180f, 784f, 120f, 16f), p.toDisplay(Bounds(784f, 100f, 16f, 120f)))
    }
    @Test fun `runtime bottom stays bottom in both landscape rotations`() {
        val settings = TriggerSettings(portrait = Placement(Edge.BOTTOM, .5f), lengthDp = 350f, landscapeEnabled = true)
        for (rotation in listOf(1, 3)) {
            val p = PreviewGeometry.calculate(800f, 400f, Insets(bottom = 24f), rotation, true)
            assertEquals(0, p.quarterTurns)
            val bounds = TriggerGeometry.bounds(p.width, p.height, 1f, p.insets, settings, true)
            assertEquals(bounds, p.toDisplay(bounds))
            assertEquals(360f, bounds.y)
        }
    }
    @Test fun `portrait preview uses the correct side in reverse landscape and keeps safe bounds`() {
        for (rotation in listOf(1, 3)) {
            val actual = Insets(18f, 2f, 20f, 24f)
            val p = PreviewGeometry.calculate(800f, 400f, actual, rotation, false)
            assertEquals(if (rotation == 1) 3 else 1, p.quarterTurns)
            val settings = TriggerSettings(portrait = Placement(Edge.BOTTOM, .8f), lengthDp = 600f, longerTouchLength = true)
            val b = p.toDisplay(TriggerGeometry.bounds(p.width, p.height, 1f, p.insets, settings, false))
            assertTrue(b.x >= actual.left); assertTrue(b.y >= actual.top)
            assertTrue(b.x + b.width <= 800f - actual.right)
            assertTrue(b.y + b.height <= 400f - actual.bottom)
            if (rotation == 1) assertEquals(800f - actual.right, b.x + b.width) else assertEquals(actual.left, b.x)
            assertEquals(600f, settings.lengthDp)
        }
    }
}
