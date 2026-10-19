package com.modukey.app.updates

data class ApkIdentity(val packageName: String, val versionName: String?, val versionCode: Long, val signers: Set<String>)

internal fun checkApkIdentity(installed: ApkIdentity, downloaded: ApkIdentity?, target: ApkTarget): ApkDownloadFailure? = when {
    installed.packageName != "com.modukey.app" -> ApkDownloadFailure.DEBUG_BUILD
    downloaded == null -> ApkDownloadFailure.INVALID_APK
    downloaded.packageName != installed.packageName -> ApkDownloadFailure.PACKAGE
    downloaded.versionName != target.versionName || downloaded.versionCode <= installed.versionCode -> ApkDownloadFailure.VERSION
    installed.signers.isEmpty() || downloaded.signers != installed.signers -> ApkDownloadFailure.SIGNATURE
    else -> null
}
