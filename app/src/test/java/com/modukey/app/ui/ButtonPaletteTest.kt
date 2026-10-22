package com.modukey.app.ui

import androidx.compose.ui.graphics.Color
import com.modukey.app.core.*
import org.junit.Assert.*
import org.junit.Test

class ButtonPaletteTest {
    private val panel = PanelConfig(automaticButtonColor = false, buttonColor = 0xFF112233,
        automaticIconColor = false, iconColor = 0xFFCCDDEE,
        activeAppearance = ActiveButtonAppearance(automaticBackground = false, background = 0xFFBB3322, automaticIcon = false, icon = 0xFF44EE66))
    @Test fun customNormalColorsDoNotMaskCustomActiveColors() {
        val normal = buttonPalette(panel, false, true, Color.White)
        val active = buttonPalette(panel, true, true, Color.White)
        assertEquals(Color(0xFF112233), normal.background); assertEquals(Color(0xFFCCDDEE), normal.icon)
        assertEquals(Color(0xFFBB3322), active.background); assertEquals(Color(0xFF44EE66), active.icon)
        assertNull(active.outline)
    }
    @Test fun backgroundAndIconChangesCanBeEnabledIndependently() {
        val background = buttonPalette(panel.copy(activeAppearance = panel.activeAppearance.copy(useIcon = false)), true, true, Color.White)
        assertEquals(Color(0xFFBB3322), background.background); assertEquals(Color(0xFFCCDDEE), background.icon)
        val icon = buttonPalette(panel.copy(activeAppearance = panel.activeAppearance.copy(useBackground = false)), true, true, Color.White)
        assertEquals(Color(0xFF112233), icon.background); assertEquals(Color(0xFF44EE66), icon.icon)
        assertNull(icon.outline)
    }
    @Test fun standaloneSelectionUsesAnOutlineAndRetainsTheExistingOpacityControls() {
        val selected = buttonPalette(panel.copy(buttonOpacity = .4f), true, false, Color.White)
        assertEquals(Color(0xFFBB3322).copy(alpha = .4f), selected.outline)
        assertEquals(Color(0xFF44EE66), selected.icon)
        assertNull(buttonPalette(panel, false, false, Color.White).outline)
    }
}
