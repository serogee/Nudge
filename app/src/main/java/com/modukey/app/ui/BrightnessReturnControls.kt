package com.modukey.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.modukey.app.R
import com.modukey.app.core.BrightnessReturnSettings

/** Shared by panel buttons, direct gestures, and the Home shortcut button editor. */
@Composable fun BrightnessReturnControls(settings: BrightnessReturnSettings, change: (BrightnessReturnSettings) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Help(R.string.brightness_help)
        Toggle(R.string.brightness_use_previous, R.string.brightness_previous_help, settings.usePrevious, default = true) {
            change(settings.copy(usePrevious = it))
        }
        PercentSlider(R.string.brightness_return_value, settings.fraction, default = .5f) { change(settings.copy(fraction = it)) }
        Help(R.string.brightness_return_help)
    }
}
