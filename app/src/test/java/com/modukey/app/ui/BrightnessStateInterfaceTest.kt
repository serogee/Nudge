package com.modukey.app.ui

import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.modukey.app.MainActivity
import com.modukey.app.NudgeApplication
import com.modukey.app.core.*
import com.modukey.app.platform.*
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], shadows = [BrightnessControllerTest.Writes::class])
class BrightnessStateInterfaceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val app get() = rule.activity.application as NudgeApplication
    private var cleanupHost: () -> Unit = {}
    @After fun cleanup() { cleanupHost(); BrightnessControllerTest.Writes.allowed = false; app.onTerminate() }

    @Test fun actualPanelShowsBothButtonsInactiveAtEveryIntermediateBrightness() {
        rule.activity.setContent { NudgeTheme { Text("Test") } }
        BrightnessControllerTest.Writes.allowed = true
        shadowOf(app).grantPermissions("${app.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
        val service = Robolectric.buildService(EdgeAccessibilityService::class.java).create()
        val actions = PanelActions(service.get())
        val panel = PanelConfig(buttons = listOf(PanelButton(action = ButtonAction.BRIGHTNESS_MAXIMUM), PanelButton(action = ButtonAction.BRIGHTNESS_MINIMUM)))
        val geometry = PanelGeometry.calculate(Bounds(0f, 0f, 400f, 800f), 1f, panel.portrait, 2, Bounds(384f, 200f, 16f, 120f), Edge.RIGHT)
        lateinit var host: AccessibilityPanelHost
        host = AccessibilityPanelHost(rule.activity, rule.activity.getSystemService(WindowManager::class.java), actions,
            {}, { _, _ -> false }, { button -> actions.execute("owner", button, false); host.refreshSound() })
        cleanupHost = { host.hide(false); service.destroy() }
        rule.runOnIdle {
            Settings.System.putInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 100)
            Settings.System.putInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, 0)
            host.show(PanelSpec(panel, panel.portrait, geometry, false))
        }
        for (name in listOf("Maximum brightness", "Minimum brightness")) {
            for (level in listOf(65, 128, 192)) {
                rule.onNodeWithContentDescription("$name. Off").performClick()
                rule.onNodeWithContentDescription("$name. On").assertIsSelected()
                rule.runOnIdle { Settings.System.putInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS, level) }
                rule.onNodeWithContentDescription("Maximum brightness. Off").assertIsNotSelected()
                rule.onNodeWithContentDescription("Minimum brightness. Off").assertIsNotSelected()
            }
        }
        for (level in listOf(255, 1)) {
            rule.runOnIdle { Settings.System.putInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS, level) }
            rule.onNodeWithContentDescription("Maximum brightness. Off").assertIsNotSelected()
            rule.onNodeWithContentDescription("Minimum brightness. Off").assertIsNotSelected()
        }
    }
}
