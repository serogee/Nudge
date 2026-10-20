package com.modukey.app.core

import org.junit.Assert.*
import org.junit.Test

class HoldRecognitionTest {
    @Test fun independentSwipeHoldDistanceDoesNotChangeOrdinarySwipeDistance() {
        val g = HandleGesture(Edge.LEFT, 60f, 600, 8f, holdDistance = 24f)
        g.down(0f, 0f, 0); g.move(30f, 0f, 100)
        assertEquals(GestureKind.SWIPE_HOLD, g.up(30f, 0f, 650))
        g.down(0f, 0f, 1000); assertNull(g.up(30f, 0f, 1200))
        g.down(0f, 0f, 2000); assertEquals(GestureKind.SWIPE, g.up(65f, 0f, 2100))
    }
    @Test fun immediateHoldsFireAtTheDeadlineAndNeverRepeatOnRelease() {
        val g = HandleGesture(Edge.LEFT, 24f, 600, 8f, holdImmediately = true, swipeHoldImmediately = true)
        g.down(0f, 0f, 0)
        assertEquals(500L, g.nextDeadline()); assertNull(g.poll(499))
        assertEquals(GestureKind.HOLD, g.poll(500)); assertNull(g.poll(501)); assertNull(g.up(0f, 0f, 600))
        g.down(0f, 0f, 1000); g.move(30f, 0f, 1100)
        assertEquals(1600L, g.nextDeadline()); assertNull(g.poll(1599))
        assertEquals(GestureKind.SWIPE_HOLD, g.poll(1600)); assertNull(g.up(30f, 0f, 1650))
    }
    @Test fun releaseJitterDoesNotRestartACompletedDwell() {
        val g = HandleGesture(Edge.LEFT, 24f, 600, 8f)
        g.down(0f, 0f, 0); g.move(30f, 0f, 100)
        g.move(39f, 2f, 300)
        assertEquals(GestureKind.SWIPE_HOLD, g.up(48f, 3f, 650))
    }
    @Test fun briefInitialDiagonalMovementCanBecomeAValidInwardSwipe() {
        val g = HandleGesture(Edge.LEFT, 24f, 600, 8f)
        g.down(0f, 0f, 0); g.move(9f, 11f, 50); g.move(30f, 12f, 100)
        assertEquals(GestureKind.SWIPE_HOLD, g.up(30f, 12f, 650))
    }
    @Test fun ongoingMovementRestartsTheDwellAndCancellationRemovesTheDeadline() {
        val g = HandleGesture(Edge.LEFT, 24f, 600, 8f, swipeHoldImmediately = true)
        g.down(0f, 0f, 0); g.move(30f, 0f, 100); g.move(60f, 0f, 400)
        assertEquals(900L, g.nextDeadline()); assertNull(g.poll(899))
        g.cancel(); assertNull(g.nextDeadline()); assertNull(g.poll(900)); assertNull(g.up(60f, 0f, 1000))
    }
    @Test fun releaseModeDoesNotScheduleADeadline() {
        val g = HandleGesture(Edge.BOTTOM, 24f, 600, 8f)
        g.down(0f, 100f, 0); assertNull(g.nextDeadline())
        g.move(0f, 70f, 100); assertNull(g.nextDeadline()); assertNull(g.poll(700))
        assertEquals(GestureKind.SWIPE_HOLD, g.up(0f, 70f, 700))
    }
}
