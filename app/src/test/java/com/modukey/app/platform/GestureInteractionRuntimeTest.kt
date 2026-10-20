package com.modukey.app.platform

import android.app.KeyguardManager
import android.os.Looper
import android.os.PowerManager
import android.os.VibratorManager
import android.view.MotionEvent
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
import org.robolectric.util.ReflectionHelpers
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36], qualifiers = "mdpi")
class GestureInteractionRuntimeTest {
    private val app get() = ApplicationProvider.getApplicationContext<NudgeApplication>()
    @After fun cleanup() { app.onTerminate() }
    private class Host : PanelHost {
        val shown = mutableListOf<PanelSpec>()
        override fun show(spec: PanelSpec) { shown += spec }
        override fun hide(animate: Boolean) {}
        override fun refreshSound() {}
    }
    @Suppress("DEPRECATION")
    @Test fun draggingAHandleShowsThePanelOnlyWhenTheGestureCompletes() {
        val controller = Robolectric.buildService(EdgeAccessibilityService::class.java).create()
        val service = controller.get()
        try {
            shadowOf(app).grantPermissions(android.Manifest.permission.VIBRATE, "${app.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
            shadowOf(service.getSystemService(PowerManager::class.java)).setIsInteractive(true)
            shadowOf(service.getSystemService(KeyguardManager::class.java)).setKeyguardLocked(false)
            repeat(300) { if (app.configuration.state.value == null) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(16)); Thread.sleep(5) } }
            val panel = SavedPanel(name = "Tools")
            val handle = HandleConfig(id = "pull", enabled = true, trigger = TriggerSettings(landscapeEnabled = true, portrait = Placement(Edge.LEFT),
                swipeHoldFeedback = GestureFeedback(true, .8f)), bindings = mapOf(GestureSlot.SWIPE_HOLD to GestureTarget.panel(panel.id)))
            app.configuration.update { AppConfiguration(enabled = true, handles = listOf(handle), panels = listOf(panel)) }
            ReflectionHelpers.callInstanceMethod<Void>(service, "onServiceConnected")
            val live = Host()
            ReflectionHelpers.setField(service, "panelHost", live)
            ReflectionHelpers.callInstanceMethod<Void>(service, "refresh")
            val view = ReflectionHelpers.getField<Map<String, HandleTriggerView>>(service, "views").getValue(handle.id)
            fun touch(action: Int, time: Long, x: Float) {
                val event = MotionEvent.obtain(0, time, action, x, 20f, 0)
                try { assertTrue(view.onTouchEvent(event)) } finally { event.recycle() }
            }
            val vibrator = service.getSystemService(VibratorManager::class.java).defaultVibrator
            shadowOf(vibrator).setHasVibrator(true)
            touch(MotionEvent.ACTION_DOWN, 100, 1f)
            touch(MotionEvent.ACTION_MOVE, 200, 31f)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(16))
            assertTrue(live.shown.isEmpty())
            assertFalse(shadowOf(vibrator).isVibrating)
            touch(MotionEvent.ACTION_UP, 800, 31f)
            assertEquals(panel.settings, live.shown.last().panel)
            assertFalse(live.shown.last().editing)
            assertTrue(shadowOf(vibrator).isVibrating)
        } finally { controller.destroy() }
    }
}
