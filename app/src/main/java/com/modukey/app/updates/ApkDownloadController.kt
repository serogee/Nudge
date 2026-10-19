package com.modukey.app.updates

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.URI

data class ApkTarget(val tag: String, val url: String) {
    val versionName: String get() = tag.removePrefix("v").removePrefix("V")
    val filename: String get() = "Nudge-$versionName.apk"

    fun valid(): Boolean = ReleaseVersion.parse(tag) != null && try {
        val uri = URI(url)
        uri.scheme == "https" && uri.host.equals("github.com", ignoreCase = true) && uri.userInfo == null &&
            uri.port in listOf(-1, 443) && uri.query == null && uri.fragment == null &&
            uri.path == "/serogee/Nudge/releases/download/$tag/$filename"
    } catch (_: IllegalArgumentException) { false }
    catch (_: java.net.URISyntaxException) { false }
}

data class ApkDownloadRecord(val id: Long, val target: ApkTarget, val filename: String)
enum class ApkDownloadPhase { QUEUED, DOWNLOADING, PAUSED, VERIFYING }
enum class ApkDownloadFailure { UNAVAILABLE, STORAGE, NETWORK, MISSING, INVALID_APK, PACKAGE, VERSION, SIGNATURE, DEBUG_BUILD, UNKNOWN }
class ApkDownloadException(val reason: ApkDownloadFailure) : Exception()

sealed interface ApkTransfer {
    data class Active(val phase: ApkDownloadPhase, val bytes: Long = 0, val total: Long = -1) : ApkTransfer
    data object Complete : ApkTransfer
    data class Failed(val reason: ApkDownloadFailure) : ApkTransfer
}

sealed interface ApkDownloadState {
    val target: ApkTarget? get() = null
    data object Idle : ApkDownloadState
    data class Active(override val target: ApkTarget, val phase: ApkDownloadPhase, val bytes: Long = 0, val total: Long = -1) : ApkDownloadState
    data class Ready(override val target: ApkTarget) : ApkDownloadState
    data class Failed(override val target: ApkTarget?, val reason: ApkDownloadFailure) : ApkDownloadState
}

interface ApkDownloadBackend {
    val completions: Flow<Long>
    fun restore(): ApkDownloadRecord?
    fun enqueue(target: ApkTarget): ApkDownloadRecord
    fun query(record: ApkDownloadRecord): ApkTransfer
    fun validate(record: ApkDownloadRecord): ApkDownloadFailure?
    fun remove(record: ApkDownloadRecord?)
}

/** Owns one download. Android owns the transfer; observation exists only while the update UI is resumed. */
class ApkDownloadController(
    private val backend: ApkDownloadBackend,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val mutableState = MutableStateFlow<ApkDownloadState>(ApkDownloadState.Idle)
    val state = mutableState.asStateFlow()
    private val mutex = Mutex()
    private var loaded = false
    private var record: ApkDownloadRecord? = null
    private var refreshJob: Job? = null

    fun download(target: ApkTarget) = scope.launch {
        mutex.withLock {
            try {
                restore()
                if (record != null) refreshLocked()
                if (mutableState.value is ApkDownloadState.Active ||
                    (mutableState.value is ApkDownloadState.Ready && record?.target == target)) return@withLock
                if (!target.valid()) {
                    mutableState.value = ApkDownloadState.Failed(target, ApkDownloadFailure.UNAVAILABLE)
                    return@withLock
                }
                mutableState.value = ApkDownloadState.Active(target, ApkDownloadPhase.QUEUED)
                withContext(io) { backend.remove(record) }
                record = null
                record = withContext(io) { backend.enqueue(target) }
                refreshLocked()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutableState.value = ApkDownloadState.Failed(target, (e as? ApkDownloadException)?.reason ?: ApkDownloadFailure.UNAVAILABLE) }
        }
    }

    fun retry() { mutableState.value.target?.let(::download) }

    fun cancel() = scope.launch {
        mutex.withLock {
            try {
                restore()
                withContext(io) { backend.remove(record) }
                record = null
                mutableState.value = ApkDownloadState.Idle
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutableState.value = ApkDownloadState.Failed(record?.target, ApkDownloadFailure.STORAGE) }
        }
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = scope.launch {
            mutex.withLock {
                try { restore(); refreshLocked() }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { mutableState.value = ApkDownloadState.Failed(record?.target, ApkDownloadFailure.UNAVAILABLE) }
            }
        }
    }

    suspend fun observe(): Unit = coroutineScope {
        launch { backend.completions.collect { id -> if (id == record?.id) refresh() } }
        refresh()
        state.map { it is ApkDownloadState.Active }.distinctUntilChanged().collectLatest { active ->
            if (active) while (isActive) { delay(1_000); refresh() }
        }
    }

    /** Recheck the file at the explicit Install tap, including after returning from source-access settings. */
    suspend fun prepareInstall(): ApkDownloadRecord? = mutex.withLock {
        try {
            restore()
            refreshLocked(forceValidation = true)
            record.takeIf { mutableState.value is ApkDownloadState.Ready }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) {
            mutableState.value = ApkDownloadState.Failed(record?.target, ApkDownloadFailure.INVALID_APK)
            null
        }
    }

    private suspend fun restore() {
        if (!loaded) { record = withContext(io) { backend.restore() }; loaded = true }
    }

    private suspend fun refreshLocked(forceValidation: Boolean = false) {
        val current = record ?: return
        when (val transfer = withContext(io) { backend.query(current) }) {
            is ApkTransfer.Active -> mutableState.value = ApkDownloadState.Active(current.target, transfer.phase, transfer.bytes, transfer.total)
            is ApkTransfer.Failed -> mutableState.value = ApkDownloadState.Failed(current.target, transfer.reason)
            ApkTransfer.Complete -> {
                if (!forceValidation && mutableState.value is ApkDownloadState.Ready) return
                mutableState.value = ApkDownloadState.Active(current.target, ApkDownloadPhase.VERIFYING)
                val failure = withContext(io) { backend.validate(current) }
                mutableState.value = if (failure == null) ApkDownloadState.Ready(current.target)
                    else ApkDownloadState.Failed(current.target, failure)
            }
        }
    }
}
