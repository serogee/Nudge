package com.modukey.app.core

import org.junit.Assert.*
import org.junit.Test

class BrightnessSessionTest {
    private val original = BrightnessValue(100, true)
    private fun apply(session: BrightnessSession, preset: BrightnessPreset, value: BrightnessValue): BrightnessValue =
        requireNotNull(session.change(preset, value, 1, 255)).also(session::applied).after

    @Test fun `either limit toggles back to the previous level and automatic mode`() {
        for (preset in BrightnessPreset.entries) {
            val session = BrightnessSession()
            val limit = apply(session, preset, original)
            assertEquals(BrightnessValue(if (preset == BrightnessPreset.MAXIMUM) 255 else 1, false), limit)
            assertTrue(session.tracking)
            assertEquals(original, apply(session, preset, limit))
            assertFalse(session.tracking)
        }
    }
    @Test fun `switching between presets retains each previous value`() {
        val session = BrightnessSession()
        val maximum = apply(session, BrightnessPreset.MAXIMUM, original)
        val minimum = apply(session, BrightnessPreset.MINIMUM, maximum)
        assertEquals(maximum, apply(session, BrightnessPreset.MINIMUM, minimum))
        assertEquals(original, apply(session, BrightnessPreset.MAXIMUM, maximum))
    }
    @Test fun `external level or mode changes discard stale restore values even if the limit returns`() {
        for (changed in listOf(BrightnessValue(101, false), BrightnessValue(255, true))) {
            val session = BrightnessSession()
            val maximum = apply(session, BrightnessPreset.MAXIMUM, original)
            session.observe(changed); session.observe(maximum)
            assertFalse(session.tracking)
            assertNull(session.change(BrightnessPreset.MAXIMUM, maximum, 1, 255))
            val next = requireNotNull(session.change(BrightnessPreset.MINIMUM, changed, 1, 255))
            assertEquals(changed, next.before); assertFalse(next.restoring)
        }
    }
    @Test fun `unapplied writes and fresh sessions cannot create restore values`() {
        val session = BrightnessSession()
        assertNotNull(session.change(BrightnessPreset.MAXIMUM, original, 1, 255))
        assertFalse(session.tracking)
        assertNull(session.change(BrightnessPreset.MAXIMUM, BrightnessValue(255, false), 1, 255))
        assertEquals(BrightnessValue(12, false), requireNotNull(session.change(BrightnessPreset.MINIMUM, original, 12, 4095)).after)
        apply(session, BrightnessPreset.MAXIMUM, original); session.clear()
        assertFalse(session.tracking)
    }
}
