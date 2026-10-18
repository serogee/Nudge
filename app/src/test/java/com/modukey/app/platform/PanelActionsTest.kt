package com.modukey.app.platform

import android.media.AudioManager
import android.app.NotificationManager
import android.app.KeyguardManager
import android.os.PowerManager
import com.modukey.app.NudgeApplication
import com.modukey.app.core.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implements
import org.robolectric.annotation.Implementation
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowAudioManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class PanelActionsTest {
    @Test @Config(shadows = [RecordingAudio::class])
    fun `volume popup option changes only the UI flag and still uses one media step`() {
        val service = Robolectric.buildService(EdgeAccessibilityService::class.java).create().get()
        val audio = service.getSystemService(AudioManager::class.java)
        val recording = Shadow.extract<RecordingAudio>(audio)
        val actions = PanelActions(service)
        for (show in listOf(false, true)) for (action in listOf(ButtonAction.VOLUME_UP, ButtonAction.VOLUME_DOWN)) {
            val result = actions.execute("handle", PanelButton(action = action, showVolumePopup = show), false)
            assertEquals(Triple(AudioManager.STREAM_MUSIC, if (action == ButtonAction.VOLUME_UP) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
                if (show) AudioManager.FLAG_SHOW_UI else 0), recording.last)
            assertFalse(result.close)
        }
    }
    @Implements(AudioManager::class)
    class RecordingAudio : ShadowAudioManager() {
        var last: Triple<Int, Int, Int>? = null
        @Implementation override fun adjustStreamVolume(stream: Int, direction: Int, flags: Int) { last = Triple(stream, direction, flags) }
    }
    @Test fun `media adjustments use one step and do not close the panel or change the ringtone`() {
        val service=Robolectric.buildService(EdgeAccessibilityService::class.java).create().get()
        val audio=service.getSystemService(AudioManager::class.java)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC,3,0)
        val ringer=audio.ringerMode; val actions=PanelActions(service)
        val raised=actions.execute("handle",PanelButton(action=ButtonAction.VOLUME_UP),false)
        assertEquals(4,audio.getStreamVolume(AudioManager.STREAM_MUSIC)); assertFalse(raised.close); assertNull(raised.message)
        actions.execute("handle",PanelButton(action=ButtonAction.VOLUME_DOWN),false)
        assertEquals(3,audio.getStreamVolume(AudioManager.STREAM_MUSIC)); assertEquals(ringer,audio.ringerMode)
    }
    @Test fun `silent and vibrate modes toggle back to normal when access is granted`() {
        val service=Robolectric.buildService(EdgeAccessibilityService::class.java).create().get()
        shadowOf(service.getSystemService(NotificationManager::class.java)).setNotificationPolicyAccessGranted(true)
        val audio=service.getSystemService(AudioManager::class.java); val actions=PanelActions(service)
        for((action,mode) in listOf(ButtonAction.SILENT to AudioManager.RINGER_MODE_SILENT,ButtonAction.VIBRATE to AudioManager.RINGER_MODE_VIBRATE)) {
            audio.ringerMode=AudioManager.RINGER_MODE_NORMAL
            assertNull(actions.execute("handle",PanelButton(action=action),false).message); assertEquals(mode,audio.ringerMode)
            actions.execute("handle",PanelButton(action=action),false); assertEquals(AudioManager.RINGER_MODE_NORMAL,audio.ringerMode)
        }
    }
    @Test fun `unlock continuation validates the exact gesture source instead of requiring a panel button`() {
        val button=PanelButton(action=ButtonAction.OPEN_APP,appPackage="test.camera")
        val handle=HandleConfig(bindings=mapOf(GestureSlot.HOLD to GestureTarget.direct(button)))
        val config=AppConfiguration(enabled=true,handles=listOf(handle))
        val pending=PendingLaunch(handle.id,button,false,false,GestureSlot.HOLD)
        assertEquals(button,pending.currentButton(config))
        assertNull(pending.copy(gestureSlot=GestureSlot.TAP).currentButton(config))
        assertNull(pending.currentButton(config.copy(handles=emptyList())))
        assertNull(pending.currentButton(config.editHandle(handle.id) { it.copy(bindings=emptyMap()) }))
        val changed=config.editHandle(handle.id) {
            it.copy(bindings=mapOf(GestureSlot.HOLD to GestureTarget.direct(button.copy(appPackage="other.app"))))
        }
        assertNull(pending.currentButton(changed))
        val panel=SavedPanel(settings=PanelConfig(buttons=listOf(button)))
        assertNull(pending.currentButton(AppConfiguration(handles=listOf(handle.copy(bindings=emptyMap())),panels=listOf(panel))))
        assertEquals(button,pending.copy(gestureSlot=null).currentButton(config.copy(panels=listOf(panel))))
    }
    @Test fun `locked direct app and policy actions retain their gesture source in one shot unlock requests`() {
        val service=Robolectric.buildService(EdgeAccessibilityService::class.java).create().get()
        val app=service.application as NudgeApplication
        shadowOf(service.getSystemService(PowerManager::class.java)).setIsInteractive(true)
        shadowOf(service.getSystemService(KeyguardManager::class.java)).setKeyguardLocked(true)
        shadowOf(service.getSystemService(NotificationManager::class.java)).setNotificationPolicyAccessGranted(false)
        val executor=PanelActions(service)
        for (action in listOf(ButtonAction.OPEN_APP,ButtonAction.PHONE_SETTINGS,ButtonAction.NUDGE_SETTINGS,ButtonAction.DND)) {
            val button=PanelButton(action=action,appPackage="test.camera")
            val result=executor.execute("side",button,false,GestureSlot.HOLD)
            assertTrue(result.succeeded); assertTrue(result.close)
            val intent=shadowOf(service).nextStartedActivity
            assertEquals(UnlockActionActivity::class.java.name,intent.component!!.className)
            val token=intent.getStringExtra("token")!!
            assertEquals(PendingLaunch("side",button,false,action==ButtonAction.DND,GestureSlot.HOLD),app.unlocks.take(token))
            assertNull(app.unlocks.take(token))
        }
        app.onTerminate()
    }
}
