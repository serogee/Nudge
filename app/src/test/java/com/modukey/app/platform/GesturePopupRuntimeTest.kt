package com.modukey.app.platform

import android.app.KeyguardManager
import android.media.AudioManager
import android.os.Looper
import android.os.PowerManager
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
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowAudioManager
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter
import androidx.test.core.app.ApplicationProvider
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36], shadows = [GesturePopupRuntimeTest.RecordingAudio::class])
class GesturePopupRuntimeTest {
    @After fun cleanup() { ApplicationProvider.getApplicationContext<NudgeApplication>().onTerminate() }
    @Suppress("DEPRECATION")
    @Test fun `direct volume uses one request and other direct actions request the popup first`() {
        val controller = Robolectric.buildService(EdgeAccessibilityService::class.java).create()
        val service = controller.get(); val app = service.application as NudgeApplication
        shadowOf(app).grantPermissions("${app.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
        shadowOf(service.getSystemService(PowerManager::class.java)).setIsInteractive(true)
        shadowOf(service.getSystemService(KeyguardManager::class.java)).setKeyguardLocked(false)
        repeat(300) {
            if (app.configuration.state.value == null) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(16)); Thread.sleep(5) }
        }
        assertNotNull(app.configuration.state.value)
        val h = HandleConfig(id = "popup", enabled = true, trigger = TriggerSettings(landscapeEnabled = true), bindings = mapOf(
            GestureSlot.TAP to GestureTarget(volume = true, action = PanelButton(action = ButtonAction.VOLUME_UP)),
            GestureSlot.HOLD to GestureTarget(volume = true, action = PanelButton(action = ButtonAction.LOCK))))
        app.configuration.update { AppConfiguration(enabled = true, handles = listOf(h)) }
        ReflectionHelpers.callInstanceMethod<Void>(service, "onServiceConnected")
        fun gesture(kind: GestureKind) { ReflectionHelpers.callInstanceMethod<Void>(service, "activate",
            ClassParameter.from(String::class.java, h.id), ClassParameter.from(GestureKind::class.java, kind)) }
        RecordingAudio.events.clear(); RecordingAudio.flags.clear()
        gesture(GestureKind.TAP)
        assertEquals(listOf("adjust"), RecordingAudio.events)
        assertEquals(listOf(AudioManager.FLAG_SHOW_UI), RecordingAudio.flags)
        var actions = 0
        ReflectionHelpers.setField(service, "actions", object : ActionExecutor {
            override fun snapshot() = SoundState()
            override fun execute(handleId: String, button: PanelButton, testing: Boolean, gestureSlot: GestureSlot?): ActionResult {
                assertEquals(listOf("popup"), RecordingAudio.events)
                assertEquals(ButtonAction.LOCK, button.action); actions++
                return ActionResult(close = true)
            }
        })
        RecordingAudio.events.clear()
        gesture(GestureKind.HOLD)
        assertEquals(1, actions); assertEquals(listOf("popup"), RecordingAudio.events)
        controller.destroy()
    }
    @Implements(AudioManager::class)
    class RecordingAudio : ShadowAudioManager() {
        companion object { val events = mutableListOf<String>(); val flags = mutableListOf<Int>() }
        @Implementation override fun adjustStreamVolume(stream: Int, direction: Int, flags: Int) {
            events += "adjust"; RecordingAudio.flags += flags
        }
        @Implementation fun adjustSuggestedStreamVolume(direction: Int, stream: Int, flags: Int) { events += "popup" }
    }
}
