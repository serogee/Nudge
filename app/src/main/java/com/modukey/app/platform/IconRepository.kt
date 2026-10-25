package com.modukey.app.platform

import android.content.ComponentName
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.util.LruCache
import android.util.Xml
import androidx.core.graphics.createBitmap
import com.modukey.app.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.security.MessageDigest

data class IconPackInfo(val packageName: String, val label: String)
private data class PackIndex(val stamp: Long, val mappings: Map<String, String>, val names: List<String>)
private data class IconKey(val pack: String, val drawable: String, val action: ButtonAction,
    val appPackage: String, val activity: String, val pixels: Int)
private data class CachedIcon(val version: String, val bitmap: Bitmap)

@SuppressLint("DiscouragedApi") // External icon packs identify resources by name, not this app's generated IDs.
class IconRepository(private val context: Context) {
    companion object {
        val PACK_ACTIONS = listOf("com.novalauncher.THEME", "org.adw.launcher.THEMES", "com.anddoes.launcher.THEME", "com.teslacoilsw.launcher.THEME")
        const val CACHE_BYTES = 8 * 1024 * 1024
        internal const val DISK_BYTES = 16L * 1024 * 1024
        private val SIZES = listOf(48, 96, 144, 192, 288)
    }
    private val bitmaps = object : LruCache<IconKey, CachedIcon>(CACHE_BYTES) {
        override fun sizeOf(key: IconKey, value: CachedIcon) = value.bitmap.allocationByteCount
    }
    private val directory = File(context.cacheDir, "button-icons-v1")
    private val indices = linkedMapOf<String, PackIndex>()
    private val lock = Mutex()
    private fun stamp(pkg: String): Long = if (pkg.isBlank()) -1 else try { @Suppress("DEPRECATION") context.packageManager.getPackageInfo(pkg, 0).lastUpdateTime } catch (_: PackageManager.NameNotFoundException) { -1 }
    private fun key(button: PanelButton, pack: String, size: Int) = IconKey(button.icon.packageName.ifBlank { pack },
        button.icon.drawable, button.action, button.appPackage, button.appActivity,
        SIZES.firstOrNull { it >= size } ?: SIZES.last())

    /** Memory only: a new composition can show artwork before its background version check. */
    fun cached(button: PanelButton, defaultPack: String, size: Int): Bitmap? {
        val request = key(button, defaultPack, size)
        val sizes = listOf(request.pixels) + SIZES.filter { it > request.pixels } + SIZES.filter { it < request.pixels }.reversed()
        return sizes.firstNotNullOfOrNull { bitmaps.get(request.copy(pixels = it))?.bitmap }
    }

    fun invalidate(packageName: String) {
        bitmaps.snapshot().keys.filter { it.pack == packageName || it.appPackage == packageName }.forEach(bitmaps::remove)
    }

    suspend fun preload(config: AppConfiguration) {
        val buttons = config.panels.flatMap { panel -> panel.settings.buttons.map { it to panel.settings.iconPack } } +
            config.handles.flatMap { handle -> handle.bindings.values.mapNotNull { it.action }.map { it to "" } } +
            config.shortcuts.map { it.button to "" }
        for ((button, pack) in buttons.distinctBy { key(it.first, it.second, 192) }) {
            if (button.icon.builtIn == null && (button.action == ButtonAction.OPEN_APP || button.icon.packageName.isNotBlank() || pack.isNotBlank()))
                icon(button, pack, 192)
            kotlinx.coroutines.yield()
        }
    }

    suspend fun packs(): List<IconPackInfo> = withContext(Dispatchers.IO) {
        PACK_ACTIONS.flatMap { action ->
            @Suppress("DEPRECATION")
            context.packageManager.queryIntentActivities(Intent(action), PackageManager.MATCH_ALL)
        }.distinctBy { it.activityInfo.packageName }.map { IconPackInfo(it.activityInfo.packageName, it.loadLabel(context.packageManager).toString()) }.sortedBy { it.label.lowercase() }
    }
    suspend fun names(pack: String): List<String> = withContext(Dispatchers.IO) { lock.withLock { index(pack)?.names ?: emptyList() } }
    suspend fun available(pack: String): Boolean = withContext(Dispatchers.IO) { pack.isBlank() || stamp(pack) >= 0 }

    suspend fun icon(button: PanelButton, defaultPack: String, size: Int): Bitmap? = withContext(Dispatchers.IO) {
        lock.withLock {
            val request = key(button, defaultPack, size)
            val pixels = request.pixels
            val pack = request.pack
            val component = if (button.appActivity.isNotBlank()) ComponentName(button.appPackage, button.appActivity).flattenToString() else ""
            val version = "${stamp(pack)}:${stamp(button.appPackage)}"
            for (candidate in SIZES.filter { it >= pixels }) {
                bitmaps.get(request.copy(pixels = candidate))?.takeIf { it.version == version }?.let { return@withLock it.bitmap }
            }
            try {
                val file = diskFile(request, version)
                read(file, pixels)?.let { bitmap -> bitmaps.put(request, CachedIcon(version, bitmap)); return@withLock bitmap }
                val index = if (pack.isNotBlank()) index(pack) else null
                val name = button.icon.drawable.ifBlank { index?.mappings?.get(component).orEmpty() }
                val drawable = if (pack.isNotBlank() && name.isNotBlank()) {
                    val res = context.packageManager.getResourcesForApplication(pack)
                    val id = res.getIdentifier(name, "drawable", pack)
                    if (id != 0) res.getDrawable(id, null) else null
                } else null
                val fallback = drawable ?: if (button.action == ButtonAction.OPEN_APP && button.appPackage.isNotBlank()) {
                    try { if (button.appActivity.isNotBlank()) context.packageManager.getActivityIcon(ComponentName(button.appPackage, button.appActivity)) else context.packageManager.getApplicationIcon(button.appPackage) }
                    catch (_: PackageManager.NameNotFoundException) { context.packageManager.getApplicationIcon(button.appPackage) }
                } else null
                fallback ?: return@withLock null
                createBitmap(pixels, pixels).also { bitmap ->
                    fallback.setBounds(0, 0, pixels, pixels); fallback.draw(Canvas(bitmap))
                    bitmaps.put(request, CachedIcon(version, bitmap)); write(file, bitmap)
                }
            } catch (_: Exception) { null }
        }
    }
    private fun diskFile(key: IconKey, version: String): File {
        val hash = MessageDigest.getInstance("SHA-256").digest("$key:$version".toByteArray()).joinToString("") { "%02x".format(it) }
        return File(directory, "$hash.png")
    }
    private fun read(file: File, pixels: Int): Bitmap? = if (!file.isFile) null else try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth != pixels || bounds.outHeight != pixels) { file.delete(); null }
        else BitmapFactory.decodeFile(file.path)?.also { file.setLastModified(System.currentTimeMillis()) }
    } catch (_: Exception) { file.delete(); null }
    private fun write(file: File, bitmap: Bitmap) {
        var temporary: File? = null
        try {
            if (!directory.isDirectory && !directory.mkdirs()) return
            temporary = File.createTempFile("icon-", ".tmp", directory)
            val saved = temporary.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            if (saved) temporary.renameTo(file)
            var bytes = 0L
            directory.listFiles()?.filter { it.extension == "png" }?.sortedByDescending(File::lastModified)?.forEach {
                bytes += it.length(); if (bytes > DISK_BYTES) it.delete()
            }
        } catch (_: Exception) { /* Cache storage is optional. Keep the loaded artwork. */ }
        finally { temporary?.delete() }
    }
    private fun index(pack: String): PackIndex? {
        val stamp = stamp(pack)
        if (stamp < 0) { indices.remove(pack); return null }
        indices[pack]?.takeIf { it.stamp == stamp }?.let { return it }
        val mappings = linkedMapOf<String, String>(); val names = linkedSetOf<String>()
        try {
            val res = context.packageManager.getResourcesForApplication(pack)
            fun parse(parser: XmlPullParser) {
                var event = parser.eventType
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG) {
                        val drawable = parser.getAttributeValue(null, "drawable")
                        if (!drawable.isNullOrBlank()) {
                            names.add(drawable)
                            parser.getAttributeValue(null, "component")?.removePrefix("ComponentInfo{")?.removeSuffix("}")?.let { raw ->
                                ComponentName.unflattenFromString(raw)?.let { mappings[it.flattenToString()] = drawable }
                            }
                        }
                    }
                    event = parser.next()
                }
            }
            for (file in listOf("appfilter", "drawable")) {
                val id = res.getIdentifier(file, "xml", pack)
                if (id != 0) res.getXml(id).use { parse(it) }
                else try { res.assets.open("$file.xml").use { input -> parse(Xml.newPullParser().apply { setInput(input, "UTF-8") }) } } catch (_: Exception) { }
            }
        } catch (_: Exception) { return null }
        val result = PackIndex(stamp, mappings, names.sorted())
        indices[pack] = result
        while (indices.size > 4) indices.remove(indices.keys.first())
        return result
    }
}
