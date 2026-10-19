package com.modukey.app.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.modukey.app.BuildConfig
import com.modukey.app.R
import com.modukey.app.core.*
import com.modukey.app.data.ConfigurationError
import com.modukey.app.platform.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun NudgeApp(vm: NudgeViewModel = viewModel()) {
    val config by vm.configuration.collectAsStateWithLifecycle()
    val service by vm.status.collectAsStateWithLifecycle()
    val accessibility by vm.accessibilityEnabled.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val draft by vm.draft.collectAsStateWithLifecycle()
    var pendingNavigation by remember { mutableStateOf<(() -> Unit)?>(null) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var handleId by rememberSaveable { mutableStateOf<String?>(null) }
    var panelId by key("independent-panel-navigation") { rememberSaveable { mutableStateOf<String?>(null) } }
    var previewHandleId by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val snackbar = remember { SnackbarHostState() }
    val handle = config?.handle(handleId)
    val panel = config?.panel(panelId)
    val previewHandle = config?.handle(previewHandleId)
    var overlayPreview by rememberSaveable(handleId, panelId) { mutableStateOf(true) }
    var previewLandscape by rememberSaveable(handleId, panelId) { mutableStateOf(false) }
    var handleTab by rememberSaveable(handleId) { mutableIntStateOf(0) }
    fun navigate(action: () -> Unit) {
        if (draft?.changed == true) pendingNavigation = action
        else { vm.closeEditing(); action() }
    }
    fun back() = navigate { if (tab == 2) panelId = null else handleId = null }
    BackHandler(enabled = handle != null || panel != null) { back() }
    LaunchedEffect(vm) { vm.notices.collect { snackbar.showSnackbar(context.getString(it)) } }
    DisposableEffect(lifecycle, tab, handle?.id, panel?.id, previewHandle?.id, overlayPreview, handleTab, previewLandscape) {
        val token = Any()
        fun resume() {
            vm.refreshPermissions()
            if (tab == 1 && handle != null) vm.beginEditor(token, handle.id, preview = overlayPreview && handleTab == 0, landscape = previewLandscape)
            else if (tab == 2 && panel != null) vm.beginEditor(token, previewHandle?.id, panel.id, preview = overlayPreview, landscape = previewLandscape)
        }
        val observer = LifecycleEventObserver { _, event -> when (event) {
            Lifecycle.Event.ON_RESUME -> resume()
            Lifecycle.Event.ON_PAUSE -> { vm.endEditor(token); vm.flush() }
            else -> Unit
        } }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) resume()
        onDispose { vm.endEditor(token); vm.flush(); lifecycle.removeObserver(observer) }
    }
    fun settings(action: String) {
        try { context.startActivity(Intent(action).apply { if (action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS) data = "package:${context.packageName}".toUri() }) }
        catch (_: android.content.ActivityNotFoundException) { vm.tell(R.string.action_failed) }
        catch (_: SecurityException) { vm.tell(R.string.action_failed) }
    }
    CompositionLocalProvider(LocalFinishAdjustment provides vm::flush) { NudgeTheme {
        Scaffold(topBar = {
            if (tab != 0) TopAppBar(title = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { Image(painterResource(R.drawable.ic_modukey), null, Modifier.size(32.dp)); Text(t(R.string.app_name)) } },
                navigationIcon = { if (handle != null || panel != null) EditIcon(AppIcons.Back, R.string.back, action = ::back) },
                actions = { Text(BuildConfig.VERSION_NAME, Modifier.padding(end = 16.dp), style = MaterialTheme.typography.labelSmall) })
        }, bottomBar = { NavigationBar {
            listOf(R.string.home, R.string.handles, R.string.panels, R.string.info).forEachIndexed { index, title ->
                NavigationBarItem(selected = tab == index, onClick = { navigate { tab = index; handleId = null; panelId = null; previewHandleId = null } }, label = { Text(t(title)) },
                    icon = { Icon(when(index) { 0 -> Icons.Outlined.Home; 1 -> AppIcons.Handle; 2 -> AppIcons.Apps; else -> Icons.Outlined.Info }, null) })
            }
        } }, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (error != null) {
                    Notice(t(if (error == ConfigurationError.READ) R.string.read_failed else R.string.save_failed))
                    OutlinedButton(onClick = vm::retrySaving) { Text(t(R.string.retry)) }
                }
                service.error?.let { Notice(it) }
                val c = config
                if (c == null) { if (error == null) CircularProgressIndicator() }
                else when {
                    tab == 1 && handle != null -> HandleEditor(c, handle, vm, handleTab, { handleTab = it }, overlayPreview, { overlayPreview = it }, previewLandscape) { previewLandscape = it }
                    tab == 1 -> HandlesScreen(c, vm) { handleId = it }
                    tab == 2 && panel != null -> PanelEditor(c, panel, previewHandle?.id, vm, { previewHandleId = it }, overlayPreview, { overlayPreview = it }, previewLandscape) { previewLandscape = it }
                    tab == 2 -> PanelsScreen(c, vm) { panelId = it; previewHandleId = c.handles.firstOrNull()?.id }
                    else -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (tab == 0) HomeScreen(c, vm, service, accessibility, ::settings) else SettingsScreen(c, vm, ::settings)
                    }
                }
            }
        }
        pendingNavigation?.let { destination ->
            AlertDialog(onDismissRequest = { pendingNavigation = null }, title = { Text(t(R.string.unsaved_changes)) },
                text = { Text(t(R.string.unsaved_changes_help)) },
                confirmButton = { TextButton(onClick = { vm.saveEditing(); vm.closeEditing(); pendingNavigation = null; destination() }) { Text(t(R.string.save)) } },
                dismissButton = { Row {
                    TextButton(onClick = { vm.closeEditing(); pendingNavigation = null; destination() }) { Text(t(R.string.discard)) }
                    TextButton(onClick = { pendingNavigation = null }) { Text(t(R.string.keep_editing)) }
                } })
        }
    } }
}

@Composable private fun HomeScreen(config: AppConfiguration, vm: NudgeViewModel, service: ServiceState, accessibility: Boolean, openSettings: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Image(painterResource(R.drawable.ic_modukey), null, Modifier.size(52.dp))
        Text(t(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
    }
    val policy by vm.policyAccess.collectAsStateWithLifecycle()
    val policyActions = setOf(ButtonAction.DND, ButtonAction.SILENT, ButtonAction.VIBRATE)
    val needsPolicy = config.panels.any { p -> p.settings.buttons.any { it.action in policyActions } } ||
        config.handles.any { h -> h.bindings.values.any { it.action?.action in policyActions } }
    Section(R.string.app_settings) {
        Toggle(R.string.enable_gestures, R.string.master_help, config.enabled, default = false) { value -> vm.edit { it.copy(enabled = value) }; vm.flush() }
        StatusLine(t(if (accessibility) R.string.accessibility_on else R.string.accessibility_off), accessibility)
        StatusLine(t(if (service.connected) R.string.service_connected else R.string.service_disconnected), service.connected)
        StatusLine(t(R.string.active_count, service.attachedHandleIds.size, config.handles.size), service.attachedHandleIds.isNotEmpty())
        if (!accessibility || !service.connected) {
            Help(if (!accessibility) R.string.accessibility_help else R.string.reconnect_help)
            OutlinedButton(onClick = { openSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS) }) { Text(t(R.string.open_accessibility)) }
        }
        if (needsPolicy) {
            StatusLine(t(if (policy) R.string.dnd_access_on else R.string.dnd_access_off), policy)
            if (!policy) OutlinedButton(onClick = { openSettings(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS) }) { Text(t(R.string.open_policy)) }
        }
    }
    Section(R.string.test_volume) {
        val check by vm.popupCheck.collectAsStateWithLifecycle()
        Help(R.string.volume_test_help)
        Button(onClick = vm::testVolume) { Text(t(R.string.test_volume)) }
        Text(t(check))
        if (check != R.string.volume_unchecked) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.confirmPopup(true) }) { Text(t(R.string.popup_seen)) }
            OutlinedButton(onClick = { vm.confirmPopup(false) }) { Text(t(R.string.popup_missing)) }
        }
    }
    Help(R.string.touch_help)
    HyperOsSetup(vm, openSettings)
}

@Composable private fun StatusLine(text: String, good: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(7.dp).background(if (good) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable private fun HandlesScreen(config: AppConfiguration, vm: NudgeViewModel, edit: (String) -> Unit) {
    val context = LocalContext.current
    val service by vm.status.collectAsStateWithLifecycle()
    var deleting by remember { mutableStateOf<String?>(null) }
    Text(t(R.string.manage_handles), style = MaterialTheme.typography.headlineMedium)
    Surface(Modifier.fillMaxSize().padding(bottom = 12.dp), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(t(R.string.handles), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                FilledTonalButton(onClick = { edit(vm.addHandle()) }) { Icon(AppIcons.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(t(R.string.add_handle)) }
            }
            Help(R.string.manage_handles_help)
            if (config.handles.isEmpty()) Help(R.string.no_handles)
            ReorderableList(config.handles, { it.id }, { context.handleLabel(config, it) }, Modifier.weight(1f),
                moveTo = vm::reorderHandle, finish = vm::flush) { h, _ ->
                Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            HandleBadge(h, context.handleLabel(config, h))
                            Text(context.handleLabel(config, h), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Switch(h.enabled, { value -> vm.editHandle(h.id) { it.copy(enabled = value) }; vm.flush() },
                                modifier = Modifier.semanticsDescription(context.getString(R.string.enable_handle)))
                        }
                        if (service.handles[h.id] == HandleAvailability.ERROR) Text(t(R.string.overlay_error), style = MaterialTheme.typography.bodySmall)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                            EditIcon(AppIcons.Edit, R.string.edit) { edit(h.id) }
                            EditIcon(AppIcons.Duplicate, R.string.duplicate) { vm.duplicate(h.id) }
                            EditIcon(AppIcons.Delete, R.string.delete) { deleting = h.id }
                        }
                    }
                }
            }
        }
    }
    deleting?.let { id -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text(t(R.string.delete_handle)) }, text = { Text(t(R.string.delete_handle_help)) },
        confirmButton = { TextButton(onClick = { vm.remove(id); deleting = null }) { Text(t(R.string.delete)) } },
        dismissButton = { TextButton(onClick = { deleting = null }) { Text(t(R.string.cancel)) } }) }
}
