# Nudge

An invisible edge shortcut that opens Android's own volume popup. Built with Kotlin, Jetpack Compose and Material 3 for personal use on Redmi Note 12 / HyperOS 1.

Nudge's touch area is an accessibility overlay. Invisible mode draws nothing during normal use, but touches inside its rectangle belong to the shortcut. The default is a 16 × 120 dp area on the right edge, centered at 65% of the usable edge. Swipe inward 24 dp within 600 ms and release. Nudge requests the system popup without changing volume; HyperOS owns its appearance and dismissal. Landscape and lock-screen availability start disabled. The master switch starts off until you finish setup.

## Build

Use Android SDK 36, build-tools 36.0.0 and Java 17 or newer supported by Gradle 9.1. The local build was performed with JDK 24. Set `ANDROID_HOME` or create an ignored `local.properties` containing `sdk.dir=C\:/path/to/Android/Sdk` (escape the drive colon).

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lint :app:assembleDebug
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. Its application ID is `com.modukey.app.debug`; releases use the permanent ID `com.modukey.app`. The source namespace retains the original project identifier; the displayed name is **Nudge**.

## Install and set up on HyperOS 1

1. Install the APK and open Nudge. Open Accessibility from Home.
2. Enable **Nudge edge shortcut** under Accessibility → Downloaded apps. Some phones use Settings → Additional settings → Accessibility.
3. If Android blocks this for a sideloaded APK, open Nudge's App info → ⋮ → **Allow restricted settings**, then return to Accessibility.
4. Turn on the master switch. Home reports permission enabled, service connected and trigger attached independently.
5. Use **Test volume popup** and visually confirm it appeared. Also test an actual swipe while another app is foregrounded. A submitted request is not proof the popup appeared.
6. If HyperOS stops the service, enable Background autostart under Settings → Apps → Permissions; choose App info → Battery saver → No restrictions. For background app locking, try Security → Settings → Boost speed → Lock apps → Nudge, or lock it in Recents where available. Menu names vary.

While **Customize is resumed**, the real indicator and a faint touch-area guide stay visible with Accessibility connected, including when the master switch is off. This editing overlay passes touches through and cannot activate volume. Leaving the screen, backgrounding or closing the activity restores **Show outside customization** without changing it. With Accessibility disconnected, the in-app preview still works. The on-phone preview follows the actual orientation; the opposite orientation in the phone drawing is an estimate until rotation.

Position means the trigger's center as a percentage of the usable edge, from top to bottom on a side or left to right on the bottom. Resizing preserves this center until a screen corner requires clamping. Indicator length is a continuous percentage of touch length. Thickness and radius are fitted at draw time; changing the touch area does not overwrite saved appearance values. Landscape inherits the default edge and position unless **Override placement in landscape** is enabled; turning the override off retains its values. Bottom remains the logical bottom in either landscape rotation.

Edits update an application-owned draft immediately. Overlay updates coalesce to one display frame when needed; there is no idle frame loop. Disk saves wait for 150 ms of quiet, and flush on gesture completion or backgrounding. Field changes merge against current settings; failed saves retain the draft and offer Retry saving. Behavior includes swipe/tap/hold activation, haptics and launcher-app exclusions. A hold activates **on release** after 500 ms.

The service observes window-change package names only when the master switch is on and exclusions are configured, without retrieving screen content. With the default empty exclusions, accessibility event delivery is disabled at the system subscription. Repeated events for the same package and System UI panels are ignored. After enabling exclusions, switch apps once to initialize visibility.

Battery use is a design priority: a system-bound accessibility service, event-driven screen/rotation/settings handling, no foreground service, polling, wake lock, periodic jobs, boot loop, network requests or ongoing overlay animation. The small trigger draws only when its appearance changes. Appearance changes do not move its window, equal bounds do not update the window layout, and redraws schedule no follow-up frames. The save debounce exists only while edits are pending. Actual drain must be measured on the Redmi; no zero-drain claim is made.

There is no root, ADB runtime dependency, Internet permission or separate draw-over-apps permission. Keep the reserved rectangle clear of native volume controls when positioning it. **Available on lock screen** optionally permits the same shortcut on the awake lock screen after the first unlock following reboot. App exclusions are ignored while locked; normal indicator visibility and landscape availability still apply. Nudge does not wake the screen, run on the always-on display, dismiss keyguard or unlock the phone. Force-stop and permission revocation require manual recovery.

Connection distinguishes **Disabled**, **Enabled but disconnected**, and **Connected**, plus window errors. An enabled service reported as “malfunctioning” may have lost its connection; this alone does not establish whether Nudge crashed or HyperOS killed it. Startup and **Copy compatibility details** inspect recent Android process-exit reasons. Bounded app-private lifecycle/error records survive process restarts; they contain no screen contents or app-switch history. Capture these details before re-enabling the service. No restart loop is used or survival after force-stop promised.

## Releases and updates

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Initialize-Signing.ps1
# Securely back up .local/signing outside this workspace before distributing an APK.
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Release.ps1 -CreateTag
```

The signing initializer refuses to replace an existing key. Its randomly generated passwords are stored only in the ignored `.local/signing/keystore.properties`; do not commit or share that directory. Back up the key **and** properties file securely. Losing this key prevents normal updates to existing installations.

The release script requires a clean Git working tree and an existing signing key. It runs tests and lint, builds the minified signed release, verifies its signature and identity, and compares its signer with the archived prior release when available. It writes `releases/Nudge-<version>.apk`, a SHA-256 file and public build metadata including the commit, SDKs, signer and verification status. `-CreateTag` adds an annotated local release tag after success; it never pushes anything. Version values live in `version.properties`.

For each new distributed version: change `versionName` (for example `0.1.1`) and increase `versionCode`, update `CHANGELOG.md`, run checks, commit, and run the release script. Install the new release over the old release, without uninstalling, to retain settings. Debug and release are separate installations. Recheck accessibility after updating. Reverting a bug fix means a new commit and a **higher** version code; do not move release tags or expect Android to accept a downgrade.

The repository uses Git on `main` with the existing GitHub remote. Releases and uploads remain manual; the release script does not push, publish to a store or install an updater.

Version 0.1.1 migrates the old indicator length once to `oldIndicatorLengthDp / oldTouchLengthDp`. Existing numeric position percentages are retained with their new center meaning. Landscape availability, saved landscape placement and all unrelated preferences are preserved; the new landscape override and lock-screen flags default off. A slight initial position shift from 0.1.0 reflects the center definition.

## Device verification

See [the compatibility checklist](docs/compatibility.md). An AOSP emulator or Robolectric cannot establish HyperOS compatibility. With a connected, authorized phone, run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Device-Profile.ps1
# For multiple devices: add -Serial <adb-serial>
```

The profile is saved under `.local/device-checks`; manually complete the checklist with the model, exact HyperOS build, navigation mode and observations. Home's popup confirmation is session-local and user-reported. Copy compatibility details from Behavior & help for support.

Official references: [accessibility overlays](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_ACCESSIBILITY_OVERLAY), [AudioManager](https://developer.android.com/reference/android/media/AudioManager), [restricted settings](https://support.google.com/android/answer/12623953), [Xiaomi autostart](https://www.mi.com/global/support/faq/details/KA-507611/), [Xiaomi background app locking (menus vary)](https://www.mi.com/global/support/faq/details/KA-535435/), [Android process-exit reasons](https://developer.android.com/reference/android/app/ApplicationExitInfo), [Android app signing](https://developer.android.com/studio/publish/app-signing).
