package com.modukey.app.platform

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.modukey.app.NudgeApplication
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36])
class AccessibilityControlTest {
    private val app get() = ApplicationProvider.getApplicationContext<NudgeApplication>()
    @After fun stop() { app.onTerminate() }
    @Test fun enablingRedirectsToAndroidWithoutGrantingPermission() {
        val control = AccessibilityControl(app)
        assertFalse(control.enabled)
        assertTrue(control.setEnabled(true))
        assertFalse(control.enabled)
        assertFalse(AccessibilityPermission.enabled(app))
        val intent = shadowOf(app).nextStartedActivity
        assertEquals(Settings.ACTION_ACCESSIBILITY_SETTINGS, intent.action)
        @Suppress("DEPRECATION") val component = intent.getParcelableExtra<ComponentName>(Intent.EXTRA_COMPONENT_NAME)
        assertEquals(ComponentName(app, EdgeAccessibilityService::class.java), component)
        assertNotEquals(0, intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    @Test fun onlyTheCurrentOwnerCanUnbindAndTurningOffNeverChangesOtherServices() {
        Settings.Secure.putString(app.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, "another.app/.Service")
        val control = AccessibilityControl(app)
        var stopped = 0
        val old = Any(); val current = Any()
        control.bind(old) { fail("An old service cannot receive the request") }
        control.bind(current) { stopped++; control.unbind(current) }
        control.unbind(old)
        assertTrue(control.enabled)
        assertTrue(control.toggle())
        assertEquals(1, stopped)
        assertFalse(control.enabled)
        assertTrue(control.setEnabled(false))
        assertEquals(1, stopped)
        assertEquals("another.app/.Service", Settings.Secure.getString(app.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES))
        assertNull(shadowOf(app).nextStartedActivity)
    }
    @Test fun enabledButDisconnectedServicesRedirectForManualDisabling() {
        val component = ComponentName(app, EdgeAccessibilityService::class.java).flattenToShortString()
        Settings.Secure.putString(app.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, component)
        val control = AccessibilityControl(app)
        assertTrue(control.enabled)
        assertTrue(control.setEnabled(false))
        assertEquals(Settings.ACTION_ACCESSIBILITY_SETTINGS, shadowOf(app).nextStartedActivity.action)
        assertTrue(control.enabled)
        assertEquals(component, Settings.Secure.getString(app.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES))
    }
    @Test fun missingOrBlockedSettingsAreReportedWithoutChangingPermission() {
        for (blocked in listOf(false, true)) {
            val context = object : ContextWrapper(app) {
                override fun startActivity(intent: Intent) {
                    if (blocked) throw SecurityException() else throw ActivityNotFoundException()
                }
            }
            assertFalse(AccessibilityControl(context).setEnabled(true))
            assertFalse(AccessibilityPermission.enabled(app))
        }
    }
}
