# Changelog

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

### Added

- Create, name, duplicate, order, enable, and delete independent handles, each with three private panels.
- Assign tap, hold, swipe and hold, and three immediate swipe stages. Volume sequences use a configurable expiry without background timers.
- Build Fixed or Scrollable panels in either grid direction, with live previews, safe placement, and retained landscape overrides. Fixed overflow buttons remain saved and editable but do not appear or cause scrolling.
- Choose whether each panel blocks or passes through an outside touch while closing. Back dismisses the panel without navigating the app beneath.
- Add media, ringtone, DND, lock, settings, and app buttons. Choose static icon packs, manual icon overrides, shapes, and corners.
- Enable handles independently on the awake lock screen. App and settings actions continue only after the normal system unlock succeeds.
- Export and import configuration backups, including hidden buttons and icon references. Added copies start disabled; replacement leaves the master switch off.

### Changed

- Replace the single-handle interface with Home, Handles, and Settings. Remove slogans and use short, consistent app text from string resources.
- Migrate existing customization and activation into Handle 1 while retaining global settings and unrelated preferences.
- Share one accessibility service across handles. Reconcile changed windows, release closed panel resources, and load cached icons off the main thread.

### Compatibility

- Redmi Note 12 / HyperOS native-popup coexistence, touch delivery, authentication, Recents recovery, battery measurements, and signed upgrade checks remain pending physical-device testing.
- No release version, signing identity, published notes, or release tags changed.

### Maintenance

- Store each version's release files in `releases/<version>/`. Retain prior-APK signer checks for version folders and the older flat layout.

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
