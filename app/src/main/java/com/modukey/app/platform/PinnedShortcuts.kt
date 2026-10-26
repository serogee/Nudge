package com.modukey.app.platform

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Canvas
import android.graphics.drawable.Icon
import androidx.core.graphics.createBitmap
import com.modukey.app.MainActivity
import com.modukey.app.R
import com.modukey.app.core.HomeShortcut
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Launcher calls and icon loading run only after an explicit Home action. */
class PinnedShortcuts(private val context: Context, private val icons: IconRepository) {
    private val manager get() = context.getSystemService(ShortcutManager::class.java)
    suspend fun supported(): Boolean = withContext(Dispatchers.IO) { runCatching { manager.isRequestPinShortcutSupported }.getOrDefault(false) }
    suspend fun pinned(): Set<String> = withContext(Dispatchers.IO) { runCatching { manager.pinnedShortcuts.filter { it.isEnabled }.mapTo(mutableSetOf()) { it.id } }.getOrDefault(emptySet()) }
    suspend fun pin(shortcut: HomeShortcut, label: String): Boolean = withContext(Dispatchers.IO) {
        if (!manager.isRequestPinShortcutSupported) return@withContext false
        val image = ShortcutIconRenderer.render(context, icons, shortcut.button)
        // Keep the complete square artwork inside the adaptive icon's circular safe area.
        val adaptiveImage = createBitmap(image.width * 2, image.height * 2).also {
            Canvas(it).drawBitmap(image, image.width / 2f, image.height / 2f, null)
        }
        image.recycle()
        val info = ShortcutInfo.Builder(context, shortcut.id).setShortLabel(label.take(80)).setLongLabel(label.take(80))
            .setActivity(ComponentName(context, MainActivity::class.java))
            .setIcon(Icon.createWithAdaptiveBitmap(adaptiveImage))
            .setIntent(Intent(context, ShortcutActionActivity::class.java).setAction(Intent.ACTION_VIEW)
                .putExtra(ShortcutActionActivity.EXTRA_ID, shortcut.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS))
            .build()
        manager.enableShortcuts(listOf(shortcut.id))
        if (manager.pinnedShortcuts.any { it.id == shortcut.id } && !manager.updateShortcuts(listOf(info))) return@withContext false
        manager.requestPinShortcut(info, null)
    }
    suspend fun disable(id: String) = withContext(Dispatchers.IO) { manager.disableShortcuts(listOf(id), context.getString(R.string.shortcut_missing)) }
}
