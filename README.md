# Nudge

Configurable edge handles for Android volume controls and custom button panels. Built with Kotlin, Jetpack Compose, and Material 3 for personal use on Redmi Note 12 / HyperOS 1.

Each handle has its own gestures, appearance, placement, exclusions, and lock-screen option. Panels are created and edited separately. Several handles can open the same panel. All handles share one accessibility service. New and duplicated handles start disabled. Home provides a master switch and actual connection status.

The default handle uses a 16 × 120 dp touch area on the right edge, centered at 65%. An inward swipe of 24 dp within 600 ms requests the native volume popup without changing volume. The handle can be invisible; its touch area still receives touches.

Version [0.4.3 is published](https://github.com/serogee/Nudge/releases/tag/v0.4.3). Version 0.4.4 is in preparation and adds guarded swipe adjustment, ordinary swipe direction selection, and clearer settings controls. Brightness calibration is now below the brightness slider controls in Home. See [Handles and panels](docs/handles-and-panels.md) for controls and behavior. The archived 0.2.0 build has the earlier per-handle panels.

Unreleased development adds **Automatic calibration** beside **Manual calibration** in Home. It uses a separately enabled, temporary **Nudge brightness calibration** accessibility service. After enabling it, return to Home and select **Start calibration**. It moves the identified native brightness control through five positions, restores brightness and automatic mode, then offers Save. Screenshots, screen recording, and coordinate taps are not used. The normal handle service cannot read window content. Unsupported native controls keep the manual capture flow. See the [calibration and recovery instructions](docs/handles-and-panels.md#brightness-percentages-and-calibration).

## Build

Use Android SDK 36, build-tools 36.0.0, and a JDK supported by Gradle 9.1. The local environment uses JDK 24. Set `ANDROID_HOME` or create the ignored `local.properties` with your SDK path.

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lint :app:assembleDebug
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. Its application ID is `com.modukey.app.debug`; releases use `com.modukey.app`. Debug and release are separate installations.

## HyperOS setup

1. Install the APK and open Nudge. Select Open Accessibility on Home.
2. Enable **Nudge handles** under Accessibility → Downloaded apps. Some builds use Settings → Additional settings → Accessibility.
3. If Android blocks Accessibility, open Nudge App info → ⋮ → Allow restricted settings. Return to Accessibility and enable Nudge.
4. Enable Background autostart for Nudge. Look under Settings → Apps → Permissions → Background autostart, Apps → Autostart, or Security → Permissions.
5. Enable the desired handles and the master switch. Home distinguishes Disabled, Enabled but disconnected, and Connected.
6. Use Test volume popup and visually confirm that it appears. Test the gesture from another app as well.
7. If HyperOS still disconnects Nudge, try App info → Battery saver → No restrictions. Keep current restrictions if Nudge stays connected.
8. If needed, lock Nudge through Security → Settings → Boost speed → Lock apps, or Recents where supported.

Menu names vary by build. Nudge cannot verify Xiaomi autostart or background-lock settings. Force-stop and revoked permissions require manual recovery. Copy diagnostics before re-enabling a disconnected service.

Do Not Disturb access is needed for DND and certain ringtone changes. Lock screen uses the existing accessibility service. No root, device administrator, gesture injection, hardware-key interception, or separate Display over other apps permission is requested.

## Customization and panels

Open Handles to create, order, duplicate, or edit handles. The first handle receives touches where areas overlap. Sticky Visual Preview (Non-interactive) switches control non-touchable phone guides, even with the master off. Mini previews are display-only and allow page scrolling. Normal activation resumes when editing ends.

Open Panels to create, name, order, duplicate, edit, or delete panels. A fresh configuration has no panels. Choose No action, Select panel, or Direct action for each gesture. Show Volume Popup can accompany any selection. No action with the switch on opens only the volume popup. Existing configured or assigned panels migrate automatically; unused empty slots are omitted.

Select Direct action in a gesture assignment to use Lock screen, Open app, settings, or any sound control without a panel. Open app includes an app picker. Volume up and Volume down can also show the volume popup. Save applies the complete assignment; Cancel keeps the previous one. Swipe sequence timeout applies to actions without an open panel and uses an expiry timestamp without background work.

Hold time and Swipe and hold time are separate controls in each handle's Gestures tab. Each accepts 300 to 3000 ms and defaults to 500 ms. Actions run on release. Existing settings and older backups retain the default delays.

Add Input Method as a panel button or a direct gesture action to open Android's native keyboard chooser. Nudge uses a short-lived internal activity to obtain input focus. It does not enable a keyboard or choose one for you. Locked use requires normal system unlock. A failed request closes the internal activity and reports an error.

Choose Nudge in the built-in icon library to use the app mark without its background. The mark follows the panel's icon color. Select Standalone to remove the button container too.

Panels keep their own buttons, appearance, layouts, placement, and landscape settings. Follow handle uses the handle that opens the panel. Select a preview handle in the panel editor to check placement. Deleting a panel clears its gesture links and keeps the volume part of combined assignments. Deleting a handle keeps its panels available.

Fixed panels show only the first buttons within their row and column limits and never scroll. Extra buttons stay saved and editable with a Not shown label. Scrollable panels expose all buttons inside an adjustable viewport. Switching modes retains button order and mode settings.

Each panel can block and close, pass through and close, or pass through and stay open after an outside touch. Back closes the panel without navigating the app beneath. Handle gestures remain available. Preview buttons do not run actions; Test panel opens working controls.

Each panel can close automatically after 1–60 seconds without touch input. This option starts off. Opening or touching the panel restarts its timer; held input pauses it. Expiry closes the panel and resets its handle's swipe sequence to the first assigned stage. Editing guides stay open. Only an open interactive panel has a pending timer; closing or replacing it cancels that timer.

Each panel has separate Open animation and Close animation settings. Both offer None, Fade, Slide and fade, or Scale, slide, and fade. Existing panels start with a closing effect that matches their saved opening effect. Later changes are independent. Exit moves toward the invoking handle's edge. Input stops immediately on dismissal; rendered content stays until the short exit ends. Screen-off, editor changes, and service cleanup remove panels immediately. Animations respect the system setting and schedule no idle frames.

Handle and panel landscape overrides retain separate values when disabled. Bottom remains the logical bottom after rotation. Center-based placement remains stable during resizing, subject to safe screen bounds.

Lock-screen availability is configured per handle and defaults off. All configured actions remain available. Apps and settings request Android's normal unlock step before opening. Nudge does not wake the screen or operate on always-on display. After reboot, unlock the phone once before use.

Backups use the system file picker and include panels, gesture links, hidden buttons, and icon references. Add imports disabled handle copies and independent panel copies, preserving shared links within the imported group. Replace requires confirmation and leaves the master switch off. Older backups migrate on import. Icon packs must be installed separately.

## Global handle controls

In Handles, **Pause handles** and **Hide visible handles** are above **Add handle**. Pause stops input without changing saved Enable switches. Switch it off to resume all paused handles. Hiding removes only the visible bars; gestures remain available. Hiding stays active across restarts and retains each handle's saved appearance. Editing previews remain available.

Use **Toggle handle visibility** from a panel, direct gesture, or Home shortcut. Select All handles, This handle, or Other handles. It targets enabled handles; the Handles tab switch applies to every saved handle. **Toggle and hide handles** provides separate pause/resume and hide/show scopes. Each group toggles independently. An invoking panel stays usable after its handle is paused.

## Brightness slider and Home screen shortcuts

Add Increase brightness or Decrease brightness as a panel button, direct gesture, or Home screen shortcut. Each action changes brightness by five percent and selects manual brightness. Maximum and Minimum have independent **Return brightness** values. Enable **Use previous brightness** to restore a valid previous level and automatic mode. Otherwise, the return level defaults to 50% in manual mode. Previous maximum brightness is unavailable; Minimum also rejects its own minimum. A manually selected maximum makes Maximum brightness active. Its next press uses Return brightness when no previous value is available.

Enable **Show brightness slider** in a gesture assignment. It works alone or with a panel, direct action, and **Show Volume Popup**. The custom slider opens on the left by default, opposite the usual right-side volume popup. Android does not report Xiaomi's private volume popup bounds. Home provides the volume-side selection, custom placement, width, height, timeout, colors, and opacity. The starting size is 64 ? 240 dp; compare and adjust it on your phone. It approximates MIUI styling, rather than using Xiaomi's native volume window.

**Corner radius** changes slider rounding. **Show slider background** controls outer padding; off uses the full track width. **Vertical center from bottom** starts at 0% on the bottom. Existing placement migrates without a jump. The optional **Automatic brightness** button is shown below the slider by default. The same toggle is available for panel buttons, gestures, and shortcuts.

Home's **Touch behavior** selects **Jump to touch** or **Slide to adjust**. Jump uses the touched track position. Slide keeps the current level on touch, then changes it in proportion to the drag. Existing setups keep Jump to touch. Slide follows direction changes immediately at either limit.

In gesture assignments, **Slide to adjust** keeps touch after activation. Select exactly one volume or brightness popup. Keep your finger down, then slide vertically. Each handle has separate travel distances and optional editor guides. A continuous swipe disables Swipe and hold while retaining its saved assignment. This option is separate from the direct slider touch behavior above.

Home's preview is non-interactive and ends when Home is left or the app goes into the background. It works with the master switch off when accessibility is connected. The live slider requires **Modify system settings** access. Dragging selects manual brightness; showing a preview does not change brightness. Outside touch and Back dismiss the live slider. It also closes on timeout, screen-off, source ineligibility, and service cleanup. Locked use follows the invoking handle's lock-screen setting and never wakes the phone.

Use **Home ? Home screen shortcuts ? Add shortcut** to choose an action, app, icon, and name. For This handle or Other handles, select the handle that the action uses. Confirm the request in your launcher. A successful request is not proof that you accepted the launcher prompt; Home checks pinned status when you return. Unsupported launchers show guidance. No legacy installation broadcast is used.

Shortcuts require a connected Nudge accessibility service. They can run with Enable Gestures off. Their icons are static; runtime panel buttons keep their active/inactive indicators. A locked launch requests normal system unlock and rechecks the saved action. Missing or deleted actions do not execute. Removing a shortcut in Nudge disables its launcher entry; remove the remaining Home screen icon separately. Backups contain shortcut definitions and slider settings, but cannot grant permissions or add launcher icons. Add handles preserves current global settings and shortcuts; Replace configuration imports them without pinning icons.

## Battery use and compatibility

One system-bound service manages all handles. It does not poll while idle. It has no wake lock, periodic restart work, foreground service, or idle redraw loop. Gesture timing uses touch timestamps. Panels release their composition and observers when closed. The brightness slider observes settings only while visible and coalesces drag writes to one per display frame. Its timeout is a single pending callback. During held volume adjustment, one unchanged-value popup request runs each second. It stops when input ends. Shortcuts run only after a tap or an explicit pin request. Icons load off the main thread with an 8 MiB bitmap cache. Update checks run only when requested.

Physical Redmi Note 12 checks and battery measurements remain pending. Passing automated tests does not prove HyperOS compatibility or zero battery drain. Record the exact model, Android version, HyperOS build, and navigation mode in the local compatibility checklist.

## Releases and updates

Release preparation happens only when you request a new release. Feature changes remain under Unreleased until then; tests, lint and debug builds can run during development. GitHub publishing is a separate manual step.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Initialize-Signing.ps1
# Securely back up .local/signing outside this workspace before distributing an APK.
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Release.ps1 -CreateTag
```

The signing initializer refuses to replace an existing key. Its randomly generated passwords are stored only in the ignored `.local/signing/keystore.properties`; do not commit or share that directory. Back up the key **and** properties file securely. Losing this key prevents normal updates to existing installations.

The release script requires a clean Git working tree, an existing signing key and reviewed notes for the version. It runs changelog integration checks, app tests and lint, builds the minified signed release, verifies its signature and identity, and compares its signer with the archived prior release when available. `-CreateTag` adds an annotated local release tag after success; it never pushes anything. Version values live in `version.properties`.

Each version has its own folder under `releases/`. For example:

```text
releases/0.3.1/
  Nudge-0.3.1.apk
  Nudge-0.3.1.apk.sha256
  Nudge-0.3.1-notes.md
  Nudge-0.3.1.json
```

Build metadata records the commit, SDKs, signer, and verification status. Exported notes are copied from the committed, reviewed `docs/release-notes-<version>.md`. Changelog generation with `-Artifact` uses the same version folder. Prior-APK signer checks prefer the version folder and also accept the older flat layout. Archived builds remain under `releases/archive/`. All release artifacts remain ignored by Git; upload the files inside the version folder when publishing on GitHub.

For each new distributed version, commit the completed features/fixes, then increase `versionName` and `versionCode` in `version.properties`. Generate the changelog from the commits since the previous reachable release tag:

```powershell
# Preview changes since the latest published release:
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Generate-Changelog.ps1 -Version 0.4.4 -FromTag v0.4.3
# After changing version.properties, update that version's changelog and notes:
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Generate-Changelog.ps1 -FromTag v0.4.3 -Write
# Review the generated files, add device observations, and commit the version and notes.
# Run the signed build from clean main after merging:
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Release.ps1 -CreateTag
```

The generator uses the first line of each full commit message, including messages with body bullets immediately after the title. `feat` becomes Added, `fix` Fixed, `perf` Performance, and build/docs/test/chore commits Maintenance. Other changes go under Changed; `!` or a `BREAKING CHANGE:` footer identifies breaking changes. Scoped and older unscoped titles both work. Each entry includes its commit hash. Scopes and message rules are defined in [.plans/scopes.md](.plans/scopes.md) and [AGENTS.md](AGENTS.md).

`-Write` replaces only the current unreleased version's entry and creates `docs/release-notes-<version>.md`. Re-running it is safe before tagging, but replaces manual edits to those generated notes, so review and add manual compatibility observations after the final generation. Existing released entries are preserved; writing an already tagged version is refused. Previewing a tagged version uses its tag's commit rather than newer work. `-FromTag v0.4.3` selects the baseline for 0.4.4; it must be an ancestor and lower version. No extra tools, network calls, automatic version bumps or GitHub publishing are involved. To check the generator, run `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Test-Changelog.ps1`.

Established local version tags identify release versions in this repository. The newest tag can remain a working release until marked ready. Version 0.4.4, code 13, is being prepared from published `v0.4.3`, code 12. GitHub confirmed v0.4.3 as the latest stable release on 2026-10-23. Select the latest earlier published tag, excluding any working tag. If an earlier working tag remains unready, pass the published baseline explicitly with `-FromTag`.

Install the new release over the old release, without uninstalling, to retain settings. Debug and release are separate installations. Recheck accessibility after updating. Reverting a bug fix means a new commit and a **higher** version code; do not move release tags or expect Android to accept a downgrade.

In **Info → Updates**, tap **Check for updates**. Nudge reads the [public GitHub release list](https://github.com/serogee/Nudge/releases), compares version numbers automatically and reports an update, the current version, or a newer local build. **Include pre-releases (testing)** is saved and defaults to off: off checks stable releases only; on checks stable releases and published test versions, with pre-releases labelled. Changing this option cancels the active check, clears its result and discards any late reply. It does not start a new request. Customization reset preserves this update preference. Drafts and unsupported version tags are excluded. Only published releases are used, not local Git tags. Numeric versions compare correctly (for example, 0.1.10 is newer than 0.1.9); the debug suffix does not count as a new release. Checks are bounded by connection/read timeouts, run off the main thread and report network/rate-limit errors instead of claiming the app is current.

If only pre-releases are published, checks with testing off report that no stable release is available. Enable testing to include them. The app does not change GitHub release labels.

When a newer version is available, **Download APK** submits the attached `Nudge-<version>.apk` to Android Download Manager. Nudge shows progress, retry and cancel controls. Android manages the transfer and its notification. The saved download ID restores status after reopening the app. Files stay in Nudge's app-specific download directory; no storage permission is requested. Progress checks run only while Info is resumed and a download is active. There is no updater polling while idle or in the background.

After download, Nudge checks the package, release version, increasing version code, and existing signing key. Tap **Install update** to open Android's installer. If asked, allow Nudge under **Install unknown apps**. Android still requires installation confirmation. Canceling installation retains the verified download for another attempt. A release APK cannot update the separate debug app; install release builds from GitHub separately. The direct update path needs a Redmi/HyperOS device check before release.

**View release notes** continues to open the GitHub release page. Downloads and installation require your action. Missing APKs, failed downloads, unavailable browsers and missing releases are reported. No account, token, device diagnostics or saved settings are sent with the metadata request. See the [GitHub releases API](https://docs.github.com/en/rest/releases/releases) for the public metadata format.

The repository uses Git on `main` with the existing GitHub remote. Releases and uploads remain manual; the release script does not push or publish to a store. The app has no scheduled updater or automatic installer.

Version 0.1.1 migrates the old indicator length once to `oldIndicatorLengthDp / oldTouchLengthDp`. Existing numeric position percentages are retained with their new center meaning. Landscape availability, saved landscape placement and all unrelated preferences are preserved; the new landscape override and lock-screen flags default off. A slight initial position shift from 0.1.0 reflects the center definition.

Version 0.1.2 migrates indicator thickness once to `oldIndicatorThicknessDp / oldTouchDepthDp`, capped at 100% where it already filled the touch depth. Existing saved thickness and depth use their old validity limits before conversion. Later touch-depth changes keep this percentage, and all unrelated preferences are retained.

Version 0.3.1 restores visible handle thickness in dp, with a 4 dp default and a 0?32 dp control. Earlier percentages convert once using the saved touch depth. Length remains a percentage. Smaller touch areas constrain drawing without changing saved thickness. New backups use schema 5; older backups remain importable.

## Device verification

Keep device results in the local, Git-ignored checklist at `plans/compatibility.md` (formerly `docs/compatibility.md`). This file is not included in a fresh clone. An AOSP emulator or Robolectric cannot establish HyperOS compatibility. With a connected, authorized phone, run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Device-Profile.ps1
# For multiple devices: add -Serial <adb-serial>
```

The profile is saved under `.local/device-checks`; manually complete the checklist with the model, exact HyperOS build, navigation mode and observations. Home's popup confirmation is session-local and user-reported. Copy compatibility details from Info for support.

Official references: [accessibility overlays](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_ACCESSIBILITY_OVERLAY), [AudioManager](https://developer.android.com/reference/android/media/AudioManager), [restricted settings](https://support.google.com/android/answer/12623953), [Xiaomi autostart](https://www.mi.com/global/support/faq/details/KA-507611/), [Xiaomi background app locking (menus vary)](https://www.mi.com/global/support/faq/details/KA-535435/), [Android process-exit reasons](https://developer.android.com/reference/android/app/ApplicationExitInfo), [Android app signing](https://developer.android.com/studio/publish/app-signing).

## 0.4.4 preparation and publication

Version 0.4.4, code 13, is the next release. Its notes use published `v0.4.3` as the baseline. Keep the existing release tags and signing key.

The `feat/brightness-gesture-settings` branch contains the version and reviewed notes. See [the 0.4.4 release checklist](docs/release-checklist-0.4.4.md). Push this branch, open a pull request into main, and select **Create a merge commit** to preserve the separate commits.

After GitHub merges the pull request, run each command in order from a clean workspace. Stop if a command fails:

```powershell
git switch main
git pull --ff-only origin main
Get-Content version.properties
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Finalize-Release.ps1
```

Confirm `versionName=0.4.4` and `versionCode=13` before running the finalizer. It fetches main and tags, checks that the current commit is on remote main, and fast-forwards local main. It then runs the release checks, builds the signed APK, verifies its signer and version, exports the four release files, and creates local tag `v0.4.4`. It does not push or publish. It refuses an unmerged or dirty checkout. `-CheckOnly` checks local preparation without fetching, switching branches, building, or tagging.

The final signed APK and tag must be created after the merge so both identify the merged main commit. Do not tag the feature branch or regenerate the reviewed notes after merging. If a command fails, resolve the failure before continuing.

Install the signed APK over the current release without uninstalling. Check retained settings, accessibility, downloads, brightness, shortcuts, rotation, and panel actions. Record actual phone results in the ignored `plans/compatibility.md`. Exact Redmi sizing, update installation, and battery checks remain pending until tested.

After the finalizer succeeds and the files are ready, push its existing tag:

```powershell
git push origin v0.4.4
```

Create a GitHub release for that tag. Use **Nudge 0.4.4** as the title and paste `releases/0.4.4/Nudge-0.4.4-notes.md` into the description. Attach these files from `releases/0.4.4/`:

- `Nudge-0.4.4.apk`
- `Nudge-0.4.4.apk.sha256`
- `Nudge-0.4.4.json`
- `Nudge-0.4.4-notes.md`

Publish only after all four files are attached. For a stable release, leave the pre-release option off and set the release as latest. Confirm publication before using `v0.4.4` as a future changelog baseline.
