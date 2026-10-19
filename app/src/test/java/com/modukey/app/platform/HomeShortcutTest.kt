package com.modukey.app.platform

import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent
import android.content.pm.ShortcutManager
import android.media.AudioManager
import android.os.Looper
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import com.modukey.app.NudgeApplication
import com.modukey.app.core.*
import kotlinx.coroutines.runBlocking
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
import org.robolectric.shadows.ShadowKeyguardManager
import org.robolectric.util.ReflectionHelpers
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36], shadows = [HomeShortcutTest.Keyguard::class])
@Suppress("DEPRECATION")
class HomeShortcutTest {
    private val app get() = ApplicationProvider.getApplicationContext<NudgeApplication>()
    private val main get() = shadowOf(Looper.getMainLooper())
    private val shortcut = HomeShortcut(name = "Louder", button = PanelButton(action = ButtonAction.VOLUME_UP))
    private fun await(condition: () -> Boolean) {
        repeat(300) { main.idleFor(Duration.ofMillis(16)); if (condition()) return; Thread.sleep(5) }
        assertTrue(condition())
    }
    @Before fun prepare() {
        Keyguard.callback = null
        shadowOf(app).grantPermissions("${app.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
        shadowOf(app.getSystemService(PowerManager::class.java)).setIsInteractive(true)
        shadowOf(app.getSystemService(KeyguardManager::class.java)).setKeyguardLocked(false)
        await { app.configuration.state.value != null }
        app.configuration.update { AppConfiguration(enabled = false, shortcuts = listOf(shortcut)) }
    }
    @After fun stop() { app.onTerminate() }
    @Test fun nativePinUsesOnlySavedIdAndUnsupportedLaunchersHaveNoFallback() = runBlocking {
        val manager = app.getSystemService(ShortcutManager::class.java)
        val launcher = PinnedShortcuts(app, app.icons)
        shadowOf(manager).setIsRequestPinShortcutSupported(false)
        assertFalse(launcher.supported()); assertFalse(launcher.pin(shortcut, "Louder")); assertTrue(manager.pinnedShortcuts.isEmpty())
        shadowOf(manager).setIsRequestPinShortcutSupported(true)
        assertTrue(launcher.pin(shortcut, "Louder"))
        val info = manager.pinnedShortcuts.single()
        assertEquals(shortcut.id, info.id); assertEquals("Louder", info.shortLabel)
        assertEquals(ShortcutActionActivity::class.java.name, info.intent!!.component!!.className)
        assertEquals(setOf(ShortcutActionActivity.EXTRA_ID), info.intent!!.extras!!.keySet())
        assertEquals(shortcut.id, info.intent!!.getStringExtra(ShortcutActionActivity.EXTRA_ID))
        assertEquals(setOf(shortcut.id), launcher.pinned())
        launcher.disable(shortcut.id)
        // ShadowShortcutManager keeps disabled entries without updating ShortcutInfo.isEnabled.
        assertEquals(shortcut.id, manager.pinnedShortcuts.single().id)
    }
    @Test fun savedShortcutRunsWithoutMasterSwitchAndNeverQueuesAfterDisconnect() {
        val controller = Robolectric.buildService(EdgeAccessibilityService::class.java).create()
        val service = controller.get(); val audio = app.getSystemService(AudioManager::class.java)
        try {
            assertFalse(app.shortcuts.run(shortcut.id))
            ReflectionHelpers.callInstanceMethod<Void>(service, "onServiceConnected")
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, 3, 0)
            assertTrue(app.shortcuts.run(shortcut.id)); assertEquals(4, audio.getStreamVolume(AudioManager.STREAM_MUSIC))
            assertFalse(app.configuration.state.value!!.enabled)
            app.configuration.update { it.copy(shortcuts = emptyList()) }
            assertFalse(app.shortcuts.run(shortcut.id)); assertEquals(4, audio.getStreamVolume(AudioManager.STREAM_MUSIC))
        } finally { controller.destroy() }
        assertFalse(app.shortcuts.run(shortcut.id))
    }
    @Test fun exportedEntryRejectsMissingIdsAndDispatchesAValidSavedActionOnce() {
        var calls = 0; app.shortcuts.bind(this) { calls++; it == shortcut.id }
        for (id in listOf("missing", shortcut.id)) {
            val intent = Intent(app, ShortcutActionActivity::class.java).setAction(Intent.ACTION_VIEW)
                .putExtra(ShortcutActionActivity.EXTRA_ID, id).putExtra("action", ButtonAction.LOCK.name)
            val controller = Robolectric.buildActivity(ShortcutActionActivity::class.java, intent).create().start().resume()
            await { controller.get().isFinishing }
            assertEquals(if (id == shortcut.id) 1 else 0, calls)
            controller.pause().stop().destroy()
        }
    }
    @Test fun changedShortcutAndCancelledUnlockNeverExecute() {
        var calls = 0; app.shortcuts.bind(this) { calls++; true }
        shadowOf(app.getSystemService(KeyguardManager::class.java)).setKeyguardLocked(true)
        val intent = Intent(app, ShortcutActionActivity::class.java).setAction(Intent.ACTION_VIEW).putExtra(ShortcutActionActivity.EXTRA_ID, shortcut.id)
        val first = Robolectric.buildActivity(ShortcutActionActivity::class.java, intent).create().start().resume()
        await { Keyguard.callback != null }
        app.configuration.update { it.copy(shortcuts = emptyList()) }
        shadowOf(app.getSystemService(KeyguardManager::class.java)).setKeyguardLocked(false)
        Keyguard.callback!!.onDismissSucceeded(); assertEquals(0, calls); assertTrue(first.get().isFinishing)
        first.pause().stop().destroy()
        app.configuration.update { it.copy(shortcuts = listOf(shortcut)) }; Keyguard.callback = null
        shadowOf(app.getSystemService(KeyguardManager::class.java)).setKeyguardLocked(true)
        val second = Robolectric.buildActivity(ShortcutActionActivity::class.java, intent).create().start().resume()
        await { Keyguard.callback != null }; Keyguard.callback!!.onDismissCancelled()
        assertEquals(0, calls); assertTrue(second.get().isFinishing); second.pause().stop().destroy()
    }
    @Test fun pendingInputAndBrightnessRequestsValidateTheirExactSource() {
        val input = HomeShortcut(button = PanelButton(action = ButtonAction.INPUT_METHOD_PICKER))
        val config = AppConfiguration(shortcuts = listOf(input))
        val pending = PendingLaunch("", input.button, false, false, shortcutId = input.id)
        assertEquals(input.button, pending.currentButton(config))
        assertNull(pending.currentButton(config.copy(shortcuts = emptyList())))
        val target = GestureTarget(brightness = true)
        val handle = HandleConfig(bindings = mapOf(GestureSlot.HOLD to target))
        val brightness = pending.copy(shortcutId = null, handleId = handle.id, gestureSlot = GestureSlot.HOLD, brightnessTarget = target, writeSettings = true)
        assertEquals(input.button, brightness.currentButton(AppConfiguration(handles = listOf(handle))))
        assertNull(brightness.currentButton(AppConfiguration(handles = listOf(handle.copy(bindings = emptyMap())))))
    }
    @Implements(KeyguardManager::class)
    class Keyguard : ShadowKeyguardManager() {
        companion object { var callback: KeyguardManager.KeyguardDismissCallback? = null }
        @Implementation override fun requestDismissKeyguard(activity: Activity, callback: KeyguardManager.KeyguardDismissCallback) { Keyguard.callback = callback }
    }
}
