# Changelog

## Unreleased

### Added

- Add native Notifications and Control panel actions to panel buttons and direct gestures.
- Reveal the launch icon pills from their centers, then move and rotate them into the N mark.
- Add Maximum brightness and Minimum brightness to panel buttons and direct gesture actions.
- Restore the previous brightness and automatic mode on a second tap, unless brightness changed externally.
- Toggle All handles, This handle, or Other handles without changing saved Enable switches.
- Show active handle-toggle states and provide Resume handles above the handle list.

### Changed

- Show bold handle initials with a small inset pill beside the configured edge.
- Group Cancel and Save inside a pill-shaped control. Keep the editor open and restore rename after saving or cancelling.
- Confirm saves after storage completes and show Save/Cancel toasts.

### Fixed

- Keep the selected handle, panel, or button during reorder scrolling. Make edge scrolling gradual in both directions.
- Center the launch mark with padding. Keep its rounded container inside Android's splash icon safe area.
- Download update APKs through Android Download Manager instead of passing the direct file link to Chrome.
- Show download progress, retry and cancel controls, and retain the download ID after reopening Nudge.
- Check the APK package, version, and signing identity before offering installation. Android requires installation confirmation.

## 0.3.1 - 2026-10-19

Changes since v0.3.0, the latest published release. Version code increases from 7 to 8.

### Release highlights

- Set visible handle thickness in dp, from 0 to 32 dp, with a 4 dp default. Length remains a percentage.
- Save or cancel all handle and panel edits from the header. Live previews show pending changes immediately.
- Choose Save, Discard, or Keep editing before leaving an editor. Names, gestures, buttons, and layout changes share its draft.
- Set Hold time and Swipe and hold time independently for each handle, from 300 to 3000 ms.
- Assign Close panels or Screenshot to a panel button or direct gesture. Close panels resets the owning swipe sequence.
- Request Android's screenshot UI after interactive Nudge panels are removed. Nudge does not read or store the image.
- Select a background-free Nudge mark from the built-in icon library. It uses the panel icon color and supports Standalone.
- Use the separated pill app icon throughout Nudge and ExtraBold initials in handle badges.
- Identify gesture choices with a four-square icon and Panel: or Action: prefixes. Panel options have clear gaps.
- Keep wrapped action names centered. Rename Input method picker to Input Method.
- Show icons in Excluded apps. Selected apps move to the top; clearing a selection restores its original order.
- Label both editor previews Visual Preview (Non-interactive).

### Fixes

- Truncate long editor titles with an ellipsis. Keep Save, Cancel, and rename controls visible.
- Track handle windows before attachment completes. Remove pending and attached windows during replacement, reconnection, and rotation.
- Apply the selected editor orientation to on-phone guides. Landscape Bottom appears on the left of an upright phone.
- Keep runtime Bottom at the logical bottom in both landscape rotations.
- Rotate panel guide content and use the same safe insets in mini previews and on-phone guides.

### Compatibility and known limits

- Automated checks passed: 279 unit/Robolectric tests, lint with zero errors and 15 existing warnings, and the debug APK build.
- The application ID remains com.modukey.app. Use the existing signing key for the final release build and updates.
- Existing thickness percentages convert once using each handle's saved touch depth. Other settings keep their values. Missing hold-time fields use 500 ms defaults.
- Changing touch depth keeps dp thickness fixed. Drawing is limited to the touch area without changing the saved thickness.
- New backups use schema 5. Older backups remain importable; older Nudge versions cannot import schema-5 backups.
- Pending editor changes stay in memory. Save applies them to normal operation and storage. Cancel restores saved settings.
- Drafts survive rotation and temporary backgrounding. Closing the activity or process discards unsaved edits. Backups contain saved settings.
- The release script verifies the signed APK and compares its signer with the archived 0.3.0 APK when available.
- The attached Nudge-0.3.1.json records signed-build verification, the source commit, signing certificate, and APK checksum.
- Redmi/HyperOS, installation over 0.3.0, screenshot UI, system text sizes, navigation, and overnight battery checks remain pending.
- The reported intermittent duplicate handle was not reproduced on the Redmi. Automated tests cover window ownership and repeated rotation.
- Visible handles can appear in screenshots. Android provides no reliable pre-capture signal for every device screenshot method.
- This release adds no polling, wake locks, periodic jobs, or idle animation loop.

### Added

- persist independent hold times (3448963)
- recognize configurable hold intervals (d9dc37f)
- apply hold delays to native handle input (069775d)
- expose hold timing beside gesture assignments (674b33a)
- add the Nudge mark to the icon library (2f128d1)
- map editor geometry across rotations (375afb6)
- defer native screenshots until panels clear (9fe8ce1)
- define panel utility and preview labels (8ac2fac)
- add panel closing and screenshot actions (60851f8)
- rotate non-touchable panel guides (11a6578)
- connect panel utilities and rotated editing guides (152186e)
- show icons and pin excluded apps (bc6b48a)
- stage handle and panel edits (849ed18)
- preview pending editor changes (d6340c4)
- save or cancel handle and panel edits (a4439cf)
- use dp for visible handle thickness (f109c90)

### Fixed

- base Nudge 0.3.0 notes on v0.2.1 (650adcc)
- use the separated pill app icon (a7e3991)
- strengthen handle icon initials (5c0309e)
- identify panel and direct gesture choices (259797f)
- center wrapped action names (57f074a)
- clarify preview and input method labels (7281b3e)
- retain pending handle window ownership (71fdcfb)
- share safe insets across editor previews (85c72e9)

### Maintenance

- distinguish working tags from published releases (727f213)
- explain published and working tag baselines (12fe475)
- describe hold controls and icon choices (aa5150c)
- record pending gesture and icon changes (5341c3c)
- verify gesture timing and picker controls (f1f059f)
- verify panel utilities and rotation cleanup (8827850)
- verify utility choices and preview orientation (f082c36)
- explain panel utilities and rotated previews (3685f66)
- record panel utilities and rotation fixes (ceb5775)
- record excluded app list improvements (d395dc8)
- explain editor drafts and save controls (b6d3043)
- record editor save controls (42b16b0)
- mark Nudge 0.3.0 as published (2bd9b7f)
- explain the 0.3.1 publication steps (c2c2db7)
- prepare Nudge 0.3.1 (b2dfe37)
- explain fixed handle thickness (0a4fe5f)
- clarify the thickness follow-up (4f6540a)
- include dp thickness in 0.3.1 notes (386775a)
- record authorized local release merges (ad5afea)
- explain local merge publication (6ca0d6c)

## 0.3.0 - 2026-10-18

Changes since v0.2.1, the latest published baseline.

### Release highlights

- Add full-screen soft hide. The first gesture reveals the handle with a brief bounce. Later gestures run assigned actions.
- Extend touch length to the usable short edge, with an option for the longer edge. Preserve saved values when rendering is limited.
- Control real editing overlays with sticky Preview switches. Keep temporary preview choices during activity recreation.
- Add independent empty panel buttons with container-only, blank, or slash appearances.
- Add an outside-touch option that passes touches through and keeps the panel open. Back and timeout dismissal remain available.
- Move panel search into a separate selection dialog. Select Show Volume Popup independently for panels and direct actions.
- Set button-background and icon opacity independently. Use one palette and automatic contrast for built-in symbols.
- Keep handle badges beside bold titles. Add orientation-aware notches, an edge-handle tab icon, and the shared N app mark.
- Make positive Follow handle Y offsets move panels upward. Preserve old physical placement and assignments through schema-4 migration.

### Fixes

- Allow page scrolling over display-only mini previews.
- Cancel panel button clicks after drag movement or multiple pointers. Retain valid taps and accessibility actions.
- Keep panels open for initial activity observations, stale events, and ambiguous popup windows. Close on confirmed app transitions.
- Observe required activity events before opening panels. Keep volume controls and sound updates from causing false app changes.
- Record bounded dismissal reasons, package context, event class, and panel age. Copy these details from Info after an incident.
- Report unexpected panel-window removal once and reset its swipe sequence.
- Preserve editor choices while settings load. Keep legacy popup flags limited to media volume steps.
- Keep empty buttons out of direct gesture actions. Retain minimum touch length on small displays and stop the bounce when soft hide ends.

### Compatibility and known limits

- Automated checks passed: 217 unit/Robolectric tests, lint with zero errors and seven warnings, and the signed release build.
- The verified signing certificate matches 0.2.1. Version code increases from 6 to 7. The application ID remains com.modukey.app.
- Panel dismissal regressions cover API 31 and 36. The cause of the reported Redmi incident still needs device diagnostics.
- Physical Redmi/HyperOS, popup coexistence, touch routing, unlock, Recents, battery comparisons, and signed upgrade checks remain pending.
- App and icon-pack images retain their colors. Automatic contrast applies to built-in symbols. Some controls lack individual reset icons.
- Unresolved activity events cannot confirm an app transition. Multi-window and HyperOS reporting still need device checks.

### Added

- preserve expanded handle and panel controls (1cf6b20)
- keep panels open after outside touches (00b63fd)
- add independent color opacity controls (ade7175)
- separate gesture targets from volume popup requests (0bf5d72)
- add independent empty panel buttons (a11edca)
- expose screen-based touch length limits (9e3e271)
- shape the shared app mark into an N (a799493)
- show handle placement with notched badges (acb2de9)
- retain app and handle icons in headers (7d87ca5)
- define an event-driven soft hide session (1e9c1d1)
- control editing overlays with sticky preview switches (0003c27)
- reveal full-screen handles before activation (4314309)

### Fixed

- let page scroll through mini previews (b052d9a)
- cancel panel clicks after drag movement (fb83956)
- ignore unused legacy popup flags (55ed09c)
- retain minimum length on tiny displays (b36ac02)
- stop the bounce when soft hide ends (27b1d75)
- reject empty direct gesture actions (0cc76f7)
- retain editor choices while configuration loads (6e6f25c)
- distinguish app activities from popup events (4ea4d31)
- report panel window dismissal reasons (b9a15b5)
- preserve panels across ambiguous window events (235ca63)

### Maintenance

- remove obsolete customization instructions (cac94f3)
- describe the new handle and panel controls (28766af)
- record handle and panel changes under Unreleased (06c8b96)
- clarify configuration and timeout ranges (72213ee)
- verify sticky preview and empty button controls (c172b08)
- verify preview scrolling and drag cancellation (a88a134)
- verify native handle soft hide (d5b549c)
- verify combined gesture popup requests (9b36c6c)
- remove trailing action whitespace (f473906)
- bound both foreground metadata caches (f038c3b)
- retain panels during sound updates (89bcc4c)
- use independent picker button IDs (8ba830e)
- record panel dismissal findings (9a51440)
- link panel dismissal diagnostics (3d77e15)
- record unreleased panel dismissal fixes (9343f50)
- record the Nudge 0.3.0 release workflow (42c7697)
- prepare Nudge 0.3.0 (1419b37)
- describe the post-merge packaging steps (faa69e8)

## 0.2.1 - 2026-10-18

### Release highlights

- Create and share independent panels. Assign gestures to panels, volume, combined actions, or direct app, settings, sound, lock, and keyboard-picker actions.
- Add Android's native Input method picker with normal unlock, focused requests, stale-request validation, and bounded cleanup.
- Add optional panel auto-close, pause it during held input, and reset swipe progression on expiry. Keep direct-action sequence timing event-driven.
- Retain Follow handle offsets and custom percentages separately. Preserve portrait and landscape settings, Fixed/Scrollable layouts, and hidden overflow buttons.
- Add separate open/close effects, optional volume-popup flags, standalone icon shapes, icon packs, built-in icons, MIUI swatches, and automatic colors.
- Add Home/Handles/Panels/Info navigation, joined tabs, pinned handle titles, detailed permission status, drag grips, panel-card previews, and staged pickers.
- Add reset icons to common settings and optional HyperOS autostart navigation. Preserve clear app text, manual recovery guidance, and the existing Info controls.
- Preserve single-handle preferences and backups through schema-3 migration. Remap copied IDs, retain shared links, and disable imported handles for review.
- Retain lock-screen options, outside-touch policies, Back dismissal, one accessibility service, bounded icon caching, and versioned release folders.

### Compatibility and known limits

- Automated validation passed: 167 unit/Robolectric tests, lint with zero errors and 14 existing warnings, and the debug APK build.
- Physical Redmi/HyperOS, native keyboard and volume popups, touch delivery, unlock, Recents, battery, and signed installation/settings checks remain pending.
- The autostart shortcut depends on the build. App/pack images keep their colors; automatic contrast applies to built-in symbols.
- Icon-background visibility has no automatic container choice. Some controls lack individual reset icons. Handle badges use portrait placement, and Edit shares the bottom action row.

### Added

- make indicator thickness a percentage (634e444)
- check GitHub updates automatically on request (9ae4350)
- add per-handle configuration and panel models (4e4e5af)
- define editing sessions and handle eligibility (d1c7f74)
- reconcile ordered handle windows (33bc560)
- add native handle input and status (dc15e8d)
- load static icon pack resources (6267535)
- add panel actions and unlock continuation (de093e1)
- share controls and render panel grids (5a3e82e)
- host panels with touch dismissal (30f6451)
- connect independent handle windows (709fc59)
- preview panel placement and handle overlaps (396deac)
- connect handle and panel settings (819d64f)
- add an event-driven panel timeout (315e1aa)
- add a HyperOS autostart shortcut (cfed5ed)
- report the active panel ID (55a29d1)
- add shared panels and direct gesture actions (c9f0600)
- open the native input method picker (ed56829)

### Fixed

- round customization highlights into half-squircles (e021154)
- apply handle visibility immediately (d26cb05)
- round the launch icon container (40932fb)
- keep configuration storage off the main thread (1239cd8)
- bind settings viewmodels to the current application (79d486e)

### Changed

- define clear handle and panel text (a0f6b04)
- classify update check failures (402db0b)
- remove the legacy runtime writer (adb6fac)

### Maintenance

- define commit scopes and message rules (c2aeda2)
- generate changelogs from scoped commits (cefeb75)
- prepare Nudge 0.1.2 (25b6ad1)
- prepare Nudge 0.1.3 (c84ca0a)
- require explicit release requests (7f02c30)
- keep the compatibility checklist local (54cc453)
- recommend HyperOS background autostart (2966580)
- finalize Nudge 0.1.2 (942146b)
- define app text writing rules (45fd487)
- verify handle and panel customization (b22e8c2)
- document handle and panel controls (229cfff)
- group artifacts by version (013382e)
- update the 0.2.0 preparation context (18b34c4)
- finalize Nudge 0.2.0 (d577022)
- verify panel offsets and screen bounds (319d261)
- verify panel and action persistence (ad14a52)
- verify volume flags and unlock sources (3c2312e)
- verify panel touch completion (64865ea)
- verify panel window cleanup (df95eaf)
- verify directional panel effects (292cce5)
- describe independent panels and gesture actions (66d36b6)
- update the app setup and feature guide (d9325fc)

## 0.2.0 - 2026-10-17

### Added

- make indicator thickness a percentage (634e444)
- check GitHub updates automatically on request (9ae4350)
- add per-handle configuration and panel models (4e4e5af)
- define editing sessions and handle eligibility (d1c7f74)
- reconcile ordered handle windows (33bc560)
- add native handle input and status (dc15e8d)
- load static icon pack resources (6267535)
- add panel actions and unlock continuation (de093e1)
- share controls and render panel grids (5a3e82e)
- host panels with touch dismissal (30f6451)
- connect independent handle windows (709fc59)
- preview panel placement and handle overlaps (396deac)
- connect handle and panel settings (819d64f)

### Fixed

- round customization highlights into half-squircles (e021154)
- apply handle visibility immediately (d26cb05)
- round the launch icon container (40932fb)
- keep configuration storage off the main thread (1239cd8)
- bind settings viewmodels to the current application (79d486e)

### Changed

- define clear handle and panel text (a0f6b04)
- classify update check failures (402db0b)
- remove the legacy runtime writer (adb6fac)

### Maintenance

- define commit scopes and message rules (c2aeda2)
- generate changelogs from scoped commits (cefeb75)
- prepare Nudge 0.1.2 (25b6ad1)
- prepare Nudge 0.1.3 (c84ca0a)
- require explicit release requests (7f02c30)
- keep the compatibility checklist local (54cc453)
- recommend HyperOS background autostart (2966580)
- finalize Nudge 0.1.2 (942146b)
- define app text writing rules (45fd487)
- verify handle and panel customization (b22e8c2)
- document handle and panel controls (229cfff)
- group artifacts by version (013382e)
- update the 0.2.0 preparation context (18b34c4)

## Unreleased

No pending changes. Reviewed development entries are included in the 0.2.1 notes above.

## 0.1.2 - 2026-10-17

### Added

- Set indicator thickness as a continuous percentage of touch depth. Existing thickness settings migrate once and stay proportional when resized. (634e444)
- Check GitHub versions automatically from Behavior & help, with APK and release-note links. Include pre-releases (testing) is saved and defaults off; checks run only when tapped. (9ae4350)

### Fixed

- Round customization guides into half-squircles with a flat side against the screen edge. (e021154)
- Apply Make handle visible immediately in both previews. Grey out and disable handle appearance controls while hidden, retaining saved values. (d26cb05)
- Use the same rounded icon container on launch and inside the app. (40932fb)

### Maintenance

- Recommend Background autostart during HyperOS setup and document menu variations. Battery restriction changes remain recovery steps for disconnections. (2966580)
- Generate changelogs from scoped commits and document commit conventions. (cefeb75, c2aeda2)
- Keep the compatibility checklist local at plans/compatibility.md, excluded from Git. (54cc453)

### Update and compatibility

- Version code 4. Install over 0.1.1 or the earlier local 0.1.2 build without uninstalling to preserve settings; releases use the same application ID and signing key.
- The testing preference starts off and survives customization reset. Update requests run off the main thread and report network or rate-limit failures.
- Debug verification: 53 JVM/Robolectric tests passed; lint reported 0 errors and 14 existing warnings. Release packaging runs signed-build and certificate checks, recorded in the generated build metadata.
- Redmi Note 12 / HyperOS 1 popup behavior, Recents recovery, battery use and installation over a previous release remain pending physical-device testing.

## 0.1.1 — 2026-10-17

- Continuous, non-touchable indicator and touch-area preview while Customize is visible, even with the master switch off. Normal visibility returns on navigation or backgrounding.
- Immediate shared settings state, display-frame coalescing, 150 ms save debounce and final-value flush. Failed saves keep edits visible with a retry action; field changes preserve unrelated settings.
- Center-based position percentages, stable resizing and grab-offset dragging. Indicator length is now a percentage of touch length; saved thickness/radius survive smaller touch areas.
- Default placement is inherited in landscape, including logical bottom. An optional landscape override retains separate placement values.
- Optional awake lock-screen availability, off by default. Screen-off/AOD operation remains disabled; the phone is never woken or unlocked.
- Separate enabled-permission and service-connection status, idempotent cleanup, interruption recovery, bounded local lifecycle/process-exit diagnostics and expanded HyperOS recovery guidance.
- Reuse the launcher icon for in-app branding.
- Preserve application ID, release signing identity and existing preferences, with one-time indicator-length migration. Version code 2.
- Redmi Recents behavior, lock-screen compatibility, battery measurements and installation over 0.1.0 remain pending physical-device testing.

## 0.1.0 — 2026-10-17

- Initial Nudge release: an invisible, configurable accessibility edge trigger for the native volume popup.
- Compose setup, live preview, gesture and appearance controls, landscape settings and app exclusions.
- HyperOS setup/recovery guidance, explicit connection status and session-local native-popup confirmation.
- Battery-conscious service: accessibility window events are disabled unless app exclusions require them; no polling, wake locks, periodic work or ongoing overlay animation.
- Local Git milestones, persistent preferences, signed APK release tooling and device verification checklist.
- Redmi Note 12 / HyperOS 1 compatibility remains pending physical-device testing; see docs/compatibility.md.
