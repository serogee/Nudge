package com.modukey.app.ui

import android.content.ComponentName
import android.provider.Settings
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import com.modukey.app.MainActivity
import com.modukey.app.NudgeApplication
import com.modukey.app.platform.EdgeAccessibilityService
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AccessibilityInterfaceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val app get() = rule.activity.application as NudgeApplication
    @After fun stop() { app.onTerminate() }
    private fun ready() = rule.waitUntil(10_000) { rule.runOnIdle { app.configuration.state.value != null } }
    @Test fun homeSwitchShowsPermissionStateAndDoesNotPretendEnablingSucceeded() {
        ready()
        rule.onNodeWithContentDescription("Nudge accessibility").assertIsOff().performClick().assertIsOff()
        rule.runOnIdle {
            assertEquals(Settings.ACTION_ACCESSIBILITY_SETTINGS, shadowOf(app).nextStartedActivity.action)
            val component = ComponentName(app, EdgeAccessibilityService::class.java).flattenToString()
            Settings.Secure.putString(app.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, component)
            ViewModelProvider(rule.activity)[NudgeViewModel::class.java].refreshPermissions()
        }
        rule.onNodeWithContentDescription("Nudge accessibility").assertIsOn().performClick().assertIsOn()
        rule.runOnIdle { assertEquals(Settings.ACTION_ACCESSIBILITY_SETTINGS, shadowOf(app).nextStartedActivity.action) }
    }
    @Test fun turningOffClosesHandlesAndRetainsSettingsWithoutChangingTheMasterSwitch() {
        ready()
        val controller = Robolectric.buildService(EdgeAccessibilityService::class.java).create()
        try {
            rule.runOnIdle {
                shadowOf(app).grantPermissions("${app.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
                app.configuration.update { it.copy(enabled = true) }
                ReflectionHelpers.callInstanceMethod<Void>(controller.get(), "onServiceConnected")
                assertTrue(app.status.state.value.connected)
            }
            val saved = rule.runOnIdle { app.configuration.state.value }
            rule.onNodeWithContentDescription("Nudge accessibility").assertIsOn().performClick().assertIsOff()
            rule.runOnIdle {
                assertFalse(app.status.state.value.connected)
                assertTrue(app.status.state.value.attachedHandleIds.isEmpty())
                assertEquals(saved, app.configuration.state.value)
                assertTrue(app.configuration.state.value!!.enabled)
            }
        } finally { rule.runOnIdle { controller.destroy() } }
    }
}
