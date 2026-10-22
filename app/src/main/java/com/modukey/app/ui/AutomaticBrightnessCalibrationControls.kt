package com.modukey.app.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.modukey.app.NudgeApplication
import com.modukey.app.R
import com.modukey.app.platform.*

@Composable internal fun AutomaticBrightnessCalibrationControls() {
    val context = LocalContext.current
    val app = context.applicationContext as NudgeApplication
    val coordinator = app.automaticCalibration
    val state by coordinator.state.collectAsState()
    var consent by rememberSaveable { mutableStateOf(false) }
    var canWrite by remember { mutableStateOf(app.brightness.canWrite) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) canWrite = app.brightness.canWrite }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    fun openSettings(write: Boolean = false) {
        try {
            context.startActivity(if (write) Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
                else Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (_: ActivityNotFoundException) { coordinator.settingsUnavailable() }
        catch (_: SecurityException) { coordinator.settingsUnavailable() }
    }
    if (!state.busy && state.phase !in listOf(AutomaticCalibrationPhase.REVIEW, AutomaticCalibrationPhase.RECOVERY)) {
        OutlinedButton(onClick = { consent = true }) { Text(t(R.string.automatic_brightness_calibration)) }
    }
    when (state.phase) {
        AutomaticCalibrationPhase.WAITING_SERVICE -> {
            Help(R.string.auto_calibration_enable_help)
            TextButton(onClick = { openSettings() }) { Text(t(R.string.enable_calibration_service)) }
            TextButton(onClick = { coordinator.cancel() }) { Text(t(R.string.cancel)) }
        }
        AutomaticCalibrationPhase.READY -> {
            Help(R.string.auto_calibration_ready)
            Button(onClick = coordinator::start) { Text(t(R.string.start_brightness_calibration)) }
            TextButton(onClick = { coordinator.cancel() }) { Text(t(R.string.cancel)) }
        }
        AutomaticCalibrationPhase.RUNNING -> {
            Text(t(R.string.auto_calibration_progress, state.position))
            LinearProgressIndicator(progress = { state.position / 100f }, modifier = Modifier.fillMaxWidth())
            TextButton(onClick = { coordinator.cancel() }) { Text(t(R.string.cancel)) }
        }
        AutomaticCalibrationPhase.REVIEW -> {
            Help(R.string.auto_calibration_review)
            Button(onClick = coordinator::save) { Text(t(R.string.save)) }
            TextButton(onClick = coordinator::discard) { Text(t(R.string.cancel)) }
        }
        AutomaticCalibrationPhase.SAVED -> Help(R.string.auto_calibration_saved)
        AutomaticCalibrationPhase.CANCELLED -> Help(R.string.auto_calibration_cancelled)
        AutomaticCalibrationPhase.RECOVERY -> {
            Help(R.string.auto_calibration_recovery)
            if (!canWrite) TextButton(onClick = { openSettings(true) }) { Text(t(R.string.allow_brightness_access)) }
            Button(onClick = { coordinator.recover(true) }, enabled = canWrite) { Text(t(R.string.restore_calibration_brightness)) }
            TextButton(onClick = { coordinator.recover(false) }) { Text(t(R.string.keep_calibration_brightness)) }
        }
        else -> Unit
    }
    state.error?.let { Help(calibrationError(it)) }
    if (consent) AlertDialog(onDismissRequest = { consent = false }, title = { Text(t(R.string.automatic_brightness_calibration)) }, text = {
        Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Help(R.string.auto_calibration_consent)
            Help(R.string.auto_calibration_access_disclosure)
            Help(R.string.auto_calibration_privacy)
            Help(R.string.auto_calibration_restore_disclosure)
            Help(R.string.auto_calibration_enable_help)
        }
    }, confirmButton = {
        TextButton(onClick = {
            if (!app.brightness.canWrite) openSettings(true)
            else if (coordinator.prepare()) { consent = false; openSettings() }
        }) { Text(t(if (canWrite) R.string.enable_calibration_service else R.string.allow_brightness_access)) }
    }, dismissButton = { TextButton(onClick = { consent = false }) { Text(t(R.string.cancel)) } })
}

@StringRes private fun calibrationError(error: AutomaticCalibrationError) = when (error) {
    AutomaticCalibrationError.ACCESS -> R.string.brightness_access_required
    AutomaticCalibrationError.LOCKED -> R.string.auto_calibration_locked
    AutomaticCalibrationError.SETUP_TIMEOUT -> R.string.auto_calibration_setup_timeout
    AutomaticCalibrationError.UNSUPPORTED -> R.string.auto_calibration_unsupported
    AutomaticCalibrationError.CONTROL_CHANGED -> R.string.auto_calibration_control_changed
    AutomaticCalibrationError.UNSTABLE -> R.string.auto_calibration_unstable
    AutomaticCalibrationError.INVALID_SAMPLES -> R.string.auto_calibration_invalid
    AutomaticCalibrationError.RESTORE -> R.string.auto_calibration_restore_failed
    AutomaticCalibrationError.SAVE -> R.string.auto_calibration_save_failed
    AutomaticCalibrationError.DISCONNECTED -> R.string.auto_calibration_disconnected
    AutomaticCalibrationError.SETTINGS -> R.string.auto_calibration_settings_failed
}
