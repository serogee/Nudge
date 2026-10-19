package com.modukey.app.platform

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.modukey.app.core.BrightnessPreset
import com.modukey.app.core.BrightnessSession
import com.modukey.app.core.BrightnessValue
import kotlin.math.roundToInt

enum class BrightnessResult { CHANGED, ALREADY_SET, NEED_ACCESS, FAILED }
data class BrightnessStatus(val maximum: Boolean = false, val minimum: Boolean = false, val automatic: Boolean = false)

/** Observe the two brightness settings only while a restore value exists. Never poll or wake the display. */
class BrightnessController(context: Context) {
    private val app = context.applicationContext
    private val resolver = app.contentResolver
    private val session = BrightnessSession()
    private var observing = false
    // SCREEN_BRIGHTNESS uses 1..255. Private hardware resources can use a different scale.
    private val minimum = 1
    private val maximum = 255
    private var writing = false
    private var lastStatus = BrightnessStatus()
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            if (writing) return
            runCatching { session.observe(read()) }.onFailure { session.clear() }
            observeIfNeeded()
        }
    }
    fun status(): BrightnessStatus = try {
        if (writing) lastStatus else {
            val current = read(); session.observe(current); observeIfNeeded()
            BrightnessStatus(session.active(BrightnessPreset.MAXIMUM, current), session.active(BrightnessPreset.MINIMUM, current), current.automatic).also { lastStatus = it }
        }
    } catch (_: RuntimeException) { BrightnessStatus() }
    val canWrite get() = Settings.System.canWrite(app)
    fun fraction(): Float = runCatching { ((read().level - minimum).toFloat() / (maximum - minimum).coerceAtLeast(1)).coerceIn(0f, 1f) }.getOrDefault(.5f)
    fun toggleAutomatic(): BrightnessResult {
        if (!canWrite) return BrightnessResult.NEED_ACCESS
        return try {
            val before = read()
            val mode = if (before.automatic) Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL else Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            // Change only mode. Android controls the actual level while automatic brightness is active.
            if (!Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, mode)) BrightnessResult.FAILED
            else if (read().automatic == before.automatic) BrightnessResult.FAILED
            else { session.clear(); observeIfNeeded(); BrightnessResult.CHANGED }
        } catch (_: RuntimeException) { BrightnessResult.FAILED }
    }
    fun adjust(increase: Boolean): BrightnessResult {
        if (!canWrite) return BrightnessResult.NEED_ACCESS
        return try {
            val step = ((maximum - minimum) * .05f).roundToInt().coerceAtLeast(1)
            setLevel((read().level + if (increase) step else -step).coerceIn(minimum, maximum))
        } catch (_: RuntimeException) { BrightnessResult.FAILED }
    }
    fun setFraction(value: Float): BrightnessResult {
        if (!value.isFinite()) return BrightnessResult.FAILED
        if (!canWrite) return BrightnessResult.NEED_ACCESS
        return try { setLevel(minimum + ((maximum - minimum) * value.coerceIn(0f, 1f)).roundToInt()) }
        catch (_: RuntimeException) { BrightnessResult.FAILED }
    }
    private fun setLevel(level: Int): BrightnessResult {
        val before = read(); val after = BrightnessValue(level, false)
        if (before == after) return BrightnessResult.ALREADY_SET
        session.clear(); observeIfNeeded()
        if (write(after) && read() == after) return BrightnessResult.CHANGED
        val partial = read()
        if (partial != before && partial.level in setOf(before.level, after.level)) write(before)
        return BrightnessResult.FAILED
    }

    fun toggle(preset: BrightnessPreset): BrightnessResult {
        if (!Settings.System.canWrite(app)) return BrightnessResult.NEED_ACCESS
        writing = true
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
        finally { writing = false; status() }
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
}
