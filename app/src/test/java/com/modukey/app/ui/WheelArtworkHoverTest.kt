package com.modukey.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.size
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
