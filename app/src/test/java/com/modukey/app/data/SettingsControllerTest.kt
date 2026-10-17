package com.modukey.app.data

import com.modukey.app.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsControllerTest {
    private class SlowStore : SettingsRepository {
        val disk = MutableStateFlow(TriggerSettings())
        override val settings = disk.asStateFlow()
        var writes = 0
        var latency = 0L
        var fail = false
        override suspend fun update(transform: (TriggerSettings) -> TriggerSettings) {
            writes++
            delay(latency)
            if (fail) throw IOException("full")
            disk.value = transform(disk.value)
        }
        override suspend fun reset() = update { TriggerSettings() }
    }

    @Test fun `updates are immediate and saves wait for quiet or gesture completion`() = runTest {
        val store = SlowStore()
        val controller = SettingsController(store, backgroundScope)
        runCurrent()
        repeat(20) { n ->
            controller.update { it.copy(lengthDp = 120f + n) }
            assertEquals(120f + n, controller.state.value!!.lengthDp)
            advanceTimeBy(100)
            runCurrent()
        }
        assertEquals(0, store.writes)
        controller.flush()
        runCurrent()
        assertEquals(1, store.writes)
        assertEquals(139f, store.disk.value.lengthDp)
        controller.update { it.copy(depthDp = 25f) }
        advanceTimeBy(149)
        runCurrent()
        assertEquals(1, store.writes)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(2, store.writes)
    }

    @Test fun `slow save rapid independent edits and stale emissions cannot rewind the draft`() = runTest {
        val store = SlowStore().apply { latency = 1_000 }
        val c = SettingsController(store, backgroundScope)
        runCurrent()
        c.update { it.copy(lengthDp = 160f) }
        c.flush()
        runCurrent()
        c.update { it.copy(indicatorOpacity = 0.2f) }
        c.update { it.copy(indicatorThicknessFraction = 0.7f) }
        c.update { it.copy(portrait = it.portrait.copy(edge = Edge.BOTTOM)) }
        c.update { it.copy(portrait = it.portrait.copy(position = 0.2f)) }
        c.update { it.copy(lengthDp = 190f) }
        store.disk.value = TriggerSettings(depthDp = 30f)
        runCurrent()
        assertEquals(190f, c.state.value!!.lengthDp)
        assertEquals(Placement(Edge.BOTTOM, 0.2f), c.state.value!!.portrait)
        assertEquals(0.2f, c.state.value!!.indicatorOpacity)
        assertEquals(0.7f, c.state.value!!.indicatorThicknessFraction)
        c.flush()
        advanceTimeBy(3_000)
        runCurrent()
        assertEquals(190f, store.disk.value.lengthDp)
        assertEquals(30f, store.disk.value.depthDp)
        assertEquals(0.7f, store.disk.value.indicatorThicknessFraction)
        assertEquals(Placement(Edge.BOTTOM, 0.2f), store.disk.value.portrait)
        assertEquals(store.disk.value, c.state.value)
        store.disk.value = TriggerSettings()
        runCurrent()
        assertEquals(190f, c.state.value!!.lengthDp)
    }

    @Test fun `failed persistence retains all dirty fields and retries newest values`() = runTest {
        val store = SlowStore().apply { fail = true }
        val c = SettingsController(store, backgroundScope)
        runCurrent()
        c.update { it.copy(lockScreenEnabled = true, indicatorLengthFraction = 0.42f, indicatorThicknessFraction = 0.37f) }
        c.flush()
        runCurrent()
        assertNotNull(c.error.value)
        assertTrue(c.state.value!!.lockScreenEnabled)
        assertEquals(0.42f, c.state.value!!.indicatorLengthFraction)
        assertEquals(0.37f, c.state.value!!.indicatorThicknessFraction)
        c.update { it.copy(depthDp = 29f) }
        c.update { it.copy(indicatorLengthFraction = 0.91f) }
        c.update { it.copy(indicatorThicknessFraction = 0.81f) }
        store.fail = false
        c.retry()
        runCurrent()
        assertNull(c.error.value)
        assertEquals(c.state.value, store.disk.value)
        assertTrue(store.disk.value.lockScreenEnabled)
        assertEquals(0.91f, store.disk.value.indicatorLengthFraction)
        assertEquals(0.81f, store.disk.value.indicatorThicknessFraction)
        assertEquals(29f, store.disk.value.depthDp)
        assertFalse(store.disk.value.indicatorVisible)
        assertFalse(store.disk.value.landscapeEnabled)
    }

    @Test fun `completing a slow save does not bypass a newer adjustment debounce`() = runTest {
        val store = SlowStore().apply { latency = 1_000 }
        val c = SettingsController(store, backgroundScope)
        runCurrent()
        c.update { it.copy(lengthDp = 140f) }
        c.flush()
        runCurrent()
        advanceTimeBy(900)
        c.update { it.copy(lengthDp = 170f) }
        runCurrent()
        advanceTimeBy(100)
        runCurrent()
        assertEquals(1, store.writes)
        assertEquals(170f, c.state.value!!.lengthDp)
        advanceTimeBy(50)
        runCurrent()
        assertEquals(2, store.writes)
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(170f, store.disk.value.lengthDp)
    }

    @Test fun `read failure can retry without concurrent reloads rewinding later edits`() = runTest {
        var reads = 0
        var disk = TriggerSettings()
        val store = object : SettingsRepository {
            override val settings = flow {
                reads++
                if (reads == 1) throw IOException("temporary read failure")
                delay(1_000)
                emit(disk)
            }
            override suspend fun update(transform: (TriggerSettings) -> TriggerSettings) { disk = transform(disk) }
            override suspend fun reset() = Unit
        }
        val c = SettingsController(store, backgroundScope)
        runCurrent()
        assertNull(c.state.value)
        assertNotNull(c.error.value)
        c.retry()
        c.retry()
        runCurrent()
        assertEquals(2, reads)
        advanceTimeBy(1_000)
        runCurrent()
        c.update { it.copy(lengthDp = 190f) }
        c.flush()
        runCurrent()
        assertEquals(190f, c.state.value!!.lengthDp)
        assertEquals(190f, disk.lengthDp)
        assertNull(c.error.value)
    }
}
