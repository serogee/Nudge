package com.modukey.app.platform

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.modukey.app.core.BrightnessPreset
import com.modukey.app.core.BrightnessReturnSettings
import com.modukey.app.core.BrightnessSession
import com.modukey.app.core.BrightnessValue

enum class BrightnessResult { CHANGED, ALREADY_SET, NEED_ACCESS, FAILED }
data class BrightnessStatus(val maximum: Boolean = false, val minimum: Boolean = false, val automatic: Boolean = false)

/** Observe the two brightness settings only while a restore value exists. Never poll or wake the display. */
class BrightnessController(context: Context) {
    private val app = context.applicationContext
    private val resolver = app.contentResolver
    private val session = BrightnessSession()
    val calibration = BrightnessCalibration(app)
    private val calibrationRestore = BrightnessCalibrationRestore(app)
    val calibrationNeedsRecovery get() = calibrationRestore.pending
    internal fun beginCalibration(): Boolean { close(); return calibrationRestore.begin() }
    internal fun restoreAfterCalibration() = calibrationRestore.restore()
    internal fun keepBrightnessAfterCalibration() = calibrationRestore.discard()
    private var observer: ContentObserver? = null
    // SCREEN_BRIGHTNESS uses 1..255. Private hardware resources can use a different scale.
    private val minimum = 1
    private val maximum = 255
    private var writing = false
    private var lastStatus = BrightnessStatus()
    fun status(): BrightnessStatus = try {
        if (writing) lastStatus else {
            val current = read(); session.observe(current); observeIfNeeded()
            BrightnessStatus(!current.automatic && current.level == maximumLevel(), session.active(BrightnessPreset.MINIMUM, current), current.automatic).also { lastStatus = it }
        }
    } catch (_: RuntimeException) { BrightnessStatus() }
    val canWrite get() = Settings.System.canWrite(app)
    fun fraction(): Float = runCatching { calibration.scale.fraction(read().level) }.getOrDefault(.5f)
    fun calibrationSample(): Int? = runCatching { read().takeUnless { it.automatic }?.level }.getOrNull()
    private fun minimumLevel() = calibration.scale.samples?.first() ?: minimum
    private fun maximumLevel() = calibration.scale.samples?.last() ?: maximum
    fun toggleAutomatic(): BrightnessResult {
        if (calibrationNeedsRecovery) return BrightnessResult.FAILED
        if (!canWrite) return BrightnessResult.NEED_ACCESS
        return changing {
            val before = read()
            val mode = if (before.automatic) Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL else Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            // Change only mode. Android controls the actual level while automatic brightness is active.
            if (!Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, mode)) BrightnessResult.FAILED
            else if (read().automatic == before.automatic) BrightnessResult.FAILED
            else { session.clear(); observeIfNeeded(); BrightnessResult.CHANGED }
        }
    }
    fun adjust(increase: Boolean): BrightnessResult {
        if (calibrationNeedsRecovery) return BrightnessResult.FAILED
        if (!canWrite) return BrightnessResult.NEED_ACCESS
        return try {
            val before = read().level
            val target = calibration.scale.level((calibration.scale.fraction(before) + if (increase) .05f else -.05f).coerceIn(0f, 1f))
            val next = when {
                increase && target <= before && before < maximumLevel() -> before + 1
                !increase && target >= before && before > minimumLevel() -> before - 1
                else -> target
            }
            setLevel(next)
        } catch (_: RuntimeException) { BrightnessResult.FAILED }
    }
    fun setFraction(value: Float): BrightnessResult {
        if (calibrationNeedsRecovery) return BrightnessResult.FAILED
        if (!value.isFinite()) return BrightnessResult.FAILED
        if (!canWrite) return BrightnessResult.NEED_ACCESS
        return try { setLevel(calibration.scale.level(value)) }
        catch (_: RuntimeException) { BrightnessResult.FAILED }
    }
    private fun setLevel(level: Int): BrightnessResult = changing {
        val before = read(); val after = BrightnessValue(level, false)
        // Moving a slider to an endpoint is still a manual operation, even if its value is unchanged.
        session.clear()
        if (before == after) return@changing BrightnessResult.ALREADY_SET
        if (write(after) && read() == after) return@changing BrightnessResult.CHANGED
        val partial = read()
        if (partial != before && partial.level in setOf(before.level, after.level)) write(before)
        BrightnessResult.FAILED
    }

    fun toggle(preset: BrightnessPreset, settings: BrightnessReturnSettings = BrightnessReturnSettings()): BrightnessResult {
        if (calibrationNeedsRecovery) return BrightnessResult.FAILED
        if (!Settings.System.canWrite(app)) return BrightnessResult.NEED_ACCESS
        return changing {
            val change = session.change(preset, read(), minimumLevel(), maximumLevel(), settings, calibration.scale.level(settings.normalized().fraction))
            if (change.after == change.before) {
                session.applied(change)
                return@changing BrightnessResult.ALREADY_SET
            }
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
        }
    }
    private inline fun changing(block: () -> BrightnessResult): BrightnessResult {
        writing = true
        // Do not receive delayed notifications for our own writes. Every observed write is external.
        stopObserving()
        return try { block() }
        catch (_: RuntimeException) { session.clear(); BrightnessResult.FAILED }
        finally { writing = false; observeIfNeeded(); status() }
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
        if (writing) return
        if (session.tracking && observer == null) {
            val next = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    if (observer !== this || writing) return
                    // A manual write never activates a preset, including a write to the same endpoint.
                    session.clear(); observeIfNeeded()
                }
            }
            observer = next
            try {
                resolver.registerContentObserver(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS), false, next)
                resolver.registerContentObserver(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE), false, next)
            } catch (_: RuntimeException) { stopObserving(); session.clear() }
        } else if (!session.tracking) stopObserving()
    }
    private fun stopObserving() {
        val previous = observer ?: return
        observer = null
        runCatching { resolver.unregisterContentObserver(previous) }
    }
}
