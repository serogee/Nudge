package com.modukey.app.core

interface OverlayHost<T> {
    fun attach(value: T)
    fun update(value: T)
    fun detach()
    fun raise(value: T) { detach(); attach(value) }
}

/** Makes repeated events idempotent. Forget ownership before removal, even if the host fails. */
class SingleOverlay<T>(private val host: OverlayHost<T>) {
    private var current: T? = null

    fun show(value: T, raise: Boolean = false) {
        if (current == null) host.attach(value) else if (raise) host.raise(value) else if (current != value) host.update(value)
        current = value
    }

    fun hide() {
        val attached = current != null
        current = null
        if (attached) host.detach()
    }
}
