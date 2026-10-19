package com.modukey.app.data

import com.modukey.app.core.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BrightnessReturnConfigurationTest {
    @Test fun returnPreferencesSurviveEveryActionLocationAndDuplication() {
        val button = PanelButton(action = ButtonAction.BRIGHTNESS_MAXIMUM, brightnessReturn = BrightnessReturnSettings(false, .2f))
        val config = AppConfiguration(
            handles = listOf(HandleConfig(bindings = mapOf(GestureSlot.HOLD to GestureTarget.direct(button.copy(id = newId()))))),
            panels = listOf(SavedPanel(settings = PanelConfig(buttons = listOf(button)))),
            shortcuts = listOf(HomeShortcut(button = button.copy(id = newId())))).normalized()
        assertEquals(config, ConfigurationCodec.decode(ConfigurationCodec.encode(config)))
        val copies = AppConfiguration().addCopies(config)
        assertEquals(button.brightnessReturn, copies.handles.last().target(GestureSlot.HOLD).action!!.brightnessReturn)
        assertEquals(button.brightnessReturn, copies.panels.last().settings.buttons.single().brightnessReturn)
        assertNotEquals(button.id, copies.panels.last().settings.buttons.single().id)
    }

    @Test fun schemaNineActionsReceiveDefaultsWithoutChangingOtherFields() {
        val button = PanelButton(action = ButtonAction.BRIGHTNESS_MINIMUM, iconContainer = IconContainer.STANDALONE)
        val config = AppConfiguration(panels = listOf(SavedPanel(settings = PanelConfig(buttons = listOf(button))))).normalized()
        val old = JSONObject(ConfigurationCodec.encode(config)).put("schema", 9)
        old.getJSONArray("panels").getJSONObject(0).getJSONArray("buttons").getJSONObject(0).remove("brightnessReturn")
        val upgraded = ConfigurationCodec.upgrade(old.toString())
        assertEquals(10, JSONObject(upgraded).getInt("schema"))
        assertEquals(config, ConfigurationCodec.decode(upgraded))
    }

    @Test fun normalizationLimitsEachReturnValueWithoutChangingOtherSettings() {
        for ((input, expected) in listOf(Float.NaN to .5f, -1f to 0f, 2f to 1f)) {
            val button = PanelButton(brightnessReturn = BrightnessReturnSettings(false, input), showVolumePopup = true)
            val config = AppConfiguration(panels = listOf(SavedPanel(settings = PanelConfig(buttons = listOf(button))))).normalized()
            val saved = config.panels.single().settings.buttons.single()
            assertEquals(expected, saved.brightnessReturn.fraction)
            assertFalse(saved.brightnessReturn.usePrevious); assertTrue(saved.showVolumePopup)
        }
    }
}
