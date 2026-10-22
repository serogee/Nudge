package com.modukey.app.data

import com.modukey.app.core.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ActiveButtonConfigurationTest {
    @Test fun activeColorsSurviveBackupCopyAndOlderConfigurationsReceiveDefaults() {
        val appearance = ActiveButtonAppearance(useBackground = false, automaticIcon = false, icon = 0xFF22AA55)
        val config = AppConfiguration(panels = listOf(SavedPanel(settings = PanelConfig(activeAppearance = appearance)))).normalized()
        assertEquals(config, ConfigurationCodec.decode(ConfigurationCodec.encode(config)))
        assertEquals(appearance, AppConfiguration().addCopies(config).panels.single().settings.activeAppearance)
        val old = JSONObject(ConfigurationCodec.encode(config)).put("schema", 16)
        old.getJSONArray("panels").getJSONObject(0).remove("activeAppearance")
        assertEquals(ActiveButtonAppearance(), ConfigurationCodec.decode(ConfigurationCodec.upgrade(old.toString())).panels.single().settings.activeAppearance)
    }
}
