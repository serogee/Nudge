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
            repository.update { it.copy(enabled = true, portrait = Placement(Edge.BOTTOM, 0.2f), excludedPackages = setOf("sample.app")) }
            repository.update { it.copy(indicatorOpacity = 0.3f, landscapeEnabled = true) }
        }
        session { repository ->
            val saved = repository.settings.first()
            assertTrue(saved.enabled)
            assertEquals(Placement(Edge.BOTTOM, 0.2f), saved.portrait)
            assertEquals(setOf("sample.app"), saved.excludedPackages)
            assertEquals(0.3f, saved.indicatorOpacity)
            repository.reset()
            assertEquals(TriggerSettings(enabled = true), repository.settings.first())
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
}
