package com.modukey.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.Alignment
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

@Composable fun HandleEditor(config: AppConfiguration, h: HandleConfig, vm: NudgeViewModel, tab: Int, selectTab: (Int) -> Unit, previewEnabled: Boolean, setPreview: (Boolean) -> Unit, landscape: Boolean, selectOrientation: (Boolean) -> Unit) {
    val context = LocalContext.current
    val service by vm.status.collectAsStateWithLifecycle()
    val draft by vm.draft.collectAsStateWithLifecycle()
    val saving by vm.savingEditor.collectAsStateWithLifecycle()
    var exclusions by remember { mutableStateOf(false) }
    var reset by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    val scroll = rememberSaveable(h.id, tab, saver = ScrollState.Saver) { ScrollState(0) }
    val collapsed by remember(scroll) { derivedStateOf { scroll.value > 80 } }
    Surface(Modifier.fillMaxSize().padding(bottom = 12.dp), shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column {
            EditorHeader(context.handleLabel(config, h), draft?.changed == true, R.string.rename_handle,
                rename = { name = h.name; renaming = true }, save = vm::saveEditing, cancel = vm::cancelEditing, saving = saving) {
                HandleBadge(h, context.handleLabel(config, h))
            }
            AnimatedVisibility(!collapsed) {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Toggle(R.string.enable_handle, R.string.enable_handle_help, h.enabled, default = false) { value -> vm.editHandle(h.id) { it.copy(enabled = value) } }
                }
            }
            BrowserTabs(listOf(t(R.string.appearance), t(R.string.gestures), t(R.string.availability)), tab, selectTab)
            if (tab == 0) OverlayPreviewToggle(previewEnabled, service.connected, setPreview)
            Column(Modifier.weight(1f).verticalScroll(scroll).padding(12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                val s = h.trigger
                when(tab) {
                    0 -> {
                        val screen = rememberPreviewSpace(landscape)
                        val bounds = TriggerGeometry.bounds(screen.width, screen.height, screen.density, screen.insets, s, landscape)
                        val others = config.handles.filter { it.id != h.id && it.enabled }.map { it.trigger }
                        Section(R.string.preview) {
                            OrientationChoices(landscape, selectOrientation)
                            PhonePreview(s, landscape, others)
                            Help(R.string.preview_help)
                            Help(if (service.connected) R.string.preview_orientation else R.string.preview_unavailable)
                            if (others.any { bounds.overlaps(TriggerGeometry.bounds(screen.width, screen.height, screen.density, screen.insets, it, landscape)) }) Notice(t(R.string.overlap_help))
                            Toggle(R.string.landscape_enabled, R.string.landscape_enabled_help, s.landscapeEnabled, default = false) { value -> vm.editTrigger(h.id) { it.copy(landscapeEnabled = value) } }
                            Toggle(R.string.landscape_override, R.string.override_help, s.landscapeOverrideEnabled, default = false) { value -> vm.editTrigger(h.id) { it.copy(landscapeOverrideEnabled = value) } }
                            if (landscape && !s.landscapeOverrideEnabled) Help(R.string.inherited_help)
                        }
                        Section(R.string.touch_area) {
                            val placement = s.placement(landscape)
                            SettingTitle(t(R.string.edge), reset = if (placement.edge != Edge.RIGHT) ({
                                vm.editTrigger(h.id) { current ->
                                    if (landscape && current.landscapeOverrideEnabled) current.copy(landscape = current.landscape.copy(edge = Edge.RIGHT))
                                    else current.copy(portrait = current.portrait.copy(edge = Edge.RIGHT))
                                }
                            }) else null)
                            Choices(Edge.entries, placement.edge) { edge -> vm.editTrigger(h.id) { current ->
                                if (landscape && current.landscapeOverrideEnabled) current.copy(landscape = current.landscape.copy(edge = edge)) else current.copy(portrait = current.portrait.copy(edge = edge))
                            } }
                            PercentSlider(R.string.center_position, placement.position, default = .65f) { value -> vm.editTrigger(h.id) { current ->
                                if (landscape && current.landscapeOverrideEnabled) current.copy(landscape = current.landscape.copy(position = value)) else current.copy(portrait = current.portrait.copy(position = value))
                            } }
                            val maximumLength = TriggerGeometry.maximumLengthDp(screen.width, screen.height, screen.density, screen.insets, s.longerTouchLength).coerceAtLeast(48f)
                            SettingSlider(R.string.touch_length, s.lengthDp, 48f..maximumLength, t(R.string.dp_value, s.lengthDp.roundToInt()), default = 120f) { value -> vm.editTrigger(h.id) { it.copy(lengthDp = value) } }
                            Toggle(R.string.longer_touch_length, R.string.longer_touch_help, s.longerTouchLength, default = false) { value -> vm.editTrigger(h.id) { it.copy(longerTouchLength = value) } }
                            val effectiveLength = (if (s.placement(landscape).edge == Edge.BOTTOM) bounds.width else bounds.height) / screen.density
                            if (s.lengthDp > effectiveLength + .5f) Notice(t(R.string.touch_length_limited, s.lengthDp.roundToInt(), effectiveLength.roundToInt()))
                            SettingSlider(R.string.touch_depth, s.depthDp, 8f..32f, t(R.string.dp_value, s.depthDp.roundToInt()), default = 16f) { value -> vm.editTrigger(h.id) { it.copy(depthDp = value) } }
                        }
                        Section(R.string.visible_handle) {
                            Toggle(R.string.make_handle_visible, R.string.visible_help, s.indicatorVisible, default = false, optionsExpanded = s.indicatorVisible) { value -> vm.editTrigger(h.id) { it.copy(indicatorVisible = value) } }
                            DependentSettings(R.string.make_handle_visible) {
                                SettingSlider(R.string.indicator_length, s.indicatorLengthFraction, 0f..1f, t(R.string.touch_length_percent, (s.indicatorLengthFraction*100).roundToInt()), s.indicatorVisible, default = 2f / 3f, valueScale = 100f) { value -> vm.editTrigger(h.id) { it.copy(indicatorLengthFraction = value) } }
                                SettingSlider(R.string.indicator_thickness, s.indicatorThicknessDp, 0f..32f, t(R.string.dp_value, s.indicatorThicknessDp.roundToInt()), s.indicatorVisible, default = 4f) { value -> vm.editTrigger(h.id) { it.copy(indicatorThicknessDp = value) } }
                                Help(R.string.indicator_thickness_help)
                                PercentSlider(R.string.opacity, s.indicatorOpacity, s.indicatorVisible, default = .65f) { value -> vm.editTrigger(h.id) { it.copy(indicatorOpacity = value) } }
                                SettingSlider(R.string.corner_radius, s.cornerRadiusDp, 0f..16f, t(R.string.dp_value, s.cornerRadiusDp.roundToInt()), s.indicatorVisible, default = 2f) { value -> vm.editTrigger(h.id) { it.copy(cornerRadiusDp = value) } }
                                ColorPicker(s.indicatorColor, s.indicatorVisible, default = TriggerSettings().indicatorColor) { value -> vm.editTrigger(h.id) { it.copy(indicatorColor = value) } }
                            }
                            if (!s.indicatorVisible) Help(R.string.visible_off_help)
                        }
                    }
                    1 -> HandleGestureSettings(config, h, vm, landscape)
                    else -> Section(R.string.availability) {
                        Text(t(availabilityLabel(service.handles[h.id], h.enabled)), style = MaterialTheme.typography.bodyMedium)
                        Toggle(R.string.lock_available, R.string.lock_help, s.lockScreenEnabled, default = false) { value -> vm.editTrigger(h.id) { it.copy(lockScreenEnabled = value) } }
                        Toggle(R.string.soft_hide, R.string.soft_hide_help, s.softHideEnabled, default = false, optionsExpanded = s.softHideEnabled) { value -> vm.editTrigger(h.id) { it.copy(softHideEnabled = value) } }
                        if (s.softHideEnabled) DependentSettings(R.string.soft_hide) {
                            DurationInput(R.string.hide_after, s.softHideDelayMs, 1000L..30000L, default = 3000) { value -> vm.editTrigger(h.id) { it.copy(softHideDelayMs = value) } }
                            Help(R.string.soft_hide_detection)
                        }
                        Help(R.string.screen_off_help)
                        SettingTitle(t(R.string.excluded_apps), reset = if (s.excludedPackages.isNotEmpty()) ({ vm.editTrigger(h.id) { it.copy(excludedPackages = emptySet()) } }) else null)
                        OutlinedButton(onClick = { vm.loadApps(); exclusions = true }) { Text(t(R.string.excluded_apps)); Spacer(Modifier.width(8.dp)); Icon(AppIcons.Down, null, Modifier.size(18.dp)) }
                        Help(R.string.exclusions_help)
                        TextButton(onClick = { reset = true }) { Text(t(R.string.reset_handle)) }
                    }
                }
            }
        }
    }
    if (renaming) AlertDialog(onDismissRequest = { renaming = false }, title = { Text(t(R.string.rename_handle)) },
        text = { OutlinedTextField(name, { name = it.take(80) }, label = { Text(t(R.string.name)) }, singleLine = true,
            trailingIcon = { if (name.isNotEmpty()) IconButton(onClick = { name = "" }) { Icon(AppIcons.Reset, t(R.string.reset_setting, t(R.string.name)), Modifier.size(18.dp)) } }) },
        confirmButton = { TextButton(onClick = { vm.editHandle(h.id) { it.copy(name = name.trim()) }; vm.flush(); renaming = false }) { Text(t(R.string.save)) } },
        dismissButton = { TextButton(onClick = { renaming = false }) { Text(t(R.string.cancel)) } })
    if (exclusions) ExclusionsDialog(h, vm) { exclusions = false }
    if (reset) AlertDialog(onDismissRequest = { reset = false }, title = { Text(t(R.string.reset_handle_title)) }, text = { Text(t(R.string.reset_handle_help)) },
        confirmButton = { TextButton(onClick = { vm.resetHandle(h.id); reset = false }) { Text(t(R.string.reset_handle)) } }, dismissButton = { TextButton(onClick = { reset = false }) { Text(t(R.string.cancel)) } })
}

@Composable fun OrientationChoices(landscape: Boolean, change: (Boolean) -> Unit) {
    BrowserTabs(listOf(t(R.string.portrait), t(R.string.landscape)), if (landscape) 1 else 0) { change(it == 1) }
}
