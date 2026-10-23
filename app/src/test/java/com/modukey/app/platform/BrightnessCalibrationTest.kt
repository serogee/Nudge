package com.modukey.app.platform

import androidx.test.core.app.ApplicationProvider
import com.modukey.app.NudgeApplication
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [AtomicFilePlatformShadow::class])
class BrightnessCalibrationTest {
    @Test fun denseProfilesReloadAndCanBeReplacedByFiveManualPoints() {
        val app = ApplicationProvider.getApplicationContext<NudgeApplication>()
        val calibration = BrightnessCalibration(app)
        try {
            val dense = listOf(0, 0, 1, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 255, 255)
            assertTrue(calibration.save(dense))
            assertEquals(dense, BrightnessCalibration(app).scale.samples)
            val manual = listOf(0, 16, 64, 144, 255)
            assertTrue(calibration.save(manual))
            assertEquals(manual, BrightnessCalibration(app).scale.samples)
        } finally { calibration.reset(); app.onTerminate() }
    }
    @Test fun measurementsSurviveReloadAndResetWithoutEnteringConfiguration() {
        val app = ApplicationProvider.getApplicationContext<NudgeApplication>()
        val calibration = BrightnessCalibration(app)
        try {
            assertTrue(calibration.save(listOf(0, 15, 60, 140, 255)))
            assertEquals(0, BrightnessCalibration(app).scale.level(0f))
            assertEquals(60, BrightnessCalibration(app).scale.level(.5f))
            assertFalse(calibration.save(listOf(1, 15, 15, 140, 255)))
            assertEquals(60, calibration.scale.level(.5f))
            assertTrue(calibration.reset()); assertFalse(BrightnessCalibration(app).calibrated)
        } finally { calibration.reset(); app.onTerminate() }
    }
}
