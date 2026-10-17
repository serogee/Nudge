package com.modukey.app.data

import com.modukey.app.core.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class ConfigurationCodecTest {
    @Test fun `backup roundtrip retains overflow buttons layout values icons and channels`() {
        val p = PanelConfig(buttons = List(12) { PanelButton(action = ButtonAction.OPEN_APP, appPackage = "app.$it", icon = IconReference("pack", "icon_$it")) },
            portrait = PanelLayout(crossCount = 2, maximumLines = 3), landscapeOverride = true,
            landscape = PanelLayout(mode = PanelMode.SCROLLABLE, direction = PanelDirection.HORIZONTAL, crossCount = 3), outsideTouch = OutsideTouch.PASS_THROUGH)
        val h = HandleConfig(name = "Side", enabled = true, panels = listOf(p, PanelConfig(), PanelConfig()))
        val c = AppConfiguration(enabled = true, includePrereleases = true, handles = listOf(h, h.duplicate())).normalized()
        assertEquals(c, ConfigurationCodec.decode(ConfigurationCodec.encode(c)))
        assertFalse(c.handles[1].enabled); assertNotEquals(h.id, c.handles[1].id)
        assertNotEquals(h.panels[0].buttons[0].id, c.handles[1].panels[0].buttons[0].id)
    }
    @Test fun `invalid future duplicate and oversized documents are rejected`() {
        for (s in listOf("{}", "{\"schema\":2}", "x".repeat(ConfigurationCodec.MAX_BYTES + 1),
            ConfigurationCodec.encode(AppConfiguration(handles = listOf(HandleConfig(id = "same"), HandleConfig(id = "same")))))) {
            try { ConfigurationCodec.decode(s); fail("accepted invalid document") } catch (_: IOException) { }
        }
    }
}
