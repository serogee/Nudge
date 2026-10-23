package com.modukey.app.platform

import android.app.KeyguardManager
import android.content.Intent
import android.media.AudioManager
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.modukey.app.NudgeApplication
import com.modukey.app.core.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36], shadows = [BrightnessControllerTest.Writes::class])
@Suppress("DEPRECATION")
class BrightnessGestureRuntimeTest {
    private val app get() = ApplicationProvider.getApplicationContext<NudgeApplication>()
    private var destroy: () -> Unit = {}
    @After fun cleanup() { destroy(); BrightnessControllerTest.Writes.allowed = false; app.onTerminate() }
    private fun await(condition: () -> Boolean) {
        repeat(300) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(16)); if (condition()) return; Thread.sleep(5) }
        assertTrue(app.diagnostics.details.value, condition())
    }
    private fun fixture(lock: Boolean = false): Pair<EdgeAccessibilityService, HandleConfig> {
        val controller = Robolectric.buildService(EdgeAccessibilityService::class.java).create(); destroy = { controller.destroy() }
        val service = controller.get()
        shadowOf(app).grantPermissions("${app.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
        shadowOf(app.getSystemService(PowerManager::class.java)).setIsInteractive(true)
        shadowOf(app.getSystemService(KeyguardManager::class.java)).setKeyguardLocked(false)
        await { app.configuration.state.value != null }
        val handle = HandleConfig(enabled = true, trigger = TriggerSettings(landscapeEnabled = true, lockScreenEnabled = lock),
            bindings = mapOf(GestureSlot.TAP to GestureTarget(volume = true, brightness = true)))
        app.configuration.update { AppConfiguration(enabled = true, handles = listOf(handle)) }
        BrightnessControllerTest.Writes.allowed = true
        Settings.System.putInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 100)
        Settings.System.putInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, 1)
        ReflectionHelpers.callInstanceMethod<Void>(service, "onServiceConnected")
        return service to handle
    }
    private fun host(service: EdgeAccessibilityService) = ReflectionHelpers.getField<BrightnessPopupHost>(service, "brightnessHost")
    private fun tap(service: EdgeAccessibilityService, id: String) = ReflectionHelpers.callInstanceMethod<Void>(service, "activate",
        ClassParameter.from(String::class.java, id), ClassParameter.from(GestureKind::class.java, GestureKind.TAP))
    @Test fun bothPopupsOpenWithoutChangingLevelsAndDisabledSourceClosesBrightness() {
        val (service, handle) = fixture(); val audio = app.getSystemService(AudioManager::class.java)
        val volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        tap(service, handle.id); assertTrue(host(service).visible)
        assertEquals(volume, audio.getStreamVolume(AudioManager.STREAM_MUSIC))
        assertEquals(100, Settings.System.getInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS))
        assertEquals(1, Settings.System.getInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE))
        app.configuration.update { it.editHandle(handle.id) { h -> h.copy(enabled = false) } }
        await { !host(service).visible }
    }
    @Test fun homePreviewWorksWithMasterOffAndEndsOnScreenOffOrOwnerEnd() {
        val (service, _) = fixture()
        app.configuration.update { it.copy(enabled = false) }
        val token = Any(); app.brightnessPreview.begin(token); await { host(service).visible }
        app.brightnessPreview.end(token); await { !host(service).visible }
        app.brightnessPreview.begin(token); await { host(service).visible }
        shadowOf(app.getSystemService(PowerManager::class.java)).setIsInteractive(false)
        app.sendBroadcast(Intent(Intent.ACTION_SCREEN_OFF)); await { !host(service).visible }
        assertEquals(100, Settings.System.getInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS))
    }
    @Test fun lockedUseFollowsHandleAvailabilityAndNeverUnlocksOrWakes() {
        val (service, handle) = fixture(lock = true)
        val keyguard = app.getSystemService(KeyguardManager::class.java)
        shadowOf(keyguard).setKeyguardLocked(true)
        tap(service, handle.id); assertTrue(host(service).visible); assertTrue(keyguard.isKeyguardLocked)
        app.configuration.update { it.editHandle(handle.id) { h -> h.copy(trigger = h.trigger.copy(lockScreenEnabled = false)) } }
        await { !host(service).visible }
        tap(service, handle.id); assertFalse(host(service).visible)
    }
    @Test fun savedAutomaticCalibrationRefreshesTheOpenHomePreview() {
        val (service, _) = fixture()
        val preview = Any(); app.brightnessPreview.begin(preview); await { host(service).visible }
        try {
            var nativeFraction = .5f
            assertTrue(app.automaticCalibration.prepare())
            assertTrue(app.automaticCalibration.connect(object : CalibrationServicePort {
                override fun open() = true
                override fun control() = NativeBrightnessControl("brightness", 0f, 100f, nativeFraction * 100, true)
                override fun move(expected: NativeBrightnessControl, fraction: Float): Boolean {
                    nativeFraction = fraction
                    Settings.System.putInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS, listOf(0, 16, 64, 144, 255)[(fraction * 4).toInt()])
                    return true
                }
                override fun progress(percent: Int) = Unit
                override fun close() = Unit
            }))
            app.automaticCalibration.start()
            // Seventeen points each need 500 ms of stable simulated time.
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(14))
            val result = app.automaticCalibration.state.value
            assertEquals(result.toString(), AutomaticCalibrationPhase.REVIEW, result.phase)
            assertEquals(BrightnessScale.AUTOMATIC_SAMPLE_COUNT, result.samples.size)
            await { host(service).visible }
            fun displayed(): Float {
                val view = ReflectionHelpers.getField<android.view.ViewGroup>(host(service), "view")
                return ReflectionHelpers.getField(view.getChildAt(0), "level")
            }
            val before = displayed()
            app.automaticCalibration.save()
            val expected = app.brightness.fraction()
            assertNotEquals(before, expected, .01f)
            await { kotlin.math.abs(displayed() - expected) < .001f }
            assertEquals(100, Settings.System.getInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS))
        } finally { app.brightnessPreview.end(preview); app.brightness.calibration.reset() }
    }
}
