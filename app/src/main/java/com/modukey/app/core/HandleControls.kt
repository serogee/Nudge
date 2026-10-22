package com.modukey.app.core

enum class HandleGroupScope { NONE, ALL, CURRENT, OTHERS }

fun handleGroupTargets(scope: HandleGroupScope, owner: String?, enabled: Set<String>): Set<String> =
    if (scope == HandleGroupScope.NONE) emptySet() else handleToggleTargets(HandleToggleScope.valueOf(scope.name), owner, enabled)

val PanelButton.needsHandleOwner: Boolean get() = when (action) {
    ButtonAction.TOGGLE_HANDLES, ButtonAction.TOGGLE_HANDLE_VISIBILITY -> handleScope != HandleToggleScope.ALL
    ButtonAction.TOGGLE_AND_HIDE_HANDLES -> listOf(toggleScope, visibilityScope).any { it == HandleGroupScope.CURRENT || it == HandleGroupScope.OTHERS }
    else -> false
}
val PanelButton.hasHandleGroups get() = toggleScope != HandleGroupScope.NONE || visibilityScope != HandleGroupScope.NONE

data class HandleControlChange(val pauseTargets: Set<String>, val paused: Boolean, val visibilityTargets: Set<String>, val hidden: Boolean)

/** Resolve both groups before any mutation. Pauses and visibility use independent group toggles. */
fun handleControlChange(configuration: AppConfiguration, owner: String?, paused: Set<String>,
    toggleScope: HandleGroupScope, visibilityScope: HandleGroupScope): HandleControlChange? {
    if (toggleScope == HandleGroupScope.NONE && visibilityScope == HandleGroupScope.NONE) return null
    if (toggleScope != HandleGroupScope.NONE && !configuration.enabled) return null
    val enabled = configuration.handles.filter { it.enabled }.mapTo(mutableSetOf()) { it.id }
    val pauseTargets = handleGroupTargets(toggleScope, owner, enabled)
    val visibilityTargets = handleGroupTargets(visibilityScope, owner, enabled)
    if (toggleScope != HandleGroupScope.NONE && pauseTargets.isEmpty()) return null
    if (visibilityScope != HandleGroupScope.NONE && visibilityTargets.isEmpty()) return null
    return HandleControlChange(pauseTargets, pauseTargets.none { it in paused }, visibilityTargets,
        visibilityTargets.none { it in configuration.hiddenIndicatorIds })
}
