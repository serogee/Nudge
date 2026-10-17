package com.modukey.app.updates

import android.app.Application
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE, application = Application::class)
class GitHubReleaseSourceTest {
    private fun release(tag: String = "v0.1.1", draft: Boolean = false, published: Boolean = true, url: String? = null, prerelease: Boolean = true): JSONObject {
        val filename = "Nudge-${tag.removePrefix("v")}.apk"
        val download = url ?: "https://github.com/serogee/Nudge/releases/download/$tag/$filename"
        return JSONObject().put("tag_name", tag).put("draft", draft).put("prerelease", prerelease)
            .put("published_at", if (published) "2026-10-17T11:07:54Z" else JSONObject.NULL)
            .put("html_url", "https://unrelated.example/ignored")
            .put("assets", JSONArray().put(JSONObject().put("name", filename).put("state", "uploaded").put("browser_download_url", download)))
    }

    @Test fun `highest published version wins regardless of order and published prereleases are included`() {
        val body = JSONArray().put(release("v0.1.9")).put(release("v9.0.0", draft = true))
            .put(release("v8.0.0", published = false)).put(release("nightly")).put(release("v0.1.10")).toString()
        val latest = requireNotNull(GitHubReleaseSource.parseReleases(body, true))
        assertEquals("v0.1.10", latest.tag)
        assertTrue(latest.prerelease)
        assertEquals("https://github.com/serogee/Nudge/releases/tag/v0.1.10", latest.pageUrl)
        assertEquals("https://github.com/serogee/Nudge/releases/download/v0.1.10/Nudge-0.1.10.apk", latest.apkUrl)
    }

    @Test fun `missing APKs and empty lists remain distinct from malformed metadata`() {
        assertNull(GitHubReleaseSource.parseReleases("[]"))
        val withoutApk = release().put("assets", JSONArray())
        assertNull(requireNotNull(GitHubReleaseSource.parseReleases(JSONArray().put(withoutApk).toString(), true)).apkUrl)
        listOf("{}", "<html>unavailable</html>").forEach { body ->
            assertThrows(UpdateCheckException::class.java) { GitHubReleaseSource.parseReleases(body) }
        }
    }

    @Test fun `APK links must belong to the matching release in the Nudge repository`() {
        listOf(
            "http://github.com/serogee/Nudge/releases/download/v0.1.1/Nudge-0.1.1.apk",
            "https://github.com.evil.example/serogee/Nudge/releases/download/v0.1.1/Nudge-0.1.1.apk",
            "https://github.com@evil.example/serogee/Nudge/releases/download/v0.1.1/Nudge-0.1.1.apk",
            "https://github.com/another/repo/releases/download/v0.1.1/Nudge-0.1.1.apk",
            "https://github.com/serogee/Nudge/releases/download/v0.1.0/Nudge-0.1.1.apk",
        ).forEach { url ->
            val latest = requireNotNull(GitHubReleaseSource.parseReleases(JSONArray().put(release(url = url)).toString(), true))
            assertNull(url, latest.apkUrl)
            assertEquals("https://github.com/serogee/Nudge/releases/tag/v0.1.1", latest.pageUrl)
        }
    }

    @Test fun `successful HTTP checks use bounded timeouts and release the connection`() = runTest {
        val connection = FakeConnection(JSONArray().put(release()).toString())
        val latest = GitHubReleaseSource { connection }.latest(true)
        assertEquals("v0.1.1", latest?.tag)
        assertTrue(connection.disconnected)
        assertTrue(connection.connectTimeout in 1..30_000)
        assertTrue(connection.readTimeout in 1..30_000)
        assertEquals("GET", connection.requestMethod)
    }

    @Test fun `rate limits missing repositories server errors and timeouts fail without leaking connections`() = runTest {
        listOf(403, 429, 404, 500).forEach { status ->
            val connection = FakeConnection("", status)
            try {
                GitHubReleaseSource { connection }.latest(true)
                fail("HTTP $status must not report an installed version as current")
            } catch (error: UpdateCheckException) {
                assertTrue(error.message.orEmpty().isNotBlank())
                if (status == 403 || status == 429) assertTrue(error.message.orEmpty().contains("limit"))
            }
            assertTrue(connection.disconnected)
            assertFalse(connection.streamOpened)
        }
        val timeout = FakeConnection("", failure = SocketTimeoutException("timeout"))
        try {
            GitHubReleaseSource { timeout }.latest(true)
            fail("A timeout must remain a failed check")
        } catch (_: IOException) {
            assertTrue(timeout.disconnected)
        }
    }

    @Test fun `oversized and invalid HTTP bodies fail and close the stream and connection`() = runTest {
        listOf("x".repeat(1_048_577), "not JSON").forEach { body ->
            val connection = FakeConnection(body)
            try {
                GitHubReleaseSource { connection }.latest(true)
                fail("Invalid metadata must not produce an update result")
            } catch (_: UpdateCheckException) {
                assertTrue(connection.disconnected)
                assertTrue(connection.streamClosed)
            }
        }
    }

    @Test fun `stable is the default channel and testing includes GitHub and semantic prereleases`() {
        val body = JSONArray().put(release("v0.1.0", prerelease = false)).put(release("v0.1.2"))
            .put(release("v0.1.3-rc.1", prerelease = false)).toString()
        assertEquals("v0.1.0", GitHubReleaseSource.parseReleases(body)?.tag)
        val testing = requireNotNull(GitHubReleaseSource.parseReleases(body, true))
        assertEquals("v0.1.3-rc.1", testing.tag)
        assertTrue(testing.prerelease)
        val onlyTesting = JSONArray().put(release()).toString()
        assertNull(GitHubReleaseSource.parseReleases(onlyTesting))
        assertEquals("v0.1.1", GitHubReleaseSource.parseReleases(onlyTesting, true)?.tag)
    }

    private class FakeConnection(body: String, private val status: Int = 200, private val failure: IOException? = null) : HttpURLConnection(URL("https://api.github.com/repos/serogee/Nudge/releases")) {
        var disconnected = false
        var streamOpened = false
        var streamClosed = false
        private val stream = object : ByteArrayInputStream(body.toByteArray()) {
            override fun close() { streamClosed = true; super.close() }
        }
        override fun connect() = Unit
        override fun usingProxy() = false
        override fun disconnect() { disconnected = true }
        override fun getResponseCode(): Int { failure?.let { throw it }; return status }
        override fun getHeaderField(name: String?): String? = if (name == "X-RateLimit-Remaining" && status == 403) "0" else null
        override fun getInputStream() = stream.also { streamOpened = true }
    }
}
