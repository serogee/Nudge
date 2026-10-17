package com.modukey.app.ui

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
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
    @Test fun `home and handle editor render and new handles start disabled`() {
        val app=ready()
        rule.onNode(hasText("Handles") and hasClickAction()).performClick()
        rule.onNodeWithText("Add handle").performClick()
        rule.runOnIdle { assertEquals(2,app.configuration.state.value!!.handles.size) }
        rule.onNode(hasText("Name") and hasSetTextAction()).performClick().performTextInput("Second")
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
        rule.onNodeWithText("Edit").performClick()
        rule.onNodeWithText("Hex color").assertIsNotEnabled()
        rule.onNodeWithText("Make handle visible").performScrollTo().performClick()
        rule.onNodeWithText("Hex color").assertIsEnabled()
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
}
