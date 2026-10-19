package com.modukey.app.data

import com.modukey.app.core.*
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
    private val mutablePreview = MutableStateFlow<AppConfiguration?>(null)
    /** UI and editing overlays only. Runtime actions and disk saves use [state]. */
    val preview = mutablePreview.asStateFlow()
    private val mutableDraft = MutableStateFlow<ConfigurationDraft?>(null)
    val draft = mutableDraft.asStateFlow()
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
            try { val first = repository.configuration.first(); if (mutable.value == null) { mutable.value = first; publishPreview(); mutableError.value = null } }
            catch (_: IOException) { mutableError.value = ConfigurationError.READ }
        }
    }
    fun update(edit: (AppConfiguration) -> AppConfiguration) {
        val before = mutable.value ?: return
        val after = edit(before).normalized()
        if (before == after) return
        mutable.value = after; revision++
        publishPreview()
        debounce?.cancel()
        debounce = scope.launch { delay(150); debounce = null; scope.launch { save() } }
    }
    fun openHandle(id: String) {
        if ((mutableDraft.value as? ConfigurationDraft.Handle)?.value?.id == id) return
        check(mutableDraft.value?.changed != true) { "Resolve the current draft before opening another editor" }
        mutableDraft.value = mutable.value?.handle(id)?.let { ConfigurationDraft.Handle(it) }
        publishPreview()
    }
    fun openPanel(id: String) {
        if ((mutableDraft.value as? ConfigurationDraft.Panel)?.value?.id == id) return
        check(mutableDraft.value?.changed != true) { "Resolve the current draft before opening another editor" }
        mutableDraft.value = mutable.value?.panel(id)?.let { ConfigurationDraft.Panel(it) }
        publishPreview()
    }
    fun editHandle(id: String, edit: (HandleConfig) -> HandleConfig) {
        val current = mutableDraft.value as? ConfigurationDraft.Handle
        if (current?.value?.id != id) { update { it.editHandle(id, edit) }; return }
        val configuration = mutable.value ?: return
        val value = configuration.editHandle(id) { edit(current.value).copy(id = id) }.normalized().handle(id) ?: return
        mutableDraft.value = current.copy(value = value)
        publishPreview()
    }
    fun editPanel(id: String, edit: (SavedPanel) -> SavedPanel) {
        val current = mutableDraft.value as? ConfigurationDraft.Panel
        if (current?.value?.id != id) { update { it.editPanel(id, edit) }; return }
        mutableDraft.value = current.copy(value = edit(current.value).copy(id = id).normalized())
        publishPreview()
    }
    fun saveDraft(completed: ((Boolean) -> Unit)? = null) {
        val current = mutableDraft.value ?: return
        update(current::applyTo)
        cancelDraft()
        debounce?.cancel(); debounce = null
        scope.launch { val success = save(); completed?.invoke(success) }
    }
    fun cancelDraft() {
        mutableDraft.value = mutable.value?.let { mutableDraft.value?.baseline(it) }
        publishPreview()
    }
    fun closeDraft() { mutableDraft.value = null; publishPreview() }
    private fun publishPreview() {
        val configuration = mutable.value
        val current = mutableDraft.value
        if (configuration != null && current != null) {
            val baseline = current.baseline(configuration)
            if (baseline == null || !current.changed) mutableDraft.value = baseline
        }
        mutablePreview.value = configuration?.let { mutableDraft.value?.applyTo(it)?.normalized() ?: it }
    }
    fun flush() { debounce?.cancel(); debounce = null; scope.launch { save() } }
    suspend fun flushNow(): Boolean { debounce?.cancel(); debounce = null; return save() }
    fun retry() { if (mutable.value == null) load() else flush() }
    private suspend fun save(): Boolean = saveLock.withLock {
        val snapshot = mutable.value ?: return@withLock false
        if (savedRevision == revision) return@withLock true
        val writingRevision = revision
        try { repository.write(snapshot); savedRevision = writingRevision; mutableError.value = null; true }
        catch (_: IOException) { mutableError.value = ConfigurationError.SAVE; false }
    }
}
