package com.modukey.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataMigration
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
            landscapeOverrideEnabled = p[booleanPreferencesKey("landscape.override")] ?: false,
            lockScreenEnabled = p[booleanPreferencesKey("lockscreen.enabled")] ?: false,
            lengthDp = p[floatPreferencesKey("length")] ?: 120f,
            depthDp = p[floatPreferencesKey("depth")] ?: 16f,
            indicatorVisible = p[booleanPreferencesKey("indicator.visible")] ?: false,
            indicatorLengthFraction = p[floatPreferencesKey("indicator.fraction")] ?: legacyIndicatorFraction(p),
            indicatorThicknessFraction = p[floatPreferencesKey("indicator.thickness.fraction")] ?: legacyThicknessFraction(p),
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
        p[booleanPreferencesKey("landscape.override")] = s.landscapeOverrideEnabled
        p[booleanPreferencesKey("lockscreen.enabled")] = s.lockScreenEnabled
        p[floatPreferencesKey("length")] = s.lengthDp
        p[floatPreferencesKey("depth")] = s.depthDp
        p[booleanPreferencesKey("indicator.visible")] = s.indicatorVisible
        p[floatPreferencesKey("indicator.fraction")] = s.indicatorLengthFraction
        p[floatPreferencesKey("indicator.thickness.fraction")] = s.indicatorThicknessFraction
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

private fun legacyIndicatorFraction(p: Preferences): Float =
    ((p[floatPreferencesKey("indicator.length")] ?: 80f) /
        (p[floatPreferencesKey("length")] ?: 120f)).let {
        if (it.isFinite()) it.coerceIn(0f, 1f) else 2f / 3f
    }

/** Writes the ratio once, leaving every unrelated preference and legacy placement intact. */
class IndicatorFractionMigration : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences) =
        currentData[floatPreferencesKey("indicator.fraction")] == null
    override suspend fun migrate(currentData: Preferences): Preferences = currentData.toMutablePreferences().apply {
        this[floatPreferencesKey("indicator.fraction")] = legacyIndicatorFraction(currentData)
        remove(floatPreferencesKey("indicator.length"))
    }
    override suspend fun cleanUp() = Unit
}

private fun legacyThicknessFraction(p: Preferences): Float {
    // Apply the old independent limits before converting, including corrupt stored values.
    val thickness = (p[floatPreferencesKey("indicator.thickness")] ?: 4f)
        .let { if (it.isFinite()) it else 4f }.coerceIn(1f, 32f)
    val depth = (p[floatPreferencesKey("depth")] ?: 16f)
        .let { if (it.isFinite()) it else 16f }.coerceIn(8f, 32f)
    return (thickness / depth).coerceIn(0f, 1f)
}

/** Preserve the previous rendered proportion, then keep the percentage independent of depth. */
class IndicatorThicknessFractionMigration : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences) =
        currentData[floatPreferencesKey("indicator.thickness.fraction")] == null
    override suspend fun migrate(currentData: Preferences): Preferences = currentData.toMutablePreferences().apply {
        this[floatPreferencesKey("indicator.thickness.fraction")] = legacyThicknessFraction(currentData)
        remove(floatPreferencesKey("indicator.thickness"))
    }
    override suspend fun cleanUp() = Unit
}

private inline fun <reified T : Enum<T>> enumValue(value: String?, default: T): T =
    enumValues<T>().firstOrNull { it.name == value } ?: default
