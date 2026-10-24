package com.modukey.app.platform

import android.content.ContentResolver
import android.app.Activity
import android.content.Context
import android.app.KeyguardManager
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.WindowManager
import android.view.MotionEvent
import androidx.test.core.app.ApplicationProvider
import com.modukey.app.NudgeApplication
import com.modukey.app.core.BrightnessPreset
import com.modukey.app.core.BrightnessReturnSettings
import com.modukey.app.core.ButtonAction
import com.modukey.app.core.PanelButton
import com.modukey.app.core.GestureSlot
import com.modukey.app.core.BrightnessPopupSettings
import com.modukey.app.core.Insets
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
            var onWrite: (() -> Unit)? = null
            @JvmStatic @Implementation fun canWrite(context: Context): Boolean = allowed
            @JvmStatic @Implementation(methodName = "putInt") fun writeInt(resolver: ContentResolver, name: String, value: Int): Boolean =
                name != failedSetting && ReflectionHelpers.callStaticMethod<Boolean>(ShadowSettings.ShadowSystem::class.java, "putInt",
                    ClassParameter.from(ContentResolver::class.java, resolver), ClassParameter.from(String::class.java, name),
                    ClassParameter.from(Int::class.javaPrimitiveType, value)).also { if (it) onWrite?.invoke() }
        }
    }
    private val app get() = ApplicationProvider.getApplicationContext<NudgeApplication>()
    private val resolver get() = app.contentResolver
    @After fun cleanup() { Writes.allowed = false; Writes.failedSetting = null; Writes.onWrite = null; app.onTerminate() }
    private fun set(level: Int, automatic: Boolean) {
        Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, level)
        Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, if (automatic) 1 else 0)
        shadowOf(Looper.getMainLooper()).idle()
    }
    private fun level() = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS)
    private fun mode() = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE)
    @Test fun manualMaximumUsesReturnOnTheFirstPressAndRemainsActiveForAFullReturn() {
        Writes.allowed = true
        for (usePrevious in listOf(true, false)) {
            set(255, false); app.brightness.close()
            assertTrue(app.brightness.status().maximum)
            assertEquals(BrightnessResult.CHANGED, app.brightness.toggle(BrightnessPreset.MAXIMUM, BrightnessReturnSettings(usePrevious, .5f)))
            assertEquals(22, level()); assertFalse(app.brightness.status().maximum)
        }
        set(255, false)
        assertEquals(BrightnessResult.ALREADY_SET, app.brightness.toggle(BrightnessPreset.MAXIMUM, BrightnessReturnSettings(true, 1f)))
        assertTrue(app.brightness.status().maximum)
        assertEquals(BrightnessResult.CHANGED, app.brightness.toggle(BrightnessPreset.MAXIMUM))
        assertEquals(22, level())
    }
    @Test fun synchronousPanelSnapshotsCannotClearPresetsDuringOwnWrites() {
        Writes.allowed = true; set(100, true)
        Writes.onWrite = { app.brightness.status() }
        app.brightness.toggle(BrightnessPreset.MAXIMUM)
        app.brightness.toggle(BrightnessPreset.MINIMUM)
        assertTrue(app.brightness.status().minimum)
        app.brightness.toggle(BrightnessPreset.MINIMUM)
        assertEquals(22, level()); assertEquals(0, mode())
        assertFalse(app.brightness.status().maximum); assertFalse(app.brightness.status().minimum)
        app.brightness.toggle(BrightnessPreset.MAXIMUM); app.brightness.toggle(BrightnessPreset.MAXIMUM)
        assertEquals(22, level())
    }
    @Test fun customReturnAndUnavailablePreviousWorkThroughTheActionExecutor() {
        Writes.allowed = true
        val service = Robolectric.buildService(EdgeAccessibilityService::class.java).create().get()
        val actions = PanelActions(service)
        for (action in listOf(ButtonAction.BRIGHTNESS_MAXIMUM, ButtonAction.BRIGHTNESS_MINIMUM)) {
            val button = PanelButton(action = action, brightnessReturn = BrightnessReturnSettings(false, .25f))
            set(100, true)
            assertTrue(actions.execute("owner", button, false).succeeded)
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(if (action == ButtonAction.BRIGHTNESS_MAXIMUM) actions.snapshot().maximumBrightness else actions.snapshot().minimumBrightness)
            assertTrue(actions.execute("owner", button, false).succeeded)
            assertEquals(6, level()); assertEquals(0, mode())
            assertFalse(actions.snapshot().maximumBrightness); assertFalse(actions.snapshot().minimumBrightness)
            set(255, true)
            val previous = button.copy(brightnessReturn = BrightnessReturnSettings(true, .75f))
            actions.execute("owner", previous, false); actions.execute("owner", previous, false)
            assertEquals(68, level()); assertEquals(0, mode())
            assertFalse(actions.snapshot().maximumBrightness); assertFalse(actions.snapshot().minimumBrightness)
        }
    }
    @Test fun sliderUsesPublicRangeAndPresetsHaveOwnedActiveStates() {
        Writes.allowed = true; set(100, false)
        app.brightness.setFraction(.25f); assertEquals(6, level())
        app.brightness.setFraction(.5f); assertEquals(22, level())
        app.brightness.setFraction(.75f); assertEquals(68, level())
        assertEquals(.75f, app.brightness.fraction(), .003f)
        assertFalse(app.brightness.status().maximum)
        app.brightness.toggle(BrightnessPreset.MAXIMUM)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(255, level()); assertTrue(app.brightness.status().maximum)
        app.brightness.toggle(BrightnessPreset.MAXIMUM)
        assertEquals(68, level()); assertFalse(app.brightness.status().maximum)
        app.brightness.toggle(BrightnessPreset.MINIMUM); shadowOf(Looper.getMainLooper()).idle()
        assertEquals(1, level()); assertTrue(app.brightness.status().minimum)
        set(70, false); assertFalse(app.brightness.status().minimum)
        app.brightness.toggle(BrightnessPreset.MINIMUM); app.brightness.toggle(BrightnessPreset.MINIMUM)
        assertEquals(70, level())
    }
    @Test fun manualSliderUseAtAnUnchangedEndpointClearsBothPresetStates() {
        Writes.allowed = true
        for (preset in BrightnessPreset.entries) {
            set(100, false)
            app.brightness.toggle(preset)
            assertEquals(preset == BrightnessPreset.MAXIMUM, app.brightness.status().maximum)
            val endpoint = if (preset == BrightnessPreset.MAXIMUM) 1f else 0f
            assertEquals(BrightnessResult.ALREADY_SET, app.brightness.setFraction(endpoint))
            assertEquals(preset == BrightnessPreset.MAXIMUM, app.brightness.status().maximum); assertFalse(app.brightness.status().minimum)
        }
    }
    @Test fun endpointTapsStayStillAndSwipeInputRefreshesTheOpenPanelState() {
        Writes.allowed = true; set(100, false)
        val activity = Robolectric.buildActivity(Activity::class.java).setup()
        val context = activity.get()
        val states = mutableListOf<BrightnessStatus>()
        val host = BrightnessPopupHost(context, context.getSystemService(WindowManager::class.java), app.brightness,
            {}, { fail("Slider failed: $it") }, changed = { states += app.brightness.status() })
        try {
            app.brightness.toggle(BrightnessPreset.MAXIMUM)
            assertTrue(host.show(BrightnessPopupSettings(showBackground = false, showAutomaticToggle = false), ScreenSpace(400f, 800f, 1f, Insets(), false), false))
            val view = ReflectionHelpers.getField<BrightnessPopupView>(host, "view")
            view.measure(android.view.View.MeasureSpec.makeMeasureSpec(64, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(240, android.view.View.MeasureSpec.EXACTLY))
            view.layout(0, 0, 64, 240)
            for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
                MotionEvent.obtain(0, 10, action, 30f, 0f, 0).let { view.onTouchEvent(it); it.recycle() }
            }
            assertEquals(255, level())
            assertTrue("A stationary tap must not submit a brightness change", states.isEmpty())
            for ((action, y) in listOf(MotionEvent.ACTION_DOWN to 30f, MotionEvent.ACTION_MOVE to 0f, MotionEvent.ACTION_UP to 0f)) {
                MotionEvent.obtain(0, 10, action, 30f, y, 0).let { view.onTouchEvent(it); it.recycle() }
            }
            assertEquals(255, level())
            assertTrue(states.last().maximum); assertFalse(states.last().minimum)
        } finally { host.hide(); activity.pause().stop().destroy() }
    }
    @Test fun externalEndpointNotificationsClearOwnershipWithoutIntermediateReads() {
        Writes.allowed = true
        for (preset in BrightnessPreset.entries) {
            set(100, false); app.brightness.toggle(preset)
            shadowOf(Looper.getMainLooper()).idle()
            val endpoint = level()
            // A quick external drag can return to its endpoint before the callback reads Settings.
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, 80)
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, endpoint)
            resolver.notifyChange(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS), null)
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(endpoint, level())
            assertEquals(preset == BrightnessPreset.MAXIMUM, app.brightness.status().maximum); assertFalse(app.brightness.status().minimum)
        }
    }
    @Test fun automaticToggleKeepsLevelAndHandlesDenialAndFailedWrites() {
        set(100, false)
        assertEquals(BrightnessResult.NEED_ACCESS, app.brightness.toggleAutomatic())
        assertEquals(100, level()); assertEquals(0, mode())
        Writes.allowed = true
        assertEquals(BrightnessResult.CHANGED, app.brightness.toggleAutomatic())
        assertTrue(app.brightness.status().automatic); assertEquals(100, level())
        assertEquals(BrightnessResult.CHANGED, app.brightness.toggleAutomatic())
        assertFalse(app.brightness.status().automatic); assertEquals(100, level())
        Writes.failedSetting = Settings.System.SCREEN_BRIGHTNESS_MODE
        assertEquals(BrightnessResult.FAILED, app.brightness.toggleAutomatic())
        assertEquals(0, mode()); assertEquals(100, level())
    }
    @Test fun automaticPanelActionSharesModeAndPermissionHandling() {
        val service = Robolectric.buildService(EdgeAccessibilityService::class.java).create().get()
        shadowOf(service.getSystemService(PowerManager::class.java)).setIsInteractive(true)
        shadowOf(service.getSystemService(KeyguardManager::class.java)).setKeyguardLocked(false)
        val actions = PanelActions(service)
        val button = PanelButton(action = ButtonAction.BRIGHTNESS_AUTOMATIC)
        set(100, false)
        assertTrue(actions.execute("owner", button, false).close)
        assertEquals(Settings.ACTION_MANAGE_WRITE_SETTINGS, shadowOf(service).nextStartedActivity.action)
        Writes.allowed = true
        val result = actions.execute("owner", button, false)
        assertTrue(result.succeeded); assertFalse(result.close); assertTrue(actions.snapshot().automaticBrightness)
        actions.execute("owner", button, false)
        assertFalse(actions.snapshot().automaticBrightness); assertEquals(100, level())
    }
    @Test fun `permission denial leaves brightness and mode unchanged`() {
        set(100, true)
        assertEquals(BrightnessResult.NEED_ACCESS, app.brightness.toggle(BrightnessPreset.MAXIMUM))
        assertEquals(100, level()); assertEquals(1, mode())
    }
    @Test fun `manual steps and slider writes respect limits and clear old restore values`() {
        Writes.allowed = true; set(100, true)
        assertEquals(BrightnessResult.CHANGED, app.brightness.adjust(true))
        assertTrue(level() > 100); assertEquals(0, mode())
        app.brightness.adjust(false); assertEquals(100.0, level().toDouble(), 1.0)
        app.brightness.toggle(BrightnessPreset.MAXIMUM)
        app.brightness.setFraction(.3f); val changed = level()
        app.brightness.toggle(BrightnessPreset.MAXIMUM); app.brightness.toggle(BrightnessPreset.MAXIMUM)
        assertEquals(changed, level())
        assertEquals(BrightnessResult.CHANGED, app.brightness.setFraction(-1f)); assertEquals(0f, app.brightness.fraction())
        assertEquals(BrightnessResult.ALREADY_SET, app.brightness.adjust(false))
        app.brightness.setFraction(2f); assertEquals(1f, app.brightness.fraction())
        assertEquals(BrightnessResult.ALREADY_SET, app.brightness.adjust(true))
        app.brightness.setFraction(0f)
        assertEquals(BrightnessResult.CHANGED, app.brightness.adjust(true)); assertTrue(level() > 1)
        assertEquals(BrightnessResult.FAILED, app.brightness.setFraction(Float.NaN))
        Writes.allowed = false
        assertEquals(BrightnessResult.NEED_ACCESS, app.brightness.adjust(true))
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
        assertTrue(app.brightness.status().maximum)
        assertEquals(BrightnessResult.CHANGED, app.brightness.toggle(BrightnessPreset.MAXIMUM))
        assertEquals(22, level())
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
