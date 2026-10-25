package com.modukey.app.ui

import android.graphics.Region
import androidx.compose.ui.graphics.asAndroidPath
import com.modukey.app.core.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.hypot

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WheelIconLayoutTest {
    private fun geometry(settings: WheelLayoutSettings, edge: Edge = Edge.LEFT, density: Float = 1f) =
        WheelGeometry.calculate(Bounds(10f, 20f, 800f * density, 900f * density), density, PanelLayout(),
            settings, 6, Bounds(10f, 300f, 16f, 120f), edge)

    @Test fun fullSquareArtworkFitsRoundedWedgesAtExtremeRadiiAndEveryAnimationFrame() {
        for (edge in Edge.entries) for (density in listOf(1f, 3f)) for ((inner, outer) in listOf(1f to 49f, 1f to 336f, 100f to 156f, 288f to 336f)) {
            val settings = WheelLayoutSettings(innerRadiusDp = inner, outerRadiusDp = outer, selectedScale = 1.5f)
            val g = geometry(settings, edge, density)
            for (frame in 0..4) {
                val weights = List(g.count) { if (it == 0) 1f - frame / 4f else if (it == 1) frame / 4f else 0f }
                for (slice in g.slices(weights)) for (corner in listOf(0f, 48f)) {
                    val outline = wheelShape(g, slice, corner * density)
                    val region = Region().apply { setPath(outline.asAndroidPath(), Region(-10_000, -10_000, 10_000, 10_000)) }
                    for (position in listOf(0f, .25f, .5f, .75f, 1f)) for (adaptive in listOf(true, false)) {
                        val icon = wheelIconLayout(g, slice, settings.copy(iconPosition = position, adaptIconSize = adaptive, iconSizeDp = 128f), outline, density)
                        assertTrue("An endpoint must retain an icon: $edge $inner $outer $position", icon.size >= 11.9f * density)
                        for (x in 0..4) for (y in 0..4) {
                            val px = icon.point.x - g.bounds.x + (x / 4f - .5f) * icon.size
                            val py = icon.point.y - g.bounds.y + (y / 4f - .5f) * icon.size
                            assertTrue("Artwork leaves its wedge: $edge $inner $outer $position $frame", region.contains(px.toInt(), py.toInt()))
                        }
                    }
                }
            }
        }
    }
    @Test fun positionMovesRadiallyAndDefaultKeepsTheBandCenter() {
        val settings = WheelLayoutSettings(innerRadiusDp = 1f, outerRadiusDp = 300f, expandSelection = false)
        val g = geometry(settings)
        val slice = g.slices()[g.count / 2]
        val outline = wheelShape(g, slice, 24f)
        val icons = listOf(0f, .25f, .5f, .75f, 1f).map { wheelIconLayout(g, slice, settings.copy(iconPosition = it), outline, 1f) }
        val radii = icons.map { hypot(it.point.x - g.center.x, it.point.y - g.center.y) }
        radii.zipWithNext().forEach { (a, b) -> assertTrue(a < b) }
        assertEquals(slice.point.x, icons[2].point.x, .001f)
        assertEquals(slice.point.y, icons[2].point.y, .001f)
        assertTrue(icons[1].size < icons[2].size)
        assertTrue(icons[2].size < icons[3].size)
    }
    @Test fun manualSizeIsAPreferenceAndAppearanceDoesNotChangeTouchGeometry() {
        val settings = WheelLayoutSettings(innerRadiusDp = 1f, outerRadiusDp = 300f, expandSelection = false,
            adaptIconSize = false, iconSizeDp = 128f)
        val g = geometry(settings)
        val slice = g.slices()[0]
        val outline = wheelShape(g, slice, 48f)
        val icon = wheelIconLayout(g, slice, settings, outline, 1f)
        assertTrue(icon.size < settings.iconSizeDp)
        assertEquals(128f, settings.iconSizeDp)
        assertEquals(12f, wheelIconLayout(g, slice, settings.copy(iconSizeDp = 12f), outline, 1f).size, .001f)
        val edited = geometry(settings.copy(iconPosition = 1f, iconSizeDp = 12f, adaptIconSize = true))
        assertEquals(g, edited)
        assertEquals(6, g.count)
        g.slices().forEachIndexed { index, target -> assertEquals(index, g.tapHit(target.point.x, target.point.y)) }
    }
}
