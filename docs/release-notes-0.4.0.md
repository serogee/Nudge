# Nudge 0.4.0

Changes since published `v0.3.1`. Version code increases from 8 to 9.

## Added

- Add Increase brightness and Decrease brightness actions with five-percent steps.
- Open a custom MIUI-style brightness slider from gestures, alone or with the native volume popup.
- Set the slider's side, position, width, height, timeout, colors, and opacity from Home. Preview it without changing brightness.
- Create Home screen shortcuts for button actions with custom names and icons. Validate saved actions after unlock.
- Add native Notifications and Control panel actions to panel buttons and direct gestures.
- Reveal the launch icon pills from their centers, then move and rotate them into the N mark.
- Add Maximum brightness and Minimum brightness to panel buttons and direct gesture actions.
- Restore the previous brightness and automatic mode on a second tap, unless brightness changed externally.
- Toggle All handles, This handle, or Other handles without changing saved Enable switches.
- Show active handle-toggle states and provide Resume handles above the handle list.

## Changed

- Show bold handle initials with a small inset pill beside the configured edge.
- Group Cancel and Save inside a pill-shaped control. Keep the editor open and restore rename after saving or cancelling.
- Confirm saves after storage completes and show Save/Cancel toasts.

## Fixed

- Keep the selected handle, panel, or button during reorder scrolling. Make edge scrolling gradual in both directions.
- Center the launch mark with padding. Keep its rounded container inside Android's splash icon safe area.
- Download update APKs through Android Download Manager instead of passing the direct file link to Chrome.
- Show download progress, retry and cancel controls, and retain the download ID after reopening Nudge.
- Check the APK package, version, and signing identity before offering installation. Android requires installation confirmation.

## Compatibility and known limits

- The application ID remains `com.modukey.app`. The final build uses the existing release signing key.
- Install over 0.3.1 without uninstalling to keep saved settings. Recheck accessibility after updating.
- New backups use schema 7. Older backups remain importable. Older Nudge versions cannot import schema-7 backups.
- Brightness actions require Modify system settings access. Nudge requests access when a brightness action needs it.
- The brightness slider is a custom accessibility overlay. Its default 64 by 240 dp size approximates the native volume control.
- The default slider side assumes the native volume popup is on the right. Adjust its side, size, and position for your build.
- Nudge cannot read HyperOS's private volume-panel dimensions or provide an identical native brightness popup.
- Brightness steps switch to manual mode. Min/max recovery values and handle pauses are temporary and are not included in backups.
- Home shortcuts require launcher confirmation and a connected accessibility service. The master gesture switch can remain off.
- App and settings shortcuts use normal system unlock. Cancelling unlock cancels the action.
- Backups include shortcut definitions and icon references. Importing does not add icons to the launcher.
- Notifications and Control panel request Android's system panels. Some phones use the same panel for both actions.
- Downloads use Android Download Manager. Installation requires Android confirmation and may require Install unknown apps access.
- A release APK updates the release app. It cannot update the separate debug installation.
- Automated checks passed: 380 unit/Robolectric tests, lint with zero errors and 22 warnings, and the 0.4.0 debug build.
- Release tooling passed 29 changelog checks and 10 finalization checks. Signed APK verification runs after the GitHub merge.
- Redmi/HyperOS rendering, launcher confirmation, APK installation over 0.3.1, accessibility recovery, and overnight battery comparisons remain untested.
- No physical-device result is claimed. Keep actual results in the ignored `plans/compatibility.md`.
- These changes add no periodic restart jobs, wake locks, separate foreground service, or continuous idle redraw loop.
- Download progress checks run only while Info is resumed and a download is active. Brightness observers stop when no longer needed.

## Commit log

### Added

- track reversible brightness presets (dd8dca0)
- add scoped handle pause state (a4faed3)
- define brightness and handle control text (f71fe81)
- add brightness action symbols (5bcd7bb)
- share brightness and handle pause controls (43d5bb5)
- execute brightness and scoped handle actions (0ce5e9a)
- reconcile paused handle windows (2c7b50c)
- show scoped actions and live toggle states (9f5ae70)
- connect editor feedback and handle recovery (89a103a)
- center and animate the launch icon (56377a8)
- defer native shades until panel cleanup (2f600e6)
- define native system panel labels and symbols (ff4e2c0)
- open native notifications and control panels (56e7dd0)
- add brightness step actions (d08ad46)
- store brightness slider and shortcut configuration (9a17098)
- add slider and shortcut instructions (c65484f)
- show a bounded brightness slider (f43d50c)
- customize brightness slider from Home (a9f19ba)
- bind shortcut commands to service lifetime (e336087)
- render static icons for Home shortcuts (199345e)
- execute saved Home screen shortcuts (2e4ee3d)
- explain shortcut definitions in backup previews (73bc626)
- create named shortcuts from Home (965c459)

### Fixed

- track persistent APK download sessions (aace564)
- define direct download messages (03bab7b)
- download APKs through Android Download Manager (9299970)
- connect update downloads and installation (22b2d28)
- align handle badges with the tab icon (89c344d)
- report editor save completion (c2c4c55)
- retain reorder gestures during edge scrolling (13c2731)
- reuse brightness slider drawing geometry (7e89f17)
- observe brightness preview connection status (00a2ce5)

### Changed

- configure available button editor actions (252dae7)

### Maintenance

- explain direct APK updates (97c1f1e)
- record the APK download fix (3d7ca96)
- explain brightness and handle pause controls (cf0d750)
- record brightness and handle control changes (6ac9ccd)
- explain native shades in action editors (341a02b)
- describe reorder scrolling and launch behavior (73dbc3f)
- record launch and native shade improvements (b38c6c7)
- verify brightness popup eligibility (f557e0d)
- verify shortcut save failures (4c32bd6)
- explain brightness and Home shortcuts (1d44ccb)
- record unreleased brightness and shortcut features (bf739f6)
- wait for saved navigation to finish (5fad69e)
- observe shortcut state during composition (bf2eed6)
- record the published 0.3.1 baseline (5ba9178)
- finalize artifacts after the GitHub merge (209a486)
- explain the 0.4.0 publication steps (d086332)
- keep APK download targets newer than the app (6623c22)
