# Changelog

## Unreleased

### Added

- Add sticky controls for real handle and panel editing previews.
- Extend touch length to the usable short screen edge, with an option for the longer edge. Preserve saved values when rendering is limited.
- Add full-screen soft hide with a reveal-only gesture, a finite bounce, and adjustable inactivity delay.
- Add independent empty panel buttons with container-only, blank, and slash appearances.
- Add an outside-touch option that passes touches through and keeps the panel open.
- Add independent button-background and icon opacity controls, including automatic colors.
- Record bounded panel dismissal reasons and event context in local diagnostics.

### Changed

- Move panel search into a separate gesture selection dialog. Allow Show Volume Popup with panels and every direct action.
- Keep handle badges visible beside bold titles. Use notched rounded-square badges, an edge-handle navigation icon, and the shared N app mark.
- Combine color swatches into one palette and calculate symbol contrast against the effective background.
- Migrate schema-3 configuration to schema 4 while retaining assignments and physical panel placement.

### Fixed

- Allow page scrolling over display-only mini previews. Cancel panel button clicks after drag movement.
- Make positive Follow handle Y offsets move panels upward.
- Preserve temporary editor choices during activity recreation and configuration loading.
- Keep legacy popup flags limited to volume adjustments. Reject empty buttons as direct gesture actions.
- Keep panels open for the first activity observation, stale events, and ambiguous popup windows. Require a confirmed app transition before app-change dismissal.
- Track required activity events before opening panels. Keep sound updates and volume controls from causing false app changes.
- Report unexpected runtime panel window removal and reset its swipe sequence.

### Pending verification

- Physical Redmi/HyperOS checks for full-screen detection, Back after pass-through touches, popup coexistence, and battery use remain pending.
- Automated tests reproduce the previous false-dismissal paths. The cause of the reported Redmi incident still needs device diagnostics.

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
