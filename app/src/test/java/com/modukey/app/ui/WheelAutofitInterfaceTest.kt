package com.modukey.app.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
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
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w480dp-h900dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WheelAutofitInterfaceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val app get() = rule.activity.application as NudgeApplication
    @After fun cleanup() { app.onTerminate() }
    private fun start(): String {
        rule.waitUntil(10_000) { rule.runOnIdle { app.configuration.preview.value != null } }
        rule.runOnIdle { app.configuration.update { AppConfiguration() } }
        rule.onNode(hasText("Wheels") and hasClickAction()).performClick()
        rule.onNodeWithText("Add wheel").performClick()
        return rule.runOnIdle { app.configuration.preview.value!!.panels.last().id }
    }
    private fun settings(id: String) = app.configuration.preview.value!!.panel(id)!!.settings
    @Test fun manualSelectedSizeStartsAndResetsAt110Percent() {
        val id = start()
        rule.onNodeWithContentDescription("Selected size").performScrollTo()
        rule.runOnIdle { assertEquals(1.1f, settings(id).portrait.wheel.selectedScale) }
        rule.onNodeWithContentDescription("Selected size").performSemanticsAction(SemanticsActions.SetProgress) { it(1.4f) }
        rule.onNodeWithContentDescription("Reset Selected size").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(1.1f, settings(id).portrait.wheel.selectedScale); assertFalse(settings(id).portrait.wheel.autoFit) }
    }
    @Test fun addButtonsFitsUpToSixAndDisablingRetainsTheFittedValues() {
        val id = start()
        rule.onNodeWithContentDescription("Autofit").assertIsOn()
        for (count in 1..6) {
            rule.onNodeWithText("Add button").performScrollTo().assertIsEnabled().performClick()
            rule.onNode(hasText("Volume up") and hasAnyAncestor(isDialog())).performScrollTo().performClick()
            rule.onNode(hasText("Save") and hasAnyAncestor(isDialog())).performClick()
            rule.runOnIdle {
                assertEquals(count, settings(id).buttons.size)
                assertTrue(settings(id).portrait.wheel.autoFit)
                assertNotNull(settings(id).portrait.wheel.innerRadiusDp)
            }
        }
        rule.onNodeWithText("Add button").performScrollTo().assertIsNotEnabled()
        val fitted = rule.runOnIdle { settings(id).portrait.wheel }
        rule.onNodeWithContentDescription("Autofit").performScrollTo().performClick().assertIsOff()
        rule.runOnIdle { assertEquals(fitted.copy(autoFit = false), settings(id).portrait.wheel) }
        rule.onNodeWithContentDescription("Save").performClick()
        rule.waitUntil(10_000) { rule.runOnIdle { app.configuration.state.value!!.panel(id)!!.settings.buttons.size == 6 } }
        rule.runOnIdle { assertEquals(fitted.copy(autoFit = false), app.configuration.state.value!!.panel(id)!!.settings.portrait.wheel) }
    }
    @Test fun manualSizeChangesDisableAutofitOnlyForTheEditedOrientationAndCancelRestoresIt() {
        val id = start()
        rule.runOnIdle {
            val vm = ViewModelProvider(rule.activity)[NudgeViewModel::class.java]
            vm.editPanel(id) { it.copy(landscapeOverride = true) }
            vm.editLayout(id, true) { it.copy(wheel = it.wheel.copy(selectedScale = 1.4f)) }
            assertFalse(settings(id).landscape.wheel.autoFit)
            assertTrue(settings(id).portrait.wheel.autoFit)
            vm.editLayout(id, false) { it.copy(offsetXDp = 20f, wheel = it.wheel.copy(animationsEnabled = false)) }
            assertTrue(settings(id).portrait.wheel.autoFit)
        }
        rule.onNodeWithContentDescription("Outer radius").performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) { it(180f) }
        rule.onNodeWithContentDescription("Autofit").performScrollTo().assertIsOff()
        rule.onNodeWithContentDescription("Cancel").performClick()
        rule.runOnIdle { assertTrue(settings(id).portrait.wheel.autoFit); assertTrue(settings(id).landscape.wheel.autoFit) }
        rule.onNodeWithText("Icon corners").performScrollTo()
        rule.onNodeWithText("Icon shape").assertDoesNotExist()
    }
}
