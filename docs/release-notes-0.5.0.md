# Nudge 0.5.0

## Added

- Create reusable semicircular wheels in the new **Wheels** tab. Show up to six buttons facing inward from a left, right, or bottom handle. Smaller layouts can show fewer buttons. Keep overflow buttons saved, editable, and reorderable. (0a95ca5, 59069b4)
- Tap Wheel buttons normally or enable **Drag to pick** for a non-tap gesture. Configure selection distances, selection expansion, and selected size. Release a highlighted button to run its action once and close the Wheel. Use **Allow later swipes** and its quick-release window to continue a swipe sequence without picking. (0a95ca5, 59069b4)
- Measure 17 automatic brightness positions at 6.25% intervals. Keep manual calibration at five points and retain existing profiles. Dense curves accept repeated hardware levels and wait for stable readings. (0cbb3a7)
- Enable **Keep calibration service enabled** to reuse an idle connection. Select **Disable calibration service** to turn it off manually. Automatic disabling remains the default. The idle service clears event subscriptions and window-query flags. (e6baef8)

## Fixed

- Apply the saved background corner radius to the Wheel fan. (76f4339)
- Keep Wheel touch targets at least 48 dp, prevent overlap during expansion, and clamp placement within safe screen bounds. Retain the selected button at a shared boundary until the finger crosses it. Show capacity and disable Add when the Wheel is full. (f91d972, 4ac0026, 88429b3, f6efb4b)
- Include non-interactive brightness containers and known slider wrappers during discovery. Match exact translated native brightness labels when new HyperOS sliders share an ID. Accept Xiaomi plugin controls only from the installed system app. Keep the main handle service permissions unchanged. (5a66d1c)
- Include bounded brightness discovery metadata in **Info → Copy compatibility details**. Exclude screen text, accessibility descriptions, and current brightness values. Reject unrelated controls and ambiguous matches. (5a66d1c)

## Maintenance

- Share original handle touch capture between sliders and Wheels. Cancel the original recognizer and its deadlines after capture. (88d31b0)
- Update Help and the user guide for Wheel controls, swipe timing, calibration density, service reuse, and observed Redmi behavior. (dc5ec91, 559615b)
- Verify highlighted Wheel cancellation through Back, screen-off, interruption, and service loss. (57f8ef5)

## Compatibility and known limits

- Backups now use schema 21. Older backups remain importable. Nudge 0.4.6 and earlier cannot import schema-21 backups. Wheel copies and imports retain shared panel links, settings, and overflow buttons.
- A drag Wheel disables **Slide to adjust** for its assignment. An ordinary swipe with a drag Wheel suppresses **Swipe and hold**. The first drag Wheel with **Allow later swipes** off disables later stages without deleting their assignments.
- Quick release advances only before the configured deadline. At or after that deadline, fresh movement beyond 8 dp is required before highlighting. Release cannot create the first highlight. Selection distances start at the visible Wheel center.
- Grid orientations use normal panel behavior and retain their saved Wheel drag settings. Hold and Swipe and hold open a drag Wheel at their activation deadlines while keeping their saved release-execution preferences.
- Automatic calibration still needs the separate **Nudge brightness calibration** accessibility service and **Modify system settings** access. On Xiaomi, open Control center manually after Start. Use expanded quick settings with the old style or swipe from the top right with the new style.
- Discovery waits up to 30 seconds without changing brightness or automatic mode. The 60-second measurement deadline starts after a supported control is found. Calibration restores brightness and mode before showing results. Unsupported controls still require **Manual calibration**.
- Profiles, recovery records, and the keep-enabled preference remain device-local and are excluded from configuration exports and Android backup. The normal handle service still cannot retrieve window content. The calibration service reads controls only during an explicit run.
- On Redmi Note 12 / Android 14 / HyperOS 1.0, an earlier old-style five-point debug run completed and was saved. Its immediate brightness and mode restoration comparison passed. The final new-style label fix, 17-point sampling, and enabled standby still need a physical-device recheck.
- Native shell requests opened notifications and the new Control center as distinct panels on that Redmi. This does not establish the app's accessibility action routing. The Control panel action remains vendor-dependent and retains Xiaomi guidance. No top-right swipe is injected.
- Physical Wheel touch ownership, especially with native popups, remains pending. Installation over 0.4.6, final brightness mapping, and battery comparisons also require device checks. Do not treat automated passes as those results.

## Automated preparation checks

- Staged unit verification covered 722 test cases across 110 classes, with no failures, errors, or skipped cases after completed reruns. Focused brightness and final integration checks followed the completed Wheel suite.
- Debug lint passed with 0 errors and 25 warnings. The debug build passed with version 0.5.0-debug, code 16.
- Release tools passed 29 changelog integration checks and 10 isolated finalization checks. All 28 local documentation links passed. Published notes, release artifacts, and earlier changelog entries remain unchanged.
- Final signing, signer comparison, and installation over 0.4.6 happen after the GitHub merge. No final release APK or version tag is created on the feature branch.

## Release baseline

- Previous stable release: [v0.4.6](https://github.com/serogee/Nudge/releases/tag/v0.4.6), code 15. GitHub publication at 2026-10-23T06:34:04Z was confirmed on 2026-10-23.
- This release uses version 0.5.0, code 16. Notes use `-FromTag v0.4.6`.
- Build the final signed APK and create `v0.5.0` from clean merged main. Use the [development and release guide](development.md#build-and-tag-after-the-github-merge). Publish after the signed update and changed features pass the required device checks.
