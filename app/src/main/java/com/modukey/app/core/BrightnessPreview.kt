package com.modukey.app.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A departing activity must not clear a newer activity's preview. */
class BrightnessPreview {
    private var owner: Any? = null
    private val mutable = MutableStateFlow(false)
    val state = mutable.asStateFlow()
    fun begin(token: Any) { owner = token; mutable.value = true }
    fun end(token: Any) { if (owner === token) { owner = null; mutable.value = false } }
}
