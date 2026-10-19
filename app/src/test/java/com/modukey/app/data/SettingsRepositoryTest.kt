package com.modukey.app.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.*
import androidx.datastore.core.okio.OkioStorage
import com.modukey.app.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import okio.Path.Companion.toPath
import okio.FileSystem

class SettingsRepositoryTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun `settings survive store reopen and partial edits preserve exclusions`() = runTest {
        val file = folder.newFile("settings.preferences_pb")
        suspend fun session(block: suspend (DataStoreSettingsRepository) -> Unit) {
            val job = SupervisorJob()
            // Android's default FileStorage uses renameTo, which cannot replace a file on
            // Windows. Okio uses a portable atomic move with the same preference serializer.
            val storage = OkioStorage(FileSystem.SYSTEM, PreferencesSerializer) { file.absolutePath.toPath() }
            val repository = DataStoreSettingsRepository(PreferenceDataStoreFactory.create(storage = storage, scope = CoroutineScope(Dispatchers.IO + job)))
            try { block(repository) } finally { job.cancelAndJoin() }
        }
        session { repository ->
            assertFalse(repository.settings.first().enabled)
            assertFalse(repository.settings.first().includePrereleases)
            repository.update { it.copy(enabled = true, portrait = Placement(Edge.BOTTOM, 0.2f), excludedPackages = setOf("sample.app"), includePrereleases = true) }
            repository.update { it.copy(indicatorOpacity = 0.3f, indicatorThicknessDp = 0.4f, landscapeEnabled = true) }
        }
        session { repository ->
            val saved = repository.settings.first()
            assertTrue(saved.enabled)
            assertTrue(saved.includePrereleases)
            assertEquals(Placement(Edge.BOTTOM, 0.2f), saved.portrait)
            assertEquals(setOf("sample.app"), saved.excludedPackages)
            assertEquals(0.3f, saved.indicatorOpacity)
            assertEquals(0.4f, saved.indicatorThicknessDp)
            repository.reset()
            assertEquals(TriggerSettings(enabled = true, includePrereleases = true), repository.settings.first())
        }
    }

    @Test fun `legacy indicator ratio migrates once and preserves unrelated preferences`() = runTest {
        val file = folder.newFile("legacy.preferences_pb")
        suspend fun session(migrate: Boolean, block: suspend (androidx.datastore.core.DataStore<Preferences>) -> Unit) {
            val job = SupervisorJob()
            val storage = OkioStorage(FileSystem.SYSTEM, PreferencesSerializer) { file.absolutePath.toPath() }
            val store = PreferenceDataStoreFactory.create(storage = storage, migrations = if (migrate) listOf(IndicatorFractionMigration()) else emptyList(), scope = CoroutineScope(Dispatchers.IO + job))
            try { block(store) } finally { job.cancelAndJoin() }
        }
        session(false) { store ->
            store.edit {
                it[floatPreferencesKey("length")] = 180f
                it[floatPreferencesKey("indicator.length")] = 45f
                it[floatPreferencesKey("portrait.position")] = 0.31f
                it[stringPreferencesKey("landscape.edge")] = "BOTTOM"
                it[floatPreferencesKey("landscape.position")] = 0.9f
                it[booleanPreferencesKey("landscape.enabled")] = true
                it[stringSetPreferencesKey("excluded.packages")] = setOf("game")
                it[stringPreferencesKey("future.field")] = "keep me"
            }
        }
        session(true) { store ->
            val repo = DataStoreSettingsRepository(store)
            val s = repo.settings.first()
            assertEquals(0.25f, s.indicatorLengthFraction)
            assertEquals(0.31f, s.portrait.position)
            assertEquals(Placement(Edge.BOTTOM, 0.9f), s.landscape)
            assertTrue(s.landscapeEnabled)
            assertFalse(s.landscapeOverrideEnabled)
            assertFalse(s.lockScreenEnabled)
            assertFalse(s.includePrereleases)
            assertEquals(setOf("game"), s.excludedPackages)
            assertNull(store.data.first()[floatPreferencesKey("indicator.length")])
            repo.update { it.copy(lengthDp = 80f, lockScreenEnabled = true) }
        }
        session(true) { store ->
            val s = DataStoreSettingsRepository(store).settings.first()
            assertEquals(0.25f, s.indicatorLengthFraction)
            assertEquals(80f, s.lengthDp)
            assertTrue(s.lockScreenEnabled)
            assertEquals("keep me", store.data.first()[stringPreferencesKey("future.field")])
        }
    }

    @Test fun `legacy dp thickness migrates once and stays fixed after depth changes and reopen`() = runTest {
        val file = folder.newFile("thickness.preferences_pb")
        suspend fun session(migrate: Boolean, block: suspend (androidx.datastore.core.DataStore<Preferences>) -> Unit) {
            val job = SupervisorJob()
            val storage = OkioStorage(FileSystem.SYSTEM, PreferencesSerializer) { file.absolutePath.toPath() }
            val store = PreferenceDataStoreFactory.create(storage = storage,
                migrations = if (migrate) listOf(IndicatorFractionMigration(), IndicatorThicknessDpMigration()) else emptyList(),
                scope = CoroutineScope(Dispatchers.IO + job))
            try { block(store) } finally { job.cancelAndJoin() }
        }
        val expected = TriggerSettings(enabled = true, portrait = Placement(Edge.BOTTOM, 0.31f),
            landscape = Placement(Edge.LEFT, 0.9f), landscapeEnabled = true, landscapeOverrideEnabled = true,
            lockScreenEnabled = true, lengthDp = 180f, depthDp = 24f, indicatorVisible = true,
            indicatorLengthFraction = 0.4f, indicatorThicknessDp = 6f, indicatorOpacity = 0.3f,
            cornerRadiusDp = 15f, activation = Activation.TAP, haptics = true, excludedPackages = setOf("game"))
        session(false) { store ->
            DataStoreSettingsRepository(store).update { expected }
            store.edit {
                it.remove(floatPreferencesKey("indicator.thickness.dp"))
                it[floatPreferencesKey("indicator.thickness")] = 6f
                it[stringPreferencesKey("future.field")] = "keep me"
            }
        }
        session(true) { store ->
            val repo = DataStoreSettingsRepository(store)
            assertEquals(expected, repo.settings.first())
            assertNull(store.data.first()[floatPreferencesKey("indicator.thickness")])
            assertEquals(6f, store.data.first()[floatPreferencesKey("indicator.thickness.dp")])
            repo.update { it.copy(depthDp = 8f) }
        }
        session(true) { store ->
            val repo = DataStoreSettingsRepository(store)
            assertEquals(expected.copy(depthDp = 8f), repo.settings.first())
            repo.update { it.copy(indicatorThicknessDp = 0f) }
        }
        session(true) { store ->
            assertEquals(expected.copy(depthDp = 8f, indicatorThicknessDp = 0f), DataStoreSettingsRepository(store).settings.first())
            assertEquals("keep me", store.data.first()[stringPreferencesKey("future.field")])
        }
    }

    @Test fun `thickness migration handles defaults corrupt dimensions and an existing dp value`() = runTest {
        val migration = IndicatorThicknessDpMigration()
        val key = floatPreferencesKey("indicator.thickness.dp")
        assertEquals(4f, migration.migrate(emptyPreferences())[key])
        for ((thickness, depth, expected) in listOf(
            Triple(40f, 8f, 8f), Triple(Float.NaN, Float.NaN, 4f),
            Triple(-1f, 0f, 1f), Triple(4f, 32f, 4f),
        )) {
            val old = preferencesOf(floatPreferencesKey("indicator.thickness") to thickness, floatPreferencesKey("depth") to depth)
            assertTrue(migration.shouldMigrate(old))
            val migrated = migration.migrate(old)
            assertEquals(expected, migrated[key])
            assertFalse(migration.shouldMigrate(migrated))
        }
        val existing = preferencesOf(key to 0f, floatPreferencesKey("indicator.thickness") to 32f)
        assertFalse(migration.shouldMigrate(existing))
    }

    @Test fun `percentage preferences convert using their saved depth and keep unrelated keys`() = runTest {
        val migration = IndicatorThicknessDpMigration()
        val key = floatPreferencesKey("indicator.thickness.dp")
        val fraction = floatPreferencesKey("indicator.thickness.fraction")
        for ((value, depth, expected) in listOf(Triple(.25f, 24f, 6f), Triple(0f, 24f, 0f),
            Triple(1f, 8f, 8f), Triple(2f, 24f, 24f), Triple(-1f, 24f, 0f), Triple(Float.NaN, Float.NaN, 4f))) {
            val old = preferencesOf(fraction to value, floatPreferencesKey("depth") to depth,
                stringPreferencesKey("future.field") to "keep")
            val migrated = migration.migrate(old)
            assertEquals(expected, migrated[key])
            assertNull(migrated[fraction]); assertEquals("keep", migrated[stringPreferencesKey("future.field")])
            assertFalse(migration.shouldMigrate(migrated))
        }
        val existing = preferencesOf(key to 0f, fraction to 1f)
        assertFalse(migration.shouldMigrate(existing))
    }
}
