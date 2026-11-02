package com.modukey.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.modukey.app.core.PanelConfig
import com.modukey.app.core.ButtonBackground
import com.modukey.app.core.ButtonBackgroundMode

data class ButtonPalette(val background: Color, val icon: Color, val outline: Color?)

/** Active colors remain independent of the normal automatic/manual color controls. */
fun buttonPalette(panel: PanelConfig, selected: Boolean, container: Boolean, surface: Color,
    appearance: ButtonBackground = ButtonBackground()): ButtonPalette {
    val normal = when (appearance.mode) {
        ButtonBackgroundMode.TRANSPARENT -> Color.Transparent
        ButtonBackgroundMode.CUSTOM -> Color(appearance.color)
        ButtonBackgroundMode.AUTOMATIC -> if (panel.automaticButtonColor) {
            if (Color(panel.background).luminance() < .5f) Color(0xFF3C3C3E) else Color(0xFFE1E3E5)
        } else Color(panel.buttonColor)
    }
    val active = panel.activeAppearance
    val activeBackground = if (active.automaticBackground) Color(0xFF3482FF) else Color(active.background)
    val fill = if (selected && active.useBackground && container) activeBackground else normal
    val background = fill.copy(alpha = fill.alpha * panel.buttonOpacity)
    val panelBackground = Color(panel.background).copy(alpha = panel.opacity).compositeOver(surface)
    val normalBehind = normal.copy(alpha = normal.alpha * panel.buttonOpacity).compositeOver(panelBackground)
    val behind = background.compositeOver(panelBackground)
    val normalIcon = if (panel.automaticIconColor) contrastOn(normalBehind) else Color(panel.iconColor)
    val icon = if (selected && active.useIcon) {
        if (active.automaticIcon) {
            if (container && active.useBackground) contrastOn(behind) else Color(0xFF3482FF)
        } else Color(active.icon)
    } else normalIcon
    return ButtonPalette(background, icon, if (selected && !container && active.useBackground) activeBackground.copy(alpha = panel.buttonOpacity) else null)
}
