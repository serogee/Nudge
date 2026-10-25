package com.modukey.app.ui

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.modukey.app.MainActivity
import com.modukey.app.NudgeApplication
import com.modukey.app.core.WheelLayoutSettings
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WheelIconInterfaceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @After fun stop() { (rule.activity.application as NudgeApplication).onTerminate() }
    @Test fun adaptiveDefaultHidesManualSizeAndTogglingRetainsAllValues() {
        val settings = mutableStateOf(WheelLayoutSettings())
        rule.activity.setContent { NudgeTheme { Column { WheelIconControls(settings.value) { settings.value = it } } } }
        rule.onNodeWithContentDescription("Adapt icon size").assertIsOn()
        rule.onNodeWithContentDescription("Icon size").assertDoesNotExist()
        rule.onNodeWithContentDescription("Icon position").performSemanticsAction(SemanticsActions.SetProgress) { it(.8f) }
        rule.onNodeWithContentDescription("Adapt icon size").performClick()
        rule.onNodeWithContentDescription("Icon size").performSemanticsAction(SemanticsActions.SetProgress) { it(96f) }
        rule.onNodeWithContentDescription("Adapt icon size").performClick().assertIsOn()
        rule.onNodeWithContentDescription("Icon size").assertDoesNotExist()
        rule.onNodeWithContentDescription("Adapt icon size").performClick()
        rule.runOnIdle { assertEquals(96f, settings.value.iconSizeDp); assertEquals(.8f, settings.value.iconPosition); assertTrue(settings.value.autoFit) }
    }
}
