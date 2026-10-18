package com.modukey.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.modukey.app.R
import com.modukey.app.core.*

/** A button is saved once. Cancel cannot leave a partly configured app action. */
@Composable fun ButtonEditor(button: PanelButton, panel: PanelConfig, vm: NudgeViewModel, dismiss: () -> Unit, save: (PanelButton) -> Unit) {
    var draft by remember(button.id) { mutableStateOf(button) }
    var page by remember(button.id) { mutableIntStateOf(if (button.action == ButtonAction.OPEN_APP && button.appPackage.isBlank()) 2 else 0) }
    val context = LocalContext.current
    val packs by vm.iconPacks.collectAsStateWithLifecycle()
    when(page) {
        1 -> ActionPicker({ page = 0 }) { action -> draft = draft.copy(action = action); page = if (action == ButtonAction.OPEN_APP) 2 else 0 }
        2 -> AppPicker(vm, { page = 0 }) { app -> draft = draft.copy(appPackage = app.packageName, appActivity = app.activityName, appLabel = app.label); page = 0 }
        3 -> IconPicker(packs, { page = 0 }, draft.icon) { icon -> draft = draft.copy(icon = icon); page = 0 }
        else -> AlertDialog(onDismissRequest = dismiss, title = { Text(t(R.string.edit_button)) }, text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(Modifier.fillMaxWidth(), color = androidx.compose.ui.graphics.Color(panel.background).copy(alpha = panel.opacity), shape = MaterialTheme.shapes.large) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        PanelButtonFace(draft, panel, 64.dp)
                        Text(context.buttonLabel(draft), color = contrastOn(androidx.compose.ui.graphics.Color(panel.background)), style = MaterialTheme.typography.titleSmall)
                    }
                }
                SettingTitle(t(R.string.button_action), reset = if (draft.action != ButtonAction.VOLUME_UP) ({ draft = draft.copy(action = ButtonAction.VOLUME_UP) }) else null)
                OutlinedButton(onClick = { page = 1 }, modifier = Modifier.fillMaxWidth()) {
                    ButtonGraphic(PanelButton(action = draft.action), "", Modifier.size(24.dp)); Spacer(Modifier.width(12.dp))
                    Text(t(label(draft.action)), Modifier.weight(1f)); Icon(AppIcons.Edit, null, Modifier.size(18.dp))
                }
                if (draft.action == ButtonAction.OPEN_APP) OutlinedButton(onClick = { page = 2 }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (draft.appPackage.isBlank()) t(R.string.choose_app) else draft.appLabel.ifBlank { draft.appPackage }, Modifier.weight(1f)); Icon(AppIcons.Apps, null)
                }
                if (draft.action in listOf(ButtonAction.VOLUME_UP, ButtonAction.VOLUME_DOWN))
                    Toggle(R.string.show_volume_popup, R.string.show_volume_help, draft.showVolumePopup, default = false) { draft = draft.copy(showVolumePopup = it) }
                if (draft.action == ButtonAction.EMPTY) {
                    OptionRow(t(R.string.appearance), EmptyAppearance.entries, draft.emptyAppearance, EmptyAppearance.SLASH) { draft = draft.copy(emptyAppearance = it) }
                    Help(R.string.empty_button_help)
                } else {
                SettingTitle(t(R.string.button_icon), reset = if (draft.icon != IconReference()) ({ draft = draft.copy(icon = IconReference()) }) else null)
                OutlinedButton(onClick = { vm.loadPacks(); page = 3 }, modifier = Modifier.fillMaxWidth()) {
                    ButtonGraphic(draft, panel.iconPack, Modifier.size(24.dp)); Spacer(Modifier.width(12.dp)); Text(t(R.string.choose_icon), Modifier.weight(1f)); Icon(AppIcons.Edit, null, Modifier.size(18.dp))
                }
                OptionRow(t(R.string.icon_container), IconContainer.entries, draft.iconContainer, IconContainer.DEFAULT) { draft = draft.copy(iconContainer = it) }
                }
            }
        }, confirmButton = { Button(onClick = { save(draft) }, enabled = draft.action != ButtonAction.OPEN_APP || draft.appPackage.isNotBlank()) { Text(t(R.string.save)) } },
            dismissButton = { TextButton(onClick = dismiss) { Text(t(R.string.cancel)) } })
    }
}
