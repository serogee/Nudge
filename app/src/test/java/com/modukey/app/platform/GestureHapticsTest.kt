package com.modukey.app.platform

import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibratorManager
import androidx.test.core.app.ApplicationProvider
import com.modukey.app.NudgeApplication
import com.modukey.app.core.GestureFeedback
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36])
class GestureHapticsTest {
    private val app get() = ApplicationProvider.getApplicationContext<NudgeApplication>()
    @After fun cleanup() { app.onTerminate() }
    @Test fun enabledFeedbackUsesTheVibratorWithStrengthAndAccessibilityUsage() {
        @Suppress("DEPRECATION")
        val permissions = app.packageManager.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
        assertTrue(android.Manifest.permission.VIBRATE in permissions)
        shadowOf(app).grantPermissions(android.Manifest.permission.VIBRATE)
        val vibrator = app.getSystemService(VibratorManager::class.java).defaultVibrator
        val shadow = shadowOf(vibrator)
        shadow.setHasVibrator(true); shadow.setHasAmplitudeControl(true)
        val haptics = GestureHaptics(app)
        assertFalse(haptics.perform(GestureFeedback()))
        assertTrue(haptics.perform(GestureFeedback(true, .2f))); assertTrue(shadow.isVibrating)
        val low = shadow.milliseconds
        assertTrue("Low strength must remain brief", low < 30)
        assertTrue(haptics.perform(GestureFeedback(true, 1f)))
        assertEquals("Maximum strength must use a stronger pulse", 140L, shadow.milliseconds)
        if (Build.VERSION.SDK_INT >= 33) assertEquals(VibrationAttributes.USAGE_ACCESSIBILITY,
            (shadow.vibrationAttributesFromLastVibration as VibrationAttributes).usage)
        shadow.setHasAmplitudeControl(false)
        assertTrue(haptics.perform(GestureFeedback(true)))
        shadow.setHasVibrator(false); assertFalse(haptics.perform(GestureFeedback(true)))
    }
}
