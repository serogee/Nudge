package com.modukey.app.updates

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateCheckerTest {
    private fun release(tag: String = "v0.1.2", prerelease: Boolean = false) = PublishedRelease(tag, requireNotNull(ReleaseVersion.parse(tag)), prerelease, "https://github.com/serogee/Nudge/releases/tag/$tag", null)

    @Test fun `checks stay idle until tapped and duplicate taps share one request with no periodic work`() = runTest {
        var requests = 0
        val response = CompletableDeferred<PublishedRelease>()
        val checker = UpdateChecker(ReleaseSource { requests++; response.await() }, "0.1.1", backgroundScope)
        advanceTimeBy(86_400_000)
        runCurrent()
        assertEquals(UpdateState.Idle, checker.state.value)
        assertEquals(0, requests)
        checker.check()
        checker.check()
        assertEquals(UpdateState.Checking, checker.state.value)
        runCurrent()
        assertEquals(1, requests)
        response.complete(release())
        runCurrent()
        assertTrue((checker.state.value as UpdateState.Result).comparison > 0)
        advanceTimeBy(86_400_000)
        runCurrent()
        assertEquals(1, requests)
    }

    @Test fun `equal and newer local builds never offer a downgrade and debug suffix is ignored`() = runTest {
        listOf("0.1.2" to 0, "0.1.2-debug" to 0, "0.1.3-debug" to -1, "0.1.1" to 1).forEach { (installed, expected) ->
            val checker = UpdateChecker(ReleaseSource { release() }, installed, backgroundScope)
            checker.check()
            runCurrent()
            val result = checker.state.value as UpdateState.Result
            assertEquals(expected, result.comparison.sign())
            assertFalse(result.release.prerelease)
        }
    }

    @Test fun `failed requests remain errors without retries until tapped again`() = runTest {
        var requests = 0
        val checker = UpdateChecker(ReleaseSource { if (++requests == 1) throw IOException("offline") else release() }, "0.1.1", backgroundScope)
        checker.check()
        runCurrent()
        assertTrue(checker.state.value is UpdateState.Failed)
        advanceTimeBy(86_400_000)
        runCurrent()
        assertEquals(1, requests)
        checker.check()
        runCurrent()
        assertTrue((checker.state.value as UpdateState.Result).comparison > 0)
        assertEquals(2, requests)
    }

    @Test fun `no release and unsupported installed versions are not reported as up to date`() = runTest {
        val missing = UpdateChecker(ReleaseSource { null }, "0.1.1", backgroundScope)
        missing.check()
        runCurrent()
        assertEquals(UpdateState.NoRelease(false), missing.state.value)
        val unknown = UpdateChecker(ReleaseSource { error("Invalid installed versions must not start a request") }, "local", backgroundScope)
        unknown.check()
        runCurrent()
        assertTrue(unknown.state.value is UpdateState.Failed)
    }

    @Test fun `cancellation is preserved and does not become an update error`() = runTest {
        val checkScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val checker = UpdateChecker(ReleaseSource { awaitCancellation() }, "0.1.1", checkScope)
        checker.check()
        runCurrent()
        checkScope.cancel()
        runCurrent()
        assertEquals(UpdateState.Idle, checker.state.value)
    }

    @Test fun `changing channel clears results and a cancelled older request cannot overwrite the new channel`() = runTest {
        val channels = mutableListOf<Boolean>()
        val stable = CompletableDeferred<PublishedRelease>()
        val testing = CompletableDeferred<PublishedRelease>()
        val checker = UpdateChecker(ReleaseSource { include ->
            channels += include
            if (include) testing.await() else withContext(NonCancellable) { stable.await() }
        }, "0.1.1", backgroundScope)
        checker.check()
        runCurrent()
        checker.clear()
        assertEquals(UpdateState.Idle, checker.state.value)
        checker.check(true)
        runCurrent()
        stable.complete(release())
        runCurrent()
        assertEquals(UpdateState.Checking, checker.state.value)
        testing.complete(release("v0.1.3", prerelease = true))
        runCurrent()
        val result = checker.state.value as UpdateState.Result
        assertEquals("v0.1.3", result.release.tag)
        assertTrue(result.includePrereleases)
        assertEquals(listOf(false, true), channels)
        checker.clear()
        advanceTimeBy(86_400_000)
        runCurrent()
        assertEquals(UpdateState.Idle, checker.state.value)
        assertEquals(listOf(false, true), channels)
    }

    private fun Int.sign() = compareTo(0)
}
