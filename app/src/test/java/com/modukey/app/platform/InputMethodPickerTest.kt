package com.modukey.app.platform

import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Looper
import android.os.PowerManager
import android.view.inputmethod.InputMethodManager
import androidx.test.core.app.ApplicationProvider
import com.modukey.app.NudgeApplication
import com.modukey.app.R
import com.modukey.app.core.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadows.ShadowInputMethodManager
import org.robolectric.shadows.ShadowKeyguardManager
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[31,36], shadows=[InputMethodPickerTest.RecordingInput::class,InputMethodPickerTest.RecordingKeyguard::class])
@Suppress("DEPRECATION")
class InputMethodPickerTest {
    private val app get()=ApplicationProvider.getApplicationContext<NudgeApplication>()
    private val button=PanelButton(action=ButtonAction.INPUT_METHOD_PICKER)
    private val handle=HandleConfig(enabled=true,bindings=mapOf(GestureSlot.HOLD to GestureTarget.direct(button)))
    private val service by lazy { Robolectric.buildService(EdgeAccessibilityService::class.java).create().get() }
    private val keyguard get()=app.getSystemService(KeyguardManager::class.java)
    private val input get()=app.getSystemService(InputMethodManager::class.java)
    private val main get()=shadowOf(Looper.getMainLooper())
    private fun awaitLoaded() {
        repeat(300) { main.idleFor(Duration.ofMillis(16)); if(app.configuration.state.value!=null) return; Thread.sleep(5) }
        fail("Configuration did not load")
    }
    @Before fun prepare() {
        RecordingInput.requests=0; RecordingInput.fail=false; RecordingKeyguard.callback=null
        shadowOf(app).grantPermissions("${app.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
        shadowOf(app.packageManager).setSystemFeature(PackageManager.FEATURE_INPUT_METHODS,true)
        shadowOf(app.getSystemService(PowerManager::class.java)).setIsInteractive(true)
        shadowOf(keyguard).setKeyguardLocked(false)
        awaitLoaded()
        app.configuration.update { AppConfiguration(enabled=true,handles=listOf(handle)) }
        app.status.report(ServiceState(connected=true))
    }
    @After fun stop() { app.onTerminate() }
    private fun request(panel:Boolean=false):ActivityController<UnlockActionActivity> {
        val selected=if(panel) button.copy(id=newId()) else button
        if(panel) app.configuration.update { it.copy(panels=listOf(SavedPanel(settings=PanelConfig(buttons=listOf(selected))))) }
        val configuration=checkNotNull(app.configuration.state.value)
        assertEquals(configuration,com.modukey.app.data.ConfigurationCodec.decode(com.modukey.app.data.ConfigurationCodec.encode(configuration)))
        val result=PanelActions(service).execute(handle.id,selected,false,if(panel) null else GestureSlot.HOLD)
        assertTrue(result.succeeded); assertTrue(result.close)
        val intent=shadowOf(service).nextStartedActivity
        assertEquals(UnlockActionActivity::class.java.name,intent.component!!.className)
        assertEquals(!panel,app.unlocks.peek(intent.getStringExtra("token")!!)!!.gestureSlot!=null)
        return Robolectric.buildActivity(UnlockActionActivity::class.java,intent).create().start().resume()
    }
    private fun focus(controller:ActivityController<UnlockActionActivity>) {
        controller.visible().windowFocusChanged(true); main.idle()
    }
    @Test fun `panel and direct actions request one native picker after window focus without showing the keyboard`() {
        for(panel in listOf(false,true)) {
            val controller=request(panel)
            val initial=RecordingInput.requests
            assertFalse(shadowOf(input).isSoftInputVisible)
            focus(controller)
            assertEquals(initial+1,RecordingInput.requests)
            assertFalse(shadowOf(input).isSoftInputVisible)
            controller.windowFocusChanged(false); controller.windowFocusChanged(true); main.idle()
            assertTrue(controller.get().isFinishing)
            main.idleFor(Duration.ofSeconds(3)); assertEquals(initial+1,RecordingInput.requests)
            controller.pause().stop().destroy()
        }
    }
    @Test fun `a suppressed or failed picker cannot leave the transparent activity in front`() {
        val suppressed=request(); focus(suppressed)
        assertFalse(suppressed.get().isFinishing)
        main.idleFor(Duration.ofMillis(2_001)); assertTrue(suppressed.get().isFinishing)
        suppressed.pause().stop().destroy()
        RecordingInput.fail=true
        val failed=request(); focus(failed)
        assertTrue(failed.get().isFinishing)
        failed.pause().stop().destroy()
    }
    @Test fun `changed assignments and lost permission are rejected before requesting the picker`() {
        for(change in 0..3) {
            app.configuration.update { AppConfiguration(enabled=true,handles=listOf(handle)) }
            app.status.report(ServiceState(connected=true))
            val controller=request()
            when(change) {
                0 -> app.configuration.update { it.editHandle(handle.id) { h -> h.copy(bindings=emptyMap()) } }
                1 -> app.configuration.update { it.editHandle(handle.id) { h -> h.copy(enabled=false) } }
                2 -> app.configuration.update { it.copy(enabled=false) }
                else -> app.status.report(ServiceState())
            }
            focus(controller)
            assertEquals(0,RecordingInput.requests); assertTrue(controller.get().isFinishing)
            controller.pause().stop().destroy()
        }
    }
    @Test fun `lock screen cancellation and a disabled lock screen option cancel continuation`() {
        app.configuration.update { it.editHandle(handle.id) { h -> h.copy(trigger=h.trigger.copy(lockScreenEnabled=true)) } }
        shadowOf(keyguard).setKeyguardLocked(true)
        val cancelled=request(); focus(cancelled)
        assertEquals(0,RecordingInput.requests)
        RecordingKeyguard.callback!!.onDismissCancelled(); assertTrue(cancelled.get().isFinishing)
        cancelled.pause().stop().destroy()
        val changed=request(); focus(changed)
        app.configuration.update { it.editHandle(handle.id) { h -> h.copy(trigger=h.trigger.copy(lockScreenEnabled=false)) } }
        shadowOf(keyguard).setKeyguardLocked(false)
        RecordingKeyguard.callback!!.onDismissSucceeded(); main.idle()
        assertEquals(0,RecordingInput.requests); assertTrue(changed.get().isFinishing)
        changed.pause().stop().destroy()
    }
    @Test fun `lock screen success requests the picker once after authentication`() {
        app.configuration.update { it.editHandle(handle.id) { h -> h.copy(trigger=h.trigger.copy(lockScreenEnabled=true)) } }
        shadowOf(keyguard).setKeyguardLocked(true)
        val controller=request(); focus(controller)
        assertEquals(0,RecordingInput.requests)
        shadowOf(keyguard).setKeyguardLocked(false)
        RecordingKeyguard.callback!!.onDismissSucceeded(); main.idle()
        assertEquals(1,RecordingInput.requests)
        controller.pause().stop().destroy()
    }
    @Test fun `screen off cancels the pending request and unsupported devices report failure`() {
        val controller=request()
        app.sendBroadcast(Intent(Intent.ACTION_SCREEN_OFF)); main.idle()
        assertTrue(controller.get().isFinishing); assertEquals(0,RecordingInput.requests)
        controller.pause().stop().destroy()
        shadowOf(app.packageManager).setSystemFeature(PackageManager.FEATURE_INPUT_METHODS,false)
        val result=PanelActions(service).execute(handle.id,button,false,GestureSlot.HOLD)
        assertFalse(result.succeeded); assertEquals(R.string.input_method_picker_failed,result.message)
        assertNull(shadowOf(service).nextStartedActivity)
    }
    @Implements(InputMethodManager::class)
    class RecordingInput:ShadowInputMethodManager() {
        companion object { var requests=0; var fail=false }
        @Implementation fun showInputMethodPicker() { requests++; if(fail) throw SecurityException("Denied") }
    }
    @Implements(KeyguardManager::class)
    class RecordingKeyguard:ShadowKeyguardManager() {
        companion object { var callback:KeyguardManager.KeyguardDismissCallback?=null }
        @Implementation override fun requestDismissKeyguard(activity:Activity,callback:KeyguardManager.KeyguardDismissCallback) { RecordingKeyguard.callback=callback }
    }
}
