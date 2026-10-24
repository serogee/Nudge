package com.modukey.app.ui

import androidx.compose.runtime.Composable
import com.modukey.app.R
import com.modukey.app.core.WheelHoverAppearance

@Composable internal fun WheelHoverControls(settings: WheelHoverAppearance, change: (WheelHoverAppearance) -> Unit) {
    val defaults = WheelHoverAppearance()
    SettingTitle(t(R.string.wheel_hover_colors))
    Toggle(R.string.wheel_hover_enabled, R.string.wheel_hover_help, settings.enabled, default = true,
        optionsExpanded = settings.enabled) { change(settings.copy(enabled = it)) }
    if (settings.enabled) DependentSettings(R.string.wheel_hover_enabled) {
        Toggle(R.string.wheel_hover_custom_icon, null, settings.useIcon, default = false,
            optionsExpanded = settings.useIcon) { change(settings.copy(useIcon = it)) }
        if (settings.useIcon) DependentSettings(R.string.wheel_hover_custom_icon) {
            ColorPicker(settings.icon, title = R.string.wheel_hover_icon_color, default = defaults.icon) { change(settings.copy(icon = it)) }
        }
        Toggle(R.string.wheel_hover_custom_background, null, settings.useBackground, default = false,
            optionsExpanded = settings.useBackground) { change(settings.copy(useBackground = it)) }
        if (settings.useBackground) DependentSettings(R.string.wheel_hover_custom_background) {
            ColorPicker(settings.background, title = R.string.wheel_hover_background_color, default = defaults.background) { change(settings.copy(background = it)) }
        }
        Toggle(R.string.wheel_hover_custom_highlight, null, settings.useHighlight, default = false,
            optionsExpanded = settings.useHighlight) { change(settings.copy(useHighlight = it)) }
        if (settings.useHighlight) DependentSettings(R.string.wheel_hover_custom_highlight) {
            ColorPicker(settings.highlight, title = R.string.wheel_hover_highlight_color, default = defaults.highlight) { change(settings.copy(highlight = it)) }
        }
    }
}
