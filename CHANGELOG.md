# Changelog

## Unreleased

### Added

- Add Check for updates in Behavior & help with automatic GitHub version comparison, APK and release-note links, and network error handling. A saved Include pre-releases (testing) option defaults off; checks run only when tapped.
- make indicator thickness a percentage (634e444)

### Fixed

- Round the shared app icon background, including the launch screen, and use that shape directly inside the app.
- Make handle visible immediately shows or hides the handle in both previews. Its appearance controls are grey and disabled while it is off, with saved values retained.
- Round customization highlights into half-squircles (e021154; included in the unpublished local 0.1.2 build).

### Maintenance

- Recommend Background autostart during HyperOS setup, document menu variations, and reserve battery setting changes for disconnections. Exact Redmi requirements remain pending device testing.
- Move the local compatibility checklist to plans/compatibility.md and exclude it from Git.

## 0.1.2 - 2026-10-17 (local build only; not published)

### Fixed

- round customization highlights into half-squircles (e021154)

### Maintenance

- define commit scopes and message rules (c2aeda2)
- generate changelogs from scoped commits (cefeb75)

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
