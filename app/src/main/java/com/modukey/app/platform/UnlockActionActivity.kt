package com.modukey.app.platform

import android.app.KeyguardManager
import android.content.*
import android.os.Bundle
import android.os.PowerManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.modukey.app.NudgeApplication
import com.modukey.app.R

/** Internal one-shot authentication bridge. Never wakes the screen or bypasses keyguard. */
class UnlockActionActivity : ComponentActivity() {
    private val app get() = application as NudgeApplication
    private var token = ""
    private var registered = false
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { app.unlocks.cancel(token); finish() }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        token = intent.getStringExtra("token").orEmpty()
        if (!app.unlocks.contains(token) || !getSystemService(PowerManager::class.java).isInteractive) { app.unlocks.cancel(token); finish(); return }
        setShowWhenLocked(true)
        setTurnScreenOn(false)
        ContextCompat.registerReceiver(this, receiver, IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_NOT_EXPORTED)
        registered = true
        getSystemService(KeyguardManager::class.java).requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
            override fun onDismissSucceeded() {
                val request = app.unlocks.take(token)
                val config = app.configuration.state.value
                val handle = config?.handle(request?.handleId)
                val button = config?.let { request?.currentButton(it) }
                if (request != null && config != null && button != null && button == request.button && app.status.state.value.connected &&
                    (request.testing || (handle != null && config.enabled && handle.enabled && handle.trigger.lockScreenEnabled)) &&
                    getSystemService(PowerManager::class.java).isInteractive && !getSystemService(KeyguardManager::class.java).isKeyguardLocked) {
                    try { PanelActions.launchUnlocked(this@UnlockActionActivity, button, request.policy) }
                    catch (_: RuntimeException) { Toast.makeText(this@UnlockActionActivity, R.string.action_failed, Toast.LENGTH_SHORT).show() }
                }
                finish()
            }
            override fun onDismissCancelled() { app.unlocks.cancel(token); finish() }
            override fun onDismissError() { app.unlocks.cancel(token); finish() }
        })
    }
    override fun onDestroy() {
        if (isFinishing) app.unlocks.cancel(token)
        if (registered) { registered = false; try { unregisterReceiver(receiver) } catch (_: RuntimeException) { } }
        super.onDestroy()
    }
}
