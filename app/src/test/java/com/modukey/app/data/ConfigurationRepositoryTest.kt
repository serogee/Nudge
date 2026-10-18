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
import java.io.File
import org.json.JSONObject

@RunWith(RobolectricTestRunner::class)
class ConfigurationRepositoryTest {
    @get:Rule val folder=TemporaryFolder()
    private suspend fun session(file:File,block:suspend (DataStore<Preferences>)->Unit) {
        val job=SupervisorJob()
        val storage=OkioStorage(FileSystem.SYSTEM,PreferencesSerializer) { file.absolutePath.toPath() }
        val store=PreferenceDataStoreFactory.create(storage=storage,scope=CoroutineScope(Dispatchers.IO+job))
        try { block(store) } finally { job.cancelAndJoin() }
    }
    @Test fun `panel upgrade persists one complete document and stable references across reopening`()=runTest {
        val file=folder.newFile("panels.preferences_pb")
        val key=stringPreferencesKey("configuration.v1")
        val source=checkNotNull(javaClass.classLoader!!.getResourceAsStream("configuration-v1.json")).bufferedReader().use { it.readText() }
        var migrated:AppConfiguration?=null
        session(file) { store ->
            store.edit { it[key]=source; it[stringPreferencesKey("future.field")]="keep" }
            val result=DataStoreConfigurationRepository(store) { name,_,slot -> "$name / ${slot+1}" }.configuration.first()
            assertEquals(2,result.panels.size); assertEquals("Side / 1",result.panels.first().name)
            assertEquals(result,ConfigurationCodec.decode(store.data.first()[key]!!))
            assertEquals(ConfigurationCodec.SCHEMA,JSONObject(store.data.first()[key]!!).getInt("schema"))
            assertEquals("keep",store.data.first()[stringPreferencesKey("future.field")])
            migrated=result
        }
        session(file) { store ->
            assertEquals(migrated,DataStoreConfigurationRepository(store).configuration.first())
        }
    }
    @Test fun `failed panel upgrade leaves original data intact`()=runTest {
        val file=folder.newFile("invalid.preferences_pb")
        val key=stringPreferencesKey("configuration.v1")
        val source=checkNotNull(javaClass.classLoader!!.getResourceAsStream("configuration-v1.json")).bufferedReader().use { it.readText() }
        val broken=JSONObject(source).apply { getJSONArray("handles").getJSONObject(0).getJSONArray("panels").getJSONObject(1).remove("buttons") }.toString()
        session(file) { store ->
            store.edit { it[key]=broken }
            try { DataStoreConfigurationRepository(store).configuration.first(); fail("Accepted an incomplete migration") } catch (_:IOException) { }
            assertEquals(broken,store.data.first()[key])
        }
    }
    @Test fun `direct actions survive disk reopening and schema two upgrade keeps existing assignments`()=runTest {
        val file=folder.newFile("actions.preferences_pb")
        val key=stringPreferencesKey("configuration.v1")
        val panel=SavedPanel(name="Tools")
        val old=AppConfiguration(enabled=true,includePrereleases=true,panels=listOf(panel),handles=listOf(HandleConfig(enabled=true,
            volumeSequenceMs=6_000,bindings=mapOf(GestureSlot.SWIPE_1 to GestureTarget.panel(panel.id,true)))))
        val direct=PanelButton(action=ButtonAction.OPEN_APP,appPackage="test.camera",appActivity="test.camera.Main",appLabel="Camera")
        var saved:AppConfiguration?=null
        session(file) { store ->
            store.edit { it[key]=JSONObject(ConfigurationCodec.encode(old)).put("schema",2).toString() }
            val repo=DataStoreConfigurationRepository(store)
            val upgraded=repo.configuration.first()
            assertEquals(old.normalized(),upgraded)
            assertEquals(ConfigurationCodec.SCHEMA,JSONObject(store.data.first()[key]!!).getInt("schema"))
            saved=upgraded.editHandle(upgraded.handles.single().id) { h -> h.copy(bindings=h.bindings+(GestureSlot.HOLD to GestureTarget.direct(direct))) }.normalized()
            repo.write(saved!!)
        }
        session(file) { store -> assertEquals(saved,DataStoreConfigurationRepository(store).configuration.first()) }
    }
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
