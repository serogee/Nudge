package com.modukey.app.core

interface OverlayHost<T> {
    fun attach(value: T)
    fun update(value: T)
    fun detach()
}

/** Makes repeated settings/window/screen events idempotent. State changes only after host success. */
class SingleOverlay<T>(private val host: OverlayHost<T>) {
    private var current: T? = null

    fun show(value: T) {
        if (current == null) host.attach(value) else if (current != value) host.update(value)
        current = value
    }

    fun hide() {
        if (current != null) host.detach()
        current = null
    }
}
