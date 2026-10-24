package com.modukey.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
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
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w700dp-h900dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WheelRenderingTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @After fun cleanup() { (rule.activity.application as NudgeApplication).onTerminate() }
    @Test fun hoverColorsRenderOnOnlyTheSelectedWedgeAndClearOnExit() {
        val selected = mutableStateOf<Int?>(1)
        val enabled = mutableStateOf(true)
        val layout = PanelLayout(mode = PanelMode.WHEEL, wheel = WheelLayoutSettings(160f, expandSelection = false, animationsEnabled = false))
        rule.activity.setContent { NudgeTheme {
            val panel = PanelConfig(buttons = List(3) { PanelButton(action = ButtonAction.PHONE_SETTINGS,
                icon = IconReference(builtIn = BuiltInIcon.CAMERA)) }, portrait = layout,
                wheelHover = WheelHoverAppearance(enabled = enabled.value, useIcon = true, icon = 0xFF00FF00,
                    useBackground = true, background = 0xFF0000FF, useHighlight = true, highlight = 0xFFFF0000))
            val g = PanelGeometry.calculate(Bounds(0f, 0f, 600f, 600f), 1f, layout, 3, Bounds(0f, 240f, 16f, 120f), Edge.LEFT)
            Box(Modifier.size(g.bounds.width.dp, g.bounds.height.dp)) { PanelContent(panel, layout, g, wheelSelection = selected.value) }
        } }
        fun pixels(): Map<Int, Int> {
            rule.waitForIdle()
            return rule.runOnIdle {
                val view = rule.activity.window.decorView
                val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                try {
                    view.draw(Canvas(bitmap))
                    val data = IntArray(bitmap.width * bitmap.height)
                    bitmap.getPixels(data, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                    data.asList().groupingBy { it }.eachCount()
                } finally { bitmap.recycle() }
            }
        }
        val hovered = pixels()
        for (color in listOf(android.graphics.Color.GREEN, android.graphics.Color.BLUE, android.graphics.Color.RED)) {
            assertTrue("Custom hover channel must render", (hovered[color] ?: 0) > 10)
        }
        rule.runOnIdle { selected.value = null }
        val clear = pixels()
        for (color in listOf(android.graphics.Color.GREEN, android.graphics.Color.BLUE, android.graphics.Color.RED)) assertEquals(0, clear[color] ?: 0)
        rule.runOnIdle { selected.value = 1; enabled.value = false }
        val disabled = pixels()
        for (color in listOf(android.graphics.Color.GREEN, android.graphics.Color.BLUE, android.graphics.Color.RED)) assertEquals(0, disabled[color] ?: 0)
    }
    @Test fun backgroundCornerRadiusChangesTheRenderedFan() {
        val corner = mutableStateOf(0f)
        val layout = PanelLayout(mode = PanelMode.WHEEL, wheel = WheelLayoutSettings(200f))
        rule.activity.setContent { NudgeTheme {
            val panel = PanelConfig(buttons = emptyList(), portrait = layout, background = 0xFF00FF00, opacity = 1f, cornerDp = corner.value)
            val g = PanelGeometry.calculate(Bounds(0f, 0f, 600f, 600f), 1f, layout, 0, Bounds(0f, 240f, 16f, 120f), Edge.LEFT)
            Box(Modifier.size(g.bounds.width.dp, g.bounds.height.dp)) { PanelContent(panel, layout, g) }
        } }
        fun backgroundPixels(): Int {
            rule.waitForIdle()
            return rule.runOnIdle {
                val view = rule.activity.window.decorView
                val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                try {
                    view.draw(Canvas(bitmap))
                    val pixels = IntArray(bitmap.width * bitmap.height)
                    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                    pixels.count { it == 0xFF00FF00.toInt() }
                } finally { bitmap.recycle() }
            }
        }
        val square = backgroundPixels()
        rule.runOnIdle { corner.value = 48f }
        val rounded = backgroundPixels()
        assertTrue("The fan must render its background", rounded > 0)
        assertTrue("Rounded corners must trim the fan background", rounded < square)
    }
    @Test fun allEdgesRenderNormalAndExpandedGuidesWithoutClickableActions() {
        val edge = mutableStateOf(Edge.LEFT)
        val selected = mutableStateOf<Int?>(null)
        val panel = PanelConfig(buttons = listOf(ButtonAction.VOLUME_UP, ButtonAction.VOLUME_DOWN, ButtonAction.MEDIA_MUTE,
            ButtonAction.BRIGHTNESS_AUTOMATIC, ButtonAction.LOCK, ButtonAction.SCREENSHOT).map { PanelButton(action = it) },
            portrait = PanelLayout(mode = PanelMode.WHEEL, wheel = WheelLayoutSettings(200f)))
        rule.activity.setContent { NudgeTheme {
            val g = PanelGeometry.calculate(Bounds(0f, 0f, 600f, 600f), 1f, panel.portrait, 6, Bounds(0f, 240f, 16f, 120f), edge.value)
            Box(Modifier.size(g.bounds.width.dp, g.bounds.height.dp).testTag("wheel-render")) {
                PanelContent(panel, panel.portrait, g, wheelSelection = selected.value)
            }
        } }
        for (direction in Edge.entries) for (highlight in listOf(null, 2)) {
            rule.runOnIdle { edge.value = direction; selected.value = highlight }
            rule.waitForIdle()
            rule.onAllNodes(hasClickAction()).assertCountEquals(0)
            val output = File("build/reports/wheel-previews/${direction.name.lowercase()}-${if (highlight == null) "normal" else "expanded"}.png")
            output.parentFile?.mkdirs()
            rule.runOnIdle {
                val view = rule.activity.window.decorView
                val image = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                view.draw(Canvas(image))
                output.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
                image.recycle()
            }
        }
    }
}
