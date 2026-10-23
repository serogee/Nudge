package com.modukey.app.platform

import android.os.Looper
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration
import kotlin.math.roundToInt
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36])
class AutomaticBrightnessCalibrationTest {
    private class Brightness : CalibrationBrightnessAccess {
        override var canWrite = true
        override var pendingRestore = false
        var level = 87; var automatic = true
        var original = 87 to true
        var restores = 0; var saved: List<Int>? = listOf(0, 10, 50, 130, 255)
        var restoreAllowed = true; var saveAllowed = true; var unstable = false; var reads = 0
        override fun begin(): Boolean { original = level to automatic; pendingRestore = true; automatic = false; return true }
        override fun sample() = if (automatic) null else if (unstable) level + ++reads % 2 else level
        override fun restore(): Boolean {
            restores++
            if (!restoreAllowed) return false
            level = original.first; automatic = original.second; pendingRestore = false; return true
        }
        override fun discardRestore(): Boolean { pendingRestore = false; return true }
        override fun save(samples: List<Int>): Boolean { if (!saveAllowed) return false; saved = samples; return true }
    }
    private class Port(private val brightness: Brightness) : CalibrationServicePort {
        var fraction = .5f; var reads = 0; var opens = 0; var closes = 0; var disables = 0
        var exposed = true; var accepted = true; var key = "brightness"; var duplicateSamples = false
        var quantized = false; var writeDelay = 0L
        var touchRequired = false
        override var canCompleteTouch = false
        override var inputInProgress = false
        private var temporary = 0
        val moves = mutableListOf<Float>()
        override fun open(): Boolean { opens++; return true }
        override fun control(): NativeBrightnessControl? {
            reads++; return if (exposed) NativeBrightnessControl(key, 0f, 1600f, fraction * 1600, true, requiresTouch = touchRequired) else null
        }
        override fun move(expected: NativeBrightnessControl, fraction: Float): Boolean {
            if (!accepted) return false
            moves += fraction; this.fraction = fraction
            val point = (fraction * 16).roundToInt()
            val level = if (duplicateSamples) 25 else if (quantized) point / 2 else (point * point).coerceAtMost(255)
            if (touchRequired) temporary = level
            else if (writeDelay == 0L) brightness.level = level
            else android.os.Handler(Looper.getMainLooper()).postDelayed({ brightness.level = level }, writeDelay)
            return true
        }
        override suspend fun apply(expected: NativeBrightnessControl, fraction: Float): Boolean {
            if (touchRequired && !canCompleteTouch) return false
            if (!move(expected, fraction)) return false
            if (touchRequired) {
                inputInProgress = true
                try { withContext(NonCancellable) { delay(50); brightness.level = temporary } }
                finally { inputInProgress = false }
            }
            return true
        }
        override fun progress(percent: Int) = Unit
        override fun close() { closes++ }
        override fun disable() { disables++; close() }
    }
    private var coordinator: AutomaticBrightnessCalibration? = null
    @Test fun temporaryOnlySlidersNeedConsentBeforeAnyBrightnessChange() {
        val (next, brightness, port) = run()
        port.touchRequired = true
        next.start(); idle(100)
        assertEquals(AutomaticCalibrationError.TOUCH_REQUIRED, next.state.value.error)
        assertEquals(87, brightness.level); assertTrue(brightness.automatic)
        assertEquals(0, brightness.restores); assertTrue(port.moves.isEmpty())
    }
    @Test fun touchCompletionCommitsSeventeenNativePositionsBeforeReview() {
        val (next, brightness, port) = run()
        port.touchRequired = true; port.canCompleteTouch = true
        next.start(); idle(14_000)
        assertEquals(AutomaticCalibrationPhase.REVIEW, next.state.value.phase)
        assertEquals((0..16).map { (it * it).coerceAtMost(255) }, next.state.value.samples)
        assertEquals(87, brightness.level); assertTrue(brightness.automatic)
        assertEquals(1, brightness.restores); assertFalse(port.inputInProgress)
    }
    @Test fun cancellationWaitsForNativeReleaseBeforeRestoringBrightness() {
        val (next, brightness, port) = run()
        port.touchRequired = true; port.canCompleteTouch = true
        next.start(); idle(10)
        assertTrue(port.inputInProgress)
        next.cancel()
        assertEquals(0, brightness.restores)
        idle(1_000)
        assertEquals(AutomaticCalibrationPhase.CANCELLED, next.state.value.phase)
        assertEquals(87, brightness.level); assertTrue(brightness.automatic)
        assertEquals(1, brightness.restores); assertFalse(port.inputInProgress)
    }
    @Test fun disconnectDuringNativeTouchStillReleasesBeforeRestoration() {
        val (next, brightness, port) = run()
        port.touchRequired = true; port.canCompleteTouch = true
        next.start(); idle(10); next.disconnected(port)
        assertEquals(0, brightness.restores)
        idle(1_000)
        assertEquals(AutomaticCalibrationError.DISCONNECTED, next.state.value.error)
        assertEquals(87, brightness.level); assertEquals(1, brightness.restores)
    }
    @After fun cleanup() { coordinator?.close() }
    private fun idle(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
    private fun run(brightness: Brightness = Brightness(), awake: () -> Boolean = { true },
        keepEnabled: () -> Boolean = { false }): Triple<AutomaticBrightnessCalibration, Brightness, Port> {
        val next = AutomaticBrightnessCalibration(brightness, awake, keepServiceEnabled = keepEnabled); coordinator = next
        val port = Port(brightness)
        assertTrue(next.prepare()); assertTrue(next.connect(port))
        return Triple(next, brightness, port)
    }
    @Test fun enablingAloneDoesNotReadWindowsAndCompleteResultsNeedSaveAfterRestore() {
        val (next, brightness, port) = run()
        val previous = brightness.saved
        idle(1_000); assertEquals(0, port.reads); assertEquals(0, port.opens)
        next.start(); idle(14_000)
        assertEquals(AutomaticCalibrationPhase.REVIEW, next.state.value.phase)
        assertEquals((0..16).map { it / 16f }, port.moves)
        assertEquals(87, brightness.level); assertTrue(brightness.automatic); assertEquals(1, brightness.restores)
        assertEquals(previous, brightness.saved); assertEquals(1, port.closes)
        next.save(); assertEquals((0..16).map { (it * it).coerceAtMost(255) }, brightness.saved)
        assertEquals(AutomaticCalibrationPhase.SAVED, next.state.value.phase)
        val reads = port.reads; idle(30_000); assertEquals(reads, port.reads)
    }
    @Test fun enabledStandbyHasNoReadsOrDeadlineAndRepeatedRunsDoNotNeedReconnection() {
        val brightness = Brightness(); val port = Port(brightness)
        val next = AutomaticBrightnessCalibration(brightness, awake = { true }, keepServiceEnabled = { true }); coordinator = next
        assertTrue(next.connect(port)); assertTrue(next.connected.value)
        idle(240_000)
        assertEquals(AutomaticCalibrationPhase.IDLE, next.state.value.phase)
        assertEquals(0, port.reads); assertEquals(0, port.opens)
        repeat(2) {
            assertTrue(next.prepare()); assertEquals(AutomaticCalibrationPhase.READY, next.state.value.phase)
            next.start(); idle(14_000)
            assertEquals(AutomaticCalibrationPhase.REVIEW, next.state.value.phase)
            assertTrue(next.connected.value)
            val reads = port.reads; idle(120_000); assertEquals(reads, port.reads)
            next.save()
        }
        assertEquals(2, port.opens); assertEquals(2, brightness.restores)
        next.close(); assertFalse(next.connected.value); assertEquals(1, port.disables)
    }
    @Test fun manualDisableAndLateDisconnectKeepCompletedMeasurementsAvailable() {
        val (next, brightness, port) = run(keepEnabled = { true })
        next.start(); idle(14_000)
        val samples = next.state.value.samples
        next.interrupted(port); assertEquals(AutomaticCalibrationPhase.REVIEW, next.state.value.phase)
        next.disableService(); next.disconnected(port)
        assertFalse(next.connected.value); assertEquals(1, port.disables)
        assertEquals(samples, next.state.value.samples)
        next.save(); assertEquals(samples, brightness.saved)
    }
    @Test fun cancellationAndSetupTimeoutRetainStandbyButARealDisconnectDetachesIt() {
        val (next, brightness, port) = run(keepEnabled = { true })
        idle(120_001)
        assertEquals(AutomaticCalibrationError.SETUP_TIMEOUT, next.state.value.error); assertTrue(next.connected.value)
        assertTrue(next.prepare()); next.start(); idle(100); next.cancel()
        assertTrue(next.connected.value); assertEquals(87, brightness.level)
        assertTrue(next.prepare()); next.start(); idle(100); next.disconnected(port)
        assertFalse(next.connected.value); assertEquals(AutomaticCalibrationError.DISCONNECTED, next.state.value.error)
        assertEquals(87, brightness.level); assertTrue(brightness.automatic)
        val reads = port.reads; idle(5_000); assertEquals(reads, port.reads)
        val replacement = Port(brightness); assertTrue(next.connect(replacement))
        next.disconnected(port); assertTrue(next.connected.value)
        assertEquals(0, replacement.opens)
    }
    @Test fun aReconnectedServiceCannotResumeAnInterruptedMeasurementBeforeRecovery() {
        val brightness = Brightness().apply { pendingRestore = true; level = 12; automatic = false }
        val port = Port(brightness)
        val next = AutomaticBrightnessCalibration(brightness, awake = { true }, keepServiceEnabled = { true }); coordinator = next
        assertTrue(next.connect(port)); next.start(); idle(5_000)
        assertEquals(AutomaticCalibrationPhase.RECOVERY, next.state.value.phase)
        assertFalse(next.prepare()); assertEquals(0, port.reads); assertEquals(0, port.opens)
        next.recover(true); assertEquals(87, brightness.level); assertTrue(brightness.automatic)
        assertTrue(next.prepare()); assertEquals(AutomaticCalibrationPhase.READY, next.state.value.phase)
    }
    @Test fun cancellationAndServiceLossRestoreOnceAndKeepThePreviousProfile() {
        val (next, brightness, port) = run()
        val previous = brightness.saved
        next.start(); idle(450); next.cancel(); idle(5_000)
        assertEquals(AutomaticCalibrationPhase.CANCELLED, next.state.value.phase)
        assertEquals(87, brightness.level); assertTrue(brightness.automatic)
        assertEquals(previous, brightness.saved); assertEquals(1, brightness.restores); assertEquals(1, port.closes)
        assertTrue(next.prepare()); val replacement = Port(brightness); assertTrue(next.connect(replacement))
        next.start(); idle(150); next.disconnected(replacement); idle(5_000)
        assertEquals(AutomaticCalibrationError.DISCONNECTED, next.state.value.error)
        assertEquals(2, brightness.restores); assertEquals(1, replacement.closes)
        next.disconnected(port); assertEquals(AutomaticCalibrationError.DISCONNECTED, next.state.value.error)
    }
    @Test fun refusedActionsUnstableReadingsAndFlatSamplesNeverReplaceTheProfile() {
        val (next, brightness, port) = run()
        val previous = brightness.saved
        port.accepted = false; next.start(); idle(100)
        assertEquals(AutomaticCalibrationError.UNSUPPORTED, next.state.value.error)
        assertEquals(previous, brightness.saved)
        assertTrue(next.prepare()); val duplicate = Port(brightness).apply { duplicateSamples = true }; assertTrue(next.connect(duplicate))
        next.start(); idle(14_000)
        assertEquals(AutomaticCalibrationError.INVALID_SAMPLES, next.state.value.error)
        assertEquals(previous, brightness.saved)
        assertTrue(next.prepare()); val unstable = Port(brightness); assertTrue(next.connect(unstable))
        brightness.unstable = true; next.start(); idle(4_000)
        assertEquals(AutomaticCalibrationError.UNSTABLE, next.state.value.error)
        assertEquals(previous, brightness.saved); assertEquals(87, brightness.level)
    }
    @Test fun denseHardwarePlateausAreAcceptedButOldManualProfilesAreNotChangedUntilSave() {
        val (next, brightness, port) = run()
        val previous = brightness.saved
        port.quantized = true; next.start(); idle(14_000)
        assertEquals(AutomaticCalibrationPhase.REVIEW, next.state.value.phase)
        assertEquals((0..16).map { it / 2 }, next.state.value.samples)
        assertEquals(previous, brightness.saved)
        next.save(); assertEquals(17, brightness.saved!!.size)
    }
    @Test fun nativeSettingWritesMustSettleBeforeTheDenseSamplesAreAccepted() {
        val (next, brightness, port) = run()
        port.writeDelay = 450; next.start(); idle(22_000)
        assertEquals(AutomaticCalibrationPhase.REVIEW, next.state.value.phase)
        assertEquals((0..16).map { (it * it).coerceAtMost(255) }, next.state.value.samples)
        assertEquals(87, brightness.level); assertTrue(brightness.automatic)
    }
    @Test fun setupTimesOutAndLockOrChangedControlStopsActiveMeasurement() {
        var awake = true
        val (next, brightness, port) = run(awake = { awake })
        idle(120_001); assertEquals(AutomaticCalibrationError.SETUP_TIMEOUT, next.state.value.error)
        assertEquals(0, port.reads); assertEquals(1, port.closes); assertEquals(0, brightness.restores)
        assertTrue(next.prepare()); val active = Port(brightness); assertTrue(next.connect(active))
        next.start(); idle(100); awake = false; idle(200)
        assertEquals(AutomaticCalibrationError.LOCKED, next.state.value.error); assertEquals(87, brightness.level)
        awake = true; assertTrue(next.prepare()); val changed = Port(brightness); assertTrue(next.connect(changed))
        next.start(); idle(100); changed.key = "different"; idle(200)
        assertEquals(AutomaticCalibrationError.CONTROL_CHANGED, next.state.value.error)
    }
    @Test fun failedRestorationRequiresRecoveryAndRestartDoesNotResumeARequest() {
        val brightness = Brightness().apply { restoreAllowed = false }
        val (next, _, _) = run(brightness)
        next.start(); idle(14_000)
        assertEquals(AutomaticCalibrationPhase.RECOVERY, next.state.value.phase)
        assertFalse(next.prepare()); next.save(); assertTrue(brightness.pendingRestore)
        next.close()
        val restarted = AutomaticBrightnessCalibration(brightness, awake = { true }); coordinator = restarted
        assertEquals(AutomaticCalibrationPhase.RECOVERY, restarted.state.value.phase)
        assertFalse(restarted.connect(Port(brightness)))
        brightness.restoreAllowed = true; restarted.recover(true)
        assertEquals(87, brightness.level); assertTrue(brightness.automatic)
        assertEquals(AutomaticCalibrationPhase.IDLE, restarted.state.value.phase)
    }
    @Test fun discardAndSaveFailureRetainTheOldCalibration() {
        val (next, brightness, _) = run()
        val previous = brightness.saved
        next.start(); idle(14_000); brightness.saveAllowed = false; next.save()
        assertEquals(AutomaticCalibrationPhase.REVIEW, next.state.value.phase)
        assertEquals(AutomaticCalibrationError.SAVE, next.state.value.error); assertEquals(previous, brightness.saved)
        next.discard(); assertEquals(AutomaticCalibrationPhase.IDLE, next.state.value.phase)
        assertEquals(previous, brightness.saved)
    }
    @Test fun missingControlTimesOutAndPermissionDenialNeverStartsMeasurement() {
        val brightness = Brightness().apply { canWrite = false }
        val next = AutomaticBrightnessCalibration(brightness, awake = { true }); coordinator = next
        assertFalse(next.prepare()); assertEquals(AutomaticCalibrationError.ACCESS, next.state.value.error)
        assertFalse(next.connect(Port(brightness)))
        brightness.canWrite = true; assertTrue(next.prepare())
        val unsupported = Port(brightness).apply { exposed = false }; assertTrue(next.connect(unsupported))
        next.start(); idle(3_500)
        assertEquals(AutomaticCalibrationPhase.RUNNING, next.state.value.phase)
        assertTrue(next.state.value.waitingForPanel)
        assertTrue(brightness.automatic); assertFalse(brightness.pendingRestore)
        assertTrue(unsupported.moves.isEmpty())
        idle(27_000)
        assertEquals(AutomaticCalibrationError.UNSUPPORTED, next.state.value.error)
        assertEquals(87, brightness.level); assertEquals(1, unsupported.closes)
        assertEquals(0, brightness.restores)
        val reads = unsupported.reads; idle(30_000); assertEquals(reads, unsupported.reads)
    }
    @Test fun delayedPanelDiscoveryLeavesBrightnessUntouchedUntilTheControlAppears() {
        val (next, brightness, port) = run()
        port.exposed = false
        next.start(); idle(5_000)
        assertTrue(next.state.value.waitingForPanel); assertTrue(brightness.automatic)
        assertFalse(brightness.pendingRestore); assertEquals(0, brightness.restores)
        assertTrue(port.moves.isEmpty())
        brightness.level = 101
        port.exposed = true; idle(14_000)
        assertEquals(AutomaticCalibrationPhase.REVIEW, next.state.value.phase)
        assertEquals((0..16).map { (it * it).coerceAtMost(255) }, next.state.value.samples)
        assertEquals(101, brightness.level); assertTrue(brightness.automatic)
        assertEquals(1, brightness.restores)
    }
    @Test fun cancelOrLockWhileWaitingDoesNotWriteBrightnessOrLeaveCallbacks() {
        var awake = true
        val (next, brightness, port) = run(awake = { awake })
        port.exposed = false; next.start(); idle(4_000); next.cancel(); idle(1_000)
        assertEquals(AutomaticCalibrationPhase.CANCELLED, next.state.value.phase)
        assertEquals(0, brightness.restores); assertFalse(brightness.pendingRestore)
        assertEquals(87, brightness.level); assertTrue(brightness.automatic)
        val reads = port.reads; idle(30_000); assertEquals(reads, port.reads)
        assertTrue(next.prepare()); val replacement = Port(brightness).apply { exposed = false }; assertTrue(next.connect(replacement))
        next.start(); idle(100); awake = false; idle(200)
        assertEquals(AutomaticCalibrationError.LOCKED, next.state.value.error)
        assertEquals(0, brightness.restores); assertTrue(brightness.automatic)
    }
}
