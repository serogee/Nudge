package com.modukey.app.platform

import android.content.ContentResolver
import android.content.Context
import android.app.KeyguardManager
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.modukey.app.NudgeApplication
import com.modukey.app.core.BrightnessPreset
import com.modukey.app.core.ButtonAction
import com.modukey.app.core.PanelButton
import com.modukey.app.core.GestureSlot
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowSettings
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36], shadows = [BrightnessControllerTest.Writes::class])
class BrightnessControllerTest {
    @Implements(Settings.System::class)
    class Writes : ShadowSettings.ShadowSystem() {
        companion object {
            var allowed = false
            var failedSetting: String? = null
            @JvmStatic @Implementation fun canWrite(context: Context): Boolean = allowed
            @JvmStatic @Implementation(methodName = "putInt") fun writeInt(resolver: ContentResolver, name: String, value: Int): Boolean =
                name != failedSetting && ReflectionHelpers.callStaticMethod(ShadowSettings.ShadowSystem::class.java, "putInt",
                    ClassParameter.from(ContentResolver::class.java, resolver), ClassParameter.from(String::class.java, name),
                    ClassParameter.from(Int::class.javaPrimitiveType, value))
        }
    }
    private val app get() = ApplicationProvider.getApplicationContext<NudgeApplication>()
    private val resolver get() = app.contentResolver
    @After fun cleanup() { Writes.allowed = false; Writes.failedSetting = null; app.onTerminate() }
    private fun set(level: Int, automatic: Boolean) {
        Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, level)
        Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, if (automatic) 1 else 0)
        shadowOf(Looper.getMainLooper()).idle()
    }
    private fun level() = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS)
    private fun mode() = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE)
    @Test fun `permission denial leaves brightness and mode unchanged`() {
        set(100, true)
        assertEquals(BrightnessResult.NEED_ACCESS, app.brightness.toggle(BrightnessPreset.MAXIMUM))
        assertEquals(100, level()); assertEquals(1, mode())
    }
    @Test fun `brightness actions request system access and retain their source behind keyguard`() {
        val service = Robolectric.buildService(EdgeAccessibilityService::class.java).create().get()
        shadowOf(service.getSystemService(PowerManager::class.java)).setIsInteractive(true)
        val keyguard = shadowOf(service.getSystemService(KeyguardManager::class.java))
        val button = PanelButton(action = ButtonAction.BRIGHTNESS_MAXIMUM)
        val actions = PanelActions(service)
        keyguard.setKeyguardLocked(false)
        assertTrue(actions.execute("owner", button, false, GestureSlot.HOLD).close)
        val intent = shadowOf(service).nextStartedActivity
        assertEquals(Settings.ACTION_MANAGE_WRITE_SETTINGS, intent.action)
        assertEquals("package:${app.packageName}", intent.data.toString())
        keyguard.setKeyguardLocked(true)
        assertTrue(actions.execute("owner", button, false, GestureSlot.HOLD).close)
        val bridge = shadowOf(service).nextStartedActivity
        assertEquals(UnlockActionActivity::class.java.name, bridge.component!!.className)
        val pending = requireNotNull(app.unlocks.take(requireNotNull(bridge.getStringExtra("token"))))
        assertTrue(pending.writeSettings); assertEquals(GestureSlot.HOLD, pending.gestureSlot)
        assertEquals(button, pending.button)
    }
    @Test fun `both presets restore automatic brightness after asynchronous setting notifications`() {
        Writes.allowed = true
        for (preset in BrightnessPreset.entries) {
            set(100, true)
            assertEquals(BrightnessResult.CHANGED, app.brightness.toggle(preset))
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(0, mode())
            assertEquals(preset == BrightnessPreset.MAXIMUM, app.brightness.status().maximum)
            assertEquals(preset == BrightnessPreset.MINIMUM, app.brightness.status().minimum)
            assertEquals(BrightnessResult.CHANGED, app.brightness.toggle(preset))
            assertEquals(100, level()); assertEquals(1, mode())
        }
    }
    @Test fun `manual brightness changes discard restore even after returning to the limit`() {
        Writes.allowed = true; set(100, false)
        app.brightness.toggle(BrightnessPreset.MAXIMUM); shadowOf(Looper.getMainLooper()).idle()
        val maximum = level()
        set(80, false); set(maximum, false)
        assertEquals(BrightnessResult.ALREADY_SET, app.brightness.toggle(BrightnessPreset.MAXIMUM))
        assertEquals(maximum, level())
        set(75, false)
        app.brightness.toggle(BrightnessPreset.MINIMUM); app.brightness.toggle(BrightnessPreset.MINIMUM)
        assertEquals(75, level()); assertEquals(0, mode())
    }
    @Test fun `failed partial writes restore mode and cannot leave a stale restore session`() {
        Writes.allowed = true; set(100, true)
        Writes.failedSetting = Settings.System.SCREEN_BRIGHTNESS
        assertEquals(BrightnessResult.FAILED, app.brightness.toggle(BrightnessPreset.MAXIMUM))
        assertEquals(100, level()); assertEquals(1, mode())
        Writes.failedSetting = null
        app.brightness.toggle(BrightnessPreset.MINIMUM); app.brightness.toggle(BrightnessPreset.MINIMUM)
        assertEquals(100, level())
    }
}
