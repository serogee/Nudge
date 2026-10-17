package com.modukey.app.ui

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityManager
import android.accessibilityservice.AccessibilityServiceInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.modukey.app.BuildConfig
import com.modukey.app.NudgeApplication
import com.modukey.app.core.TriggerSettings
import com.modukey.app.platform.EdgeAccessibilityService
import com.modukey.app.platform.VolumeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LaunchableApp(val label: String, val packageName: String)

class NudgeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NudgeApplication
    val error = MutableStateFlow<String?>(null)
    val settings = app.settings.settings.map<TriggerSettings, TriggerSettings?> { it }
        .catch { error.value = "Saved settings could not be read. Close Nudge and retry; your settings have not been reset." }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val status = app.status.state
    val accessibilityEnabled = MutableStateFlow(false)
    val launchableApps = MutableStateFlow<List<LaunchableApp>>(emptyList())
    val appsLoaded = MutableStateFlow(false)
    val popupCheck = MutableStateFlow("Not checked this session")

    fun update(transform: (TriggerSettings) -> TriggerSettings) {
        viewModelScope.launch {
            try { app.settings.update(transform); error.value = null }
            catch (_: java.io.IOException) { error.value = "Could not save this change. Check available storage and try again." }
        }
    }

    fun reset() {
        viewModelScope.launch {
            try { app.settings.reset() } catch (_: java.io.IOException) { error.value = "Could not reset settings. Try again." }
        }
    }

    fun refreshPermissions() {
        val expected = ComponentName(app, EdgeAccessibilityService::class.java)
        accessibilityEnabled.value = app.getSystemService(AccessibilityManager::class.java)
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { ComponentName(it.resolveInfo.serviceInfo.packageName, it.resolveInfo.serviceInfo.name) == expected }
    }

    fun highlight(): Boolean {
        if (!status.value.triggerAttached) return false
        app.status.highlightUntil.value = android.os.SystemClock.elapsedRealtime() + 10_000
        return true
    }

    fun testVolume(): String = when (val result = app.volume.show()) {
        VolumeResult.Requested -> {
            popupCheck.value = "Request sent; awaiting your visual confirmation"
            "Volume request sent. Confirm below whether the native popup appeared."
        }
        is VolumeResult.Failed -> { popupCheck.value = "Request failed: ${result.message}"; result.message }
    }

    fun confirmPopup(visible: Boolean) {
        popupCheck.value = if (visible) "Native popup visible (user-confirmed this session)" else "Popup did not appear: compatibility check failed"
    }

    fun loadApps() {
        if (appsLoaded.value) return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                @Suppress("DEPRECATION")
                app.packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
                    .filter { it.activityInfo.packageName != app.packageName }
                    .distinctBy { it.activityInfo.packageName }
                    .map { LaunchableApp(it.loadLabel(app.packageManager).toString(), it.activityInfo.packageName) }
                    .sortedBy { it.label.lowercase() }
            }
            launchableApps.value = result
            appsLoaded.value = true
        }
    }

    fun diagnostics(): String = """
        Nudge ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})
        Model: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}
        Device: ${android.os.Build.DEVICE}
        Android: ${android.os.Build.VERSION.RELEASE} / API ${android.os.Build.VERSION.SDK_INT}
        Build: ${android.os.Build.DISPLAY}
        HyperOS version / region: copy from Settings → About phone
        4G/5G variant / navigation mode: record manually
        Accessibility enabled: ${accessibilityEnabled.value}
        Service connected: ${status.value.connected}
        Trigger attached: ${status.value.triggerAttached}
        Native popup: ${popupCheck.value}
        Service error: ${status.value.error ?: "none"}
    """.trimIndent()
}
