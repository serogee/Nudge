package com.modukey.app.core

import org.junit.Assert.*
import org.junit.Test

class GesturePopupOptionTest {
    @Test fun `legacy flags affect volume steps but do not enable popups on unrelated actions`() {
        assertTrue(GestureTarget.direct(PanelButton(action = ButtonAction.VOLUME_UP, showVolumePopup = true)).volume)
        assertTrue(GestureTarget.direct(PanelButton(action = ButtonAction.VOLUME_DOWN, showVolumePopup = true)).volume)
        for (action in ButtonAction.entries.filter { it !in listOf(ButtonAction.VOLUME_UP, ButtonAction.VOLUME_DOWN) }) {
            val old = GestureTarget.direct(PanelButton(action = action, showVolumePopup = true))
            assertFalse(old.volume); assertFalse(old.action!!.showVolumePopup)
            assertTrue(old.copy(volume = true).normalized().volume)
        }
    }
}
