package com.modukey.app.platform

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ConnectionState { DISABLED, ENABLED_DISCONNECTED, CONNECTED }
data class ServiceState(val connected: Boolean = false, val triggerAttached: Boolean = false, val error: String? = null) {
    fun connection(enabled: Boolean) = when {
        connected -> ConnectionState.CONNECTED
        enabled -> ConnectionState.ENABLED_DISCONNECTED
        else -> ConnectionState.DISABLED
    }
}
interface ServiceStatus { val state: StateFlow<ServiceState> }

class ServiceStatusStore : ServiceStatus {
    private val mutable = MutableStateFlow(ServiceState())
    override val state = mutable.asStateFlow()
    fun report(state: ServiceState) { mutable.value = state }
}
