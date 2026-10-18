package com.modukey.app.core

import org.junit.Assert.*
import org.junit.Test

class SoftHideSessionTest {
    @Test fun `first gesture only reveals and the deadline hides without idle work`() {
        val s = SoftHideSession()
        s.configure(true, true, 3_000, 0)
        assertNull(s.deadline); assertFalse(s.revealed)
        assertTrue(s.gesture(100)); assertEquals(3_100L, s.deadline)
        assertFalse(s.gesture(200)); assertEquals(3_200L, s.deadline)
        s.expire(3_199); assertTrue(s.revealed)
        s.expire(3_200); assertFalse(s.revealed); assertNull(s.deadline)
        assertTrue(s.gesture(4_000))
    }
    @Test fun `contact and owning panels suspend expiry until interaction ends`() {
        val s = SoftHideSession(); s.configure(true, true, 7_000, 0); s.gesture(0)
        s.touch(true, 100); assertNull(s.deadline)
        s.touch(false, 200); assertEquals(7_200L, s.deadline)
        s.panel(true, 300); assertNull(s.deadline)
        s.panel(false, 400); assertEquals(7_400L, s.deadline)
        s.reset(); assertNull(s.deadline); assertFalse(s.revealed)
    }
    @Test fun `unknown visibility and disabled settings retain normal operation`() {
        val s = SoftHideSession()
        for (fullScreen in listOf<Boolean?>(null, false)) {
            s.configure(true, fullScreen, 3_000, 0)
            assertFalse(s.active); assertFalse(s.gesture(1)); assertNull(s.deadline)
        }
        s.configure(true, true, 3_000, 0); s.gesture(0)
        s.configure(false, true, 3_000, 5)
        assertFalse(s.active); assertFalse(s.revealed); assertNull(s.deadline)
    }
}
