# Nudge

An invisible edge shortcut that opens Android's own volume popup. Built with Kotlin, Jetpack Compose and Material 3 for personal use on Redmi Note 12 / HyperOS 1.

Nudge's touch area is an accessibility overlay. Invisible mode draws nothing, but touches inside its rectangle belong to the shortcut. The default is a 16 × 120 dp area on the right edge, 65% along its available travel. Swipe inward 24 dp within 600 ms and release. Nudge requests the system popup without changing volume; HyperOS owns its appearance and dismissal. Landscape starts disabled. The master switch starts off until you finish setup.

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
6. If HyperOS stops the service, enable Background autostart under Settings → Apps → Permissions; choose App info → Battery saver → No restrictions. Lock the app in Recents if your build supports it and cleanup stops the shortcut. Menu names vary.

Customize includes a draggable preview, orientation-specific edge/position, touch dimensions, an optional visible indicator with custom hex color, and a ten-second highlight of the actual area. The preview always shades the touch rectangle for positioning; this does not make the real trigger visible. The opposite orientation is an estimate until the phone is rotated. Behavior includes swipe/tap/hold activation, haptics and launcher-app exclusions. A hold activates **on release** after 500 ms.

The service observes window-change package names only when the master switch is on and exclusions are configured, without retrieving screen content. With the default empty exclusions, accessibility event delivery is disabled at the system subscription. Repeated events for the same package and System UI panels are ignored. After enabling exclusions, switch apps once to initialize visibility.

Battery use is a design priority: a system-bound accessibility service, event-driven screen/rotation/settings handling, no foreground service, polling, wake lock, periodic jobs, boot loop, network requests or ongoing overlay animation. The small trigger draws only when its appearance changes, and repeated equal geometry/settings do not update its window. The only background timer is a cancellable ten-second highlight requested explicitly by you. Actual drain must be measured on the Redmi; no zero-drain claim is made.

There is no root, ADB runtime dependency, Internet permission or separate draw-over-apps permission. Keep the reserved rectangle clear of native volume controls when positioning it. Lock-screen and screen-off operation is disabled. Force-stop and permission revocation require manual recovery.

## Releases and updates

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Initialize-Signing.ps1
# Securely back up .local/signing outside this workspace before distributing an APK.
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Release.ps1 -CreateTag
```

The signing initializer refuses to replace an existing key. Its randomly generated passwords are stored only in the ignored `.local/signing/keystore.properties`; do not commit or share that directory. Back up the key **and** properties file securely. Losing this key prevents normal updates to existing installations.

The release script requires a clean Git working tree and an existing signing key. It runs tests and lint, builds the minified signed release, verifies its signature and identity, and writes `releases/Nudge-<version>.apk`, a SHA-256 file and public build metadata. `-CreateTag` adds an annotated local release tag after success; it never pushes anything. Version values live in `version.properties`.

For each new distributed version: change `versionName` (for example `0.1.1`) and increase `versionCode`, update `CHANGELOG.md`, run checks, commit, and run the release script. Install the new release over the old release, without uninstalling, to retain settings. Debug and release are separate installations. Recheck accessibility after updating. Reverting a bug fix means a new commit and a **higher** version code; do not move release tags or expect Android to accept a downgrade.

The repository uses local Git on `main`, with separate project, trigger, interface and release-tooling milestones. No remote, store publishing or automatic updater is configured.

## Device verification

See [the compatibility checklist](docs/compatibility.md). An AOSP emulator or Robolectric cannot establish HyperOS compatibility. With a connected, authorized phone, run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Device-Profile.ps1
# For multiple devices: add -Serial <adb-serial>
```

The profile is saved under `.local/device-checks`; manually complete the checklist with the model, exact HyperOS build, navigation mode and observations. Home's popup confirmation is session-local and user-reported. Copy compatibility details from Behavior & help for support.

Official references: [accessibility overlays](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_ACCESSIBILITY_OVERLAY), [AudioManager](https://developer.android.com/reference/android/media/AudioManager), [restricted settings](https://support.google.com/android/answer/12623953), [Xiaomi autostart](https://www.mi.com/global/support/faq/details/KA-507611/), [Android app signing](https://developer.android.com/studio/publish/app-signing).
