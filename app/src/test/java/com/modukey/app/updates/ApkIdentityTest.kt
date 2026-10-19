package com.modukey.app.updates

import org.junit.Assert.*
import org.junit.Test

class ApkIdentityTest {
    private val target = ApkTarget("v0.3.2", "https://github.com/serogee/Nudge/releases/download/v0.3.2/Nudge-0.3.2.apk")
    private val installed = ApkIdentity("com.modukey.app", "0.3.1", 8, setOf("release-key"))
    private val update = installed.copy(versionName = "0.3.2", versionCode = 9)

    @Test fun `a newer matching Nudge APK is accepted`() {
        assertNull(checkApkIdentity(installed, update, target))
    }

    @Test fun `missing packages mismatched versions and non-increasing codes are rejected`() {
        assertEquals(ApkDownloadFailure.INVALID_APK, checkApkIdentity(installed, null, target))
        assertEquals(ApkDownloadFailure.PACKAGE, checkApkIdentity(installed, update.copy(packageName = "another.app"), target))
        listOf(update.copy(versionName = "0.3.3"), update.copy(versionName = null), update.copy(versionCode = 8), update.copy(versionCode = 7)).forEach {
            assertEquals(ApkDownloadFailure.VERSION, checkApkIdentity(installed, it, target))
        }
    }

    @Test fun `signers must match the existing release signing identity`() {
        listOf(emptySet(), setOf("different-key"), setOf("release-key", "different-key")).forEach {
            assertEquals(ApkDownloadFailure.SIGNATURE, checkApkIdentity(installed, update.copy(signers = it), target))
        }
        assertEquals(ApkDownloadFailure.SIGNATURE, checkApkIdentity(installed.copy(signers = emptySet()), update, target))
    }

    @Test fun `release APKs cannot overwrite the separate debug app`() {
        assertEquals(ApkDownloadFailure.DEBUG_BUILD, checkApkIdentity(installed.copy(packageName = "com.modukey.app.debug"), update, target))
    }
}
