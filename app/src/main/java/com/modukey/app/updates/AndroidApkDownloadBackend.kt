package com.modukey.app.updates

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.modukey.app.R
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.UUID

class AndroidApkDownloadBackend(context: Context) : ApkDownloadBackend {
    private val app = context.applicationContext
    private val preferences by lazy { app.getSharedPreferences("apk_download", Context.MODE_PRIVATE) }
    private val manager get() = app.getSystemService(DownloadManager::class.java)
        ?: throw ApkDownloadException(ApkDownloadFailure.UNAVAILABLE)

    override val completions = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                    trySend(intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1))
                }
            }
        }
        var registered = false
        try {
            // DownloadsProvider has a different UID. A received ID only causes a fresh system query.
            ContextCompat.registerReceiver(app, receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_EXPORTED)
            registered = true
        } catch (_: RuntimeException) { /* Foreground progress queries still recover completion. */ }
        awaitClose { if (registered) runCatching { app.unregisterReceiver(receiver) } }
    }

    @Suppress("DEPRECATION")
    override fun restore(): ApkDownloadRecord? {
        val raw = preferences.getString(RECORD_KEY, null) ?: return null
        val record = runCatching {
            require(raw.length <= 8_192)
            val saved = JSONObject(raw)
            ApkDownloadRecord(saved.getLong("id"), ApkTarget(saved.getString("tag"), saved.getString("url")), saved.getString("filename"))
                .also { require(it.id >= 0 && it.target.valid()); file(it) }
        }.getOrNull() ?: return null
        val installed = ReleaseVersion.parse(app.packageManager.getPackageInfo(app.packageName, 0).versionName.orEmpty())
        val selected = ReleaseVersion.parse(record.target.tag)
        if (installed != null && selected != null && installed >= selected) {
            remove(record)
            return null
        }
        return record
    }

    override fun enqueue(target: ApkTarget): ApkDownloadRecord {
        require(target.valid())
        val folder = directory()
        if (!folder.isDirectory && !folder.mkdirs()) throw ApkDownloadException(ApkDownloadFailure.STORAGE)
        val filename = "update-${UUID.randomUUID()}.apk"
        val destination = File(folder, filename)
        val request = DownloadManager.Request(target.url.toUri())
            .setDestinationUri(destination.toUri())
            .setMimeType(APK_MIME)
            .setTitle(app.getString(R.string.apk_download_title, target.versionName))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        val id = manager.enqueue(request)
        if (id < 0) throw ApkDownloadException(ApkDownloadFailure.UNAVAILABLE)
        val record = ApkDownloadRecord(id, target, filename)
        val saved = JSONObject().put("id", id).put("tag", target.tag).put("url", target.url).put("filename", filename)
        if (!preferences.edit().putString(RECORD_KEY, saved.toString()).commit()) {
            manager.remove(id)
            throw ApkDownloadException(ApkDownloadFailure.STORAGE)
        }
        return record
    }

    override fun query(record: ApkDownloadRecord): ApkTransfer {
        manager.query(DownloadManager.Query().setFilterById(record.id))?.use { cursor ->
            if (!cursor.moveToFirst()) return ApkTransfer.Failed(ApkDownloadFailure.MISSING)
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val bytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)).coerceAtLeast(0)
            val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            return when (status) {
                DownloadManager.STATUS_PENDING -> ApkTransfer.Active(ApkDownloadPhase.QUEUED, bytes, total)
                DownloadManager.STATUS_RUNNING -> ApkTransfer.Active(ApkDownloadPhase.DOWNLOADING, bytes, total)
                DownloadManager.STATUS_PAUSED -> ApkTransfer.Active(ApkDownloadPhase.PAUSED, bytes, total)
                DownloadManager.STATUS_SUCCESSFUL -> if (file(record).isFile) ApkTransfer.Complete else ApkTransfer.Failed(ApkDownloadFailure.MISSING)
                else -> {
                    val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                    ApkTransfer.Failed(when (reason) {
                        DownloadManager.ERROR_INSUFFICIENT_SPACE, DownloadManager.ERROR_DEVICE_NOT_FOUND, DownloadManager.ERROR_FILE_ERROR,
                        DownloadManager.ERROR_FILE_ALREADY_EXISTS -> ApkDownloadFailure.STORAGE
                        DownloadManager.ERROR_TOO_MANY_REDIRECTS, DownloadManager.ERROR_HTTP_DATA_ERROR -> ApkDownloadFailure.NETWORK
                        in 400..599 -> ApkDownloadFailure.NETWORK
                        else -> ApkDownloadFailure.UNKNOWN
                    })
                }
            }
        }
        return ApkTransfer.Failed(ApkDownloadFailure.UNAVAILABLE)
    }

    @Suppress("DEPRECATION")
    override fun validate(record: ApkDownloadRecord): ApkDownloadFailure? {
        val apk = file(record)
        if (!apk.isFile || apk.length() == 0L) return ApkDownloadFailure.MISSING
        val pm = app.packageManager
        val installed = pm.getPackageInfo(app.packageName, PackageManager.GET_SIGNING_CERTIFICATES).identity()
        val downloaded = pm.getPackageArchiveInfo(apk.path, PackageManager.GET_SIGNING_CERTIFICATES)?.identity()
        return checkApkIdentity(installed, downloaded, record.target)
    }

    override fun remove(record: ApkDownloadRecord?) {
        if (record != null) {
            manager.remove(record.id)
            val apk = file(record)
            if (apk.exists() && !apk.delete()) throw ApkDownloadException(ApkDownloadFailure.STORAGE)
        }
        if (!preferences.edit().remove(RECORD_KEY).commit()) throw ApkDownloadException(ApkDownloadFailure.STORAGE)
    }

    fun installIntent(record: ApkDownloadRecord): Intent {
        if (query(record) != ApkTransfer.Complete) throw ApkDownloadException(ApkDownloadFailure.MISSING)
        val uri = manager.getUriForDownloadedFile(record.id) ?: throw ApkDownloadException(ApkDownloadFailure.MISSING)
        require(uri.scheme == "content")
        return Intent(Intent.ACTION_VIEW).setDataAndType(uri, APK_MIME)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .apply { clipData = ClipData.newRawUri("", uri) }
    }

    private fun directory(): File = File(app.getExternalFilesDirs(null).firstOrNull()
        ?: throw ApkDownloadException(ApkDownloadFailure.STORAGE), "${Environment.DIRECTORY_DOWNLOADS}/updates")

    private fun file(record: ApkDownloadRecord): File {
        require(Regex("update-[0-9a-f-]{36}\\.apk").matches(record.filename))
        val directory = directory().canonicalFile
        return File(directory, record.filename).canonicalFile.also { require(it.parentFile == directory) }
    }

    private fun PackageInfo.identity(): ApkIdentity = ApkIdentity(packageName, versionName, longVersionCode,
        signingInfo?.apkContentsSigners.orEmpty().map { signature ->
            MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") { "%02x".format(it) }
        }.toSet())

    companion object {
        private const val RECORD_KEY = "active"
        const val APK_MIME = "application/vnd.android.package-archive"
    }
}
