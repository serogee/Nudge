package com.modukey.app.platform

import android.content.Context
import android.util.AtomicFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/** Permission workflow preference stays on this device, outside configuration and Android backups. */
class BrightnessCalibrationOptions(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "brightness-calibration-options"))
    private val mutable = MutableStateFlow(readEnabled())
    val keepServiceEnabled = mutable.asStateFlow()

    private fun readEnabled() = runCatching { file.openRead().use { it.read() == 1 && it.read() == -1 } }.getOrDefault(false)
    fun setKeepServiceEnabled(enabled: Boolean): Boolean {
        if (mutable.value == enabled) return true
        return runCatching {
            val stream = file.startWrite()
            try { stream.write(if (enabled) 1 else 0); file.finishWrite(stream) }
            catch (error: Exception) { file.failWrite(stream); throw error }
            check(readEnabled() == enabled)
            mutable.value = enabled
            true
        }.getOrDefault(false)
    }
}
