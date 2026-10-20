package com.modukey.app.platform

import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.WindowInsets
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.modukey.app.NudgeApplication
import com.modukey.app.core.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36])
class HandleWindowHostTest {
    private val app get() = ApplicationProvider.getApplicationContext<NudgeApplication>()
    @After fun cleanup() { app.onTerminate() }
    // Model WindowManager accepting a window before ViewRoot delivers attachment.
    private class PendingWindows(private val delegate: WindowManager, private val attachViews: Boolean = false) : WindowManager by delegate {
        val registered = linkedMapOf<View, ViewGroup.LayoutParams>()
        var adds = 0; var removes = 0
        override fun addView(view: View, params: ViewGroup.LayoutParams) {
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
            host.raise(spec)
            assertEquals("Rapid raises keep one old bar", 2, windows.registered.size)
            assertTrue(original in windows.registered)
            // Deliver the draw event explicitly; legacy Robolectric skips window drawing.
            active!!.viewTreeObserver.dispatchOnDraw()
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
