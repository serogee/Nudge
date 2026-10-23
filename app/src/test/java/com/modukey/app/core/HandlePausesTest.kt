package com.modukey.app.core

import org.junit.Assert.*
import org.junit.Test

class HandlePausesTest {
    private val config = AppConfiguration(enabled = true, handles = listOf(
        HandleConfig(id = "owner", enabled = true), HandleConfig(id = "other", enabled = true), HandleConfig(id = "disabled")))
    @Test fun explicitPauseAndResumeAreIdempotentAndRetainSavedSwitches() {
        val pauses = HandlePauses()
        pauses.toggle(HandleToggleScope.CURRENT, "owner", config)
        pauses.setAll(true, config); pauses.setAll(true, config)
        assertEquals(setOf("owner", "other"), pauses.state.value)
        assertEquals(listOf(true, true, false), config.handles.map { it.enabled })
        pauses.setAll(false, config); pauses.setAll(false, config)
        assertTrue(pauses.state.value.isEmpty())
        pauses.setAll(true, config.copy(enabled = false))
        assertTrue(pauses.state.value.isEmpty())
    }
    @Test fun `all current and others pause independently without changing saved enable switches`() {
        for ((scope, targets) in listOf(HandleToggleScope.ALL to setOf("owner", "other"),
            HandleToggleScope.CURRENT to setOf("owner"), HandleToggleScope.OTHERS to setOf("other"))) {
            val pauses = HandlePauses()
            assertTrue(pauses.toggle(scope, "owner", config)); assertEquals(targets, pauses.state.value)
            assertEquals(listOf(true, true, false), config.handles.map { it.enabled })
            assertTrue(pauses.toggle(scope, "owner", config)); assertTrue(pauses.state.value.isEmpty())
        }
    }
    @Test fun `mixed target state resumes the group and never enables saved disabled handles`() {
        val pauses = HandlePauses()
        pauses.toggle(HandleToggleScope.CURRENT, "owner", config)
        pauses.toggle(HandleToggleScope.ALL, "owner", config)
        assertTrue(pauses.state.value.isEmpty())
        assertFalse(pauses.toggle(HandleToggleScope.CURRENT, "disabled", config))
        assertFalse(pauses.toggle(HandleToggleScope.OTHERS, null, config))
        assertFalse(pauses.toggle(HandleToggleScope.CURRENT, null, config))
        assertFalse(pauses.toggle(HandleToggleScope.ALL, "owner", config.copy(enabled = false)))
    }
    @Test fun disablingAndMasterChangesRetainPausesUntilDeletionOrExplicitResume() {
        val pauses = HandlePauses()
        pauses.toggle(HandleToggleScope.ALL, null, config)
        pauses.reconcile(config.editHandle("owner") { it.copy(enabled = false) })
        assertEquals(setOf("owner", "other"), pauses.state.value)
        pauses.reconcile(config.copy(handles = config.handles.filterNot { it.id == "other" }))
        assertEquals(setOf("owner"), pauses.state.value)
        pauses.reconcile(config.copy(enabled = false))
        assertEquals(setOf("owner"), pauses.state.value)
        pauses.clear()
        assertTrue(pauses.state.value.isEmpty())
    }
    @Test fun pausingEnabledHandlesRetainsDormantPausesAndResumeAllClearsThem() {
        val pauses = HandlePauses()
        pauses.restore(config.copy(pausedHandleIds = setOf("disabled")))
        pauses.setAll(true, config)
        assertEquals(setOf("owner", "other", "disabled"), pauses.state.value)
        pauses.setAll(false, config.copy(enabled = false))
        assertTrue(pauses.state.value.isEmpty())
    }
    @Test fun savedSelectionsRestoreWithoutEnablingHandlesAndDropDeletedIds() {
        val stored = config.copy(enabled = false, pausedHandleIds = setOf("owner", "disabled", "deleted")).normalized()
        assertEquals(setOf("owner", "disabled"), stored.pausedHandleIds)
        val pauses = HandlePauses(); pauses.restore(stored)
        assertEquals(stored.pausedHandleIds, pauses.state.value)
        assertEquals(config.handles.map { it.enabled }, stored.handles.map { it.enabled })
    }
}
