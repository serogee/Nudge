# Nudge 0.4.4

## Added

- Choose **Any direction** or **Inward only** for ordinary swipes. Any direction measures displacement from the first touch. Moving back reduces that distance. This choice is independent of Swipe and hold. New and existing handles default to Inward only. (131f212)
- Find additional options through setting chevrons and action-picker gear badges. Small arrow indicators connect expanded controls to their parent setting. Gear badges share the picker background and remain inside the action's touch target. (f86fb2f)

## Fixed

- Move brightness calibration to **Home**, below the brightness slider controls. Capture instructions now point to Home. (f51d2b2)
- Prevent the end of an opening swipe from immediately changing volume or brightness. The popup opens first. **After hold** waits for a stationary hold, then permits adjustment without lifting your finger. Set **Adjustment hold time** from 100 to 1,000 ms; the default is 200 ms. Select **Immediate** for instant adjustment. (170e52a)

## Compatibility and known limits

- New backups use schema 20. Older backups remain importable. Earlier Nudge versions cannot import schema-20 backups.
- Existing swipe bindings with **Slide to adjust** now default to **After hold**. Each swipe binding saves its activation choice and hold time. Copies and backups retain both settings.
- Movement beyond an 8 dp radius restarts the adjustment hold, including horizontal or diagonal movement. Small jitter does not restart it. Releasing before activation leaves the slider value unchanged.
- When the hold ends, adjustment starts at the current system value and finger position. The first 8 dp of vertical movement is ignored. Further movement is proportional, and reversals respond immediately. A brief ready cue uses the swipe haptic switch and Android's haptic policy.
- **Hold** and **Swipe and hold** use their existing activation deadlines. They do not add this extra hold. An assigned button action still runs when the popup opens; the guard delays only slide adjustment.
- HyperOS can map brightness differently from the default Android curve. Use manual calibration in Home when percentages differ from the native slider.
- Install over 0.4.3 without uninstalling. Check retained bindings, calibration, accessibility, brightness access, backups, and Home shortcuts.
- Physical Redmi and HyperOS hold timing, native popup touch capture, brightness mapping, update installation, and battery comparisons remain pending.

## Automated preparation checks

- All 555 unit tests passed with no failures, errors, or skipped tests.
- Debug lint passed with 0 errors and 25 warnings. The debug build passed with version 0.4.4-debug, code 13.
- Release tools passed 29 changelog integration checks and 10 isolated finalization checks.
- The final signed release build, signer comparison, and installation over 0.4.3 run after the GitHub merge.

## Release baseline

- Previous stable release: [v0.4.3](https://github.com/serogee/Nudge/releases/tag/v0.4.3), code 12. GitHub publication at 2026-10-22T15:05:57Z was confirmed on 2026-10-23.
- This release uses version 0.4.4, code 13. Build the final signed APK and create its tag from clean merged main.
