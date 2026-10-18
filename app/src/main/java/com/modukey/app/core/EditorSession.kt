package com.modukey.app.core

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

data class EditorSelection(val handleId: String? = null, val panelId: String? = null, val preview: Boolean = true)
data class PanelTestRequest(val panelId: String, val handleId: String? = null)
class EditorSession {
    private var owner: Any? = null
    private val mutable = MutableStateFlow<EditorSelection?>(null)
    val state = mutable.asStateFlow()
    private val requests = MutableSharedFlow<PanelTestRequest>(extraBufferCapacity = 1)
    val tests = requests.asSharedFlow()
    fun begin(token: Any, selection: EditorSelection) { owner = token; mutable.value = selection }
    fun end(token: Any) { if (owner === token) { owner = null; mutable.value = null } }
    fun test(selection: PanelTestRequest) { requests.tryEmit(selection) }
}
