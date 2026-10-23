package com.modukey.app.core

import org.junit.Assert.*
import org.junit.Test

class HandleControlsTest {
    private val config = AppConfiguration(enabled = true, handles = listOf(
        HandleConfig(id = "a", enabled = true), HandleConfig(id = "b", enabled = true), HandleConfig(id = "disabled")))
    @Test fun separateGroupsFollowThePauseDirectionWithoutChangingUnrelatedHandles() {
        val current = config.hideIndicators(setOf("disabled"), true)
        val change = checkNotNull(handleControlChange(current, "a", setOf("a"), HandleGroupScope.CURRENT, HandleGroupScope.OTHERS))
        assertEquals(setOf("a"), change.pauseTargets); assertFalse(change.paused)
        assertEquals(setOf("b"), change.visibilityTargets); assertFalse(change.hidden)
        assertEquals(setOf("disabled"), current.hideIndicators(change.visibilityTargets, change.hidden).hiddenIndicatorIds)
    }
    @Test fun mixedGroupsResumeAndRevealTheWholeTargetGroup() {
        val change = checkNotNull(handleControlChange(config.hideIndicators(setOf("b"), true), null, setOf("a"), HandleGroupScope.ALL, HandleGroupScope.ALL))
        assertEquals(setOf("a", "b"), change.pauseTargets); assertFalse(change.paused); assertFalse(change.hidden)
    }
    @Test fun combinedDirectionRepairsEitherKindOfMismatch() {
        for (isPaused in listOf(false, true)) for (isHidden in listOf(false, true)) {
            val current = config.hideIndicators(isHidden)
            val change = checkNotNull(handleControlChange(current, "a", if (isPaused) setOf("b") else emptySet(),
                HandleGroupScope.OTHERS, HandleGroupScope.ALL))
            assertEquals(!isPaused, change.paused)
            assertEquals(change.paused, change.hidden)
            assertEquals(setOf("b"), change.pauseTargets)
            assertEquals(setOf("a", "b"), change.visibilityTargets)
        }
    }
    @Test fun visibilityOnlyStillUsesItsOwnStateWhileMasterIsOff() {
        val current = config.copy(enabled = false).hideIndicators(setOf("a"), true)
        val change = checkNotNull(handleControlChange(current, null, setOf("b"), HandleGroupScope.NONE, HandleGroupScope.ALL))
        assertTrue(change.pauseTargets.isEmpty()); assertFalse(change.hidden)
        val shown = current.hideIndicators(change.visibilityTargets, change.hidden)
        val next = checkNotNull(handleControlChange(shown, null, setOf("b"), HandleGroupScope.NONE, HandleGroupScope.ALL))
        assertTrue(next.hidden)
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
