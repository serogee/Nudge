package com.modukey.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.modukey.app.R
import com.modukey.app.core.*

@Composable fun GestureBindingRow(config: AppConfiguration, slot: GestureSlot, target: GestureTarget, vm: NudgeViewModel, change: (GestureTarget) -> Unit) {
    val context = LocalContext.current
    var picking by remember { mutableStateOf(false) }
    val default = if (slot == GestureSlot.SWIPE_1) GestureTarget.VOLUME else GestureTarget.NONE
    Column {
        SettingTitle(t(label(slot)), reset = if (target != default) ({ change(default) }) else null)
        OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) {
            Text(context.targetLabel(config, target), Modifier.weight(1f)); Icon(AppIcons.Edit, null, Modifier.size(18.dp))
        }
    }
    if (picking) GestureBindingPicker(config, target, vm, { picking = false }) { change(it); picking = false }
}

@Composable private fun GestureBindingPicker(config: AppConfiguration, original: GestureTarget, vm: NudgeViewModel, dismiss: () -> Unit, choose: (GestureTarget) -> Unit) {
    val context = LocalContext.current
    var draft by remember { mutableStateOf(original) }
    var query by remember { mutableStateOf("") }
    var action by remember { mutableStateOf(original.action ?: PanelButton()) }
    var page by remember { mutableIntStateOf(0) }
    when (page) {
        1 -> {
            ActionPicker({ page = 0 }) { selected ->
                action = action.copy(action = selected)
                draft = GestureTarget.direct(action)
                page = if (selected == ButtonAction.OPEN_APP) 2 else 0
            }
            return
        }
        2 -> {
            AppPicker(vm, { page = 0 }) { app ->
                action = action.copy(appPackage = app.packageName, appActivity = app.activityName, appLabel = app.label)
                draft = GestureTarget.direct(action); page = 0
            }
            return
        }
    }
    AlertDialog(onDismissRequest = dismiss, title = { Text(t(R.string.gesture_action)) }, text = {
        LazyColumn(Modifier.heightIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (config.panels.isNotEmpty()) item {
                OutlinedTextField(query, { query = it }, label = { Text(t(R.string.search_panels)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            item { TargetChoice(t(R.string.no_action), draft.isNone) { draft = GestureTarget.NONE } }
            item { TargetChoice(t(R.string.volume_popup), draft == GestureTarget.VOLUME) { draft = GestureTarget.VOLUME } }
            item {
                TargetChoice(if (draft.action != null) context.buttonLabel(action) else t(R.string.direct_action), draft.action != null, action) { page = 1 }
            }
            items(config.panels.filter { context.panelLabel(it).contains(query, true) }, key = { it.id }) { panel ->
                TargetChoice(context.panelLabel(panel), draft.panelId == panel.id) { draft = GestureTarget.panel(panel.id, draft.volume && draft.panelId != null) }
            }
            if (config.panels.isEmpty() && draft.action == null) item { Help(R.string.create_panel_help) }
            if (draft.panelId != null) item { Toggle(R.string.also_volume, R.string.also_volume_help, draft.volume) { draft = draft.copy(volume = it) } }
            if (draft.action != null) {
                item { Help(R.string.direct_action_help) }
                if (action.action == ButtonAction.OPEN_APP) item {
                    OutlinedButton(onClick = { page = 2 }, modifier = Modifier.fillMaxWidth()) {
                        Icon(AppIcons.Apps, null, Modifier.size(24.dp)); Spacer(Modifier.width(12.dp))
                        Text(if (action.appPackage.isBlank()) t(R.string.choose_app) else context.buttonLabel(action), Modifier.weight(1f))
                        Icon(AppIcons.Edit, null, Modifier.size(18.dp))
                    }
                }
                if (action.action in listOf(ButtonAction.VOLUME_UP, ButtonAction.VOLUME_DOWN)) item {
                    Toggle(R.string.show_volume_popup, R.string.show_volume_help, action.showVolumePopup, default = false) {
                        action = action.copy(showVolumePopup = it); draft = GestureTarget.direct(action)
                    }
                }
            }
        }
    }, confirmButton = { Button(onClick = { choose(draft) }, enabled = (draft.panelId == null || config.panel(draft.panelId) != null) &&
        (draft.action?.action != ButtonAction.OPEN_APP || draft.action?.appPackage?.isNotBlank() == true)) { Text(t(R.string.save)) } },
        dismissButton = { TextButton(onClick = dismiss) { Text(t(R.string.cancel)) } })
}

@Composable private fun TargetChoice(title: String, selected: Boolean, graphic: PanelButton? = null, select: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = select).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected, null)
        if (graphic != null) { ButtonGraphic(graphic, "", Modifier.size(24.dp)); Spacer(Modifier.width(12.dp)) }
        Text(title, Modifier.weight(1f))
        if (graphic != null) Icon(AppIcons.Edit, null, Modifier.size(18.dp))
    }
}
