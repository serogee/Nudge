package com.modukey.app.core

import org.junit.Assert.*
import org.junit.Test

class HandleControlsTest {
    private val config = AppConfiguration(enabled = true, handles = listOf(
        HandleConfig(id = "a", enabled = true), HandleConfig(id = "b", enabled = true), HandleConfig(id = "disabled")))
    @Test fun independentGroupsResolveTheirCurrentStatesWithoutChangingOtherHandles() {
        val current = config.hideIndicators(setOf("disabled"), true)
        val change = checkNotNull(handleControlChange(current, "a", setOf("a"), HandleGroupScope.CURRENT, HandleGroupScope.OTHERS))
        assertEquals(setOf("a"), change.pauseTargets); assertFalse(change.paused)
        assertEquals(setOf("b"), change.visibilityTargets); assertTrue(change.hidden)
        assertEquals(setOf("b", "disabled"), current.hideIndicators(change.visibilityTargets, change.hidden).hiddenIndicatorIds)
    }
    @Test fun mixedGroupsResumeAndRevealTheWholeTargetGroup() {
        val change = checkNotNull(handleControlChange(config.hideIndicators(setOf("b"), true), null, setOf("a"), HandleGroupScope.ALL, HandleGroupScope.ALL))
        assertEquals(setOf("a", "b"), change.pauseTargets); assertFalse(change.paused); assertFalse(change.hidden)
    }
    @Test fun invalidOrMissingGroupsFailBeforeAnyMutationAndNoneSkipsAGroup() {
        assertNull(handleControlChange(config, null, emptySet(), HandleGroupScope.CURRENT, HandleGroupScope.ALL))
        assertNull(handleControlChange(config, "disabled", emptySet(), HandleGroupScope.ALL, HandleGroupScope.OTHERS))
        assertNull(handleControlChange(config, "a", emptySet(), HandleGroupScope.NONE, HandleGroupScope.NONE))
        assertNull(handleControlChange(config.copy(enabled = false), "a", emptySet(), HandleGroupScope.ALL, HandleGroupScope.NONE))
        assertNotNull(handleControlChange(config.copy(enabled = false), null, emptySet(), HandleGroupScope.NONE, HandleGroupScope.ALL))
    }
    @Test fun shortcutsRequireAnOwnerForEitherRelativeScope() {
        val action = PanelButton(action = ButtonAction.TOGGLE_AND_HIDE_HANDLES, visibilityScope = HandleGroupScope.OTHERS)
        assertFalse(HomeShortcut(button = action).available(config))
        assertTrue(HomeShortcut(button = action, handleId = "a").available(config))
        assertFalse(HomeShortcut(button = action, handleId = "removed").available(config))
        assertTrue(HomeShortcut(button = action.copy(visibilityScope = HandleGroupScope.ALL)).available(config))
    }
}
