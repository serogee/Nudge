package com.modukey.app.platform

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.modukey.app.core.HandleAvailability

enum class ConnectionState { DISABLED, ENABLED_DISCONNECTED, CONNECTED }
data class ServiceState(val connected: Boolean = false, val triggerAttached: Boolean = false, val error: String? = null,
    val attachedHandleIds: Set<String> = emptySet(), val handles: Map<String, HandleAvailability> = emptyMap(), val panelHandleId: String? = null, val panelId: String? = null) {
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
