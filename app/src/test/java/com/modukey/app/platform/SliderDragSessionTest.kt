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
    @Test fun guardedAdjustmentWaitsForConfiguredDwellAndUsesFreshValueWithoutAnInitialJump() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val view = View(activity.get()); activity.get().setContentView(view)
            val writes = mutableListOf<Float>(); var cues = 0; var current = .5f
            val session = SliderDragSession(view, .5f, 200f, 200f,
                { writes += it; SliderApplied(it, .01f) }, failed = { fail() }, startX = 30f, holdMs = 350,
                readOnArm = { current }, onArmed = { cues++ })
            session.start()
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(349))
            session.move(60f, 150f) // Finishing the opening diagonal swipe resets the dwell.
            current = .3f // A hardware or system change during the dwell becomes the new anchor.
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(349))
            assertEquals(0, cues); assertTrue(writes.isEmpty())
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1))
            assertEquals(1, cues); assertTrue(writes.isEmpty())
            session.move(60f, 142f)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(32)); assertTrue(writes.isEmpty())
            session.move(60f, 122f)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(32))
            assertEquals(.4f, writes.single(), .0001f)
            session.move(60f, 132f); session.finish(false)
            assertEquals(.35f, writes.last(), .0001f)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2)); assertEquals(1, cues)
        } finally { activity.destroy() }
    }
    @Test fun releaseCancellationAndReadFailureCannotLeaveALateGuardCallback() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup()
        try {
            var cues = 0; var failures = 0
            fun session(read: () -> Float? = { .5f }) = SliderDragSession(View(activity.get()), .5f, 200f, 200f,
                { fail("A waiting drag must not write"); null }, failed = { failures++ }, holdMs = 200,
                readOnArm = read, onArmed = { cues++ })
            val released = session(); released.start()
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(199))
            released.move(0f, 100f, allowActivation = false)
            assertTrue(released.finish(false))
            val cancelled = session(); cancelled.start(); assertFalse(cancelled.finish(true))
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
            assertEquals(0, cues); assertEquals(0, failures)
            val failed = session { null }; failed.start()
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))
            assertEquals(1, failures); assertEquals(0, cues); assertFalse(failed.finish(false))
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1)); assertEquals(1, failures)
        } finally { activity.destroy() }
    }
}
