package com.modukey.app.core

import org.junit.Assert.*
import org.junit.Test

class GesturePopupOptionTest {
    @Test fun noActionIsDisabledOnlyWhenBothPopupOptionsAreOff() {
        for (action in listOf(null, PanelButton(action = ButtonAction.EMPTY))) {
            for (volume in listOf(false, true)) for (brightness in listOf(false, true)) {
                val target = GestureTarget(action = action, volume = volume, brightness = brightness).normalized()
                assertEquals(!volume && !brightness, target.isNone)
            }
        }
        assertFalse(GestureTarget.panel("tools").isNone)
        assertFalse(GestureTarget.direct(PanelButton(action = ButtonAction.VOLUME_UP)).isNone)
    }
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
