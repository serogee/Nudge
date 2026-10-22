package com.modukey.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.modukey.app.MainActivity
import com.modukey.app.NudgeApplication
import com.modukey.app.R
import com.modukey.app.core.*
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
@Config(sdk = [36])
class OptionIndicatorsInterfaceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val app get() = rule.activity.application as NudgeApplication
    @After fun cleanup() { app.onTerminate() }

    @Test @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun pickerBadgeDoesNotInterceptTheActionAndRespectsGestureOptions() {
        val volumeOptions = mutableStateOf(true)
        var chosen: ButtonAction? = null
        rule.activity.setContent { NudgeTheme {
            ActionPicker({}, volumePopupOptions = volumeOptions.value) { chosen = it }
        } }
        val description = app.getString(R.string.action_has_options, app.getString(R.string.volume_up))
        val badge = rule.onNodeWithContentDescription(description, useUnmergedTree = true)
        badge.assertIsDisplayed()
        val output = File("build/outputs/ui-review/action-options-picker.png")
        output.parentFile?.mkdirs()
        val bitmap = rule.runOnIdle {
            val dialog = checkNotNull(ShadowDialog.getLatestDialog())
            val view = checkNotNull(dialog.window).decorView
            Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
        }
        output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        badge.performTouchInput { click() }
        rule.runOnIdle { assertEquals(ButtonAction.VOLUME_UP, chosen); volumeOptions.value = false }
        badge.assertDoesNotExist()
        rule.onNode(hasScrollToIndexAction()).performScrollToIndex(ButtonAction.OPEN_APP.ordinal)
        rule.onNodeWithContentDescription(app.getString(R.string.action_has_options, app.getString(R.string.open_app)), useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun dependentColorOptionsAppearAndRetainTheirValuesWhenTheParentChanges() {
        val savedColor = 0xFFCC8833L
        val panel = mutableStateOf(PanelConfig(activeAppearance = ActiveButtonAppearance(background = savedColor)))
        rule.activity.setContent { NudgeTheme {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                ActiveButtonControls(panel.value) { panel.value = panel.value.copy(activeAppearance = it) }
            }
        } }
        val parent = app.getString(R.string.options_for_setting, app.getString(R.string.use_active_background))
        val custom = app.getString(R.string.options_for_setting, app.getString(R.string.automatic_active_background))
        rule.onNodeWithContentDescription(parent, useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription(custom, useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithContentDescription(app.getString(R.string.automatic_active_background)).performScrollTo().performClick()
        rule.onNodeWithContentDescription(custom, useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription(app.getString(R.string.edit_color, app.getString(R.string.active_background_color)))
            .performScrollTo().assertIsEnabled()
        val output = File("build/outputs/ui-review/dependent-options.png")
        output.parentFile?.mkdirs()
        val bitmap = rule.runOnIdle {
            val view = rule.activity.window.decorView
            Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
        }
        output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        rule.onNodeWithContentDescription(app.getString(R.string.automatic_active_background)).performScrollTo().performClick()
        rule.onNodeWithContentDescription(custom, useUnmergedTree = true).assertDoesNotExist()
        rule.runOnIdle { assertEquals(savedColor, panel.value.activeAppearance.background) }
        rule.onNodeWithContentDescription(app.getString(R.string.use_active_background)).performScrollTo().performClick()
        rule.onNodeWithContentDescription(parent, useUnmergedTree = true).assertDoesNotExist()
    }
}
