package com.modukey.app.data

import com.modukey.app.core.*
import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConfigurationDraftTest {
    private class Store : ConfigurationRepository {
        val disk = MutableStateFlow(AppConfiguration(panels = listOf(SavedPanel(id = "tools"))).normalized())
        override val configuration = disk
        var writes = 0
        var fail = false
        var latency = 0L
        override suspend fun write(value: AppConfiguration) {
            writes++; delay(latency)
            if (fail) throw IOException()
            disk.value = value
        }
    }

    @Test fun `handle draft previews all edits without scheduling disk writes`() = runTest {
        val store = Store(); val c = ConfigurationController(store, backgroundScope); runCurrent()
        val original = c.state.value
        c.openHandle("first-handle")
        c.editHandle("first-handle") { it.copy(name = "Pending", enabled = false) }
        c.editHandle("first-handle") { it.copy(trigger = it.trigger.copy(depthDp = 28f)) }
        c.flush(); advanceTimeBy(10_000); runCurrent()
        assertEquals(original, c.state.value); assertEquals(original, store.disk.value); assertEquals(0, store.writes)
        assertEquals("Pending", c.preview.value!!.handles.single().name)
        assertEquals(28f, c.preview.value!!.handles.single().trigger.depthDp)
        assertTrue(c.draft.value!!.changed)
        c.cancelDraft()
        assertEquals(original, c.preview.value); assertFalse(c.draft.value!!.changed)
    }

    @Test fun `save applies only the selected item against current configuration`() = runTest {
        val store = Store(); val c = ConfigurationController(store, backgroundScope); runCurrent()
        c.openHandle("first-handle")
        c.editHandle("first-handle") { it.copy(name = "Saved") }
        c.update { it.copy(enabled = true, panels = it.panels + SavedPanel(id = "other", name = "Other")) }
        c.saveDraft(); runCurrent()
        assertEquals("Saved", store.disk.value.handles.single().name)
        assertTrue(store.disk.value.enabled); assertEquals(2, store.disk.value.panels.size)
        assertEquals(c.state.value, c.preview.value); assertFalse(c.draft.value!!.changed)
        c.editHandle("first-handle") { it.copy(name = "Discarded") }
        c.cancelDraft(); assertEquals("Saved", c.preview.value!!.handles.single().name)
    }

    @Test fun `panel button layout name and appearance edits cancel together`() = runTest {
        val store = Store(); val c = ConfigurationController(store, backgroundScope); runCurrent()
        val original = c.state.value
        c.openPanel("tools")
        c.editPanel("tools") { it.copy(name = "Draft", settings = it.settings.copy(buttons = listOf(PanelButton(action = ButtonAction.LOCK)),
            opacity = .5f, portrait = it.settings.portrait.copy(offsetYDp = 60f))) }
        assertEquals(1, c.preview.value!!.panel("tools")!!.settings.buttons.size)
        c.closeDraft(); c.flush(); runCurrent()
        assertEquals(original, c.preview.value); assertEquals(original, store.disk.value)
        assertNull(c.draft.value); assertEquals(0, store.writes)
    }

    @Test fun `restoring original values clears pending state and reopening retains draft`() = runTest {
        val store = Store(); val c = ConfigurationController(store, backgroundScope); runCurrent()
        c.openPanel("tools")
        c.editPanel("tools") { it.copy(name = "Draft") }
        c.openPanel("tools")
        assertEquals("Draft", c.preview.value!!.panel("tools")!!.name)
        c.editPanel("tools") { it.copy(name = "") }
        assertFalse(c.draft.value!!.changed)
        c.update { it.editPanel("tools") { p -> p.copy(name = "External") } }
        assertEquals("External", c.preview.value!!.panel("tools")!!.name)
    }

    @Test fun `slow previous save and save failure cannot write an unapproved draft`() = runTest {
        val store = Store().apply { latency = 1_000 }; val c = ConfigurationController(store, backgroundScope); runCurrent()
        c.update { it.copy(enabled = true) }; c.flush(); runCurrent()
        c.openPanel("tools"); c.editPanel("tools") { it.copy(name = "Pending") }
        c.flush(); advanceTimeBy(2_000); runCurrent()
        assertEquals("", store.disk.value.panel("tools")!!.name)
        store.fail = true; c.saveDraft(); advanceTimeBy(1_000); runCurrent()
        assertEquals(ConfigurationError.SAVE, c.error.value)
        assertEquals("Pending", c.state.value!!.panel("tools")!!.name)
        c.editPanel("tools") { it.copy(name = "Still editing") }
        store.fail = false; c.retry(); advanceTimeBy(1_000); runCurrent()
        assertEquals("Pending", store.disk.value.panel("tools")!!.name)
        assertEquals("Still editing", c.preview.value!!.panel("tools")!!.name)
        assertTrue(c.draft.value!!.changed); assertNull(c.error.value)
    }

    @Test fun `removal ends a draft without restoring its item on save`() = runTest {
        val store = Store(); val c = ConfigurationController(store, backgroundScope); runCurrent()
        c.openPanel("tools"); c.editPanel("tools") { it.copy(name = "Pending") }
        c.update { it.removePanel("tools") }; c.saveDraft(); c.flush(); runCurrent()
        assertNull(c.draft.value); assertNull(c.preview.value!!.panel("tools")); assertNull(store.disk.value.panel("tools"))
    }

    @Test fun `another editor cannot replace an unresolved draft`() = runTest {
        val store = Store(); val c = ConfigurationController(store, backgroundScope); runCurrent()
        c.openHandle("first-handle"); c.editHandle("first-handle") { it.copy(name = "Pending") }
        assertThrows(IllegalStateException::class.java) { c.openPanel("tools") }
        assertEquals("Pending", c.preview.value!!.handles.single().name)
        c.cancelDraft(); c.openPanel("tools")
        assertTrue(c.draft.value is ConfigurationDraft.Panel)
    }

    @Test fun `save feedback waits for storage and reports failure without discarding pending settings`() = runTest {
        val store = Store().apply { latency = 1_000 }; val c = ConfigurationController(store, backgroundScope); runCurrent()
        c.openHandle("first-handle"); c.editHandle("first-handle") { it.copy(name = "Saved") }
        var result: Boolean? = null
        c.saveDraft { result = it }; runCurrent()
        assertNull(result); advanceTimeBy(1_000); runCurrent()
        assertEquals(true, result); assertEquals("Saved", store.disk.value.handles.single().name)
        store.fail = true; result = null
        c.editHandle("first-handle") { it.copy(name = "Pending") }; c.saveDraft { result = it }
        runCurrent(); assertNull(result); advanceTimeBy(1_000); runCurrent()
        assertEquals(false, result); assertEquals(ConfigurationError.SAVE, c.error.value)
        assertEquals("Pending", c.state.value!!.handles.single().name)
        assertEquals("Saved", store.disk.value.handles.single().name)
        store.fail = false; c.retry(); advanceTimeBy(1_000); runCurrent()
        assertEquals("Pending", store.disk.value.handles.single().name)
    }
}
