package com.modukey.app.platform

import android.content.ComponentName
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AccessibilityPermissionTest {
    @Test fun `enabled component membership is independent of live service connection`() {
        val own = ComponentName("com.modukey.app", "com.modukey.app.platform.EdgeAccessibilityService")
        val enabled = "other.app/.Service:com.modukey.app/.platform.EdgeAccessibilityService"
        assertTrue(AccessibilityPermission.contains(enabled, own))
        assertFalse(AccessibilityPermission.contains(enabled, ComponentName("com.modukey.app.debug", own.className)))
        assertFalse(AccessibilityPermission.contains(null, own))
        assertFalse(AccessibilityPermission.contains("other.app/.Service", own))
        assertEquals(ConnectionState.DISABLED, ServiceState().connection(false))
        assertEquals(ConnectionState.ENABLED_DISCONNECTED, ServiceState().connection(true))
        assertEquals(ConnectionState.CONNECTED, ServiceState(connected = true).connection(true))
    }
}
