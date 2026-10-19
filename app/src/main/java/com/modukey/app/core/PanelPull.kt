package com.modukey.app.core

data class PanelPull(val handleId: String, val panelId: String, val distancePx: Float)

object PanelPullPreview {
    fun distance(inward: Float, density: Float): Float = (inward.coerceAtLeast(0f) * .15f).coerceAtMost(12f * density)
    fun target(handle: HandleConfig, preferHold: Boolean, sessions: GestureSessions, now: Long): String? {
        val hold = handle.target(GestureSlot.SWIPE_HOLD)
        return if (preferHold && !hold.isNone) hold.panelId else sessions.next(handle, GestureKind.SWIPE, now).first.panelId
    }
}
