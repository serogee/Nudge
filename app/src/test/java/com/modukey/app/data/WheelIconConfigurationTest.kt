package com.modukey.app.data

import com.modukey.app.core.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WheelIconConfigurationTest {
    @Test fun iconChoicesRoundTripPerOrientationAndDoNotDisableAutofit() {
        val portrait = PanelLayout(mode = PanelMode.WHEEL, wheel = WheelLayoutSettings(adaptIconSize = false, iconSizeDp = 96f, iconPosition = .8f))
        val landscape = portrait.copy(wheel = portrait.wheel.copy(adaptIconSize = true, iconSizeDp = 64f, iconPosition = .2f))
        val config = AppConfiguration(panels = listOf(SavedPanel(settings = PanelConfig(portrait = portrait, landscape = landscape, landscapeOverride = true)))).normalized()
        assertEquals(config, ConfigurationCodec.decode(ConfigurationCodec.encode(config)))
        assertTrue(portrait.editSize { it.copy(wheel = landscape.wheel) }.wheel.autoFit)
    }
    @Test fun olderWheelsGetAdaptiveIconsWithoutChangingTheirStoredSizes() {
        val wheel = WheelLayoutSettings(innerRadiusDp = 1f, outerRadiusDp = 300f, adaptIconSize = false, iconSizeDp = 96f, iconPosition = .8f)
        val config = AppConfiguration(panels = listOf(SavedPanel(settings = PanelConfig(portrait = PanelLayout(mode = PanelMode.WHEEL, wheel = wheel)))))
        val old = JSONObject(ConfigurationCodec.encode(config)).put("schema", 27)
        val saved = old.getJSONArray("panels").getJSONObject(0).getJSONObject("portrait").getJSONObject("wheel")
        listOf("adaptIconSize", "iconSizeDp", "iconPosition").forEach(saved::remove)
        val restored = ConfigurationCodec.decode(ConfigurationCodec.upgrade(old.toString())).panels.single().settings.portrait.wheel
        assertEquals(wheel.copy(adaptIconSize = true, iconSizeDp = 32f, iconPosition = .5f), restored)
    }
    @Test fun invalidIconSettingsUseFiniteBoundedValues() {
        assertEquals(32f, WheelLayoutSettings(iconSizeDp = Float.NaN).normalized().iconSizeDp)
        assertEquals(.5f, WheelLayoutSettings(iconPosition = Float.POSITIVE_INFINITY).normalized().iconPosition)
        assertEquals(12f, WheelLayoutSettings(iconSizeDp = -100f).normalized().iconSizeDp)
        assertEquals(128f, WheelLayoutSettings(iconSizeDp = 1_000f).normalized().iconSizeDp)
        assertEquals(0f, WheelLayoutSettings(iconPosition = -1f).normalized().iconPosition)
        assertEquals(1f, WheelLayoutSettings(iconPosition = 2f).normalized().iconPosition)
    }
}
