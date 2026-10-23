package com.modukey.app.platform

import android.os.SystemClock
import com.modukey.app.core.BrightnessScale
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.roundToInt

enum class AutomaticCalibrationPhase { IDLE, WAITING_SERVICE, READY, RUNNING, REVIEW, SAVED, CANCELLED, FAILED, RECOVERY }
enum class AutomaticCalibrationError { ACCESS, LOCKED, SETUP_TIMEOUT, UNSUPPORTED, CONTROL_CHANGED, UNSTABLE, INVALID_SAMPLES, RESTORE, SAVE, DISCONNECTED, SETTINGS, TOUCH_REQUIRED }
data class AutomaticCalibrationState(val phase: AutomaticCalibrationPhase = AutomaticCalibrationPhase.IDLE,
    val position: Int = 0, val samples: List<Int> = emptyList(), val error: AutomaticCalibrationError? = null,
    val waitingForPanel: Boolean = false) {
    val busy get() = phase in listOf(AutomaticCalibrationPhase.WAITING_SERVICE, AutomaticCalibrationPhase.READY, AutomaticCalibrationPhase.RUNNING)
}
data class NativeBrightnessControl(val key: String, val minimum: Float, val maximum: Float, val current: Float, val integer: Boolean,
    val touchBounds: com.modukey.app.core.Bounds? = null, val requiresTouch: Boolean = false, val windowId: Int = -1) {
    fun target(fraction: Float) = (minimum + (maximum - minimum) * fraction).let { if (integer) it.roundToInt().toFloat() else it }
    fun matches(fraction: Float) = abs(current - target(fraction)) <= if (integer) .51f else maxOf(.0001f, (maximum - minimum) * .00001f)
    fun sameRange(other: NativeBrightnessControl) = key == other.key && minimum == other.minimum && maximum == other.maximum && integer == other.integer
}
interface CalibrationServicePort {
    fun open(): Boolean
    fun control(): NativeBrightnessControl?
    fun move(expected: NativeBrightnessControl, fraction: Float): Boolean
    val canCompleteTouch: Boolean get() = false
    val inputInProgress: Boolean get() = false
    suspend fun apply(expected: NativeBrightnessControl, fraction: Float): Boolean = move(expected, fraction)
    fun waitingForPanel() = Unit
    fun progress(percent: Int)
    fun close()
    fun disable() = close()
}
internal interface CalibrationBrightnessAccess {
    val canWrite: Boolean
    val pendingRestore: Boolean
    fun begin(): Boolean
    fun sample(): Int?
    fun restore(): Boolean
    suspend fun restoreAfterTouch(): Boolean = restore()
    fun discardRestore(): Boolean
    fun save(samples: List<Int>): Boolean
}
internal class ControllerCalibrationAccess(private val controller: BrightnessController) : CalibrationBrightnessAccess {
    override val canWrite get() = controller.canWrite
    override val pendingRestore get() = controller.calibrationNeedsRecovery
    override fun begin() = controller.beginCalibration()
    override fun sample() = controller.calibrationSample()
    override fun restore() = controller.restoreAfterCalibration()
    override suspend fun restoreAfterTouch() = controller.restoreAfterCalibrationTouch()
    override fun discardRestore() = controller.keepBrightnessAfterCalibration()
    override fun save(samples: List<Int>) = controller.calibration.save(samples)
}

/** Consent and setup are memory-only. A killed process can recover brightness but cannot resume a run. */
class AutomaticBrightnessCalibration internal constructor(private val brightness: CalibrationBrightnessAccess,
    private val awake: () -> Boolean, private val clock: () -> Long = SystemClock::uptimeMillis,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    private val keepServiceEnabled: () -> Boolean = { false }, private val diagnostic: (String) -> Unit = {}) {
    private val mutable = MutableStateFlow(AutomaticCalibrationState(if (brightness.pendingRestore) AutomaticCalibrationPhase.RECOVERY else AutomaticCalibrationPhase.IDLE))
    val state = mutable.asStateFlow()
    private val mutableConnected = MutableStateFlow(false)
    val connected = mutableConnected.asStateFlow()
    private var port: CalibrationServicePort? = null
    private var setup: Job? = null
    private var job: Job? = null
    private var token: Any? = null
    private var requestedError: AutomaticCalibrationError? = null
    private var usedTouch = false
    private var finishing = false
    private class Failure(val reason: AutomaticCalibrationError) : RuntimeException()

    fun prepare(): Boolean {
        if (mutable.value.busy || brightness.pendingRestore) return false
        val error = if (!brightness.canWrite) AutomaticCalibrationError.ACCESS else if (!awake()) AutomaticCalibrationError.LOCKED else null
        if (error != null) { mutable.value = AutomaticCalibrationState(AutomaticCalibrationPhase.FAILED, error = error); return false }
        mutable.value = AutomaticCalibrationState(if (port == null) AutomaticCalibrationPhase.WAITING_SERVICE else AutomaticCalibrationPhase.READY)
        setupTimeout()
        return true
    }
    internal fun connect(next: CalibrationServicePort): Boolean {
        if (port != null) return false
        val waiting = mutable.value.phase == AutomaticCalibrationPhase.WAITING_SERVICE
        if (!waiting && (!keepServiceEnabled() || mutable.value.busy)) return false
        port = next; mutableConnected.value = true
        if (waiting) { mutable.value = AutomaticCalibrationState(AutomaticCalibrationPhase.READY); setupTimeout() }
        return true
    }
    private fun setupTimeout() {
        setup?.cancel()
        setup = scope.launch { delay(120_000); cancel(AutomaticCalibrationError.SETUP_TIMEOUT) }
    }
    fun settingsUnavailable() = cancel(AutomaticCalibrationError.SETTINGS)
    fun start() {
        val service = port ?: return
        if (mutable.value.phase != AutomaticCalibrationPhase.READY) return
        setup?.cancel(); setup = null
        val owner = Any(); token = owner; requestedError = null; usedTouch = false
        mutable.value = AutomaticCalibrationState(AutomaticCalibrationPhase.RUNNING, waitingForPanel = true)
        job = scope.launch(start = CoroutineStart.LAZY) {
            var result: List<Int>? = null
            var failure: AutomaticCalibrationError? = null
            try {
                eligible()
                if (!service.open()) throw Failure(AutomaticCalibrationError.UNSUPPORTED)
                if (token !== owner) throw CancellationException()
                service.waitingForPanel()
                var control: NativeBrightnessControl? = null
                withTimeoutOrNull(30_000) { while (control == null) { eligible(); control = service.control(); if (control == null) delay(100) } }
                if (control == null) throw Failure(AutomaticCalibrationError.UNSUPPORTED)
                eligible()
                if (token !== owner) throw CancellationException()
                if (control!!.requiresTouch && !service.canCompleteTouch) throw Failure(AutomaticCalibrationError.TOUCH_REQUIRED)
                if (!brightness.begin()) throw Failure(if (!brightness.canWrite) AutomaticCalibrationError.ACCESS else AutomaticCalibrationError.RESTORE)
                val expected = requireNotNull(control)
                usedTouch = expected.requiresTouch
                result = withTimeout(60_000) {
                    val samples = mutableListOf<Int>()
                    val steps = BrightnessScale.AUTOMATIC_SAMPLE_COUNT - 1
                    for (point in 0..steps) {
                        val fraction = point.toFloat() / steps
                        val percent = (fraction * 100).roundToInt()
                        eligible(); mutable.value = AutomaticCalibrationState(AutomaticCalibrationPhase.RUNNING, percent)
                        service.progress(percent)
                        if (!service.apply(expected, fraction)) throw Failure(AutomaticCalibrationError.UNSUPPORTED)
                        currentCoroutineContext().ensureActive()
                        val value = settled(service, expected, fraction)
                        if (value !in 0..65535 || samples.lastOrNull()?.let { value < it } == true) {
                            diagnostic("Brightness calibration rejected: point=$point; reason=${if (value !in 0..65535) "out_of_range" else "decreasing"}")
                            throw Failure(AutomaticCalibrationError.INVALID_SAMPLES)
                        }
                        samples += value
                    }
                    if (!BrightnessScale.validSamples(samples)) {
                        diagnostic("Brightness calibration rejected: points=${samples.size}; reason=flat_curve")
                        throw Failure(AutomaticCalibrationError.INVALID_SAMPLES)
                    }
                    samples.toList()
                }
            } catch (error: TimeoutCancellationException) { failure = AutomaticCalibrationError.UNSTABLE }
            catch (error: Failure) { failure = error.reason }
            catch (error: CancellationException) { throw error }
            catch (_: RuntimeException) { failure = AutomaticCalibrationError.UNSUPPORTED }
            finally { finish(owner, result, failure) }
        }
        job?.start()
    }
    private fun eligible() {
        if (!brightness.canWrite) throw Failure(AutomaticCalibrationError.ACCESS)
        if (!awake()) throw Failure(AutomaticCalibrationError.LOCKED)
    }
    private suspend fun settled(service: CalibrationServicePort, expected: NativeBrightnessControl, fraction: Float): Int {
        var last: Int? = null; var stableSince = clock(); var result: Int? = null
        withTimeout(3_000) {
            while (result == null) {
                eligible()
                val current = service.control()
                if (current != null && !expected.sameRange(current)) {
                    diagnostic("Brightness calibration stopped: reason=control_identity_or_range_changed")
                    throw Failure(AutomaticCalibrationError.CONTROL_CHANGED)
                }
                if (current?.matches(fraction) != true) { last = null; stableSince = clock() }
                else {
                    val value = brightness.sample() ?: run {
                        diagnostic("Brightness calibration rejected: reason=unreadable_or_automatic_mode")
                        throw Failure(AutomaticCalibrationError.INVALID_SAMPLES)
                    }
                    if (value == last) { if (clock() - stableSince >= 500) result = value }
                    else { last = value; stableSince = clock() }
                }
                if (result == null) delay(100)
            }
        }
        return requireNotNull(result)
    }
    fun cancel(error: AutomaticCalibrationError? = null) {
        if (finishing) return
        val owner = token
        val running = job
        if (owner != null && port?.inputInProgress == true) {
            requestedError = error; running?.cancel(); return
        }
        if (owner != null) finish(owner, null, error)
        else {
            setup?.cancel(); setup = null
            releasePort()
            mutable.value = AutomaticCalibrationState(if (brightness.pendingRestore) AutomaticCalibrationPhase.RECOVERY
                else if (error == null) AutomaticCalibrationPhase.CANCELLED else AutomaticCalibrationPhase.FAILED, error = error)
        }
        running?.cancel()
    }
    private fun finish(owner: Any, result: List<Int>?, error: AutomaticCalibrationError?) {
        if (token !== owner) return
        val failure = error ?: requestedError
        requestedError = null
        token = null; job = null
        fun publish(restored: Boolean) {
            releasePort()
            mutable.value = when {
                !restored -> AutomaticCalibrationState(AutomaticCalibrationPhase.RECOVERY, error = AutomaticCalibrationError.RESTORE)
                result != null && failure == null -> AutomaticCalibrationState(AutomaticCalibrationPhase.REVIEW, samples = result)
                failure != null -> AutomaticCalibrationState(AutomaticCalibrationPhase.FAILED, error = failure)
                else -> AutomaticCalibrationState(AutomaticCalibrationPhase.CANCELLED)
            }
        }
        if (usedTouch && brightness.pendingRestore) {
            finishing = true
            scope.launch(NonCancellable) {
                try { publish(runCatching { brightness.restoreAfterTouch() }.getOrDefault(false)) }
                finally { finishing = false }
            }
        } else publish(runCatching { !brightness.pendingRestore || brightness.restore() }.getOrDefault(false))
    }
    private fun releasePort() {
        val service = port ?: return
        if (!keepServiceEnabled()) { port = null; mutableConnected.value = false }
        if (runCatching { service.close() }.isFailure) {
            port = null; mutableConnected.value = false
            runCatching { service.disable() }
        }
    }
    internal fun disconnected(service: CalibrationServicePort) {
        if (port !== service) return
        port = null; mutableConnected.value = false
        if (mutable.value.busy) {
            if (service.inputInProgress && token != null) { requestedError = AutomaticCalibrationError.DISCONNECTED; job?.cancel() }
            else cancel(AutomaticCalibrationError.DISCONNECTED)
        }
        runCatching { service.close() }
    }
    internal fun interrupted(service: CalibrationServicePort) {
        if (port === service && mutable.value.busy) cancel(AutomaticCalibrationError.DISCONNECTED)
    }
    fun disableService() {
        if (mutable.value.busy) cancel()
        val service = port; port = null; mutableConnected.value = false
        runCatching { service?.disable() }
    }
    fun save() {
        val current = mutable.value
        if (current.phase != AutomaticCalibrationPhase.REVIEW || !BrightnessScale.validSamples(current.samples)) return
        mutable.value = if (brightness.save(current.samples)) AutomaticCalibrationState(AutomaticCalibrationPhase.SAVED)
            else current.copy(error = AutomaticCalibrationError.SAVE)
    }
    fun discard() { if (!mutable.value.busy && !brightness.pendingRestore) mutable.value = AutomaticCalibrationState() }
    fun recover(restore: Boolean) {
        if (mutable.value.busy || finishing) return
        fun publish(success: Boolean) {
            mutable.value = AutomaticCalibrationState(if (success) AutomaticCalibrationPhase.IDLE else AutomaticCalibrationPhase.RECOVERY,
                error = if (success) null else AutomaticCalibrationError.RESTORE)
        }
        if (!restore) { publish(brightness.discardRestore()); return }
        finishing = true
        scope.launch(NonCancellable) {
            try { publish(runCatching { brightness.restoreAfterTouch() }.getOrDefault(false)) }
            finally { finishing = false }
        }
    }
    fun close() { disableService(); scope.cancel() }
}
