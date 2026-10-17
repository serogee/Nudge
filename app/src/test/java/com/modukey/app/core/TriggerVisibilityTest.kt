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
}
