package com.modukey.app.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class HandleToggleScope { ALL, CURRENT, OTHERS }

fun handleToggleTargets(scope: HandleToggleScope, owner: String?, enabled: Set<String>): Set<String> = when (scope) {
    HandleToggleScope.ALL -> enabled
    HandleToggleScope.CURRENT -> enabled.filterTo(mutableSetOf()) { it == owner }
    HandleToggleScope.OTHERS -> if (owner == null || owner !in enabled) emptySet() else enabled - owner
}

/** Pause selections use the configuration writer when supplied. Enable switches remain independent. */
class HandlePauses(private val persist: ((Set<String>) -> Unit)? = null) {
    private val mutable = MutableStateFlow<Set<String>>(emptySet())
    val state = mutable.asStateFlow()
    private fun replace(next: Set<String>) {
        if (mutable.value == next) return
        if (persist == null) mutable.value = next else persist(next)
    }
    fun restore(configuration: AppConfiguration) {
        mutable.value = configuration.pausedHandleIds.intersect(configuration.handles.map { it.id }.toSet())
    }
    fun setTargets(targets: Set<String>, paused: Boolean) {
        replace(if (paused) mutable.value + targets else mutable.value - targets)
    }
    fun setAll(paused: Boolean, configuration: AppConfiguration) {
        if (!paused) clear()
        else if (configuration.enabled) setTargets(configuration.handles.filter { it.enabled }.mapTo(mutableSetOf()) { it.id }, true)
    }
    fun toggle(scope: HandleToggleScope, owner: String?, configuration: AppConfiguration): Boolean {
        if (!configuration.enabled) return false
        val targets = handleToggleTargets(scope, owner, configuration.handles.filter { it.enabled }.mapTo(mutableSetOf()) { it.id })
        if (targets.isEmpty()) return false
        setTargets(targets, targets.none { it in mutable.value })
        return true
    }
    fun reconcile(configuration: AppConfiguration) {
        if (persist != null) restore(configuration)
        else replace(mutable.value.intersect(configuration.handles.map { it.id }.toSet()))
    }
    fun clear() { replace(emptySet()) }
}
