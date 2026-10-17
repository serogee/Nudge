package com.modukey.app.platform

import android.content.ComponentName
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
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

data class IconPackInfo(val packageName: String, val label: String)
private data class PackIndex(val stamp: Long, val mappings: Map<String, String>, val names: List<String>)

@SuppressLint("DiscouragedApi") // External icon packs identify resources by name, not this app's generated IDs.
class IconRepository(private val context: Context) {
    companion object {
        val PACK_ACTIONS = listOf("com.novalauncher.THEME", "org.adw.launcher.THEMES", "com.anddoes.launcher.THEME", "com.teslacoilsw.launcher.THEME")
        const val CACHE_BYTES = 8 * 1024 * 1024
    }
    private val bitmaps = object : LruCache<String, Bitmap>(CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }
    private val indices = linkedMapOf<String, PackIndex>()
    private val lock = Mutex()
    private fun stamp(pkg: String): Long = try { @Suppress("DEPRECATION") context.packageManager.getPackageInfo(pkg, 0).lastUpdateTime } catch (_: PackageManager.NameNotFoundException) { -1 }

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
            val pixels = size.coerceIn(24, 288)
            val pack = button.icon.packageName.ifBlank { defaultPack }
            val component = if (button.appActivity.isNotBlank()) ComponentName(button.appPackage, button.appActivity).flattenToString() else ""
            val key = listOf(pack, stamp(pack), button.icon.drawable, button.action, button.appPackage, button.appActivity, stamp(button.appPackage), pixels).joinToString("|")
            bitmaps.get(key)?.let { return@withLock it }
            try {
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
                    fallback.setBounds(0, 0, pixels, pixels); fallback.draw(Canvas(bitmap)); bitmaps.put(key, bitmap)
                }
            } catch (_: Exception) { null }
        }
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
