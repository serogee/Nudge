package com.modukey.app.ui

import androidx.compose.runtime.Composable
import com.modukey.app.R
import com.modukey.app.core.WheelLayoutSettings
import kotlin.math.roundToInt

@Composable internal fun WheelIconControls(settings: WheelLayoutSettings, change: (WheelLayoutSettings) -> Unit) {
    Toggle(R.string.wheel_adapt_icon_size, R.string.wheel_adapt_icon_size_help, settings.adaptIconSize,
        default = true, optionsExpanded = !settings.adaptIconSize) { change(settings.copy(adaptIconSize = it)) }
    if (!settings.adaptIconSize) DependentSettings(R.string.wheel_adapt_icon_size) {
        SettingSlider(R.string.wheel_icon_size, settings.iconSizeDp, 12f..128f,
            t(R.string.dp_value, settings.iconSizeDp.roundToInt()), default = 32f) { change(settings.copy(iconSizeDp = it)) }
        Help(R.string.wheel_icon_size_help)
    }
    PercentSlider(R.string.wheel_icon_position, settings.iconPosition, default = WheelLayoutSettings.DEFAULT_ICON_POSITION) { change(settings.copy(iconPosition = it)) }
    Help(R.string.wheel_icon_position_help)
}
