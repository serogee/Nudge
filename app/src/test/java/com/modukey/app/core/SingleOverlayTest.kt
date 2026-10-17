package com.modukey.app.core

import org.junit.Assert.*
import org.junit.Test

class SingleOverlayTest {
    @Test fun `repeated bind and window events cannot duplicate the touch window`() {
        val calls = mutableListOf<String>()
        val overlay = SingleOverlay(object : OverlayHost<Int> {
            override fun attach(value: Int) { calls += "attach $value" }
            override fun update(value: Int) { calls += "update $value" }
            override fun detach() { calls += "detach" }
        })
        overlay.show(1)
        overlay.show(1)
        overlay.show(2)
        overlay.hide()
        overlay.hide()
        overlay.show(2)
        assertEquals(listOf("attach 1", "update 2", "detach", "attach 2"), calls)
    }

    @Test fun `failed attach is retried as attach rather than update`() {
        var attempts = 0
        val overlay = SingleOverlay(object : OverlayHost<Int> {
            override fun attach(value: Int) { if (++attempts == 1) error("blocked") }
            override fun update(value: Int) { error("no view was attached") }
            override fun detach() {}
        })
        runCatching { overlay.show(1) }
        overlay.show(1)
        assertEquals(2, attempts)
    }

    @Test fun `failed removal is attempted once and repeated cleanup is safe`() {
        var removals = 0
        val overlay = SingleOverlay(object : OverlayHost<Int> {
            override fun attach(value: Int) = Unit
            override fun update(value: Int) = Unit
            override fun detach() { removals++; error("window already removed by system") }
        })
        overlay.show(1)
        assertTrue(runCatching { overlay.hide() }.isFailure)
        overlay.hide()
        overlay.hide()
        assertEquals(1, removals)
    }
}
