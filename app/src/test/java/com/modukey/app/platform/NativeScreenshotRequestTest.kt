package com.modukey.app.platform

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
class NativeScreenshotRequestTest {
    @After fun cleanup() { ApplicationProvider.getApplicationContext<NudgeApplication>().onTerminate() }
    private fun idle() = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
    @Test fun `capture follows panel cleanup and repeated requests submit once`() {
        val events = mutableListOf<String>()
        val request = NativeScreenshotRequest(Choreographer.getInstance(), { true }, { events += "remove panels" },
            { events += "capture"; true }, { fail("Unexpected capture failure") })
        assertTrue(request.request()); assertTrue(request.request())
        assertEquals(listOf("remove panels"), events)
        idle(); assertEquals(listOf("remove panels", "capture"), events)
        idle(); assertEquals(2, events.size)
        request.cancel()
    }
    @Test fun `screen off and cancellation prevent a pending capture`() {
        var awake = true; var captures = 0
        val request = NativeScreenshotRequest(Choreographer.getInstance(), { awake }, {}, { captures++; true }, {})
        request.request(); awake = false; idle(); assertEquals(0, captures)
        assertFalse(request.request())
        awake = true; request.request(); request.cancel(); request.cancel(); idle(); assertEquals(0, captures)
        request.request(); idle(); assertEquals(1, captures)
    }
    @Test fun `a rejected or failed system request reports once and can be retried`() {
        var failures = 0; var mode = 0
        val request = NativeScreenshotRequest(Choreographer.getInstance(), { true }, {},
            { if (mode == 1) throw IllegalStateException("unavailable"); mode == 2 }, { failures++ })
        request.request(); idle(); assertEquals(1, failures)
        mode = 1; request.request(); idle(); assertEquals(2, failures)
        mode = 2; request.request(); idle(); assertEquals(2, failures)
    }
}
