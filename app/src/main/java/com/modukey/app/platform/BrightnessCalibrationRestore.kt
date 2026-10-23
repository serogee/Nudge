package com.modukey.app.platform

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File

/** The journal contains only the original brightness and mode, never window content. */
internal class BrightnessCalibrationRestore(private val context: Context) {
    private val resolver = context.contentResolver
    private val journal = AtomicFile(File(context.noBackupFilesDir, "brightness-calibration-restore"))
    val pending get() = journal.baseFile.exists() || File(journal.baseFile.path + ".bak").exists()
    fun begin(): Boolean {
        if (pending || !Settings.System.canWrite(context)) return false
        return runCatching {
            val level = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS)
            val mode = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE)
            require(level in 0..65535 && mode in 0..1)
            val record = JSONObject().put("fingerprint", Build.FINGERPRINT).put("level", level).put("mode", mode)
            val stream = journal.startWrite()
            try { stream.write(record.toString().toByteArray(Charsets.UTF_8)); journal.finishWrite(stream) }
            catch (error: Exception) { journal.failWrite(stream); throw error }
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, 0) &&
                Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE) == 0
        }.getOrDefault(false)
    }
    fun restore(): Boolean {
        if (!pending) return true
        if (!Settings.System.canWrite(context)) return false
        return runCatching {
            val record = JSONObject(journal.openRead().use { stream ->
                val bytes = stream.readBytes(); require(bytes.size <= 2048); bytes.toString(Charsets.UTF_8)
            })
            require(record.getString("fingerprint") == Build.FINGERPRINT)
            val level = record.getInt("level"); val mode = record.getInt("mode")
            require(level in 0..65535 && mode in 0..1)
            // Verify the saved level before automatic mode can start changing it again.
            if (!Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, 0) ||
                !Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, level) ||
                Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS) != level ||
                !Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, mode) ||
                Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE) != mode) return false
            discard()
        }.getOrDefault(false)
    }
    fun discard(): Boolean = runCatching { journal.delete(); !pending }.getOrDefault(false)
}
