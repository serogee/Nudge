package com.modukey.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.modukey.app.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SettingsRepository {
    val settings: Flow<TriggerSettings>
    suspend fun update(transform: (TriggerSettings) -> TriggerSettings)
    suspend fun reset()
}

class DataStoreSettingsRepository(private val store: DataStore<Preferences>) : SettingsRepository {
    override val settings = store.data.map(::decode)

    override suspend fun update(transform: (TriggerSettings) -> TriggerSettings) {
        store.edit { prefs -> encode(prefs, transform(decode(prefs)).normalized()) }
    }

    // Preserve the master switch: reset appearance/behavior must not silently enable the trigger.
    override suspend fun reset() = update { TriggerSettings(enabled = it.enabled) }

    private fun decode(p: Preferences): TriggerSettings {
        val defaults = TriggerSettings()
        return TriggerSettings(
            enabled = p[booleanPreferencesKey("enabled")] ?: defaults.enabled,
            portrait = Placement(enumValue(p[stringPreferencesKey("portrait.edge")], Edge.RIGHT), p[floatPreferencesKey("portrait.position")] ?: 0.65f),
            landscape = Placement(enumValue(p[stringPreferencesKey("landscape.edge")], Edge.RIGHT), p[floatPreferencesKey("landscape.position")] ?: 0.65f),
            landscapeEnabled = p[booleanPreferencesKey("landscape.enabled")] ?: false,
            lengthDp = p[floatPreferencesKey("length")] ?: 120f,
            depthDp = p[floatPreferencesKey("depth")] ?: 16f,
            indicatorVisible = p[booleanPreferencesKey("indicator.visible")] ?: false,
            indicatorLengthDp = p[floatPreferencesKey("indicator.length")] ?: 80f,
            indicatorThicknessDp = p[floatPreferencesKey("indicator.thickness")] ?: 4f,
            indicatorColor = p[longPreferencesKey("indicator.color")] ?: defaults.indicatorColor,
            indicatorOpacity = p[floatPreferencesKey("indicator.opacity")] ?: 0.65f,
            cornerRadiusDp = p[floatPreferencesKey("indicator.radius")] ?: 2f,
            activation = enumValue(p[stringPreferencesKey("activation")], Activation.SWIPE),
            swipeDistanceDp = p[floatPreferencesKey("swipe.distance")] ?: 24f,
            swipeTimeoutMs = p[longPreferencesKey("swipe.timeout")] ?: 600L,
            haptics = p[booleanPreferencesKey("haptics")] ?: false,
            excludedPackages = p[stringSetPreferencesKey("excluded.packages")] ?: emptySet(),
        ).normalized()
    }

    private fun encode(p: MutablePreferences, s: TriggerSettings) {
        p[booleanPreferencesKey("enabled")] = s.enabled
        p[stringPreferencesKey("portrait.edge")] = s.portrait.edge.name
        p[floatPreferencesKey("portrait.position")] = s.portrait.position
        p[stringPreferencesKey("landscape.edge")] = s.landscape.edge.name
        p[floatPreferencesKey("landscape.position")] = s.landscape.position
        p[booleanPreferencesKey("landscape.enabled")] = s.landscapeEnabled
        p[floatPreferencesKey("length")] = s.lengthDp
        p[floatPreferencesKey("depth")] = s.depthDp
        p[booleanPreferencesKey("indicator.visible")] = s.indicatorVisible
        p[floatPreferencesKey("indicator.length")] = s.indicatorLengthDp
        p[floatPreferencesKey("indicator.thickness")] = s.indicatorThicknessDp
        p[longPreferencesKey("indicator.color")] = s.indicatorColor
        p[floatPreferencesKey("indicator.opacity")] = s.indicatorOpacity
        p[floatPreferencesKey("indicator.radius")] = s.cornerRadiusDp
        p[stringPreferencesKey("activation")] = s.activation.name
        p[floatPreferencesKey("swipe.distance")] = s.swipeDistanceDp
        p[longPreferencesKey("swipe.timeout")] = s.swipeTimeoutMs
        p[booleanPreferencesKey("haptics")] = s.haptics
        p[stringSetPreferencesKey("excluded.packages")] = s.excludedPackages
    }
}

private inline fun <reified T : Enum<T>> enumValue(value: String?, default: T): T =
    enumValues<T>().firstOrNull { it.name == value } ?: default
