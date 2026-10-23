package com.modukey.app.platform

import android.util.AtomicFile
import android.util.Log
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Model Android's POSIX atomic replacement on Windows test hosts. */
@Implements(AtomicFile::class)
class AtomicFilePlatformShadow {
    companion object {
        @JvmStatic @Implementation fun rename(source: File, target: File) {
            if (target.isDirectory) target.delete()
            try { Files.move(source.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING) }
            catch (error: IOException) { Log.e("AtomicFile", "Atomic replacement failed", error) }
        }
    }
}
