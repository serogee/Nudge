package com.modukey.app.core

/** A burst requests one frame. Rendering never schedules the next frame while idle. */
class FrameCoalescer(private val schedule: () -> Unit, private val cancel: () -> Unit, private val refresh: () -> Unit) {
    private var pending = false
    fun request() { if (!pending) { pending = true; schedule() } }
    fun onFrame() { if (pending) { pending = false; refresh() } }
    fun stop() { if (pending) { pending = false; cancel() } }
}
