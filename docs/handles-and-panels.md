# Handles and panels

This feature is under development. Physical Redmi Note 12 / HyperOS checks remain pending. It does not create a new release.

## Handles

Open **Handles** to add, name, enable, duplicate, move, or delete a handle. New and duplicated handles start disabled. The first handle receives touches where touch areas overlap. The master switch on Home must also be on.

Each handle owns its touch area, visible handle, gestures, exclusions, lock-screen option, and three panels. Duplicating a handle copies these settings with new IDs. Changes to one copy do not affect the other.

In Appearance, drag the touch area or use Center along edge. Position measures the center along the usable edge. Resizing preserves it until a screen limit requires clamping. Handle length and thickness use percentages of the touch area. Turning off Make handle visible keeps saved appearance values and disables those controls.

While an editor is resumed, its phone guide passes touches through. Normal handle activation is suppressed. Closing the editor restores normal availability. The phone guide follows the actual orientation; rotate the phone to check landscape. The in-app opposite-orientation preview estimates its system insets.

## Gestures

Assign No action, Volume popup, or Panel 1–3 to each gesture. All six gesture assignments can coexist.

- Tap: release within 250 ms without moving beyond touch slop.
- Hold: hold still for at least 500 ms, then release.
- Swipe: move inward at least 24 dp and release within 600 ms by default.
- Swipe and hold: reach the swipe threshold within its time limit, hold still for 500 ms, then release.

Swipes run immediately. Further swipes advance through assigned stages and return to the first stage after the last. Unassigned stages are skipped. Hold gestures do not run until release. Cancelled, multitouch, outward, and mainly parallel gestures do not activate a handle.

A Nudge panel keeps its owner's swipe sequence active. Dismissing it resets that sequence. Another panel replaces it. A volume action closes the Nudge panel and requests the stock popup without changing volume. Android controls the stock popup's appearance and dismissal.

After a volume action, the next swipe advances only within the configured sequence period, which defaults to 3 seconds. This uses an expiry timestamp, not an idle timer or popup observation. Tap, Hold, and Swipe and hold reset ordinary swipe progression.

## Panel layouts

| Direction | Fixed | Scrollable | Fill order |
| --- | --- | --- | --- |
| Vertical | Columns and Maximum rows | Columns and Panel height | Left to right, then down |
| Horizontal | Rows and Maximum columns | Rows and Panel width | Top to bottom, then right |

Changing direction transposes the controls and retains their numeric values. Icons stay upright. The row or column count is shared across modes. Fixed limits and Scrollable size are saved separately.

Fixed panels never scroll. For example, two columns and three maximum rows show the first six buttons. Screen limits can reduce this count without changing saved settings. Extra buttons remain in the editor with a grey appearance and a Not shown label. Their edit, remove, and move controls remain available. Reorder a button into the visible range or select Scrollable to show it.

Scrollable panels keep all buttons available. Their length uses a percentage of the usable screen. Use Test panel to check scrolling and actions. Preview buttons do not run actions.

Follow handle places the panel inward from its handle. Turn it off to use center X/Y percentages or drag the panel preview. Optional landscape overrides retain separate mode, direction, counts, sizing, and placement. Buttons, appearance, and outside-touch behavior remain shared.

## Closing panels

Each panel has an Outside touch setting:

- Close and block touch consumes the complete outside gesture and closes on release. Nothing beneath is activated. This is the default.
- Close and pass touch through closes the panel and lets Android deliver the original touch to the window beneath.

Nudge does not replay or inject touches. A drag that starts inside the panel does not become an outside dismissal. Handle gestures remain available above the panel. Back closes the panel without navigating the app beneath. Panels have no Close button.

When a native volume popup is behind the panel, the same outside-touch setting applies. In blocking mode, the first outside tap closes Nudge's panel without changing the native controls.

## Actions and icons

Panels provide Phone settings, Nudge settings, media volume up/down/mute, Silent ringer, Vibrate, Do Not Disturb, Lock screen, and Open app. Sound adjustments keep the panel open. App, settings, and lock actions close it. Power controls are excluded.

Silent ringer and Vibrate select their respective modes. Tapping the active mode returns to Normal. Media mute does not change the ringtone mode. Sound-mode actions can require Do Not Disturb access. On Android 15 and later, another active DND rule can keep DND on after Nudge's rule is turned off.

Choose a default icon pack for a panel or a manual static icon for any button. Common appfilter mappings and drawable lists are supported. Missing apps and packs retain their references and use fallback icons. Dynamic clocks and calendars are excluded. Icon shape and corners are independent of panel corners.

Each handle can be enabled on the awake lock screen. Apps and settings use Android's normal unlock step. Cancelling authentication cancels the action. Nudge does not wake the screen. Screen-off and always-on-display operation remain disabled.

## Backups and persistence

Export or import JSON through Settings → Backup. Files contain configuration and icon references, including hidden buttons. They do not contain icon-pack images, permissions, signing material, or transient sessions.

Add handles is the default import mode. It creates independent disabled copies and retains current global settings. Replace configuration requires confirmation and leaves the master switch off for review. Invalid files and files larger than 1 MiB are rejected before configuration changes.

Existing single-handle preferences migrate into Handle 1. Its old activation mode keeps the volume popup assignment. The master switch, testing preference, appearance, landscape settings, and exclusions are retained. Migration keeps unrelated stored keys.

Field changes apply immediately to the current draft. Disk saves wait for 150 ms of quiet and flush when an adjustment ends or the app backgrounds. Failed saves retain the draft and offer Retry. Delayed writes do not roll back newer edits or restore deleted handles.

## Battery and verification

All handles share one accessibility service. Static windows schedule no idle frames. Hold recognition and volume sequences use timestamps. Panels own their event observers only while open. Icons load off the main thread with an 8 MiB bitmap cache.

There are no wake locks, periodic restart jobs, foreground services, or scheduled update checks. HyperOS Background autostart remains recommended. Use No restrictions only when needed for reliability, then measure battery use.

Automated tests cover models, persistence, gestures, layout, window ordering, and panel touch handling. They cannot establish HyperOS behavior, native-popup coexistence, Recents survival, or battery drain. Record those checks in the ignored `plans/compatibility.md` on the actual phone. A signed upgrade check waits for the next authorized release.
