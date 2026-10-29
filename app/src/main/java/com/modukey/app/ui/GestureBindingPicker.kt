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
import kotlin.math.roundToInt

@Composable fun GestureBindingRow(config: AppConfiguration, slot: GestureSlot, target: GestureTarget, vm: NudgeViewModel, enabled: Boolean = true, change: (GestureTarget) -> Unit) {
    val context = LocalContext.current
    var picking by remember { mutableStateOf(false) }
    val default = if (slot == GestureSlot.SWIPE_1) GestureTarget.VOLUME else GestureTarget.NONE
    Column {
        SettingTitle(t(label(slot)), enabled = enabled, reset = if (enabled && target != default) ({ change(default) }) else null)
        OutlinedButton(onClick = { picking = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            if (target.panelId != null || target.action != null) {
                Icon(if (config.panel(target.panelId)?.isWheel == true) AppIcons.Wheel else AppIcons.Apps, null, Modifier.size(24.dp)); Spacer(Modifier.width(12.dp))
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
                else { draft = draft.copy(panelId = null, action = action).normalized(); page = 0 }
            }
            return
        }
        2 -> {
            AppPicker(vm, { pendingApp = null; page = 0 }) { app ->
                val action = (pendingApp ?: draft.action ?: PanelButton(action = ButtonAction.OPEN_APP)).copy(
                    appPackage = app.packageName, appActivity = app.activityName, appLabel = app.label)
                draft = draft.copy(panelId = null, action = action).normalized()
                pendingApp = null; page = 0
            }
            return
        }
        3 -> {
            PanelTargetPicker(config, { page = 0 }) { id -> draft = draft.copy(panelId = id, action = null); page = 0 }
            return
        }
        4 -> {
            PanelTargetPicker(config, { page = 0 }, wheels = true) { id -> draft = draft.copy(panelId = id, action = null); page = 0 }
            return
        }
    }
    AlertDialog(onDismissRequest = dismiss, title = { Text(t(R.string.gesture_action)) }, text = {
        LazyColumn(Modifier.heightIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { TargetChoice(t(R.string.no_action), draft.panelId == null && draft.action == null) { draft = draft.copy(panelId = null, action = null).normalized() } }
            item { TargetChoice(t(R.string.panel_target, config.panel(draft.panelId)?.takeIf { !it.isWheel }?.let { context.panelLabel(it) } ?: t(R.string.select_panel)), config.panel(draft.panelId)?.isWheel == false, showTypeIcon = true) { page = 3 } }
            item { TargetChoice(t(R.string.wheel_target, config.panel(draft.panelId)?.takeIf { it.isWheel }?.let { context.panelLabel(it) } ?: t(R.string.select_wheel)), config.panel(draft.panelId)?.isWheel == true, showTypeIcon = true) { page = 4 } }
            item { TargetChoice(t(R.string.action_target, draft.action?.let { context.buttonLabel(it) } ?: t(R.string.direct_action)), draft.action != null, showTypeIcon = true) { page = 1 } }
            if (slot != GestureSlot.TAP && config.panel(draft.panelId)?.isWheel == true) item {
                DependentSettings(R.string.wheel) {
                    Toggle(R.string.wheel_drag, R.string.wheel_drag_help, draft.wheelPick.enabled, default = false, optionsExpanded = draft.wheelPick.enabled) {
                        draft = draft.copy(wheelPick = draft.wheelPick.copy(enabled = it), slideToAdjust = if (it) false else draft.slideToAdjust)
                    }
                    if (draft.wheelPick.enabled) DependentSettings(R.string.wheel_drag) {
                        val pick = draft.wheelPick
                        val wheel = requireNotNull(config.panel(draft.panelId)).settings
                        val automatic = wheel.portrait.wheel.hitbox.mode == WheelHitboxMode.AUTOMATIC ||
                            (wheel.landscapeOverride && wheel.landscape.wheel.hitbox.mode == WheelHitboxMode.AUTOMATIC)
                        if (automatic) {
                            SettingSlider(R.string.wheel_minimum_distance, pick.minimumDistanceDp, 16f..160f, t(R.string.dp_value, pick.minimumDistanceDp.roundToInt()), default = 32f) {
                                draft = draft.copy(wheelPick = pick.copy(minimumDistanceDp = it).normalized())
                            }
                            SettingSlider(R.string.wheel_maximum_distance, pick.maximumDistanceDp, maxOf(48f, pick.minimumDistanceDp + 8f)..600f, t(R.string.dp_value, pick.maximumDistanceDp.roundToInt()), default = 240f) {
                                draft = draft.copy(wheelPick = pick.copy(maximumDistanceDp = it).normalized())
                            }
                            OptionInfo(R.string.wheel_distance_help)
                        }
                        OptionInfo(R.string.wheel_hitbox_gesture_help)
                        if (slot.ordinal < 3) {
                            Toggle(R.string.wheel_allow_later, R.string.wheel_allow_later_help, pick.allowLaterSwipes, default = false, optionsExpanded = pick.allowLaterSwipes) {
                                draft = draft.copy(wheelPick = pick.copy(allowLaterSwipes = it))
                            }
                            if (pick.allowLaterSwipes) DependentSettings(R.string.wheel_allow_later) {
                                DurationInput(R.string.wheel_release_window, pick.releaseWindowMs, 100L..1_000L, 200) { draft = draft.copy(wheelPick = pick.copy(releaseWindowMs = it)) }
                                OptionInfo(R.string.wheel_release_help)
                            }
                        } else OptionInfo(R.string.wheel_hold_help)
                        OptionInfo(R.string.wheel_orientation_help)
                    }
                }
            }
            if (draft.action?.action in listOf(ButtonAction.TOGGLE_HANDLES, ButtonAction.TOGGLE_HANDLE_VISIBILITY, ButtonAction.TOGGLE_AND_HIDE_HANDLES)) item {
                HandleActionControls(requireNotNull(draft.action)) { draft = draft.copy(action = it) }
            }
            if (draft.action?.action in listOf(ButtonAction.BRIGHTNESS_MAXIMUM, ButtonAction.BRIGHTNESS_MINIMUM)) item {
                val action = requireNotNull(draft.action)
                DependentSettings(t(label(action.action))) {
                    BrightnessReturnControls(action.brightnessReturn) { draft = draft.copy(action = action.copy(brightnessReturn = it)) }
                }
            }
            if (draft.action?.action == ButtonAction.BRIGHTNESS_AUTOMATIC) item { OptionInfo(R.string.brightness_automatic_action_help) }
            if (draft.action?.action in listOf(ButtonAction.BRIGHTNESS_UP, ButtonAction.BRIGHTNESS_DOWN)) item { OptionInfo(R.string.brightness_step_help) }
            if (draft.action?.action in listOf(ButtonAction.NOTIFICATIONS, ButtonAction.CONTROL_PANEL)) item { OptionInfo(R.string.native_panel_help) }
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
            if (slot != GestureSlot.TAP && draft.volume && !draft.brightness && !draft.wheelPick.enabled) item {
                SliderAdjustmentOptions(slot, draft) { draft = it }
            }
            item { Toggle(R.string.show_brightness_slider, R.string.brightness_gesture_help, draft.brightness, default = false,
                optionsExpanded = if (slot != GestureSlot.TAP && !draft.volume) draft.brightness else null) { draft = draft.copy(brightness = it).normalized() } }
            if (slot != GestureSlot.TAP && draft.brightness && !draft.volume && !draft.wheelPick.enabled) item {
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
            optionsExpanded = if (ordinarySwipe) target.slideToAdjust else null) { change(target.copy(slideToAdjust = it, wheelPick = target.wheelPick.copy(enabled = false))) }
        if (ordinarySwipe && target.slideToAdjust) DependentSettings(R.string.slide_to_adjust) {
            OptionRow(t(R.string.slider_activation), SliderActivation.entries, target.sliderActivation, SliderActivation.AFTER_HOLD) { change(target.copy(sliderActivation = it)) }
            if (target.sliderActivation == SliderActivation.AFTER_HOLD) DependentSettings(R.string.slider_after_hold) {
                DurationInput(R.string.slider_hold_time, target.sliderHoldMs, 100L..1_000L, 200) { change(target.copy(sliderHoldMs = it)) }
                OptionInfo(R.string.slider_hold_help)
            } else OptionInfo(R.string.slider_immediate_help)
            if (target.action != null) OptionInfo(R.string.slider_action_timing_help)
        }
    }
}

@Composable private fun PanelTargetPicker(config: AppConfiguration, dismiss: () -> Unit, wheels: Boolean = false, choose: (String) -> Unit) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = dismiss, title = { Text(t(if (wheels) R.string.select_wheel else R.string.select_panel)) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(query, { query = it }, label = { Text(t(if (wheels) R.string.search_wheels else R.string.search_panels)) }, singleLine = true)
            if (config.panels.none { it.isWheel == wheels }) Help(if (wheels) R.string.create_wheel_help else R.string.create_panel_help)
            LazyColumn(Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(config.panels.filter { it.isWheel == wheels && context.panelLabel(it).contains(query, true) }, key = { it.id }) { panel ->
                    ListItem(headlineContent = { Text(context.panelLabel(panel)) }, leadingContent = { Icon(if (wheels) AppIcons.Wheel else AppIcons.Apps, null) },
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
