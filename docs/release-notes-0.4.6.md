# Nudge 0.4.6

## Added

- Tap a settings slider's percentage or dp readout to enter a value. Save or keyboard Done applies the value within that slider's minimum and maximum. Cancel keeps the current value. Disabled controls remain disabled. (754f4e5)

## Fixed

- Wait up to 30 seconds for a supported native brightness control before recording or changing brightness. On Xiaomi, open Control center manually during calibration. Search System UI application windows and the active System UI root while rejecting other apps and duplicate roots. (c55e744)
- Show Xiaomi guidance when the Control panel action opens notifications. Keep gesture injection and screenshot capabilities disabled. (c55e744)
- Use a 75%-opaque background for action-picker settings badges. Keep the gear icon fully opaque and retain the action's touch target. (c78fbf5)

## Maintenance

- License Nudge under **GNU GPL version 3 or later** (`GPL-3.0-or-later`). Add the full license and project grant to APK assets. Document matching-source distribution and third-party notices. (3f100d4)
- Rebuild the README with the app logo, stack badges, contents, demo placeholders, features, privacy, development, and support. Condense the user guide and replace repeated release checklists with one development and release guide. Remove the old panel investigation write-up. (e6deefc)
- Keep `.plans/` local and untracked. Move the approved commit scopes to AGENTS.md so fresh clones retain the rules. Use the same directory pattern as the other ignore rules. (9f2b20a, dff8f1c)

## Compatibility and known limits

- Backup schema remains 20. Install over 0.4.5 without uninstalling. Check retained bindings, brightness calibration, permissions, Home shortcuts, and backup import.
- Automatic calibration still needs the separate **Nudge brightness calibration** accessibility service and **Modify system settings** access. Enable the service, return to Home, and select **Start calibration**.
- On Xiaomi, open Control center manually after Start. With the new style, swipe from the top right. With the old style, expand quick settings until the brightness slider is visible. Other phones request Android's quick settings panel.
- Discovery leaves brightness and automatic mode unchanged. The 20-second measurement deadline starts only after a supported control is found. Cancelling while waiting leaves both values unchanged.
- The Control panel action still depends on Android's native request. On Redmi Note 12 / HyperOS 1.0, it was reported to open notifications. The action shows guidance; it does not inject a top-right swipe.
- Unsupported native controls still require **Manual calibration**. The discovery fix has not yet been rechecked on the reported Redmi with either Control center style.
- Numeric entry accepts decimal values, including signed dp offsets. Empty, invalid, and non-finite values cannot be saved. Out-of-range values use the nearest allowed limit.
- Physical HyperOS control exposure, real brightness mapping and restoration, native popup touch ownership, update installation, and battery comparisons remain pending.

## Automated preparation checks

- All 636 unit tests passed across 104 classes, with no failures, errors, or skipped tests.
- Debug lint passed with 0 errors and 25 warnings. The debug build passed with version 0.4.6-debug, code 15.
- Release tools passed 29 changelog integration checks and 10 isolated finalization checks. All 28 local documentation links passed. Published notes and earlier changelog entries remain unchanged.
- The debug APK contains the GPL-3.0-or-later project notice and the full license text matching LICENSE.
- Final signing, signer comparison, and installation over 0.4.5 happen after the GitHub merge.

## Release baseline

- Previous stable release: [v0.4.5](https://github.com/serogee/Nudge/releases/tag/v0.4.5), code 14. GitHub publication at 2026-10-23T03:34:56Z was confirmed on 2026-10-23.
- This release uses version 0.4.6, code 15. Notes use `-FromTag v0.4.5`.
- Build the final signed APK and create `v0.4.6` from clean merged main. Use the [development and release guide](development.md#build-and-tag-after-the-github-merge).
