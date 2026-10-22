# Nudge 0.4.3

## Added

- Set **Slide to adjust** for swipe stages, Hold, or Swipe and hold. Keep your finger down, then slide vertically to adjust media volume or brightness. Select exactly one popup. Tap does not offer this option. (6e079ee)
- Set separate volume and brightness travel distances for each handle. The default is 240 dp. Optional editor guides show the full distance and current value without changing system values. (6e079ee)
- Select **All handles**, **This handle**, or **Other handles** for Toggle handle visibility. Visibility stays saved for each handle. (a0dd097)
- Add **Toggle and hide handles** with separate pause/resume and hide/show scopes. Each group toggles independently. An open panel stays usable after its invoking handle is paused. (a0dd097)
- Set active button background and icon colors in each panel. Each effect has automatic and manual colors. Standalone icons use an active outline. There are no individual button color overrides. (5474efb)
- Calibrate brightness at 0%, 25%, 50%, 75%, and 100% of the native slider in Help. Calibration stays on this device and resets after an OS build change. (fe021c9)

## Fixed

- Map Return brightness percentages to perceptual slider positions. Restore previous brightness with its exact system value and mode. (fe021c9)
- Return immediately from a manually selected maximum when no previous value is available. The Maximum brightness button reflects the current manual maximum. (fe021c9)
- Recognize Swipe and hold by displacement from the start. Movement beyond the distance keeps its timer. Retreat below the distance resets it. Choose Any direction or Inward only; Any direction is the default for existing and new handles. (22fd33e)
- Remove the brightness and MIUI background comparison samples. Keep the live brightness preview and appearance controls. (1e2ff9c)
- Keep one handle drawing during window replacement to prevent overlapping translucent handles from appearing brighter when a panel opens. (6973ece)
- Keep swipe sequences reset after their panel closes or a Close panels action runs during continuous adjustment. (625a259)

## Compatibility and known limits

- New backups use schema 18. Older backups remain importable. Older Nudge versions cannot import schema-18 backups.
- Slide to adjust starts at the current value without an initial jump. Horizontal movement after activation does not affect the value. Release ends adjustment without running the action again.
- A continuous swipe stage disables Swipe and hold for that handle. Its saved assignment stays intact. Disable Slide to adjust on all swipe stages to use it again.
- Brightness stays open while held. Native media volume receives an unchanged-value popup request once per second while held. This stops when input ends. There is no idle keepalive.
- Cancellation, multitouch, rotation, unavailable handles, and service cleanup stop adjustment and discard pending writes.
- Existing global visibility hiding migrates to each handle. Pauses remain temporary; visibility remains saved.
- Brightness uses the Android perceptual curve by default. HyperOS can use a different mapping. Use calibration when the native slider differs.
- Install over 0.4.2 without uninstalling. Check retained settings, accessibility, brightness access, backups, and Home shortcuts.
- Physical Redmi and HyperOS touch capture, slider mapping, visible flicker, update installation, and battery comparisons remain pending.

## Maintenance

- Correct handle guide punctuation and check hold deadline, retreat, and recrossing boundaries. (2923f42, 9ea839e)

## Automated preparation checks

- All 533 unit tests passed with no failures, errors, or skipped tests.
- Debug lint passed with 0 errors and 25 warnings. The debug build passed with version 0.4.3-debug, code 12.
- Release tools passed 29 changelog integration checks and 10 isolated finalization checks.
- The final signed release build, signer comparison, and installation over 0.4.2 run after the GitHub merge. They are not completed by preparation checks.

## Release baseline

- Previous stable release: [v0.4.2](https://github.com/serogee/Nudge/releases/tag/v0.4.2), code 11. GitHub publication was confirmed on 2026-10-22.
- This release uses version 0.4.3, code 12. Build the final signed APK and create its tag from clean merged main.
