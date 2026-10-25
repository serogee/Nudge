package com.modukey.app.core

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

data class EditorSelection(val handleId: String? = null, val panelId: String? = null, val preview: Boolean = true, val landscape: Boolean? = null) {
    fun matches(handleId: String?, panelId: String?) = this.handleId == handleId && this.panelId == panelId
}
data class PanelTestRequest(val panelId: String, val handleId: String? = null)
data class WheelHitboxPreview(val panelId: String, val landscape: Boolean, val generation: Long)
class EditorSession {
    private var owner: Any? = null
    private val mutable = MutableStateFlow<EditorSelection?>(null)
    val state = mutable.asStateFlow()
    private val requests = MutableSharedFlow<PanelTestRequest>(extraBufferCapacity = 1)
    val tests = requests.asSharedFlow()
    private val hitbox = MutableStateFlow<WheelHitboxPreview?>(null)
    val hitboxPreview = hitbox.asStateFlow()
    private var generation = 0L
    fun begin(token: Any, selection: EditorSelection) {
        if (owner !== token || mutable.value != selection) clearHitbox()
        owner = token; mutable.value = selection
    }
    fun end(token: Any) { if (owner === token) { owner = null; mutable.value = null; clearHitbox() } }
    fun showHitbox(panelId: String, landscape: Boolean): WheelHitboxPreview? {
        val selected = mutable.value ?: return null
        if (selected.panelId != panelId || (selected.landscape ?: false) != landscape) return null
        return WheelHitboxPreview(panelId, landscape, ++generation).also { hitbox.value = it }
    }
    fun clearHitbox(request: WheelHitboxPreview? = null) { if (request == null || hitbox.value == request) hitbox.value = null }
    fun test(selection: PanelTestRequest) { requests.tryEmit(selection) }
}
