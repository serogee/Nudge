package com.modukey.app.ui

import android.content.pm.ShortcutManager
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import com.modukey.app.R
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BrightnessAndShortcutInterfaceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val app get() = rule.activity.application as NudgeApplication
    @After fun cleanup() { app.onTerminate() }
    private fun ready() {
        rule.waitUntil(10_000) { rule.runOnIdle { app.configuration.state.value != null || app.configuration.error.value != null } }
        rule.runOnIdle { assertNull(app.configuration.error.value); app.configuration.update { AppConfiguration() } }
        rule.waitForIdle()
    }
    @Test fun brightnessPreviewEndsWhenLeavingHomeAndSizeChangesRemainIndependent() {
        ready()
        rule.onNodeWithContentDescription("Preview brightness slider").performScrollTo().performClick()
        rule.runOnIdle { assertTrue(app.brightnessPreview.state.value) }
        rule.onNode(hasText("Info") and hasClickAction()).performClick()
        rule.runOnIdle { assertFalse(app.brightnessPreview.state.value) }
        val vm = NudgeViewModel(app)
        rule.runOnIdle { vm.edit { it.copy(brightnessPopup = it.brightnessPopup.copy(widthDp = 88f)) } }
        rule.runOnIdle {
            assertEquals(88f, app.configuration.state.value!!.brightnessPopup.widthDp)
            assertEquals(240f, app.configuration.state.value!!.brightnessPopup.heightDp)
            assertEquals(AppConfiguration().normalized().handles, app.configuration.state.value!!.handles)
        }
    }
    @Test fun brightnessAppearanceControlsKeepSavedColorsWhileBackgroundIsHidden() {
        ready(); val vm = NudgeViewModel(app)
        rule.activity.setContent {
            val current by app.configuration.state.collectAsStateWithLifecycle()
            NudgeTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
                BrightnessPopupSettingsSection(checkNotNull(current), vm)
            } }
        }
        rule.onNodeWithContentDescription(rule.activity.getString(R.string.expand_setup)).performClick()
        rule.onNodeWithContentDescription("Corner radius").performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) { it(18f) }
        rule.onNodeWithContentDescription("Show slider background").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Background opacity").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithContentDescription("Show automatic brightness toggle").performScrollTo().performClick()
        rule.runOnIdle {
            val s = app.configuration.state.value!!.brightnessPopup
            assertEquals(18f, s.cornerRadiusDp); assertFalse(s.showBackground); assertFalse(s.showAutomaticToggle)
            assertEquals(BrightnessPopupSettings().background, s.background)
        }
    }
    @Test fun automaticBrightnessCanBeAssignedAsADirectAction() {
        ready(); val vm = NudgeViewModel(app)
        val target = mutableStateOf(GestureTarget.NONE)
        rule.activity.setContent { NudgeTheme { GestureBindingRow(AppConfiguration(), GestureSlot.HOLD, target.value, vm) { target.value = it } } }
        rule.onNodeWithText("No action").performClick()
        rule.onNodeWithText("Action: Direct action").performClick()
        rule.onNode(hasScrollToIndexAction()).performScrollToIndex(ButtonAction.BRIGHTNESS_AUTOMATIC.ordinal)
        rule.onNodeWithText("Automatic brightness").performClick()
        rule.onNodeWithText("Save").performClick()
        rule.runOnIdle { assertEquals(ButtonAction.BRIGHTNESS_AUTOMATIC, target.value.action?.action) }
    }
    @Test fun gestureActionChangesRetainBothPopupOptions() {
        ready(); val vm = NudgeViewModel(app)
        val target = mutableStateOf(GestureTarget(volume = true, brightness = true))
        rule.activity.setContent { NudgeTheme { GestureBindingRow(AppConfiguration(), GestureSlot.HOLD, target.value, vm) { target.value = it } } }
        rule.onNodeWithText("Volume popup + Brightness slider").performClick()
        rule.onNodeWithText("Action: Direct action").performClick()
        rule.onNode(hasScrollToIndexAction()).performScrollToIndex(ButtonAction.BRIGHTNESS_UP.ordinal)
        rule.onNodeWithText("Increase brightness").performClick()
        rule.onNodeWithText("Save").performClick()
        rule.runOnIdle { assertTrue(target.value.brightness); assertTrue(target.value.volume); assertEquals(ButtonAction.BRIGHTNESS_UP, target.value.action?.action) }
    }
    @Test fun homeCreatesNamedShortcutAfterActionAndIconReview() {
        ready()
        val manager = app.getSystemService(ShortcutManager::class.java)
        shadowOf(manager).setIsRequestPinShortcutSupported(true)
        val vm = NudgeViewModel(app)
        var notice: Int? = null
        rule.activity.lifecycleScope.launch { vm.notices.collect { notice = it } }
        rule.activity.setContent {
            val current by app.configuration.state.collectAsStateWithLifecycle()
            NudgeTheme { HomeShortcutsSection(checkNotNull(current), vm) }
        }
        rule.waitUntil(10_000) { vm.shortcutSupported.value == true }
        rule.onNodeWithText("Add shortcut").performClick()
        rule.onNodeWithText("Volume up").performClick()
        rule.onNodeWithText("Save").performClick()
        rule.onNode(hasText("Shortcut name") and hasSetTextAction()).performTextClearance()
        rule.onNode(hasText("Shortcut name") and hasSetTextAction()).performTextInput("Louder")
        rule.onNodeWithText("Add to Home screen").performClick()
        rule.waitUntil(10_000) { rule.runOnIdle { !vm.shortcutBusy.value } }
        rule.runOnIdle {
            assertNull(app.configuration.error.value)
            assertTrue("Notice: " + notice?.let(rule.activity::getString), manager.pinnedShortcuts.isNotEmpty())
        }
        rule.runOnIdle {
            val stored = app.configuration.state.value!!.shortcuts.single()
            assertEquals("Louder", stored.name); assertEquals(ButtonAction.VOLUME_UP, stored.button.action)
            assertEquals(stored.id, manager.pinnedShortcuts.single().id)
        }
    }
}
