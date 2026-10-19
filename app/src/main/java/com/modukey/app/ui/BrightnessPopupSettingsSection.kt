package com.modukey.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.modukey.app.R
import com.modukey.app.core.*
import kotlin.math.roundToInt

@Composable fun BrightnessPopupSettingsSection(config: AppConfiguration, vm: NudgeViewModel) {
    val service by vm.status.collectAsStateWithLifecycle()
    val s = config.brightnessPopup
    val defaults = remember { BrightnessPopupSettings() }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var preview by rememberSaveable { mutableStateOf(false) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, preview) {
        val token = Any()
        fun update() = vm.previewBrightness(token, preview && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        val observer = LifecycleEventObserver { _, _ -> update() }
        lifecycle.addObserver(observer); update()
        onDispose { vm.previewBrightness(token, false); lifecycle.removeObserver(observer) }
    }
    fun edit(change: (BrightnessPopupSettings) -> BrightnessPopupSettings) = vm.edit { it.copy(brightnessPopup = change(it.brightnessPopup)) }
    Section(R.string.brightness_slider) {
        Toggle(R.string.brightness_preview, R.string.brightness_preview_help, preview) { preview = it }
        if (preview && !service.connected) Help(R.string.preview_unavailable)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(t(R.string.appearance), Modifier.weight(1f))
            EditIcon(if (expanded) AppIcons.ChevronUp else AppIcons.Down, if (expanded) R.string.collapse_setup else R.string.expand_setup) { expanded = !expanded }
        }
        if (expanded) {
            Help(R.string.brightness_slider_help)
            Help(R.string.brightness_size_help)
            OptionRow(t(R.string.brightness_position), BrightnessSide.entries, s.side, defaults.side) { v -> edit { it.copy(side = v) } }
            if (s.side == BrightnessSide.OPPOSITE_VOLUME) {
                OptionRow(t(R.string.volume_popup_side), VolumeSide.entries, s.volumeSide, defaults.volumeSide) { v -> edit { it.copy(volumeSide = v) } }
                Help(R.string.volume_side_help)
            }
            if (s.side == BrightnessSide.CUSTOM) PercentSlider(R.string.center_x, s.x, default = defaults.x) { v -> edit { it.copy(x = v) } }
            PercentSlider(R.string.center_y, s.y, default = defaults.y) { v -> edit { it.copy(y = v) } }
            SettingSlider(R.string.slider_width, s.widthDp, 48f..128f, t(R.string.dp_value, s.widthDp.roundToInt()), default = defaults.widthDp) { v -> edit { it.copy(widthDp = v) } }
            SettingSlider(R.string.slider_height, s.heightDp, 120f..480f, t(R.string.dp_value, s.heightDp.roundToInt()), default = defaults.heightDp) { v -> edit { it.copy(heightDp = v) } }
            SettingSlider(R.string.slider_timeout, s.timeoutMs / 1000f, 1f..10f, t(R.string.seconds_value, s.timeoutMs / 1000f), default = 3f) { v -> edit { it.copy(timeoutMs = (v * 1000).toLong()) } }
            ColorPicker(s.background, title = R.string.slider_background, default = defaults.background) { v -> edit { it.copy(background = v) } }
            PercentSlider(R.string.slider_background_opacity, s.backgroundOpacity, default = 1f) { v -> edit { it.copy(backgroundOpacity = v) } }
            ColorPicker(s.track, title = R.string.slider_track, default = defaults.track) { v -> edit { it.copy(track = v) } }
            PercentSlider(R.string.slider_track_opacity, s.trackOpacity, default = 1f) { v -> edit { it.copy(trackOpacity = v) } }
            ColorPicker(s.fill, title = R.string.slider_fill, default = defaults.fill) { v -> edit { it.copy(fill = v) } }
            PercentSlider(R.string.slider_fill_opacity, s.fillOpacity, default = 1f) { v -> edit { it.copy(fillOpacity = v) } }
            ColorPicker(s.icon, title = R.string.slider_icon, default = defaults.icon) { v -> edit { it.copy(icon = v) } }
            PercentSlider(R.string.slider_icon_opacity, s.iconOpacity, default = 1f) { v -> edit { it.copy(iconOpacity = v) } }
            OutlinedButton(onClick = vm::openBrightnessAccess) { Text(t(R.string.open_brightness_access)) }
            Help(R.string.brightness_access_help)
        }
    }
}
