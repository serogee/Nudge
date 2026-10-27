package com.modukey.app.platform

import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.WindowInsets
import android.os.Looper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.modukey.app.NudgeApplication
import com.modukey.app.core.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.Shadows.shadowOf
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36])
class HandleWindowHostTest {
    @Test fun inactiveHandlesPassTouchesThroughAndRestoreInputWithoutReplacingTheWindow() {
        val windows = PendingWindows(app.getSystemService(WindowManager::class.java))
        var actions = 0
        val host = HandleWindowHost(app, windows, { actions++ }, {}) {}
        val spec = HandleWindowSpec(Bounds(0f, 200f, 16f, 120f), TriggerSettings(indicatorVisible = true), Edge.LEFT, false,
            inputEnabled = false)
        try {
            host.attach(spec)
            val view = windows.registered.keys.single() as HandleTriggerView
            view.layout(0, 0, 16, 120)
            fun touchable() = (windows.registered.getValue(view) as WindowManager.LayoutParams).flags and
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE == 0
            assertFalse(touchable()); assertFalse(view.isClickable); assertFalse(view.performClick())
            assertTrue(view.systemGestureExclusionRects.isEmpty())
            assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO, view.importantForAccessibility)
            host.update(spec.copy(inputEnabled = true))
            assertSame(view, windows.registered.keys.single())
            assertTrue(touchable()); assertTrue(view.isClickable); assertTrue(view.performClick())
            assertFalse(view.systemGestureExclusionRects.isEmpty()); assertEquals(1, actions)
            host.update(spec)
            assertFalse(touchable()); assertFalse(view.performClick()); assertEquals(1, actions)
            assertEquals(1, windows.adds); assertEquals(0, windows.removes)
        } finally { host.detach() }
    }
    private val app get() = ApplicationProvider.getApplicationContext<NudgeApplication>()
    @After fun cleanup() { app.onTerminate() }
    // Model WindowManager accepting a window before ViewRoot delivers attachment.
    private class PendingWindows(private val delegate: WindowManager, private val attachViews: Boolean = false) : WindowManager by delegate {
        val registered = linkedMapOf<View, ViewGroup.LayoutParams>()
        var adds = 0; var removes = 0
        var rejectNext = false
        override fun addView(view: View, params: ViewGroup.LayoutParams) {
            if (rejectNext) { rejectNext = false; throw IllegalStateException("Rejected window") }
            if (attachViews) delegate.addView(view, params)
            check(registered.put(view, params) == null); adds++
        }
        override fun updateViewLayout(view: View, params: ViewGroup.LayoutParams) {
            check(view in registered); if (attachViews) delegate.updateViewLayout(view, params); registered[view] = params
        }
        override fun removeViewImmediate(view: View) {
            check(registered.remove(view) != null); if (attachViews) delegate.removeViewImmediate(view); removes++
        }
    }
    @Test @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun translucentAlphaStaysConstantAcrossRapidRaisesAndTheirFirstFrames() {
        val windows = PendingWindows(app.getSystemService(WindowManager::class.java), attachViews = true)
        var active: HandleTriggerView? = null
        val host = HandleWindowHost(app, windows, {}, {}) { active = it }
        val spec = HandleWindowSpec(Bounds(0f, 200f, 16f, 120f), TriggerSettings(indicatorVisible = true, indicatorOpacity = .45f), Edge.LEFT, false)
        fun alpha(): Int {
            val bitmap = Bitmap.createBitmap(16, 120, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            windows.registered.keys.forEach { it.layout(0, 0, 16, 120); it.draw(canvas) }
            val pixels = IntArray(16 * 120); bitmap.getPixels(pixels, 0, 16, 0, 0, 16, 120); bitmap.recycle()
            return pixels.maxOf { Color.alpha(it) }
        }
        try {
            host.attach(spec); shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
            val expected = alpha(); assertTrue(expected in 1..254)
            repeat(4) { host.raise(spec); assertEquals(expected, alpha()) }
            active!!.viewTreeObserver.dispatchOnPreDraw()
            assertEquals(expected, alpha()); assertEquals(1, windows.registered.size)
        } finally { host.detach() }
    }
    @Test fun failedReplacementKeepsTheOriginalDrawingAndAcceptingInput() {
        val windows = PendingWindows(app.getSystemService(WindowManager::class.java))
        var active: HandleTriggerView? = null; var actions = 0
        val host = HandleWindowHost(app, windows, { actions++ }, {}) { active = it }
        val spec = HandleWindowSpec(Bounds(0f, 200f, 16f, 120f), TriggerSettings(indicatorVisible = true), Edge.LEFT, false)
        try {
            host.attach(spec); val original = active!!
            windows.rejectNext = true
            try { host.raise(spec); fail() } catch (_: IllegalStateException) { }
            assertSame(original, active); assertTrue(original.drawingEnabled)
            assertTrue(original.performClick()); assertEquals(1, actions)
            assertEquals(setOf(original), windows.registered.keys)
        } finally { host.detach() }
    }
    @Test fun raisingKeepsTheDrawnHandleUntilItsReplacementDrawsAndCleanupCancelsTheHandoff() {
        val windows = PendingWindows(app.getSystemService(WindowManager::class.java), attachViews = true)
        var active: HandleTriggerView? = null
        val host = HandleWindowHost(app, windows, {}, {}) { active = it }
        val spec = HandleWindowSpec(Bounds(0f, 200f, 16f, 120f), TriggerSettings(indicatorVisible = true), Edge.LEFT, false)
        try {
            host.attach(spec)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
            val original = active!!
            host.raise(spec)
            assertEquals(2, windows.registered.size)
            assertTrue(original in windows.registered); assertTrue(original.isAttachedToWindow)
            assertNotSame(original, active)
            assertTrue(original.drawingEnabled); assertFalse(active!!.drawingEnabled)
            assertFalse(original.performClick())
            host.raise(spec)
            assertEquals("Rapid raises keep one old bar", 2, windows.registered.size)
            assertTrue(original in windows.registered)
            assertEquals(1, windows.registered.keys.count { (it as HandleTriggerView).drawingEnabled })
            // Deliver the frame boundary explicitly; legacy Robolectric skips window drawing.
            active!!.viewTreeObserver.dispatchOnPreDraw()
            assertTrue(active!!.drawingEnabled); assertFalse(original.drawingEnabled)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
            assertEquals(setOf(active), windows.registered.keys)
            assertFalse(original.isAttachedToWindow)
            host.raise(spec)
            host.detach(); host.detach()
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
            assertTrue(windows.registered.isEmpty()); assertNull(active)
            assertEquals(windows.adds, windows.removes)
        } finally { host.detach() }
    }
    @Test fun raisingRetainsARevealedHandleAndItsPanelStateInFullScreen() {
        val windows = PendingWindows(app.getSystemService(WindowManager::class.java))
        var active: HandleTriggerView? = null
        var actions = 0
        val host = HandleWindowHost(app, windows, { actions++ }, {}) { active = it }
        val spec = HandleWindowSpec(Bounds(0f, 200f, 16f, 120f), TriggerSettings(softHideEnabled = true), Edge.LEFT, false)
        try {
            host.attach(spec)
            active!!.dispatchApplyWindowInsets(WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), false).build())
            active!!.performClick(); assertEquals(0, actions)
            active!!.setPanelOpen(true)
            host.raise(spec)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4))
            active!!.performClick(); assertEquals("Raising must not hide a revealed bar", 1, actions)
        } finally { host.detach() }
    }
    @Test fun visibilityUpdatesKeepTheSameBoundedTouchableWindow() {
        val windows = PendingWindows(app.getSystemService(WindowManager::class.java))
        val host = HandleWindowHost(app, windows, {}, {}) {}
        val first = HandleWindowSpec(Bounds(0f, 200f, 16f, 120f), TriggerSettings(indicatorVisible = true), Edge.LEFT, false)
        host.attach(first)
        val view = windows.registered.keys.single()
        val params = windows.registered.getValue(view)
        host.update(first.copy(hideIndicator = true)); host.update(first)
        assertSame(view, windows.registered.keys.single())
        assertSame(params, windows.registered.getValue(view))
        assertEquals(1, windows.adds); assertEquals(0, windows.removes)
        host.detach()
    }
    @Test fun `clearing or reordering pending windows never leaves a second active handle`() {
        val windows = PendingWindows(app.getSystemService(WindowManager::class.java))
        val views = mutableMapOf<String, HandleTriggerView>()
        val registry = OverlayRegistry<String, HandleWindowSpec> { id -> HandleWindowHost(app, windows, {}, {}) { v ->
            if (v == null) views.remove(id) else views[id] = v
        } }
        val spec = HandleWindowSpec(Bounds(100f, 380f, 120f, 16f), TriggerSettings(), Edge.BOTTOM, false)
        repeat(6) { rotation ->
            assertTrue(registry.reconcile(linkedMapOf("a" to spec, "b" to spec)).isEmpty())
            assertEquals(2, windows.registered.size)
            assertTrue(windows.registered.keys.none { it.isAttachedToWindow })
            assertTrue(registry.reconcile(linkedMapOf("b" to spec, "a" to spec), raise = rotation % 2 == 0).isEmpty())
            assertEquals(2, windows.registered.size)
            registry.clear(); registry.clear()
            assertTrue(windows.registered.isEmpty()); assertTrue(views.isEmpty())
        }
        assertEquals(windows.adds, windows.removes)
    }
    @Test fun `layout updates retain a single pending window and cleanup is idempotent`() {
        val windows = PendingWindows(app.getSystemService(WindowManager::class.java))
        val host = HandleWindowHost(app, windows, {}, {}) {}
        val first = HandleWindowSpec(Bounds(0f, 200f, 16f, 120f), TriggerSettings(), Edge.LEFT, true, 1)
        host.attach(first)
        val view = windows.registered.keys.single()
        host.update(first.copy(bounds = Bounds(0f, 100f, 16f, 250f)))
        assertSame(view, windows.registered.keys.single())
        assertEquals(1, windows.adds)
        val p = windows.registered.getValue(view) as WindowManager.LayoutParams
        assertEquals(250, p.height)
        assertTrue(p.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE != 0)
        host.detach(); host.detach()
        assertTrue(windows.registered.isEmpty()); assertEquals(1, windows.removes)
    }
}
