package com.modukey.app.platform

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.*
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.service.notification.Condition
import android.widget.Toast
import com.modukey.app.MainActivity
import com.modukey.app.NudgeApplication
import com.modukey.app.R
import com.modukey.app.core.*

data class SoundState(val volume: Int = 0, val muted: Boolean = false, val ringer: Int = AudioManager.RINGER_MODE_NORMAL, val dnd: Boolean = false, val ownDnd: Boolean = false)
data class ActionResult(val close: Boolean = false, val message: Int? = null, val succeeded: Boolean = true)
interface ActionExecutor {
    fun execute(handleId: String, button: PanelButton, testing: Boolean, gestureSlot: GestureSlot? = null): ActionResult
    fun snapshot(): SoundState
}
class PanelActions(private val service: AccessibilityService,
    private val screenshot: () -> Boolean = { service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT) }) : ActionExecutor {
    private val app get() = service.application as NudgeApplication
    private val audio = service.getSystemService(AudioManager::class.java)
    private val notification = service.getSystemService(NotificationManager::class.java)
    override fun snapshot(): SoundState = try {
        val dnd = notification.currentInterruptionFilter in listOf(NotificationManager.INTERRUPTION_FILTER_PRIORITY, NotificationManager.INTERRUPTION_FILTER_ALARMS, NotificationManager.INTERRUPTION_FILTER_NONE)
        val own = if (Build.VERSION.SDK_INT >= 35 && notification.isNotificationPolicyAccessGranted) {
            notification.automaticZenRules.keys.any { notification.getAutomaticZenRuleState(it) == Condition.STATE_TRUE }
        } else dnd
        SoundState(audio.getStreamVolume(AudioManager.STREAM_MUSIC), audio.isStreamMute(AudioManager.STREAM_MUSIC), audio.ringerMode, dnd, own)
    } catch (_: RuntimeException) { SoundState() }

    override fun execute(handleId: String, button: PanelButton, testing: Boolean, gestureSlot: GestureSlot?): ActionResult = try {
        when (button.action) {
            ButtonAction.EMPTY -> ActionResult()
            ButtonAction.CLOSE_PANELS -> ActionResult(close = true)
            ButtonAction.SCREENSHOT -> if (screenshot()) ActionResult(close = true)
                else ActionResult(message = R.string.screenshot_failed, succeeded = false)
            ButtonAction.PHONE_SETTINGS, ButtonAction.NUDGE_SETTINGS, ButtonAction.OPEN_APP, ButtonAction.INPUT_METHOD_PICKER -> {
                if (button.action == ButtonAction.INPUT_METHOD_PICKER && !service.packageManager.hasSystemFeature(PackageManager.FEATURE_INPUT_METHODS))
                    ActionResult(message = R.string.input_method_picker_failed, succeeded = false)
                else { launch(handleId, button, testing, gestureSlot = gestureSlot); ActionResult(close = true) }
            }
            ButtonAction.VOLUME_UP, ButtonAction.VOLUME_DOWN -> {
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, if (button.action == ButtonAction.VOLUME_UP) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
                    if (button.showVolumePopup) AudioManager.FLAG_SHOW_UI else 0)
                ActionResult()
            }
            ButtonAction.MEDIA_MUTE -> { audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, 0); ActionResult() }
            ButtonAction.SILENT, ButtonAction.VIBRATE, ButtonAction.DND -> {
                if (!notification.isNotificationPolicyAccessGranted) {
                    launch(handleId, button, testing, policy = true, gestureSlot = gestureSlot)
                    ActionResult(close = true, message = R.string.policy_required)
                } else {
                    when (button.action) {
                        ButtonAction.DND -> notification.setInterruptionFilter(if (snapshot().ownDnd) NotificationManager.INTERRUPTION_FILTER_ALL else NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                        else -> {
                            val mode = if (button.action == ButtonAction.SILENT) AudioManager.RINGER_MODE_SILENT else AudioManager.RINGER_MODE_VIBRATE
                            audio.ringerMode = if (audio.ringerMode == mode) AudioManager.RINGER_MODE_NORMAL else mode
                        }
                    }
                    ActionResult(message = if (button.action == ButtonAction.DND && Build.VERSION.SDK_INT >= 35) R.string.dnd_other_rule else null)
                }
            }
            ButtonAction.LOCK -> if (service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)) ActionResult(close = true) else ActionResult(message = R.string.action_failed, succeeded = false)
        }
    } catch (_: ActivityNotFoundException) { ActionResult(message = R.string.app_missing, succeeded = false) }
    catch (_: RuntimeException) { ActionResult(message = R.string.action_failed, succeeded = false) }

    private fun launch(handleId: String, button: PanelButton, testing: Boolean, policy: Boolean = false, gestureSlot: GestureSlot? = null) {
        if (!service.getSystemService(PowerManager::class.java).isInteractive) return
        val locked = service.getSystemService(KeyguardManager::class.java).isKeyguardLocked
        if (button.action == ButtonAction.INPUT_METHOD_PICKER || locked) {
            val token = app.unlocks.put(PendingLaunch(handleId, button, testing, policy, gestureSlot, fromLockScreen = locked))
            service.startActivity(Intent(service, UnlockActionActivity::class.java).putExtra("token", token)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or Intent.FLAG_ACTIVITY_NO_ANIMATION))
        } else launchUnlocked(service, button, policy)
    }
    companion object {
        fun launchUnlocked(context: Context, button: PanelButton, policy: Boolean = false) {
            val intent = if (policy) Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS) else when (button.action) {
                ButtonAction.PHONE_SETTINGS -> Intent(Settings.ACTION_SETTINGS)
                ButtonAction.NUDGE_SETTINGS -> Intent(context, MainActivity::class.java)
                ButtonAction.OPEN_APP -> {
                    val explicit = if (button.appActivity.isNotBlank()) Intent.makeMainActivity(ComponentName(button.appPackage, button.appActivity)) else null
                    if (explicit?.resolveActivity(context.packageManager) != null) explicit else context.packageManager.getLaunchIntentForPackage(button.appPackage)
                        ?: throw ActivityNotFoundException()
                }
                else -> return
            }
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

data class PendingLaunch(val handleId: String, val button: PanelButton, val testing: Boolean, val policy: Boolean, val gestureSlot: GestureSlot? = null,
    val fromLockScreen: Boolean = true) {
    // Recheck the exact source after authentication. Deleted or changed assignments must not launch.
    fun currentButton(config: AppConfiguration): PanelButton? {
        val current = if (gestureSlot != null) config.handle(handleId)?.target(gestureSlot)?.action
            else config.panels.asSequence().flatMap { it.settings.buttons }.firstOrNull { it.id == button.id }
        return current?.takeIf { it == button }
    }
}
class UnlockRequests {
    private val pending = mutableMapOf<String, PendingLaunch>()
    fun put(request: PendingLaunch): String { pending.clear(); return newId().also { pending[it] = request } }
    fun take(token: String) = pending.remove(token)
    fun contains(token: String) = token in pending
    fun peek(token: String) = pending[token]
    fun cancel(token: String) { pending.remove(token) }
    fun clear() = pending.clear()
}
