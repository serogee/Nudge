package com.modukey.app.data

import com.modukey.app.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ConfigurationControllerTest {
    private class Store : ConfigurationRepository {
        val disk = MutableStateFlow(AppConfiguration())
        override val configuration = disk
        var latency = 0L; var fail = false; var writes = 0
        override suspend fun write(value: AppConfiguration) { writes++; delay(latency); if (fail) throw IOException(); disk.value = value }
    }
    @Test fun `slow storage cannot rewind alternating field edits or resurrect deletions`() = runTest {
        val store = Store().apply { latency = 1000 }; val c = ConfigurationController(store, backgroundScope); runCurrent()
        val id = c.state.value!!.handles.first().id
        c.update { it.editHandle(id) { h -> h.copy(name = "A") } }; c.flush(); runCurrent()
        c.update { it.editHandle(id) { h -> h.copy(trigger = h.trigger.copy(lengthDp = 180f)) } }
        c.update { it.copy(handles = emptyList()) }; c.flush(); runCurrent()
        advanceTimeBy(1000); runCurrent(); assertTrue(c.state.value!!.handles.isEmpty())
        advanceTimeBy(1000); runCurrent(); assertTrue(store.disk.value.handles.isEmpty())
    }
    @Test fun `save failure retains all edits until retry succeeds`() = runTest {
        val store = Store().apply { fail = true }; val c = ConfigurationController(store, backgroundScope); runCurrent()
        c.update { it.copy(enabled = true) }; c.flush(); runCurrent(); assertEquals(ConfigurationError.SAVE, c.error.value)
        c.update { it.copy(includePrereleases = true) }; store.fail = false; c.retry(); runCurrent()
        assertTrue(store.disk.value.enabled); assertTrue(store.disk.value.includePrereleases); assertNull(c.error.value)
    }
    @Test fun `continuous edits debounce and completion flushes final value`() = runTest {
        val store = Store(); val c = ConfigurationController(store, backgroundScope); runCurrent()
        repeat(10) { index -> c.update { v -> v.editHandle("first-handle") { h -> h.copy(name = "Handle $index") } }; advanceTimeBy(100) }
        assertEquals(0, store.writes); c.flush(); runCurrent(); assertEquals(1, store.writes)
        assertEquals(c.state.value, store.disk.value)
    }
    @Test fun `legacy modes map once without enabling other gestures`() {
        for (a in Activation.entries) {
            val old = TriggerSettings(enabled = true, activation = a, includePrereleases = true, indicatorVisible = true, excludedPackages = setOf("game"))
            val c = AppConfiguration.fromLegacy(old)
            assertTrue(c.enabled); assertTrue(c.includePrereleases); assertEquals(1, c.handles.first().bindings.values.count { it != GestureTarget.NONE })
            assertTrue(c.handles.first().enabled); assertTrue(c.handles.first().trigger.indicatorVisible)
            assertEquals(setOf("game"), c.handles.first().trigger.excludedPackages)
        }
    }
}
