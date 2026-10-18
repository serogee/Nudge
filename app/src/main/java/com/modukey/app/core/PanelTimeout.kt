package com.modukey.app.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** One cancellable delay for the open interactive panel. No task exists while off or held. */
class PanelTimeout(private val scope: CoroutineScope, private val expire: () -> Unit) {
    private var duration = 0L
    private var held = false
    private var pending: Job? = null

    fun configure(durationMs: Long) {
        val next = durationMs.coerceAtLeast(0)
        if (duration == next) return
        duration = next
        restart()
    }
    fun touch(pressed: Boolean) {
        if (held == pressed) return
        held = pressed
        restart()
    }
    fun restart() {
        pending?.cancel(); pending = null
        if (duration == 0L || held) return
        pending = scope.launch {
            delay(duration)
            pending = null; duration = 0
            expire()
        }
    }
    fun stop() {
        pending?.cancel(); pending = null
        duration = 0; held = false
    }
}
