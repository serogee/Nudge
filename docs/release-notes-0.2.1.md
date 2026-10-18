# Nudge 0.2.1

Changes use v0.1.1 as the last confirmed published baseline. Earlier local builds and tags remain preserved.

## Release highlights

- Create, rename, duplicate, order, and delete shared panels. New configurations have no fixed panel slots. Keep independent handle controls.
- Assign six gestures to No action, Volume popup, a panel, a panel with Volume popup, or a direct action.
- Run settings, app, lock, and sound actions directly. Choose an app and optionally show the volume popup with volume adjustments.
- Open Android's native Input method picker from a panel or gesture. Use normal unlock when locked and a short-lived focused activity.
- Close panels automatically after 1–60 seconds without input. Pause during held input and reset the swipe sequence on expiry.
- Set Follow handle X/Y offsets. Keep custom center percentages when switching modes, with separate retained landscape layouts.
- Select independent open and close effects: None, Fade, Slide and fade, or Scale, slide, and fade. Follow the invoking handle edge.
- Use icon packs, static library icons such as Camera and Wallet, and container or standalone icons with saved corners.
- Select MIUI dark palette swatches and automatic button and built-in icon colors. Active sound controls use blue.
- Use Home, Handles, Panels, and Info navigation, joined editor tabs, a collapsing handle header, detailed permission status, and reset icons.
- Drag left grips to order cards and buttons. Show panel icons or No buttons set up. Use staged action, app, and icon pickers.
- Retain Fixed/Scrollable layouts, hidden overflow buttons, outside-touch policies, Back dismissal, and per-handle lock-screen availability.
- Preserve saved preferences through schema-3 migration and backup imports. Give added copies fresh IDs and disable them for review.
- Share one accessibility service. Keep timers and effects limited to active panels or actions. Store artifacts in version folders.

## Compatibility and known limits

- Automated validation passed: 167 unit/Robolectric tests, lint with zero errors and 14 existing warnings, and the debug APK build.
- Native picker tests cover API 31 and 36 focus, dismissal, suppression, unlock, stale requests, unsupported devices, and screen-off cleanup.
- Physical Redmi Note 12 / HyperOS checks remain pending, including keyboard selection, native popup coexistence, touch delivery, unlock, Recents recovery, and battery use.
- Signed installation over an older version and settings retention still require a phone. Automated checks do not establish device compatibility.
- The HyperOS autostart shortcut depends on the build. Keep the manual setup steps available.
- Automatic icon color applies to built-in symbols. App and icon-pack images retain their original colors.
- Icon background visibility has On/Off and per-button inheritance, without an automatic container choice.
- Some controls lack individual reset icons. Handle badges use portrait placement. Edit shares the bottom card action row with Duplicate and Delete.

## Added

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

## Fixed

- round customization highlights into half-squircles (e021154)
- apply handle visibility immediately (d26cb05)
- round the launch icon container (40932fb)
- keep configuration storage off the main thread (1239cd8)
- bind settings viewmodels to the current application (79d486e)

## Changed

- define clear handle and panel text (a0f6b04)
- classify update check failures (402db0b)
- remove the legacy runtime writer (adb6fac)

## Maintenance

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
