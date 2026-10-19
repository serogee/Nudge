package com.modukey.app.data

import com.modukey.app.core.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HandleVisibilityConfigurationTest {
    @Test fun visibilityOverrideAndActionsRoundTripWithoutChangingPerHandleSettings() {
        val config = AppConfiguration(hideHandleIndicators = true, handles = listOf(
            HandleConfig(trigger = TriggerSettings(indicatorVisible = true)), HandleConfig(trigger = TriggerSettings(indicatorVisible = false))),
            panels = listOf(SavedPanel(settings = PanelConfig(buttons = listOf(PanelButton(action = ButtonAction.TOGGLE_HANDLE_VISIBILITY)))))).normalized()
        assertEquals(config, ConfigurationCodec.decode(ConfigurationCodec.encode(config)))
        assertEquals(listOf(true, false), config.handles.map { it.trigger.indicatorVisible })
        assertFalse(AppConfiguration().addCopies(config).hideHandleIndicators)
        assertTrue(config.addCopies(AppConfiguration()).hideHandleIndicators)
    }
    @Test fun earlierConfigurationKeepsVisibilityAndTheNewOverrideStartsOff() {
        val config = AppConfiguration(handles = listOf(HandleConfig(trigger = TriggerSettings(indicatorVisible = true)))).normalized()
        val old = JSONObject(ConfigurationCodec.encode(config)).put("schema", 11).apply { remove("hideHandleIndicators") }
        val upgraded = ConfigurationCodec.upgrade(old.toString())
        assertEquals(config, ConfigurationCodec.decode(upgraded))
        assertFalse(JSONObject(upgraded).getBoolean("hideHandleIndicators"))
    }
    @Test fun toggleHandlesFollowsClosePanelsAndVisibilityFollowsToggleHandles() {
        val close = ButtonAction.entries.indexOf(ButtonAction.CLOSE_PANELS)
        assertEquals(ButtonAction.TOGGLE_HANDLES, ButtonAction.entries[close + 1])
        assertEquals(ButtonAction.TOGGLE_HANDLE_VISIBILITY, ButtonAction.entries[close + 2])
    }
}
