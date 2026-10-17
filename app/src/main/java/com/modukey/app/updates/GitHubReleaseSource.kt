package com.modukey.app.updates

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import java.net.HttpURLConnection
import java.net.URI

class GitHubReleaseSource(
    private val connectionFactory: () -> HttpURLConnection = { URI(API_URL).toURL().openConnection() as HttpURLConnection },
) : ReleaseSource {
    override suspend fun latest(includePrereleases: Boolean): PublishedRelease? = withContext(Dispatchers.IO) {
        val connection = connectionFactory()
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "Nudge-Android")
            connection.setRequestProperty("X-GitHub-Api-Version", "2026-03-10")
            when (val code = connection.responseCode) {
                200 -> Unit
                429 -> throw UpdateCheckException("GitHub's update limit was reached. Try again later.", UpdateFailure.RATE_LIMIT)
                403 -> {
                    val limited = connection.getHeaderField("X-RateLimit-Remaining") == "0"
                    throw UpdateCheckException(if (limited) "GitHub's update limit was reached. Try again later." else "GitHub blocked the update check. Try again later or view releases on GitHub.", if (limited) UpdateFailure.RATE_LIMIT else UpdateFailure.BLOCKED)
                }
                404 -> throw UpdateCheckException("Nudge's public releases could not be found on GitHub.", UpdateFailure.NOT_FOUND)
                else -> throw UpdateCheckException(if (code >= 500) "GitHub is unavailable. Try again later." else "GitHub could not complete the update check. View releases on GitHub.", if (code >= 500) UpdateFailure.UNAVAILABLE else UpdateFailure.BLOCKED)
            }
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                val text = StringBuilder()
                val buffer = CharArray(4096)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val count = reader.read(buffer)
                    if (count < 0) break
                    if (text.length + count > MAX_RESPONSE_CHARS) throw UpdateCheckException("GitHub's update response was too large. View releases on GitHub.", UpdateFailure.TOO_LARGE)
                    text.append(buffer, 0, count)
                }
                text.toString()
            }
            parseReleases(body, includePrereleases)
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val RELEASES_PAGE = "https://github.com/serogee/Nudge/releases"
        private const val API_URL = "https://api.github.com/repos/serogee/Nudge/releases?per_page=100"
        private const val MAX_RESPONSE_CHARS = 1_048_576

        internal fun parseReleases(body: String, includePrereleases: Boolean = false): PublishedRelease? {
            try {
                val releases = JSONArray(body)
                var latest: PublishedRelease? = null
                for (index in 0 until releases.length()) {
                    val release = releases.optJSONObject(index) ?: continue
                    if (release.optBoolean("draft", true) || release.isNull("published_at") || release.optString("published_at", "").isBlank()) continue
                    val tag = release.optString("tag_name", "")
                    val version = ReleaseVersion.parse(tag) ?: continue
                    val prerelease = release.optBoolean("prerelease", true) || version.isPrerelease
                    if (prerelease && !includePrereleases) continue
                    val filename = "Nudge-${tag.removePrefix("v").removePrefix("V")}.apk"
                    val assets = release.optJSONArray("assets") ?: JSONArray()
                    var apkUrl: String? = null
                    for (assetIndex in 0 until assets.length()) {
                        val asset = assets.optJSONObject(assetIndex) ?: continue
                        val url = asset.optString("browser_download_url", "")
                        if (asset.optString("name") == filename && asset.optString("state") == "uploaded" && validApkUrl(url, tag, filename)) {
                            apkUrl = url
                            break
                        }
                    }
                    val candidate = PublishedRelease(tag, version, prerelease, "$RELEASES_PAGE/tag/$tag", apkUrl)
                    if (latest == null || version > latest.version || (version == latest.version && !prerelease && latest.prerelease)) latest = candidate
                }
                return latest
            } catch (_: JSONException) {
                throw UpdateCheckException("GitHub's release data could not be read. Try again later or view releases on GitHub.", UpdateFailure.INVALID_RESPONSE)
            }
        }

        private fun validApkUrl(value: String, tag: String, filename: String): Boolean = try {
            val uri = URI(value)
            uri.scheme == "https" && uri.host.equals("github.com", ignoreCase = true) && uri.userInfo == null &&
                (uri.port == -1 || uri.port == 443) && uri.query == null && uri.fragment == null &&
                uri.path == "/serogee/Nudge/releases/download/$tag/$filename"
        } catch (_: java.net.URISyntaxException) {
            false
        }
    }
}
