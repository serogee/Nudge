package com.modukey.app.platform

import android.animation.ValueAnimator
import android.content.Intent
import android.media.AudioManager
import android.os.Looper
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import com.modukey.app.NudgeApplication
import com.modukey.app.core.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36], qualifiers = "mdpi")
class PanelHostTest {
    private val app get() = ApplicationProvider.getApplicationContext<NudgeApplication>()
    private val hosts = mutableListOf<AccessibilityPanelHost>()
    private fun idle(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
    private fun scale(value: Float) = ReflectionHelpers.callStaticMethod<Void>(ValueAnimator::class.java, "setDurationScale", ClassParameter.from(Float::class.javaPrimitiveType, value))
    @After fun cleanup() {
        hosts.forEach { it.hide(animate = false) }
        scale(1f); app.onTerminate()
    }
    private class Windows(private val delegate: WindowManager) : WindowManager by delegate {
        val attached = mutableListOf<View>()
        val params = mutableMapOf<View, WindowManager.LayoutParams>()
        var removals = 0
        var failRemoval = false
        override fun addView(view: View, layout: ViewGroup.LayoutParams) {
            delegate.addView(view, layout); attached += view; params[view] = layout as WindowManager.LayoutParams
        }
        override fun updateViewLayout(view: View, layout: ViewGroup.LayoutParams) {
            delegate.updateViewLayout(view, layout); params[view] = layout as WindowManager.LayoutParams
        }
        override fun removeViewImmediate(view: View) {
            delegate.removeViewImmediate(view); attached.remove(view); params.remove(view); removals++
            if (failRemoval) { failRemoval = false; throw IllegalStateException("test cleanup failure") }
        }
    }
    private data class Fixture(val host: AccessibilityPanelHost, val windows: Windows, val errors: MutableList<RuntimeException>, val snapshots: () -> Int)
    private fun fixture(dismiss: (PanelDismissReason) -> Unit = {}): Fixture {
        shadowOf(app).grantPermissions("${app.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
        scale(1f)
        val windows = Windows(app.getSystemService(WindowManager::class.java))
        val errors = mutableListOf<RuntimeException>()
        var snapshots = 0
        val actions = object : ActionExecutor {
            override fun snapshot() = SoundState().also { snapshots++ }
            override fun execute(handleId: String, button: PanelButton, testing: Boolean, gestureSlot: GestureSlot?) = ActionResult()
        }
        val host = AccessibilityPanelHost(app, windows, actions, dismiss, { _, _ -> false }, {}, onError = { errors += it })
        hosts += host
        return Fixture(host, windows, errors) { snapshots }
    }
    private fun spec(animation: PanelAnimation = PanelAnimation.FADE, edge: Edge = Edge.RIGHT, editing: Boolean = false, closeAnimation: PanelAnimation = animation): PanelSpec {
        val p = PanelConfig(animation = animation, closeAnimation = closeAnimation)
        val g = PanelGeometry.calculate(Bounds(0f, 0f, 400f, 700f), 1f, p.portrait, 0, Bounds(384f, 200f, 16f, 120f), edge)
        return PanelSpec(p, p.portrait, g, editing, edge)
    }
    @Test fun pullFeedbackIsSlightSuspendedAndSettlesAfterRelease() {
        val f = fixture(); val normal = spec(PanelAnimation.NONE)
        f.host.show(normal.copy(inputSuspended = true)); idle(50)
        val root = f.windows.attached.single()
        val flags = f.windows.params.getValue(root).flags
        assertTrue(flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE != 0)
        assertTrue(flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0)
        assertEquals(0, f.snapshots())
        f.host.pull(12f, Edge.RIGHT)
        assertEquals(-12f, root.translationX); assertEquals(0f, root.translationY)
        idle(700); assertEquals(-12f, root.translationX)
        f.host.show(normal); f.host.pull(0f, Edge.RIGHT, settle = true)
        idle(250)
        assertEquals(0f, root.translationX); assertSame(root, f.windows.attached.single())
        assertTrue(f.windows.params.getValue(root).flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE == 0)
        assertTrue(f.snapshots() > 0)
    }
    @Test fun `editing preview rotates its content and bounds without enabling input or sound observers`() {
        val f = fixture()
        val logical = spec(editing = true)
        val b = logical.geometry.bounds
        f.host.show(logical.copy(geometry = logical.geometry.copy(bounds = Bounds(0f, 100f, b.height, b.width)), previewQuarterTurns = 1))
        idle(100)
        val root = f.windows.attached.single() as PanelRoot
        val content = root.getChildAt(0)
        assertEquals(90f, content.rotation)
        assertEquals(b.width.toInt(), content.layoutParams.width)
        assertEquals(b.height.toInt(), content.layoutParams.height)
        assertEquals(b.height, content.translationX)
        assertTrue(f.windows.params.getValue(root).flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE != 0)
        assertEquals(0, f.snapshots())
        f.host.show(logical.copy(previewQuarterTurns = 0))
        idle(100)
        assertSame(root, f.windows.attached.single())
        assertEquals(0f, content.rotation); assertEquals(0f, content.translationX)
    }
    @Test fun `exit retains rendered content disables input and disposes after the animation`() {
        val f = fixture()
        f.host.show(spec()); idle(250)
        val root = f.windows.attached.single() as PanelRoot
        val compose = root.getChildAt(0) as ComposeView
        val owner = root.findViewTreeLifecycleOwner()!!
        assertEquals(1f, root.alpha)
        assertTrue(compose.hasComposition)
        val snapshots = f.snapshots()
        f.host.hide()
        assertTrue(root.isAttachedToWindow); assertTrue(compose.hasComposition)
        assertEquals(Lifecycle.State.RESUMED, owner.lifecycle.currentState)
        val flags = f.windows.params.getValue(root).flags
        assertTrue(flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE != 0)
        assertTrue(flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0)
        val touch = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 10f, 10f, 0)
        try { assertFalse(root.dispatchTouchEvent(touch)) } finally { touch.recycle() }
        f.host.refreshSound(); assertEquals(snapshots, f.snapshots())
        f.host.hide() // Repeated dismissal must not blank or remove the exit early.
        assertTrue(root.isAttachedToWindow)
        idle(32)
        assertTrue("Exit alpha: ${root.alpha}", root.alpha < 1f)
        idle(200)
        assertFalse(root.isAttachedToWindow); assertFalse(compose.hasComposition)
        assertEquals(Lifecycle.State.DESTROYED, owner.lifecycle.currentState)
        assertTrue(f.windows.attached.isEmpty()); assertEquals(1, f.windows.removals); assertTrue(f.errors.isEmpty())
    }
    @Test fun `rapid replacement keeps one inert exit and old callbacks cannot remove the new panel`() {
        val f = fixture()
        f.host.show(spec(PanelAnimation.SLIDE, Edge.LEFT)); idle(250)
        val a = f.windows.attached.single()
        f.host.hide(); f.host.show(spec(PanelAnimation.SCALE, Edge.BOTTOM))
        assertEquals(2, f.windows.attached.size)
        val b = f.windows.attached.last()
        idle(32)
        f.host.hide(); f.host.show(spec(PanelAnimation.FADE))
        assertFalse(a.isAttachedToWindow); assertTrue(f.windows.attached.size in 1..2)
        val c = f.windows.attached.last()
        idle(300)
        assertFalse(b.isAttachedToWindow); assertEquals(listOf(c), f.windows.attached)
        assertEquals(1f, c.alpha); assertEquals(Lifecycle.State.RESUMED, c.findViewTreeLifecycleOwner()!!.lifecycle.currentState)
        assertTrue(f.errors.isEmpty())
    }
    @Test fun `forced cleanup cancels entering and exiting panels without later callbacks`() {
        val f = fixture()
        f.host.show(spec()); idle(250)
        f.host.hide(); f.host.show(spec(PanelAnimation.SCALE))
        f.host.hide(animate = false); f.host.hide(animate = false)
        assertTrue(f.windows.attached.isEmpty()); assertEquals(2, f.windows.removals)
        f.host.show(spec(PanelAnimation.NONE))
        val next = f.windows.attached.single()
        idle(500)
        assertEquals(listOf(next), f.windows.attached); assertEquals(2, f.windows.removals); assertTrue(f.errors.isEmpty())
    }
    @Test fun `none editing and system disabled animations close immediately`() {
        val f = fixture()
        for (s in listOf(spec(PanelAnimation.NONE), spec(PanelAnimation.SCALE, editing = true))) {
            f.host.show(s)
            assertEquals(1f, f.windows.attached.single().alpha)
            f.host.hide()
            assertTrue(f.windows.attached.isEmpty())
        }
        scale(0f)
        f.host.show(spec(PanelAnimation.SLIDE))
        assertEquals(1f, f.windows.attached.single().alpha)
        f.host.hide()
        assertTrue(f.windows.attached.isEmpty()); assertTrue(f.errors.isEmpty())
    }
    @Test fun `asynchronous cleanup errors are reported once and other windows remain usable`() {
        val f = fixture()
        f.host.show(spec()); idle(250)
        val owner = f.windows.attached.single().findViewTreeLifecycleOwner()!!
        f.windows.failRemoval = true
        f.host.hide(); idle(250)
        assertEquals(1, f.errors.size); assertTrue(f.windows.attached.isEmpty())
        assertEquals(Lifecycle.State.DESTROYED, owner.lifecycle.currentState)
        f.host.hide(animate = false)
        f.host.show(spec(PanelAnimation.NONE)); f.host.hide()
        assertEquals(1, f.errors.size); assertEquals(2, f.windows.removals)
    }
    @Test fun `open and close effects can be selected independently`() {
        val f = fixture()
        f.host.show(spec(PanelAnimation.NONE, closeAnimation = PanelAnimation.FADE)); idle(32)
        assertEquals(1f, f.windows.attached.single().alpha)
        f.host.hide()
        assertEquals(1, f.windows.attached.size)
        idle(250); assertTrue(f.windows.attached.isEmpty())
        f.host.show(spec(PanelAnimation.FADE, closeAnimation = PanelAnimation.NONE)); idle(250)
        f.host.hide(); assertTrue(f.windows.attached.isEmpty()); assertTrue(f.errors.isEmpty())
    }
    @Test fun `scale exit uses the current center after resizing or rotation`() {
        val f = fixture()
        val initial = spec(PanelAnimation.NONE, closeAnimation = PanelAnimation.SCALE)
        f.host.show(initial); idle(32)
        val old = initial.geometry.bounds
        val resized = initial.copy(geometry = initial.geometry.copy(bounds = old.copy(width = old.width + 48f, height = old.height + 32f)))
        f.host.show(resized); idle(32)
        val root = f.windows.attached.single()
        f.host.hide()
        assertEquals(resized.geometry.bounds.width / 2, root.pivotX)
        assertEquals(resized.geometry.bounds.height / 2, root.pivotY)
        idle(250); assertTrue(f.windows.attached.isEmpty()); assertTrue(f.errors.isEmpty())
    }
    @Test fun `unexpected removal reports once and normal cleanup does not report dismissal`() {
        val reasons = mutableListOf<PanelDismissReason>()
        val f = fixture { reasons += it }
        f.host.show(spec(PanelAnimation.NONE)); idle(32)
        f.windows.removeViewImmediate(f.windows.attached.single())
        assertEquals(listOf(PanelDismissReason.WINDOW_DETACHED), reasons)
        f.host.hide(animate = false)
        f.host.show(spec(PanelAnimation.NONE)); f.host.hide(animate = false)
        f.host.show(spec(PanelAnimation.NONE, editing = true)); idle(32)
        f.windows.removeViewImmediate(f.windows.attached.single())
        assertEquals(listOf(PanelDismissReason.WINDOW_DETACHED), reasons)
        assertTrue(f.errors.isEmpty())
    }
    @Test fun `sound broadcasts and settings refresh retain the current window`() {
        val reasons = mutableListOf<PanelDismissReason>()
        val f = fixture { reasons += it }
        f.host.show(spec(PanelAnimation.NONE)); idle(32)
        val root = f.windows.attached.single(); val snapshots = f.snapshots()
        app.sendBroadcast(Intent(AudioManager.RINGER_MODE_CHANGED_ACTION))
        app.contentResolver.notifyChange(Settings.System.CONTENT_URI, null)
        idle(32)
        assertTrue(f.snapshots() > snapshots)
        assertEquals(listOf(root), f.windows.attached)
        assertTrue(reasons.isEmpty()); assertTrue(f.errors.isEmpty())
    }
}
