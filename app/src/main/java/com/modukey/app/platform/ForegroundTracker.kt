package com.modukey.app.platform

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
import com.modukey.app.MainActivity

internal data class ForegroundObservation(val previous: String?, val current: String) {
    val switched get() = previous != null && previous != current
}

/** Window-state events also describe panes and dialogs. Only resolved app activities establish a baseline. */
internal class ForegroundTracker(private val packages: PackageManager, private val ownPackage: String) {
    var packageName: String? = null
        private set
    private var tracking = false
    private var lastEventTime = 0L
    private val activities = BoundedCache<ComponentName>(64)
    private val applications = BoundedCache<String>(32)

    fun track(enabled: Boolean, now: Long) {
        if (tracking == enabled) return
        tracking = enabled
        packageName = null
        lastEventTime = now
        activities.clear(); applications.clear()
    }

    fun observe(event: AccessibilityEvent, now: Long, panelOpenedAt: Long?): ForegroundObservation? {
        if (!tracking || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || event.contentChangeTypes != 0) return null
        // AccessibilityManager stamps events with uptimeMillis, not elapsedRealtime (which includes sleep).
        if (event.eventTime <= 0 || event.eventTime > now || event.eventTime < lastEventTime ||
            (panelOpenedAt != null && event.eventTime < panelOpenedAt)) return null
        val pkg = event.packageName?.toString()?.takeIf { it.isNotBlank() && it.length <= 256 } ?: return null
        val name = event.className?.toString()?.takeIf { it.isNotBlank() && it.length <= 512 } ?: return null
        if (pkg == "android" || pkg == "com.android.systemui") return null
        if (pkg == ownPackage) {
            if (name != MainActivity::class.java.name) return null
        } else {
            val component = ComponentName(pkg, if (name.startsWith('.')) pkg + name else name)
            if (!activities.getOrPut(component) { isActivity(component) } || !applications.getOrPut(pkg) { isUserApp(pkg) }) return null
        }
        val observation = ForegroundObservation(packageName, pkg)
        packageName = pkg; lastEventTime = event.eventTime
        return observation
    }

    private fun isActivity(component: ComponentName): Boolean = try {
        @Suppress("DEPRECATION")
        packages.getActivityInfo(component, 0).enabled
    } catch (_: PackageManager.NameNotFoundException) { false }
    catch (_: RuntimeException) { false }

    private fun isUserApp(pkg: String): Boolean = try {
        // A declared activity in a volume plugin or permission dialog is not sufficient evidence of an app switch.
        packages.getLaunchIntentForPackage(pkg) != null ||
            packages.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)?.activityInfo?.packageName == pkg
    } catch (_: RuntimeException) { false }

    private class BoundedCache<K>(private val limit: Int) : LinkedHashMap<K, Boolean>(limit, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, Boolean>?) = size > limit
    }
}
