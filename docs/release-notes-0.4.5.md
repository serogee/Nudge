# Nudge 0.4.5

## Added

- Add **Automatic calibration** beside **Manual calibration** in Home. Enable the separate **Nudge brightness calibration** accessibility service, return to Home, and select **Start calibration**. Enabling the service alone does not move the slider. (68a1a8e)
- Measure the native brightness slider at 0%, 25%, 50%, 75%, and 100%. Restore the original brightness and automatic mode before showing results. Keep the saved calibration until you select **Save**. (68a1a8e)
- Show progress and **Cancel** during measurement. Keep a device-local recovery record if restoration is interrupted. Choose **Restore previous brightness** or **Keep current brightness** when recovery is needed. (68a1a8e)

## Fixed

- Accept zero as a native brightness calibration minimum. Continue to reject negative, repeated, or decreasing samples. Manual calibration and saved profiles retain a valid zero minimum. (bf3a55d)
- Keep completed measurements available for Save if the disabled calibration service sends a late interruption. Refresh an open brightness preview and active button states after saving. (94b6c93)
- Recognize known brightness control IDs from System UI plugin resources. Check System UI ownership and reject unrelated color or dimming sliders. Accept an already-selected position without requiring another native slider action. (87edbe3)
- Place **Slide to adjust** directly below its selected volume or brightness option. Keep action and panel layout options below their parent controls. Replace nesting arrows with subtle left lines. Shared slider travel settings keep their own section. (f93a1e8)

## Compatibility and known limits

- The temporary calibration service requests window-content access separately from **Nudge handles**. The handle service still cannot retrieve window content. Nudge limits calibration control lookup to System UI. It does not inspect text, take screenshots, record the screen, or inject coordinate taps.
- Enable the calibration service for each automatic run. It disables itself after completion, cancellation, failure, or a two-minute setup timeout. Measurement is limited to 20 seconds, with shorter discovery and settling deadlines. There is no idle sampling or automatic retry.
- Cancellation, screen-off, lock, rotation, or service loss stops measurement and attempts restoration. After process death, Nudge offers recovery instead of resuming measurement. Other Nudge brightness writes remain blocked until recovery is resolved.
- A supported native control must expose a valid range and accept accessibility progress actions. Missing, ambiguous, refused, changing, unstable, or non-monotonic controls leave the saved profile unchanged. Use **Manual calibration** when automatic calibration is unsupported.
- Five measured positions define an interpolated curve. This does not establish an exact mapping at every intermediate position. Profiles remain device-local, are excluded from configuration backups, and require calibration again after a device fingerprint change.
- Backup schema remains 20. Install over 0.4.4 without uninstalling. Check saved bindings, calibration, permissions, Home shortcuts, and backup import.
- Physical Redmi and HyperOS control exposure, real brightness mapping, restoration, overlay placement, update installation, and battery comparisons remain pending.

## Automated preparation checks

- All 607 unit tests passed with no failures, errors, or skipped tests.
- Debug lint passed with 0 errors and 25 warnings. The debug build passed with version 0.4.5-debug, code 14.
- Release tools passed 29 changelog integration checks and 10 isolated finalization checks.
- The final signed release build, signer comparison, and installation over 0.4.4 run after the GitHub merge.

## Release baseline

- Previous stable release: [v0.4.4](https://github.com/serogee/Nudge/releases/tag/v0.4.4), code 13. GitHub publication at 2026-10-22T17:51:33Z was confirmed on 2026-10-23.
- This release uses version 0.4.5, code 14. Build the final signed APK and create its tag from clean merged main.
