package com.modukey.app.data

import com.modukey.app.core.TriggerSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

/** Application-owned draft. All callers use the main dispatcher; disk never gates UI feedback. */
class SettingsController(private val repository: SettingsRepository, private val scope: CoroutineScope) {
    private val mutableState = MutableStateFlow<TriggerSettings?>(null)
    val state = mutableState.asStateFlow()
    private val mutableError = MutableStateFlow<String?>(null)
    val error = mutableError.asStateFlow()
    private data class Edit(val revision: Long, val apply: (TriggerSettings) -> TriggerSettings)
    private val pending = linkedMapOf<String, Edit>()
    private var revision = 0L
    private var debounce: Job? = null
    private var loadJob: Job? = null
    private val saveLock = Mutex()

    init { load() }

    private fun load() {
        if (loadJob?.isActive == true) return
        loadJob = scope.launch {
            try {
                // This is the only in-process writer. Later storage emissions are deliberately
                // not UI state: a delayed disk snapshot must never rewind an optimistic edit.
                val initial = repository.settings.first()
                if (mutableState.value == null) {
                    mutableState.value = initial
                    mutableError.value = null
                }
            } catch (_: IOException) { mutableError.value = "Saved settings could not be read. Retry when storage is available." }
        }
    }

    fun update(transform: (TriggerSettings) -> TriggerSettings) {
        val before = mutableState.value ?: return
        val after = transform(before).normalized()
        if (before == after) return
        fun <T> field(key: String, old: T, value: T, apply: (TriggerSettings, T) -> TriggerSettings) {
            if (old != value) pending[key] = Edit(++revision) { apply(it, value) }
        }
        field("enabled", before.enabled, after.enabled) { s, v -> s.copy(enabled = v) }
        field("portrait.edge", before.portrait.edge, after.portrait.edge) { s, v -> s.copy(portrait = s.portrait.copy(edge = v)) }
        field("portrait.position", before.portrait.position, after.portrait.position) { s, v -> s.copy(portrait = s.portrait.copy(position = v)) }
        field("landscape.edge", before.landscape.edge, after.landscape.edge) { s, v -> s.copy(landscape = s.landscape.copy(edge = v)) }
        field("landscape.position", before.landscape.position, after.landscape.position) { s, v -> s.copy(landscape = s.landscape.copy(position = v)) }
        field("landscape.enabled", before.landscapeEnabled, after.landscapeEnabled) { s, v -> s.copy(landscapeEnabled = v) }
        field("landscape.override", before.landscapeOverrideEnabled, after.landscapeOverrideEnabled) { s, v -> s.copy(landscapeOverrideEnabled = v) }
        field("lockscreen", before.lockScreenEnabled, after.lockScreenEnabled) { s, v -> s.copy(lockScreenEnabled = v) }
        field("length", before.lengthDp, after.lengthDp) { s, v -> s.copy(lengthDp = v) }
        field("depth", before.depthDp, after.depthDp) { s, v -> s.copy(depthDp = v) }
        field("indicator.visible", before.indicatorVisible, after.indicatorVisible) { s, v -> s.copy(indicatorVisible = v) }
        field("indicator.fraction", before.indicatorLengthFraction, after.indicatorLengthFraction) { s, v -> s.copy(indicatorLengthFraction = v) }
        field("indicator.thickness.fraction", before.indicatorThicknessFraction, after.indicatorThicknessFraction) { s, v -> s.copy(indicatorThicknessFraction = v) }
        field("indicator.color", before.indicatorColor, after.indicatorColor) { s, v -> s.copy(indicatorColor = v) }
        field("indicator.opacity", before.indicatorOpacity, after.indicatorOpacity) { s, v -> s.copy(indicatorOpacity = v) }
        field("indicator.radius", before.cornerRadiusDp, after.cornerRadiusDp) { s, v -> s.copy(cornerRadiusDp = v) }
        field("activation", before.activation, after.activation) { s, v -> s.copy(activation = v) }
        field("swipe.distance", before.swipeDistanceDp, after.swipeDistanceDp) { s, v -> s.copy(swipeDistanceDp = v) }
        field("swipe.timeout", before.swipeTimeoutMs, after.swipeTimeoutMs) { s, v -> s.copy(swipeTimeoutMs = v) }
        field("haptics", before.haptics, after.haptics) { s, v -> s.copy(haptics = v) }
        field("exclusions", before.excludedPackages, after.excludedPackages) { s, v -> s.copy(excludedPackages = v) }
        field("updates.include.prereleases", before.includePrereleases, after.includePrereleases) { s, v -> s.copy(includePrereleases = v) }
        mutableState.value = after
        debounce?.cancel()
        debounce = scope.launch { delay(150); debounce = null; scope.launch { save() } }
    }

    fun flush() {
        debounce?.cancel()
        debounce = null
        scope.launch { save() }
    }

    fun retry() { if (mutableState.value == null) load() else flush() }
    fun reset() { update { TriggerSettings(enabled = it.enabled, includePrereleases = it.includePrereleases) }; flush() }

    private suspend fun save() = saveLock.withLock {
        if (pending.isNotEmpty()) {
            val batch = pending.toMap()
            var written: TriggerSettings? = null
            try {
                repository.update { disk ->
                    batch.values.fold(disk) { s, edit -> edit.apply(s) }.normalized().also { written = it }
                }
                batch.forEach { (key, edit) -> if (pending[key]?.revision == edit.revision) pending.remove(key) }
                mutableState.value = pending.values.fold(checkNotNull(written)) { s, edit -> edit.apply(s) }
                mutableError.value = null
            } catch (_: IOException) {
                mutableError.value = "Changes are still visible but have not been saved. Check storage, then tap Retry saving."
                return@withLock
            }
        }
    }
}
