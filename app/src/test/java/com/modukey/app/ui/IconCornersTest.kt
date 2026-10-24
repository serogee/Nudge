package com.modukey.app.ui

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.modukey.app.core.PanelConfig
import org.junit.Assert.*
import org.junit.Test

class IconCornersTest {
    @Test fun squareRoundedAndCircularIconsShareTheSameCornerControl() {
        for (fraction in listOf(0f, .25f, .5f)) {
            val outline = iconShape(PanelConfig(iconCornerFraction = fraction))
                .createOutline(Size(80f, 80f), LayoutDirection.Ltr, Density(1f))
            if (fraction == 0f) assertTrue(outline is Outline.Rectangle)
            else {
                val rounded = (outline as Outline.Rounded).roundRect
                assertEquals(80f * fraction, rounded.topLeftCornerRadius.x, .001f)
                assertEquals(80f * fraction, rounded.bottomRightCornerRadius.y, .001f)
            }
        }
    }
}
