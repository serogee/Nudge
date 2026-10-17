package com.modukey.app.platform

import android.app.KeyguardManager
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
import org.robolectric.util.ReflectionHelpers
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
        app.configuration.update { AppConfiguration(enabled=true,handles=handles.take(1)) }
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
        val token=Any(); app.editor.begin(token,EditorSelection("runtime-0",0))
        await { app.status.state.value.handles.values.all { it==HandleAvailability.EDITING } }
        assertNull(app.status.state.value.error)
        app.editor.test(PanelTestRequest("runtime-0",0))
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
}
