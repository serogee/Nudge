package com.modukey.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import androidx.compose.runtime.saveable.rememberSaveable
import com.modukey.app.platform.HyperOsSettings
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.modukey.app.BuildConfig
import com.modukey.app.R
import com.modukey.app.core.AppConfiguration
import com.modukey.app.updates.*

@Composable fun SettingsScreen(config:AppConfiguration,vm:NudgeViewModel,openSettings:(String)->Unit) {
    val context=LocalContext.current
    val busy by vm.backupBusy.collectAsStateWithLifecycle(); val pending by vm.importPreview.collectAsStateWithLifecycle()
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { it?.let(vm::export) }
    val importLauncher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::readImport) }
    Section(R.string.backup) {
        Help(R.string.backup_help)
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick={ export.launch("Nudge-configuration.json") },enabled=!busy) { Text(t(R.string.export_config)) }
            OutlinedButton(onClick={ importLauncher.launch(arrayOf("application/json","text/plain")) },enabled=!busy) { Text(t(R.string.import_config)) }
        }
        if(busy) CircularProgressIndicator()
    }
    HyperOsSetup(vm, openSettings)
    Section(R.string.wheel_help_title) {
        Help(R.string.wheel_help_setup)
        Help(R.string.wheel_autofit_help)
        Help(R.string.wheel_autofit_priority)
        Help(R.string.wheel_radius_help)
        Help(R.string.wheel_gap_help)
        Help(R.string.wheel_expand_help)
        Help(R.string.wheel_animations_help)
        Help(R.string.wheel_drag_help)
        Help(R.string.wheel_distance_help)
        Help(R.string.wheel_release_help)
    }
    Section(R.string.dnd) {
        Help(R.string.policy_required); Help(R.string.dnd_other_rule)
        OutlinedButton(onClick={ openSettings(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS) }) { Text(t(R.string.open_policy)) }
    }
    Section(R.string.battery_use) { Help(R.string.battery_help) }
    Section(R.string.updates) { UpdateControls(config,vm) }
    Section(R.string.diagnostics) {
        Text(t(R.string.device_version,android.os.Build.MANUFACTURER,android.os.Build.MODEL,android.os.Build.VERSION.RELEASE,BuildConfig.VERSION_NAME))
        OutlinedButton(onClick={ vm.inspectDiagnostics { text ->
            context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(context.getString(R.string.diagnostics),text)); vm.tell(R.string.diagnostics_copied)
        } }) { Text(t(R.string.copy_diagnostics)) }
    }
    pending?.let { imported ->
        var replace by remember(imported) { mutableStateOf(false) }
        AlertDialog(onDismissRequest={ vm.importPreview.value=null },title={ Text(t(R.string.import_config)) },text={ Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(t(R.string.import_summary,imported.handles.size,imported.panels.size,imported.panels.sumOf { it.settings.buttons.size }))
            Text(t(R.string.import_shortcut_summary, imported.shortcuts.size))
            Help(R.string.import_shortcut_help)
            imported.handles.forEach { Text(context.handleLabel(imported,it)) }
            imported.panels.forEach { Text(context.panelLabel(it)) }
            Row { RadioButton(!replace,{ replace=false }); TextButton(onClick={ replace=false }) { Text(t(R.string.import_add)) } }
            Help(R.string.import_add_help)
            Row { RadioButton(replace,{ replace=true }); TextButton(onClick={ replace=true }) { Text(t(R.string.import_replace)) } }
            Help(R.string.import_replace_help)
        } },confirmButton={ TextButton(onClick={ vm.applyImport(replace) }) { Text(t(R.string.confirm_import)) } },dismissButton={ TextButton(onClick={ vm.importPreview.value=null }) { Text(t(R.string.cancel)) } })
    }
}

@Composable private fun UpdateControls(config:AppConfiguration,vm:NudgeViewModel) {
    val context=LocalContext.current; val state by vm.updates.collectAsStateWithLifecycle()
    val download by vm.apkDownload.collectAsStateWithLifecycle()
    fun open(url:String) { try { context.startActivity(Intent(Intent.ACTION_VIEW,url.toUri()).addCategory(Intent.CATEGORY_BROWSABLE)) } catch (_:android.content.ActivityNotFoundException) { vm.tell(R.string.no_browser) } }
    Toggle(R.string.include_prereleases,R.string.prerelease_help,config.includePrereleases,default=false,change=vm::setIncludePrereleases)
    OutlinedButton(onClick=vm::checkForUpdates,enabled=state!=UpdateState.Checking) { Text(t(if(state==UpdateState.Checking) R.string.checking else R.string.check_updates)) }
    when(val s=state) {
        UpdateState.Idle -> Help(R.string.update_idle)
        UpdateState.Checking -> CircularProgressIndicator()
        is UpdateState.NoRelease -> { Help(if(s.includePrereleases) R.string.update_no_release_testing else R.string.update_no_release); OutlinedButton(onClick={ open(GitHubReleaseSource.RELEASES_PAGE) }) { Text(t(R.string.view_releases)) } }
        is UpdateState.Failed -> {
            Help(when(s.reason) {
                UpdateFailure.RATE_LIMIT -> R.string.update_rate_limit; UpdateFailure.BLOCKED -> R.string.update_blocked
                UpdateFailure.NOT_FOUND -> R.string.update_not_found; UpdateFailure.UNAVAILABLE -> R.string.update_unavailable
                UpdateFailure.INVALID_RESPONSE -> R.string.update_invalid; UpdateFailure.TOO_LARGE -> R.string.update_too_large
                UpdateFailure.VERSION -> R.string.update_version; else -> R.string.update_failed
            })
            OutlinedButton(onClick={ open(GitHubReleaseSource.RELEASES_PAGE) }) { Text(t(R.string.view_releases)) }
        }
        is UpdateState.Result -> {
            Text(t(if(s.comparison>0) R.string.update_available else if(s.comparison==0) R.string.update_current else R.string.update_newer,s.release.tag))
            if(s.release.prerelease) Help(R.string.pre_release)
            if(s.comparison>0) {
                if(BuildConfig.DEBUG) Help(R.string.debug_install)
                s.release.apkUrl?.let { url ->
                    if (download.target?.url != url || download == ApkDownloadState.Idle) {
                        Button(onClick={ vm.downloadApk(s.release) }, enabled=download !is ApkDownloadState.Active) { Text(t(R.string.download_apk)) }
                    }
                }
                    ?: Help(R.string.update_apk_missing)
            }
            OutlinedButton(onClick={ open(s.release.pageUrl) }) { Text(t(R.string.view_notes)) }
        }
    }
    ApkDownloadControls(vm)
}


@Composable fun HyperOsSetup(vm: NudgeViewModel, openSettings: (String) -> Unit) {
    val context = LocalContext.current
    var open by rememberSaveable { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth().clickable { open = !open }, verticalAlignment = Alignment.CenterVertically) {
                Text(t(R.string.hyperos_setup), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { open = !open }) {
                    Icon(AppIcons.Down, t(if (open) R.string.collapse_setup else R.string.expand_setup), Modifier.rotate(if (open) 180f else 0f))
                }
            }
            Help(R.string.autostart_note)
            if (open) DependentSettings(R.string.hyperos_setup) {
                Text(t(R.string.open_accessibility), style = MaterialTheme.typography.titleSmall)
                Help(R.string.setup_accessibility)
                OutlinedButton(onClick = { openSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS) }) { Text(t(R.string.open_accessibility)); Spacer(Modifier.width(8.dp)); Icon(AppIcons.Open, null, Modifier.size(16.dp)) }
                Text(t(R.string.restricted_settings), style = MaterialTheme.typography.titleSmall)
                Help(R.string.setup_restricted)
                OutlinedButton(onClick = { openSettings(Settings.ACTION_APPLICATION_DETAILS_SETTINGS) }) { Text(t(R.string.app_info)) }
                Text(t(R.string.autostart), style = MaterialTheme.typography.titleSmall)
                Help(R.string.setup_autostart)
                OutlinedButton(onClick = { if (!HyperOsSettings.openAutostart(context)) vm.tell(R.string.autostart_unavailable) }) {
                    Text(t(R.string.open_autostart)); Spacer(Modifier.width(8.dp)); Icon(AppIcons.Open, null, Modifier.size(16.dp))
                }
                Text(t(R.string.battery_recovery), style = MaterialTheme.typography.titleSmall)
                Help(R.string.setup_battery); Help(R.string.setup_recents); Help(R.string.setup_recovery)
            }
        }
    }
}
