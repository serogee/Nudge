package com.modukey.app.data

import com.modukey.app.core.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SliderConfigurationTest {
    @Test fun sliderSettingsSurviveBackupAndCopyWhileOlderBackupsUseDefaults() {
        val target = GestureTarget(volume = true, slideToAdjust = true)
        val handle = HandleConfig(volumeTravelDp = 300f, brightnessTravelDp = 600f, bindings = mapOf(GestureSlot.HOLD to target))
        val config = AppConfiguration(handles = listOf(handle)).normalized()
        assertEquals(config, ConfigurationCodec.decode(ConfigurationCodec.encode(config)))
        val copy = AppConfiguration(handles = emptyList()).addCopies(config).handles.single()
        assertEquals(300f, copy.volumeTravelDp); assertEquals(target, copy.target(GestureSlot.HOLD))
        val old = JSONObject(ConfigurationCodec.encode(config)).put("schema", 17)
        val h = old.getJSONArray("handles").getJSONObject(0)
        h.remove("volumeTravelDp"); h.remove("brightnessTravelDp")
        h.getJSONObject("bindings").getJSONObject("HOLD").remove("slideToAdjust")
        val restored = ConfigurationCodec.decode(old.toString()).handles.single()
        assertEquals(240f, restored.volumeTravelDp); assertEquals(240f, restored.brightnessTravelDp)
        assertFalse(restored.target(GestureSlot.HOLD).slideToAdjust)
    }
    @Test fun tapAndAmbiguousPopupsCannotCaptureAndTravelHasFiniteBounds() {
        val h = HandleConfig(volumeTravelDp = Float.NaN, brightnessTravelDp = 1000f, bindings = mapOf(
            GestureSlot.TAP to GestureTarget(volume = true, slideToAdjust = true),
            GestureSlot.HOLD to GestureTarget(volume = true, brightness = true, slideToAdjust = true),
            GestureSlot.SWIPE_1 to GestureTarget(slideToAdjust = true))).normalized()
        assertTrue(h.bindings.values.none { it.slideToAdjust })
        assertEquals(240f, h.volumeTravelDp); assertEquals(600f, h.brightnessTravelDp)
        assertEquals(48f, h.copy(volumeTravelDp = 1f).normalized().volumeTravelDp)
    }
}
