package com.modukey.app.platform

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
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
        val info = ShortcutInfo.Builder(context, shortcut.id).setShortLabel(label.take(80)).setLongLabel(label.take(80))
            .setActivity(ComponentName(context, MainActivity::class.java))
            .setIcon(Icon.createWithBitmap(image))
            .setIntent(Intent(context, ShortcutActionActivity::class.java).setAction(Intent.ACTION_VIEW)
                .putExtra(ShortcutActionActivity.EXTRA_ID, shortcut.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS))
            .build()
        manager.enableShortcuts(listOf(shortcut.id))
        manager.requestPinShortcut(info, null)
    }
    suspend fun disable(id: String) = withContext(Dispatchers.IO) { manager.disableShortcuts(listOf(id), context.getString(R.string.shortcut_missing)) }
}
