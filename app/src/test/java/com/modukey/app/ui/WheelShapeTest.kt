package com.modukey.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.asAndroidPath
import com.modukey.app.core.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WheelShapeTest {
    @Test fun tinyHolesStayHollowWithRoundedWedgesOnEveryEdge() {
        for (edge in Edge.entries) {
            val g = WheelGeometry.calculate(Bounds(0f, 0f, 800f, 900f), 4f, PanelLayout(),
                WheelLayoutSettings(innerRadiusDp = 1f, outerRadiusDp = 81f, expandSelection = false), 6,
                Bounds(0f, 300f, 16f, 120f), edge)
            assertTrue(g.count > 0)
            val bitmap = Bitmap.createBitmap(800, 900, Bitmap.Config.ARGB_8888)
            try {
                val canvas = Canvas(bitmap)
                g.slices().forEach { slice ->
                    canvas.drawPath(wheelShape(g, slice, 96f).asAndroidPath(), Paint().apply { color = android.graphics.Color.GREEN })
                }
                val hole = g.point(0f, g.innerRadius / 2f)
                assertEquals(0, bitmap.getPixel((hole.x - g.bounds.x).toInt(), (hole.y - g.bounds.y).toInt()))
                g.slices().forEach { slice ->
                    assertEquals(android.graphics.Color.GREEN, bitmap.getPixel((slice.point.x - g.bounds.x).toInt(), (slice.point.y - g.bounds.y).toInt()))
                }
            } finally { bitmap.recycle() }
        }
    }
    @Test fun roundedWedgesKeepConcentricArcsOnEveryEdge() {
        for (edge in Edge.entries) for (selected in listOf(null, 2)) {
            val g = WheelGeometry.calculate(Bounds(0f, 0f, 800f, 900f), 1f, PanelLayout(),
                WheelLayoutSettings(200f), 6, Bounds(0f, 300f, 16f, 120f), edge)
            val bitmap = Bitmap.createBitmap(800, 900, Bitmap.Config.ARGB_8888)
            try {
                val canvas = Canvas(bitmap)
                g.slices(selected).forEach { slice ->
                    bitmap.eraseColor(0)
                    canvas.drawPath(wheelShape(g, slice, 24f).asAndroidPath(), Paint().apply { color = android.graphics.Color.GREEN })
                    // Away from the four rounded joins, both boundaries must follow the same center.
                    for (fraction in listOf(.4f, .5f, .6f)) {
                        val angle = slice.start + (slice.end - slice.start) * fraction
                        for ((radius, filled) in listOf(slice.innerRadius - 3f to false, slice.innerRadius + 3f to true,
                            slice.outerRadius - 3f to true, slice.outerRadius + 3f to false)) {
                            val p = g.point(angle, radius)
                            assertEquals("$edge $selected $fraction $radius", filled,
                                bitmap.getPixel((p.x - g.bounds.x).toInt(), (p.y - g.bounds.y).toInt()) != 0)
                        }
                    }
                }
            } finally { bitmap.recycle() }
        }
    }
}
