package com.modukey.app.data

import com.modukey.app.core.AppConfiguration
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

enum class ConfigurationError { READ, SAVE }

/** Sole in-process writer. Every field edit transforms the latest immutable draft on Main.
 * Revisioned snapshots keep pending edits (including deletions) until their save succeeds.
 * Disk completion never replaces a newer draft, and storage emissions never drive live editing.
 */
class ConfigurationController(private val repository: ConfigurationRepository, private val scope: CoroutineScope) {
    private val mutable = MutableStateFlow<AppConfiguration?>(null)
    val state = mutable.asStateFlow()
    private val mutableError = MutableStateFlow<ConfigurationError?>(null)
    val error = mutableError.asStateFlow()
    private var revision = 0L
    private var savedRevision = 0L
    private var debounce: Job? = null
    private var loading: Job? = null
    private val saveLock = Mutex()
    init { load() }
    private fun load() {
        if (loading?.isActive == true) return
        loading = scope.launch {
            try { val first = repository.configuration.first(); if (mutable.value == null) { mutable.value = first; mutableError.value = null } }
            catch (_: IOException) { mutableError.value = ConfigurationError.READ }
        }
    }
    fun update(edit: (AppConfiguration) -> AppConfiguration) {
        val before = mutable.value ?: return
        val after = edit(before).normalized()
        if (before == after) return
        mutable.value = after; revision++
        debounce?.cancel()
        debounce = scope.launch { delay(150); debounce = null; scope.launch { save() } }
    }
    fun flush() { debounce?.cancel(); debounce = null; scope.launch { save() } }
    fun retry() { if (mutable.value == null) load() else flush() }
    private suspend fun save() = saveLock.withLock {
        val snapshot = mutable.value ?: return@withLock
        if (savedRevision == revision) return@withLock
        val writingRevision = revision
        try { repository.write(snapshot); savedRevision = writingRevision; mutableError.value = null }
        catch (_: IOException) { mutableError.value = ConfigurationError.SAVE }
    }
}
