package com.modukey.app.core

/** Pinned launchers contain only this ID. Actions remain inside validated app configuration. */
data class HomeShortcut(val id: String = newId(), val name: String = "", val button: PanelButton = PanelButton(), val handleId: String? = null) {
    fun available(config: AppConfiguration) = button.action != ButtonAction.EMPTY &&
        (!button.needsHandleOwner || config.handle(handleId) != null)
}
