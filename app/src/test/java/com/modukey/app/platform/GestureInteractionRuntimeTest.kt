package com.modukey.app.platform

import android.app.KeyguardManager
import android.os.Looper
import android.os.PowerManager
import android.os.VibratorManager
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
import org.robolectric.util.ReflectionHelpers.ClassParameter
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36])
class GestureInteractionRuntimeTest {
    private val app get() = ApplicationProvider.getApplicationContext<NudgeApplication>()
    @After fun cleanup() { app.onTerminate() }
    private class Host : PanelHost {
        val shown = mutableListOf<PanelSpec>()
        var settled = false
        override fun show(spec: PanelSpec) { shown += spec }
        override fun hide(animate: Boolean) {}
        override fun refreshSound() {}
        override fun pull(distance: Float, edge: Edge, settle: Boolean) { if (settle) settled = true }
    }
    @Suppress("DEPRECATION")
    @Test fun pullPreviewDoesNotActivateUntilTheGestureAndKeepsTheLivePanelSuspended() {
        val controller = Robolectric.buildService(EdgeAccessibilityService::class.java).create()
        val service = controller.get()
        try {
            shadowOf(app).grantPermissions(android.Manifest.permission.VIBRATE, "${app.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
            shadowOf(service.getSystemService(PowerManager::class.java)).setIsInteractive(true)
            shadowOf(service.getSystemService(KeyguardManager::class.java)).setKeyguardLocked(false)
            repeat(300) { if (app.configuration.state.value == null) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(16)); Thread.sleep(5) } }
            val panel = SavedPanel(name = "Tools")
            val handle = HandleConfig(id = "pull", enabled = true, trigger = TriggerSettings(landscapeEnabled = true,
                swipeHoldFeedback = GestureFeedback(true, .8f)), bindings = mapOf(GestureSlot.SWIPE_HOLD to GestureTarget.panel(panel.id)))
            app.configuration.update { AppConfiguration(enabled = true, handles = listOf(handle), panels = listOf(panel)) }
            ReflectionHelpers.callInstanceMethod<Void>(service, "onServiceConnected")
            val preview = Host(); val live = Host()
            ReflectionHelpers.setField(service, "pullHost", preview); ReflectionHelpers.setField(service, "panelHost", live)
            val vibrator = service.getSystemService(VibratorManager::class.java).defaultVibrator
            shadowOf(vibrator).setHasVibrator(true)
            fun pull(distance: Float?) = ReflectionHelpers.callInstanceMethod<Void>(service, "updatePull",
                ClassParameter.from(String::class.java, handle.id), ClassParameter.from(Float::class.javaObjectType, distance),
                ClassParameter.from(Boolean::class.javaPrimitiveType, true))
            pull(100f); ReflectionHelpers.callInstanceMethod<Void>(service, "refresh")
            assertTrue(preview.shown.last().editing); assertTrue(live.shown.isEmpty())
            assertFalse(shadowOf(vibrator).isVibrating)
            ReflectionHelpers.callInstanceMethod<Void>(service, "activate", ClassParameter.from(String::class.java, handle.id),
                ClassParameter.from(GestureKind::class.java, GestureKind.SWIPE_HOLD))
            assertTrue(live.shown.last().inputSuspended); assertTrue(shadowOf(vibrator).isVibrating)
            pull(null); ReflectionHelpers.callInstanceMethod<Void>(service, "refresh")
            assertTrue(live.settled); assertFalse(live.shown.last().inputSuspended)
        } finally { controller.destroy() }
    }
}
