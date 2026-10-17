package com.modukey.app

import android.app.Application
import android.media.AudioManager
import androidx.datastore.preferences.preferencesDataStore
import com.modukey.app.data.DataStoreSettingsRepository
import com.modukey.app.platform.NativeVolumeController
import com.modukey.app.platform.ServiceStatusStore

private val Application.preferences by preferencesDataStore("trigger_settings")

class NudgeApplication : Application() {
    val settings by lazy { DataStoreSettingsRepository(preferences) }
    val status = ServiceStatusStore()
    val volume by lazy { NativeVolumeController(getSystemService(AudioManager::class.java)) }
}
