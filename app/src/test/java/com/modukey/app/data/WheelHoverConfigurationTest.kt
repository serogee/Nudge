package com.modukey.app.data

import com.modukey.app.core.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WheelHoverConfigurationTest {
    private val hover = WheelHoverAppearance(enabled = false, useIcon = true, icon = 0xFF123456,
        useBackground = true, background = 0xFF234567, useHighlight = true, highlight = 0xFF345678)
    @Test fun colorsAndDisabledChoicesSurviveBackupsAndCopies() {
        val config = AppConfiguration(panels = listOf(SavedPanel(settings = PanelConfig(
            portrait = PanelLayout(mode = PanelMode.WHEEL), wheelHover = hover)))).normalized()
        assertEquals(config, ConfigurationCodec.decode(ConfigurationCodec.encode(config)))
        assertEquals(hover, config.panels.single().duplicate().settings.wheelHover)
        assertEquals(hover, AppConfiguration().addCopies(config).panels.single().settings.wheelHover)
    }
    @Test fun oldBackupsGetSubtleDefaultsWithoutChangingPanelColorsOrActiveStates() {
        val panel = PanelConfig(portrait = PanelLayout(mode = PanelMode.WHEEL), background = 0xFF123456,
            activeAppearance = ActiveButtonAppearance(automaticIcon = false, icon = 0xFF445566))
        val config = AppConfiguration(panels = listOf(SavedPanel(settings = panel))).normalized()
        val old = JSONObject(ConfigurationCodec.encode(config)).put("schema", 24)
        old.getJSONArray("panels").getJSONObject(0).remove("wheelHover")
        assertEquals(config, ConfigurationCodec.decode(ConfigurationCodec.upgrade(old.toString())))
        assertEquals(WheelHoverAppearance(), config.panels.single().settings.wheelHover)
    }
    @Test fun customColorsAreNormalizedToOpaqueRgb() {
        val normalized = hover.copy(icon = 0x123456, background = 0x234567, highlight = 0x345678).normalized()
        assertEquals(hover, normalized)
    }
}
