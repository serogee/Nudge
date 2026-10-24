package com.modukey.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.modukey.app.MainActivity
import com.modukey.app.NudgeApplication
import com.modukey.app.R
import java.io.File
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36])
class SliderValueInterfaceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @After fun cleanup() { (rule.activity.application as NudgeApplication).onTerminate() }
    private fun entry() = rule.onNode(hasSetTextAction() and hasAnyAncestor(isDialog()))

    @Test @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun percentReadoutSavesTheDisplayedScaleAndTracksSliderChanges() {
        val value = mutableStateOf(.5f)
        var finishes = 0
        rule.activity.setContent { NudgeTheme {
            CompositionLocalProvider(LocalFinishAdjustment provides { finishes++ }) {
                PercentSlider(R.string.opacity, value.value, default = .65f) { value.value = it }
            }
        } }
        rule.onNodeWithText("50%").performClick()
        entry().assertTextEquals("50").performTextReplacement("75")
        rule.runOnIdle { assertEquals(.5f, value.value); assertEquals(0, finishes) }
        val output = File("build/outputs/ui-review/slider-value-entry.png")
        output.parentFile?.mkdirs()
        val bitmap = rule.runOnIdle {
            val view = checkNotNull(ShadowDialog.getLatestDialog().window).decorView
            Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
        }
        output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        rule.onNodeWithText("Save").performClick()
        rule.runOnIdle { assertEquals(.75f, value.value); assertEquals(1, finishes) }
        rule.onNodeWithText("75%").assertExists()
        rule.onNodeWithContentDescription("Opacity").performSemanticsAction(SemanticsActions.SetProgress) { it(.42f) }
        rule.onNodeWithText("42%").performClick()
        entry().assertTextEquals("42")
        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithContentDescription("Reset Opacity").performClick()
        rule.onNodeWithText("65%").assertExists()
    }

    @Test fun iconCornersUseTheFullZeroToOneHundredPercentRange() {
        val value = mutableStateOf(.25f)
        rule.activity.setContent { NudgeTheme {
            SettingSlider(R.string.icon_corners, value.value, 0f.. .5f, t(R.string.percent, (value.value * 200).toInt()),
                valueScale = 200f) { value.value = it }
        } }
        rule.onNodeWithText("50%").performClick()
        entry().performTextReplacement("100")
        rule.onNodeWithText("Save").performClick()
        rule.runOnIdle { assertEquals(.5f, value.value) }
        rule.onNodeWithText("100%").performClick()
        entry().performTextReplacement("0")
        entry().performImeAction()
        rule.runOnIdle { assertEquals(0f, value.value) }
    }

    @Test fun manualPercentEntryUsesEachSlidersOwnMinimumAndMaximum() {
        val value = mutableStateOf(.25f)
        rule.activity.setContent { NudgeTheme {
            SettingSlider(R.string.icon_corners, value.value, .15f.. .5f, t(R.string.percent, (value.value * 100).toInt()),
                valueScale = 100f) { value.value = it }
        } }
        rule.onNodeWithText("25%").performClick()
        entry().performTextReplacement("-20")
        rule.onNodeWithText("Save").performClick()
        rule.runOnIdle { assertEquals(.15f, value.value) }
        rule.onNodeWithText("15%").performClick()
        entry().performTextReplacement("900")
        entry().performImeAction()
        rule.runOnIdle { assertEquals(.5f, value.value) }
        rule.onNodeWithText("50%").performClick()
        entry().performTextReplacement("25,5")
        rule.onNodeWithText("Save").performClick()
        rule.runOnIdle { assertEquals(.255f, value.value) }
    }

    @Test fun dpEntrySupportsSignedDecimalsAndClampsAtBothLimits() {
        val value = mutableStateOf(0f)
        rule.activity.setContent { NudgeTheme {
            SettingSlider(R.string.offset_x, value.value, -240f..240f, t(R.string.dp_value, value.value.toInt())) { value.value = it }
        } }
        for ((input, expected) in listOf("-12.5" to -12.5f, "-9999" to -240f, "9999" to 240f)) {
            rule.onNodeWithContentDescription("Edit X offset value").performClick()
            entry().performTextReplacement(input)
            entry().performImeAction()
            rule.runOnIdle { assertEquals(expected, value.value) }
        }
    }

    @Test fun invalidDraftsAndCancelDoNotChangeTheValue() {
        val value = mutableStateOf(.5f)
        var finishes = 0
        rule.activity.setContent { NudgeTheme {
            CompositionLocalProvider(LocalFinishAdjustment provides { finishes++ }) {
                PercentSlider(R.string.opacity, value.value) { value.value = it }
            }
        } }
        rule.onNodeWithText("50%").performClick()
        for (input in listOf("", "text", "NaN", "Infinity", "1e999")) {
            entry().performTextReplacement(input)
            rule.onNodeWithText("Save").assertIsNotEnabled()
            entry().performImeAction()
            rule.onNode(isDialog()).assertExists()
            rule.runOnIdle { assertEquals(.5f, value.value); assertEquals(0, finishes) }
        }
        entry().performTextReplacement("80")
        rule.onNodeWithText("Cancel").performClick()
        rule.runOnIdle { assertEquals(.5f, value.value); assertEquals(0, finishes) }
        rule.onNodeWithText("50%").performClick()
        entry().assertTextEquals("50")
        rule.onNodeWithText("Cancel").performClick()
    }

    @Test fun disablingTheSliderDiscardsAnOpenEntryAndPreventsEditing() {
        val value = mutableStateOf(.5f)
        val enabled = mutableStateOf(false)
        rule.activity.setContent { NudgeTheme {
            PercentSlider(R.string.opacity, value.value, enabled = enabled.value) { value.value = it }
        } }
        rule.onNodeWithText("50%").assertIsNotEnabled().performClick()
        rule.onNode(isDialog()).assertDoesNotExist()
        rule.runOnIdle { enabled.value = true }
        rule.onNodeWithText("50%").performClick()
        entry().performTextReplacement("80")
        rule.runOnIdle { enabled.value = false }
        rule.onNode(isDialog()).assertDoesNotExist()
        rule.runOnIdle { enabled.value = true; assertEquals(.5f, value.value) }
        rule.onNode(isDialog()).assertDoesNotExist()
        rule.onNodeWithText("50%").performClick()
        entry().assertTextEquals("50")
        rule.onNodeWithText("Cancel").performClick()
    }

    @Test fun changedLimitsAreAppliedWhenAnOpenEntryIsSaved() {
        val maximum = mutableStateOf(480f)
        val value = mutableStateOf(120f)
        rule.activity.setContent { NudgeTheme {
            SettingSlider(R.string.touch_length, value.value, 48f..maximum.value, t(R.string.dp_value, value.value.toInt())) { value.value = it }
        } }
        rule.onNodeWithText("120 dp").performClick()
        entry().performTextReplacement("400")
        rule.runOnIdle { maximum.value = 240f }
        rule.onNodeWithText("Save").performClick()
        rule.runOnIdle { assertEquals(240f, value.value) }
    }
}
