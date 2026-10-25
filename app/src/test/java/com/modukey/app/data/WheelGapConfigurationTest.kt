package com.modukey.app.data

import com.modukey.app.core.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WheelGapConfigurationTest {
    @Test fun gapsRoundTripIndependentlyAndOlderBackupsReceiveFourDp() {
        val portrait = PanelLayout(mode = PanelMode.WHEEL, wheel = WheelLayoutSettings(gapDp = 0f))
        val landscape = portrait.copy(wheel = portrait.wheel.copy(gapDp = 24f))
        val config = AppConfiguration(panels = listOf(SavedPanel(settings = PanelConfig(
            portrait = portrait, landscape = landscape, landscapeOverride = true)))).normalized()
        assertEquals(config, ConfigurationCodec.decode(ConfigurationCodec.encode(config)))
        val old = JSONObject(ConfigurationCodec.encode(config)).put("schema", 30)
        val panel = old.getJSONArray("panels").getJSONObject(0)
        for (orientation in listOf("portrait", "landscape")) panel.getJSONObject(orientation).getJSONObject("wheel").remove("gapDp")
        val restored = ConfigurationCodec.decode(ConfigurationCodec.upgrade(old.toString())).panels.single().settings
        assertEquals(portrait.copy(wheel = portrait.wheel.copy(gapDp = 4f)), restored.portrait)
        assertEquals(landscape.copy(wheel = landscape.wheel.copy(gapDp = 4f)), restored.landscape)
    }
}
