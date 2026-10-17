package com.modukey.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.*
import com.modukey.app.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class ConfigurationRepositoryTest {
    @get:Rule val folder=TemporaryFolder()
    @Test fun `legacy migration is atomic retains unknown keys and persists multiple handles on reopen`()=runTest {
        val file=folder.newFile("configuration.preferences_pb")
        suspend fun session(block:suspend (DataStore<Preferences>)->Unit) {
            val job=SupervisorJob(); val storage=OkioStorage(FileSystem.SYSTEM,PreferencesSerializer) { file.absolutePath.toPath() }
            val store=PreferenceDataStoreFactory.create(storage=storage,scope=CoroutineScope(Dispatchers.IO+job))
            try { block(store) } finally { job.cancelAndJoin() }
        }
        session { store ->
            DataStoreSettingsRepository(store).update { it.copy(enabled=true,activation=Activation.LONG_PRESS,includePrereleases=true,landscapeEnabled=true,portrait=Placement(Edge.BOTTOM,0.2f),excludedPackages=setOf("game")) }
            store.edit { it[stringPreferencesKey("future.field")]="keep" }
            val repo=DataStoreConfigurationRepository(store); val migrated=repo.configuration.first()
            assertTrue(migrated.enabled); assertTrue(migrated.includePrereleases)
            assertEquals(GestureTarget.VOLUME,migrated.handles[0].target(GestureSlot.HOLD)); assertEquals(GestureTarget.NONE,migrated.handles[0].target(GestureSlot.SWIPE_1))
            assertTrue(migrated.handles[0].trigger.landscapeEnabled)
            repo.write(migrated.copy(handles=migrated.handles+HandleConfig(name="Extra")))
        }
        session { store ->
            val current=DataStoreConfigurationRepository(store).configuration.first()
            assertEquals(2,current.handles.size); assertEquals(Placement(Edge.BOTTOM,0.2f),current.handles[0].trigger.portrait)
            assertEquals("keep",store.data.first()[stringPreferencesKey("future.field")])
            assertFalse(current.handles[1].enabled)
            store.edit { it[stringPreferencesKey("configuration.v1")]="{\"schema\":999}" }
            try { DataStoreConfigurationRepository(store).configuration.first(); fail() } catch (_:IOException) { }
            assertEquals("{\"schema\":999}",store.data.first()[stringPreferencesKey("configuration.v1")])
        }
    }
}
