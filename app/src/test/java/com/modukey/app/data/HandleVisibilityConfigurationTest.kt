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
        val config = AppConfiguration(handles = listOf(
            HandleConfig(trigger = TriggerSettings(indicatorVisible = true)), HandleConfig(trigger = TriggerSettings(indicatorVisible = false))),
            panels = listOf(SavedPanel(settings = PanelConfig(buttons = listOf(PanelButton(action = ButtonAction.TOGGLE_HANDLE_VISIBILITY)))))).normalized().hideIndicators(true)
        assertEquals(config, ConfigurationCodec.decode(ConfigurationCodec.encode(config)))
        assertEquals(listOf(true, false), config.handles.map { it.trigger.indicatorVisible })
        assertFalse(AppConfiguration().addCopies(config).hideHandleIndicators)
        assertFalse(config.addCopies(AppConfiguration()).hideHandleIndicators)
        assertTrue(config.handles.all { it.indicatorHidden })
    }
    @Test fun earlierConfigurationKeepsVisibilityAndTheNewOverrideStartsOff() {
        val config = AppConfiguration(handles = listOf(HandleConfig(trigger = TriggerSettings(indicatorVisible = true)))).normalized()
        val old = JSONObject(ConfigurationCodec.encode(config)).put("schema", 11).apply { remove("hideHandleIndicators") }
        val upgraded = ConfigurationCodec.upgrade(old.toString())
        assertEquals(config, ConfigurationCodec.decode(upgraded))
        assertFalse(JSONObject(upgraded).getJSONArray("handles").getJSONObject(0).getBoolean("indicatorHidden"))
    }
    @Test fun oldGlobalHidingMigratesToEachHandleAndCombinedScopesRoundTrip() {
        val button = PanelButton(action = ButtonAction.TOGGLE_AND_HIDE_HANDLES, toggleScope = HandleGroupScope.CURRENT, visibilityScope = HandleGroupScope.OTHERS)
        val config = AppConfiguration(panels = listOf(SavedPanel(settings = PanelConfig(buttons = listOf(button))))).normalized()
        assertEquals(config, ConfigurationCodec.decode(ConfigurationCodec.encode(config)))
        val old = JSONObject(ConfigurationCodec.encode(config)).put("schema", 15).put("hideHandleIndicators", true)
        val migrated = ConfigurationCodec.decode(ConfigurationCodec.upgrade(old.toString()))
        assertTrue(migrated.handles.all { it.indicatorHidden })
        assertEquals(button, migrated.panels.single().settings.buttons.single())
    }
    @Test fun toggleHandlesFollowsClosePanelsAndVisibilityFollowsToggleHandles() {
        val close = ButtonAction.entries.indexOf(ButtonAction.CLOSE_PANELS)
        assertEquals(ButtonAction.TOGGLE_HANDLES, ButtonAction.entries[close + 1])
        assertEquals(ButtonAction.TOGGLE_HANDLE_VISIBILITY, ButtonAction.entries[close + 2])
    }
}
