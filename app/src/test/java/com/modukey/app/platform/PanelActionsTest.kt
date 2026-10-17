package com.modukey.app.platform

import android.media.AudioManager
import android.app.NotificationManager
import com.modukey.app.core.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class PanelActionsTest {
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
}
