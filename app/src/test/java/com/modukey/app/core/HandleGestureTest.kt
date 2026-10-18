package com.modukey.app.core

import org.junit.Assert.*
import org.junit.Test

class HandleGestureTest {
    private fun gesture(edge: Edge = Edge.LEFT) = HandleGesture(edge, 24f, 600, 8f)
    @Test fun `tap and hold run on release with no duplicate result`() {
        val g = gesture(); g.down(0f, 0f, 0)
        assertEquals(GestureKind.TAP, g.up(0f, 0f, 250)); assertNull(g.up(0f, 0f, 250))
        g.down(0f, 0f, 300); assertEquals(GestureKind.HOLD, g.up(1f, 0f, 800))
        g.down(0f, 0f, 0); assertNull(g.up(0f, 0f, 300))
    }
    @Test fun `swipe hold is distinct from a normal swipe`() {
        val g = gesture(); g.down(0f, 0f, 0); g.move(30f, 0f, 100)
        assertEquals(GestureKind.SWIPE_HOLD, g.up(30f, 0f, 600))
        g.down(0f, 0f, 0); g.move(30f, 0f, 100)
        assertEquals(GestureKind.SWIPE, g.up(30f, 0f, 200))
    }
    @Test fun `late threshold parallel outward and cancelled gestures do nothing`() {
        for ((x, y) in listOf(-30f to 0f, 5f to 30f, 23f to 0f)) {
            val g = gesture(); g.down(0f, 0f, 0); assertNull(g.up(x, y, 100))
        }
        val g = gesture(); g.down(0f, 0f, 0); assertNull(g.up(30f, 0f, 601))
        g.down(0f, 0f, 0); g.cancel(); assertNull(g.up(30f, 0f, 100))
    }
    @Test fun `right and bottom use inward screen coordinates`() {
        val right = gesture(Edge.RIGHT); right.down(100f, 0f, 0); assertEquals(GestureKind.SWIPE, right.up(70f, 0f, 100))
        val bottom = gesture(Edge.BOTTOM); bottom.down(0f, 100f, 0); assertEquals(GestureKind.SWIPE, bottom.up(0f, 70f, 100))
    }
    @Test fun `volume deadline cycles skips gaps and expires at boundary`() {
        val h = HandleConfig(bindings = mapOf(GestureSlot.SWIPE_1 to GestureTarget.VOLUME, GestureSlot.SWIPE_3 to GestureTarget.panel("tools")))
        val s = GestureSessions()
        val first = s.next(h, GestureKind.SWIPE, 0); assertEquals(GestureTarget.VOLUME to 0, first)
        s.submitted(h, first.second, first.first, 0)
        assertEquals(GestureTarget.panel("tools") to 2, s.next(h, GestureKind.SWIPE, 2999))
        assertEquals(GestureTarget.VOLUME to 0, s.next(h, GestureKind.SWIPE, 3000))
        s.submitted(h, 2, GestureTarget.panel("tools"), 3001)
        assertEquals(GestureTarget.VOLUME to 0, s.next(h, GestureKind.SWIPE, 100000))
    }
    @Test fun `handles are independent and tap resets only its handle`() {
        val a = HandleConfig(); val b = HandleConfig(); val s = GestureSessions()
        s.submitted(a, 0, GestureTarget.panel("tools"), 0); s.submitted(b, 0, GestureTarget.panel("tools"), 0)
        s.next(a, GestureKind.TAP, 100); s.reset(b.id)
        assertEquals(GestureTarget.VOLUME to 0, s.next(a, GestureKind.SWIPE, 100))
        assertEquals(GestureTarget.VOLUME to 0, s.next(b, GestureKind.SWIPE, 100))
    }
    @Test fun `combined targets advance while their panel is open and none stages are skipped`() {
        val target = GestureTarget.panel("tools", volume = true)
        val h = HandleConfig(bindings = mapOf(GestureSlot.SWIPE_1 to target, GestureSlot.SWIPE_3 to GestureTarget.VOLUME))
        val sessions = GestureSessions()
        assertEquals(target to 0, sessions.next(h, GestureKind.SWIPE, 0))
        sessions.submitted(h, 0, target, 0)
        assertEquals(GestureTarget.VOLUME to 2, sessions.next(h, GestureKind.SWIPE, 100_000))
        sessions.reset(h.id)
        assertEquals(target to 0, sessions.next(h, GestureKind.SWIPE, 100_001))
    }
    @Test fun `direct swipe actions cycle skip gaps and reset exactly at the saved deadline`() {
        val up=GestureTarget.direct(PanelButton(action=ButtonAction.VOLUME_UP))
        val down=GestureTarget.direct(PanelButton(action=ButtonAction.VOLUME_DOWN))
        val h=HandleConfig(volumeSequenceMs=5_000,bindings=mapOf(GestureSlot.SWIPE_1 to up,GestureSlot.SWIPE_3 to down,GestureSlot.HOLD to down))
        val sessions=GestureSessions()
        assertEquals(up to 0,sessions.next(h,GestureKind.SWIPE,0))
        sessions.submitted(h,0,up,0)
        assertEquals(down to 2,sessions.next(h,GestureKind.SWIPE,4_999))
        assertEquals(up to 0,sessions.next(h,GestureKind.SWIPE,5_000))
        sessions.submitted(h,2,down,5_000)
        assertEquals(up to 0,sessions.next(h,GestureKind.SWIPE,5_001))
        assertEquals(down to null,sessions.next(h,GestureKind.HOLD,5_002))
        assertEquals(up to 0,sessions.next(h,GestureKind.SWIPE,5_003))
    }
}
