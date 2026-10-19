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

data class SoundState(val volume: Int = 0, val muted: Boolean = false, val ringer: Int = AudioManager.RINGER_MODE_NORMAL, val dnd: Boolean = false, val ownDnd: Boolean = false,
    val maximumBrightness: Boolean = false, val minimumBrightness: Boolean = false, val enabledHandleIds: Set<String> = emptySet(),
    val pausedHandleIds: Set<String> = emptySet(), val ownerHandleId: String? = null, val handlesEnabled: Boolean = false, val automaticBrightness: Boolean = false)
data class ActionResult(val close: Boolean = false, val message: Int? = null, val succeeded: Boolean = true)
interface ActionExecutor {
    fun execute(handleId: String, button: PanelButton, testing: Boolean, gestureSlot: GestureSlot? = null): ActionResult
    fun executeShortcut(shortcut: HomeShortcut): ActionResult = execute(shortcut.handleId.orEmpty(), shortcut.button, false)
    fun showBrightness(handleId: String, slot: GestureSlot): ActionResult = ActionResult(succeeded = false)
    fun snapshot(): SoundState
}
class PanelActions(private val service: AccessibilityService,
    private val screenshot: () -> Boolean = { service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT) },
    private val systemPanel: (Int) -> Boolean = { service.performGlobalAction(it) },
    private val brightnessPopup: (String) -> Boolean = { false }) : ActionExecutor {
    private var shortcutSource: HomeShortcut? = null
    private val app get() = service.application as NudgeApplication
    private val audio = service.getSystemService(AudioManager::class.java)
    private val notification = service.getSystemService(NotificationManager::class.java)
    override fun snapshot(): SoundState = try {
        val dnd = notification.currentInterruptionFilter in listOf(NotificationManager.INTERRUPTION_FILTER_PRIORITY, NotificationManager.INTERRUPTION_FILTER_ALARMS, NotificationManager.INTERRUPTION_FILTER_NONE)
        val own = if (Build.VERSION.SDK_INT >= 35 && notification.isNotificationPolicyAccessGranted) {
            notification.automaticZenRules.keys.any { notification.getAutomaticZenRuleState(it) == Condition.STATE_TRUE }
        } else dnd
        val brightness = app.brightness.status()
        val config = app.configuration.state.value
        SoundState(audio.getStreamVolume(AudioManager.STREAM_MUSIC), audio.isStreamMute(AudioManager.STREAM_MUSIC), audio.ringerMode, dnd, own,
            brightness.maximum, brightness.minimum, config?.handles?.filter { it.enabled }?.mapTo(mutableSetOf()) { it.id }.orEmpty(),
            app.handlePauses.state.value, handlesEnabled = config?.enabled == true, automaticBrightness = brightness.automatic)
    } catch (_: RuntimeException) { SoundState() }

    override fun execute(handleId: String, button: PanelButton, testing: Boolean, gestureSlot: GestureSlot?): ActionResult = try {
        when (button.action) {
            ButtonAction.EMPTY -> ActionResult()
            ButtonAction.CLOSE_PANELS -> ActionResult(close = true)
            ButtonAction.SCREENSHOT -> if (screenshot()) ActionResult(close = true)
                else ActionResult(message = R.string.screenshot_failed, succeeded = false)
            ButtonAction.NOTIFICATIONS, ButtonAction.CONTROL_PANEL ->
                if (systemPanel(if (button.action == ButtonAction.NOTIFICATIONS) AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS else AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)) ActionResult(close = true)
                else ActionResult(message = R.string.native_panel_failed, succeeded = false)
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
            ButtonAction.BRIGHTNESS_MAXIMUM, ButtonAction.BRIGHTNESS_MINIMUM, ButtonAction.BRIGHTNESS_UP, ButtonAction.BRIGHTNESS_DOWN, ButtonAction.BRIGHTNESS_AUTOMATIC -> {
                val result = if (button.action == ButtonAction.BRIGHTNESS_AUTOMATIC) app.brightness.toggleAutomatic()
                    else if (button.action in listOf(ButtonAction.BRIGHTNESS_UP, ButtonAction.BRIGHTNESS_DOWN)) app.brightness.adjust(button.action == ButtonAction.BRIGHTNESS_UP)
                    else app.brightness.toggle(if (button.action == ButtonAction.BRIGHTNESS_MAXIMUM) BrightnessPreset.MAXIMUM else BrightnessPreset.MINIMUM, button.brightnessReturn)
                when (result) {
                    BrightnessResult.CHANGED -> ActionResult()
                    BrightnessResult.ALREADY_SET -> ActionResult(message = R.string.brightness_already_set)
                    BrightnessResult.NEED_ACCESS -> {
                        launch(handleId, button, testing, gestureSlot = gestureSlot, writeSettings = true)
                        ActionResult(close = true, message = R.string.brightness_access_required)
                    }
                    BrightnessResult.FAILED -> ActionResult(message = R.string.brightness_failed, succeeded = false)
                }
            }
            ButtonAction.TOGGLE_HANDLES -> {
                val config = app.configuration.state.value
                if (config != null && app.handlePauses.toggle(button.handleScope, handleId.takeIf { it.isNotBlank() }, config)) ActionResult()
                else ActionResult(message = R.string.handle_toggle_unavailable, succeeded = false)
            }
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

    override fun executeShortcut(shortcut: HomeShortcut): ActionResult {
        val config = app.configuration.state.value
        if (config?.shortcuts?.firstOrNull { it.id == shortcut.id } != shortcut || !shortcut.available(config)) return ActionResult(message = R.string.shortcut_missing, succeeded = false)
        shortcutSource = shortcut
        return try { execute(shortcut.handleId.orEmpty(), shortcut.button, false) } finally { shortcutSource = null }
    }
    override fun showBrightness(handleId: String, slot: GestureSlot): ActionResult {
        val target = app.configuration.state.value?.handle(handleId)?.target(slot) ?: return ActionResult(succeeded = false)
        if (!target.brightness) return ActionResult(succeeded = false)
        if (!app.brightness.canWrite) {
            launch(handleId, PanelButton(id = "brightness-permission", action = ButtonAction.BRIGHTNESS_UP), false,
                gestureSlot = slot, writeSettings = true, brightnessTarget = target)
            return ActionResult(message = R.string.brightness_access_required, succeeded = false)
        }
        return if (brightnessPopup(handleId)) ActionResult() else ActionResult(message = R.string.action_failed, succeeded = false)
    }
    private fun launch(handleId: String, button: PanelButton, testing: Boolean, policy: Boolean = false, gestureSlot: GestureSlot? = null, writeSettings: Boolean = false, brightnessTarget: GestureTarget? = null) {
        if (!service.getSystemService(PowerManager::class.java).isInteractive) return
        val locked = service.getSystemService(KeyguardManager::class.java).isKeyguardLocked
        if (button.action == ButtonAction.INPUT_METHOD_PICKER || locked) {
            val token = app.unlocks.put(PendingLaunch(handleId, button, testing, policy, gestureSlot, fromLockScreen = locked, writeSettings = writeSettings, brightnessTarget = brightnessTarget, shortcutId = shortcutSource?.id))
            service.startActivity(Intent(service, UnlockActionActivity::class.java).putExtra("token", token)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or Intent.FLAG_ACTIVITY_NO_ANIMATION))
        } else launchUnlocked(service, button, policy, writeSettings)
    }
    companion object {
        fun launchUnlocked(context: Context, button: PanelButton, policy: Boolean = false, writeSettings: Boolean = false) {
            val intent = if (writeSettings) Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, android.net.Uri.parse("package:${context.packageName}"))
            else if (policy) Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS) else when (button.action) {
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
    val fromLockScreen: Boolean = true, val writeSettings: Boolean = false, val brightnessTarget: GestureTarget? = null, val shortcutId: String? = null) {
    // Recheck the exact source after authentication. Deleted or changed assignments must not launch.
    fun currentButton(config: AppConfiguration): PanelButton? {
        if (shortcutId != null) return config.shortcuts.firstOrNull { it.id == shortcutId && it.available(config) }?.button?.takeIf { it == button }
        if (brightnessTarget != null) return button.takeIf { writeSettings && gestureSlot != null && brightnessTarget.brightness && config.handle(handleId)?.target(gestureSlot) == brightnessTarget }
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
