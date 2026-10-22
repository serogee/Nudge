package com.modukey.app.platform

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.test.core.app.ApplicationProvider
import com.modukey.app.NudgeApplication
import com.modukey.app.R
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implements
import org.robolectric.annotation.Implementation
import org.robolectric.shadows.ShadowAccessibilityService
import org.robolectric.util.ReflectionHelpers
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36], shadows = [BrightnessControllerTest.Writes::class, BrightnessCalibrationServiceTest.ServiceInfo::class])
@Suppress("DEPRECATION")
class BrightnessCalibrationServiceTest {
    /** Model the system-held configuration; the default unbound shadow always returns null. */
    @Implements(AccessibilityService::class)
    class ServiceInfo : ShadowAccessibilityService() {
        private var reported = AccessibilityServiceInfo()
        @Implementation fun getServiceInfo(): AccessibilityServiceInfo = reported
        @Implementation fun setServiceInfo(info: AccessibilityServiceInfo) { reported = info }
    }
    private val app get() = ApplicationProvider.getApplicationContext<NudgeApplication>()
    private var destroy: () -> Unit = {}
    @After fun cleanup() {
        destroy(); BrightnessCalibrationRestore(app).discard(); app.onTerminate()
        BrightnessControllerTest.Writes.allowed = false
    }
    private fun idle(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
    private fun fixture(consent: Boolean = true): BrightnessCalibrationService {
        BrightnessControllerTest.Writes.allowed = true
        shadowOf(app).grantPermissions("${app.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
        shadowOf(app.getSystemService(PowerManager::class.java)).setIsInteractive(true)
        shadowOf(app.getSystemService(KeyguardManager::class.java)).setKeyguardLocked(false)
        Settings.System.putInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 73)
        Settings.System.putInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, 1)
        if (consent) assertTrue(app.automaticCalibration.prepare())
        val controller = Robolectric.buildService(BrightnessCalibrationService::class.java).create()
        destroy = { controller.destroy() }
        return controller.get().also { ReflectionHelpers.callInstanceMethod<Void>(it, "onServiceConnected") }
    }
    private fun provideBrightnessWindow(service: BrightnessCalibrationService, writes: MutableList<Int>) {
        val node = AccessibilityNodeInfo.obtain().apply {
            packageName = NativeBrightnessNode.SYSTEM_UI; viewIdResourceName = "com.android.systemui:id/brightness_slider"
            isEnabled = true; isVisibleToUser = true
            rangeInfo = AccessibilityNodeInfo.RangeInfo.obtain(0, 0f, 100f, 50f)
            addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS)
        }
        shadowOf(node).setOnPerformActionListener { code, args ->
            if (code != AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id) false else {
                val position = args.getFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE)
                writes += position.toInt()
                node.rangeInfo = AccessibilityNodeInfo.RangeInfo.obtain(0, 0f, 100f, position)
                Settings.System.putInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS, listOf(0, 16, 64, 144, 255)[position.toInt() / 25])
                true
            }
        }
        val window = AccessibilityWindowInfo.obtain()
        shadowOf(window).setType(AccessibilityWindowInfo.TYPE_SYSTEM); shadowOf(window).setRoot(node)
        shadowOf(service).setWindows(listOf(window))
    }
    @Test fun calibrationHasSeparateContentCapabilityWithoutGesturesOrScreenshots() {
        fun attribute(resource: Int, name: String): Boolean = app.resources.getXml(resource).use { xml ->
            while (xml.next() != org.xmlpull.v1.XmlPullParser.START_TAG) Unit
            xml.getAttributeBooleanValue("http://schemas.android.com/apk/res/android", name, false)
        }
        assertFalse(attribute(R.xml.accessibility_service, "canRetrieveWindowContent"))
        assertTrue(attribute(R.xml.brightness_calibration_service, "canRetrieveWindowContent"))
        for (name in listOf("canPerformGestures", "canTakeScreenshot", "canRequestFilterKeyEvents", "canRequestTouchExplorationMode"))
            assertFalse(attribute(R.xml.brightness_calibration_service, name))
        val metadata = app.packageManager.getServiceInfo(ComponentName(app, BrightnessCalibrationService::class.java), PackageManager.GET_META_DATA)
        assertEquals("android.permission.BIND_ACCESSIBILITY_SERVICE", metadata.permission)
    }
    @Test fun unsolicitedConnectionDisablesItselfWithoutOpeningOrReadingControls() {
        val service = fixture(consent = false)
        assertTrue(ReflectionHelpers.getField(service, "stopping"))
        assertEquals(0, service.serviceInfo.eventTypes); assertEquals(0, service.serviceInfo.flags)
        assertTrue(shadowOf(service).globalActionsPerformed.isEmpty())
        assertNull(service.control())
    }
    @Test fun realServiceMeasuresThroughProgressActionsAndCleansUpBeforeReview() {
        val service = fixture(); val writes = mutableListOf<Int>()
        provideBrightnessWindow(service, writes)
        assertEquals(AutomaticCalibrationPhase.READY, app.automaticCalibration.state.value.phase)
        assertTrue(shadowOf(service).globalActionsPerformed.isEmpty())
        app.automaticCalibration.start(); idle(3_000)
        assertEquals(AutomaticCalibrationPhase.REVIEW, app.automaticCalibration.state.value.phase)
        assertEquals(listOf(0, 25, 50, 75, 100), writes)
        assertEquals(73, Settings.System.getInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS))
        assertEquals(1, Settings.System.getInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE))
        assertNull(ReflectionHelpers.getField(service, "overlay")); assertEquals(0, service.serviceInfo.flags)
        assertEquals(listOf(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS, AccessibilityService.GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE), shadowOf(service).globalActionsPerformed)
        assertTrue(shadowOf(service).gesturesDispatched.isEmpty())
        assertEquals(0, service.serviceInfo.eventTypes)
        service.onInterrupt()
        assertEquals(AutomaticCalibrationPhase.REVIEW, app.automaticCalibration.state.value.phase)
        val count = writes.size; idle(30_000); assertEquals(count, writes.size)
    }
    @Test fun overlayCancelRestoresBrightnessAndDiscardsPendingWork() {
        val service = fixture(); val writes = mutableListOf<Int>(); provideBrightnessWindow(service, writes)
        app.automaticCalibration.start(); idle(100)
        val overlay = ReflectionHelpers.getField<android.widget.LinearLayout>(service, "overlay")
        assertTrue(overlay.getChildAt(1).performClick())
        idle(3_000)
        assertEquals(AutomaticCalibrationPhase.CANCELLED, app.automaticCalibration.state.value.phase)
        assertEquals(73, Settings.System.getInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS))
        assertNull(ReflectionHelpers.getField(service, "overlay")); assertEquals(1, writes.size)
    }
    @Test fun screenOffCancelsTheRunAndRemovesItsOverlay() {
        val service = fixture(); val writes = mutableListOf<Int>(); provideBrightnessWindow(service, writes)
        app.automaticCalibration.start(); idle(100)
        app.sendBroadcast(Intent(Intent.ACTION_SCREEN_OFF)); idle(200)
        assertEquals(AutomaticCalibrationError.LOCKED, app.automaticCalibration.state.value.error)
        assertEquals(73, Settings.System.getInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS))
        assertNull(ReflectionHelpers.getField(service, "overlay"))
        val count = writes.size; idle(5_000); assertEquals(count, writes.size)
    }
    @Test fun taskRemovalCancelsAndRestoresTheActiveRun() {
        val service = fixture(); val writes = mutableListOf<Int>(); provideBrightnessWindow(service, writes)
        app.automaticCalibration.start(); idle(100); service.onTaskRemoved(Intent()); idle(3_000)
        assertEquals(AutomaticCalibrationPhase.CANCELLED, app.automaticCalibration.state.value.phase)
        assertEquals(73, Settings.System.getInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS))
        assertNull(ReflectionHelpers.getField(service, "overlay")); assertEquals(1, writes.size)
    }
}
