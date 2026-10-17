package com.modukey.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.modukey.app.core.AppConfiguration
import kotlinx.coroutines.flow.*

interface ConfigurationRepository {
    val configuration: Flow<AppConfiguration>
    suspend fun write(value: AppConfiguration)
}
class DataStoreConfigurationRepository(private val store: DataStore<Preferences>) : ConfigurationRepository {
    private val key = stringPreferencesKey("configuration.v1")
    override val configuration = store.data.onStart {
        // One atomic update. Legacy and unknown keys remain intact; subsequent starts use the document.
        store.edit { p -> if (p[key] == null) p[key] = ConfigurationCodec.encode(AppConfiguration.fromLegacy(DataStoreSettingsRepository(store).decode(p))) }
    }.map { ConfigurationCodec.decode(checkNotNull(it[key])) }
    override suspend fun write(value: AppConfiguration) {
        val encoded = ConfigurationCodec.encode(value.normalized())
        store.edit { it[key] = encoded }
    }
}
