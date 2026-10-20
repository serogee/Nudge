package com.modukey.app.platform

import android.animation.ValueAnimator
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
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
class BrightnessPopupAnimationTest {
    private val app get() = ApplicationProvider.getApplicationContext<NudgeApplication>()
    private val hosts = mutableListOf<BrightnessPopupHost>()
    private fun scale(value: Float) = ReflectionHelpers.callStaticMethod<Void>(ValueAnimator::class.java,
        "setDurationScale", ClassParameter.from(Float::class.javaPrimitiveType, value))
    private fun idle(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
    @After fun cleanup() { hosts.forEach { it.hide() }; scale(1f); app.onTerminate() }
    private class Windows(private val delegate: WindowManager) : WindowManager by delegate {
        val attached = linkedMapOf<View, WindowManager.LayoutParams>()
        override fun addView(view: View, params: ViewGroup.LayoutParams) {
            delegate.addView(view, params); attached[view] = params as WindowManager.LayoutParams
        }
        override fun updateViewLayout(view: View, params: ViewGroup.LayoutParams) {
            delegate.updateViewLayout(view, params); attached[view] = params as WindowManager.LayoutParams
        }
        override fun removeViewImmediate(view: View) { delegate.removeViewImmediate(view); attached.remove(view) }
    }
    private fun fixture(): Pair<BrightnessPopupHost, Windows> {
        val windows = Windows(app.getSystemService(WindowManager::class.java))
        lateinit var host: BrightnessPopupHost
        host = BrightnessPopupHost(app, windows, app.brightness, { host.hide(animate = true) }, { fail("Window failed: $it") })
        hosts += host
        return host to windows
    }
    private val screen = ScreenSpace(400f, 800f, 1f, Insets(), false)
    @Test fun allPanelEffectsAnimateOnThePopupSideAndClosingDisablesInputImmediately() {
        scale(1f)
        val (host, windows) = fixture()
        for (side in listOf(BrightnessSide.LEFT, BrightnessSide.RIGHT)) for (animation in PanelAnimation.entries) {
            val settings = BrightnessPopupSettings(side = side, animation = animation, closeAnimation = animation)
            assertTrue(host.show(settings, screen, false))
            val root = windows.attached.keys.single()
            assertEquals(if (animation == PanelAnimation.NONE) 1f else 0f, root.alpha)
            if (animation == PanelAnimation.SLIDE || animation == PanelAnimation.SCALE) {
                assertTrue(if (side == BrightnessSide.LEFT) root.translationX < 0f else root.translationX > 0f)
                assertEquals(if (animation == PanelAnimation.SCALE) .88f else 1f, root.scaleX)
            }
            idle(250)
            assertEquals(1f, root.alpha); assertEquals(0f, root.translationX); assertEquals(1f, root.scaleX)
            host.show(settings, screen, false)
            assertSame(root, windows.attached.keys.single()); assertEquals(1f, root.alpha)
            host.hide(animate = true)
            assertFalse(host.visible)
            if (animation != PanelAnimation.NONE) {
                assertTrue(root in windows.attached)
                val flags = windows.attached.getValue(root).flags
                assertTrue(flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE != 0)
                assertTrue(flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0)
                host.hide(animate = true)
                assertTrue("Refresh must preserve the exit", root in windows.attached)
            }
            idle(200)
            assertTrue(windows.attached.isEmpty())
        }
    }
    @Test fun replacementPreviewDisabledAnimationsAndCleanupCancelOldTransitions() {
        scale(1f)
        val (host, windows) = fixture()
        val settings = BrightnessPopupSettings(animation = PanelAnimation.SCALE, closeAnimation = PanelAnimation.FADE)
        host.show(settings, screen, false); idle(250)
        host.hide(animate = true)
        val old = windows.attached.keys.single()
        host.show(settings, screen, false)
        assertEquals(1, windows.attached.size); assertFalse(old in windows.attached)
        idle(250); assertTrue(host.visible)
        host.hide(animate = true); host.hide()
        idle(250); assertTrue(windows.attached.isEmpty())
        host.show(settings, screen, true)
        assertEquals(1f, windows.attached.keys.single().alpha)
        host.hide(animate = true); assertTrue(windows.attached.isEmpty())
        scale(0f)
        host.show(settings, screen, false)
        val root = windows.attached.keys.single()
        assertEquals(1f, root.alpha); assertEquals(1f, root.scaleX); assertEquals(0f, root.translationX)
        host.hide(animate = true); assertTrue(windows.attached.isEmpty())
    }
    @Test fun timeoutUsesTheClosingAnimationAndLeavesNoIdleWindow() {
        scale(1f)
        val (host, windows) = fixture()
        host.show(BrightnessPopupSettings(timeoutMs = 1000, closeAnimation = PanelAnimation.FADE), screen, false)
        idle(1001)
        assertFalse(host.visible)
        assertTrue(windows.attached.isEmpty())
    }
}
