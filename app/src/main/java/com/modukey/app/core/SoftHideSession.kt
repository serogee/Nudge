package com.modukey.app.core

/** Transient visibility only. A host schedules one callback at deadline, never a repeating task. */
class SoftHideSession {
    var active = false; private set
    var revealed = false; private set
    var deadline: Long? = null; private set
    private var delayMs = 3_000L
    private var touching = false
    private var panelOpen = false

    fun configure(enabled: Boolean, fullScreen: Boolean?, delayMs: Long, now: Long) {
        val next = enabled && fullScreen == true
        val delayChanged = this.delayMs != delayMs
        this.delayMs = delayMs.coerceIn(1_000, 30_000)
        if (next != active) { active = next; reset() }
        else if (delayChanged) restart(now)
    }

    /** True means reveal only: do not execute an action or advance its sequence. */
    fun gesture(now: Long): Boolean {
        if (!active || revealed) { restart(now); return false }
        revealed = true; restart(now); return true
    }
    fun touch(pressed: Boolean, now: Long) { touching = pressed; restart(now) }
    fun panel(open: Boolean, now: Long) { if (panelOpen != open) { panelOpen = open; restart(now) } }
    fun expire(now: Long) { if (deadline?.let { now >= it } == true) reset() }
    fun reset() { revealed = false; deadline = null }
    private fun restart(now: Long) { deadline = if (active && revealed && !touching && !panelOpen) now + delayMs else null }
}
