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
    @Test fun switchingPresetsCannotRestoreMaximumOrReactivateAnOlderPreset() {
        val session = BrightnessSession()
        val maximum = apply(session, BrightnessPreset.MAXIMUM, original)
        val minimum = apply(session, BrightnessPreset.MINIMUM, maximum)
        assertFalse(session.active(BrightnessPreset.MAXIMUM, minimum))
        assertEquals(BrightnessValue(22, false), apply(session, BrightnessPreset.MINIMUM, minimum))
        assertFalse(session.tracking)
    }
    @Test fun `external level or mode changes discard stale restore values even if the limit returns`() {
        for (changed in listOf(BrightnessValue(101, false), BrightnessValue(255, true))) {
            val session = BrightnessSession()
            val maximum = apply(session, BrightnessPreset.MAXIMUM, original)
            session.observe(changed); session.observe(maximum)
            assertFalse(session.tracking)
            assertNotNull(session.change(BrightnessPreset.MAXIMUM, maximum, 1, 255))
            val next = requireNotNull(session.change(BrightnessPreset.MINIMUM, changed, 1, 255))
            assertEquals(changed, next.before); assertFalse(next.restoring)
        }
    }
    @Test fun manuallyMaxedBrightnessReturnsImmediatelyWithoutCapturingTheMaximum() {
        for (previous in listOf(true, false)) {
            val session = BrightnessSession()
            val change = session.change(BrightnessPreset.MAXIMUM, BrightnessValue(255, false), 1, 255,
                BrightnessReturnSettings(previous, .5f))
            assertTrue(change.restoring)
            assertEquals(BrightnessValue(22, false), change.after)
            session.applied(change); assertFalse(session.tracking)
        }
    }
    @Test fun customReturnOverridesPreviousAndClearsOwnershipEvenAtTheLimit() {
        val session = BrightnessSession()
        val maximum = apply(session, BrightnessPreset.MAXIMUM, original)
        val change = session.change(BrightnessPreset.MAXIMUM, maximum, 1, 255, BrightnessReturnSettings(false, .25f))
        assertEquals(BrightnessValue(6, false), change.after)
        session.applied(change); assertFalse(session.tracking)
        apply(session, BrightnessPreset.MAXIMUM, change.after)
        val unchanged = session.change(BrightnessPreset.MAXIMUM, maximum, 1, 255, BrightnessReturnSettings(false, 1f))
        session.applied(unchanged)
        assertEquals(maximum, unchanged.after); assertFalse(session.active(BrightnessPreset.MAXIMUM, maximum))
    }
    @Test fun eitherPresetFallsBackWhenPreviousBrightnessIsMaximumOrItsOwnTarget() {
        for (preset in BrightnessPreset.entries) {
            for (before in listOf(255, if (preset == BrightnessPreset.MAXIMUM) 255 else 1)) {
                val session = BrightnessSession()
                val current = apply(session, preset, BrightnessValue(before, true))
                val change = session.change(preset, current, 1, 255, BrightnessReturnSettings(true, .75f))
                assertEquals(BrightnessValue(68, false), change.after)
                session.applied(change); assertFalse(session.tracking)
            }
        }
    }
    @Test fun `unapplied writes and fresh sessions cannot create restore values`() {
        val session = BrightnessSession()
        assertNotNull(session.change(BrightnessPreset.MAXIMUM, original, 1, 255))
        assertFalse(session.tracking)
        assertNotNull(session.change(BrightnessPreset.MAXIMUM, BrightnessValue(255, false), 1, 255))
        assertEquals(BrightnessValue(12, false), requireNotNull(session.change(BrightnessPreset.MINIMUM, original, 12, 4095)).after)
        apply(session, BrightnessPreset.MAXIMUM, original); session.clear()
        assertFalse(session.tracking)
    }
}
