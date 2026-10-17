package com.modukey.app.ui

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.modukey.app.BuildConfig
import com.modukey.app.NudgeApplication
import com.modukey.app.core.TriggerSettings
import com.modukey.app.platform.AccessibilityPermission
import com.modukey.app.platform.VolumeResult
import com.modukey.app.updates.GitHubReleaseSource
import com.modukey.app.updates.UpdateChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LaunchableApp(val label: String, val packageName: String)

class NudgeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NudgeApplication
    val error = app.settings.error
    val settings = app.settings.state
    val status = app.status.state
    val accessibilityEnabled = MutableStateFlow(false)
    val launchableApps = MutableStateFlow<List<LaunchableApp>>(emptyList())
    val appsLoaded = MutableStateFlow(false)
    val popupCheck = MutableStateFlow("Not checked this session")
    private val updateChecker = UpdateChecker(GitHubReleaseSource(), BuildConfig.VERSION_NAME, viewModelScope)
    val updates = updateChecker.state
    private val permissionObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) { refreshPermissions() }
    }

    init {
        app.contentResolver.registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES), false, permissionObserver)
        refreshPermissions()
    }

    fun update(transform: (TriggerSettings) -> TriggerSettings) {
        app.settings.update(transform)
    }

    fun reset() {
        app.settings.reset()
    }

    fun flush() = app.settings.flush()
    fun retrySaving() = app.settings.retry()
    fun beginCustomization(token: Any) = app.customization.begin(token)
    fun endCustomization(token: Any) = app.customization.end(token)

    fun refreshPermissions() {
        accessibilityEnabled.value = AccessibilityPermission.enabled(app)
    }

    fun checkForUpdates() = updateChecker.check(settings.value?.includePrereleases ?: false)

    fun setIncludePrereleases(value: Boolean) {
        if (settings.value?.includePrereleases == value) return
        updateChecker.clear()
        app.settings.update { it.copy(includePrereleases = value) }
        app.settings.flush()
    }

    override fun onCleared() {
        app.contentResolver.unregisterContentObserver(permissionObserver)
        super.onCleared()
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

    fun inspectDiagnostics(ready: (String) -> Unit) {
        viewModelScope.launch { app.diagnostics.inspectExits(); ready(diagnostics()) }
    }

    private fun diagnostics(): String = """
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
    """.trimIndent() + "\n\nRecent local lifecycle and process-exit records:\n" + app.diagnostics.details.value
}
