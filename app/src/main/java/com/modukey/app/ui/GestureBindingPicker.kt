package com.modukey.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.modukey.app.R
import com.modukey.app.core.*

@Composable fun GestureBindingRow(config: AppConfiguration, slot: GestureSlot, target: GestureTarget, vm: NudgeViewModel, enabled: Boolean = true, change: (GestureTarget) -> Unit) {
    val context = LocalContext.current
    var picking by remember { mutableStateOf(false) }
    val default = if (slot == GestureSlot.SWIPE_1) GestureTarget.VOLUME else GestureTarget.NONE
    Column {
        SettingTitle(t(label(slot)), enabled = enabled, reset = if (enabled && target != default) ({ change(default) }) else null)
        OutlinedButton(onClick = { picking = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            if (target.panelId != null || target.action != null) {
                Icon(AppIcons.Apps, null, Modifier.size(24.dp)); Spacer(Modifier.width(12.dp))
            }
            Text(context.targetLabel(config, target), Modifier.weight(1f)); Icon(AppIcons.Down, null, Modifier.size(18.dp))
        }
    }
    if (picking && enabled) GestureBindingPicker(config, slot, target, vm, { picking = false }) { change(it); picking = false }
}

@Composable private fun GestureBindingPicker(config: AppConfiguration, slot: GestureSlot, original: GestureTarget, vm: NudgeViewModel, dismiss: () -> Unit, choose: (GestureTarget) -> Unit) {
    val context = LocalContext.current
    var draft by remember { mutableStateOf(original) }
    var pendingApp by remember { mutableStateOf<PanelButton?>(null) }
    var page by remember { mutableIntStateOf(0) }
    when (page) {
        1 -> {
            ActionPicker({ page = 0 }, includeEmpty = false, volumePopupOptions = false) { selected ->
                val action = (draft.action ?: PanelButton()).copy(action = selected, showVolumePopup = false)
                if (selected == ButtonAction.OPEN_APP) { pendingApp = action; vm.loadApps(); page = 2 }
                else { draft = draft.copy(panelId = null, action = action); page = 0 }
            }
            return
        }
        2 -> {
            AppPicker(vm, { pendingApp = null; page = 0 }) { app ->
                val action = (pendingApp ?: draft.action ?: PanelButton(action = ButtonAction.OPEN_APP)).copy(
                    appPackage = app.packageName, appActivity = app.activityName, appLabel = app.label)
                draft = draft.copy(panelId = null, action = action)
                pendingApp = null; page = 0
            }
            return
        }
        3 -> {
            PanelTargetPicker(config, { page = 0 }) { id -> draft = draft.copy(panelId = id, action = null); page = 0 }
            return
        }
    }
    AlertDialog(onDismissRequest = dismiss, title = { Text(t(R.string.gesture_action)) }, text = {
        LazyColumn(Modifier.heightIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { TargetChoice(t(R.string.no_action), draft.panelId == null && draft.action == null) { draft = draft.copy(panelId = null, action = null) } }
            item { TargetChoice(t(R.string.panel_target, config.panel(draft.panelId)?.let { context.panelLabel(it) } ?: t(R.string.select_panel)), draft.panelId != null, showTypeIcon = true) { page = 3 } }
            item { TargetChoice(t(R.string.action_target, draft.action?.let { context.buttonLabel(it) } ?: t(R.string.direct_action)), draft.action != null, showTypeIcon = true) { page = 1 } }
            if (draft.action?.action in listOf(ButtonAction.TOGGLE_HANDLES, ButtonAction.TOGGLE_HANDLE_VISIBILITY, ButtonAction.TOGGLE_AND_HIDE_HANDLES)) item {
                HandleActionControls(requireNotNull(draft.action)) { draft = draft.copy(action = it) }
            }
            if (draft.action?.action in listOf(ButtonAction.BRIGHTNESS_MAXIMUM, ButtonAction.BRIGHTNESS_MINIMUM)) item {
                val action = requireNotNull(draft.action)
                DependentSettings(t(label(action.action))) {
                    BrightnessReturnControls(action.brightnessReturn) { draft = draft.copy(action = action.copy(brightnessReturn = it)) }
                }
            }
            if (draft.action?.action == ButtonAction.BRIGHTNESS_AUTOMATIC) item { Help(R.string.brightness_automatic_action_help) }
            if (draft.action?.action in listOf(ButtonAction.BRIGHTNESS_UP, ButtonAction.BRIGHTNESS_DOWN)) item { Help(R.string.brightness_step_help) }
            if (draft.action?.action in listOf(ButtonAction.NOTIFICATIONS, ButtonAction.CONTROL_PANEL)) item { Help(R.string.native_panel_help) }
            if (draft.action?.action == ButtonAction.OPEN_APP) item {
                DependentSettings(t(label(ButtonAction.OPEN_APP))) {
                    OutlinedButton(onClick = { pendingApp = draft.action; vm.loadApps(); page = 2 }, modifier = Modifier.fillMaxWidth()) {
                        Icon(AppIcons.Apps, null, Modifier.size(24.dp)); Spacer(Modifier.width(12.dp))
                        Text(context.buttonLabel(requireNotNull(draft.action)), Modifier.weight(1f))
                        Icon(AppIcons.Down, null, Modifier.size(18.dp))
                    }
                }
            }
            item { Toggle(R.string.show_volume_popup, R.string.gesture_popup_help, draft.volume, default = false,
                optionsExpanded = if (slot != GestureSlot.TAP && !draft.brightness) draft.volume else null) { draft = draft.copy(volume = it).normalized() } }
            if (slot != GestureSlot.TAP && draft.volume && !draft.brightness) item {
                SliderAdjustmentOptions(slot, draft) { draft = it }
            }
            item { Toggle(R.string.show_brightness_slider, R.string.brightness_gesture_help, draft.brightness, default = false,
                optionsExpanded = if (slot != GestureSlot.TAP && !draft.volume) draft.brightness else null) { draft = draft.copy(brightness = it).normalized() } }
            if (slot != GestureSlot.TAP && draft.brightness && !draft.volume) item {
                SliderAdjustmentOptions(slot, draft) { draft = it }
            }
        }
    }, confirmButton = { Button(onClick = { choose(draft.normalized()) }, enabled = (draft.panelId == null || config.panel(draft.panelId) != null) &&
        (draft.action?.action != ButtonAction.TOGGLE_AND_HIDE_HANDLES || draft.action?.hasHandleGroups == true) &&
        (draft.action?.action != ButtonAction.OPEN_APP || draft.action?.appPackage?.isNotBlank() == true)) { Text(t(R.string.save)) } },
        dismissButton = { TextButton(onClick = dismiss) { Text(t(R.string.cancel)) } })
}

@Composable private fun SliderAdjustmentOptions(slot: GestureSlot, target: GestureTarget, change: (GestureTarget) -> Unit) {
    DependentSettings(if (target.volume) R.string.show_volume_popup else R.string.show_brightness_slider) {
        val ordinarySwipe = slot in listOf(GestureSlot.SWIPE_1, GestureSlot.SWIPE_2, GestureSlot.SWIPE_3)
        Toggle(R.string.slide_to_adjust, R.string.slide_to_adjust_help, target.slideToAdjust, default = false,
            optionsExpanded = if (ordinarySwipe) target.slideToAdjust else null) { change(target.copy(slideToAdjust = it)) }
        if (ordinarySwipe && target.slideToAdjust) DependentSettings(R.string.slide_to_adjust) {
            OptionRow(t(R.string.slider_activation), SliderActivation.entries, target.sliderActivation, SliderActivation.AFTER_HOLD) { change(target.copy(sliderActivation = it)) }
            if (target.sliderActivation == SliderActivation.AFTER_HOLD) DependentSettings(R.string.slider_after_hold) {
                DurationInput(R.string.slider_hold_time, target.sliderHoldMs, 100L..1_000L, 200) { change(target.copy(sliderHoldMs = it)) }
                Help(R.string.slider_hold_help)
            } else Help(R.string.slider_immediate_help)
            if (target.action != null) Help(R.string.slider_action_timing_help)
        }
    }
}

@Composable private fun PanelTargetPicker(config: AppConfiguration, dismiss: () -> Unit, choose: (String) -> Unit) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = dismiss, title = { Text(t(R.string.select_panel)) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(query, { query = it }, label = { Text(t(R.string.search_panels)) }, singleLine = true)
            if (config.panels.isEmpty()) Help(R.string.create_panel_help)
            LazyColumn(Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(config.panels.filter { context.panelLabel(it).contains(query, true) }, key = { it.id }) { panel ->
                    ListItem(headlineContent = { Text(context.panelLabel(panel)) }, leadingContent = { Icon(AppIcons.Apps, null) },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(role = Role.Button) { choose(panel.id) })
                }
            }
        }
    }, confirmButton = { TextButton(onClick = dismiss) { Text(t(R.string.cancel)) } })
}

@Composable private fun TargetChoice(title: String, selected: Boolean, showTypeIcon: Boolean = false, select: () -> Unit) {
    Row(Modifier.fillMaxWidth().selectable(selected, role = Role.RadioButton, onClick = select).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected, null)
        Spacer(Modifier.width(12.dp))
        if (showTypeIcon) { Icon(AppIcons.Apps, null, Modifier.size(24.dp)); Spacer(Modifier.width(12.dp)) }
        Text(title, Modifier.weight(1f))
        if (showTypeIcon) { Spacer(Modifier.width(8.dp)); Icon(AppIcons.Down, null, Modifier.size(18.dp)) }
    }
}
