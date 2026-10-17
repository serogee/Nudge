package com.modukey.app.core

import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CustomizationSessionTest {
    @Test fun `preview remains active beyond ten seconds and old activity cannot close its replacement`() = runTest {
        val session = CustomizationSession()
        val first = Any()
        val replacement = Any()
        session.begin(first)
        advanceTimeBy(60_000)
        assertTrue(session.active.value)
        session.begin(replacement)
        session.end(first)
        assertTrue(session.active.value)
        session.end(replacement)
        assertFalse(session.active.value)
        session.end(replacement)
        assertFalse(session.active.value)
    }
}
