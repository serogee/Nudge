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
class WheelInterfaceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @After fun cleanup() { (rule.activity.application as NudgeApplication).onTerminate() }
    private fun ready(): NudgeApplication {
        val app = rule.activity.application as NudgeApplication
        rule.waitUntil(10_000) { rule.runOnIdle { app.configuration.preview.value != null } }
        val wheel = SavedPanel(id = "wheel", name = "Tools", settings = PanelConfig(buttons = List(9) { PanelButton() }, portrait = PanelLayout(mode = PanelMode.WHEEL)))
        val target = GestureTarget.panel(wheel.id)
        rule.runOnIdle { app.configuration.update { AppConfiguration(panels = listOf(SavedPanel(id = "grid", name = "Grid"), wheel),
            handles = listOf(HandleConfig(id = "h", bindings = mapOf(GestureSlot.SWIPE_1 to target, GestureSlot.SWIPE_2 to GestureTarget.VOLUME, GestureSlot.TAP to target)))) } }
        rule.waitForIdle()
        return app
    }
    private fun tab(name: String) = rule.onNode(hasText(name) and hasClickAction()).performClick()
    @Test fun wheelsHaveASeparateTabAndCreateWheelLayouts() {
        val app = ready()
        tab("Panels"); rule.onNodeWithText("Grid").assertExists(); rule.onNodeWithText("Tools").assertDoesNotExist()
        tab("Wheels"); rule.onNodeWithText("Tools").assertExists(); rule.onNodeWithText("Grid").assertDoesNotExist()
        rule.onNodeWithText("Add wheel").performClick()
        rule.onNodeWithContentDescription("Rename wheel").assertExists()
        rule.runOnIdle { assertTrue(app.configuration.preview.value!!.panels.last().isWheel) }
    }
    @Test fun editorsCannotConvertWheelsAndPanelsEvenWithLandscapeOverrides() {
        val app = ready()
        tab("Wheels"); rule.onNodeWithContentDescription("Edit wheel").performClick()
        rule.onNodeWithContentDescription("Outer radius").performScrollTo()
        rule.onNodeWithText("Fixed").assertDoesNotExist()
        rule.runOnIdle {
            val vm = ViewModelProvider(rule.activity)[NudgeViewModel::class.java]
            vm.editLayout("wheel", false) { it.copy(mode = PanelMode.FIXED) }
            vm.editPanel("wheel") { it.copy(landscapeOverride = true) }
            vm.editLayout("wheel", true) { it.copy(mode = PanelMode.SCROLLABLE) }
            val wheel = app.configuration.preview.value!!.panel("wheel")!!
            assertEquals(PanelMode.WHEEL, wheel.settings.portrait.mode)
            assertEquals(PanelMode.WHEEL, wheel.settings.landscape.mode)
            vm.editLayout("grid", false) { it.copy(mode = PanelMode.WHEEL) }
            assertFalse(app.configuration.preview.value!!.panel("grid")!!.isWheel)
        }
    }
    @Test fun wheelCapacityRetainsOverflowAndDraftSaveAndCancelWork() {
        val app = ready(); tab("Wheels")
        rule.onNodeWithContentDescription("Edit wheel").performClick()
        rule.onNodeWithText("Add button").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithContentDescription("Outer radius").performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) { it(180f) }
        rule.runOnIdle {
            assertEquals(180f, app.configuration.preview.value!!.panel("wheel")!!.settings.portrait.wheel.radii(56f).second)
            assertEquals(156f, app.configuration.state.value!!.panel("wheel")!!.settings.portrait.wheel.radii(56f).second)
        }
        rule.onNodeWithContentDescription("Cancel").performClick()
        rule.runOnIdle { assertEquals(156f, app.configuration.preview.value!!.panel("wheel")!!.settings.portrait.wheel.radii(56f).second) }
        rule.onNodeWithContentDescription("Outer radius").performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) { it(180f) }
        rule.onNodeWithContentDescription("Save").performClick()
        rule.waitUntil(10_000) { rule.runOnIdle { app.configuration.state.value!!.panel("wheel")!!.settings.portrait.wheel.radii(56f).second == 180f } }
        rule.runOnIdle { assertEquals(9, app.configuration.state.value!!.panel("wheel")!!.settings.buttons.size) }
        rule.onNodeWithContentDescription("Outer radius").performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) { it(148f) }
        rule.onNodeWithContentDescription("Wheel animations").performScrollTo().performClick()
        rule.runOnIdle {
            assertEquals(148f, app.configuration.preview.value!!.panel("wheel")!!.settings.portrait.wheel.radii(56f).second)
            assertFalse(app.configuration.preview.value!!.panel("wheel")!!.settings.portrait.wheel.animationsEnabled)
            assertTrue(app.configuration.state.value!!.panel("wheel")!!.settings.portrait.wheel.animationsEnabled)
        }
        rule.onNodeWithContentDescription("Cancel").performClick()
        rule.runOnIdle {
            assertEquals(180f, app.configuration.preview.value!!.panel("wheel")!!.settings.portrait.wheel.radii(56f).second)
            assertTrue(app.configuration.preview.value!!.panel("wheel")!!.settings.portrait.wheel.animationsEnabled)
        }
    }
    @Test fun highlightTransitionCanBeChangedAndSavedAsNone() {
        val app = ready(); tab("Wheels")
        rule.onNodeWithContentDescription("Edit wheel").performClick()
        rule.onNodeWithText("Smooth").performScrollTo().performClick()
        rule.onNodeWithText("Spring").performClick()
        rule.runOnIdle { assertEquals(WheelFocusAnimation.SPRING, app.configuration.preview.value!!.panel("wheel")!!.settings.portrait.wheel.focusAnimation) }
        rule.onNodeWithText("Spring").performClick()
        rule.onNode(hasText("No animation") and hasAnyAncestor(isPopup())).performClick()
        rule.onNodeWithContentDescription("Save").performClick()
        rule.waitUntil(10_000) { rule.runOnIdle { app.configuration.state.value!!.panel("wheel")!!.settings.portrait.wheel.focusAnimation == WheelFocusAnimation.NONE } }
    }
    @Test fun wheelPickerOffersDragForSwipesAndExcludesTap() {
        val app = ready(); tab("Handles")
        rule.onNodeWithContentDescription("Edit").performClick()
        rule.onNodeWithText("Gestures").performClick()
        rule.onAllNodesWithText("Wheel: Tools").onFirst().performScrollTo().performClick()
        rule.onNodeWithText("Drag to pick").performScrollTo().performClick()
        rule.onNodeWithText("Allow later swipes").performScrollTo().assertExists()
        rule.onNodeWithText("Save").performClick()
        rule.runOnIdle { assertTrue(app.configuration.preview.value!!.handle("h")!!.target(GestureSlot.SWIPE_1).wheelPick.enabled) }
        rule.onNodeWithText("Volume popup").performScrollTo().assertIsNotEnabled()
        rule.onAllNodesWithText("Wheel: Tools").onLast().performScrollTo().performClick()
        rule.onNodeWithText("Drag to pick").assertDoesNotExist()
    }
    @Test fun wheelOrderingDoesNotReorderPanelsAndDuplicationKeepsTheLayout() {
        val app = ready()
        rule.runOnIdle {
            val vm = ViewModelProvider(rule.activity)[NudgeViewModel::class.java]
            vm.duplicatePanel("wheel")
            val copy = app.configuration.preview.value!!.panels.last()
            vm.reorderPanel(copy.id, 0)
            val panels = app.configuration.preview.value!!.panels
            assertEquals("grid", panels.first().id)
            assertEquals(copy.id, panels[1].id)
            assertTrue(copy.isWheel)
            assertTrue(copy.settings.buttons.map { it.id }.intersect(app.configuration.preview.value!!.panel("wheel")!!.settings.buttons.map { it.id }.toSet()).isEmpty())
        }
    }
    @Test fun changingAwayFromAWheelRestoresSliderControlsBeforeSaving() {
        val app = ready()
        rule.runOnIdle { app.configuration.update { config -> config.editHandle("h") { handle ->
            handle.copy(bindings = handle.bindings + (GestureSlot.SWIPE_1 to handle.target(GestureSlot.SWIPE_1).copy(
                volume = true, wheelPick = WheelPickSettings(enabled = true))))
        } } }
        tab("Handles"); rule.onNodeWithContentDescription("Edit").performClick()
        rule.onNodeWithText("Gestures").performClick()
        val swipeLabel = rule.runOnIdle {
            val config = app.configuration.preview.value!!
            rule.activity.targetLabel(config, config.handle("h")!!.target(GestureSlot.SWIPE_1))
        }
        rule.onNodeWithText(swipeLabel).performScrollTo().performClick()
        rule.onNode(hasText("No action") and hasAnyAncestor(isDialog())).performScrollTo().performClick()
        rule.onNodeWithContentDescription("Slide to adjust").performScrollTo().assertExists()
        rule.onNodeWithText("Cancel").performClick()
        rule.runOnIdle { assertTrue(app.configuration.preview.value!!.handle("h")!!.target(GestureSlot.SWIPE_1).wheelPick.enabled) }
    }
}
