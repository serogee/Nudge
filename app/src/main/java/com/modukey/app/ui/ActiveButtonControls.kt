package com.modukey.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.modukey.app.R
import com.modukey.app.core.*
import com.modukey.app.platform.SoundState

@Composable fun ActiveButtonControls(panel: PanelConfig, change: (ActiveButtonAppearance) -> Unit) {
    val s = panel.activeAppearance
    val defaults = ActiveButtonAppearance()
    SettingTitle(t(R.string.active_button_colors))
    OptionInfo(R.string.active_button_colors_help)
    Toggle(R.string.use_active_background, null, s.useBackground, default = true, optionsExpanded = s.useBackground) { change(s.copy(useBackground = it)) }
    if (s.useBackground) DependentSettings(R.string.use_active_background) {
        Toggle(R.string.automatic_active_background, null, s.automaticBackground, default = true, optionsExpanded = !s.automaticBackground) { change(s.copy(automaticBackground = it)) }
        if (!s.automaticBackground) DependentSettings(R.string.automatic_active_background) {
            ColorPicker(s.background, title = R.string.active_background_color, default = defaults.background) { change(s.copy(background = it)) }
        }
    }
    Toggle(R.string.use_active_icon, null, s.useIcon, default = true, optionsExpanded = s.useIcon) { change(s.copy(useIcon = it)) }
    if (s.useIcon) DependentSettings(R.string.use_active_icon) {
        Toggle(R.string.automatic_active_icon, null, s.automaticIcon, default = true, optionsExpanded = !s.automaticIcon) { change(s.copy(automaticIcon = it)) }
        if (!s.automaticIcon) DependentSettings(R.string.automatic_active_icon) {
            ColorPicker(s.icon, title = R.string.active_icon_color, default = defaults.icon) { change(s.copy(icon = it)) }
        }
    }
    val button = remember { PanelButton(action = ButtonAction.MEDIA_MUTE) }
    Surface(color = Color(panel.background).copy(alpha = panel.opacity), contentColor = contrastOn(Color(panel.background)), shape = MaterialTheme.shapes.medium) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally)) {
            for (active in listOf(false, true)) Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PanelButtonFace(button, panel, 56.dp, SoundState(muted = active))
                Text(t(if (active) R.string.active_button else R.string.inactive_button), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
