package com.modukey.app.core

import org.junit.Assert.*
import org.junit.Test

class HandlePausesTest {
    private val config = AppConfiguration(enabled = true, handles = listOf(
        HandleConfig(id = "owner", enabled = true), HandleConfig(id = "other", enabled = true), HandleConfig(id = "disabled")))
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
    @Test fun `saved disabling deletion master off and explicit resume clear obsolete pauses`() {
        val pauses = HandlePauses()
        pauses.toggle(HandleToggleScope.ALL, null, config)
        pauses.reconcile(config.editHandle("owner") { it.copy(enabled = false) })
        assertEquals(setOf("other"), pauses.state.value)
        pauses.reconcile(config.copy(handles = config.handles.filterNot { it.id == "other" }))
        assertTrue(pauses.state.value.isEmpty())
        pauses.toggle(HandleToggleScope.ALL, null, config); pauses.reconcile(config.copy(enabled = false))
        assertTrue(pauses.state.value.isEmpty())
        pauses.toggle(HandleToggleScope.ALL, null, config); pauses.clear()
        assertTrue(pauses.state.value.isEmpty())
    }
}
