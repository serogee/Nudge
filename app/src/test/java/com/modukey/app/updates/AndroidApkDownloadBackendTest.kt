package com.modukey.app.updates

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36], application = Application::class)
class AndroidApkDownloadBackendTest {
    private val app get() = ApplicationProvider.getApplicationContext<Application>()
    private val manager get() = app.getSystemService(DownloadManager::class.java)
    private val target = ApkTarget("v0.3.2", "https://github.com/serogee/Nudge/releases/download/v0.3.2/Nudge-0.3.2.apk")

    @Before fun clearDownloadRecord() {
        app.getSharedPreferences("apk_download", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun `APK transfer uses DownloadManager without opening a browser and survives backend recreation`() {
        val backend = AndroidApkDownloadBackend(app)
        val record = backend.enqueue(target)
        val request = shadowOf(shadowOf(manager).getRequest(record.id))
        assertEquals(target.url, request.uri.toString())
        assertEquals(AndroidApkDownloadBackend.APK_MIME, request.mimeType.toString())
        assertEquals(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED, request.notificationVisibility)
        assertEquals(apkFile(record).canonicalFile, File(requireNotNull(request.destination.path)).canonicalFile)
        assertNull(shadowOf(app).nextStartedActivity)
        assertEquals(record, AndroidApkDownloadBackend(app).restore())
    }

    @Test fun `system progress is queried and a missing completed file is not ready`() {
        val backend = AndroidApkDownloadBackend(app)
        val record = backend.enqueue(target)
        val request = shadowOf(shadowOf(manager).getRequest(record.id))
        request.setStatus(DownloadManager.STATUS_RUNNING)
        request.setBytesSoFar(50)
        request.setTotalSize(100)
        assertEquals(ApkTransfer.Active(ApkDownloadPhase.DOWNLOADING, 50, 100), backend.query(record))
        request.setStatus(DownloadManager.STATUS_PAUSED)
        assertEquals(ApkTransfer.Active(ApkDownloadPhase.PAUSED, 50, 100), backend.query(record))
        request.setStatus(DownloadManager.STATUS_SUCCESSFUL)
        assertEquals(ApkTransfer.Failed(ApkDownloadFailure.MISSING), backend.query(record))
        assertEquals(ApkDownloadFailure.MISSING, backend.validate(record))
    }

    @Test fun `cancel removes only its owned file system request and saved ID`() {
        val backend = AndroidApkDownloadBackend(app)
        val record = backend.enqueue(target)
        val apk = apkFile(record)
        apk.writeText("test")
        backend.remove(record)
        assertFalse(apk.exists())
        assertNull(shadowOf(manager).getRequest(record.id))
        assertNull(AndroidApkDownloadBackend(app).restore())
    }

    @Test fun `installer receives the completed download URI with read access and no browser category`() {
        val backend = AndroidApkDownloadBackend(app)
        val record = backend.enqueue(target)
        val apk = apkFile(record)
        apk.writeText("test")
        shadowOf(shadowOf(manager).getRequest(record.id)).setStatus(DownloadManager.STATUS_SUCCESSFUL)
        val intent = backend.installIntent(record)
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(AndroidApkDownloadBackend.APK_MIME, intent.type)
        assertEquals("content", intent.data?.scheme)
        assertEquals(manager.getUriForDownloadedFile(record.id), intent.data)
        assertEquals(Intent.FLAG_GRANT_READ_URI_PERMISSION, intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION)
        assertEquals(0, intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        assertFalse(intent.categories.orEmpty().contains(Intent.CATEGORY_BROWSABLE))
        assertEquals(intent.data, intent.clipData?.getItemAt(0)?.uri)
        assertThrows(IllegalArgumentException::class.java) { backend.installIntent(record.copy(filename = "../../private.apk")) }
        shadowOf(shadowOf(manager).getRequest(record.id)).setStatus(DownloadManager.STATUS_RUNNING)
        assertThrows(ApkDownloadException::class.java) { backend.installIntent(record) }
    }

    @Test fun `corrupt saved state cannot redirect a download or share another file`() {
        val preferences = app.getSharedPreferences("apk_download", Context.MODE_PRIVATE)
        preferences.edit().putString("active", "not JSON").commit()
        val backend = AndroidApkDownloadBackend(app)
        assertNull(backend.restore())
        val record = backend.enqueue(target)
        val stored = requireNotNull(preferences.getString("active", null))
        preferences.edit().putString("active", stored.replace(record.filename, "../../private.apk")).commit()
        assertNull(backend.restore())
        preferences.edit().putString("active", stored.replace("github.com", "unrelated.example")).commit()
        assertNull(backend.restore())
    }

    @Suppress("DEPRECATION")
    @Test fun `reopening after installation removes the old update record instead of reporting a failed update`() {
        val backend = AndroidApkDownloadBackend(app)
        val record = backend.enqueue(target)
        apkFile(record).writeText("test")
        val installed = app.packageManager.getPackageInfo(app.packageName, 0)
        installed.versionName = target.versionName
        shadowOf(app.packageManager).installPackage(installed)
        assertNull(AndroidApkDownloadBackend(app).restore())
        assertNull(shadowOf(manager).getRequest(record.id))
        assertFalse(apkFile(record).exists())
    }

    private fun apkFile(record: ApkDownloadRecord) = File(app.getExternalFilesDirs(null).first(),
        "${Environment.DIRECTORY_DOWNLOADS}/updates/${record.filename}")
}
