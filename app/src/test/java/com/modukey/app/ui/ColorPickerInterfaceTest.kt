package com.modukey.app.ui

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.modukey.app.MainActivity
import com.modukey.app.NudgeApplication
import com.modukey.app.R
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
class ColorPickerInterfaceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @After fun cleanup() { (rule.activity.application as NudgeApplication).onTerminate() }
    @Test fun longColorTitlesAllowHexEntryAndPaletteSelection() {
        val color = mutableStateOf(0xFF505052L)
        rule.activity.setContent { NudgeTheme { Column { ColorPicker(color.value, title = R.string.active_background_color) { color.value = it } } } }
        rule.onNodeWithContentDescription("Edit Active background color").performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.onNode(hasSetTextAction() and hasAnyAncestor(isDialog())).performTextReplacement("6BB7E8")
        rule.onNodeWithText("Done").performClick()
        rule.runOnIdle { assertEquals(0xFF6BB7E8L, color.value) }
        rule.onNodeWithContentDescription("Edit Active background color").performClick()
        rule.onNodeWithContentDescription("Color #F1BA70").performClick()
        rule.onNodeWithText("Done").performClick()
        rule.runOnIdle { assertEquals(0xFFF1BA70L, color.value) }
    }
}
