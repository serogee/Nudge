package com.modukey.app.data

import com.modukey.app.core.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [31])
class ConfigurationControlsTest {
    @Test fun `schema three preserves physical Y placement and upgrades only once`() {
        val p = SavedPanel(settings = PanelConfig(portrait = PanelLayout(offsetYDp = 24f), landscape = PanelLayout(offsetYDp = -31f)))
        val old = JSONObject(ConfigurationCodec.encode(AppConfiguration(panels = listOf(p)))).put("schema", 3).toString()
        val upgraded = ConfigurationCodec.decode(ConfigurationCodec.upgrade(old))
        assertEquals(-24f, upgraded.panels.single().settings.portrait.offsetYDp)
        assertEquals(31f, upgraded.panels.single().settings.landscape.offsetYDp)
        assertEquals(upgraded, ConfigurationCodec.decode(ConfigurationCodec.upgrade(ConfigurationCodec.encode(upgraded))))
    }

    @Test fun `new controls and direct popup choices survive export and independent changes`() {
        val button = PanelButton(action = ButtonAction.LOCK, emptyAppearance = EmptyAppearance.NO_BUTTON)
        val h = HandleConfig(trigger = TriggerSettings(lengthDp = 900f, longerTouchLength = true, softHideEnabled = true, softHideDelayMs = 7_000),
            bindings = mapOf(GestureSlot.HOLD to GestureTarget(action = button, volume = true)))
        val p = SavedPanel(settings = PanelConfig(buttons = listOf(button.copy(id = newId())), opacity = 0f, buttonOpacity = .2f, iconOpacity = .7f))
        val original = AppConfiguration(handles = listOf(h), panels = listOf(p)).normalized()
        assertEquals(original, ConfigurationCodec.decode(ConfigurationCodec.encode(original)))
        val edited = original.editHandle(h.id) { it.copy(trigger = it.trigger.copy(longerTouchLength = false)) }
        assertEquals(900f, ConfigurationCodec.decode(ConfigurationCodec.encode(edited)).handles.single().trigger.lengthDp)
        assertEquals(p, edited.panels.single())
    }

    @Test fun `legacy direct volume flags become a single gesture popup option`() {
        val h = HandleConfig(bindings = mapOf(GestureSlot.TAP to GestureTarget(action = PanelButton(showVolumePopup = true))))
        val old = JSONObject(ConfigurationCodec.encode(AppConfiguration(handles = listOf(h)))).put("schema", 3).toString()
        val target = ConfigurationCodec.decode(old).handles.single().target(GestureSlot.TAP)
        assertTrue(target.volume)
        assertFalse(target.action!!.showVolumePopup)
    }
}
