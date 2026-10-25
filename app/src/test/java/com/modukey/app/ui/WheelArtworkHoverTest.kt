package com.modukey.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w480dp-h900dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WheelArtworkHoverTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @After fun cleanup() { (rule.activity.application as NudgeApplication).onTerminate() }
    @Test fun squareWheelArtworkKeepsTheCornersOutsideItsCircularTouchTarget() {
        val corners = mutableStateOf(0f)
        val layout = PanelLayout(mode = PanelMode.WHEEL, wheel = WheelLayoutSettings(innerRadiusDp = 1f, outerRadiusDp = 200f,
            expandSelection = false, animationsEnabled = false, adaptIconSize = false, iconSizeDp = 96f))
        rule.activity.setContent { NudgeTheme {
            val panel = PanelConfig(buttons = listOf(PanelButton(action = ButtonAction.NUDGE_SETTINGS)), portrait = layout,
                iconCornerFraction = corners.value, wheelHover = WheelHoverAppearance(enabled = false))
            val g = PanelGeometry.calculate(Bounds(0f, 0f, 600f, 600f), 1f, layout, 1, Bounds(0f, 240f, 16f, 120f), Edge.LEFT)
            Box(Modifier.size(g.bounds.width.dp, g.bounds.height.dp)) { PanelContent(panel, layout, g) }
        } }
        fun artworkPixels(): Int {
            rule.waitForIdle()
            return rule.runOnIdle {
                val view = rule.activity.window.decorView
                val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                try {
                    view.draw(Canvas(bitmap))
                    val data = IntArray(bitmap.width * bitmap.height)
                    bitmap.getPixels(data, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                    data.count { it == 0xFFDDF6A5.toInt() }
                } finally { bitmap.recycle() }
            }
        }
        val square = artworkPixels()
        rule.runOnIdle { corners.value = .5f }
        val circular = artworkPixels()
        assertTrue(circular > 0)
        assertTrue("The touch target must not crop square artwork to a circle", square > circular + 20)
    }
    @Test fun artworkKeepsItsColorsUntilExplicitlyTintedAndRestoresThemAfterHover() {
        val tint = mutableStateOf<Color?>(null)
        rule.activity.setContent { NudgeTheme {
            ButtonGraphic(PanelButton(action = ButtonAction.NUDGE_SETTINGS), "", Modifier.size(64.dp), imageTint = tint.value)
        } }
        fun greenPixels(): Int {
            rule.waitForIdle()
            return rule.runOnIdle {
                val view = rule.activity.window.decorView
                val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                try {
                    view.draw(Canvas(bitmap))
                    val data = IntArray(bitmap.width * bitmap.height)
                    bitmap.getPixels(data, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                    data.count { it == android.graphics.Color.GREEN }
                } finally { bitmap.recycle() }
            }
        }
        assertEquals(0, greenPixels())
        rule.runOnIdle { tint.value = Color.Green }
        assertTrue(greenPixels() > 10)
        rule.runOnIdle { tint.value = null }
        assertEquals(0, greenPixels())
    }
}
