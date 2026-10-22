package com.modukey.app.core

import org.junit.Assert.*
import org.junit.Test

class SwipeDirectionTest {
    private fun gesture(edge: Edge = Edge.LEFT, immediate: Boolean = false) = HandleGesture(edge, 24f, 600, 8f,
        swipeImmediately = immediate, swipeHoldEnabled = false, swipeDistanceMode = SwipeDistanceMode.ANY_DIRECTION)

    @Test fun anyDirectionAcceptsParallelDiagonalAndOutwardDisplacementOnEveryEdge() {
        for (edge in Edge.entries) for ((x, y) in listOf(0f to 30f, 18f to 18f, -30f to 0f)) {
            val g = gesture(edge)
            g.down(0f, 0f, 0); g.move(x, y, 100)
            assertEquals(GestureKind.SWIPE, g.up(x, y, 200))
        }
    }

    @Test fun returningInsideTheDistanceDoesNotUseAccumulatedPathLength() {
        val g = gesture()
        g.down(0f, 0f, 0); g.move(30f, 0f, 100); g.move(0f, 10f, 200)
        assertNull(g.up(0f, 10f, 250))
        g.down(0f, 0f, 0); g.move(15f, 0f, 100); g.move(0f, 15f, 200)
        assertNull(g.up(0f, 15f, 250))
    }

    @Test fun anyDirectionStillUsesTheSwipeTimeoutAndOnlyEmitsOnceWhenCaptured() {
        val expired = gesture()
        expired.down(0f, 0f, 0); expired.move(0f, 30f, 601)
        assertNull(expired.up(0f, 30f, 650))
        val g = gesture(immediate = true)
        g.down(0f, 0f, 0); g.move(0f, 30f, 100)
        assertEquals(GestureKind.SWIPE, g.poll(100))
        assertNull(g.poll(200)); assertNull(g.up(0f, 60f, 300))
    }

    @Test fun normalAndHeldDirectionsRemainIndependent() {
        val g = HandleGesture(Edge.LEFT, 24f, 600, 8f, holdDistanceMode = SwipeHoldDistanceMode.INWARD_ONLY,
            swipeDistanceMode = SwipeDistanceMode.ANY_DIRECTION)
        g.down(0f, 0f, 0); g.move(0f, 30f, 100)
        assertEquals(GestureKind.SWIPE, g.up(0f, 30f, 250))
        val inward = HandleGesture(Edge.LEFT, 24f, 600, 8f, swipeHoldEnabled = false)
        inward.down(0f, 0f, 0); inward.move(0f, 30f, 100)
        assertNull(inward.up(0f, 30f, 250))
    }
}
