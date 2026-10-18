package com.modukey.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.modukey.app.core.AppConfiguration
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ConfigurationRepository {
    val configuration: Flow<AppConfiguration>
    suspend fun write(value: AppConfiguration)
}
class DataStoreConfigurationRepository(private val store: DataStore<Preferences>, private val migratedName: MigratedPanelName? = null) : ConfigurationRepository {
    private val key = stringPreferencesKey("configuration.v1")
    override val configuration = store.data.onStart {
        // One atomic update. Legacy and unknown keys remain intact; subsequent starts use the document.
        withContext(Dispatchers.IO) {
            store.edit { p ->
                val raw = p[key]
                p[key] = if (raw == null) ConfigurationCodec.encode(AppConfiguration.fromLegacy(DataStoreSettingsRepository(store).decode(p)))
                    else if (migratedName == null) ConfigurationCodec.upgrade(raw) else ConfigurationCodec.upgrade(raw, migratedName)
            }
        }
    }.map { preferences -> withContext(Dispatchers.Default) { ConfigurationCodec.decode(checkNotNull(preferences[key]), checkBackupSize = false) } }
    override suspend fun write(value: AppConfiguration) {
        val encoded = withContext(Dispatchers.Default) { ConfigurationCodec.encode(value.normalized()) }
        withContext(Dispatchers.IO) { store.edit { it[key] = encoded } }
    }
}
