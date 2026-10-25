package com.modukey.app.ui

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.modukey.app.MainActivity
import com.modukey.app.NudgeApplication
import com.modukey.app.core.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WheelHitboxInterfaceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @After fun stop() { (rule.activity.application as NudgeApplication).onTerminate() }
    @Test fun dropdownRetainsManualRadiiAndOnlyThumbDraggingOrNumericEntryChangesThem() {
        val settings = mutableStateOf(WheelHitboxSettings())
        rule.activity.setContent { NudgeTheme { Column(Modifier.width(340.dp).testTag("hitbox-control")) {
            WheelHitboxControls(settings.value) { settings.value = it(settings.value) }
        } } }
        rule.onNodeWithText("Automatic").performClick()
        rule.onNodeWithText("Manual").performClick()
        val before = settings.value
        rule.onNodeWithTag("hitbox-control").performTouchInput { click(Offset(width - 24f, height - 24f)) }
        rule.runOnIdle { assertEquals(before, settings.value) }
        rule.onNodeWithContentDescription("Inner hitbox radius").performTouchInput { down(center); moveBy(Offset(40f, 0f)); up() }
        rule.runOnIdle { assertTrue(settings.value.innerRadiusDp > 32f) }
        rule.onNodeWithContentDescription("Outer hitbox radius").performSemanticsAction(SemanticsActions.SetProgress) { it(420f) }
        rule.onNodeWithContentDescription("Edit Inner hitbox radius value").performClick()
        rule.onNode(hasSetTextAction()).performTextReplacement("12")
        rule.onNodeWithText("Save").performClick()
        rule.onNodeWithText("Manual").performClick()
        rule.onNodeWithText("Use Button Size").performClick()
        rule.onNodeWithContentDescription("Inner hitbox radius").assertDoesNotExist()
        rule.onNodeWithText("Use Button Size").performClick()
        rule.onNodeWithText("Automatic").performClick()
        rule.onNodeWithText("Automatic").performClick()
        rule.onNodeWithText("Manual").performClick()
        rule.runOnIdle { assertEquals(WheelHitboxSettings(WheelHitboxMode.MANUAL, 12f, 420f), settings.value) }
    }
}
