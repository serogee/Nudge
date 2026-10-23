# Nudge 0.5.1

## Added

- Use a hollow semicircle with separated wedge buttons for Wheels. Set the radius from **40–240 dp**. Keep up to six buttons, with smaller capacities when space or touch targets require them. Retain overflow buttons for editing and reordering. (3d3fc35)
- Expand the highlighted wedge, icon, and angular hit region. Shrink other wedges and icons. Keep the current selection until the finger crosses its expanded boundary, then transfer expansion to the new selection. (3d3fc35)
- Toggle smooth Wheel focus animations. Turning **Wheel animations** off applies shape changes immediately and suppresses Wheel window motion. Respect Android's disabled-animation setting. Keep logical selection independent of animation. (3d3fc35)
- Enable optional **Slider touch completion** for supported Xiaomi brightness controls. Require explicit consent before briefly pressing and releasing the identified native relative slider to save each measured position. Keep this device-local option off by default. (bbf3a3d)

## Fixed

- Keep native slider identity stable while its panel bounds move. Wait for delayed progress updates. Recheck the current control, bounds, and covering windows before touch completion. Reject invalid or covered targets. (bbf3a3d)
- Wait for an active native touch to end before cancellation restores brightness. Refresh an unchanged saved value when needed to clear a temporary native preview during restoration and recovery. (bbf3a3d)
- Diagnose unreadable, decreasing, and flat calibration samples without recording measurement values or screen text. Keep the saved profile when a run fails. (bbf3a3d)

## Maintenance

- Allow more time for slider test configuration to load from disk. Keep gesture timing assertions unchanged. (d05e0cf)
- Update Help, the README, and the user guide for compact Wheels, selection boundaries, animation controls, native slider consent, and the observed Redmi failure. (3d3fc35, e883a21, 926daae)

## Compatibility and known limits

- Backups now use **schema 22** for compact Wheel sizes and animation choices. Older backups remain importable. Schema-21 Wheels keep their saved radius and enable animation by default. Nudge 0.5.0 and earlier cannot import schema-22 backups.
- Preserve 48 dp normal touch targets and the six-button maximum. Radius, button size, selected size, and safe screen space determine capacity. Add is unavailable at capacity; saved overflow remains editable.
- Normal Wheel taps and optional Drag to pick remain available. Retain the original hold deadlines, quick-release timing, later-swipe restrictions, saved assignments, slider exclusion, and release-to-execute rules. Animation cannot select another action under a stationary finger.
- Selection distances start at the visible Wheel center. The hollow center stays neutral. Normal taps ignore separator gaps. Release cannot create the first highlight, and the release position must still validate the highlighted action.
- The normal handle service still cannot retrieve window content or inject touch gestures. Only the separate calibration service can perform the optional native slider touches. Neither service gains screenshot capability.
- Slider touch completion applies only during an explicit automatic run on a supported native relative slider. If requested, disable and re-enable **Nudge brightness calibration** in Accessibility to grant its updated capability. Keep hands off the screen during measurement.
- Keep 17 automatic positions and five manual points. Profiles, recovery records, and both calibration service preferences remain device-local and outside configuration exports and Android backup. Existing profiles remain valid.
- On Redmi Note 12 / Android 14 / HyperOS 1.0, the new Control center reached 100% while the saved brightness stayed fixed and temporary brightness reached maximum. Inspection of the installed native plugin confirmed that saving requires a release callback. The new touch-completion and restoration path still needs a physical-device recheck.
- The separate above-zero abort was not reproduced with a confirmed error. Moving panel bounds no longer change control identity, but this does not establish the cause of that report.
- Physical Wheel touch ownership, smoothness, native popup interaction, signed installation over 0.5.0, final brightness mapping and restoration, and battery comparisons remain pending. Automated passes do not establish these results.

## Automated preparation checks

- The completed feature implementation passed **770 unit tests across 113 classes**, with no failures, errors, or skipped cases. Coverage includes expanded-boundary transfers, stationary-finger stability, compact capacities, overflow, draft controls, backup imports, delayed native progress, blocked touches, cancellation, and restoration.
- Version preparation then passed debug lint with **0 errors and 25 warnings** and built **0.5.1-debug, code 17**. No feature implementation changed after the completed unit run.
- Release tools passed **29 changelog integration checks** and **10 isolated finalization checks**. All **29 local documentation links** passed. Earlier published changelog entries and versioned notes remain unchanged.
- The finalizer repeats unit tests, full lint, and signed release assembly after the GitHub merge. Final signing, signer comparison, and installation checks remain pending.
- No final signed release APK or v0.5.1 tag is created on this feature branch.

## Release baseline

- Previous stable release: [v0.5.0](https://github.com/serogee/Nudge/releases/tag/v0.5.0), code 16. GitHub publication at **2026-10-23T16:39:14Z** was confirmed on 2026-10-24. The published tag resolves to the local release commit, `f00c5a7291544df23308924a1ee80c7b1690f579`.
- This release uses **version 0.5.1, code 17**. Notes use `-FromTag v0.5.0`.
- Build the final signed APK and create v0.5.1 from clean merged main. Use the [development and release guide](development.md#build-and-tag-after-the-github-merge). Publish after the signed update and changed features pass the required device checks.
