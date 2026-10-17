package com.modukey.app.platform

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException
import java.time.Instant

/** Bounded app-private diagnostics; no screen contents, app-switch history or periodic work. */
class LocalDiagnostics(private val context: Context, private val scope: CoroutineScope) {
    private val writes = Channel<Unit>(Channel.CONFLATED)
    private val lock = Any()
    private val entries = ArrayDeque<String>()
    private val mutable = MutableStateFlow("No diagnostics yet")
    val details = mutable.asStateFlow()
    private var lastMessage: String? = null

    init {
        scope.launch(Dispatchers.IO) {
            val file = File(context.filesDir, "diagnostics.txt")
            val previous = try { if (file.exists()) file.readText().takeLast(32_768).lines().takeLast(64) else emptyList() }
                catch (_: IOException) { listOf("Previous diagnostics could not be read") }
            synchronized(lock) {
                val current = entries.toList()
                entries.clear()
                (previous + current).takeLast(64).forEach(entries::addLast)
                publish()
            }
            for (signal in writes) {
                try { file.writeText(mutable.value) } catch (_: IOException) { /* Diagnostic failure must never stop the service. */ }
            }
        }
    }

    fun record(message: String, error: Throwable? = null) = synchronized(lock) {
        val detail = (message + (error?.let { ": ${it.javaClass.simpleName}: ${it.message}\n${it.stackTrace.take(5).joinToString("\n")}" } ?: ""))
            .replace('\n', ' ').take(1_200)
        if (lastMessage == detail) return@synchronized
        lastMessage = detail
        entries.addLast("${Instant.now()} $detail")
        publish()
        writes.trySend(Unit)
        Unit
    }

    private fun publish() {
        while (entries.size > 64) entries.removeFirst()
        mutable.value = entries.joinToString("\n").takeLast(32_768)
    }

    suspend fun inspectExits() = withContext(Dispatchers.IO) {
        try {
            val exits = context.getSystemService(ActivityManager::class.java)
                .getHistoricalProcessExitReasons(context.packageName, 0, 5)
            if (exits.isEmpty()) record("Process exits: no recent records")
            exits.forEach { exit ->
                val reason = when (exit.reason) {
                    ApplicationExitInfo.REASON_CRASH -> "crash"
                    ApplicationExitInfo.REASON_CRASH_NATIVE -> "native crash"
                    ApplicationExitInfo.REASON_ANR -> "not responding"
                    ApplicationExitInfo.REASON_USER_REQUESTED -> "user requested (may include Recents removal or force-stop)"
                    ApplicationExitInfo.REASON_LOW_MEMORY -> "low memory"
                    ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "excessive resource usage"
                    else -> "system reason ${exit.reason}"
                }
                record("Exit ${Instant.ofEpochMilli(exit.timestamp)}: $reason; status=${exit.status}; ${exit.description.orEmpty().take(240)}")
            }
        } catch (error: RuntimeException) { record("Process-exit inspection unavailable", error) }
    }
}
