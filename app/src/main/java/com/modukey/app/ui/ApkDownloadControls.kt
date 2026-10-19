package com.modukey.app.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.modukey.app.R
import com.modukey.app.updates.ApkDownloadFailure
import com.modukey.app.updates.ApkDownloadPhase
import com.modukey.app.updates.ApkDownloadState

@Composable internal fun ApkDownloadControls(vm: NudgeViewModel) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val state by vm.apkDownload.collectAsStateWithLifecycle()
    val preparing by vm.preparingUpdate.collectAsStateWithLifecycle()
    var permissionTarget by rememberSaveable { mutableStateOf<String?>(null) }
    val installer = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { /* Android reports installation results. */ }
    fun launchInstaller(intent: Intent) {
        try { installer.launch(intent) }
        catch (_: ActivityNotFoundException) { vm.tell(R.string.apk_install_failed) }
        catch (_: SecurityException) { vm.tell(R.string.apk_install_failed) }
    }
    val sourceAccess = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val expected = permissionTarget
        permissionTarget = null
        if (expected != null && context.packageManager.canRequestPackageInstalls()) {
            vm.installUpdate(expected, ::launchInstaller)
        } else if (expected != null) vm.tell(R.string.apk_install_access_required)
    }
    LaunchedEffect(lifecycle, vm) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { vm.observeApkDownload() }
    }
    if (state == ApkDownloadState.Idle) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.target?.let { Text(t(R.string.apk_download_title, it.versionName)) }
        when (val download = state) {
            ApkDownloadState.Idle -> Unit
            is ApkDownloadState.Active -> {
                Help(when (download.phase) {
                    ApkDownloadPhase.QUEUED -> R.string.apk_download_queued
                    ApkDownloadPhase.DOWNLOADING -> R.string.apk_download_running
                    ApkDownloadPhase.PAUSED -> R.string.apk_download_paused
                    ApkDownloadPhase.VERIFYING -> R.string.apk_download_verifying
                })
                if (download.total > 0 && download.phase != ApkDownloadPhase.VERIFYING) {
                    val progress = (download.bytes.toDouble() / download.total).coerceIn(0.0, 1.0).toFloat()
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    Text(t(R.string.apk_download_percent, (progress * 100).toInt()))
                } else LinearProgressIndicator(Modifier.fillMaxWidth())
                OutlinedButton(onClick = vm::cancelApkDownload, enabled = !preparing) { Text(t(R.string.cancel)) }
            }
            is ApkDownloadState.Ready -> {
                Help(R.string.apk_download_ready)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = !preparing && permissionTarget == null, onClick = {
                        if (context.packageManager.canRequestPackageInstalls()) vm.installUpdate(download.target.url, ::launchInstaller)
                        else {
                            permissionTarget = download.target.url
                            try { sourceAccess.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${context.packageName}".toUri())) }
                            catch (_: ActivityNotFoundException) { permissionTarget = null; vm.tell(R.string.apk_install_access_required) }
                            catch (_: SecurityException) { permissionTarget = null; vm.tell(R.string.apk_install_access_required) }
                        }
                    }) { Text(t(if (preparing) R.string.apk_download_verifying else R.string.apk_install)) }
                    OutlinedButton(onClick = vm::cancelApkDownload, enabled = !preparing) { Text(t(R.string.apk_remove_download)) }
                }
            }
            is ApkDownloadState.Failed -> {
                Help(when (download.reason) {
                    ApkDownloadFailure.UNAVAILABLE -> R.string.apk_download_unavailable
                    ApkDownloadFailure.STORAGE -> R.string.apk_download_storage
                    ApkDownloadFailure.NETWORK -> R.string.apk_download_network
                    ApkDownloadFailure.MISSING -> R.string.apk_download_missing
                    ApkDownloadFailure.INVALID_APK -> R.string.apk_download_invalid
                    ApkDownloadFailure.PACKAGE -> R.string.apk_download_package
                    ApkDownloadFailure.VERSION -> R.string.apk_download_version
                    ApkDownloadFailure.SIGNATURE -> R.string.apk_download_signature
                    ApkDownloadFailure.DEBUG_BUILD -> R.string.apk_download_debug
                    ApkDownloadFailure.UNKNOWN -> R.string.apk_download_failed
                })
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (download.target?.valid() == true && download.reason != ApkDownloadFailure.DEBUG_BUILD) {
                        OutlinedButton(onClick = vm::retryApkDownload) { Text(t(R.string.retry)) }
                    }
                    OutlinedButton(onClick = vm::cancelApkDownload) { Text(t(R.string.apk_remove_download)) }
                }
            }
        }
    }
}
