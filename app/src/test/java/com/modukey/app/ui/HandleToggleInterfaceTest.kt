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
import org.junit.Assert.*
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
    @Test fun combinedEditorSavesIndependentScopesAndRejectsTwoEmptyGroups() {
        val app = rule.activity.application as NudgeApplication
        rule.waitUntil(10_000) { rule.runOnIdle { app.configuration.state.value != null } }
        var saved: PanelButton? = null
        val original = PanelButton(action = ButtonAction.TOGGLE_AND_HIDE_HANDLES)
        val vm = NudgeViewModel(app)
        rule.activity.setContent { NudgeTheme { ButtonEditor(original, PanelConfig(), vm, {}) { saved = it } } }
        rule.onAllNodesWithText("All handles").onFirst().performScrollTo().performClick()
        rule.onNodeWithText("This handle").performClick()
        rule.onNodeWithText("All handles").performScrollTo().performClick()
        rule.onNodeWithText("Other handles").performClick()
        rule.onNodeWithText("Save").performClick()
        rule.runOnIdle { assertEquals(original.copy(toggleScope = HandleGroupScope.CURRENT, visibilityScope = HandleGroupScope.OTHERS), saved) }
        rule.onNodeWithText("This handle").performScrollTo().performClick(); rule.onNodeWithText("None").performClick()
        rule.onNodeWithText("Other handles").performScrollTo().performClick(); rule.onAllNodesWithText("None").onLast().performClick()
        rule.onNodeWithText("Save").assertIsNotEnabled()
    }
    @Test fun `handle tile shows group status and remains selectable without automatic colors or a container`() {
        val sound = mutableStateOf(SoundState(enabledHandleIds = setOf("a", "b"), ownerHandleId = "a", handlesEnabled = true))
        val button = PanelButton(action = ButtonAction.TOGGLE_HANDLES, handleScope = HandleToggleScope.OTHERS, iconContainer = IconContainer.STANDALONE)
        rule.activity.setContent { NudgeTheme { PanelButtonFace(button, PanelConfig(automaticButtonColor = false), 56.dp, sound.value, true) {} } }
        rule.onNodeWithContentDescription("Toggle handles. On").assertIsSelected().assertHasClickAction()
        rule.runOnIdle { sound.value = sound.value.copy(pausedHandleIds = setOf("b")) }
        rule.onNodeWithContentDescription("Toggle handles. Off").assertIsNotSelected().assertHasClickAction()
    }
    @Test fun combinedTileKeepsItsPauseStateWhenVisibilityChanges() {
        val sound = mutableStateOf(SoundState(enabledHandleIds = setOf("a", "b"), ownerHandleId = "a", handlesEnabled = true))
        val button = mutableStateOf(PanelButton(action = ButtonAction.TOGGLE_AND_HIDE_HANDLES))
        rule.activity.setContent { NudgeTheme { PanelButtonFace(button.value, PanelConfig(), 56.dp, sound.value, true) {} } }
        for ((scope, targets) in listOf(HandleGroupScope.ALL to setOf("a", "b"),
            HandleGroupScope.CURRENT to setOf("a"), HandleGroupScope.OTHERS to setOf("b"))) {
            rule.runOnIdle {
                button.value = button.value.copy(toggleScope = scope)
                sound.value = sound.value.copy(pausedHandleIds = emptySet(), hiddenIndicatorIds = emptySet())
            }
            rule.onNodeWithContentDescription("Toggle and hide handles. Gestures: Running. Visibility: Shown.").assertIsNotSelected()
            rule.runOnIdle { sound.value = sound.value.copy(pausedHandleIds = targets, hiddenIndicatorIds = setOf("a", "b")) }
            rule.onNodeWithContentDescription("Toggle and hide handles. Gestures: Paused. Visibility: Hidden.").assertIsSelected()
            rule.runOnIdle { sound.value = sound.value.copy(hiddenIndicatorIds = emptySet()) }
            rule.onNodeWithContentDescription("Toggle and hide handles. Gestures: Paused. Visibility: Shown.").assertIsSelected()
            rule.runOnIdle { sound.value = sound.value.copy(hiddenIndicatorIds = setOf("a")) }
            rule.onNodeWithContentDescription("Toggle and hide handles. Gestures: Paused. Visibility: Mixed.").assertIsSelected()
            rule.runOnIdle { sound.value = sound.value.copy(pausedHandleIds = emptySet(), hiddenIndicatorIds = setOf("a", "b")) }
            rule.onNodeWithContentDescription("Toggle and hide handles. Gestures: Running. Visibility: Hidden.").assertIsNotSelected()
        }
        rule.runOnIdle {
            button.value = button.value.copy(toggleScope = HandleGroupScope.ALL, visibilityScope = HandleGroupScope.NONE)
            sound.value = sound.value.copy(pausedHandleIds = setOf("a", "b"), hiddenIndicatorIds = emptySet())
        }
        rule.onNode(hasClickAction()).assertIsSelected()
        rule.runOnIdle { sound.value = sound.value.copy(pausedHandleIds = setOf("a")) }
        rule.onNode(hasClickAction()).assertIsNotSelected()
        rule.runOnIdle { sound.value = sound.value.copy(enabledHandleIds = emptySet()) }
        rule.onNode(hasClickAction()).assertIsNotSelected()
    }
    @Test fun combinedTileWithoutAPauseGroupFollowsVisibility() {
        val sound = mutableStateOf(SoundState(enabledHandleIds = setOf("a", "b"), pausedHandleIds = setOf("a", "b")))
        val button = mutableStateOf(PanelButton(action = ButtonAction.TOGGLE_AND_HIDE_HANDLES, toggleScope = HandleGroupScope.NONE))
        rule.activity.setContent { NudgeTheme { PanelButtonFace(button.value, PanelConfig(), 56.dp, sound.value, true) {} } }
        val tile = rule.onNode(hasClickAction())
        tile.assertIsNotSelected()
        rule.runOnIdle { sound.value = sound.value.copy(hiddenIndicatorIds = setOf("a")) }
        tile.assertIsNotSelected()
        rule.runOnIdle { sound.value = sound.value.copy(hiddenIndicatorIds = setOf("a", "b")) }
        tile.assertIsSelected()
        rule.runOnIdle { button.value = button.value.copy(visibilityScope = HandleGroupScope.NONE) }
        tile.assertIsNotSelected()
        rule.runOnIdle { button.value = button.value.copy(toggleScope = HandleGroupScope.CURRENT) }
        tile.assertIsNotSelected()
    }
    @Test fun `brightness tiles expose the current system limit`() {
        rule.activity.setContent { NudgeTheme { PanelButtonFace(PanelButton(action = ButtonAction.BRIGHTNESS_MAXIMUM), PanelConfig(), 56.dp,
            SoundState(maximumBrightness = true), true) {} } }
        rule.onNodeWithContentDescription("Maximum brightness. On").assertIsSelected()
    }
}
