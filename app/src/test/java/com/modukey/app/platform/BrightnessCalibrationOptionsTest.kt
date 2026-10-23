package com.modukey.app.platform

import androidx.test.core.app.ApplicationProvider
import com.modukey.app.NudgeApplication
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36], shadows = [AtomicFilePlatformShadow::class])
class BrightnessCalibrationOptionsTest {
    private val app get() = ApplicationProvider.getApplicationContext<NudgeApplication>()
    @After fun cleanup() {
        android.util.AtomicFile(File(app.noBackupFilesDir, "brightness-calibration-options")).delete()
        app.onTerminate()
    }
    @Test fun keepingTheServiceDefaultsOffAndSurvivesReloadOnlyWhenSelected() {
        val options = BrightnessCalibrationOptions(app)
        assertFalse(options.keepServiceEnabled.value)
        assertTrue(options.setKeepServiceEnabled(true))
        assertTrue(BrightnessCalibrationOptions(app).keepServiceEnabled.value)
        assertTrue(options.setKeepServiceEnabled(false))
        assertFalse(BrightnessCalibrationOptions(app).keepServiceEnabled.value)
    }
    @Test fun invalidOrTruncatedPreferenceFilesDefaultOff() {
        val file = File(app.noBackupFilesDir, "brightness-calibration-options")
        for (value in listOf(byteArrayOf(), byteArrayOf(2), byteArrayOf(1, 1))) {
            file.writeBytes(value)
            assertFalse(BrightnessCalibrationOptions(app).keepServiceEnabled.value)
        }
    }
}
