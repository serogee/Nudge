package com.modukey.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.modukey.app.core.*
import org.junit.Assert.*
import org.junit.Test

class WheelHoverPaletteTest {
    private val base = ButtonPalette(Color(0xFF303030).copy(alpha = .6f), Color.White, Color.Blue)
    private val panel = Color(0xFF101010).copy(alpha = .7f)
    private val custom = WheelHoverAppearance(useIcon = true, icon = 0xFF00FF00,
        useBackground = true, background = 0xFF0000FF, useHighlight = true, highlight = 0xFFFF0000)
    @Test fun defaultsOnlyApplyASubtleBackgroundChangeAndKeepOpacity() {
        for (container in listOf(false, true)) {
            val normal = wheelHoverPalette(base, panel, container, Color.Yellow, WheelHoverAppearance(), 0f)
            val hovered = wheelHoverPalette(base, panel, container, Color.Yellow, WheelHoverAppearance(), 1f)
            assertEquals(if (container) base.background else panel, normal.background)
            assertNotEquals(normal.background, hovered.background)
            assertEquals(normal.background.alpha, hovered.background.alpha)
            assertEquals(base.icon, hovered.icon); assertEquals(Color.Yellow, hovered.highlight); assertNull(hovered.imageTint)
        }
    }
    @Test fun customChannelsInterpolateAndReturnToTheUnderlyingTogglePalette() {
        val normal = wheelHoverPalette(base, panel, true, Color.Yellow, custom, 0f)
        val halfway = wheelHoverPalette(base, panel, true, Color.Yellow, custom, .5f)
        val hovered = wheelHoverPalette(base, panel, true, Color.Yellow, custom, 1f)
        assertEquals(base.background, normal.background); assertEquals(base.icon, normal.icon)
        assertEquals(Color.Blue.copy(alpha = .6f), hovered.background); assertEquals(Color.Green, hovered.icon)
        assertEquals(Color.Red, hovered.highlight); assertEquals(Color.Green, hovered.imageTint)
        assertEquals(lerp(base.background, hovered.background, .5f), halfway.background)
        assertEquals(lerp(base.icon, hovered.icon, .5f), halfway.icon)
        assertEquals(.5f, halfway.highlight.alpha, .003f); assertEquals(.5f, halfway.imageTint!!.alpha, .003f)
        assertEquals(Color.Blue, base.outline)
    }
    @Test fun disablingOverridesRetainsTheExistingThemeHighlightAndSavedColors() {
        val result = wheelHoverPalette(base, panel, true, Color.Yellow, custom.copy(enabled = false), 1f)
        assertEquals(base.background, result.background); assertEquals(base.icon, result.icon)
        assertEquals(Color.Yellow, result.highlight); assertNull(result.imageTint)
        val iconOnly = wheelHoverPalette(base, panel, true, Color.Yellow, custom.copy(useBackground = false, useHighlight = false), 1f)
        assertEquals(Color.Green, iconOnly.icon); assertEquals(Color.Yellow, iconOnly.highlight)
    }
}
