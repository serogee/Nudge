package com.modukey.app.core

import org.junit.Assert.*
import org.junit.Test

class FrameCoalescerTest {
    @Test fun `adjustments coalesce and idle rendering never schedules more work`() {
        var scheduled = 0
        var cancelled = 0
        var renders = 0
        val frames = FrameCoalescer({ scheduled++ }, { cancelled++ }, { renders++ })
        repeat(100) { frames.request() }
        assertEquals(1, scheduled)
        frames.onFrame()
        assertEquals(1, renders)
        repeat(100) { frames.onFrame() }
        assertEquals(1, renders)
        assertEquals(1, scheduled)
        frames.request()
        frames.stop()
        frames.stop()
        frames.onFrame()
        assertEquals(1, cancelled)
        assertEquals(1, renders)
    }
}
