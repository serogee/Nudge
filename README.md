# Nudge

Configurable edge handles for Android volume controls and custom button panels. Built with Kotlin, Jetpack Compose, and Material 3 for personal use on Redmi Note 12 / HyperOS 1.

Each handle has its own gestures, appearance, placement, exclusions, and lock-screen option. Panels are created and edited separately. Several handles can open the same panel. All handles share one accessibility service. New and duplicated handles start disabled. Home provides a master switch and actual connection status.

The default handle uses a 16 × 120 dp touch area on the right edge, centered at 65%. An inward swipe of 24 dp within 600 ms requests the native volume popup without changing volume. The handle can be invisible; its touch area still receives touches.

Version 0.2.1 adds shared panels, direct gesture actions, and the native input method picker. See [Handles and panels](docs/handles-and-panels.md) for controls and behavior. The archived 0.2.0 build has the earlier per-handle panels.

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

Open Handles to create, order, duplicate, or edit handles. The first handle receives touches where areas overlap. Editors show non-touchable phone guides while visible, even with the master off. Normal activation resumes when editing ends.

Open Panels to create, name, order, duplicate, edit, or delete panels. A fresh configuration has no panels. Assign each handle gesture to No action, Volume popup, a panel, or a panel with Volume popup. Existing configured or assigned panels migrate automatically; unused empty slots are omitted.

Select Direct action in a gesture assignment to use Lock screen, Open app, settings, or any sound control without a panel. Open app includes an app picker. Volume up and Volume down can also show the volume popup. Save applies the complete assignment; Cancel keeps the previous one. Swipe sequence timeout applies to actions without an open panel and uses an expiry timestamp without background work.

Add Input method picker as a panel button or a direct gesture action to open Android's native keyboard chooser. Nudge uses a short-lived internal activity to obtain input focus. It does not enable a keyboard or choose one for you. Locked use requires normal system unlock. A failed request closes the internal activity and reports an error.

Panels keep their own buttons, appearance, layouts, placement, and landscape settings. Follow handle uses the handle that opens the panel. Select a preview handle in the panel editor to check placement. Deleting a panel clears its gesture links and keeps the volume part of combined assignments. Deleting a handle keeps its panels available.

Fixed panels show only the first buttons within their row and column limits and never scroll. Extra buttons stay saved and editable with a Not shown label. Scrollable panels expose all buttons inside an adjustable viewport. Switching modes retains button order and mode settings.

Each panel chooses whether an outside touch closes and blocks the touch or closes and passes it through. Back closes the panel without navigating the app beneath. Handle gestures remain available. Preview buttons do not run actions; Test panel opens working controls.

Each panel can close automatically after 1–60 seconds without touch input. This option starts off. Opening or touching the panel restarts its timer; held input pauses it. Expiry closes the panel and resets its handle's swipe sequence to the first assigned stage. Editing guides stay open. Only an open interactive panel has a pending timer; closing or replacing it cancels that timer.

Each panel has separate Open animation and Close animation settings. Both offer None, Fade, Slide and fade, or Scale, slide, and fade. Existing panels start with a closing effect that matches their saved opening effect. Later changes are independent. Exit moves toward the invoking handle's edge. Input stops immediately on dismissal; rendered content stays until the short exit ends. Screen-off, editor changes, and service cleanup remove panels immediately. Animations respect the system setting and schedule no idle frames.

Handle and panel landscape overrides retain separate values when disabled. Bottom remains the logical bottom after rotation. Center-based placement remains stable during resizing, subject to safe screen bounds.

Lock-screen availability is configured per handle and defaults off. All configured actions remain available. Apps and settings request Android's normal unlock step before opening. Nudge does not wake the screen or operate on always-on display. After reboot, unlock the phone once before use.

Backups use the system file picker and include panels, gesture links, hidden buttons, and icon references. Add imports disabled handle copies and independent panel copies, preserving shared links within the imported group. Replace requires confirmation and leaves the master switch off. Older backups migrate on import. Icon packs must be installed separately.

## Battery use and compatibility

One system-bound service manages all handles. There is no polling, wake lock, periodic restart work, foreground service, or idle redraw loop. Gesture timing uses touch timestamps. Panels release their composition and observers when closed. Icons load off the main thread with an 8 MiB bitmap cache. Update checks run only when requested.

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
releases/0.1.2/
  Nudge-0.1.2.apk
  Nudge-0.1.2.apk.sha256
  Nudge-0.1.2-notes.md
  Nudge-0.1.2.json
```

Build metadata records the commit, SDKs, signer, and verification status. Exported notes are copied from the committed, reviewed `docs/release-notes-<version>.md`. Changelog generation with `-Artifact` uses the same version folder. Prior-APK signer checks prefer the version folder and also accept the older flat layout. Archived builds remain under `releases/archive/`. All release artifacts remain ignored by Git; upload the files inside the version folder when publishing on GitHub.

For each new distributed version, commit the completed features/fixes, then increase `versionName` and `versionCode` in `version.properties`. Generate the changelog from the commits since the previous reachable release tag:

```powershell
# Preview without changing files. After 0.1.1, for example:
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Generate-Changelog.ps1 -Version 0.1.2
# After changing version.properties, update that version's changelog and notes:
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Generate-Changelog.ps1 -Write
# Review the generated files, add device observations, and commit the version and notes.
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Release.ps1 -CreateTag
```

The generator uses the first line of each full commit message, including messages with body bullets immediately after the title. `feat` becomes Added, `fix` Fixed, `perf` Performance, and build/docs/test/chore commits Maintenance. Other changes go under Changed; `!` or a `BREAKING CHANGE:` footer identifies breaking changes. Scoped and older unscoped titles both work. Each entry includes its commit hash. Scopes and message rules are defined in [.plans/scopes.md](.plans/scopes.md) and [AGENTS.md](AGENTS.md).

`-Write` replaces only the current unreleased version's entry and creates `docs/release-notes-<version>.md`. Re-running it is safe before tagging, but replaces manual edits to those generated notes, so review and add manual compatibility observations after the final generation. Existing released entries are preserved; writing an already tagged version is refused. Previewing a tagged version uses its tag's commit rather than newer work. `-FromTag v0.1.1` can select an explicit baseline; it must be an ancestor and lower version. No extra tools, network calls, automatic version bumps or GitHub publishing are involved. To check the generator, run `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Test-Changelog.ps1`.

Install the new release over the old release, without uninstalling, to retain settings. Debug and release are separate installations. Recheck accessibility after updating. Reverting a bug fix means a new commit and a **higher** version code; do not move release tags or expect Android to accept a downgrade.

In **Info → Updates**, tap **Check for updates**. Nudge reads the [public GitHub release list](https://github.com/serogee/Nudge/releases), compares version numbers automatically and reports an update, the current version, or a newer local build. **Include pre-releases (testing)** is saved and defaults to off: off checks stable releases only; on checks stable releases and published test versions, with pre-releases labelled. Changing this option cancels the active check, clears its result and discards any late reply. It does not start a new request. Customization reset preserves this update preference. Drafts and unsupported version tags are excluded. Only published releases are used, not local Git tags. Numeric versions compare correctly (for example, 0.1.10 is newer than 0.1.9); the debug suffix does not count as a new release. Checks are bounded by connection/read timeouts, run off the main thread and report network/rate-limit errors instead of claiming the app is current.

If only pre-releases are published, checks with testing off report that no stable release is available. Enable testing to include them. The app does not change GitHub release labels.

When a newer version is available, **Download APK** opens that release's attached `Nudge-<version>.apk` in the external browser or download handler. **View release notes** opens its GitHub page. Downloads and installation require your action; Android confirms installation and checks update signing. A release APK installs separately from a debug copy. Missing APKs, unavailable browsers and missing releases are reported. No account, token, device diagnostics or saved settings are sent with the metadata request. See the [GitHub releases API](https://docs.github.com/en/rest/releases/releases) for the public metadata format.

The repository uses Git on `main` with the existing GitHub remote. Releases and uploads remain manual; the release script does not push or publish to a store. The app has no scheduled updater or automatic installer.

Version 0.1.1 migrates the old indicator length once to `oldIndicatorLengthDp / oldTouchLengthDp`. Existing numeric position percentages are retained with their new center meaning. Landscape availability, saved landscape placement and all unrelated preferences are preserved; the new landscape override and lock-screen flags default off. A slight initial position shift from 0.1.0 reflects the center definition.

Version 0.1.2 migrates indicator thickness once to `oldIndicatorThicknessDp / oldTouchDepthDp`, capped at 100% where it already filled the touch depth. Existing saved thickness and depth use their old validity limits before conversion. Later touch-depth changes keep this percentage, and all unrelated preferences are retained.

## Device verification

Keep device results in the local, Git-ignored checklist at `plans/compatibility.md` (formerly `docs/compatibility.md`). This file is not included in a fresh clone. An AOSP emulator or Robolectric cannot establish HyperOS compatibility. With a connected, authorized phone, run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Device-Profile.ps1
# For multiple devices: add -Serial <adb-serial>
```

The profile is saved under `.local/device-checks`; manually complete the checklist with the model, exact HyperOS build, navigation mode and observations. Home's popup confirmation is session-local and user-reported. Copy compatibility details from Info for support.

Official references: [accessibility overlays](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_ACCESSIBILITY_OVERLAY), [AudioManager](https://developer.android.com/reference/android/media/AudioManager), [restricted settings](https://support.google.com/android/answer/12623953), [Xiaomi autostart](https://www.mi.com/global/support/faq/details/KA-507611/), [Xiaomi background app locking (menus vary)](https://www.mi.com/global/support/faq/details/KA-535435/), [Android process-exit reasons](https://developer.android.com/reference/android/app/ApplicationExitInfo), [Android app signing](https://developer.android.com/studio/publish/app-signing).
