package com.modukey.app.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A stale activity disposal must not end its replacement activity's session. */
class CustomizationSession {
    private var owner: Any? = null
    private val mutable = MutableStateFlow(false)
    val active = mutable.asStateFlow()
    fun begin(token: Any) { owner = token; mutable.value = true }
    fun end(token: Any) { if (owner === token) { owner = null; mutable.value = false } }
}
