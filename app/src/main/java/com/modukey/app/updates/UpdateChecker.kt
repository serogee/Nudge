package com.modukey.app.updates

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

data class PublishedRelease(val tag: String, val version: ReleaseVersion, val prerelease: Boolean, val pageUrl: String, val apkUrl: String?)

fun interface ReleaseSource {
    suspend fun latest(includePrereleases: Boolean): PublishedRelease?
}

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class NoRelease(val includePrereleases: Boolean) : UpdateState
    data class Result(val release: PublishedRelease, val comparison: Int, val includePrereleases: Boolean) : UpdateState
    data class Failed(val message: String) : UpdateState
}

class UpdateCheckException(message: String) : IOException(message)

class UpdateChecker(private val source: ReleaseSource, private val installedVersion: String, private val scope: CoroutineScope) {
    private val mutableState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state = mutableState.asStateFlow()
    private var request: Job? = null
    private var revision = 0L
    private var activeIncludePrereleases = false

    fun clear() {
        revision++
        request?.cancel()
        request = null
        mutableState.value = UpdateState.Idle
    }

    fun check(includePrereleases: Boolean = false) {
        if (mutableState.value == UpdateState.Checking && activeIncludePrereleases == includePrereleases) return
        clear()
        activeIncludePrereleases = includePrereleases
        val requestRevision = revision
        fun publish(value: UpdateState) { if (revision == requestRevision) mutableState.value = value }
        mutableState.value = UpdateState.Checking
        request = scope.launch {
            try {
                val installed = ReleaseVersion.parse(installedVersion.removeSuffix("-debug"))
                    ?: throw UpdateCheckException("The installed version could not be compared. View Nudge's releases on GitHub.")
                val release = source.latest(includePrereleases)
                publish(if (release == null) UpdateState.NoRelease(includePrereleases) else UpdateState.Result(release, release.version.compareTo(installed), includePrereleases))
            } catch (error: CancellationException) {
                publish(UpdateState.Idle)
                throw error
            } catch (error: UpdateCheckException) {
                publish(UpdateState.Failed(error.message.orEmpty()))
            } catch (_: IOException) {
                publish(UpdateState.Failed("Could not connect to GitHub. Check your connection and try again."))
            } catch (_: RuntimeException) {
                publish(UpdateState.Failed("The update response could not be read. Try again later or view releases on GitHub."))
            }
        }
    }
}
