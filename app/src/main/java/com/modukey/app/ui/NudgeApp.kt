package com.modukey.app.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
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
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var handleId by rememberSaveable { mutableStateOf<String?>(null) }
    var panel by rememberSaveable { mutableStateOf<Int?>(null) }
    val context = LocalContext.current; val lifecycle = LocalLifecycleOwner.current.lifecycle
    val snackbar = remember { SnackbarHostState() }
    val handle = config?.handle(handleId)
    fun back() { if (panel != null) panel = null else handleId = null }
    BackHandler(enabled = handle != null) { back() }
    LaunchedEffect(vm) { vm.notices.collect { snackbar.showSnackbar(context.getString(it)) } }
    DisposableEffect(lifecycle, tab, handleId, panel) {
        val token = Any()
        fun resume() { vm.refreshPermissions(); if (tab == 1 && handleId != null) vm.beginEditor(token, handleId!!, panel) }
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
    }
    CompositionLocalProvider(LocalFinishAdjustment provides vm::flush) { NudgeTheme {
        Scaffold(topBar = { TopAppBar(title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Image(painterResource(R.drawable.ic_modukey), null, Modifier.size(36.dp)); Text(t(R.string.app_name))
            }
        }, navigationIcon = { if (handle != null) TextButton(onClick = ::back) { Text(t(R.string.back)) } },
            actions = { Text(BuildConfig.VERSION_NAME, Modifier.padding(end = 12.dp), style = MaterialTheme.typography.labelSmall) }) },
            bottomBar = { NavigationBar { listOf(R.string.home, R.string.handles, R.string.settings).forEachIndexed { index, title ->
                NavigationBarItem(selected = tab == index, onClick = { tab = index; handleId = null; panel = null }, label = { Text(t(title)) },
                    icon = { Icon(when(index) { 0 -> Icons.Outlined.Home; 1 -> Icons.Outlined.Settings; else -> Icons.Outlined.Info }, null) })
            } } }, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (error != null) {
                    Notice(t(if (error == ConfigurationError.READ) R.string.read_failed else R.string.save_failed))
                    OutlinedButton(onClick = vm::retrySaving) { Text(t(R.string.retry)) }
                }
                service.error?.let { Notice(it) }
                val c = config
                if (c == null) { if (error == null) CircularProgressIndicator() }
                else when {
                    tab == 0 -> HomeScreen(c, vm, service, accessibility, ::settings)
                    tab == 1 && handle != null && panel != null -> PanelEditor(c, handle, panel!!, vm)
                    tab == 1 && handle != null -> HandleEditor(c, handle, vm) { panel = it }
                    tab == 1 -> HandlesScreen(c, vm) { handleId = it; panel = null }
                    else -> SettingsScreen(c, vm, ::settings)
                }
            }
        }
    } }
}

@Composable private fun HomeScreen(config: AppConfiguration, vm: NudgeViewModel, service: ServiceState, accessibility: Boolean, openSettings: (String) -> Unit) {
    Section(R.string.handles) {
        Toggle(R.string.enable_nudge, R.string.master_help, config.enabled) { vm.edit { c -> c.copy(enabled = it) }; vm.flush() }
        Text(t(R.string.active_count, service.attachedHandleIds.size, config.handles.size))
    }
    Section(R.string.connection) {
        Text(t(when(service.connection(accessibility)) { ConnectionState.CONNECTED -> R.string.connected; ConnectionState.DISABLED -> R.string.disabled; ConnectionState.ENABLED_DISCONNECTED -> R.string.disconnected }))
        if (!accessibility || !service.connected) {
            Help(if (!accessibility) R.string.accessibility_help else R.string.reconnect_help)
            Button(onClick = { openSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS) }) { Text(t(R.string.open_accessibility)) }
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
    Section(R.string.hyperos_setup) { Help(R.string.autostart_note) }
}

@Composable private fun HandlesScreen(config: AppConfiguration, vm: NudgeViewModel, edit: (String) -> Unit) {
    val context = LocalContext.current; val service by vm.status.collectAsStateWithLifecycle()
    var deleting by remember { mutableStateOf<String?>(null) }
    Button(onClick = { edit(vm.addHandle()) }) { Text(t(R.string.add_handle)) }
    Help(R.string.order_help)
    if (config.handles.isEmpty()) Help(R.string.no_handles)
    config.handles.forEachIndexed { index, h -> key(h.id) {
        Section(context.handleLabel(config, h)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(t(label(h.trigger.portrait.edge)))
                Switch(checked = h.enabled, onCheckedChange = { value -> vm.editHandle(h.id) { it.copy(enabled = value) }; vm.flush() })
            }
            Text(t(when(service.handles[h.id]) {
                HandleAvailability.ACTIVE -> R.string.active; HandleAvailability.DISABLED -> R.string.disabled; HandleAvailability.PAUSED -> R.string.paused
                HandleAvailability.SCREEN_OFF -> R.string.hidden_screen_off; HandleAvailability.LOCKED -> R.string.hidden_locked; HandleAvailability.LANDSCAPE -> R.string.hidden_landscape
                HandleAvailability.EXCLUDED -> R.string.hidden_excluded; HandleAvailability.EDITING -> R.string.editing; HandleAvailability.ERROR -> R.string.overlay_error
                else -> if (h.enabled) R.string.pending else R.string.disabled
            }), style = MaterialTheme.typography.bodySmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { edit(h.id) }) { Text(t(R.string.edit)) }
                TextButton(onClick = { vm.duplicate(h.id) }) { Text(t(R.string.duplicate)) }
                TextButton(onClick = { vm.moveHandle(h.id, -1) }, enabled = index > 0) { Text(t(R.string.move_up)) }
                TextButton(onClick = { vm.moveHandle(h.id, 1) }, enabled = index < config.handles.lastIndex) { Text(t(R.string.move_down)) }
                TextButton(onClick = { deleting = h.id }) { Text(t(R.string.delete)) }
            }
        }
    } }
    deleting?.let { id -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text(t(R.string.delete_handle)) }, text = { Text(t(R.string.delete_handle_help)) },
        confirmButton = { TextButton(onClick = { vm.remove(id); deleting = null }) { Text(t(R.string.delete)) } },
        dismissButton = { TextButton(onClick = { deleting = null }) { Text(t(R.string.cancel)) } }) }
}
