package com.modukey.app.ui

import androidx.activity.compose.setContent
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
class PreviewInteractionTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @After fun cleanup() { (rule.activity.application as NudgeApplication).onTerminate() }

    @Test fun `scrolling starts over either mini preview`() {
        for (panel in listOf(false, true)) {
            val scroll = ScrollState(0)
            rule.runOnIdle { rule.activity.setContent { NudgeTheme {
                Column(Modifier.height(400.dp).verticalScroll(scroll)) {
                    val screen = rememberPreviewSpace(false)
                    val settings = TriggerSettings()
                    val handle = TriggerGeometry.bounds(screen.width, screen.height, screen.density, screen.insets, settings, false)
                    if (panel) {
                        val p = PanelConfig()
                        val g = PanelGeometry.calculate(screen.usable, screen.density, p.portrait, 0, handle, Edge.RIGHT)
                        Box(Modifier.testTag("mini")) { PanelPreview(p, p.portrait, g, screen, handle, settings) }
                    } else Box(Modifier.testTag("mini")) { PhonePreview(settings, false) }
                    Spacer(Modifier.height(1000.dp))
                }
            } } }
            rule.onNodeWithTag("mini").performTouchInput { swipeUp() }
            rule.runOnIdle { assertTrue("Page did not scroll over panel=$panel", scroll.value > 0) }
        }
    }

    @Test fun `a drag within a fixed button cancels clicking but a tap and accessibility click work`() {
        var clicks = 0
        rule.runOnIdle { rule.activity.setContent { NudgeTheme {
            PanelButtonFace(PanelButton(), PanelConfig(), 96.dp, interactive = true) { clicks++ }
        } } }
        val button = rule.onNodeWithContentDescription("Volume up")
        button.performTouchInput {
            down(androidx.compose.ui.geometry.Offset(width * .2f, height * .5f))
            moveTo(androidx.compose.ui.geometry.Offset(width * .8f, height * .5f))
            moveTo(androidx.compose.ui.geometry.Offset(width * .2f, height * .5f))
            up()
        }
        rule.runOnIdle { assertEquals(0, clicks) }
        button.performTouchInput { click(center) }
        rule.runOnIdle { assertEquals(1, clicks) }
        button.performClick()
        rule.runOnIdle { assertEquals(2, clicks) }
    }
}
