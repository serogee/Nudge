package com.modukey.app.platform

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.view.accessibility.AccessibilityEvent
import androidx.test.core.app.ApplicationProvider
import com.modukey.app.MainActivity
import com.modukey.app.NudgeApplication
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36])
@Suppress("DEPRECATION")
class ForegroundTrackerTest {
    private val app get() = ApplicationProvider.getApplicationContext<NudgeApplication>()
    @After fun cleanup() { app.onTerminate() }

    private fun activity(pkg: String, name: String = "$pkg.Main", launchable: Boolean = true, home: Boolean = false) {
        val info = ActivityInfo().apply {
            packageName = pkg; this.name = name; enabled = true
            applicationInfo = ApplicationInfo().apply { packageName = pkg; enabled = true }
        }
        shadowOf(app.packageManager).addOrUpdateActivity(info)
        if (launchable || home) shadowOf(app.packageManager).addResolveInfoForIntent(
            Intent(Intent.ACTION_MAIN).addCategory(if (home) Intent.CATEGORY_HOME else Intent.CATEGORY_LAUNCHER).apply { if (!home) setPackage(pkg) },
            ResolveInfo().apply { activityInfo = info })
    }
    private fun event(pkg: String, name: String = "$pkg.Main", time: Long = 200, changes: Int = 0) =
        AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED).apply {
            packageName = pkg; className = name; eventTime = time; contentChangeTypes = changes
        }
    private fun observe(tracker: ForegroundTracker, pkg: String, name: String = "$pkg.Main", time: Long = 200,
        changes: Int = 0, openedAt: Long? = null, now: Long = 1_000): ForegroundObservation? {
        val event = event(pkg, name, time, changes)
        return try { tracker.observe(event, now, openedAt) } finally { event.recycle() }
    }
    private fun tracker() = ForegroundTracker(app.packageManager, app.packageName).apply { track(true, 100) }

    @Test fun `first credible activity initializes baseline without a switch`() {
        activity("test.browser"); val tracker = tracker()
        assertFalse(observe(tracker, "test.browser", openedAt = 150)!!.switched)
        assertEquals("test.browser", tracker.packageName)
        assertFalse(observe(tracker, "test.browser", time = 201)!!.switched)
        activity("test.browser", "test.browser.Other")
        assertFalse(observe(tracker, "test.browser", "test.browser.Other", time = 202)!!.switched)
    }
    @Test fun `different confirmed activity switches apps including launcher and Nudge`() {
        activity("test.browser"); activity("test.launcher", launchable = false, home = true)
        val tracker = tracker(); observe(tracker, "test.browser")
        assertEquals(ForegroundObservation("test.browser", "test.launcher"), observe(tracker, "test.launcher", time = 201))
        assertTrue(observe(tracker, app.packageName, MainActivity::class.java.name, time = 202)!!.switched)
    }
    @Test fun `panes views dialogs vendor components and service windows are not app switches`() {
        activity("test.browser"); activity("test.camera"); activity("test.volume", launchable = false)
        val tracker = tracker(); observe(tracker, "test.browser")
        for ((pkg, name) in listOf("test.camera" to "android.app.Dialog", "test.camera" to "android.widget.FrameLayout",
            "test.volume" to "test.volume.Main", "test.unknown" to "test.unknown.Main", "android" to "android.Main",
            "com.android.systemui" to "com.android.systemui.volume.VolumeDialog", app.packageName to PanelRoot::class.java.name)) {
            assertNull("$pkg/$name", observe(tracker, pkg, name, time = 201))
        }
        assertNull(observe(tracker, "test.camera", changes = AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_APPEARED, time = 202))
        assertEquals("test.browser", tracker.packageName)
        assertTrue(observe(tracker, "test.camera", time = 203)!!.switched)
    }
    @Test fun `stale events cannot rewind baseline or dismiss a newly opened panel`() {
        activity("test.browser"); activity("test.camera"); val tracker = tracker()
        observe(tracker, "test.browser", time = 200)
        for (time in listOf(0L, 99L, 199L, 201L, 1_001L)) {
            assertNull(observe(tracker, "test.camera", time = time, openedAt = 300))
            assertEquals("test.browser", tracker.packageName)
        }
        assertTrue(observe(tracker, "test.camera", time = 300, openedAt = 300)!!.switched)
    }
    @Test fun `tracking restarts with an empty baseline and no old metadata`() {
        activity("test.browser"); val tracker = tracker(); observe(tracker, "test.browser")
        tracker.track(false, 250)
        assertNull(tracker.packageName); assertNull(observe(tracker, "test.browser", time = 260))
        tracker.track(true, 300)
        assertNull(observe(tracker, "test.browser", time = 299))
        assertFalse(observe(tracker, "test.browser", time = 301)!!.switched)
    }
    @Test fun `unknown metadata cache stays bounded`() {
        val tracker = tracker()
        repeat(100) { assertNull(observe(tracker, "unknown.$it")) }
        assertTrue(ReflectionHelpers.getField<Map<*, *>>(tracker, "activities").size <= 64)
        assertTrue(ReflectionHelpers.getField<Map<*, *>>(tracker, "applications").size <= 32)
    }
}
