package com.modukey.app.core

import org.junit.Assert.*
import org.junit.Test

class HoldTimingTest {
    private fun gesture(hold: Long = 500, swipeHold: Long = 500) = HandleGesture(Edge.LEFT, 24f, 600, 8f, hold, swipeHold)
    @Test fun `custom hold time has an exact boundary and keeps taps separate`() {
        val g = gesture(hold = 1_200)
        for (time in listOf(0L, 250L)) {
            g.down(0f, 0f, 0); assertEquals(GestureKind.TAP, g.up(0f, 0f, time))
        }
        for (time in listOf(251L, 500L, 1_199L)) {
            g.down(0f, 0f, 0); assertNull(g.up(0f, 0f, time))
        }
        g.down(0f, 0f, 0); assertEquals(GestureKind.HOLD, g.up(0f, 0f, 1_200))
        assertNull(g.up(0f, 0f, 1_201))
    }
    @Test fun `swipe hold time is independent and starts after the threshold`() {
        val g = gesture(hold = 1_200, swipeHold = 800)
        g.down(0f, 0f, 0); g.move(30f, 0f, 100)
        assertNull(g.up(30f, 0f, 899))
        g.down(0f, 0f, 0); g.move(30f, 0f, 100)
        assertEquals(GestureKind.SWIPE_HOLD, g.up(30f, 0f, 900))
        g.down(0f, 0f, 0); assertNull(g.up(0f, 0f, 900))
        g.down(0f, 0f, 0); g.move(30f, 0f, 100)
        assertEquals(GestureKind.SWIPE, g.up(30f, 0f, 200))
    }
    @Test fun `movement beyond the threshold keeps the selected hold interval`() {
        val g = gesture(swipeHold = 900)
        g.down(0f, 0f, 0); g.move(30f, 0f, 100); g.move(50f, 0f, 800)
        assertNull(g.up(50f, 0f, 999))
        g.down(0f, 0f, 0); g.move(30f, 0f, 100); g.move(50f, 0f, 800)
        assertEquals(GestureKind.SWIPE_HOLD, g.up(50f, 0f, 1_000))
    }
    @Test fun `retreat below the threshold restarts the hold interval after recrossing`() {
        val g = gesture(swipeHold = 900)
        for (release in listOf(1_799L, 1_800L)) {
            g.down(0f, 0f, 0); g.move(30f, 0f, 100)
            g.move(10f, 0f, 800); g.move(50f, 0f, 900)
            assertEquals(if (release == 1_800L) GestureKind.SWIPE_HOLD else null, g.up(50f, 0f, release))
        }
    }
    @Test fun `long hold does not bypass cancellation or late swipe thresholds`() {
        val g = gesture(hold = 3_000, swipeHold = 3_000)
        g.down(0f, 0f, 0); g.move(30f, 0f, 601)
        assertNull(g.up(30f, 0f, 3_601))
        g.down(0f, 0f, 0); g.cancel(); assertNull(g.up(0f, 0f, 3_000))
        g.down(0f, 0f, 0); g.move(0f, 30f, 100); assertNull(g.up(0f, 30f, 3_000))
    }
}
