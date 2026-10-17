package com.modukey.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.modukey.app.R
import com.modukey.app.core.*
import kotlin.math.roundToInt

@Composable fun HandleEditor(config: AppConfiguration, h: HandleConfig, vm: NudgeViewModel, openPanel: (Int) -> Unit) {
    val context = LocalContext.current
    var tab by rememberSaveable(h.id) { mutableIntStateOf(0) }
    var landscape by rememberSaveable(h.id) { mutableStateOf(false) }
    var exclusions by remember { mutableStateOf(false) }
    var reset by remember { mutableStateOf(false) }
    Section(context.handleLabel(config, h)) {
        OutlinedTextField(value = h.name, onValueChange = { value -> vm.editHandle(h.id) { it.copy(name = value) } }, label = { Text(t(R.string.name)) },
            placeholder = { Text(context.handleLabel(config, h)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Toggle(R.string.enable_handle, R.string.enable_handle_help, h.enabled) { value -> vm.editHandle(h.id) { it.copy(enabled = value) } }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(R.string.appearance, R.string.gestures, R.string.panels, R.string.availability).forEachIndexed { index, label ->
            FilterChip(selected = tab == index, onClick = { tab = index }, label = { Text(t(label)) })
        }
    }
    val s = h.trigger
    when(tab) {
        0 -> {
            val service by vm.status.collectAsStateWithLifecycle()
            val screen = rememberPreviewSpace(landscape)
            val bounds = TriggerGeometry.bounds(screen.width, screen.height, screen.density, screen.insets, s, landscape)
            val others = config.handles.filter { it.id != h.id && it.enabled }.map { it.trigger }
            Section(R.string.preview) {
                OrientationChoices(landscape) { landscape = it }
                PhonePreview(s, landscape, vm::flush, others) { value -> vm.editTrigger(h.id) { current ->
                    if (landscape && current.landscapeOverrideEnabled) current.copy(landscape = current.landscape.copy(position = value))
                    else current.copy(portrait = current.portrait.copy(position = value))
                } }
                Help(R.string.preview_help)
                Help(if (service.connected) R.string.preview_orientation else R.string.preview_unavailable)
                if (others.any { bounds.overlaps(TriggerGeometry.bounds(screen.width, screen.height, screen.density, screen.insets, it, landscape)) }) Notice(t(R.string.overlap_help))
                Toggle(R.string.landscape_enabled, R.string.landscape_enabled_help, s.landscapeEnabled) { value -> vm.editTrigger(h.id) { it.copy(landscapeEnabled = value) } }
                Toggle(R.string.landscape_override, R.string.override_help, s.landscapeOverrideEnabled) { value -> vm.editTrigger(h.id) { it.copy(landscapeOverrideEnabled = value) } }
                if (landscape && !s.landscapeOverrideEnabled) Help(R.string.inherited_help)
            }
            Section(R.string.touch_area) {
                val placement = s.placement(landscape)
                Choices(Edge.entries, placement.edge) { edge -> vm.editTrigger(h.id) { current ->
                    if (landscape && current.landscapeOverrideEnabled) current.copy(landscape = current.landscape.copy(edge = edge)) else current.copy(portrait = current.portrait.copy(edge = edge))
                } }
                PercentSlider(R.string.center_position, placement.position) { value -> vm.editTrigger(h.id) { current ->
                    if (landscape && current.landscapeOverrideEnabled) current.copy(landscape = current.landscape.copy(position = value)) else current.copy(portrait = current.portrait.copy(position = value))
                } }
                SettingSlider(R.string.touch_length, s.lengthDp, 48f..200f, t(R.string.dp_value, s.lengthDp.roundToInt())) { value -> vm.editTrigger(h.id) { it.copy(lengthDp = value) } }
                SettingSlider(R.string.touch_depth, s.depthDp, 8f..32f, t(R.string.dp_value, s.depthDp.roundToInt())) { value -> vm.editTrigger(h.id) { it.copy(depthDp = value) } }
            }
            Section(R.string.visible_handle) {
                Toggle(R.string.make_handle_visible, R.string.visible_help, s.indicatorVisible) { value -> vm.editTrigger(h.id) { it.copy(indicatorVisible = value) } }
                SettingSlider(R.string.indicator_length, s.indicatorLengthFraction, 0f..1f, t(R.string.touch_length_percent, (s.indicatorLengthFraction*100).roundToInt()), s.indicatorVisible) { value -> vm.editTrigger(h.id) { it.copy(indicatorLengthFraction = value) } }
                SettingSlider(R.string.indicator_thickness, s.indicatorThicknessFraction, 0f..1f, t(R.string.touch_depth_percent, (s.indicatorThicknessFraction*100).roundToInt()), s.indicatorVisible) { value -> vm.editTrigger(h.id) { it.copy(indicatorThicknessFraction = value) } }
                PercentSlider(R.string.opacity, s.indicatorOpacity, s.indicatorVisible) { value -> vm.editTrigger(h.id) { it.copy(indicatorOpacity = value) } }
                SettingSlider(R.string.corner_radius, s.cornerRadiusDp, 0f..16f, t(R.string.dp_value, s.cornerRadiusDp.roundToInt()), s.indicatorVisible) { value -> vm.editTrigger(h.id) { it.copy(cornerRadiusDp = value) } }
                ColorPicker(s.indicatorColor, s.indicatorVisible) { value -> vm.editTrigger(h.id) { it.copy(indicatorColor = value) } }
                if (!s.indicatorVisible) Help(R.string.visible_off_help)
            }
        }
        1 -> {
            Section(R.string.gestures) {
                GestureSlot.entries.forEach { slot -> OptionRow(t(label(slot)), GestureTarget.entries, h.target(slot)) { value -> vm.editHandle(h.id) { it.copy(bindings = it.bindings + (slot to value)) } } }
                Help(R.string.gesture_help); Help(R.string.hold_help)
            }
            Section(R.string.settings) {
                SettingSlider(R.string.swipe_distance, s.swipeDistanceDp, 12f..80f, t(R.string.dp_value, s.swipeDistanceDp.roundToInt())) { value -> vm.editTrigger(h.id) { it.copy(swipeDistanceDp = value) } }
                SettingSlider(R.string.swipe_time, s.swipeTimeoutMs.toFloat(), 200f..1200f, t(R.string.ms_value, s.swipeTimeoutMs.toInt())) { value -> vm.editTrigger(h.id) { it.copy(swipeTimeoutMs = value.toLong()) } }
                SettingSlider(R.string.volume_sequence, h.volumeSequenceMs.toFloat(), 1000f..10000f, t(R.string.seconds_value, h.volumeSequenceMs / 1000f)) { value -> vm.editHandle(h.id) { it.copy(volumeSequenceMs = value.toLong()) } }
                Help(R.string.volume_sequence_help)
                Toggle(R.string.haptics, R.string.haptics_help, s.haptics) { value -> vm.editTrigger(h.id) { it.copy(haptics = value) } }
            }
        }
        2 -> h.panels.forEachIndexed { index, p -> Section(t(R.string.panel_name, index + 1)) {
            Text(t(R.string.button_count, p.buttons.size), style = MaterialTheme.typography.bodySmall)
            Button(onClick = { openPanel(index) }) { Text(t(R.string.edit)) }
        } }
        else -> Section(R.string.availability) {
            Toggle(R.string.lock_available, R.string.lock_help, s.lockScreenEnabled) { value -> vm.editTrigger(h.id) { it.copy(lockScreenEnabled = value) } }
            Help(R.string.screen_off_help)
            OutlinedButton(onClick = { vm.loadApps(); exclusions = true }) { Text(t(R.string.excluded_apps)) }
            Help(R.string.exclusions_help)
            TextButton(onClick = { reset = true }) { Text(t(R.string.reset_handle)) }
        }
    }
    if (exclusions) ExclusionsDialog(h, vm) { exclusions = false }
    if (reset) AlertDialog(onDismissRequest = { reset = false }, title = { Text(t(R.string.reset_handle_title)) }, text = { Text(t(R.string.reset_handle_help)) },
        confirmButton = { TextButton(onClick = { vm.resetHandle(h.id); reset = false }) { Text(t(R.string.reset_handle)) } }, dismissButton = { TextButton(onClick = { reset = false }) { Text(t(R.string.cancel)) } })
}

@Composable fun OrientationChoices(landscape: Boolean, change: (Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(!landscape, { change(false) }, label = { Text(t(R.string.portrait)) })
        FilterChip(landscape, { change(true) }, label = { Text(t(R.string.landscape)) })
    }
}
