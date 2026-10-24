package com.modukey.app.data

import com.modukey.app.core.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BrightnessTouchConfigurationTest {
    @Test fun touchBehaviorRoundTripsAndEarlierDocumentsUseSwipeAdjustment() {
        val config = AppConfiguration(brightnessPopup = BrightnessPopupSettings(touchBehavior = BrightnessTouchBehavior.SLIDE_TO_ADJUST))
        assertEquals(config.normalized(), ConfigurationCodec.decode(ConfigurationCodec.encode(config)))
        val old = JSONObject(ConfigurationCodec.encode(config)).put("schema", 10)
        old.getJSONObject("brightnessPopup").remove("touchBehavior")
        val migrated = ConfigurationCodec.decode(ConfigurationCodec.upgrade(old.toString()))
        assertEquals(BrightnessTouchBehavior.SLIDE_TO_ADJUST, migrated.brightnessPopup.touchBehavior)
        assertEquals(config.copy(brightnessPopup = config.brightnessPopup.copy(touchBehavior = BrightnessTouchBehavior.JUMP_TO_TOUCH)).normalized(), migrated)
    }
}
