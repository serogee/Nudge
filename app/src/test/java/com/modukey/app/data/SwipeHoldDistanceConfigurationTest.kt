package com.modukey.app.data

import com.modukey.app.core.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SwipeHoldDistanceConfigurationTest {
    @Test fun ordinaryDirectionIsIndependentAndOlderDocumentsKeepInwardSwipes() {
        val config = AppConfiguration(handles = listOf(HandleConfig(trigger = TriggerSettings(
            swipeDistanceMode = SwipeDistanceMode.ANY_DIRECTION,
            swipeHoldDistanceMode = SwipeHoldDistanceMode.INWARD_ONLY)))).normalized()
        assertEquals(config, ConfigurationCodec.decode(ConfigurationCodec.encode(config)))
        val old = JSONObject(ConfigurationCodec.encode(config)).put("schema", 18)
        old.getJSONArray("handles").getJSONObject(0).getJSONObject("trigger").remove("swipeDistanceMode")
        val loaded = ConfigurationCodec.decode(ConfigurationCodec.upgrade(old.toString()))
        assertEquals(SwipeDistanceMode.INWARD_ONLY, loaded.handles.single().trigger.swipeDistanceMode)
        assertEquals(SwipeHoldDistanceMode.INWARD_ONLY, loaded.handles.single().trigger.swipeHoldDistanceMode)
    }

    @Test fun savesDistanceModeAndDefaultsOlderDocumentsToAnyDirection() {
        val config = AppConfiguration(handles = listOf(HandleConfig(trigger = TriggerSettings(swipeHoldDistanceMode = SwipeHoldDistanceMode.INWARD_ONLY)))).normalized()
        assertEquals(config, ConfigurationCodec.decode(ConfigurationCodec.encode(config)))
        val old = JSONObject(ConfigurationCodec.encode(config)).put("schema", 14)
        old.getJSONArray("handles").getJSONObject(0).getJSONObject("trigger").remove("swipeHoldDistanceMode")
        val loaded = ConfigurationCodec.decode(ConfigurationCodec.upgrade(old.toString()))
        assertEquals(SwipeHoldDistanceMode.ANY_DIRECTION, loaded.handles.single().trigger.swipeHoldDistanceMode)
        assertEquals(config.handles.single().bindings, loaded.handles.single().bindings)
    }
}
