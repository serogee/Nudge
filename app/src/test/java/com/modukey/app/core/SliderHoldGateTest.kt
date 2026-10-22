package com.modukey.app.core

import org.junit.Assert.*
import org.junit.Test

class SliderHoldGateTest {
    @Test fun motionOnEitherAxisRestartsDwellButSmallJitterDoesNot() {
        val gate = SliderHoldGate(30f, 200f, 100, 350, 8f)
        assertNull(gate.move(35f, 204f, 400))
        assertEquals(450L, gate.deadline)
        assertNull(gate.move(38f, 208f, 449)) // Diagonal displacement exceeds the tolerance.
        assertEquals(799L, gate.deadline)
        assertNull(gate.move(48f, 208f, 798)) // Horizontal movement also restarts the timer.
        assertFalse(gate.poll(1147))
        assertTrue(gate.poll(1148)); assertNull(gate.deadline)
        assertEquals(208f, gate.armedY)
        assertFalse(gate.poll(2000))
    }
    @Test fun armingRebasesAtLatestPositionAndConsumesDeadZoneOnlyOnce() {
        val gate = SliderHoldGate(30f, 200f, 0, 200, 8f)
        gate.move(31f, 204f, 100)
        assertTrue(gate.poll(200))
        assertNull(gate.move(500f, 196f, 210))
        assertEquals(184f, gate.move(500f, 176f, 220)) // Only 20 px of the 28 px move counts.
        assertEquals(194f, gate.move(-500f, 186f, 230)) // Reversal responds immediately.
        val downward = SliderHoldGate(0f, 100f, 0, 100, 8f)
        assertTrue(downward.poll(100))
        assertEquals(110f, downward.move(0f, 118f, 110))
    }
    @Test fun lateMotionMustBeCheckedBeforeArmingAndInvalidInputDoesNotChangeTheAnchor() {
        val gate = SliderHoldGate(0f, 100f, 0, 200, 8f)
        assertNull(gate.move(0f, 50f, 250))
        assertFalse(gate.armed); assertEquals(450L, gate.deadline)
        assertNull(gate.move(Float.NaN, 0f, 400))
        assertTrue(gate.poll(450)); assertEquals(50f, gate.armedY)
    }
}
