package com.modukey.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.modukey.app.NudgeApplication
import com.modukey.app.R

@Composable fun BrightnessCalibrationSection() {
    val app = LocalContext.current.applicationContext as NudgeApplication
    val controller = app.brightness
    var open by rememberSaveable { mutableStateOf(false) }
    var samples by rememberSaveable { mutableStateOf(listOf<Int>()) }
    var error by rememberSaveable { mutableStateOf(false) }
    var calibrated by remember { mutableStateOf(controller.calibration.calibrated) }
    Section(R.string.brightness_calibration) {
        Help(if (calibrated) R.string.brightness_calibrated else R.string.brightness_calibration_help)
        OutlinedButton(onClick = { samples = emptyList(); error = false; open = true }) { Text(t(R.string.calibrate_brightness)) }
        if (calibrated) TextButton(onClick = {
            error = !controller.calibration.reset(); calibrated = controller.calibration.calibrated
        }) { Text(t(R.string.reset_brightness_calibration)) }
        if (error && !open) Help(R.string.brightness_calibration_failed)
    }
    if (open) AlertDialog(onDismissRequest = { open = false }, title = { Text(t(R.string.brightness_calibration)) }, text = {
        Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Help(R.string.brightness_calibration_steps)
            Text(t(R.string.brightness_calibration_position, samples.size * 25))
            if (error) Help(R.string.brightness_calibration_failed)
        }
    }, confirmButton = {
        TextButton(onClick = {
            val value = controller.calibrationSample()
            if (value == null || value !in 0..65535 || samples.lastOrNull()?.let { value <= it } == true) error = true
            else {
                val next = samples + value
                if (next.size == 5) {
                    error = !controller.calibration.save(next)
                    if (!error) { calibrated = true; open = false }
                } else { samples = next; error = false }
            }
        }) { Text(t(R.string.capture_brightness)) }
    }, dismissButton = { TextButton(onClick = { open = false }) { Text(t(R.string.cancel)) } })
}
