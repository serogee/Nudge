package com.modukey.app.platform

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ServiceState(val connected: Boolean = false, val triggerAttached: Boolean = false, val error: String? = null)
interface ServiceStatus { val state: StateFlow<ServiceState> }

class ServiceStatusStore : ServiceStatus {
    private val mutable = MutableStateFlow(ServiceState())
    override val state = mutable.asStateFlow()
    fun report(state: ServiceState) { mutable.value = state }
    // Process-local monotonic time: a highlight never survives a process restart or reboot.
    val highlightUntil = MutableStateFlow(0L)
}
