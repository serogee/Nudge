package com.modukey.app.core

import org.junit.Assert.*
import org.junit.Test

class TriggerVisibilityTest {
    @Test fun `lock screen screen-off master switch and landscape are independent gates`() {
        val s = TriggerSettings(enabled = true)
        assertTrue(TriggerVisibility.allowed(s, true, false, false, "browser"))
        assertFalse(TriggerVisibility.allowed(s, true, true, false, "browser"))
        assertFalse(TriggerVisibility.allowed(s, false, false, false, "browser"))
        assertFalse(TriggerVisibility.allowed(s.copy(enabled = false), true, false, false, "browser"))
        assertFalse(TriggerVisibility.allowed(s, true, false, true, "browser"))
        assertTrue(TriggerVisibility.allowed(s.copy(landscapeEnabled = true), true, false, true, "browser"))
    }

    @Test fun `excluded apps and unknown foreground after rebind hide the trigger`() {
        val s = TriggerSettings(enabled = true, excludedPackages = setOf("game"))
        assertFalse(TriggerVisibility.allowed(s, true, false, false, "game"))
        assertFalse(TriggerVisibility.allowed(s, true, false, false, null))
        assertTrue(TriggerVisibility.allowed(s, true, false, false, "browser"))
    }

    @Test fun `lock screen opt-in ignores apps behind keyguard but never wakes the screen`() {
        val s = TriggerSettings(enabled = true, lockScreenEnabled = true, excludedPackages = setOf("game"))
        assertTrue(TriggerVisibility.allowed(s, true, true, false, "game"))
        assertTrue(TriggerVisibility.allowed(s, true, true, false, null))
        assertFalse(TriggerVisibility.allowed(s, true, false, false, "game"))
        assertFalse(TriggerVisibility.allowed(s, false, true, false, "game"))
        assertFalse(TriggerVisibility.allowed(s, true, true, true, "game"))
        assertFalse(TriggerVisibility.allowed(s.copy(enabled = false), true, true, false, "game"))
        assertFalse(s.indicatorVisible)
        assertFalse(s.landscapeEnabled)
    }

    @Test fun `editing can preview with master off but never on keyguard or screen off`() {
        assertTrue(TriggerVisibility.editingAllowed(true, false))
        assertFalse(TriggerVisibility.editingAllowed(true, true))
        assertFalse(TriggerVisibility.editingAllowed(false, false))
    }
}
