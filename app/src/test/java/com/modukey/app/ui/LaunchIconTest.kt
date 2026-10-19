package com.modukey.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import androidx.test.core.app.ApplicationProvider
import com.modukey.app.NudgeApplication
import com.modukey.app.R
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36], qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LaunchIconTest {
    private val app get() = ApplicationProvider.getApplicationContext<NudgeApplication>()
    @After fun cleanup() { app.onTerminate() }
    private fun render(resource: Int, size: Int): Bitmap {
        val drawable = requireNotNull(app.getDrawable(resource))
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { drawable.setBounds(0, 0, size, size); drawable.draw(Canvas(it)) }
    }
    @Test fun `the static N has balanced padding and a rounded transparent outer corner`() {
        val bitmap = render(R.drawable.ic_modukey, 108)
        assertEquals(0, Color.alpha(bitmap.getPixel(0, 0)))
        assertEquals(0, Color.alpha(bitmap.getPixel(107, 0)))
        assertTrue(Color.alpha(bitmap.getPixel(54, 0)) > 200)
        val points = buildList { for (y in 0 until 108) for (x in 0 until 108) {
            val pixel = bitmap.getPixel(x, y)
            if (Color.alpha(pixel) > 200 && Color.red(pixel) + Color.green(pixel) + Color.blue(pixel) < 150) add(x to y)
        } }
        assertTrue(points.isNotEmpty())
        val left = points.minOf { it.first }; val right = points.maxOf { it.first }
        val top = points.minOf { it.second }; val bottom = points.maxOf { it.second }
        assertTrue(abs((left + right + 1) / 2f - 54f) <= 1f)
        assertTrue(abs((top + bottom + 1) / 2f - 54f) <= 1f)
        assertTrue(left >= 20 && top >= 20 && right <= 87 && bottom <= 87)
    }
    @Test fun `native launch artwork keeps its rounded container inside the system safe area`() {
        assertTrue(app.getDrawable(R.drawable.ic_nudge_launch) is AnimatedVectorDrawable)
        val bitmap = render(R.drawable.ic_nudge_launch, 432)
        assertEquals(0, Color.alpha(bitmap.getPixel(0, 0)))
        assertEquals(0, Color.alpha(bitmap.getPixel(108, 108)))
        assertTrue(Color.alpha(bitmap.getPixel(216, 110)) > 200)
        assertEquals(0, Color.alpha(bitmap.getPixel(216, 80)))
    }
}
