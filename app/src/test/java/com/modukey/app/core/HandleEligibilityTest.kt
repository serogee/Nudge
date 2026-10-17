package com.modukey.app.core

import org.junit.Assert.*
import org.junit.Test

class HandleEligibilityTest {
    @Test fun `lock availability ignores exclusions but never permits screen off`() {
        val h=HandleConfig(enabled=true,trigger=TriggerSettings(lockScreenEnabled=true,excludedPackages=setOf("game")))
        val config=AppConfiguration(enabled=true,handles=listOf(h))
        assertEquals(HandleAvailability.EXCLUDED,HandleEligibility.reason(config,h,true,false,false,"game"))
        assertEquals(HandleAvailability.ACTIVE,HandleEligibility.reason(config,h,true,true,false,"game"))
        assertEquals(HandleAvailability.SCREEN_OFF,HandleEligibility.reason(config,h,false,true,false,"game"))
        assertEquals(HandleAvailability.LOCKED,HandleEligibility.reason(config,h.copy(trigger=h.trigger.copy(lockScreenEnabled=false)),true,true,false,"game"))
        assertFalse(h.trigger.indicatorVisible); assertFalse(h.trigger.landscapeEnabled)
    }
    @Test fun `independent switches and landscape availability share one eligibility rule`() {
        val h=HandleConfig(enabled=true); val c=AppConfiguration(enabled=true)
        assertEquals(HandleAvailability.ACTIVE,HandleEligibility.reason(c,h,true,false,false,null))
        assertEquals(HandleAvailability.PAUSED,HandleEligibility.reason(c.copy(enabled=false),h,true,false,false,null))
        assertEquals(HandleAvailability.DISABLED,HandleEligibility.reason(c,h.copy(enabled=false),true,false,false,null))
        assertEquals(HandleAvailability.LANDSCAPE,HandleEligibility.reason(c,h,true,false,true,null))
        assertEquals(HandleAvailability.ACTIVE,HandleEligibility.reason(c,h.copy(trigger=h.trigger.copy(landscapeEnabled=true)),true,false,true,null))
    }
}
