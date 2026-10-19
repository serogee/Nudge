package com.modukey.app.platform

import android.app.KeyguardManager
import android.content.*
import android.os.Bundle
import android.os.PowerManager
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.modukey.app.NudgeApplication
import com.modukey.app.R
import com.modukey.app.core.HomeShortcut
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** The exported launcher entry accepts only a saved ID. It cannot accept an arbitrary action. */
class ShortcutActionActivity : ComponentActivity() {
    private val app get() = application as NudgeApplication
    private var selected: HomeShortcut? = null
    private var registered = false
    private var authenticationPending = false
    private var dispatched = false
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { finish() }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.takeIf { it.action == Intent.ACTION_VIEW }?.getStringExtra(EXTRA_ID)
        if (id.isNullOrBlank() || id.length > 128 || !awake()) { finish(); return }
        setShowWhenLocked(true); setTurnScreenOn(false); setContentView(View(this))
        ContextCompat.registerReceiver(this, receiver, IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_NOT_EXPORTED)
        registered = true
        lifecycleScope.launch {
            val config = withTimeoutOrNull(5_000) { app.configuration.state.filterNotNull().first() }
            if (isFinishing) return@launch
            selected = config?.shortcuts?.firstOrNull { it.id == id }?.takeIf { it.available(config) }
            if (selected == null) { report(R.string.shortcut_missing); return@launch }
            val keyguard = getSystemService(KeyguardManager::class.java)
            if (keyguard.isKeyguardLocked) {
                authenticationPending = true
                keyguard.requestDismissKeyguard(this@ShortcutActionActivity, object : KeyguardManager.KeyguardDismissCallback() {
                    override fun onDismissSucceeded() { authenticationPending = false; dispatch() }
                    override fun onDismissCancelled() { authenticationPending = false; finish() }
                    override fun onDismissError() { authenticationPending = false; finish() }
                })
            } else dispatch()
        }
    }
    private fun awake() = getSystemService(PowerManager::class.java).isInteractive
    private fun dispatch() {
        if (dispatched || isFinishing || !awake() || getSystemService(KeyguardManager::class.java).isKeyguardLocked) { finish(); return }
        val shortcut = selected
        val config = app.configuration.state.value
        if (shortcut == null || config?.shortcuts?.firstOrNull { it.id == shortcut.id } != shortcut || !shortcut.available(config)) { report(R.string.shortcut_missing); return }
        dispatched = true
        if (!app.shortcuts.run(shortcut.id)) { report(R.string.shortcut_needs_service); return }
        finish()
    }
    private fun report(message: Int) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); finish() }
    override fun onStop() { super.onStop(); if (!authenticationPending && !isChangingConfigurations) finish() }
    override fun onDestroy() {
        if (registered) { registered = false; runCatching { unregisterReceiver(receiver) } }
        super.onDestroy()
    }
    companion object { const val EXTRA_ID = "shortcut_id" }
}
