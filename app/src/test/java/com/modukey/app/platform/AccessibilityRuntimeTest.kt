package com.modukey.app.platform

import android.app.KeyguardManager
import android.media.AudioManager
import android.os.Looper
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import com.modukey.app.MainActivity
import com.modukey.app.NudgeApplication
import com.modukey.app.core.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.After
import androidx.test.core.app.ApplicationProvider
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
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[31,36])
@Suppress("DEPRECATION")
class AccessibilityRuntimeTest {
    @After fun stopApplicationScope() { ApplicationProvider.getApplicationContext<NudgeApplication>().onTerminate() }
    private fun await(condition:()->Boolean) {
        repeat(300) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(16)); if(condition()) return; Thread.sleep(5) }
        assertTrue("Service did not reach expected state",condition())
    }
    @Test fun `one service owns ordered handles and disposes a preview panel on editor exit`() {
        val controller=Robolectric.buildService(EdgeAccessibilityService::class.java).create()
        val service=controller.get(); val app=service.application as NudgeApplication
        // Android grants the merged AndroidX signature permission at install time; Robolectric does not.
        shadowOf(app).grantPermissions("${app.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
        shadowOf(service.getSystemService(PowerManager::class.java)).setIsInteractive(true)
        shadowOf(service.getSystemService(KeyguardManager::class.java)).setKeyguardLocked(false)
        await { app.configuration.state.value!=null }
        val handles=List(12) { i -> HandleConfig(id="runtime-$i",enabled=true,trigger=TriggerSettings(landscapeEnabled=true)) }
        val panel=SavedPanel(id="runtime-panel")
        app.configuration.update { AppConfiguration(enabled=true,handles=handles.take(1),panels=listOf(panel)) }
        ReflectionHelpers.callInstanceMethod<Void>(service,"onServiceConnected")
        for(count in listOf(1,4,12)) {
            app.configuration.update { it.copy(handles=handles.take(count)) }
            await { app.status.state.value.attachedHandleIds.size==count || app.status.state.value.error!=null }
            assertNull(app.diagnostics.details.value,app.status.state.value.error); assertTrue(app.status.state.value.connected)
        }
        app.configuration.update { it.editHandle("runtime-0") { h -> h.copy(trigger=h.trigger.copy(excludedPackages=setOf("test.game"))) } }
        fun window(pkg:String,activity:String) {
            val event=AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED).apply { packageName=pkg; className=activity }
            service.onAccessibilityEvent(event); event.recycle()
        }
        window("test.game","GameActivity")
        await { app.status.state.value.attachedHandleIds.size==11 }
        window(app.packageName,MainActivity::class.java.name)
        await { app.status.state.value.attachedHandleIds.size==12 }
        ReflectionHelpers.callInstanceMethod<Void>(service,"onServiceConnected")
        await { app.status.state.value.attachedHandleIds.size==12 }
        val token=Any(); app.editor.begin(token,EditorSelection("runtime-0",panel.id))
        await { app.status.state.value.handles.values.all { it==HandleAvailability.EDITING } }
        assertNull(app.status.state.value.error)
        app.editor.test(PanelTestRequest(panel.id,"runtime-0"))
        await { app.status.state.value.panelHandleId=="runtime-0" || app.status.state.value.error!=null }
        assertNull(app.status.state.value.error)
        app.configuration.update { it.copy(enabled=false) }
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(32))
        assertNull(app.status.state.value.error)
        app.editor.end(token)
        await { app.status.state.value.attachedHandleIds.isEmpty() && app.status.state.value.panelHandleId==null }
        app.configuration.update { it.copy(enabled=true) }
        await { app.status.state.value.attachedHandleIds.size==12 }
        assertNull(app.status.state.value.panelHandleId)
        controller.destroy(); shadowOf(Looper.getMainLooper()).idle()
        assertFalse(app.status.state.value.connected); assertFalse(app.status.state.value.triggerAttached)
    }

    @Test @Config(shadows=[RecordingAudio::class])
    fun `shared panel uses its invoking handle and combined actions preserve native volume`() {
        val controller=Robolectric.buildService(EdgeAccessibilityService::class.java).create()
        val service=controller.get(); val app=service.application as NudgeApplication
        shadowOf(app).grantPermissions("${app.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
        shadowOf(service.getSystemService(PowerManager::class.java)).setIsInteractive(true)
        shadowOf(service.getSystemService(KeyguardManager::class.java)).setKeyguardLocked(false)
        await { app.configuration.state.value!=null }
        val panel=SavedPanel(settings=PanelConfig(portrait=PanelLayout(offsetXDp=12f),animation=PanelAnimation.SLIDE))
        val left=HandleConfig(id="left",enabled=true,trigger=TriggerSettings(portrait=Placement(Edge.LEFT,.25f),landscapeEnabled=true),
            bindings=mapOf(GestureSlot.SWIPE_1 to GestureTarget.panel(panel.id,true)))
        val bottom=left.copy(id="bottom",trigger=left.trigger.copy(portrait=Placement(Edge.BOTTOM,.7f)))
        app.configuration.update { AppConfiguration(enabled=true,handles=listOf(left,bottom),panels=listOf(panel)) }
        ReflectionHelpers.callInstanceMethod<Void>(service,"onServiceConnected")
        await { app.status.state.value.attachedHandleIds.size==2 }
        val host=RecordingPanelHost(); ReflectionHelpers.setField(service,"panelHost",host)
        val audio=service.getSystemService(AudioManager::class.java)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC,3,0)
        val volume=audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        RecordingAudio.requests.clear()
        fun swipe(id:String) { ReflectionHelpers.callInstanceMethod<Void>(service,"activate",
            ClassParameter.from(String::class.java,id),ClassParameter.from(GestureKind::class.java,GestureKind.SWIPE)) }
        swipe(left.id)
        assertEquals(panel.id,app.status.state.value.panelId); assertEquals(left.id,app.status.state.value.panelHandleId)
        assertEquals(Edge.LEFT,host.spec!!.edge); assertEquals(panel.settings,host.spec!!.panel)
        val firstBounds=host.spec!!.geometry.bounds
        swipe(bottom.id)
        assertEquals(bottom.id,app.status.state.value.panelHandleId); assertEquals(Edge.BOTTOM,host.spec!!.edge)
        assertNotEquals(firstBounds,host.spec!!.geometry.bounds)
        assertEquals(listOf(Triple(AudioManager.ADJUST_SAME,AudioManager.STREAM_MUSIC,AudioManager.FLAG_SHOW_UI),
            Triple(AudioManager.ADJUST_SAME,AudioManager.STREAM_MUSIC,AudioManager.FLAG_SHOW_UI)),RecordingAudio.requests)
        assertEquals(volume,audio.getStreamVolume(AudioManager.STREAM_MUSIC))
        app.configuration.update { it.copy(handles=listOf(bottom)) }
        await { app.status.state.value.attachedHandleIds==setOf(bottom.id) }
        assertEquals(panel,app.configuration.state.value!!.panels.single())
        app.configuration.update { it.removePanel(panel.id) }
        await { app.status.state.value.panelId==null && host.spec==null }
        assertEquals(GestureTarget.VOLUME,app.configuration.state.value!!.handles.single().target(GestureSlot.SWIPE_1))
        swipe(bottom.id)
        assertEquals(3,RecordingAudio.requests.size); assertNull(app.status.state.value.panelId)
        assertNull(app.status.state.value.error)
        controller.destroy()
    }

    @Test fun `independent panel can be edited and tested with no handles and master off`() {
        val controller=Robolectric.buildService(EdgeAccessibilityService::class.java).create()
        val service=controller.get(); val app=service.application as NudgeApplication
        shadowOf(app).grantPermissions("${app.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
        shadowOf(service.getSystemService(PowerManager::class.java)).setIsInteractive(true)
        shadowOf(service.getSystemService(KeyguardManager::class.java)).setKeyguardLocked(false)
        await { app.configuration.state.value!=null }
        val panel=SavedPanel(settings=PanelConfig(buttons=listOf(PanelButton(action=ButtonAction.VOLUME_UP)),autoCloseEnabled=true,autoCloseMs=1_000))
        app.configuration.update { AppConfiguration(handles=emptyList(),panels=listOf(panel)) }
        ReflectionHelpers.callInstanceMethod<Void>(service,"onServiceConnected")
        val host=RecordingPanelHost(); ReflectionHelpers.setField(service,"panelHost",host)
        val token=Any(); app.editor.begin(token,EditorSelection(panelId=panel.id))
        await { host.spec?.editing==true }
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
        assertTrue(host.spec!!.editing)
        assertTrue(app.status.state.value.attachedHandleIds.isEmpty())
        app.editor.test(PanelTestRequest(panel.id))
        await { app.status.state.value.panelId==panel.id && host.spec?.editing==false }
        assertNull(app.status.state.value.panelHandleId)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(600))
        ReflectionHelpers.callInstanceMethod<Void>(service,"runButton",ClassParameter.from(PanelButton::class.java,panel.settings.buttons.single()))
        assertEquals(1,host.soundUpdates)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(600))
        assertEquals(panel.id,app.status.state.value.panelId)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(400))
        assertNull(app.status.state.value.panelId); assertTrue(host.spec!!.editing)
        app.editor.end(token)
        await { host.spec==null && app.status.state.value.panelId==null }
        assertNull(app.status.state.value.error)
        controller.destroy()
    }

    @Test fun `panel expiry resets swipe progression and replacement cancels the old timeout`() {
        val controller=Robolectric.buildService(EdgeAccessibilityService::class.java).create()
        val service=controller.get(); val app=service.application as NudgeApplication
        shadowOf(app).grantPermissions("${app.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
        shadowOf(service.getSystemService(PowerManager::class.java)).setIsInteractive(true)
        shadowOf(service.getSystemService(KeyguardManager::class.java)).setKeyguardLocked(false)
        await { app.configuration.state.value!=null }
        val timed=SavedPanel(settings=PanelConfig(autoCloseEnabled=true,autoCloseMs=1_000))
        val untimed=SavedPanel()
        val h=HandleConfig(id="timed-handle",enabled=true,trigger=TriggerSettings(landscapeEnabled=true),bindings=mapOf(
            GestureSlot.SWIPE_1 to GestureTarget.panel(timed.id),GestureSlot.SWIPE_2 to GestureTarget.panel(untimed.id)))
        app.configuration.update { AppConfiguration(enabled=true,handles=listOf(h),panels=listOf(timed,untimed)) }
        ReflectionHelpers.callInstanceMethod<Void>(service,"onServiceConnected")
        await { app.status.state.value.attachedHandleIds==setOf(h.id) }
        val host=RecordingPanelHost(); ReflectionHelpers.setField(service,"panelHost",host)
        fun swipe() { ReflectionHelpers.callInstanceMethod<Void>(service,"activate",
            ClassParameter.from(String::class.java,h.id),ClassParameter.from(GestureKind::class.java,GestureKind.SWIPE)) }
        swipe()
        assertEquals(timed.id,app.status.state.value.panelId)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500)); service.onInterrupt()
        assertEquals(timed.id,app.status.state.value.panelId)
        // Leave room for window frames; an interrupt must not extend the original one-second deadline.
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(600))
        assertNull(app.status.state.value.panelId); assertNull(host.spec)
        swipe()
        assertEquals(timed.id,app.status.state.value.panelId)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500)); swipe()
        assertEquals(untimed.id,app.status.state.value.panelId)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
        assertEquals(untimed.id,app.status.state.value.panelId)
        assertNull(app.status.state.value.error)
        controller.destroy()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
        assertFalse(app.status.state.value.connected)
    }

    @Test fun `direct gestures dispatch their configured action and slot and respect availability`() {
        val controller=Robolectric.buildService(EdgeAccessibilityService::class.java).create()
        val service=controller.get(); val app=service.application as NudgeApplication
        shadowOf(app).grantPermissions("${app.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
        val power=service.getSystemService(PowerManager::class.java)
        val keyguard=service.getSystemService(KeyguardManager::class.java)
        shadowOf(power).setIsInteractive(true); shadowOf(keyguard).setKeyguardLocked(false)
        await { app.configuration.state.value!=null }
        val panel=SavedPanel()
        val slots=listOf(GestureSlot.SWIPE_1,GestureSlot.SWIPE_2,GestureSlot.SWIPE_3,GestureSlot.SWIPE_HOLD,GestureSlot.TAP,GestureSlot.HOLD)
        val buttons=listOf(ButtonAction.VOLUME_UP,ButtonAction.VOLUME_DOWN,ButtonAction.MEDIA_MUTE,ButtonAction.SILENT,ButtonAction.OPEN_APP,ButtonAction.LOCK)
            .map { PanelButton(action=it,appPackage="test.camera",appActivity="test.camera.Main",showVolumePopup=true) }
        val direct=slots.zip(buttons).associate { (slot,button) -> slot to GestureTarget.direct(button) }
        val h=HandleConfig(enabled=true,trigger=TriggerSettings(landscapeEnabled=true),bindings=direct+(GestureSlot.SWIPE_1 to GestureTarget.panel(panel.id)))
        app.configuration.update { AppConfiguration(enabled=true,handles=listOf(h),panels=listOf(panel)) }
        ReflectionHelpers.callInstanceMethod<Void>(service,"onServiceConnected")
        await { app.status.state.value.attachedHandleIds==setOf(h.id) }
        val host=RecordingPanelHost(); ReflectionHelpers.setField(service,"panelHost",host)
        val calls=mutableListOf<Pair<GestureSlot?,PanelButton>>()
        var succeed=true
        ReflectionHelpers.setField(service,"actions",object:ActionExecutor {
            override fun snapshot()=SoundState()
            override fun execute(handleId:String,button:PanelButton,testing:Boolean,gestureSlot:GestureSlot?):ActionResult {
                assertEquals(h.id,handleId); assertFalse(testing); assertNull(host.spec)
                calls+=gestureSlot to button
                return ActionResult(succeeded=succeed)
            }
        })
        fun activate(kind:GestureKind) { ReflectionHelpers.callInstanceMethod<Void>(service,"activate",
            ClassParameter.from(String::class.java,h.id),ClassParameter.from(GestureKind::class.java,kind)) }
        activate(GestureKind.SWIPE); assertEquals(panel.id,app.status.state.value.panelId)
        activate(GestureKind.SWIPE); activate(GestureKind.SWIPE)
        assertEquals(listOf(slots[1] to buttons[1],slots[2] to buttons[2]),calls)
        assertNull(app.status.state.value.panelId)
        activate(GestureKind.SWIPE_HOLD); activate(GestureKind.TAP); activate(GestureKind.HOLD)
        assertEquals(slots.drop(1).zip(buttons.drop(1)),calls)
        app.configuration.update { it.editHandle(h.id) { current -> current.copy(bindings=direct) } }
        activate(GestureKind.SWIPE)
        assertEquals(GestureSlot.SWIPE_1 to buttons.first(),calls.last())
        // A failed action must not advance to the next swipe stage.
        activate(GestureKind.TAP); succeed=false
        activate(GestureKind.SWIPE); activate(GestureKind.SWIPE)
        assertEquals(listOf(GestureSlot.SWIPE_1,GestureSlot.SWIPE_1),calls.takeLast(2).map { it.first })
        val before=calls.size
        shadowOf(keyguard).setKeyguardLocked(true); activate(GestureKind.HOLD)
        assertEquals(before,calls.size)
        app.configuration.update { it.editHandle(h.id) { current -> current.copy(trigger=current.trigger.copy(lockScreenEnabled=true)) } }
        activate(GestureKind.HOLD); assertEquals(before+1,calls.size)
        shadowOf(power).setIsInteractive(false); activate(GestureKind.HOLD)
        assertEquals(before+1,calls.size)
        shadowOf(power).setIsInteractive(true)
        app.configuration.update { it.copy(enabled=false) }; activate(GestureKind.HOLD)
        assertEquals(before+1,calls.size)
        controller.destroy()
    }

    private class RecordingPanelHost:PanelHost {
        var spec:PanelSpec?=null; var soundUpdates=0
        override fun show(spec:PanelSpec) { this.spec=spec }
        override fun hide(animate:Boolean) { spec=null }
        override fun refreshSound() { soundUpdates++ }
    }
    @Implements(AudioManager::class)
    class RecordingAudio:ShadowAudioManager() {
        companion object { val requests=mutableListOf<Triple<Int,Int,Int>>() }
        @Implementation fun adjustSuggestedStreamVolume(direction:Int,stream:Int,flags:Int) { requests+=Triple(direction,stream,flags) }
    }
}
