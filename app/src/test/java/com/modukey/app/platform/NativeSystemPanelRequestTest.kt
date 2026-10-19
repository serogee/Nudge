package com.modukey.app.platform

import android.accessibilityservice.AccessibilityService
import android.os.Looper
import android.view.Choreographer
import androidx.test.core.app.ApplicationProvider
import com.modukey.app.NudgeApplication
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36])
class NativeSystemPanelRequestTest {
    private val notifications = AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
    private val controls = AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS
    @After fun cleanup() { ApplicationProvider.getApplicationContext<NudgeApplication>().onTerminate() }
    private fun idle() = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
    @Test fun `system shades wait for cleanup and only the latest distinct request runs`() {
        val events = mutableListOf<Int>()
        val request = NativeSystemPanelRequest(Choreographer.getInstance(), { true }, { events += 0 }, { events += it; true }, { fail() })
        assertTrue(request.request(notifications)); assertTrue(request.request(notifications))
        assertEquals(listOf(0), events)
        assertTrue(request.request(controls)); assertEquals(listOf(0, 0), events)
        idle(); assertEquals(listOf(0, 0, controls), events)
        idle(); assertEquals(3, events.size)
        assertFalse(request.request(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN))
    }
    @Test fun `screen off and cleanup cancel requests and failures report once`() {
        var awake = true; var calls = 0; var failures = 0
        val request = NativeSystemPanelRequest(Choreographer.getInstance(), { awake }, {}, { calls++; false }, { failures++ })
        request.request(notifications); awake = false; idle(); assertEquals(0, calls)
        assertFalse(request.request(controls))
        awake = true; request.request(controls); request.cancel(); request.cancel(); idle(); assertEquals(0, calls)
        request.request(notifications); idle(); assertEquals(1, calls); assertEquals(1, failures)
        idle(); assertEquals(1, failures)
    }
}
