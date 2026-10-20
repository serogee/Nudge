package com.modukey.app.data

import com.modukey.app.core.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BrightnessAnimationConfigurationTest {
    @Test fun openingAndClosingAnimationsRoundTripIndependently() {
        for (open in PanelAnimation.entries) for (close in PanelAnimation.entries) {
            val config = AppConfiguration(brightnessPopup = BrightnessPopupSettings(animation = open, closeAnimation = close))
            assertEquals(config.normalized(), ConfigurationCodec.decode(ConfigurationCodec.encode(config)))
        }
    }
    @Test fun earlierBackupsKeepBrightnessAppearanceAndStartWithoutAnimations() {
        val config = AppConfiguration(brightnessPopup = BrightnessPopupSettings(widthDp = 88f, y = .4f,
            touchBehavior = BrightnessTouchBehavior.SLIDE_TO_ADJUST))
        val old = JSONObject(ConfigurationCodec.encode(config)).put("schema", 12)
        old.getJSONObject("brightnessPopup").apply { remove("animation"); remove("closeAnimation") }
        val upgraded = ConfigurationCodec.upgrade(old.toString())
        assertEquals(ConfigurationCodec.SCHEMA, JSONObject(upgraded).getInt("schema"))
        assertEquals(config.normalized(), ConfigurationCodec.decode(upgraded))
    }
}
