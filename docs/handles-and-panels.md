# Handles and panels

This guide covers the current development interface. Earlier release artifacts remain unchanged. Physical Redmi Note 12 / HyperOS checks remain pending.

## Handles

Open **Handles** to add, name, enable, duplicate, move, or delete a handle. New and duplicated handles start disabled. The first handle receives touches where touch areas overlap. The master switch on Home must also be on.

In the development interface, drag a card's left grip to change its priority. Select the pencil to edit or rename it. The handle title and tabs stay visible while the options scroll. A reset icon appears beside a changed setting. It restores only that setting.

Handle and panel editors keep changes in a temporary draft. The top pencil changes to Save and Cancel after an edit. Save applies all changes in that editor. Cancel restores its saved settings. Long titles use an ellipsis so both controls stay visible.

Cancel and Save share a pill-shaped control, with Cancel on the left. A gap separates the controls. Their compact height keeps the title container unchanged. Saving shows progress until storage responds. A toast confirms a successful save or cancellation. The editor stays open and the rename icon returns. Use Back to leave. Save errors retain the current settings and offer Retry.

Handle icons keep a bold initial inside a rounded square. A small inset pill marks the saved left, right, or bottom edge.

Drag a list grip to reorder handles, panels, or buttons. Hold near the top or bottom to scroll. The same item stays selected until release or cancellation. Touch outside the grip to scroll normally. Accessibility move actions remain available.

The launch icon uses a centered N with padding inside a rounded container. Its side pills reveal from their centers. The middle pill reveals upright, then rotates as the side pills move into position. The native sequence ends after 600 ms. Disabled system animations skip the wait. Backgrounding removes the launch view.

Mini previews and on-phone guides show pending changes immediately. Normal gestures use saved settings when Nudge is in the background. Pending edits survive activity recreation and temporary backgrounding. They are not included in backups or disk saves until you select Save. Closing the app process discards the draft.

Before leaving an editor with pending changes, select Save, Discard, or Keep editing. Save inside a name, gesture, or button dialog updates the editor draft. Use the top Save control to apply that draft. List switches and list management actions still apply immediately.

Each handle owns its touch area, visible handle, gestures, exclusions, and lock-screen option. Duplicating a handle creates an independent disabled handle. Panel assignments continue to refer to the same panels. Deleting or resetting a handle does not delete any panel.

In Appearance, use Center along edge to move the touch area. Mini previews are display-only; page scrolling works over them. Position measures the center along the usable edge. Resizing preserves it until a screen limit requires clamping. Handle length uses a percentage of touch length. Handle thickness uses dp and defaults to 4 dp. Changing touch depth keeps that thickness. A smaller touch area limits drawing without changing the saved value. Turning off Make handle visible keeps saved appearance values and disables those controls.

The sticky Visual Preview (Non-interactive) switch controls the real editing guide. Panel editors have the same switch. Mini previews remain visible. While an editor is resumed, its enabled phone guide passes touches through. Normal handle activation is suppressed. Closing the editor restores normal availability.

Both previews use the selected Portrait or Landscape tab. On an upright phone, a landscape bottom guide appears on the left. Landscape left appears at the top, and landscape right appears at the bottom. Cutout and navigation insets rotate with the guide. During normal use, Bottom stays at the logical bottom in both landscape rotations. Test panel uses the phone's current orientation.

## Gestures

Choose No action, Select panel, or Direct action for each gesture. Select panel opens a searchable dialog. Show Volume Popup is independent: enable it with No action for the popup alone, or combine it with a panel or direct action. All six gesture assignments can coexist. The dialog's Save updates the editor draft. Cancel keeps the existing draft assignment.

Panel choices show a four-square icon and the prefix Panel:. Direct actions show the same icon and the prefix Action:. Panel options have separate rows with space between them. Action names stay centered when they wrap.

Select **Direct action** to assign Phone settings, Nudge settings, Volume up, Volume down, Media mute, Silent ringer, Vibrate, Do Not Disturb, Lock screen, or Open app. No panel is needed. Choose an app for Open app. Volume up and Volume down request the popup through their existing adjustment, without a second request. Icon appearance controls apply only to panel buttons.

Input Method is also available for every gesture and as a panel button. It opens Android's native keyboard chooser and closes the Nudge panel. It does not change the keyboard until you select one. Nudge uses its internal activity to obtain input focus because Android can ignore requests from an unfocused service. The activity closes when the chooser returns. If no chooser takes focus within two seconds, it closes and reports an error.

On the lock screen, the picker first requests normal system unlock. Cancelled authentication, changed assignments, disabled handles, and service loss cancel the request. Screen-off cancels it too. The picker adds no idle observer or background service. Its system appearance and keyboard restoration on HyperOS require device testing.

Each assignment owns its action settings. Editing it does not change a panel button or another gesture. Direct actions close the current Nudge panel. App and settings actions use the same normal system unlock step as panel buttons. A removed or changed assignment cannot continue after unlock. Sound-mode actions show required Do Not Disturb access on Home, including when no panel uses them.

Close panels and Screenshot are available as panel buttons and direct actions. Close panels dismisses Nudge's open panel and resets its swipe sequence. It does not dismiss Android's volume popup. With no Nudge panel open, it has no effect.

Notifications opens the native notification shade. Control panel opens native Quick Settings, called Control center on some phones. Some devices use the same panel for both requests. Nudge removes its interactive panel before submitting either request. Failed requests show an error. Android controls availability on the lock screen. No extra permission is needed beyond the connected accessibility service. Verify the exact behavior on your HyperOS build.

Screenshot removes the interactive Nudge panel, waits two display frames, and requests Android's native screenshot action. Android controls capture, saving, and restrictions. Nudge does not read or store the image. Screen-off and service shutdown cancel pending requests. Requests add no idle timer or observer. Editing guides still follow their preview switch.

There is no setting to hide handles during every screenshot method. Android's supported APIs do not provide a reliable pre-capture signal for all device methods. The [screenshot detection API](https://developer.android.com/about/versions/14/features/screenshot-detection) is activity-scoped and reports supported captures after they occur. Visible handles can appear in screenshots, including those requested through Nudge.

- Tap: release within 250 ms without moving beyond touch slop.
- Hold: hold still for the selected Hold time. Run at the deadline or after release.
- Swipe: move inward at least 24 dp and release within 600 ms by default.
- Swipe and hold: reach its own distance within its own time limit. Hold still, then run at the deadline or after release.

Swipe, Swipe and hold, Hold, and Tap have separate settings sections. Each hold time accepts 300 to 3000 ms and defaults to 500 ms. Each hold action defaults to After release. Select When hold time ends for immediate execution. Swipe and hold has an independent distance and swipe time limit. Small finger drift and release jitter do not restart a completed dwell. Continued movement before the dwell ends restarts its timer.

Swipes run immediately. Further swipes advance through assigned stages and return to the first stage after the last. Unassigned stages are skipped. Each hold gesture follows its selected execution mode. It runs once, including when the finger is later released. Cancelled, multitouch, outward, and mainly parallel gestures do not activate a handle.

Each gesture type has its own haptic switch and strength. Feedback runs once when that gesture activates. Phones without amplitude control use pulse duration for strength. Hardware and Android vibration policy can limit feedback.

Pulling a panel shifts a non-interactive preview slightly inward, up to 12 dp. The preview stays in place during a hold. Releasing a completed gesture opens the assigned panel and settles it over 180 ms. An immediate swipe-and-hold action keeps its panel suspended until release. Preview buttons do not execute actions. Disabled system animations skip the motion.

A Nudge panel keeps its invoking handle's swipe sequence active. Dismissing it resets that sequence. Another panel replaces it. Volume popup alone closes the Nudge panel. A combined assignment opens its panel and requests the stock popup once without changing volume. Android controls the stock popup's appearance and dismissal.

After Volume popup alone or a direct action, the next swipe advances only within Swipe sequence timeout, which defaults to 3 seconds. The existing saved timeout is retained. A combined assignment keeps the sequence active while its Nudge panel is open. Timing uses an expiry timestamp, not an idle timer or popup observation. Tap, Hold, and Swipe and hold reset ordinary swipe progression. Failed direct actions reset progression.

## Independent panels

Open **Panels** to create, name, order, duplicate, edit, or delete panels. A new configuration has no panels. There are no fixed panel slots. Panels have their own editor and can be shared by several handles.

Deleting a panel requires confirmation. Its gesture links change to No action. Combined panel-and-volume assignments keep Volume popup. Duplicating a panel copies all its settings and buttons with fresh IDs; existing gesture links keep the original panel.

The panel editor retains the same button, appearance, layout, position, animation, outside-touch, and landscape controls. Select a preview handle to check Follow handle placement. This choice affects only the preview. At runtime, the handle that opens the panel sets its anchor and animation direction. Without a handle, previews use the default right-edge placement.

## Panel layouts

| Direction | Fixed | Scrollable | Fill order |
| --- | --- | --- | --- |
| Vertical | Columns and Maximum rows | Columns and Panel height | Left to right, then down |
| Horizontal | Rows and Maximum columns | Rows and Panel width | Top to bottom, then right |

Changing direction transposes the controls and retains their numeric values. Icons stay upright. The row or column count is shared across modes. Fixed limits and Scrollable size are saved separately.

Fixed panels never scroll. For example, two columns and three maximum rows show the first six buttons. Screen limits can reduce this count without changing saved settings. Extra buttons remain in the editor with a grey appearance and a Not shown label. Their edit, remove, and move controls remain available. Reorder a button into the visible range or select Scrollable to show it.

Scrollable panels keep all buttons available. Their length uses a percentage of the usable screen. Use Test panel to check scrolling and actions. Preview buttons do not run actions.

Follow handle places the panel inward from the handle that opens it. Turn it off to use center X/Y percentages. Optional landscape overrides retain separate mode, direction, counts, sizing, and placement. Buttons, appearance, and outside-touch behavior remain shared.

The development interface adds X/Y offsets while Follow handle is on. Positive X moves right. Positive Y moves up. Turning Follow handle off shows absolute center percentages. Each mode retains its saved values. Mini previews do not capture gestures. Legacy Y offsets migrate once to retain physical placement.

Open animation and Close animation are separate settings. Each offers No animation, Fade, Slide and fade, and Scale, slide, and fade. New panels default to No animation for both. Older configurations use their saved opening effect as the initial closing effect. Editing or resetting one setting keeps the other unchanged. Both values are included in backups and panel copies.

The handle's logical edge sets the direction. Exit moves toward that edge; scale also shrinks the panel. The animation does not depend on the distance from the handle.

Native View animations run only during transitions: 180 ms on entry and 120 ms on exit at the normal system speed. A closing panel stops receiving input and sound updates immediately. Its rendered content remains until exit completes, then its window and composition are removed. Rapid replacement retains at most one inert closing panel alongside the active panel. Completing an old exit cannot close a newer panel.

None, editing guides, and disabled system animations use no transition. Screen-off, editing-session changes, errors, and service shutdown remove windows immediately and cancel pending animations. There is no idle animation loop.

## Closing panels

Each panel has an Outside touch setting:

- Close and block touch consumes the complete outside gesture and closes on release. Nothing beneath is activated. This is the default.
- Close and pass touch through closes the panel and lets Android deliver the original touch to the window beneath.
- Pass touch through and keep open delivers the original touch beneath the panel and retains the panel.

Nudge does not replay or inject touches. A drag that starts inside the panel does not become an outside dismissal. Handle gestures remain available above the panel. Back closes the panel without navigating the app beneath. Panels have no Close button.

When a native volume popup is behind the panel, the same outside-touch setting applies. In blocking mode, the first outside tap closes Nudge's panel without changing the native controls.

Enable **Close automatically** in a panel's **Auto-close** section to set an idle timeout from 1–60 seconds. The option defaults off, with a saved duration of 3 seconds. Turning it off retains the duration.

Opening the panel or receiving touch input restarts the timer. The timer pauses while a touch is held, including scrolling. Expiry closes the panel and resets its invoking handle's swipe sequence to the first assigned stage. It also applies to combined panel-and-volume assignments; Android retains control of the native volume popup.

Editing guides have no timeout. **Test panel** uses the saved timeout. Only an open interactive panel has a pending delay. Replacement, dismissal, screen-off, and service cleanup cancel it. Unrelated settings or overlay refreshes do not extend its deadline.

## Actions and icons

Panels provide Phone settings, Nudge settings, media volume up/down/mute, Silent ringer, Vibrate, Do Not Disturb, Lock screen, and Open app. Sound adjustments keep the panel open. App, settings, and lock actions close it. Power controls are excluded.

Silent ringer and Vibrate select their respective modes. Tapping the active mode returns to Normal. Media mute does not change the ringtone mode. Sound-mode actions can require Do Not Disturb access. On Android 15 and later, another active DND rule can keep DND on after Nudge's rule is turned off.

Choose a default icon pack for a panel or a manual static icon for any button. Common appfilter mappings and drawable lists are supported. Missing apps and packs retain their references and use fallback icons. Dynamic clocks and calendars are excluded. Icon shape and corners are independent of panel corners.

The development button editor keeps changes private until Save. Volume up and Volume down have an optional Show volume popup switch. It changes the native UI flag while keeping the media adjustment to one system step. Sound buttons still keep the Nudge panel open.

Maximum brightness and Minimum brightness are available as panel buttons and direct gesture actions. The first use opens Android's Modify system settings screen if access is missing. Grant access, return, and run the action again. Nudge does not change brightness before access is granted.

Each brightness limit uses manual brightness. Only a Nudge action activates its button. A manual brightness change clears the active state, including a quick drag back to the same limit. App restarts also clear active states. Switching limits activates only the new action.

Each action has **Use previous brightness**, enabled by default, and **Return brightness**, initially 50%. Tap an active action to return to its previous level and automatic mode when available. Previous maximum brightness is unavailable. Minimum also rejects a previous minimum. If the previous level is unavailable, or Use previous brightness is off, use Return brightness in manual mode. Returning always clears the active state, even if the selected return value equals a limit.

The same controls appear for panel buttons, direct gestures, and Home shortcuts. Saved return settings are independent of runtime restore values. Return settings survive backups, duplication, and restarts. Runtime values are never exported. They do not wake the screen. Device brightness limits require a Redmi check.

Toggle handles is available as a panel button and direct gesture action. Select All handles, This handle, or Other handles. This handle means the handle that opened the panel or ran the gesture. A panel test without a preview handle cannot select This handle or Other handles.

The action pauses currently enabled handles without changing their saved Enable switches. Its tile shows On when all selected handles are available through this pause control. Tap again to resume. If only some are paused, tapping resumes the group. Existing panels remain usable while their handle is paused. Saved disabling, the master switch, and normal eligibility rules still close panels.

The **Pause handles** switch is above **Add handle**. Switch it on to pause all enabled handles. Switch it off to clear every pause, including a handle paused by its own gesture. The switch is on if any handles are paused. Turning off Enable Gestures or restarting the app clears pauses. Saved disabled handles remain disabled. Pauses are runtime state and are not included in backups.

The **Hide visible handles** switch is below **Pause handles**. It hides all runtime visible bars until switched off. Gestures still work. Each handle keeps its saved visibility, color, size, and placement. Editing previews remain available. The override survives app and phone restarts.

**Toggle handle visibility** applies the same override from a panel button, direct gesture, or Home shortcut. Its tile is On when visibility is allowed and Off while bars are hidden. It keeps an open panel usable. Restoring visibility shows only bars enabled in each handle's settings. **Toggle handles** follows **Close panels** in the action picker; **Toggle handle visibility** is next.

Choose a built-in icon, including Camera and Wallet, without an icon pack. Each button can follow the panel's background setting, use a container, or stand alone. Standalone icons fill the button and retain the configured icon corners. App and icon-pack images retain their original colors. Automatic icon color applies to built-in symbols and uses the effective background for contrast. All color swatches share one palette. Panel backgrounds, button backgrounds, icons, and visible handles have independent opacity controls, including automatic colors.

The built-in Nudge icon uses the separated pill mark without the app background. It follows the panel's icon color and opacity. Select Standalone for a button with no container. The default Nudge settings icon keeps the full app icon.

Each handle can be enabled on the awake lock screen. Apps and settings use Android's normal unlock step. Cancelling authentication cancels the action. Nudge does not wake the screen. Screen-off and always-on-display operation remain disabled.

## Backups and persistence

Export or import JSON through Info → Backup. Files contain handles, panels, gesture links, and icon references, including hidden buttons. They do not contain icon-pack images, permissions, signing material, or transient sessions.

The development navigation names this destination **Info**. Existing backup and help controls remain there. New display settings and built-in icon references are included in JSON backups. Older configurations use compatible defaults for these fields.

Add handles and panels is the default import mode. It creates disabled handle copies and independent panel copies with fresh IDs. Shared panel links within the imported group remain shared. Current settings stay unchanged. Replace configuration requires confirmation and leaves the master switch off for review. Invalid files and files larger than 1 MiB are rejected before configuration changes.

Existing single-handle preferences migrate into Handle 1. Its old activation mode keeps the volume popup assignment. The master switch, testing preference, appearance, landscape settings, and exclusions are retained. Migration keeps unrelated stored keys.

Schema 6 introduced brightness actions and per-button handle-toggle scopes. Schema 5 introduced visible thickness in dp. Schemas 1 through 5 migrate while preserving assignments and physical placement. Schema-2 documents keep their existing assignments and timeout. Existing configured or assigned panels from schema 1 migrate with their settings, button order, overflow, icons, and gesture links intact. Untouched, unassigned slots are omitted. Older backup files use the same conversion. A failed migration preserves the original stored document.

Backups include direct action settings and selected app references. Added or duplicated handles receive fresh action IDs and start disabled. Panel links remain shared within an imported group. Older app builds reject the newer format instead of silently discarding direct actions.

Field changes apply immediately to the current draft. Disk saves wait for 150 ms of quiet and flush when an adjustment ends or the app backgrounds. Failed saves retain the draft and offer Retry. Delayed writes do not roll back newer edits or restore deleted handles.

## Battery and verification

All handles share one accessibility service. Static windows schedule no idle frames. Hold recognition and swipe sequences use timestamps. Direct actions reuse the existing action executor. Panels own their event observers only while open. Brightness observes its two settings only while a restore value exists. Handle pauses use state updates without timers or polling. Icons load off the main thread with an 8 MiB bitmap cache.

Optional panel auto-close uses one cancellable delay while a panel is open. It adds no polling, wake lock, periodic job, or idle frame loop.

There are no wake locks, periodic restart jobs, foreground services, or scheduled update checks. HyperOS Background autostart remains recommended. Use No restrictions only when needed for reliability, then measure battery use.

Automated tests cover models, persistence, gestures, layout, window ordering, and panel touch handling. They cannot establish HyperOS behavior, native-popup coexistence, Recents survival, or battery drain. Record those checks in the ignored `plans/compatibility.md` on the actual phone. A signed upgrade check waits for the next authorized release.

## Unreleased handle controls

Touch length normally reaches the usable shorter screen edge. Enable longer touch length extends its maximum to the longer edge. The active edge and screen constraints limit rendering without reducing the saved length. Turning the option off retains the saved value. Re-enable it to restore the longer size.

Soft hide in full screen starts off. When enabled, the first valid gesture reveals the handle with one brief bounce and runs no action. Later gestures execute normally. Hide after defaults to three seconds and accepts 1 to 30 seconds. Contact and an owned panel suspend expiry. Hidden touch areas still receive touches. Detection uses status-bar visibility; unknown visibility retains normal behavior. HyperOS detection remains pending device verification.

## Unreleased panel controls

Pass touch through and keep open adds a third outside-touch policy. The panel and swipe sequence remain active until dismissal, including Back or timeout. Verify Back consumption after pass-through touches on the actual phone.

Empty button adds a grid cell with no action. Its Appearance dropdown selects No icon (container only), No button (blank space), or Slash icon. Each cell owns its appearance. Blank spaces retain capacity and remain inside the panel touch area. Empty cells do not become runtime click targets.

Button drags beyond touch slop cancel clicking until the next press, including when the pointer returns to the same button. Scrolling remains available. The unexpected panel dismissal investigation is deferred to a separate task; app-change detection is unchanged.

Version 0.3.1 converts earlier thickness percentages to dp using the saved touch depth. For example, 25% of 24 dp becomes 6 dp. Configuration and backup schema 5 stores that value independently. Older backups can be imported; older Nudge versions cannot import schema-5 backups.

Development backups use schema 12 for per-action brightness return preferences, slider touch behavior, and the global visible-handle override. Earlier configurations start with the override off. Add import keeps the current global override; Replace import restores the file's value. Schema 9 and older backups receive Use previous brightness on and Return brightness at 50%. Older slider settings keep Jump to touch. Existing actions, icon settings, and other preferences are retained.

## Brightness touch behavior

Home provides **Touch behavior** beside the brightness slider preview. **Jump to touch** uses the touched track position. **Slide to adjust** starts at the current brightness and applies each drag distance relative to the track height. Touching without movement does not change brightness in Slide mode. A full track of movement changes the level by 100 percentage points. Both modes clamp at the limits. Reversing direction responds immediately, including after an overshoot.

Jump to touch remains the default for new and existing configurations. Previews stay non-interactive. Accessibility progress actions use the requested level in both modes. Changing modes during a drag cancels that input. Relative motion retains frame-coalesced writes and creates no idle work.
