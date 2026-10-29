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
    @Test fun optionInfoDefaultsOnAndSavedChoicesSurviveReopeningAndBackups()=runTest {
        val file=folder.newFile("option-info.preferences_pb")
        val old=JSONObject(ConfigurationCodec.encode(AppConfiguration())).put("schema",31).apply { remove("showOptionInfo") }
        session(file) { store ->
            store.edit { it[stringPreferencesKey("configuration.v1")]=old.toString() }
            val repo=DataStoreConfigurationRepository(store)
            val loaded=repo.configuration.first()
            assertTrue(loaded.showOptionInfo)
            repo.write(loaded.copy(showOptionInfo=false))
        }
        session(file) { store ->
            val repo=DataStoreConfigurationRepository(store)
            val restored=repo.configuration.first()
            assertFalse(restored.showOptionInfo)
            val backup=ConfigurationCodec.decode(ConfigurationCodec.encode(restored))
            assertEquals(restored,backup)
            assertFalse(restored.addCopies(AppConfiguration()).showOptionInfo)
            assertTrue(AppConfiguration().addCopies(backup).showOptionInfo)
            repo.write(restored.copy(showOptionInfo=true))
        }
        session(file) { assertTrue(DataStoreConfigurationRepository(it).configuration.first().showOptionInfo) }
    }
    @Test fun pausesAndVisibilitySurviveReopeningWithMasterAndHandlesOff()=runTest {
        val file=folder.newFile("saved-pauses.preferences_pb")
        val original=AppConfiguration(enabled=false,handles=listOf(
            HandleConfig(id="owner",enabled=true),HandleConfig(id="other",enabled=false)),
            pausedHandleIds=setOf("other")).hideIndicators(true)
        session(file) { DataStoreConfigurationRepository(it).write(original) }
        session(file) { store ->
            val repo=DataStoreConfigurationRepository(store)
            val restored=repo.configuration.first()
            assertEquals(original.normalized(),restored)
            repo.write(restored.copy(enabled=true).editHandle("other") { it.copy(enabled=true) })
        }
        session(file) { store ->
            val repo=DataStoreConfigurationRepository(store)
            val restored=repo.configuration.first()
            assertEquals(setOf("other"),restored.pausedHandleIds); assertTrue(restored.hideHandleIndicators)
            val change=checkNotNull(handleControlChange(restored,"owner",restored.pausedHandleIds,HandleGroupScope.OTHERS,HandleGroupScope.ALL))
            assertFalse(change.paused); assertFalse(change.hidden)
            repo.write(restored.hideIndicators(change.visibilityTargets,change.hidden).pauseHandles(change.pauseTargets,change.paused))
        }
        session(file) { store ->
            val restored=DataStoreConfigurationRepository(store).configuration.first()
            assertTrue(restored.pausedHandleIds.isEmpty()); assertTrue(restored.hiddenIndicatorIds.isEmpty())
            assertTrue(restored.enabled); assertTrue(restored.handles.all { it.enabled })
        }
    }
    @Test fun oldDocumentsDefaultToResumedAndBackupsRetainDisabledPauseSelections() {
        val config=AppConfiguration(enabled=false,handles=listOf(HandleConfig(id="a",enabled=false)),pausedHandleIds=setOf("a","deleted")).normalized()
        assertEquals(config,ConfigurationCodec.decode(ConfigurationCodec.encode(config)))
        assertEquals(setOf("a"),config.pausedHandleIds)
        val old=JSONObject(ConfigurationCodec.encode(config)).put("schema",20)
        assertTrue(ConfigurationCodec.decode(old.toString()).pausedHandleIds.isEmpty())
        old.put("schema",21).remove("pausedHandleIds")
        assertTrue(ConfigurationCodec.decode(old.toString()).pausedHandleIds.isEmpty())
        assertEquals(setOf("a"),config.addCopies(config).pausedHandleIds)
        assertTrue(config.copy(handles=emptyList()).normalized().pausedHandleIds.isEmpty())
    }
    @Test fun globalVisibilityOverrideSurvivesReopeningWithoutChangingHandlePreferences()=runTest {
        val file=folder.newFile("visibility.preferences_pb")
        val configuration=AppConfiguration(handles=listOf(
            HandleConfig(id="visible",enabled=true,trigger=TriggerSettings(indicatorVisible=true)),
            HandleConfig(id="invisible",enabled=false,trigger=TriggerSettings(indicatorVisible=false)))).normalized().hideIndicators(true)
        session(file) { DataStoreConfigurationRepository(it).write(configuration) }
        session(file) { store ->
            val repo=DataStoreConfigurationRepository(store)
            val restored=repo.configuration.first()
            assertEquals(configuration,restored)
            repo.write(restored.hideIndicators(false))
        }
        session(file) { store ->
            val restored=DataStoreConfigurationRepository(store).configuration.first()
            assertFalse(restored.hideHandleIndicators)
            assertEquals(configuration.hideIndicators(false).handles,restored.handles)
        }
    }
    @Test fun `percentage document migrates atomically and retains dp thickness after reopening`()=runTest {
        val file=folder.newFile("thickness.preferences_pb")
        val key=stringPreferencesKey("configuration.v1")
        val old=JSONObject(ConfigurationCodec.encode(AppConfiguration())).put("schema",4)
        old.getJSONArray("handles").getJSONObject(0).getJSONObject("trigger").apply {
            remove("indicatorThicknessDp"); put("indicatorThickness",.75); put("depth",24)
        }
        session(file) { store ->
            store.edit { it[key]=old.toString(); it[stringPreferencesKey("future.field")]="keep" }
            val repo=DataStoreConfigurationRepository(store)
            val loaded=repo.configuration.first()
            assertEquals(18f,loaded.handles.single().trigger.indicatorThicknessDp)
            assertEquals(ConfigurationCodec.SCHEMA,JSONObject(store.data.first()[key]!!).getInt("schema"))
            repo.write(loaded.editHandle(loaded.handles.single().id) { it.copy(trigger=it.trigger.copy(depthDp=8f)) })
        }
        session(file) { store ->
            val loaded=DataStoreConfigurationRepository(store).configuration.first()
            assertEquals(18f,loaded.handles.single().trigger.indicatorThicknessDp)
            assertEquals(8f,loaded.handles.single().trigger.depthDp)
            assertEquals("keep",store.data.first()[stringPreferencesKey("future.field")])
        }
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
    @Test fun `hold timing defaults load from old documents and independent delays survive disk reopening`()=runTest {
        val file=folder.newFile("hold-timing.preferences_pb")
        val key=stringPreferencesKey("configuration.v1")
        val old=JSONObject(ConfigurationCodec.encode(AppConfiguration()))
        old.getJSONArray("handles").getJSONObject(0).getJSONObject("trigger").apply { remove("holdTimeMs"); remove("swipeHoldTimeMs") }
        var expected:AppConfiguration?=null
        session(file) { store ->
            store.edit { it[key]=old.toString() }
            val repo=DataStoreConfigurationRepository(store)
            val loaded=repo.configuration.first()
            assertEquals(500L,loaded.handles.single().trigger.holdTimeMs)
            assertEquals(500L,loaded.handles.single().trigger.swipeHoldTimeMs)
            val first=loaded.handles.single()
            expected=loaded.copy(handles=listOf(first.copy(trigger=first.trigger.copy(holdTimeMs=1_200,swipeHoldTimeMs=900,depthDp=22f)),
                HandleConfig(trigger=TriggerSettings(holdTimeMs=750,swipeHoldTimeMs=1_700)))).normalized()
            repo.write(expected!!)
        }
        session(file) { store ->
            val repo=DataStoreConfigurationRepository(store)
            val reopened=repo.configuration.first()
            assertEquals(expected,reopened)
            val first=reopened.handles.first()
            expected=reopened.editHandle(first.id) { h -> h.copy(trigger=h.trigger.copy(holdTimeMs=2_000)) }
            repo.write(expected!!)
        }
        session(file) { store ->
            val reopened=DataStoreConfigurationRepository(store).configuration.first()
            assertEquals(expected,reopened)
            assertEquals(900L,reopened.handles.first().trigger.swipeHoldTimeMs)
            assertEquals(22f,reopened.handles.first().trigger.depthDp)
            assertEquals(750L,reopened.handles.last().trigger.holdTimeMs)
            assertEquals(1_700L,reopened.handles.last().trigger.swipeHoldTimeMs)
        }
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
