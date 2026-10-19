package com.modukey.app.core

import org.junit.Assert.*
import org.junit.Test

class BrightnessPopupGeometryTest {
    @Test fun defaultIsOppositeRightVolumeAndRespectsInsets() {
        val b = BrightnessPopupGeometry.calculate(Bounds(20f, 30f, 400f, 800f), 1f, BrightnessPopupSettings())
        assertEquals(32f, b.x); assertEquals(64f, b.width); assertEquals(296f, b.height)
        val other = BrightnessPopupGeometry.calculate(Bounds(20f, 30f, 400f, 800f), 1f, BrightnessPopupSettings(volumeSide = VolumeSide.LEFT))
        assertEquals(344f, other.x)
    }
    @Test fun resizeKeepsCenterAndClampsWithoutChangingSettings() {
        val s = BrightnessPopupSettings(side = BrightnessSide.CUSTOM, x = .5f, y = .5f, widthDp = 128f, heightDp = 480f, showAutomaticToggle = false)
        val a = BrightnessPopupGeometry.calculate(Bounds(0f, 0f, 500f, 900f), 1f, s)
        assertEquals(250f, a.x + a.width / 2); assertEquals(450f, a.y + a.height / 2)
        val b = BrightnessPopupGeometry.calculate(Bounds(4f, 8f, 100f, 200f), 3f, s)
        assertEquals(Bounds(4f, 8f, 100f, 200f), b)
        assertEquals(480f, s.heightDp)
    }
    @Test fun rotationKeepsLogicalSideAndIndependentCoordinates() {
        val s = BrightnessPopupSettings(side = BrightnessSide.RIGHT, x = .13f, y = 1f)
        listOf(Bounds(0f, 0f, 400f, 900f), Bounds(0f, 0f, 900f, 400f)).forEach {
            val b = BrightnessPopupGeometry.calculate(it, 1f, s)
            assertEquals(it.width - b.width - 12f, b.x)
            assertEquals(0f, b.y)
        }
        assertEquals(.13f, s.normalized().x)
    }
    @Test fun positionStartsAtBottomAndAddingModeButtonKeepsSliderCenter() {
        val usable = Bounds(20f, 30f, 400f, 800f)
        val s = BrightnessPopupSettings(y = .5f)
        val withButton = BrightnessPopupGeometry.calculate(usable, 1f, s)
        val without = BrightnessPopupGeometry.calculate(usable, 1f, s.copy(showAutomaticToggle = false))
        assertEquals(without.y, withButton.y)
        assertEquals(without.height + 56f, withButton.height)
        val bottom = BrightnessPopupGeometry.calculate(usable, 1f, s.copy(y = 0f))
        val top = BrightnessPopupGeometry.calculate(usable, 1f, s.copy(y = 1f))
        assertEquals(usable.y + usable.height, bottom.y + bottom.height)
        assertEquals(usable.y, top.y)
        assertEquals(.5f, s.y)
    }
}

