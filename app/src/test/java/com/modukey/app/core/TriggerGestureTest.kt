package com.modukey.app.core

import org.junit.Assert.*
import org.junit.Test

class TriggerGestureTest {
    private fun swipe(edge: Edge = Edge.RIGHT) = TriggerGesture(edge, Activation.SWIPE, 24f, 600, 8f)

    @Test fun `inward release activates once at threshold`() {
        val gesture = swipe()
        gesture.down(100f, 100f, 0)
        gesture.move(70f, 102f)
        assertTrue(gesture.up(76f, 102f, 600))
        assertFalse(gesture.up(70f, 100f, 601))
    }

    @Test fun `all edges use the correct inward direction`() {
        for ((edge, target) in listOf(Edge.LEFT to (130f to 100f), Edge.RIGHT to (70f to 100f), Edge.BOTTOM to (100f to 70f))) {
            val gesture = swipe(edge)
            gesture.down(100f, 100f, 0)
            assertTrue(gesture.up(target.first, target.second, 100))
        }
    }

    @Test fun `short slow outward parallel and cancelled swipes do not activate`() {
        for ((x, y, time) in listOf(Triple(90f, 100f, 100L), Triple(70f, 100f, 601L), Triple(130f, 100f, 100L), Triple(70f, 150f, 100L))) {
            val gesture = swipe()
            gesture.down(100f, 100f, 0)
            assertFalse(gesture.up(x, y, time))
        }
        val gesture = swipe()
        gesture.down(100f, 100f, 0)
        gesture.cancel() // CANCEL and additional pointers share this path.
        assertFalse(gesture.up(60f, 100f, 100))
        gesture.down(100f, 100f, 200)
        assertTrue(gesture.up(60f, 100f, 300))
    }

    @Test fun `parallel detour stays cancelled even after changing direction`() {
        val gesture = swipe()
        gesture.down(100f, 100f, 0)
        gesture.move(100f, 125f)
        assertFalse(gesture.up(60f, 100f, 200))
    }

    @Test fun `tap and long press are mutually exclusive and require a still finger`() {
        for ((mode, duration, expected) in listOf(Triple(Activation.TAP, 200L, true), Triple(Activation.TAP, 500L, false), Triple(Activation.LONG_PRESS, 499L, false), Triple(Activation.LONG_PRESS, 500L, true))) {
            val gesture = TriggerGesture(Edge.RIGHT, mode, 24f, 600, 8f)
            gesture.down(0f, 0f, 0)
            assertEquals(expected, gesture.up(0f, 0f, duration))
            gesture.down(0f, 0f, 0)
            gesture.move(10f, 0f)
            assertFalse(gesture.up(0f, 0f, duration))
        }
    }
}
