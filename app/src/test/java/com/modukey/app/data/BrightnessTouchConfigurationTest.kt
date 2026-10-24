package com.modukey.app.data

import com.modukey.app.core.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BrightnessTouchConfigurationTest {
    @Test fun bothTouchBehaviorsSurviveNormalizationAndBackupRoundTrips() {
        for (mode in BrightnessTouchBehavior.entries) {
            val config = AppConfiguration(brightnessPopup = BrightnessPopupSettings(touchBehavior = mode))
            assertEquals(mode, config.normalized().brightnessPopup.touchBehavior)
            assertEquals(config.normalized(), ConfigurationCodec.decode(ConfigurationCodec.encode(config)))
        }
    }
    @Test fun newSettingsAndDocumentsWithoutATouchChoiceDefaultToSlide() {
        assertEquals(BrightnessTouchBehavior.SLIDE_TO_ADJUST, BrightnessPopupSettings().touchBehavior)
        val config = AppConfiguration(brightnessPopup = BrightnessPopupSettings())
        val old = JSONObject(ConfigurationCodec.encode(config)).put("schema", 10)
        old.getJSONObject("brightnessPopup").remove("touchBehavior")
        val migrated = ConfigurationCodec.decode(ConfigurationCodec.upgrade(old.toString()))
        assertEquals(config.normalized(), migrated)
    }
    @Test fun olderExplicitJumpChoicesArePreserved() {
        val config = AppConfiguration(brightnessPopup = BrightnessPopupSettings(touchBehavior = BrightnessTouchBehavior.JUMP_TO_TOUCH))
        val old = JSONObject(ConfigurationCodec.encode(config)).put("schema", 22)
        assertEquals(config.normalized(), ConfigurationCodec.decode(ConfigurationCodec.upgrade(old.toString())))
    }
}
