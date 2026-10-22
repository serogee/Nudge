package com.modukey.app.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class HandleToggleScope { ALL, CURRENT, OTHERS }

fun handleToggleTargets(scope: HandleToggleScope, owner: String?, enabled: Set<String>): Set<String> = when (scope) {
    HandleToggleScope.ALL -> enabled
    HandleToggleScope.CURRENT -> enabled.filterTo(mutableSetOf()) { it == owner }
    HandleToggleScope.OTHERS -> if (owner == null || owner !in enabled) emptySet() else enabled - owner
}

/** Runtime pauses keep saved Enable switches intact. No timers or background work are scheduled. */
class HandlePauses {
    private val mutable = MutableStateFlow<Set<String>>(emptySet())
    val state = mutable.asStateFlow()
    fun setTargets(targets: Set<String>, paused: Boolean) {
        mutable.value = if (paused) mutable.value + targets else mutable.value - targets
    }
    fun setAll(paused: Boolean, configuration: AppConfiguration) {
        if (!paused) clear()
        else if (configuration.enabled) mutable.value = configuration.handles.filter { it.enabled }.mapTo(mutableSetOf()) { it.id }
    }
    fun toggle(scope: HandleToggleScope, owner: String?, configuration: AppConfiguration): Boolean {
        if (!configuration.enabled) return false
        val targets = handleToggleTargets(scope, owner, configuration.handles.filter { it.enabled }.mapTo(mutableSetOf()) { it.id })
        if (targets.isEmpty()) return false
        mutable.value = if (targets.any { it in mutable.value }) mutable.value - targets else mutable.value + targets
        return true
    }
    fun reconcile(configuration: AppConfiguration) {
        mutable.value = if (configuration.enabled) mutable.value.intersect(configuration.handles.filter { it.enabled }.map { it.id }.toSet()) else emptySet()
    }
    fun clear() { mutable.value = emptySet() }
}
