package com.modukey.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.modukey.app.R
import com.modukey.app.core.*
import com.modukey.app.platform.ShortcutIconRenderer

@Composable fun HomeShortcutsSection(config: AppConfiguration, vm: NudgeViewModel) {
    val context = LocalContext.current
    val shortcutPanel = ShortcutIconRenderer.panel
    val supported by vm.shortcutSupported.collectAsStateWithLifecycle()
    val pinned by vm.pinnedShortcuts.collectAsStateWithLifecycle()
    val busy by vm.shortcutBusy.collectAsStateWithLifecycle()
    var choosing by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<HomeShortcut?>(null) }
    var naming by remember { mutableStateOf<HomeShortcut?>(null) }
    var original by remember { mutableStateOf<HomeShortcut?>(null) }
    var removing by remember { mutableStateOf<HomeShortcut?>(null) }
    Section(R.string.home_shortcuts) {
        OptionInfo(R.string.shortcut_help)
        OptionInfo(R.string.shortcut_icon_help)
        if (supported == false) Help(R.string.shortcut_unsupported)
        if (config.shortcuts.isEmpty()) Help(R.string.shortcut_none)
        config.shortcuts.forEach { shortcut ->
            val name = shortcut.name.ifBlank { context.buttonLabel(shortcut.button) }
            ListItem(headlineContent = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                supportingContent = { Text(t(if (shortcut.id in pinned) R.string.shortcut_pinned else R.string.shortcut_not_pinned)) },
                leadingContent = { ShortcutGraphic(shortcut.button, 32.dp) },
                trailingContent = { Row {
                    EditIcon(AppIcons.Edit, R.string.edit_shortcut, enabled = !busy) { original = shortcut; editing = shortcut }
                    EditIcon(AppIcons.Add, R.string.pin_again, enabled = !busy && supported == true) { vm.pinShortcut(shortcut) }
                    EditIcon(AppIcons.Delete, R.string.delete, enabled = !busy) { removing = shortcut }
                } })
        }
        FilledTonalButton(onClick = { vm.loadApps(); vm.loadPacks(); choosing = true }, enabled = supported == true && !busy) {
            Icon(AppIcons.Add, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(t(R.string.add_shortcut))
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    }
    if (choosing) ActionPicker({ choosing = false }, includeEmpty = false) { action ->
        choosing = false; original = null; editing = HomeShortcut(button = PanelButton(action = action))
    }
    editing?.let { shortcut ->
        ButtonEditor(shortcut.button, shortcutPanel, vm, { editing = null; original = null }, includeEmpty = false, homeShortcut = true) {
            editing = null; naming = shortcut.copy(button = it)
        }
    }
    naming?.let { shortcut ->
        val button = shortcut.button
        var name by remember(shortcut.id) { mutableStateOf(shortcut.name.ifBlank { context.buttonLabel(button) }) }
        var owner by remember(shortcut.id) { mutableStateOf(shortcut.handleId ?: config.handles.firstOrNull()?.id) }
        var expanded by remember { mutableStateOf(false) }
        val needsOwner = button.needsHandleOwner
        AlertDialog(onDismissRequest = { naming = null; original = null }, title = { Text(t(if (original == null) R.string.add_shortcut else R.string.edit_shortcut)) }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { ShortcutGraphic(button, 64.dp) }
                OutlinedTextField(name, { name = it.take(80) }, label = { Text(t(R.string.shortcut_name)) }, singleLine = true)
                OutlinedButton(onClick = { naming = null; editing = shortcut.copy(name = name, handleId = owner) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(AppIcons.Edit, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(t(R.string.shortcut_configure))
                }
                if (needsOwner) {
                    SettingTitle(t(R.string.shortcut_owner))
                    Box {
                        OutlinedButton(onClick = { expanded = true }) {
                            Text(config.handle(owner)?.let { context.handleLabel(config, it) } ?: t(R.string.select_handle))
                            Spacer(Modifier.width(8.dp)); Icon(AppIcons.Down, null)
                        }
                        DropdownMenu(expanded, { expanded = false }) { config.handles.forEach { h ->
                            DropdownMenuItem(text = { Text(context.handleLabel(config, h)) }, onClick = { owner = h.id; expanded = false })
                        } }
                    }
                    OptionInfo(R.string.shortcut_owner_help)
                }
            }
        }, confirmButton = { Button(onClick = {
            val updated = shortcut.copy(name = name.trim(), button = button, handleId = if (needsOwner) owner else null)
            val baseline = original
            if (baseline == null) vm.pinShortcut(updated) else vm.updateShortcut(baseline, updated)
            naming = null; original = null
        }, enabled = name.isNotBlank() && (!needsOwner || config.handle(owner) != null) && !busy) { Text(t(if (original == null) R.string.pin_shortcut else R.string.save)) } },
            dismissButton = { TextButton(onClick = { naming = null; original = null }) { Text(t(R.string.cancel)) } })
    }
    removing?.let { shortcut ->
        AlertDialog(onDismissRequest = { removing = null }, title = { Text(t(R.string.delete)) }, text = { Text(t(R.string.shortcut_remove_help)) },
            confirmButton = { TextButton(onClick = { vm.removeShortcut(shortcut.id); removing = null }) { Text(t(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { removing = null }) { Text(t(R.string.cancel)) } })
    }
}
