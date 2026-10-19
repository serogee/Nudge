package com.modukey.app.data

import com.modukey.app.core.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThicknessConfigurationTest {
    @Test fun `old schemas convert each percentage once and preserve unrelated settings`() {
        val panel = SavedPanel(name = "Tools", settings = PanelConfig(buttons = listOf(PanelButton(action = ButtonAction.LOCK))))
        val handle = HandleConfig(name = "Side", enabled = true, trigger = TriggerSettings(depthDp = 24f,
            indicatorThicknessDp = 6f, indicatorLengthFraction = .73f, cornerRadiusDp = 11f, excludedPackages = setOf("game")),
            bindings = mapOf(GestureSlot.TAP to GestureTarget.panel(panel.id)))
        val expected = AppConfiguration(enabled = true, includePrereleases = true, handles = listOf(handle), panels = listOf(panel)).normalized()
        for (schema in 2..4) {
            val old = JSONObject(ConfigurationCodec.encode(expected)).put("schema", schema)
            old.getJSONArray("handles").getJSONObject(0).getJSONObject("trigger").apply {
                remove("indicatorThicknessDp"); put("indicatorThickness", .25)
            }
            val upgraded = ConfigurationCodec.upgrade(old.toString())
            assertEquals(expected, ConfigurationCodec.decode(upgraded))
            val json = JSONObject(upgraded)
            assertEquals(5, json.getInt("schema"))
            val trigger = json.getJSONArray("handles").getJSONObject(0).getJSONObject("trigger")
            assertEquals(6.0, trigger.getDouble("indicatorThicknessDp"), 0.0)
            assertFalse(trigger.has("indicatorThickness"))
            assertEquals(upgraded, ConfigurationCodec.upgrade(upgraded))
            val resized = ConfigurationCodec.decode(upgraded).editHandle(handle.id) { it.copy(trigger = it.trigger.copy(depthDp = 8f)) }
            assertEquals(6f, ConfigurationCodec.decode(ConfigurationCodec.encode(resized)).handles.single().trigger.indicatorThicknessDp)
        }
        val source = checkNotNull(javaClass.classLoader!!.getResourceAsStream("configuration-v1.json")).bufferedReader().use { it.readText() }
        assertEquals(8.8f, ConfigurationCodec.decode(ConfigurationCodec.upgrade(source)).handles.single().trigger.indicatorThicknessDp, .0001f)
    }

    @Test fun `old percentage bounds and defaults retain the previous rendered size`() {
        for ((fraction, depth, expected) in listOf(Triple(.25f, 24f, 6f), Triple(0f, 8f, 0f),
            Triple(2f, 24f, 24f), Triple(-1f, 24f, 0f))) {
            val old = JSONObject(ConfigurationCodec.encode(AppConfiguration())).put("schema", 4)
            old.getJSONArray("handles").getJSONObject(0).getJSONObject("trigger").apply {
                remove("indicatorThicknessDp"); put("indicatorThickness", fraction); put("depth", depth)
            }
            assertEquals(expected, ConfigurationCodec.decode(old.toString()).handles.single().trigger.indicatorThicknessDp)
        }
        val modern = JSONObject(ConfigurationCodec.encode(AppConfiguration()))
        modern.getJSONArray("handles").getJSONObject(0).getJSONObject("trigger").apply {
            remove("indicatorThicknessDp"); put("indicatorThickness", 1.0)
        }
        assertEquals(4f, ConfigurationCodec.decode(modern.toString()).handles.single().trigger.indicatorThicknessDp)
    }

    @Test fun `dp settings survive backup copies and stay independent of touch depth`() {
        val h = HandleConfig(trigger = TriggerSettings(indicatorThicknessDp = 18.5f, depthDp = 8f, indicatorLengthFraction = .42f))
        val original = AppConfiguration(handles = listOf(h)).normalized()
        val backup = ConfigurationCodec.encode(original)
        assertEquals(original, ConfigurationCodec.decode(backup))
        val imported = AppConfiguration(handles = emptyList()).addCopies(ConfigurationCodec.decode(backup))
        assertNotEquals(h.id, imported.handles.single().id)
        assertEquals(h.trigger, imported.handles.single().trigger)
        val resized = original.editHandle(h.id) { it.copy(trigger = it.trigger.copy(depthDp = 32f)) }
        assertEquals(18.5f, ConfigurationCodec.decode(ConfigurationCodec.encode(resized)).handles.single().trigger.indicatorThicknessDp)
    }
}
