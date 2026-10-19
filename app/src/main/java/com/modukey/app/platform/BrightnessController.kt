package com.modukey.app.platform

import android.annotation.SuppressLint
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.modukey.app.core.BrightnessPreset
import com.modukey.app.core.BrightnessSession
import com.modukey.app.core.BrightnessValue

enum class BrightnessResult { CHANGED, ALREADY_SET, NEED_ACCESS, FAILED }
data class BrightnessStatus(val maximum: Boolean = false, val minimum: Boolean = false)

/** Observe the two brightness settings only while a restore value exists. Never poll or wake the display. */
class BrightnessController(context: Context) {
    private val app = context.applicationContext
    private val resolver = app.contentResolver
    private val session = BrightnessSession()
    private var observing = false
    private val minimum by lazy { resourceBound("config_screenBrightnessSettingMinimum", 1).coerceAtLeast(1) }
    private val maximum by lazy { resourceBound("config_screenBrightnessSettingMaximum", 255).coerceAtLeast(minimum) }
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            runCatching { session.observe(read()) }.onFailure { session.clear() }
            observeIfNeeded()
        }
    }
    fun status(): BrightnessStatus = try {
        val current = read(); session.observe(current); observeIfNeeded()
        BrightnessStatus(!current.automatic && current.level == maximum, !current.automatic && current.level == minimum)
    } catch (_: RuntimeException) { BrightnessStatus() }

    fun toggle(preset: BrightnessPreset): BrightnessResult {
        if (!Settings.System.canWrite(app)) return BrightnessResult.NEED_ACCESS
        return try {
            val change = session.change(preset, read(), minimum, maximum) ?: return BrightnessResult.ALREADY_SET
            if (!write(change.after)) {
                // Undo a partial write only while its values still match this action.
                val partial = read()
                if (partial != change.before && partial.level in setOf(change.before.level, change.after.level)) write(change.before)
                session.clear(); observeIfNeeded(); BrightnessResult.FAILED
            } else {
                val actual = read()
                if (actual != change.after) { session.clear(); observeIfNeeded(); BrightnessResult.FAILED }
                else { session.applied(change); observeIfNeeded(); BrightnessResult.CHANGED }
            }
        } catch (_: RuntimeException) { session.clear(); observeIfNeeded(); BrightnessResult.FAILED }
    }
    fun close() { session.clear(); observeIfNeeded() }
    private fun read() = BrightnessValue(Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS, 128),
        Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)
    private fun write(value: BrightnessValue): Boolean =
        if (value.automatic) {
            val levelWritten = Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, value.level)
            val modeWritten = Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)
            levelWritten && modeWritten
        } else Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL) &&
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, value.level)

    private fun observeIfNeeded() {
        if (session.tracking && !observing) {
            try {
                resolver.registerContentObserver(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS), false, observer)
                resolver.registerContentObserver(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE), false, observer)
                observing = true
            } catch (_: RuntimeException) { runCatching { resolver.unregisterContentObserver(observer) }; session.clear() }
        } else if (!session.tracking && observing) { observing = false; runCatching { resolver.unregisterContentObserver(observer) } }
    }
    @SuppressLint("DiscouragedApi")
    private fun resourceBound(name: String, fallback: Int): Int = runCatching {
        val id = app.resources.getIdentifier(name, "integer", "android")
        if (id == 0) fallback else app.resources.getInteger(id).takeIf { it in 0..65_535 } ?: fallback
    }.getOrDefault(fallback)
}
