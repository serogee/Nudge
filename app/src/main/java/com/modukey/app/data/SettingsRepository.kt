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

    // A customization reset preserves the master switch and the unrelated update preference.
    override suspend fun reset() = update { TriggerSettings(enabled = it.enabled, includePrereleases = it.includePrereleases) }

    internal fun decode(p: Preferences): TriggerSettings {
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
            indicatorThicknessDp = p[floatPreferencesKey("indicator.thickness.dp")] ?: legacyThicknessDp(p),
            indicatorColor = p[longPreferencesKey("indicator.color")] ?: defaults.indicatorColor,
            indicatorOpacity = p[floatPreferencesKey("indicator.opacity")] ?: 0.65f,
            cornerRadiusDp = p[floatPreferencesKey("indicator.radius")] ?: 2f,
            activation = enumValue(p[stringPreferencesKey("activation")], Activation.SWIPE),
            swipeDistanceDp = p[floatPreferencesKey("swipe.distance")] ?: 24f,
            swipeTimeoutMs = p[longPreferencesKey("swipe.timeout")] ?: 600L,
            haptics = p[booleanPreferencesKey("haptics")] ?: false,
            excludedPackages = p[stringSetPreferencesKey("excluded.packages")] ?: emptySet(),
            includePrereleases = p[booleanPreferencesKey("updates.include.prereleases")] ?: false,
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
        p[floatPreferencesKey("indicator.thickness.dp")] = s.indicatorThicknessDp
        p.remove(floatPreferencesKey("indicator.thickness.fraction"))
        p.remove(floatPreferencesKey("indicator.thickness"))
        p[longPreferencesKey("indicator.color")] = s.indicatorColor
        p[floatPreferencesKey("indicator.opacity")] = s.indicatorOpacity
        p[floatPreferencesKey("indicator.radius")] = s.cornerRadiusDp
        p[stringPreferencesKey("activation")] = s.activation.name
        p[floatPreferencesKey("swipe.distance")] = s.swipeDistanceDp
        p[longPreferencesKey("swipe.timeout")] = s.swipeTimeoutMs
        p[booleanPreferencesKey("haptics")] = s.haptics
        p[stringSetPreferencesKey("excluded.packages")] = s.excludedPackages
        p[booleanPreferencesKey("updates.include.prereleases")] = s.includePrereleases
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

internal fun thicknessDpFromFraction(fraction: Float, depth: Float): Float =
    (if (fraction.isFinite()) fraction else .25f).coerceIn(0f, 1f) *
        (if (depth.isFinite()) depth else 16f).coerceIn(8f, 32f)

private fun legacyThicknessDp(p: Preferences): Float {
    val depth = (p[floatPreferencesKey("depth")] ?: 16f)
        .let { if (it.isFinite()) it else 16f }.coerceIn(8f, 32f)
    p[floatPreferencesKey("indicator.thickness.fraction")]?.let { return thicknessDpFromFraction(it, depth) }
    // Apply the old independent limits before converting, including corrupt stored values.
    val thickness = (p[floatPreferencesKey("indicator.thickness")] ?: 4f)
        .let { if (it.isFinite()) it else 4f }.coerceIn(1f, 32f)
    return minOf(thickness, depth)
}

/** Preserve the visible thickness once, then keep its dp value independent of touch depth. */
class IndicatorThicknessDpMigration : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences) =
        currentData[floatPreferencesKey("indicator.thickness.dp")] == null
    override suspend fun migrate(currentData: Preferences): Preferences = currentData.toMutablePreferences().apply {
        this[floatPreferencesKey("indicator.thickness.dp")] = legacyThicknessDp(currentData)
        remove(floatPreferencesKey("indicator.thickness.fraction"))
        remove(floatPreferencesKey("indicator.thickness"))
    }
    override suspend fun cleanUp() = Unit
}

private inline fun <reified T : Enum<T>> enumValue(value: String?, default: T): T =
    enumValues<T>().firstOrNull { it.name == value } ?: default
