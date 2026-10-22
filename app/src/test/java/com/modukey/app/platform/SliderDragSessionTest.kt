package com.modukey.app.platform

import android.app.Activity
import android.os.Looper
import android.view.View
import com.modukey.app.core.SliderApplied
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36])
class SliderDragSessionTest {
    @Test fun framesCoalesceWritesReleaseFlushesAndCancelDiscardsPendingMotion() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val view = View(activity.get()); activity.get().setContentView(view)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(32))
            val writes = mutableListOf<Float>()
            fun session() = SliderDragSession(view, .5f, 200f, 200f, { writes += it; SliderApplied(it, .01f) }, failed = { fail() })
            val first = session(); first.move(180f); first.move(150f); assertTrue(writes.isEmpty())
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(32)); assertEquals(listOf(.75f), writes)
            first.move(100f); assertTrue(first.finish(false)); assertEquals(listOf(.75f, 1f), writes)
            first.move(300f); shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(32)); assertEquals(2, writes.size)
            val second = session(); second.move(0f); assertFalse(second.finish(true))
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(32)); assertEquals(2, writes.size)
        } finally { activity.destroy() }
    }
    @Test fun heldKeepaliveStopsOnReleaseAndFailureCancelsOnce() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup()
        try {
            var heartbeats = 0; var failures = 0
            val session = SliderDragSession(View(activity.get()), .5f, 0f, 200f, { null }, { ++heartbeats < 2 }, { failures++ })
            session.start()
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3))
            assertEquals(2, heartbeats); assertEquals(1, failures); assertFalse(session.finish(false))
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3)); assertEquals(2, heartbeats)
            val released = SliderDragSession(View(activity.get()), .5f, 0f, 200f, { SliderApplied(it, .01f) }, { heartbeats++; true }, { fail() })
            released.start(); released.finish(false)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3)); assertEquals(2, heartbeats)
        } finally { activity.destroy() }
    }
}
