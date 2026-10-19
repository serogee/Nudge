package com.modukey.app.core

/** Main-thread commands exist only while accessibility is connected. Requests are never queued. */
class ShortcutRuntime {
    private var owner: Any? = null
    private var execute: ((String) -> Boolean)? = null
    fun bind(token: Any, action: (String) -> Boolean) { owner = token; execute = action }
    fun unbind(token: Any) { if (owner === token) { owner = null; execute = null } }
    fun run(id: String): Boolean = execute?.invoke(id) ?: false
}
