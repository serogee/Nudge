package com.modukey.app.ui

import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import com.modukey.app.MainActivity
import com.modukey.app.NudgeApplication
import com.modukey.app.core.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ExclusionsInterfaceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @After fun cleanup() { (rule.activity.application as NudgeApplication).onTerminate() }
    private fun row(name: String) = rule.onNode(hasText(name) and hasClickAction() and hasAnyAncestor(isDialog()))
    private fun order(vararg names: String) {
        val positions = names.map { row(it).fetchSemanticsNode().boundsInRoot.top }
        assertEquals("App rows are out of order", positions.sorted(), positions)
    }
    @Test fun `selection pins apps and clearing restores order without changing another handle`() {
        val app = rule.activity.application as NudgeApplication
        rule.waitUntil(10_000) { rule.runOnIdle { app.configuration.state.value != null || app.configuration.error.value != null } }
        val handle = HandleConfig(id = "editing", trigger = TriggerSettings(depthDp = 22f, excludedPackages = setOf("test.missing")))
        val other = HandleConfig(id = "other", trigger = TriggerSettings(excludedPackages = setOf("test.delta")))
        val vm = rule.runOnIdle {
            assertNull(app.configuration.error.value)
            app.configuration.update { AppConfiguration(handles = listOf(handle, other)) }
            ViewModelProvider(rule.activity)[NudgeViewModel::class.java].also {
                it.launchableApps.value = listOf(LaunchableApp("Alpha", "test.alpha", "Main"),
                    LaunchableApp("Bravo", "test.bravo", "Main"), LaunchableApp("Bravo", "test.bravo", "Second"),
                    LaunchableApp("Charlie", "test.charlie", "Main"), LaunchableApp("Delta", "test.delta", "Main"))
                it.appsLoaded.value = true
            }
        }
        rule.runOnIdle { rule.activity.setContent { NudgeTheme {
            val current = app.configuration.state.collectAsState().value!!.handle(handle.id)!!
            ExclusionsDialog(current, vm) {}
        } } }
        order("Alpha", "Bravo", "Charlie", "Delta")
        assertTrue(row("Remove: test.missing").fetchSemanticsNode().boundsInRoot.top < row("Alpha").fetchSemanticsNode().boundsInRoot.top)
        rule.onAllNodesWithText("Bravo").assertCountEquals(1)
        row("Delta").assertIsOff()
        row("Charlie").performClick()
        order("Charlie", "Alpha", "Bravo", "Delta")
        row("Charlie").assertIsOn()
        rule.onNode(hasSetTextAction()).performTextInput("bravo")
        row("Charlie").assertDoesNotExist()
        row("Remove: test.missing").assertDoesNotExist()
        row("Bravo").performClick()
        rule.runOnIdle { assertEquals(setOf("test.missing", "test.bravo", "test.charlie"), app.configuration.state.value!!.handle(handle.id)!!.trigger.excludedPackages) }
        rule.onNode(hasSetTextAction()).performTextClearance()
        order("Bravo", "Charlie", "Alpha", "Delta")
        row("Bravo").performClick()
        order("Charlie", "Alpha", "Bravo", "Delta")
        row("Bravo").assertIsOff()
        row("Charlie").performClick()
        order("Alpha", "Bravo", "Charlie", "Delta")
        row("Remove: test.missing").performClick()
        rule.runOnIdle {
            val saved = app.configuration.state.value!!
            assertEquals(handle.normalized().copy(trigger = handle.normalized().trigger.copy(excludedPackages = emptySet())), saved.handle(handle.id))
            assertEquals(other.normalized(), saved.handle(other.id))
            assertEquals(vm.launchableApps.value.map { it.label }, listOf("Alpha", "Bravo", "Bravo", "Charlie", "Delta"))
        }
    }
}
