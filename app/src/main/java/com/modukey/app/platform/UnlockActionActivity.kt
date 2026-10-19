package com.modukey.app.platform

import android.app.KeyguardManager
import android.content.*
import android.os.Bundle
import android.os.PowerManager
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.modukey.app.NudgeApplication
import com.modukey.app.R
import com.modukey.app.core.ButtonAction

/** Internal one-shot authentication bridge. Never wakes the screen or bypasses keyguard. */
class UnlockActionActivity : ComponentActivity() {
    private val app get() = application as NudgeApplication
    private var token = ""
    private var registered = false
    private var unlocked = false
    private var pickerRequested = false
    private var pickerLostFocus = false
    private var requireLockAvailability = true
    private val pickerFallback = Runnable {
        if (!pickerLostFocus) { Toast.makeText(this, R.string.input_method_picker_failed, Toast.LENGTH_SHORT).show(); finish() }
    }
    private val complete = Runnable { completeRequest() }
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { app.unlocks.cancel(token); finish() }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        token = intent.getStringExtra("token").orEmpty()
        if (!app.unlocks.contains(token) || !getSystemService(PowerManager::class.java).isInteractive) { app.unlocks.cancel(token); finish(); return }
        setShowWhenLocked(true)
        setTurnScreenOn(false)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        setContentView(View(this).apply { isFocusableInTouchMode = true; requestFocus() })
        ContextCompat.registerReceiver(this, receiver, IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_NOT_EXPORTED)
        registered = true
        val keyguard = getSystemService(KeyguardManager::class.java)
        requireLockAvailability = app.unlocks.peek(token)?.fromLockScreen != false || keyguard.isKeyguardLocked
        if (!keyguard.isKeyguardLocked) { unlocked = true; scheduleCompletion(); return }
        keyguard.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
            override fun onDismissSucceeded() {
                unlocked = true; scheduleCompletion()
            }
            override fun onDismissCancelled() { app.unlocks.cancel(token); finish() }
            override fun onDismissError() { app.unlocks.cancel(token); finish() }
        })
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (pickerRequested) {
            if (!hasFocus) { pickerLostFocus = true; window.decorView.removeCallbacks(pickerFallback) }
            else if (pickerLostFocus) finish()
        } else if (hasFocus) scheduleCompletion()
    }
    private fun scheduleCompletion() {
        if (!unlocked || isFinishing || pickerRequested) return
        val picker = app.unlocks.peek(token)?.button?.action == ButtonAction.INPUT_METHOD_PICKER
        if (!picker || hasWindowFocus()) {
            window.decorView.removeCallbacks(complete)
            // ViewRoot reports the focused input-method client before this posted action runs.
            window.decorView.post(complete)
        }
    }
    private fun completeRequest() {
        if (isFinishing || pickerRequested) return
        if (app.unlocks.peek(token)?.button?.action == ButtonAction.INPUT_METHOD_PICKER && !hasWindowFocus()) return
        val request = app.unlocks.take(token)
        val config = if (request?.testing == true) app.configuration.preview.value else app.configuration.state.value
        val handle = config?.handle(request?.handleId)
        val button = config?.let { request?.currentButton(it) }
        val picker = button?.action == ButtonAction.INPUT_METHOD_PICKER
        val available = request != null && config != null && button != null && app.status.state.value.connected &&
            (request.shortcutId != null || request.testing || (handle != null && config.enabled && handle.enabled && (!requireLockAvailability || handle.trigger.lockScreenEnabled))) &&
            getSystemService(PowerManager::class.java).isInteractive && !getSystemService(KeyguardManager::class.java).isKeyguardLocked
        if (!available) { finish(); return }
        try {
            if (picker) {
                pickerRequested = true
                getSystemService(InputMethodManager::class.java).showInputMethodPicker()
                // A suppressed system popup must not leave a transparent activity in front.
                window.decorView.postDelayed(pickerFallback, 2_000)
                return
            }
            PanelActions.launchUnlocked(this, requireNotNull(button), requireNotNull(request).policy, request.writeSettings)
        } catch (_: RuntimeException) {
            Toast.makeText(this, if (picker) R.string.input_method_picker_failed else R.string.action_failed, Toast.LENGTH_SHORT).show()
        }
        finish()
    }
    override fun onStop() {
        super.onStop()
        if (unlocked && !isChangingConfigurations) finish()
    }
    override fun onDestroy() {
        window.decorView.removeCallbacks(complete)
        window.decorView.removeCallbacks(pickerFallback)
        if (isFinishing) app.unlocks.cancel(token)
        if (registered) { registered = false; try { unregisterReceiver(receiver) } catch (_: RuntimeException) { } }
        super.onDestroy()
    }
}
