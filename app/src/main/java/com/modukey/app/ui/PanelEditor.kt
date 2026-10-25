package com.modukey.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.modukey.app.R
import com.modukey.app.core.*
import kotlin.math.roundToInt

@Composable private fun PanelEditorContent(config: AppConfiguration, entry: SavedPanel, previewHandleId: String?, vm: NudgeViewModel, preview: (String?) -> Unit, previewEnabled: Boolean, landscape: Boolean, selectOrientation: (Boolean) -> Unit) {
    val context = LocalContext.current
    var adding by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf<PanelButton?>(null) }
    var packPicker by remember { mutableStateOf(false) }
    val packs by vm.iconPacks.collectAsStateWithLifecycle()
    val service by vm.status.collectAsStateWithLifecycle()
    LaunchedEffect(entry.id) { vm.loadPacks() }
    val p = entry.settings
    val trigger = config.handle(previewHandleId)?.trigger ?: TriggerSettings()
    val l = p.layout(landscape)
    val defaults = PanelLayout()
    val panelDefaults = PanelConfig()
    val portraitScreen = rememberPreviewSpace(false)
    val landscapeScreen = rememberPreviewSpace(true)
    val screen = if (landscape) landscapeScreen else portraitScreen
    val handle = TriggerGeometry.bounds(screen.width, screen.height, screen.density, screen.insets, trigger, landscape)
    val g = PanelGeometry.calculate(screen.usable, screen.density, l, p.buttons.size, handle, trigger.placement(landscape).edge)
    fun layout(change: (PanelLayout) -> PanelLayout) = vm.editLayout(entry.id, landscape, change)
    fun fitted(value: PanelLayout, isLandscape: Boolean, count: Int): PanelLayout {
        val space = if (isLandscape) landscapeScreen else portraitScreen
        val wheel = WheelAutofit.fit(value.wheel, space.usable, space.density, count, trigger.placement(isLandscape).edge)
        return value.copy(wheel = wheel ?: value.wheel)
    }
    fun fitButtons(value: PanelConfig): PanelConfig = if (!value.isWheel) value else value.copy(
        portrait = fitted(value.portrait, false, value.buttons.size),
        landscape = if (value.landscapeOverride) fitted(value.landscape, true, value.buttons.size) else value.landscape)
    val nextWheel = remember(l, screen, p.buttons.size, trigger) {
        if (l.mode != PanelMode.WHEEL) null else {
            val next = fitted(l, landscape, p.buttons.size + 1)
            WheelGeometry.calculate(screen.usable, screen.density, next, next.wheel, p.buttons.size + 1, handle, trigger.placement(landscape).edge)
        }
    }
    Section(R.string.buttons) {
        if (p.buttons.isEmpty()) Help(R.string.no_buttons_setup)
        else ReorderableList(p.buttons, { it.id }, context::buttonLabel, Modifier.fillMaxWidth().heightIn(max = 400.dp),
            moveTo = { id, target -> vm.reorderButton(entry.id, id, target) }, finish = vm::flush) { b, position ->
            Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(Modifier.fillMaxWidth().padding(start = 10.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f).alpha(if (position >= g.visibleCount) .38f else 1f),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (b.action == ButtonAction.EMPTY) Icon(AppIcons.Empty, null, Modifier.size(40.dp)) else PanelButtonFace(b, p, 40.dp)
                        Column { Text(context.buttonLabel(b)); if (position >= g.visibleCount) Text(t(R.string.not_shown), style = MaterialTheme.typography.labelSmall) }
                    }
                    EditIcon(AppIcons.Edit, R.string.edit_button) { draft = b }
                    EditIcon(AppIcons.Delete, R.string.delete) { vm.editPanel(entry.id) { fitButtons(it.copy(buttons = it.buttons.filterNot { item -> item.id == b.id })) }; vm.flush() }
                }
            }
        }
        if (p.buttons.size > g.visibleCount) Notice(t(R.string.panel_overflow, p.buttons.size - g.visibleCount))
        if (g.wheel != null) Text(t(R.string.wheel_capacity, g.wheel.capacity), style = MaterialTheme.typography.bodySmall)
        if (entry.isWheel) Toggle(R.string.wheel_autofit, R.string.wheel_autofit_help, l.wheel.autoFit, default = true) { enabled ->
            vm.editPanel(entry.id) { current ->
                fun update(value: PanelLayout) = value.copy(wheel = value.wheel.copy(autoFit = enabled)).let {
                    if (enabled) fitted(it, landscape, current.buttons.size) else it
                }
                if (landscape && current.landscapeOverride) current.copy(landscape = update(current.landscape))
                else current.copy(portrait = update(current.portrait))
            }
        }
        FilledTonalButton(onClick = { adding = true }, enabled = nextWheel == null || (p.buttons.size < 6 && p.buttons.size < nextWheel.capacity), modifier = Modifier.fillMaxWidth()) {
            Icon(AppIcons.Add, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(t(R.string.add_button))
        }
    }
    Section(R.string.preview) {
        PreviewHandleChoice(config, previewHandleId, preview)
        Help(R.string.preview_handle_help)
        OrientationChoices(landscape, selectOrientation)
        PanelPreview(p, l, g, screen, handle, trigger)
        Help(R.string.panel_preview_help)
        Help(if (service.connected) R.string.preview_orientation else R.string.preview_unavailable)
        OutlinedButton(onClick = { vm.testPanel(entry.id, previewHandleId) }, enabled = service.connected && previewEnabled) { Text(t(R.string.test_panel)) }
        Toggle(R.string.panel_landscape_override, R.string.override_help, p.landscapeOverride, default = false) { value -> vm.editPanel(entry.id) { fitButtons(it.copy(landscapeOverride = value)) } }
        if (landscape && !p.landscapeOverride) Help(R.string.inherited_help)
    }
    Section(R.string.layout) {
        if (!entry.isWheel) OptionRow(t(R.string.layout), listOf(PanelMode.FIXED, PanelMode.SCROLLABLE), l.mode, defaults.mode) { value -> layout { it.copy(mode = value) } }
        DependentSettings(R.string.layout) {
            if (l.mode == PanelMode.FIXED) {
                CountStepper(if (l.direction == PanelDirection.VERTICAL) R.string.maximum_rows else R.string.maximum_columns, l.maximumLines, 1, 20, 5) { value -> layout { it.copy(maximumLines = value) } }
                Help(R.string.fixed_help)
            } else if (l.mode == PanelMode.SCROLLABLE) {
                SettingSlider(if (l.direction == PanelDirection.VERTICAL) R.string.panel_height else R.string.panel_width, l.extentFraction, .15f..1f,
                    t(R.string.percent, (l.extentFraction * 100).roundToInt()), default = .6f, valueScale = 100f) { value -> layout { it.copy(extentFraction = value) } }
                Help(R.string.scrollable_help)
            }
        }
        if (l.mode == PanelMode.WHEEL) {
            WheelRadiusControl(l.wheel, l.buttonSizeDp) { inner, outer ->
                layout { it.copy(wheel = it.wheel.copy(innerRadiusDp = inner, outerRadiusDp = outer)) }
            }
            Help(R.string.wheel_autofit_priority)
            Help(R.string.wheel_radius_help)
            SettingSlider(R.string.wheel_gap, l.wheel.gapDp, 0f..24f, t(R.string.dp_value, l.wheel.gapDp.roundToInt()), default = 4f) { value ->
                vm.editPanel(entry.id) { current ->
                    val changed = current.layout(landscape).let { it.copy(wheel = it.wheel.copy(gapDp = value)) }
                    if (landscape && current.landscapeOverride) current.copy(landscape = fitted(changed, true, current.buttons.size))
                    else current.copy(portrait = fitted(changed, false, current.buttons.size))
                }
            }
            Help(R.string.wheel_gap_help)
            WheelHitboxControls(l.wheel.hitbox) { change -> layout { it.copy(wheel = it.wheel.copy(hitbox = change(it.wheel.hitbox))) } }
            WheelIconControls(l.wheel) { value -> layout { it.copy(wheel = value) } }
            Toggle(R.string.wheel_expand, R.string.wheel_expand_help, l.wheel.expandSelection, default = true, optionsExpanded = l.wheel.expandSelection) { value -> layout { it.copy(wheel = it.wheel.copy(expandSelection = value)) } }
            if (l.wheel.expandSelection) DependentSettings(R.string.wheel_expand) {
                SettingSlider(R.string.wheel_selected_size, l.wheel.selectedScale, 1f..1.5f, t(R.string.percent, (l.wheel.selectedScale * 100).roundToInt()), default = WheelLayoutSettings.DEFAULT_SELECTED_SCALE, valueScale = 100f) { value -> layout { it.copy(wheel = it.wheel.copy(selectedScale = value)) } }
            }
            Toggle(R.string.wheel_animations, R.string.wheel_animations_help, l.wheel.animationsEnabled, default = true) { value ->
                layout { it.copy(wheel = it.wheel.copy(animationsEnabled = value)) }
            }
            if (l.wheel.animationsEnabled) DependentSettings(R.string.wheel_animations) {
                OptionRow(t(R.string.wheel_focus_animation), WheelFocusAnimation.entries, l.wheel.focusAnimation, WheelFocusAnimation.SMOOTH) { value ->
                    layout { it.copy(wheel = it.wheel.copy(focusAnimation = value)) }
                }
                if (l.wheel.focusAnimation == WheelFocusAnimation.SMOOTH) SettingSlider(R.string.wheel_focus_duration,
                    l.wheel.focusDurationMs.toFloat(), 80f..400f, t(R.string.ms_value, l.wheel.focusDurationMs), default = 140f) { value ->
                    layout { it.copy(wheel = it.wheel.copy(focusDurationMs = value.roundToInt())) }
                }
                Help(R.string.wheel_focus_animation_help)
            }
        } else {
            OptionRow(t(R.string.direction), PanelDirection.entries, l.direction, defaults.direction) { value -> layout { it.copy(direction = value) } }
            DependentSettings(R.string.direction) {
                CountStepper(if (l.direction == PanelDirection.VERTICAL) R.string.columns else R.string.rows, l.crossCount, 1, 12, 1) { value -> layout { it.copy(crossCount = value) } }
            }
        }
        if (l.mode != PanelMode.WHEEL) SettingSlider(R.string.button_size, l.buttonSizeDp, 48f..96f, t(R.string.dp_value, l.buttonSizeDp.roundToInt()), default = 56f) { value -> layout { it.copy(buttonSizeDp = value) } }
        if (l.mode != PanelMode.WHEEL && g.crossCount < l.crossCount) Notice(t(R.string.panel_screen_limit, g.crossCount))
    }
    Section(R.string.position) {
        Toggle(R.string.follow_handle, R.string.follow_handle_help, l.followHandle, default = true, optionsExpanded = true) { value -> layout { it.copy(followHandle = value) } }
        DependentSettings(R.string.follow_handle) {
            if (l.followHandle) {
                SettingSlider(R.string.offset_x, l.offsetXDp, -240f..240f, t(R.string.dp_value, l.offsetXDp.roundToInt()), default = 0f) { value -> layout { it.copy(offsetXDp = value) } }
                SettingSlider(R.string.offset_y, l.offsetYDp, -240f..240f, t(R.string.dp_value, l.offsetYDp.roundToInt()), default = 0f) { value -> layout { it.copy(offsetYDp = value) } }
                Help(R.string.offset_help)
            } else {
                PercentSlider(R.string.center_x, l.x, default = .5f) { value -> layout { it.copy(x = value) } }
                PercentSlider(R.string.center_y, l.y, default = .5f) { value -> layout { it.copy(y = value) } }
            }
        }
    }
    Section(R.string.outside_touch) {
        OptionRow(t(R.string.outside_touch), OutsideTouch.entries, p.outsideTouch, OutsideTouch.BLOCK) { value -> vm.editPanel(entry.id) { it.copy(outsideTouch = value) } }
        Help(R.string.outside_help)
    }
    Section(R.string.panel_timeout) {
        Toggle(R.string.auto_close_panel, R.string.auto_close_help, p.autoCloseEnabled, default = false, optionsExpanded = p.autoCloseEnabled) { value -> vm.editPanel(entry.id) { it.copy(autoCloseEnabled = value) } }
        if (p.autoCloseEnabled) DependentSettings(R.string.auto_close_panel) {
            DurationInput(R.string.close_after, p.autoCloseMs, 1000L..60000L, default = 3000) { value -> vm.editPanel(entry.id) { it.copy(autoCloseMs = value) } }
            Help(R.string.panel_timeout_help)
        }
    }
    Section(R.string.panel_animation) {
        if (l.mode == PanelMode.WHEEL && !l.wheel.animationsEnabled) Help(R.string.wheel_animations_help)
        else {
            OptionRow(t(R.string.open_animation), PanelAnimation.entries, p.animation, PanelAnimation.NONE) { value -> vm.editPanel(entry.id) { it.copy(animation = value) } }
            OptionRow(t(R.string.close_animation), PanelAnimation.entries, p.closeAnimation, PanelAnimation.NONE) { value -> vm.editPanel(entry.id) { it.copy(closeAnimation = value) } }
            Help(R.string.animation_help)
        }
    }
    Section(R.string.appearance) {
        ColorPicker(p.background, title = R.string.panel_background, default = panelDefaults.background) { value -> vm.editPanel(entry.id) { it.copy(background = value) } }
        SettingSlider(R.string.opacity, p.opacity, 0f..1f, t(R.string.percent, (p.opacity * 100).roundToInt()), default = .98f, valueScale = 100f) { value -> vm.editPanel(entry.id) { it.copy(opacity = value) } }
        SettingSlider(R.string.corner_radius, p.cornerDp, 0f..48f, t(R.string.dp_value, p.cornerDp.roundToInt()), default = 24f) { value -> vm.editPanel(entry.id) { it.copy(cornerDp = value) } }
        SettingSlider(R.string.icon_corners, p.iconCornerFraction, 0f.. .5f,
            t(R.string.percent, (p.iconCornerFraction * 200).roundToInt()), default = .25f, valueScale = 200f) { value -> vm.editPanel(entry.id) { it.copy(iconCornerFraction = value) } }
        Help(R.string.icon_corners_help)
        Toggle(R.string.icon_background, R.string.icon_background_help, p.iconBackground, default = true) { value -> vm.editPanel(entry.id) { it.copy(iconBackground = value) } }
        Toggle(R.string.automatic_button_color, R.string.auto_button_help, p.automaticButtonColor, default = true, optionsExpanded = !p.automaticButtonColor) { value -> vm.editPanel(entry.id) { it.copy(automaticButtonColor = value) } }
        if (!p.automaticButtonColor) DependentSettings(R.string.automatic_button_color) {
            ColorPicker(p.buttonColor, title = R.string.button_color, default = panelDefaults.buttonColor) { value -> vm.editPanel(entry.id) { it.copy(buttonColor = value) } }
        }
        PercentSlider(R.string.button_opacity, p.buttonOpacity, default = 1f) { value -> vm.editPanel(entry.id) { it.copy(buttonOpacity = value) } }
        Toggle(R.string.automatic_icon_color, R.string.auto_icon_help, p.automaticIconColor, default = true, optionsExpanded = !p.automaticIconColor) { value -> vm.editPanel(entry.id) { it.copy(automaticIconColor = value) } }
        if (!p.automaticIconColor) DependentSettings(R.string.automatic_icon_color) {
            ColorPicker(p.iconColor, title = R.string.icon_color, default = panelDefaults.iconColor) { value -> vm.editPanel(entry.id) { it.copy(iconColor = value) } }
        }
        PercentSlider(R.string.icon_opacity, p.iconOpacity, default = 1f) { value -> vm.editPanel(entry.id) { it.copy(iconOpacity = value) } }
        if (entry.isWheel) WheelHoverControls(p.wheelHover) { value -> vm.editPanel(entry.id) { it.copy(wheelHover = value) } }
        ActiveButtonControls(p) { value -> vm.editPanel(entry.id) { it.copy(activeAppearance = value) } }
        SettingTitle(t(R.string.icon_pack), reset = if (p.iconPack.isNotBlank()) ({ vm.editPanel(entry.id) { it.copy(iconPack = "") } }) else null)
        OutlinedButton(onClick = { vm.loadPacks(); packPicker = true }, modifier = Modifier.fillMaxWidth()) {
            Text(packs.firstOrNull { it.packageName == p.iconPack }?.label ?: if (p.iconPack.isBlank()) t(R.string.original_icons) else p.iconPack, Modifier.weight(1f))
            Icon(AppIcons.Down, null, Modifier.size(18.dp))
        }
        if (p.iconPack.isNotBlank() && packs.none { it.packageName == p.iconPack }) Help(R.string.pack_unavailable)
    }
    if (adding) ActionPicker({ adding = false }) { action -> adding = false; draft = PanelButton(action = action) }
    draft?.let { button -> ButtonEditor(button, p, vm, dismiss = { draft = null }) { saved ->
        vm.editPanel(entry.id) { current ->
            if (current.buttons.any { it.id == saved.id }) current.copy(buttons = current.buttons.map { if (it.id == saved.id) saved else it })
            else fitButtons(current.copy(buttons = current.buttons + saved))
        }
        vm.flush(); draft = null
    } }
    if (packPicker) PackPicker(packs, { packPicker = false }) { pack -> vm.editPanel(entry.id) { it.copy(iconPack = pack) }; packPicker = false }
}

@Composable private fun CountStepper(title: Int, value: Int, min: Int, max: Int, default: Int, change: (Int) -> Unit) {
    val text = t(title)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) { SettingTitle(text, reset = if (value != default) ({ change(default) }) else null) }
        IconButton(onClick = { change(value - 1) }, enabled = value > min) { Icon(AppIcons.Minus, t(R.string.decrease, text)) }
        Text(t(R.string.count_value, value))
        IconButton(onClick = { change(value + 1) }, enabled = value < max) { Icon(AppIcons.Add, t(R.string.increase, text)) }
    }
}


@Composable fun PanelEditor(config: AppConfiguration, panel: SavedPanel, previewHandleId: String?, vm: NudgeViewModel, preview: (String?) -> Unit, previewEnabled: Boolean, setPreview: (Boolean) -> Unit, landscape: Boolean, selectOrientation: (Boolean) -> Unit) {
    val context = LocalContext.current
    val draft by vm.draft.collectAsStateWithLifecycle()
    val saving by vm.savingEditor.collectAsStateWithLifecycle()
    val service by vm.status.collectAsStateWithLifecycle()
    var renaming by rememberSaveable(panel.id) { mutableStateOf(false) }
    var name by rememberSaveable(panel.id) { mutableStateOf("") }
    Surface(Modifier.fillMaxSize().padding(bottom = 12.dp), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column {
            EditorHeader(context.panelLabel(panel), draft?.changed == true, if (panel.isWheel) R.string.rename_wheel else R.string.rename_panel,
                rename = { name = panel.name; renaming = true }, save = vm::saveEditing, cancel = vm::cancelEditing, saving = saving) {
                Icon(if (panel.isWheel) AppIcons.Wheel else AppIcons.Apps, null, Modifier.size(28.dp))
            }
            OverlayPreviewToggle(previewEnabled, service.connected, setPreview)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                PanelEditorContent(config, panel, previewHandleId, vm, preview, previewEnabled, landscape, selectOrientation)
            }
        }
    }
    if (renaming) AlertDialog(onDismissRequest = { renaming = false }, title = { Text(t(if (panel.isWheel) R.string.rename_wheel else R.string.rename_panel)) },
        text = { OutlinedTextField(name, { name = it.take(80) }, label = { Text(t(R.string.name)) }, singleLine = true) },
        confirmButton = { TextButton(onClick = { vm.editSavedPanel(panel.id) { it.copy(name = name.trim()) }; vm.flush(); renaming = false }) { Text(t(R.string.save)) } },
        dismissButton = { TextButton(onClick = { renaming = false }) { Text(t(R.string.cancel)) } })
}

@Composable private fun PreviewHandleChoice(config: AppConfiguration, selected: String?, choose: (String?) -> Unit) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    SettingTitle(t(R.string.preview_handle))
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(config.handle(selected)?.let { context.handleLabel(config, it) } ?: t(R.string.default_preview_handle), Modifier.weight(1f))
            Icon(AppIcons.Down, null, Modifier.size(18.dp))
        }
        DropdownMenu(expanded, { expanded = false }) {
            DropdownMenuItem(text = { Text(t(R.string.default_preview_handle)) }, onClick = { choose(null); expanded = false })
            config.handles.forEach { h -> DropdownMenuItem(text = { Text(context.handleLabel(config, h)) }, onClick = { choose(h.id); expanded = false }) }
        }
    }
}
