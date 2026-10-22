package com.modukey.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.modukey.app.core.PanelConfig

data class ButtonPalette(val background: Color, val icon: Color, val outline: Color?)

/** Active colors remain independent of the normal automatic/manual color controls. */
fun buttonPalette(panel: PanelConfig, selected: Boolean, container: Boolean, surface: Color): ButtonPalette {
    val normal = if (panel.automaticButtonColor) {
        if (Color(panel.background).luminance() < .5f) Color(0xFF3C3C3E) else Color(0xFFE1E3E5)
    } else Color(panel.buttonColor)
    val active = panel.activeAppearance
    val activeBackground = if (active.automaticBackground) Color(0xFF3482FF) else Color(active.background)
    val background = (if (selected && active.useBackground) activeBackground else normal).copy(alpha = panel.buttonOpacity)
    val panelBackground = Color(panel.background).copy(alpha = panel.opacity).compositeOver(surface)
    val normalBehind = if (container) normal.copy(alpha = panel.buttonOpacity).compositeOver(panelBackground) else panelBackground
    val behind = if (container) background.compositeOver(panelBackground) else panelBackground
    val normalIcon = if (panel.automaticIconColor) contrastOn(normalBehind) else Color(panel.iconColor)
    val icon = if (selected && active.useIcon) {
        if (active.automaticIcon) {
            if (container && active.useBackground) contrastOn(behind) else Color(0xFF3482FF)
        } else Color(active.icon)
    } else normalIcon
    return ButtonPalette(background, icon, if (selected && !container && active.useBackground) activeBackground.copy(alpha = panel.buttonOpacity) else null)
}
