package com.modukey.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.modukey.app.core.WheelHoverAppearance

data class WheelHoverPalette(val background: Color, val icon: Color, val highlight: Color, val imageTint: Color?)

/** Reuse focus progress so color and geometry transitions retarget together. */
internal fun wheelHoverPalette(base: ButtonPalette, panelBackground: Color, container: Boolean,
    themeHighlight: Color, appearance: WheelHoverAppearance, focus: Float): WheelHoverPalette {
    val weight = if (focus.isFinite()) focus.coerceIn(0f, 1f) else 0f
    val amount = if (appearance.enabled) weight else 0f
    val background = if (container) base.background else panelBackground
    val target = if (appearance.useBackground) Color(appearance.background) else lerp(background, contrastOn(background), .08f)
    val highlight = if (appearance.enabled && appearance.useHighlight) Color(appearance.highlight) else themeHighlight
    return WheelHoverPalette(
        lerp(background, target.copy(alpha = background.alpha), amount),
        if (appearance.useIcon) lerp(base.icon, Color(appearance.icon), amount) else base.icon,
        highlight.copy(alpha = highlight.alpha * weight),
        if (appearance.enabled && appearance.useIcon && weight > 0f) Color(appearance.icon).copy(alpha = weight) else null)
}
