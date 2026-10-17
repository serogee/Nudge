package com.modukey.app.ui

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.modukey.app.BuildConfig
import com.modukey.app.NudgeApplication
import com.modukey.app.R
import com.modukey.app.core.*
import com.modukey.app.data.ConfigurationCodec
import com.modukey.app.platform.*
import com.modukey.app.updates.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.io.ByteArrayOutputStream
import java.io.IOException

data class LaunchableApp(val label: String, val packageName: String, val activityName: String)
class NudgeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NudgeApplication
    val configuration = app.configuration.state
    val error = app.configuration.error
    val status = app.status.state
    val accessibilityEnabled = MutableStateFlow(false)
    val launchableApps = MutableStateFlow<List<LaunchableApp>>(emptyList())
    val appsLoaded = MutableStateFlow(false)
    val iconPacks = MutableStateFlow<List<IconPackInfo>>(emptyList())
    val importPreview = MutableStateFlow<AppConfiguration?>(null)
    val backupBusy = MutableStateFlow(false)
    val popupCheck = MutableStateFlow(R.string.volume_unchecked)
    private val messages = Channel<Int>(Channel.BUFFERED)
    val notices = messages.receiveAsFlow()
    private var appLoad: Job? = null
    private val updateChecker = UpdateChecker(GitHubReleaseSource(), BuildConfig.VERSION_NAME, viewModelScope)
    val updates = updateChecker.state
    private val permissionObserver = object : ContentObserver(Handler(Looper.getMainLooper())) { override fun onChange(selfChange: Boolean) = refreshPermissions() }
    init {
        app.contentResolver.registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES), false, permissionObserver)
        refreshPermissions()
    }
    fun edit(change: (AppConfiguration) -> AppConfiguration) = app.configuration.update(change)
    fun editHandle(id: String, change: (HandleConfig) -> HandleConfig) = edit { it.editHandle(id, change) }
    fun editTrigger(id: String, change: (TriggerSettings) -> TriggerSettings) = editHandle(id) { it.copy(trigger = change(it.trigger)) }
    fun editPanel(id: String, index: Int, change: (PanelConfig) -> PanelConfig) = editHandle(id) { h -> h.copy(panels = h.panels.mapIndexed { i, p -> if (i == index) change(p) else p }) }
    fun editLayout(id: String, index: Int, landscape: Boolean, change: (PanelLayout) -> PanelLayout) = editPanel(id, index) { p ->
        if (landscape && p.landscapeOverride) p.copy(landscape = change(p.landscape)) else p.copy(portrait = change(p.portrait))
    }
    fun editButton(id: String, panel: Int, button: String, change: (PanelButton) -> PanelButton) = editPanel(id, panel) { p -> p.copy(buttons = p.buttons.map { if (it.id == button) change(it) else it }) }
    fun addHandle(): String {
        val h = HandleConfig(); edit { it.copy(handles = it.handles + h) }; flush(); return h.id
    }
    fun duplicate(id: String) { edit { c -> c.handle(id)?.let { c.copy(handles = c.handles + it.duplicate()) } ?: c }; flush() }
    fun remove(id: String) { edit { it.copy(handles = it.handles.filterNot { h -> h.id == id }) }; flush() }
    fun moveHandle(id: String, delta: Int) = edit { c -> c.copy(handles = moved(c.handles, c.handles.indexOfFirst { it.id == id }, delta)) }
    fun moveButton(id: String, panel: Int, button: String, delta: Int) = editPanel(id, panel) { p -> p.copy(buttons = moved(p.buttons, p.buttons.indexOfFirst { it.id == button }, delta)) }
    private fun <T> moved(items: List<T>, index: Int, delta: Int): List<T> {
        if (index !in items.indices || index + delta !in items.indices) return items
        return items.toMutableList().apply { val value = removeAt(index); add(index + delta, value) }
    }
    fun resetHandle(id: String) { editHandle(id) { HandleConfig(id = it.id, name = it.name, enabled = it.enabled) }; flush() }
    fun flush() = app.configuration.flush()
    fun retrySaving() = app.configuration.retry()
    fun beginEditor(token: Any, id: String, panel: Int?) = app.editor.begin(token, EditorSelection(id, panel))
    fun endEditor(token: Any) = app.editor.end(token)
    fun testPanel(id: String, panel: Int) {
        if (status.value.connected) app.editor.test(PanelTestRequest(id, panel)) else tell(R.string.preview_unavailable)
    }
    fun refreshPermissions() { accessibilityEnabled.value = AccessibilityPermission.enabled(app) }
    fun checkForUpdates() = updateChecker.check(configuration.value?.includePrereleases ?: false)
    fun setIncludePrereleases(value: Boolean) {
        if (configuration.value?.includePrereleases == value) return
        updateChecker.clear(); edit { it.copy(includePrereleases = value) }; flush()
    }
    fun testVolume() { popupCheck.value = if (app.volume.show() == VolumeResult.Requested) R.string.volume_requested else R.string.volume_failed }
    fun confirmPopup(visible: Boolean) { popupCheck.value = if (visible) R.string.volume_confirmed else R.string.volume_not_shown }
    fun tell(resource: Int) { messages.trySend(resource) }
    fun loadApps() {
        if (appLoad?.isActive == true) return
        appLoad = viewModelScope.launch {
            appsLoaded.value = false
            launchableApps.value = withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                app.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), PackageManager.MATCH_ALL)
                    .filter { it.activityInfo.packageName != app.packageName }
                    .distinctBy { it.activityInfo.packageName to it.activityInfo.name }
                    .map { LaunchableApp(it.loadLabel(app.packageManager).toString(), it.activityInfo.packageName, it.activityInfo.name) }.sortedBy { it.label.lowercase() }
            }
            appsLoaded.value = true
        }
    }
    fun loadPacks() { viewModelScope.launch { iconPacks.value = app.icons.packs() } }
    fun export(uri: Uri) {
        val snapshot = configuration.value ?: return
        viewModelScope.launch {
            backupBusy.value = true
            try {
                withContext(Dispatchers.IO) {
                    val bytes = ConfigurationCodec.encode(snapshot).toByteArray(Charsets.UTF_8)
                    if (bytes.size > ConfigurationCodec.MAX_BYTES) throw IOException("backup_size")
                    app.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) } ?: throw IOException()
                }; tell(R.string.backup_saved)
            } catch (_: IOException) { tell(R.string.backup_failed) }
            catch (_: SecurityException) { tell(R.string.backup_failed) }
            finally { backupBusy.value = false }
        }
    }
    fun readImport(uri: Uri) {
        viewModelScope.launch {
            backupBusy.value = true
            try {
                importPreview.value = withContext(Dispatchers.IO) {
                    app.contentResolver.openInputStream(uri)?.use { input ->
                        val bytes = ByteArrayOutputStream(); val buffer = ByteArray(8192)
                        while (true) { val count = input.read(buffer); if (count < 0) break
                            if (bytes.size() + count > ConfigurationCodec.MAX_BYTES) throw IOException("backup_size")
                            bytes.write(buffer, 0, count)
                        }
                        ConfigurationCodec.decode(bytes.toString("UTF-8"))
                    } ?: throw IOException()
                }
            } catch (e: IOException) { tell(when (e.message) { "backup_size" -> R.string.backup_size; "configuration_version" -> R.string.configuration_version; "configuration_invalid" -> R.string.configuration_invalid; else -> R.string.backup_failed }) }
            catch (_: SecurityException) { tell(R.string.backup_failed) }
            finally { backupBusy.value = false }
        }
    }
    fun applyImport(replace: Boolean) {
        val imported = importPreview.value ?: return
        edit { current -> if (replace) imported.copy(enabled = false) else current.copy(handles = current.handles + imported.handles.map { it.duplicate() }) }
        importPreview.value = null; flush(); tell(R.string.backup_imported)
    }
    fun inspectDiagnostics(ready: (String) -> Unit) {
        viewModelScope.launch {
            app.diagnostics.inspectExits()
            ready(app.getString(R.string.diagnostic_details, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE,
                "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}", android.os.Build.VERSION.RELEASE, android.os.Build.DISPLAY,
                accessibilityEnabled.value.toString(), status.value.toString(), app.getString(popupCheck.value), app.diagnostics.details.value))
        }
    }
    override fun onCleared() { app.contentResolver.unregisterContentObserver(permissionObserver); super.onCleared() }
}
