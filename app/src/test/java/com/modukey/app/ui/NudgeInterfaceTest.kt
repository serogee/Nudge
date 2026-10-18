package com.modukey.app.ui

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModelProvider
import com.modukey.app.MainActivity
import com.modukey.app.NudgeApplication
import com.modukey.app.core.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[36])
class NudgeInterfaceTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    @After fun stopApplicationScope() { (rule.activity.application as NudgeApplication).onTerminate() }
    private fun ready():NudgeApplication {
        val app=rule.activity.application as NudgeApplication
        rule.waitUntil(10_000) { rule.runOnIdle { app.configuration.state.value!=null || app.configuration.error.value!=null } }
        rule.runOnIdle { assertNull("Configuration load failed",app.configuration.error.value); assertNotNull(app.configuration.state.value) }
        rule.runOnIdle { app.configuration.update { AppConfiguration() } }
        rule.waitForIdle()
        return app
    }
    private fun chooseAction(action:ButtonAction) {
        rule.onNode(hasScrollToIndexAction()).performScrollToIndex(action.ordinal)
        rule.onNodeWithText(rule.activity.getString(label(action))).performClick()
    }
    @Test fun `home and handle editor render and new handles start disabled`() {
        val app=ready()
        rule.onNode(hasText("Handles") and hasClickAction()).performClick()
        rule.onNodeWithText("Add handle").performClick()
        rule.runOnIdle { assertEquals(2,app.configuration.state.value!!.handles.size) }
        rule.onNodeWithContentDescription("Rename handle").performClick()
        rule.onNode(hasText("Name") and hasSetTextAction()).performTextInput("Second")
        rule.onNodeWithText("Save").performClick()
        rule.runOnIdle {
            val handles=app.configuration.state.value!!.handles
            assertEquals("Second",handles.last().name); assertFalse(handles.last().enabled)
            assertNotEquals(handles.first().id,handles.last().id)
        }
        rule.onNodeWithText("Volume, within reach.").assertDoesNotExist()
    }
    @Test fun `hidden handle appearance controls stay disabled until enabled`() {
        ready()
        rule.onNode(hasText("Handles") and hasClickAction()).performClick()
        rule.onNodeWithContentDescription("Edit").performClick()
        rule.onNodeWithText("Hex color").assertIsNotEnabled()
        rule.onNodeWithText("Make handle visible").performScrollTo().performClick()
        rule.onNodeWithText("Hex color").assertIsEnabled()
    }
    @Test fun `home shows permission details and info navigation`() {
        ready()
        rule.onNodeWithText("App Settings").assertExists()
        rule.onNodeWithText("Enable Gestures").assertExists()
        rule.onNodeWithText("Accessibility is off").assertExists()
        rule.onNodeWithText("Service is disconnected").assertExists()
        rule.onNodeWithText("Info").performClick()
        rule.onNodeWithText("Backup").assertExists()
        rule.onNodeWithContentDescription("Show setup steps").performScrollTo().performClick()
        rule.onNodeWithText("Open Background autostart").assertExists()
    }
    @Test fun `reset changes only one field and header tabs remain available after scroll`() {
        val app = ready()
        rule.runOnIdle { app.configuration.update { it.editHandle("first-handle") { h -> h.copy(trigger = h.trigger.copy(lengthDp = 172f, depthDp = 22f)) } } }
        rule.onNode(hasText("Handles") and hasClickAction()).performClick()
        rule.onNodeWithContentDescription("Edit").performClick()
        rule.onNodeWithContentDescription("Reset Touch length").performScrollTo().performClick()
        rule.runOnIdle {
            val trigger = app.configuration.state.value!!.handles.first().trigger
            assertEquals(120f, trigger.lengthDp); assertEquals(22f, trigger.depthDp)
        }
        rule.onNodeWithContentDescription("Reset Touch length").assertDoesNotExist()
        rule.onNodeWithText("Gestures").assertIsDisplayed().performClick()
        rule.onNodeWithText("Swipe 1").assertExists()
        rule.onNodeWithContentDescription("Rename handle").assertIsDisplayed()
    }
    @Test fun `follow controls hide unused coordinates and retain both saved setups`() {
        val app = ready()
        val panel = SavedPanel(name = "Tools", settings = PanelConfig(portrait = PanelLayout(x = .23f, y = .72f, offsetXDp = -24f, offsetYDp = 18f)))
        rule.runOnIdle { app.configuration.update { it.copy(panels = listOf(panel)) } }
        rule.onNode(hasText("Panels") and hasClickAction()).performClick()
        rule.onAllNodesWithText("No buttons set up").assertCountEquals(1)
        rule.onNodeWithContentDescription("Edit panel").performScrollTo().assertIsDisplayed().performClick()
        rule.onNodeWithText("Follow handle").performScrollTo()
        rule.onNodeWithContentDescription("Rename panel").assertIsDisplayed()
        rule.onNodeWithText("X offset").assertExists()
        rule.onNodeWithText("Horizontal center").assertDoesNotExist()
        rule.onNodeWithText("Follow handle").performClick()
        rule.onNodeWithText("X offset").assertDoesNotExist()
        rule.onNodeWithText("Horizontal center").assertExists()
        rule.onNodeWithText("Follow handle").performClick()
        rule.onNodeWithText("X offset").assertExists()
        rule.runOnIdle {
            assertEquals(panel.settings.portrait, app.configuration.state.value!!.panels.single().settings.portrait)
        }
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithText("Manage Panels").assertExists()
        rule.onAllNodesWithText("No buttons set up").assertCountEquals(1)
    }
    @Test fun `panels start empty and can be created renamed and deleted independently`() {
        val app = ready()
        rule.onNode(hasText("Panels") and hasClickAction()).performClick()
        rule.onNodeWithText("Manage Panels").assertExists()
        rule.onNodeWithContentDescription("Edit panel").assertDoesNotExist()
        rule.onNodeWithText("Add panel").performClick()
        rule.runOnIdle { assertEquals(1,app.configuration.state.value!!.panels.size) }
        rule.onNodeWithContentDescription("Rename panel").performClick()
        rule.onNode(hasText("Name") and hasSetTextAction()).performTextInput("Tools")
        rule.onNodeWithText("Save").performClick()
        rule.runOnIdle {
            assertEquals("Tools",app.configuration.state.value!!.panels.single().name)
            assertEquals(1,app.configuration.state.value!!.handles.size)
        }
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithText("Tools").assertExists()
        rule.onNodeWithContentDescription("Delete panel").performScrollTo().assertIsDisplayed().performClick()
        rule.runOnIdle { assertEquals(1,app.configuration.state.value!!.panels.size) }
        rule.onNodeWithText("Delete").performClick()
        rule.runOnIdle { assertTrue(app.configuration.state.value!!.panels.isEmpty()) }
        rule.onNode(hasText("Handles") and hasClickAction()).performClick()
        rule.onNodeWithContentDescription("Edit").performClick()
        rule.onNodeWithText("Gestures").assertExists()
        rule.onNodeWithText("Availability").assertExists()
        rule.onNodeWithContentDescription("Edit panel").assertDoesNotExist()
    }
    @Test fun `gesture picker saves panel plus volume volume only and no action`() {
        val app = ready()
        val panel = SavedPanel(name = "Tools")
        rule.runOnIdle { app.configuration.update { it.copy(panels = listOf(panel)) } }
        rule.onNode(hasText("Handles") and hasClickAction()).performClick()
        rule.onNodeWithContentDescription("Edit").performClick()
        rule.onNodeWithText("Gestures").performClick()
        rule.onNodeWithText("Volume popup").performScrollTo().assertIsDisplayed().performClick()
        rule.onNodeWithText("Panel: Select panel").performClick()
        rule.onNodeWithText("Search panels").assertExists()
        rule.onNodeWithText("Tools").performClick()
        rule.onNodeWithContentDescription("Show Volume Popup").assertIsOn()
        rule.runOnIdle { assertEquals(GestureTarget.VOLUME,app.configuration.state.value!!.handles.single().target(GestureSlot.SWIPE_1)) }
        rule.onNodeWithText("Save").performClick()
        rule.runOnIdle { assertEquals(GestureTarget.panel(panel.id,true),app.configuration.state.value!!.handles.single().target(GestureSlot.SWIPE_1)) }
        rule.onNodeWithText("Panel: Tools + volume popup").performClick()
        rule.onNode(hasText("No action") and hasAnyAncestor(isDialog())).performClick()
        rule.onNodeWithText("Show Volume Popup").performClick()
        rule.onNodeWithText("Save").performClick()
        rule.runOnIdle { assertEquals(GestureTarget.NONE,app.configuration.state.value!!.handles.single().target(GestureSlot.SWIPE_1)) }
        rule.onAllNodesWithText("No action").onFirst().performClick()
        rule.onNodeWithText("Show Volume Popup").performClick()
        rule.onNodeWithText("Save").performClick()
        rule.runOnIdle { assertEquals(GestureTarget.VOLUME,app.configuration.state.value!!.handles.single().target(GestureSlot.SWIPE_1)) }
    }
    @Test fun `panel auto close controls retain their duration while disabled and reset one field`() {
        val app=ready()
        val panel=SavedPanel(name="Tools",settings=PanelConfig(autoCloseMs=7_500,cornerDp=13f))
        rule.runOnIdle { app.configuration.update { it.copy(panels=listOf(panel)) } }
        rule.onNode(hasText("Panels") and hasClickAction()).performClick()
        rule.onNodeWithContentDescription("Edit panel").performScrollTo().performClick()
        rule.onNodeWithText("Close after").assertDoesNotExist()
        rule.onNodeWithText("Close automatically").performScrollTo().performClick()
        rule.onNodeWithText("Close after").assertExists()
        rule.runOnIdle { assertEquals(panel.settings.copy(autoCloseEnabled=true),app.configuration.state.value!!.panels.single().settings) }
        rule.onNodeWithText("Close automatically").performScrollTo().performClick()
        rule.onNodeWithText("Close after").assertDoesNotExist()
        rule.runOnIdle { assertEquals(panel,app.configuration.state.value!!.panels.single()) }
        rule.onNodeWithText("Close automatically").performClick()
        rule.onNodeWithContentDescription("Reset Close after").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(panel.settings.copy(autoCloseEnabled=true,autoCloseMs=3_000),app.configuration.state.value!!.panels.single().settings) }
    }
    @Test fun `direct gesture action stages lock and app choices until save and cancels without changes`() {
        val app=ready()
        val launcher=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        shadowOf(app.packageManager).addResolveInfoForIntent(launcher,ResolveInfo().apply {
            nonLocalizedLabel="Test camera"
            activityInfo=ActivityInfo().apply {
                packageName="test.camera"; name="test.camera.Main"; exported=true
                applicationInfo=ApplicationInfo().apply { packageName="test.camera" }
            }
        })
        rule.onNode(hasText("Handles") and hasClickAction()).performClick()
        rule.onNodeWithContentDescription("Edit").performClick()
        rule.onNodeWithText("Gestures").performClick()
        rule.onNodeWithText("Volume popup").performScrollTo().performClick()
        rule.onNodeWithText("Action: Direct action").performClick()
        chooseAction(ButtonAction.LOCK)
        rule.runOnIdle { assertEquals(GestureTarget.VOLUME,app.configuration.state.value!!.handles.single().target(GestureSlot.SWIPE_1)) }
        rule.onNodeWithText("Save").performClick()
        rule.runOnIdle { assertEquals(ButtonAction.LOCK,app.configuration.state.value!!.handles.single().target(GestureSlot.SWIPE_1).action!!.action) }
        rule.onNodeWithText("Action: Lock screen + volume popup").performClick()
        rule.onNode(hasText("Action: Lock screen") and hasAnyAncestor(isDialog())).performClick()
        chooseAction(ButtonAction.OPEN_APP)
        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithText("Save").assertIsEnabled()
        rule.onNode(hasText("Action: Lock screen") and hasAnyAncestor(isDialog())).performClick()
        chooseAction(ButtonAction.OPEN_APP)
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Test camera").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Test camera").performClick()
        rule.onNodeWithText("Save").performClick()
        val saved=rule.runOnIdle { app.configuration.state.value!!.handles.single() }
        assertEquals(ButtonAction.OPEN_APP,saved.target(GestureSlot.SWIPE_1).action!!.action)
        assertEquals("test.camera",saved.target(GestureSlot.SWIPE_1).action!!.appPackage)
        assertEquals("test.camera.Main",saved.target(GestureSlot.SWIPE_1).action!!.appActivity)
        rule.onNodeWithText("Action: Test camera + volume popup").performClick()
        rule.onNode(hasText("Action: Test camera") and hasAnyAncestor(isDialog())).performClick()
        chooseAction(ButtonAction.LOCK)
        rule.onNodeWithText("Cancel").performClick()
        rule.runOnIdle { assertEquals(saved,app.configuration.state.value!!.handles.single()) }
    }
    @Test fun `direct volume action saves popup option without changing another binding and home shows required policy access`() {
        val app=ready()
        val other=GestureTarget.direct(PanelButton(action=ButtonAction.DND))
        rule.runOnIdle { app.configuration.update { it.editHandle(it.handles.single().id) { h -> h.copy(bindings=h.bindings+(GestureSlot.HOLD to other)) } } }
        rule.onNodeWithText("Do Not Disturb access is needed").assertExists()
        rule.onNode(hasText("Handles") and hasClickAction()).performClick()
        rule.onNodeWithContentDescription("Edit").performClick()
        rule.onNodeWithText("Gestures").performClick()
        rule.onNodeWithText("Volume popup").performScrollTo().performClick()
        rule.onNodeWithText("Action: Direct action").performClick()
        rule.onNodeWithText("Volume up").performClick()
        rule.onNodeWithContentDescription("Show Volume Popup").assertIsOn()
        rule.onNodeWithText("Save").performClick()
        rule.runOnIdle {
            val handle=app.configuration.state.value!!.handles.single()
            assertEquals(ButtonAction.VOLUME_UP,handle.target(GestureSlot.SWIPE_1).action!!.action)
            assertTrue(handle.target(GestureSlot.SWIPE_1).volume)
            assertEquals(other,handle.target(GestureSlot.HOLD))
        }
    }
    @Test fun `panel open and close selections change and reset independently`() {
        val app=ready()
        val panel=SavedPanel(settings=PanelConfig(animation=PanelAnimation.FADE,closeAnimation=PanelAnimation.SLIDE))
        rule.runOnIdle { app.configuration.update { it.copy(panels=listOf(panel)) } }
        rule.onNode(hasText("Panels") and hasClickAction()).performClick()
        rule.onNodeWithContentDescription("Edit panel").performScrollTo().performClick()
        rule.onNodeWithText("Slide and fade").performScrollTo().performClick()
        rule.onNodeWithText("No animation").assertExists()
        rule.onNodeWithText("Scale, slide, and fade").performClick()
        rule.runOnIdle { assertEquals(panel.settings.copy(closeAnimation=PanelAnimation.SCALE),app.configuration.state.value!!.panels.single().settings) }
        rule.onNodeWithContentDescription("Reset Close animation").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(panel.settings.copy(closeAnimation=PanelAnimation.NONE),app.configuration.state.value!!.panels.single().settings) }
        rule.onNodeWithContentDescription("Reset Open animation").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(panel.settings.copy(animation=PanelAnimation.NONE,closeAnimation=PanelAnimation.NONE),app.configuration.state.value!!.panels.single().settings) }
    }
    @Test fun `left grip reorders handles without changing their settings`() {
        val app = ready()
        val one = HandleConfig(name = "One", enabled = true)
        val two = HandleConfig(name = "Two", enabled = false)
        rule.runOnIdle { app.configuration.update { it.copy(handles = listOf(one, two)) } }
        val vm = rule.runOnIdle { ViewModelProvider(rule.activity)[NudgeViewModel::class.java] }
        rule.runOnIdle { rule.activity.setContent { NudgeTheme {
            val handles = app.configuration.state.collectAsState().value!!.handles
            ReorderableList(handles, { it.id }, { it.name }, Modifier.fillMaxWidth().height(300.dp), vm::reorderHandle, vm::flush) { h, _ ->
                Text(h.name, Modifier.height(80.dp))
            }
        } } }
        val grip = rule.onNodeWithContentDescription("Drag to reorder One")
        val a = grip.fetchSemanticsNode().boundsInRoot.center.y
        val b = rule.onNodeWithContentDescription("Drag to reorder Two").fetchSemanticsNode().boundsInRoot.center.y
        grip.performTouchInput { swipe(center, center + Offset(0f, b - a), durationMillis = 500) }
        rule.runOnIdle { assertEquals(listOf(two.normalized(), one.normalized()), app.configuration.state.value!!.handles) }
        val third = HandleConfig(name = "Three")
        rule.runOnIdle {
            app.configuration.update { it.copy(handles = it.handles + third) }
            vm.reorderHandle(third.id, 1)
            vm.reorderHandle(third.id, 1)
            assertEquals(listOf(two.normalized(), third.normalized(), one.normalized()), app.configuration.state.value!!.handles)
        }
    }
    @Test fun `icon library works without an icon pack`() {
        ready()
        var picked: IconReference? = null
        rule.runOnIdle { rule.activity.setContent { NudgeTheme { IconPicker(emptyList(), {}) { picked = it } } } }
        rule.onNodeWithText("Camera").performClick()
        rule.runOnIdle { assertEquals(IconReference(builtIn = BuiltInIcon.CAMERA), picked) }
    }
    @Test fun `button editor keeps volume and container changes private until save`() {
        ready()
        val original = PanelButton(action = ButtonAction.VOLUME_UP)
        var saved: PanelButton? = null
        val vm = rule.runOnIdle { ViewModelProvider(rule.activity)[NudgeViewModel::class.java] }
        rule.runOnIdle { rule.activity.setContent { NudgeTheme { ButtonEditor(original, PanelConfig(), vm, {}) { saved = it } } } }
        rule.onNodeWithText("Show Volume Popup").performScrollTo().performClick()
        rule.runOnIdle { assertNull(saved) }
        rule.onNodeWithText("Panel default").performScrollTo().performClick()
        rule.onNodeWithText("Standalone").performClick()
        rule.runOnIdle { assertNull(saved) }
        rule.onNodeWithText("Save").performClick()
        rule.runOnIdle {
            assertEquals(original.copy(showVolumePopup = true, iconContainer = IconContainer.STANDALONE), saved)
            assertFalse(original.showVolumePopup)
        }
    }
    @Test fun `fixed runtime panel hides overflow has no scroll action and survives mode changes`() {
        ready()
        val layout=mutableStateOf(PanelLayout(crossCount=2,maximumLines=1))
        val panel=PanelConfig(buttons=listOf(PanelButton(action=ButtonAction.VOLUME_UP),PanelButton(action=ButtonAction.VOLUME_DOWN),
            PanelButton(action=ButtonAction.LOCK),PanelButton(action=ButtonAction.PHONE_SETTINGS)))
        val density=rule.activity.resources.displayMetrics.density
        var activations=0
        rule.runOnIdle { rule.activity.setContent { NudgeTheme {
            val l=layout.value
            val g=PanelGeometry.calculate(Bounds(0f,0f,400*density,700*density),density,l,panel.buttons.size,Bounds(0f,0f,16*density,120*density),Edge.LEFT)
            Box(Modifier.size((g.bounds.width/density).dp,(g.bounds.height/density).dp)) { PanelContent(panel,l,g,interactive=true) { activations++ } }
        } } }
        rule.onNodeWithContentDescription("Volume up").performClick()
        rule.onNodeWithContentDescription("Lock screen").assertDoesNotExist()
        rule.onAllNodes(hasScrollAction()).assertCountEquals(0)
        rule.runOnIdle { assertEquals(1,activations); layout.value=layout.value.copy(mode=PanelMode.SCROLLABLE) }
        rule.onNodeWithContentDescription("Lock screen").assertExists()
        rule.runOnIdle { layout.value=layout.value.copy(mode=PanelMode.FIXED) }
        rule.onNodeWithContentDescription("Lock screen").assertDoesNotExist()
        rule.onAllNodes(hasScrollAction()).assertCountEquals(0)
        assertEquals(4,panel.buttons.size)
    }
    @Test fun `horizontal content fills down while vertical content fills across`() {
        ready()
        val direction=mutableStateOf(PanelDirection.VERTICAL)
        val p=PanelConfig(buttons=listOf(PanelButton(action=ButtonAction.VOLUME_UP),PanelButton(action=ButtonAction.VOLUME_DOWN)))
        val d=rule.activity.resources.displayMetrics.density
        rule.runOnIdle { rule.activity.setContent { NudgeTheme {
            val l=PanelLayout(crossCount=2,direction=direction.value)
            val g=PanelGeometry.calculate(Bounds(0f,0f,400*d,700*d),d,l,p.buttons.size,Bounds(0f,0f,16*d,120*d),Edge.LEFT)
            Box(Modifier.size((g.bounds.width/d).dp,(g.bounds.height/d).dp)) { PanelContent(p,l,g) }
        } } }
        val first=rule.onNodeWithContentDescription("Volume up").fetchSemanticsNode().boundsInRoot
        val second=rule.onNodeWithContentDescription("Volume down").fetchSemanticsNode().boundsInRoot
        assertTrue(second.left>first.left); assertEquals(first.top,second.top)
        rule.runOnIdle { direction.value=PanelDirection.HORIZONTAL }
        val a=rule.onNodeWithContentDescription("Volume up").fetchSemanticsNode().boundsInRoot
        val b=rule.onNodeWithContentDescription("Volume down").fetchSemanticsNode().boundsInRoot
        assertTrue(b.top>a.top); assertEquals(a.left,b.left)
    }
    @Test fun `sticky preview switches change editor overlays without changing saved configuration`() {
        val app = ready()
        val panel = SavedPanel(name = "Tools")
        rule.runOnIdle {
            app.configuration.update { it.copy(panels = listOf(panel)) }
            app.status.report(com.modukey.app.platform.ServiceState(connected = true))
        }
        val original = rule.runOnIdle { app.configuration.state.value }
        rule.onNode(hasText("Handles") and hasClickAction()).performClick()
        rule.onNodeWithContentDescription("Edit").performClick()
        rule.onNodeWithContentDescription("On-phone preview").assertIsOn().performClick()
        rule.runOnIdle { assertEquals(false, app.editor.state.value?.preview); assertEquals(original, app.configuration.state.value) }
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("On-phone preview").assertIsOff()
        rule.onNodeWithText("Make handle visible").performScrollTo()
        rule.onNodeWithContentDescription("On-phone preview").assertIsDisplayed().performClick()
        rule.runOnIdle { assertEquals(true, app.editor.state.value?.preview) }
        rule.onNodeWithText("Gestures").performClick()
        rule.onNodeWithContentDescription("On-phone preview").assertDoesNotExist()
        rule.runOnIdle { assertEquals(false, app.editor.state.value?.preview) }
        rule.onNode(hasText("Panels") and hasClickAction()).performClick()
        rule.onNodeWithContentDescription("Edit panel").performScrollTo().performClick()
        rule.onNodeWithText("Follow handle").performScrollTo()
        rule.onNodeWithContentDescription("On-phone preview").assertIsDisplayed().performClick()
        rule.runOnIdle { assertEquals(false, app.editor.state.value?.preview); assertEquals(original, app.configuration.state.value) }
    }

    @Test fun `empty button editor saves only its own appearance`() {
        ready()
        val original = PanelButton(action = ButtonAction.EMPTY)
        var saved: PanelButton? = null
        val vm = rule.runOnIdle { ViewModelProvider(rule.activity)[NudgeViewModel::class.java] }
        rule.runOnIdle { rule.activity.setContent { NudgeTheme { ButtonEditor(original, PanelConfig(), vm, {}) { saved = it } } } }
        rule.onNodeWithText("Slash icon").performScrollTo().performClick()
        rule.onNodeWithText("No button").performClick()
        rule.runOnIdle { assertNull(saved) }
        rule.onNodeWithText("Save").performClick()
        rule.runOnIdle {
            assertEquals(original.copy(emptyAppearance = EmptyAppearance.NO_BUTTON), saved)
            assertEquals(EmptyAppearance.SLASH, original.emptyAppearance)
        }
    }
}
