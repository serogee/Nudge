package com.modukey.app.platform

import android.content.Context
import android.os.Build
import com.modukey.app.core.BrightnessScale
import java.io.File

/** Device-specific measurements never enter configuration backups or Android backup. */
class BrightnessCalibration(context: Context) {
    private val file = File(context.noBackupFilesDir, "brightness-calibration")
    private val identity = Build.FINGERPRINT
    var scale: BrightnessScale = read(); private set
    val calibrated get() = scale.samples != null
    fun save(samples: List<Int>): Boolean {
        if (!BrightnessScale.validSamples(samples)) return false
        return runCatching {
            val next = BrightnessScale(samples)
            android.util.AtomicFile(file).let { target ->
                val stream = target.startWrite()
                try { stream.write((identity + "\n" + samples.joinToString(",")).toByteArray()); target.finishWrite(stream) }
                catch (error: Exception) { target.failWrite(stream); throw error }
            }
            scale = next; true
        }.getOrDefault(false)
    }
    fun reset(): Boolean = runCatching {
        if (file.exists() && !file.delete()) return false
        scale = BrightnessScale(); true
    }.getOrDefault(false)
    private fun read(): BrightnessScale = runCatching {
        val lines = file.readLines()
        require(lines.size == 2 && lines[0] == identity)
        BrightnessScale(lines[1].split(',').map(String::toInt))
    }.getOrDefault(BrightnessScale())
}
