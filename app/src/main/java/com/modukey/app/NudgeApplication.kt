package com.modukey.app

import android.app.Application
import android.media.AudioManager
import androidx.datastore.preferences.preferencesDataStore
import com.modukey.app.data.DataStoreSettingsRepository
import com.modukey.app.data.IndicatorFractionMigration
import com.modukey.app.data.SettingsController
import com.modukey.app.core.CustomizationSession
import com.modukey.app.platform.LocalDiagnostics
import com.modukey.app.platform.NativeVolumeController
import com.modukey.app.platform.ServiceStatusStore
import kotlinx.coroutines.*

private val Application.preferences by preferencesDataStore("trigger_settings", produceMigrations = { listOf(IndicatorFractionMigration()) })

class NudgeApplication : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val settings by lazy { SettingsController(DataStoreSettingsRepository(preferences), scope) }
    val customization = CustomizationSession()
    val diagnostics by lazy { LocalDiagnostics(this, scope) }
    val status = ServiceStatusStore()
    val volume by lazy { NativeVolumeController(getSystemService(AudioManager::class.java)) }

    override fun onCreate() {
        super.onCreate()
        diagnostics.record("Application started")
        scope.launch { diagnostics.inspectExits() }
    }
}
