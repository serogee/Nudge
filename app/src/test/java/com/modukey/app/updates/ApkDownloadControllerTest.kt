package com.modukey.app.updates

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ApkDownloadControllerTest {
    private val target = ApkTarget("v0.3.2", "https://github.com/serogee/Nudge/releases/download/v0.3.2/Nudge-0.3.2.apk")

    @Test fun `duplicate taps enqueue one system download and retain progress`() = runTest {
        val backend = Backend()
        val controller = ApkDownloadController(backend, backgroundScope, StandardTestDispatcher(testScheduler))
        controller.download(target); controller.download(target)
        runCurrent()
        assertEquals(1, backend.enqueues)
        backend.transfer = ApkTransfer.Active(ApkDownloadPhase.DOWNLOADING, 20, 100)
        controller.refresh(); runCurrent()
        assertEquals(ApkDownloadState.Active(target, ApkDownloadPhase.DOWNLOADING, 20, 100), controller.state.value)
        assertEquals(0, backend.validations)
    }

    @Test fun `reopening restores the system download without restarting it`() = runTest {
        val backend = Backend()
        val first = ApkDownloadController(backend, backgroundScope, StandardTestDispatcher(testScheduler))
        first.download(target); runCurrent()
        val second = ApkDownloadController(backend, backgroundScope, StandardTestDispatcher(testScheduler))
        second.download(target); runCurrent()
        assertEquals(1, backend.enqueues)
        backend.transfer = ApkTransfer.Complete
        second.refresh(); runCurrent()
        assertEquals(ApkDownloadState.Ready(target), second.state.value)
        assertEquals(1, backend.validations)
    }

    @Test fun `completion verifies before ready and never installs automatically`() = runTest {
        val backend = Backend()
        val controller = ApkDownloadController(backend, backgroundScope, StandardTestDispatcher(testScheduler))
        controller.download(target); runCurrent()
        backend.transfer = ApkTransfer.Complete
        controller.refresh(); runCurrent()
        assertEquals(ApkDownloadState.Ready(target), controller.state.value)
        controller.refresh(); runCurrent()
        assertEquals(1, backend.validations)
        assertNotNull(controller.prepareInstall())
        assertEquals(2, backend.validations)
        backend.invalid = ApkDownloadFailure.SIGNATURE
        assertNull(controller.prepareInstall())
        assertEquals(ApkDownloadState.Failed(target, ApkDownloadFailure.SIGNATURE), controller.state.value)
    }

    @Test fun `failure remains visible and retry replaces only the saved download`() = runTest {
        val backend = Backend()
        val controller = ApkDownloadController(backend, backgroundScope, StandardTestDispatcher(testScheduler))
        controller.download(target); runCurrent()
        backend.transfer = ApkTransfer.Failed(ApkDownloadFailure.NETWORK)
        controller.refresh(); runCurrent()
        assertEquals(ApkDownloadState.Failed(target, ApkDownloadFailure.NETWORK), controller.state.value)
        advanceTimeBy(86_400_000); runCurrent()
        assertEquals(1, backend.enqueues)
        controller.retry(); runCurrent()
        assertEquals(2, backend.enqueues)
        assertEquals(listOf(1L), backend.removed)
        assertTrue(controller.state.value is ApkDownloadState.Active)
    }

    @Test fun `cancel removes the transfer and persistent record`() = runTest {
        val backend = Backend()
        val controller = ApkDownloadController(backend, backgroundScope, StandardTestDispatcher(testScheduler))
        controller.download(target); runCurrent()
        controller.cancel(); runCurrent()
        assertEquals(ApkDownloadState.Idle, controller.state.value)
        assertNull(backend.saved)
        assertEquals(listOf(1L), backend.removed)
        val reopened = ApkDownloadController(backend, backgroundScope, StandardTestDispatcher(testScheduler))
        reopened.refresh(); runCurrent()
        assertEquals(ApkDownloadState.Idle, reopened.state.value)
    }

    @Test fun `progress checks stop off screen and completed downloads leave no periodic work`() = runTest {
        val backend = Backend()
        val controller = ApkDownloadController(backend, backgroundScope, StandardTestDispatcher(testScheduler))
        controller.download(target); runCurrent()
        val observer = backgroundScope.launch { controller.observe() }
        runCurrent()
        assertEquals(1, backend.completions.subscriptionCount.value)
        val start = backend.queries
        advanceTimeBy(2_001); runCurrent()
        assertTrue(backend.queries > start)
        observer.cancelAndJoin()
        assertEquals(0, backend.completions.subscriptionCount.value)
        val hidden = backend.queries
        advanceTimeBy(60_000); runCurrent()
        assertEquals(hidden, backend.queries)
        backend.transfer = ApkTransfer.Complete
        val resumed = backgroundScope.launch { controller.observe() }
        runCurrent()
        assertTrue(controller.state.value is ApkDownloadState.Ready)
        val completed = backend.queries
        advanceTimeBy(60_000); runCurrent()
        assertEquals(completed, backend.queries)
        resumed.cancelAndJoin()
    }

    @Test fun `only completion for the saved ID prompts a system query`() = runTest {
        val backend = Backend()
        val controller = ApkDownloadController(backend, backgroundScope, StandardTestDispatcher(testScheduler))
        controller.download(target); runCurrent()
        val observer = backgroundScope.launch { controller.observe() }
        runCurrent()
        val before = backend.queries
        backend.completions.emit(100); runCurrent()
        assertEquals(before, backend.queries)
        backend.transfer = ApkTransfer.Complete
        backend.completions.emit(1); runCurrent()
        assertEquals(before + 1, backend.queries)
        assertTrue(controller.state.value is ApkDownloadState.Ready)
        observer.cancelAndJoin()
    }

    @Test fun `a new release cannot replace an active download`() = runTest {
        val backend = Backend()
        val controller = ApkDownloadController(backend, backgroundScope, StandardTestDispatcher(testScheduler))
        controller.download(target); runCurrent()
        controller.download(ApkTarget("v0.3.3", "https://github.com/serogee/Nudge/releases/download/v0.3.3/Nudge-0.3.3.apk")); runCurrent()
        assertEquals(1, backend.enqueues)
        assertEquals(target, controller.state.value.target)
    }

    @Test fun `save errors and missing downloads do not report success`() = runTest {
        val backend = Backend()
        backend.startError = ApkDownloadFailure.STORAGE
        val controller = ApkDownloadController(backend, backgroundScope, StandardTestDispatcher(testScheduler))
        controller.download(target); runCurrent()
        assertEquals(ApkDownloadState.Failed(target, ApkDownloadFailure.STORAGE), controller.state.value)
        backend.startError = null
        controller.retry(); runCurrent()
        backend.transfer = ApkTransfer.Failed(ApkDownloadFailure.MISSING)
        assertNull(controller.prepareInstall())
        assertEquals(ApkDownloadState.Failed(target, ApkDownloadFailure.MISSING), controller.state.value)
    }

    @Test fun `download targets reject unrelated hosts files and unsafe paths`() {
        assertTrue(target.valid())
        assertTrue(ApkTarget("v0.3.2+build.1", "https://github.com/serogee/Nudge/releases/download/v0.3.2%2Bbuild.1/Nudge-0.3.2%2Bbuild.1.apk").valid())
        listOf(
            target.copy(url = target.url.replace("https:", "http:")),
            target.copy(url = target.url.replace("github.com", "github.com.evil.example")),
            target.copy(url = target.url.replace("Nudge-0.3.2.apk", "other.apk")),
            target.copy(url = target.url + "?download=1"),
            target.copy(url = target.url + "#fragment"),
            target.copy(url = target.url.replace("Nudge-0.3.2.apk", "%2E%2E%2Fother.apk")),
            target.copy(tag = "../../file"),
            target.copy(url = "not a URL"),
        ).forEach { assertFalse(it.toString(), it.valid()) }
    }

    @Test fun `invalid direct URLs never enter the system download queue`() = runTest {
        val backend = Backend()
        val controller = ApkDownloadController(backend, backgroundScope, StandardTestDispatcher(testScheduler))
        val invalid = target.copy(url = "https://unrelated.example/file.apk")
        controller.download(invalid); runCurrent()
        assertEquals(0, backend.enqueues)
        assertEquals(ApkDownloadState.Failed(invalid, ApkDownloadFailure.UNAVAILABLE), controller.state.value)
    }

    private class Backend : ApkDownloadBackend {
        override val completions = MutableSharedFlow<Long>()
        var saved: ApkDownloadRecord? = null
        var enqueues = 0
        var queries = 0
        var validations = 0
        val removed = mutableListOf<Long>()
        var transfer: ApkTransfer = ApkTransfer.Active(ApkDownloadPhase.QUEUED)
        var invalid: ApkDownloadFailure? = null
        var startError: ApkDownloadFailure? = null
        override fun restore() = saved
        override fun enqueue(target: ApkTarget): ApkDownloadRecord {
            startError?.let { throw ApkDownloadException(it) }
            enqueues++
            transfer = ApkTransfer.Active(ApkDownloadPhase.QUEUED)
            return ApkDownloadRecord(enqueues.toLong(), target, "test.apk").also { saved = it }
        }
        override fun query(record: ApkDownloadRecord): ApkTransfer { queries++; return transfer }
        override fun validate(record: ApkDownloadRecord): ApkDownloadFailure? { validations++; return invalid }
        override fun remove(record: ApkDownloadRecord?) { record?.let { removed += it.id }; saved = null }
    }
}
