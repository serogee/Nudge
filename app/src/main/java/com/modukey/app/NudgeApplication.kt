package com.modukey.app

import android.app.Application
import android.media.AudioManager
import androidx.datastore.preferences.preferencesDataStore
import com.modukey.app.data.DataStoreSettingsRepository
import com.modukey.app.data.SettingsController
import com.modukey.app.core.CustomizationSession
import com.modukey.app.data.IndicatorFractionMigration
import com.modukey.app.data.IndicatorThicknessFractionMigration
import com.modukey.app.core.EditorSession
import com.modukey.app.data.ConfigurationController
import com.modukey.app.data.DataStoreConfigurationRepository
import com.modukey.app.platform.IconRepository
import com.modukey.app.platform.UnlockRequests
import com.modukey.app.platform.LocalDiagnostics
import com.modukey.app.platform.NativeVolumeController
import com.modukey.app.platform.ServiceStatusStore
import kotlinx.coroutines.*

private val Application.preferences by preferencesDataStore("trigger_settings", produceMigrations = {
    listOf(IndicatorFractionMigration(), IndicatorThicknessFractionMigration())
})

class NudgeApplication : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val settings by lazy { SettingsController(DataStoreSettingsRepository(preferences), scope) }
    val customization = CustomizationSession()
    val configuration by lazy { ConfigurationController(DataStoreConfigurationRepository(preferences), scope) }
    val editor = EditorSession()
    val icons by lazy { IconRepository(this) }
    val unlocks = UnlockRequests()
    val diagnostics by lazy { LocalDiagnostics(this, scope) }
    val status = ServiceStatusStore()
    val volume by lazy { NativeVolumeController(getSystemService(AudioManager::class.java)) }

    override fun onCreate() {
        super.onCreate()
        diagnostics.record("Application started")
        scope.launch { diagnostics.inspectExits() }
    }

    override fun onTerminate() {
        // Used by emulated application lifecycles; Android ends a real process without this callback.
        scope.cancel()
        super.onTerminate()
    }
}
