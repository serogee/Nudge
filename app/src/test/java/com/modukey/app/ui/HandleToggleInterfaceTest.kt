package com.modukey.app.ui

import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.modukey.app.MainActivity
import com.modukey.app.NudgeApplication
import com.modukey.app.core.*
import com.modukey.app.platform.SoundState
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HandleToggleInterfaceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @After fun cleanup() { (rule.activity.application as NudgeApplication).onTerminate() }
    @Test fun `handle tile shows group status and remains selectable without automatic colors or a container`() {
        val sound = mutableStateOf(SoundState(enabledHandleIds = setOf("a", "b"), ownerHandleId = "a", handlesEnabled = true))
        val button = PanelButton(action = ButtonAction.TOGGLE_HANDLES, handleScope = HandleToggleScope.OTHERS, iconContainer = IconContainer.STANDALONE)
        rule.activity.setContent { NudgeTheme { PanelButtonFace(button, PanelConfig(automaticButtonColor = false), 56.dp, sound.value, true) {} } }
        rule.onNodeWithContentDescription("Toggle handles. On").assertIsSelected().assertHasClickAction()
        rule.runOnIdle { sound.value = sound.value.copy(pausedHandleIds = setOf("b")) }
        rule.onNodeWithContentDescription("Toggle handles. Off").assertIsNotSelected().assertHasClickAction()
    }
    @Test fun `brightness tiles expose the current system limit`() {
        rule.activity.setContent { NudgeTheme { PanelButtonFace(PanelButton(action = ButtonAction.BRIGHTNESS_MAXIMUM), PanelConfig(), 56.dp,
            SoundState(maximumBrightness = true), true) {} } }
        rule.onNodeWithContentDescription("Maximum brightness. On").assertIsSelected()
    }
}
