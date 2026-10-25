package com.modukey.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
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
@Config(sdk = [36], qualifiers = "w1000dp-h1000dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WheelRedesignPreviewTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @After fun cleanup() { (rule.activity.application as NudgeApplication).onTerminate() }
    private val buttons = listOf(BuiltInIcon.CAMERA, BuiltInIcon.MUSIC, BuiltInIcon.WALLET,
        BuiltInIcon.MAP, BuiltInIcon.CALENDAR, BuiltInIcon.PHONE).map { PanelButton(action = ButtonAction.OPEN_APP,
        appLabel = it.name.lowercase(), icon = IconReference(builtIn = it)) }

    @Composable private fun Sample(title: String, radius: Float, count: Int, selected: Int? = null) {
        Column(Modifier.width(456.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge)
            val density = LocalDensity.current.density
            val layout = PanelLayout(mode = PanelMode.WHEEL, wheel = WheelLayoutSettings(radius))
            val panel = PanelConfig(buttons = buttons.take(count), portrait = layout)
            val g = PanelGeometry.calculate(Bounds(0f, 0f, 450f * density, 450f * density), density, layout,
                count, Bounds(200f, 400f, 100f, 12f), Edge.BOTTOM)
            Box(Modifier.size((g.bounds.width / density).dp, (g.bounds.height / density).dp)) {
                PanelContent(panel, layout, g, wheelSelection = selected)
            }
        }
    }

    @Test fun exportActualNormalFocusedAndCompactWheels() {
        rule.activity.setContent { NudgeTheme {
            Column(Modifier.fillMaxSize().background(Color(0xFF111610)).padding(32.dp), verticalArrangement = Arrangement.spacedBy(42.dp)) {
                Text("Wheel picker", style = MaterialTheme.typography.headlineLarge, color = Color(0xFFE5EBDD))
                Text("Hollow center · separated wedges · up to six actions", color = Color(0xFFC2CCB8))
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Sample("Normal · 160 dp radius", 160f, 6)
                    Sample("Focused · selected size 110%", 160f, 6, 2)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Sample("Minimum · 40 dp radius", 40f, 1)
                    Sample("Compact · 64 dp radius", 64f, 2, 0)
                }
                Text("The highlighted wedge and icon grow. Other wedges shrink.\nReturn to the hollow center to clear the selection.",
                    style = MaterialTheme.typography.bodyLarge, color = Color(0xFFC2CCB8))
            }
        } }
        rule.waitForIdle()
        rule.onAllNodes(hasClickAction()).assertCountEquals(0)
        val output = File("build/reports/wheel-previews/redesign-showcase.png")
        output.parentFile?.mkdirs()
        rule.runOnIdle {
            val view = rule.activity.window.decorView
            val image = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            try { view.draw(Canvas(image)); output.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) } }
            finally { image.recycle() }
        }
        assertTrue(output.length() > 0)
    }

    @Test fun wholeWedgesAreClickableButTheHoleAndSeparatorsAreNot() {
        val layout = PanelLayout(mode = PanelMode.WHEEL, wheel = WheelLayoutSettings(160f, animationsEnabled = false))
        val panel = PanelConfig(buttons = buttons.take(3), portrait = layout)
        val panelGeometry = PanelGeometry.calculate(Bounds(0f, 0f, 600f, 600f), 1f, layout, 3, Bounds(0f, 200f, 12f, 100f), Edge.LEFT)
        val g = requireNotNull(panelGeometry.wheel)
        var actions = 0
        rule.activity.setContent { NudgeTheme {
            Box(Modifier.size(g.bounds.width.dp, g.bounds.height.dp).testTag("wheel-taps")) {
                PanelContent(panel, layout, panelGeometry, interactive = true) { actions++ }
            }
        } }
        fun tap(point: WheelPoint) = rule.onNodeWithTag("wheel-taps").performTouchInput {
            click(Offset(point.x - g.bounds.x, point.y - g.bounds.y))
        }
        tap(g.point(0f, g.innerRadius / 2f)); rule.runOnIdle { assertEquals(0, actions) }
        tap(g.point(g.slices()[0].end)); rule.runOnIdle { assertEquals(0, actions) }
        val slice = g.slices()[1]
        tap(g.point((slice.start + slice.end) / 2f, slice.outerRadius - 2f))
        rule.runOnIdle { assertEquals(1, actions) }
        tap(slice.point); rule.runOnIdle { assertEquals(2, actions) }
        rule.onNodeWithTag("wheel-taps").performTouchInput {
            down(Offset(slice.point.x - g.bounds.x, slice.point.y - g.bounds.y))
            moveBy(Offset(0f, 80f)); up()
        }
        rule.runOnIdle { assertEquals(2, actions) }
    }
}
