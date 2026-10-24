package com.modukey.app.ui

import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
class BrightnessReturnInterfaceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val app get() = rule.activity.application as NudgeApplication
    @After fun cleanup() { app.onTerminate() }
    private fun ready(): NudgeViewModel {
        rule.waitUntil(10_000) { rule.runOnIdle { app.configuration.state.value != null } }
        return NudgeViewModel(app)
    }

    @Test fun homeUsesSwipeAdjustmentWithoutAJumpOption() {
        val vm = ready()
        val before = app.configuration.state.value!!.brightnessPopup
        rule.activity.setContent {
            val current by app.configuration.state.collectAsStateWithLifecycle()
            NudgeTheme { Column(Modifier.verticalScroll(rememberScrollState())) { BrightnessPopupSettingsSection(checkNotNull(current), vm) } }
        }
        rule.onNodeWithText("Jump to touch").assertDoesNotExist()
        rule.onNodeWithText("Swipe to adjust brightness from its current level. Touching the slider does not change the level.").assertExists()
        rule.runOnIdle { assertEquals(before, app.configuration.state.value!!.brightnessPopup) }
    }

    @Test fun buttonEditorSavesReturnSettingsWithoutChangingItsIcon() {
        val vm = ready()
        val original = PanelButton(action = ButtonAction.BRIGHTNESS_MAXIMUM, icon = IconReference(builtIn = BuiltInIcon.CAMERA))
        var saved: PanelButton? = null
        rule.activity.setContent { NudgeTheme { ButtonEditor(original, PanelConfig(), vm, {}) { saved = it } } }
        rule.onNodeWithContentDescription("Use previous brightness").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Return brightness").performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) { it(.25f) }
        rule.onNodeWithText("Save").performClick()
        rule.runOnIdle { assertEquals(original.copy(brightnessReturn = BrightnessReturnSettings(false, .25f)), saved) }
    }

    @Test fun directGestureSavesReturnSettingsAndRetainsBothPopupOptions() {
        val vm = ready()
        val original = PanelButton(action = ButtonAction.BRIGHTNESS_MINIMUM)
        val target = mutableStateOf(GestureTarget(action = original, volume = true, brightness = true))
        rule.activity.setContent { NudgeTheme { GestureBindingRow(AppConfiguration(), GestureSlot.HOLD, target.value, vm) { target.value = it } } }
        rule.onNodeWithText("Action: Minimum brightness + volume popup + Brightness slider").performClick()
        rule.onNode(hasScrollToIndexAction()).performScrollToIndex(5)
        rule.onNodeWithContentDescription("Use previous brightness").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Return brightness").performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) { it(.7f) }
        rule.onNodeWithText("Save").performClick()
        rule.runOnIdle {
            assertTrue(target.value.volume); assertTrue(target.value.brightness)
            assertEquals(original.copy(brightnessReturn = BrightnessReturnSettings(false, .7f)), target.value.action)
        }
    }

    @Test fun cancelAndResetPreserveSavedActionAndReturnValue() {
        val vm = ready()
        val original = PanelButton(action = ButtonAction.BRIGHTNESS_MAXIMUM, brightnessReturn = BrightnessReturnSettings(false, .3f))
        var saved: PanelButton? = null
        var cancelled = false
        rule.activity.setContent { NudgeTheme { ButtonEditor(original, PanelConfig(), vm, { cancelled = true }) { saved = it } } }
        rule.onNodeWithContentDescription("Reset Return brightness").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Reset Use previous brightness").performScrollTo().performClick()
        rule.onNodeWithText("Cancel").performClick()
        rule.runOnIdle { assertTrue(cancelled); assertNull(saved); assertEquals(.3f, original.brightnessReturn.fraction) }
    }
}
