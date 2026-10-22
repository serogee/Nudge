package com.modukey.app.ui

import androidx.compose.runtime.Composable
import com.modukey.app.R
import com.modukey.app.core.*

@Composable fun HandleActionControls(button: PanelButton, change: (PanelButton) -> Unit) {
    if (button.action !in listOf(ButtonAction.TOGGLE_HANDLES, ButtonAction.TOGGLE_HANDLE_VISIBILITY, ButtonAction.TOGGLE_AND_HIDE_HANDLES)) return
    DependentSettings(t(label(button.action))) {
        when (button.action) {
            ButtonAction.TOGGLE_HANDLES, ButtonAction.TOGGLE_HANDLE_VISIBILITY -> {
                OptionRow(t(R.string.handle_toggle_scope), HandleToggleScope.entries, button.handleScope, HandleToggleScope.ALL) { change(button.copy(handleScope = it)) }
                Help(if (button.action == ButtonAction.TOGGLE_HANDLES) R.string.handle_toggle_help else R.string.handle_visibility_scope_help)
            }
            ButtonAction.TOGGLE_AND_HIDE_HANDLES -> {
                OptionRow(t(R.string.handles_to_toggle), HandleGroupScope.entries, button.toggleScope, HandleGroupScope.ALL) { change(button.copy(toggleScope = it)) }
                OptionRow(t(R.string.handles_to_hide), HandleGroupScope.entries, button.visibilityScope, HandleGroupScope.ALL) { change(button.copy(visibilityScope = it)) }
                Help(R.string.toggle_and_hide_help)
                if (!button.hasHandleGroups) Help(R.string.handle_group_required)
            }
            else -> Unit
        }
    }
}
