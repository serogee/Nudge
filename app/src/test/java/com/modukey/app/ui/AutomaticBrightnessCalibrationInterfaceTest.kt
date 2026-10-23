package com.modukey.app.ui

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import android.app.KeyguardManager
import android.os.PowerManager
import android.provider.Settings
import com.modukey.app.MainActivity
import com.modukey.app.NudgeApplication
import com.modukey.app.platform.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], shadows = [BrightnessControllerTest.Writes::class])
class AutomaticBrightnessCalibrationInterfaceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val app get() = rule.activity.application as NudgeApplication
    @After fun cleanup() { app.automaticCalibration.recover(false); app.onTerminate(); BrightnessControllerTest.Writes.allowed = false }
    private fun content() {
        BrightnessControllerTest.Writes.allowed = true
        shadowOf(app.getSystemService(PowerManager::class.java)).setIsInteractive(true)
        shadowOf(app.getSystemService(KeyguardManager::class.java)).setKeyguardLocked(false)
        rule.activity.setContent { NudgeTheme { Column(Modifier.verticalScroll(rememberScrollState())) { BrightnessCalibrationSection() } } }
    }
    @Test fun disclosureDoesNotStartCalibrationAndManualCalibrationRemainsAvailable() {
        content()
        rule.onNodeWithText("Manual calibration").assertIsEnabled()
        rule.onNodeWithText("Automatic calibration").performClick()
        rule.onNodeWithText("Android grants this separate service access to window content. Nudge uses it only for System UI brightness controls during calibration.").assertExists()
        rule.onNodeWithText("Enable calibration service").assertExists()
        rule.runOnIdle { assertFalse(app.brightness.calibrationNeedsRecovery); assertEquals(AutomaticCalibrationPhase.IDLE, app.automaticCalibration.state.value.phase) }
        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithText("Manual calibration").performClick()
        rule.onNodeWithText("Capture").assertExists()
        rule.onNodeWithText("Cancel").performClick()
    }
    @Test fun readyStateRequiresStartAndCancelTurnsOffTheTemporaryPort() {
        content(); var closed = 0
        rule.runOnIdle {
            assertTrue(app.automaticCalibration.prepare())
            assertTrue(app.automaticCalibration.connect(object : CalibrationServicePort {
                override fun open(): Boolean { fail("Enable must not start measurement"); return false }
                override fun control(): NativeBrightnessControl? { fail(); return null }
                override fun move(expected: NativeBrightnessControl, fraction: Float) = false
                override fun progress(percent: Int) = Unit
                override fun close() { closed++ }
            }))
        }
        rule.onNodeWithText("Start calibration").assertIsEnabled()
        rule.onNodeWithText("Manual calibration").assertIsNotEnabled()
        rule.onNodeWithText("Cancel").performClick()
        rule.runOnIdle { assertEquals(1, closed) }
        rule.onNodeWithText("Manual calibration").assertIsEnabled()
    }
    @Test fun interruptedCalibrationOffersRecoveryWithoutChangingTheSavedProfile() {
        content()
        rule.runOnIdle {
            Settings.System.putInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 73)
            Settings.System.putInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, 1)
            assertTrue(app.brightness.beginCalibration())
            Settings.System.putInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 255)
            app.automaticCalibration.cancel()
        }
        rule.onNodeWithText("Restore previous brightness").performScrollTo().performClick()
        rule.runOnIdle {
            assertEquals(73, Settings.System.getInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS))
            assertFalse(app.brightness.calibrationNeedsRecovery)
        }
        rule.onNodeWithText("Manual calibration").assertIsEnabled()
    }
}
