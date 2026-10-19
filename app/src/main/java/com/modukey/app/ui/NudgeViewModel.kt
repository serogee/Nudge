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
    val configuration = app.configuration.preview
    val draft = app.configuration.draft
    val error = app.configuration.error
    val status = app.status.state
    val pausedHandles = app.handlePauses.state
    fun resumeHandles() = app.handlePauses.clear()
    val accessibilityEnabled = MutableStateFlow(false)
    val policyAccess = MutableStateFlow(false)
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
    private val downloadBackend = AndroidApkDownloadBackend(app)
    private val downloadController = ApkDownloadController(downloadBackend, viewModelScope)
    val apkDownload = downloadController.state
    val preparingUpdate = MutableStateFlow(false)
    val savingEditor = MutableStateFlow(false)
    private val permissionObserver = object : ContentObserver(Handler(Looper.getMainLooper())) { override fun onChange(selfChange: Boolean) = refreshPermissions() }
    init {
        app.contentResolver.registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES), false, permissionObserver)
        refreshPermissions()
    }
    fun edit(change: (AppConfiguration) -> AppConfiguration) = app.configuration.update(change)
    fun editHandle(id: String, change: (HandleConfig) -> HandleConfig) = app.configuration.editHandle(id, change)
    fun editTrigger(id: String, change: (TriggerSettings) -> TriggerSettings) = editHandle(id) { it.copy(trigger = change(it.trigger)) }
    fun editSavedPanel(id: String, change: (SavedPanel) -> SavedPanel) = app.configuration.editPanel(id, change)
    fun editPanel(id: String, change: (PanelConfig) -> PanelConfig) = editSavedPanel(id) { it.copy(settings = change(it.settings)) }
    fun editLayout(id: String, landscape: Boolean, change: (PanelLayout) -> PanelLayout) = editPanel(id) { p ->
        if (landscape && p.landscapeOverride) p.copy(landscape = change(p.landscape)) else p.copy(portrait = change(p.portrait))
    }
    fun addPanel(): String {
        val p = SavedPanel(); edit { it.copy(panels = it.panels + p) }; flush(); return p.id
    }
    fun removePanel(id: String) { edit { it.removePanel(id) }; flush() }
    fun duplicatePanel(id: String) { edit { c -> c.panel(id)?.let { c.copy(panels = c.panels + it.duplicate()) } ?: c }; flush() }
    fun reorderPanel(id: String, target: Int) = edit { c -> c.copy(panels = moved(c.panels, c.panels.indexOfFirst { it.id == id }, target)) }
    fun addHandle(): String {
        val h = HandleConfig(); edit { it.copy(handles = it.handles + h) }; flush(); return h.id
    }
    fun duplicate(id: String) { edit { c -> c.handle(id)?.let { c.copy(handles = c.handles + it.duplicate()) } ?: c }; flush() }
    fun remove(id: String) { edit { it.copy(handles = it.handles.filterNot { h -> h.id == id }) }; flush() }
    fun reorderHandle(id: String, target: Int) = edit { c -> c.copy(handles = moved(c.handles, c.handles.indexOfFirst { it.id == id }, target)) }
    fun reorderButton(id: String, button: String, target: Int) = editPanel(id) { p -> p.copy(buttons = moved(p.buttons, p.buttons.indexOfFirst { it.id == button }, target)) }
    private fun <T> moved(items: List<T>, index: Int, target: Int): List<T> {
        if (index !in items.indices || target !in items.indices || index == target) return items
        return items.toMutableList().apply { val value = removeAt(index); add(target, value) }
    }
    fun resetHandle(id: String) { editHandle(id) { HandleConfig(id = it.id, name = it.name, enabled = it.enabled) }; flush() }
    fun flush() = app.configuration.flush()
    fun retrySaving() = app.configuration.retry()
    fun saveEditing() { saveEditingThen {} }
    fun saveEditingThen(saved: () -> Unit) {
        if (savingEditor.value || draft.value == null) return
        savingEditor.value = true
        app.configuration.saveDraft { success -> viewModelScope.launch {
            savingEditor.value = false
            android.widget.Toast.makeText(app, if (success) R.string.changes_saved else R.string.save_failed, android.widget.Toast.LENGTH_SHORT).show()
            if (success) saved()
        } }
    }
    fun cancelEditing() {
        if (savingEditor.value) return
        app.configuration.cancelDraft()
        android.widget.Toast.makeText(app, R.string.changes_cancelled, android.widget.Toast.LENGTH_SHORT).show()
    }
    fun closeEditing() = app.configuration.closeDraft()
    fun beginEditor(token: Any, handleId: String? = null, panelId: String? = null, preview: Boolean = true, landscape: Boolean? = null) {
        if (panelId != null) app.configuration.openPanel(panelId) else if (handleId != null) app.configuration.openHandle(handleId)
        app.editor.begin(token, EditorSelection(handleId, panelId, preview, landscape))
    }
    fun endEditor(token: Any) = app.editor.end(token)
    fun testPanel(id: String, previewHandleId: String?) {
        if (status.value.connected) app.editor.test(PanelTestRequest(id, previewHandleId)) else tell(R.string.preview_unavailable)
    }
    fun refreshPermissions() {
        accessibilityEnabled.value = AccessibilityPermission.enabled(app)
        policyAccess.value = app.getSystemService(android.app.NotificationManager::class.java).isNotificationPolicyAccessGranted
    }
    fun checkForUpdates() = updateChecker.check(configuration.value?.includePrereleases ?: false)
    fun downloadApk(release: PublishedRelease) { release.apkUrl?.let { downloadController.download(ApkTarget(release.tag, it)) } }
    fun retryApkDownload() = downloadController.retry()
    fun cancelApkDownload() { downloadController.cancel() }
    suspend fun observeApkDownload() = downloadController.observe()
    fun installUpdate(expectedUrl: String, launch: (Intent) -> Unit) {
        if (preparingUpdate.value) return
        preparingUpdate.value = true
        viewModelScope.launch {
            try {
                val record = downloadController.prepareInstall() ?: return@launch
                if (record.target.url != expectedUrl) { tell(R.string.apk_download_changed); return@launch }
                val intent = withContext(Dispatchers.IO) { downloadBackend.installIntent(record) }
                launch(intent)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { tell(R.string.apk_install_failed) }
            finally { preparingUpdate.value = false }
        }
    }
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
        val snapshot = app.configuration.state.value ?: return
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
                        ConfigurationCodec.decode(bytes.toString("UTF-8"), migratedName = app::migratedPanelName)
                    } ?: throw IOException()
                }
            } catch (e: IOException) { tell(when (e.message) { "backup_size" -> R.string.backup_size; "configuration_version" -> R.string.configuration_version; "configuration_invalid" -> R.string.configuration_invalid; else -> R.string.backup_failed }) }
            catch (_: SecurityException) { tell(R.string.backup_failed) }
            finally { backupBusy.value = false }
        }
    }
    fun applyImport(replace: Boolean) {
        val imported = importPreview.value ?: return
        edit { current -> if (replace) imported.copy(enabled = false) else current.addCopies(imported) }
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
    override fun onCleared() { closeEditing(); app.contentResolver.unregisterContentObserver(permissionObserver); super.onCleared() }
}
