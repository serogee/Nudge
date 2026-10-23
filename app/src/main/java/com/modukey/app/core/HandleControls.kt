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

/** Resolve both groups before any mutation. A pause group sets the direction for both groups. */
fun handleControlChange(configuration: AppConfiguration, owner: String?, paused: Set<String>,
    toggleScope: HandleGroupScope, visibilityScope: HandleGroupScope): HandleControlChange? {
    if (toggleScope == HandleGroupScope.NONE && visibilityScope == HandleGroupScope.NONE) return null
    if (toggleScope != HandleGroupScope.NONE && !configuration.enabled) return null
    val enabled = configuration.handles.filter { it.enabled }.mapTo(mutableSetOf()) { it.id }
    val pauseTargets = handleGroupTargets(toggleScope, owner, enabled)
    val visibilityTargets = handleGroupTargets(visibilityScope, owner, enabled)
    if (toggleScope != HandleGroupScope.NONE && pauseTargets.isEmpty()) return null
    if (visibilityScope != HandleGroupScope.NONE && visibilityTargets.isEmpty()) return null
    val pause = pauseTargets.none { it in paused }
    return HandleControlChange(pauseTargets, pause, visibilityTargets,
        if (pauseTargets.isNotEmpty()) pause else visibilityTargets.none { it in configuration.hiddenIndicatorIds })
}
