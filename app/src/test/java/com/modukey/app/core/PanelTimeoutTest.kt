package com.modukey.app.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PanelTimeoutTest {
    @Test fun `disabled and stopped timers leave no pending task`() = runTest {
        var expired = 0
        val timer = PanelTimeout(this) { expired++ }
        timer.configure(0); timer.restart(); timer.touch(true); timer.touch(false)
        runCurrent()
        assertTrue(coroutineContext[Job]!!.children.none())
        timer.configure(1_000); runCurrent(); timer.stop(); runCurrent()
        assertTrue(coroutineContext[Job]!!.children.none())
        advanceTimeBy(60_000); runCurrent(); assertEquals(0, expired)
    }
    @Test fun `timeout expires once at its boundary`() = runTest {
        var expired = 0
        val timer = PanelTimeout(this) { expired++ }
        timer.configure(3_000)
        advanceTimeBy(2_999); runCurrent(); assertEquals(0, expired)
        advanceTimeBy(1); runCurrent(); assertEquals(1, expired)
        advanceTimeBy(60_000); runCurrent(); assertEquals(1, expired)
        assertTrue(coroutineContext[Job]!!.children.none())
    }
    @Test fun `reopening restarts timeout and unchanged refresh keeps the deadline`() = runTest {
        var expired = 0
        val timer = PanelTimeout(this) { expired++ }
        timer.configure(1_000)
        advanceTimeBy(500); timer.restart()
        advanceTimeBy(500); timer.configure(1_000)
        advanceTimeBy(499); runCurrent(); assertEquals(0, expired)
        advanceTimeBy(1); runCurrent(); assertEquals(1, expired)
    }
    @Test fun `held input pauses the timeout and release starts a full interval`() = runTest {
        var expired = 0
        val timer = PanelTimeout(this) { expired++ }
        timer.configure(1_000)
        advanceTimeBy(500); timer.touch(true)
        advanceTimeBy(10_000); runCurrent(); assertEquals(0, expired)
        assertTrue(coroutineContext[Job]!!.children.none())
        timer.touch(false)
        advanceTimeBy(999); runCurrent(); assertEquals(0, expired)
        advanceTimeBy(1); runCurrent(); assertEquals(1, expired)
    }
    @Test fun `configuration changes and replacement cancel stale timeouts`() = runTest {
        var expired = 0
        val timer = PanelTimeout(this) { expired++ }
        timer.configure(1_000)
        advanceTimeBy(500); timer.configure(3_000)
        advanceTimeBy(1_000); runCurrent(); assertEquals(0, expired)
        timer.stop(); timer.configure(5_000)
        advanceTimeBy(2_000); runCurrent(); assertEquals(0, expired)
        timer.configure(0)
        advanceTimeBy(60_000); runCurrent(); assertEquals(0, expired)
    }
}
