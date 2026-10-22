package com.modukey.app.platform

import androidx.test.core.app.ApplicationProvider
import com.modukey.app.NudgeApplication
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BrightnessCalibrationTest {
    @Test fun measurementsSurviveReloadAndResetWithoutEnteringConfiguration() {
        val app = ApplicationProvider.getApplicationContext<NudgeApplication>()
        val calibration = BrightnessCalibration(app)
        try {
            assertTrue(calibration.save(listOf(1, 15, 60, 140, 255)))
            assertEquals(60, BrightnessCalibration(app).scale.level(.5f))
            assertFalse(calibration.save(listOf(1, 15, 15, 140, 255)))
            assertEquals(60, calibration.scale.level(.5f))
            assertTrue(calibration.reset()); assertFalse(BrightnessCalibration(app).calibrated)
        } finally { calibration.reset(); app.onTerminate() }
    }
}
