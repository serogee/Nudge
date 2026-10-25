package com.modukey.app.data

import com.modukey.app.core.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WheelHitboxConfigurationTest {
    @Test fun allModesAndManualValuesPersistForEachOrientation() {
        for (mode in WheelHitboxMode.entries) {
            val portrait = PanelLayout(mode = PanelMode.WHEEL, wheel = WheelLayoutSettings(hitbox = WheelHitboxSettings(mode, 0f, 420f)))
            val landscape = portrait.copy(wheel = portrait.wheel.copy(hitbox = WheelHitboxSettings(WheelHitboxMode.MANUAL, 20f, 300f)))
            val panel = SavedPanel(settings = PanelConfig(portrait = portrait, landscape = landscape, landscapeOverride = true))
            val config = AppConfiguration(panels = listOf(panel), handles = listOf(HandleConfig(bindings = mapOf(GestureSlot.HOLD to
                GestureTarget.panel(panel.id).copy(wheelPick = WheelPickSettings(true, 48f, 280f)))))).normalized()
            assertEquals(config, ConfigurationCodec.decode(ConfigurationCodec.encode(config)))
        }
    }
    @Test fun olderBackupsKeepAutomaticPickingAndTheirExistingDistances() {
        val panel = SavedPanel(settings = PanelConfig(portrait = PanelLayout(mode = PanelMode.WHEEL)))
        val config = AppConfiguration(panels = listOf(panel), handles = listOf(HandleConfig(bindings = mapOf(GestureSlot.HOLD to
            GestureTarget.panel(panel.id).copy(wheelPick = WheelPickSettings(true, 48f, 280f)))))).normalized()
        val json = JSONObject(ConfigurationCodec.encode(config)).put("schema", 29)
        val stored = json.getJSONArray("panels").getJSONObject(0)
        for (orientation in listOf("portrait", "landscape")) stored.getJSONObject(orientation).getJSONObject("wheel").remove("hitbox")
        val restored = ConfigurationCodec.decode(ConfigurationCodec.upgrade(json.toString()))
        assertEquals(config, restored)
        assertEquals(WheelHitboxMode.AUTOMATIC, restored.panels.single().settings.portrait.wheel.hitbox.mode)
    }
}
