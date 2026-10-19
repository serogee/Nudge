package com.modukey.app.core

import org.junit.Assert.*
import org.junit.Test

class ShortcutRuntimeTest {
    @Test fun disconnectedCommandsAreNotReplayedAndOldOwnersCannotClearNewConnections() {
        val runtime = ShortcutRuntime(); val old = Any(); val current = Any(); val calls = mutableListOf<String>()
        assertFalse(runtime.run("before"))
        runtime.bind(old) { calls += it; true }; assertTrue(calls.isEmpty())
        runtime.bind(current) { calls += it; true }; runtime.unbind(old)
        assertTrue(runtime.run("now")); assertEquals(listOf("now"), calls)
        runtime.unbind(current); assertFalse(runtime.run("after")); assertEquals(listOf("now"), calls)
    }
}
