package com.modukey.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.modukey.app.R
import com.modukey.app.core.*

@Composable fun PanelsScreen(config: AppConfiguration, vm: NudgeViewModel, wheels: Boolean = false, edit: (String) -> Unit) {
    val context = LocalContext.current
    var deleting by remember { mutableStateOf<String?>(null) }
    val panels = config.panels.filter { it.isWheel == wheels }
    Text(t(if (wheels) R.string.manage_wheels else R.string.manage_panels), style = MaterialTheme.typography.headlineMedium)
    Surface(Modifier.fillMaxSize().padding(bottom = 12.dp), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(t(if (wheels) R.string.wheels else R.string.panels), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                FilledTonalButton(onClick = { edit(vm.addPanel(wheels)) }) { Icon(AppIcons.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(t(if (wheels) R.string.add_wheel else R.string.add_panel)) }
            }
            Help(if (wheels) R.string.manage_wheels_help else R.string.manage_panels_help)
            if (panels.isEmpty()) Help(if (wheels) R.string.no_wheels else R.string.no_panels)
            ReorderableList(panels, { it.id }, context::panelLabel, Modifier.weight(1f), vm::reorderPanel, vm::flush) { panel, _ ->
                Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(context.panelLabel(panel), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        PanelCardPreview(panel.settings, config.handles.firstOrNull()?.trigger ?: TriggerSettings())
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            EditIcon(AppIcons.Edit, if (wheels) R.string.edit_wheel else R.string.edit_panel) { edit(panel.id) }
                            EditIcon(AppIcons.Duplicate, R.string.duplicate) { vm.duplicatePanel(panel.id) }
                            EditIcon(AppIcons.Delete, if (wheels) R.string.delete_wheel else R.string.delete_panel_action) { deleting = panel.id }
                        }
                    }
                }
            }
        }
    }
    deleting?.let { id -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text(t(if (wheels) R.string.delete_wheel else R.string.delete_panel)) }, text = { Text(t(if (wheels) R.string.delete_wheel_help else R.string.delete_panel_help)) },
        confirmButton = { TextButton(onClick = { vm.removePanel(id); deleting = null }) { Text(t(R.string.delete)) } },
        dismissButton = { TextButton(onClick = { deleting = null }) { Text(t(R.string.cancel)) } }) }
}
