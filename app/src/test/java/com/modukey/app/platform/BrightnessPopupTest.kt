package com.modukey.app.platform

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.widget.ToggleButton
import org.robolectric.annotation.GraphicsMode
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.test.core.app.ApplicationProvider
import com.modukey.app.NudgeApplication
import com.modukey.app.core.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36])
class BrightnessPopupTest {
    private val app get() = ApplicationProvider.getApplicationContext<NudgeApplication>()
    @After fun cleanup() { app.onTerminate() }
    private fun event(action: Int, y: Float) = MotionEvent.obtain(0, 20, action, 30f, y, 0)
    @Test fun previewDoesNotConsumeTouchesOrChangeBrightness() {
        val writes = mutableListOf<Float>()
        val view = BrightnessPopupView(app, writes::add, {}, {})
        view.layout(0, 0, 64, 240); view.configure(BrightnessPopupSettings(), true, .5f)
        event(MotionEvent.ACTION_DOWN, 30f).let { assertFalse(view.onTouchEvent(it)); it.recycle() }
        event(MotionEvent.ACTION_UP, 50f).let { assertFalse(view.onTouchEvent(it)); it.recycle() }
        assertTrue(writes.isEmpty())
    }
    @Test fun finalReleaseWritesLatestLevelAndCancelledInputDoesNotFlush() {
        val writes = mutableListOf<Float>()
        val view = BrightnessPopupView(app, writes::add, {}, {})
        view.layout(0, 0, 64, 296); view.configure(BrightnessPopupSettings(), false, .5f)
        listOf(MotionEvent.ACTION_DOWN to 200f, MotionEvent.ACTION_MOVE to 150f, MotionEvent.ACTION_UP to 6f).forEach { (action, y) -> event(action, y).let { view.onTouchEvent(it); it.recycle() } }
        assertEquals(1, writes.size); assertEquals(1f, writes.single(), .01f)
        writes.clear()
        listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_CANCEL).forEach { action -> event(action, 150f).let { view.onTouchEvent(it); it.recycle() } }
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100)); assertTrue(writes.isEmpty())
    }
    @Test fun hostReusesOneWindowAndStopsItsTimeoutAfterHiding() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        val context = controller.get(); val wm = context.getSystemService(WindowManager::class.java)
        var dismissed = 0
        lateinit var host: BrightnessPopupHost
        host = BrightnessPopupHost(context, wm, app.brightness, { dismissed++; host.hide() }, { fail("Window failed: $it") })
        val screen = ScreenSpace(400f, 800f, 1f, Insets(), false)
        try {
            assertTrue(host.show(BrightnessPopupSettings(timeoutMs = 1_000), screen, false))
            assertTrue(host.show(BrightnessPopupSettings(widthDp = 88f, timeoutMs = 1_000), screen, false))
            assertTrue(host.visible)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1_001)); assertEquals(1, dismissed); assertFalse(host.visible)
            host.show(BrightnessPopupSettings(), screen, true)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(20)); assertTrue(host.visible); assertEquals(1, dismissed)
            host.hide(); host.hide()
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(20)); assertEquals(1, dismissed)
        } finally { host.hide(); controller.pause().stop().destroy() }
    }
    @Test fun automaticButtonIsSeparateFromSliderInputAndHiddenInSettings() {
        var automatic = 0
        val writes = mutableListOf<Float>()
        val touches = mutableListOf<Boolean>()
        val root = BrightnessPopupView(app, writes::add, touches::add, {}, { automatic++ })
        root.configure(BrightnessPopupSettings(), false, .5f, true)
        root.measure(View.MeasureSpec.makeMeasureSpec(64, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(296, View.MeasureSpec.EXACTLY))
        root.layout(0, 0, 64, 296)
        val mode = root.getChildAt(1) as ToggleButton
        assertTrue("automatic=${mode.isChecked}, typefaceStyle=${mode.typeface.style}", mode.isChecked && mode.typeface.style and android.graphics.Typeface.BOLD != 0)
        assertEquals(52, mode.width); assertEquals(32, mode.height)
        assertTrue(mode.width > mode.height)
        mode.performClick(); assertEquals(1, automatic); assertTrue(writes.isEmpty())
        assertEquals(listOf(true, false), touches)
        root.configure(BrightnessPopupSettings(), true, .5f, false)
        assertFalse(mode.isEnabled)
        root.configure(BrightnessPopupSettings(showAutomaticToggle = false), false, .5f)
        assertEquals(View.GONE, mode.visibility)
        root.measure(View.MeasureSpec.makeMeasureSpec(64, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(240, View.MeasureSpec.EXACTLY))
        root.layout(0, 0, 64, 240)
        assertEquals(240, root.getChildAt(0).height)
    }
    @Test @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun fullWidthTrackAndCornerRadiusMatchTheDrawnPixels() {
        fun image(settings: BrightnessPopupSettings): Bitmap {
            val view = BrightnessPopupView(app, {}, {}, {})
            view.configure(settings.copy(showAutomaticToggle = false), true, .5f)
            view.measure(View.MeasureSpec.makeMeasureSpec(64, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(240, View.MeasureSpec.EXACTLY))
            view.layout(0, 0, 64, 240)
            return Bitmap.createBitmap(64, 240, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
        }
        val base = BrightnessPopupSettings(cornerRadiusDp = 0f)
        assertEquals(base.background.toInt(), image(base).getPixel(1, 180))
        assertEquals(base.fill.toInt(), image(base.copy(showBackground = false)).getPixel(1, 180))
        assertEquals(255, Color.alpha(image(base).getPixel(0, 0)))
        assertEquals(0, Color.alpha(image(base.copy(cornerRadiusDp = 32f)).getPixel(0, 0)))
    }
    @Test fun aSmallDragUsesTheActualFullWidthTrackHeight() {
        val writes = mutableListOf<Float>()
        val view = BrightnessPopupView(app, writes::add, {}, {})
        view.configure(BrightnessPopupSettings(showBackground = false, showAutomaticToggle = false), false, .5f)
        view.measure(View.MeasureSpec.makeMeasureSpec(64, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(240, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, 64, 240)
        listOf(MotionEvent.ACTION_DOWN to 120f, MotionEvent.ACTION_MOVE to 96f, MotionEvent.ACTION_UP to 96f).forEach { (action, y) ->
            event(action, y).let { view.dispatchTouchEvent(it); it.recycle() }
        }
        assertEquals(1, writes.size); assertEquals(.6f, writes.single(), .001f)
    }
    @Test fun outsideTouchBelowTheModeButtonStillClosesThePopup() {
        var dismissed = 0
        val view = BrightnessPopupView(app, {}, {}, { dismissed++ })
        view.layout(0, 0, 64, 296); view.configure(BrightnessPopupSettings(), false, .5f)
        event(MotionEvent.ACTION_OUTSIDE, 500f).let { assertTrue(view.onTouchEvent(it)); it.recycle() }
        assertEquals(1, dismissed)
    }
    @Test fun oldPreviewOwnerCannotClearReplacementSession() {
        val preview = BrightnessPreview(); val old = Any(); val current = Any()
        preview.begin(old); preview.begin(current); preview.end(old); assertTrue(preview.state.value)
        preview.end(current); assertFalse(preview.state.value)
    }
}
