package com.modukey.app.platform

import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.modukey.app.NudgeApplication
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36], shadows = [BrightnessControllerTest.Writes::class])
class BrightnessCalibrationRestoreTest {
    private val app get() = ApplicationProvider.getApplicationContext<NudgeApplication>()
    private val resolver get() = app.contentResolver
    @After fun cleanup() {
        BrightnessControllerTest.Writes.failedSetting = null; BrightnessControllerTest.Writes.allowed = true
        BrightnessCalibrationRestore(app).discard(); app.onTerminate()
        BrightnessControllerTest.Writes.allowed = false
    }
    private fun set(level: Int, mode: Int) {
        Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, level)
        Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, mode)
    }
    @Test fun journalSurvivesRecreationAndRestoresBothManualAndAutomaticValues() {
        BrightnessControllerTest.Writes.allowed = true
        for (mode in 0..1) {
            set(73, mode)
            val first = BrightnessCalibrationRestore(app)
            assertTrue(first.begin()); assertTrue(first.pending)
            assertEquals(0, Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE))
            set(255, 0)
            val restarted = BrightnessCalibrationRestore(app)
            assertTrue(restarted.pending)
            assertEquals(255, Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS)) // No restore without a request.
            assertTrue(restarted.restore()); assertFalse(restarted.pending)
            assertEquals(73, Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS))
            assertEquals(mode, Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE))
        }
    }
    @Test fun failedRestoreKeepsRecoveryAndBlocksOtherNudgeBrightnessActions() {
        BrightnessControllerTest.Writes.allowed = true; set(0, 1)
        assertTrue(app.brightness.beginCalibration())
        set(200, 0)
        assertEquals(BrightnessResult.FAILED, app.brightness.setFraction(.5f))
        assertEquals(BrightnessResult.FAILED, app.brightness.toggleAutomatic())
        assertEquals(BrightnessResult.FAILED, app.brightness.adjust(true))
        BrightnessControllerTest.Writes.allowed = false
        assertFalse(app.brightness.restoreAfterCalibration()); assertTrue(app.brightness.calibrationNeedsRecovery)
        BrightnessControllerTest.Writes.allowed = true
        assertTrue(app.brightness.restoreAfterCalibration())
        assertEquals(0, Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS))
        assertEquals(1, Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE))
    }
    @Test fun keepingCurrentBrightnessClearsOnlyTheRestoreRecord() {
        BrightnessControllerTest.Writes.allowed = true; set(85, 0)
        val journal = BrightnessCalibrationRestore(app); assertTrue(journal.begin()); set(170, 0)
        assertTrue(journal.discard()); assertFalse(journal.pending)
        assertEquals(170, Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS))
        assertTrue(journal.restore()); assertEquals(170, Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS))
    }
}
