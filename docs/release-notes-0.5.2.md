# Nudge 0.5.2

## Added

- Set the inner and outer Wheel radii on one track with two draggable points. Press a point before dragging; touching elsewhere does not move it. Allow a **1 dp** inner radius, a **49 dp** smallest outer radius, and at least **48 dp** of band width. New wheels retain the 100 dp inner and 156 dp outer defaults. Preserve previous wheel sizes when loading older backups. (af6b2c8, 2c7ba2b)
- Choose **No animation**, **Smooth**, or **Spring** for Wheel highlight transitions. Smooth defaults to 140 ms and accepts 80–400 ms. Fade outlines with wedge and icon motion, and retarget transitions when selection changes. Keep selection boundaries independent of animation. (7284a7e)
- Set optional hover colors for Wheel icons, backgrounds, and highlight outlines. Keep current icon and outline colors by default, with a subtle background tint. Reuse focus transitions and restore normal or toggle colors when selection clears. Tint app artwork only when a custom hover icon color is enabled. (73e2339)

## Fixed

- Keep Wheel backgrounds and button containers concentric when rounding corners. Preserve the adjustable 0–48 dp corner setting and apply the corrected shape in previews and live wheels. (09c5722)
- Keep Wheels separate from panels in both orientations. Remove conversion choices, retain Fixed and Scrollable options for panels, and migrate old mixed active layouts to Wheels without deleting buttons or gesture links. Guard inactive legacy modes when enabling landscape overrides. (f4fb593, 5d23739)
- Require thumb dragging for settings value sliders. Ignore track taps and track swipes. Respond immediately when a drag reverses after reaching a limit. Keep keyboard controls, numeric entry, disabled behavior, and right-to-left support. (10e7ea9, fbf9c6b)
- Keep color picker dialogs stable with long titles. Support hexadecimal entry and preset selection. (e1e8e89)

## Changed

- Default the custom brightness popup to **Slide to adjust**, which starts at the current level and ignores stationary taps. Retain **Jump to touch** as a selectable option and preserve saved touch choices. The thumb-only rule applies to settings value sliders, not the brightness popup. (0350c3a)

## Maintenance

- Update the user guide and editor help for radius limits, hover colors, transitions, and the distinction between settings sliders and brightness touch modes.
- Make release tag discovery and isolated test cleanup work on this Linux runner. Keep release checks scoped to their disposable fixtures. (8ee3a2e)

## Compatibility and known limits

- Backups now use **schema 25**. Older backups remain importable. Builds that support schema 24 or earlier cannot import the new format. Preserve stored radius widths, animation switches, toggle colors, shared links, and overflow buttons.
- Wheels still show up to six buttons. Radius, band width, expansion, and screen space determine capacity. Keep at least 48 dp touch targets; saved overflow remains editable.
- Hover means the highlighted button during Drag to pick. Hover appearance does not change an action's toggle state. The editor preview shows a highlighted button even when expansion is off.
- Preserve the original Wheel hold, drag, quick-release, later-swipe, and release-to-execute behavior. A tiny hollow center does not change the separately configured minimum selection distance.
- Physical-device checks for Wheel shape and touch ownership, transition smoothness, native popup interaction, rotation, brightness behavior, and a signed update over 0.5.1 remain pending. Automated checks do not establish these results.

## Automated preparation checks

- Passed **801 unit tests across 120 classes**, with no failures, errors, or skipped cases. Coverage includes 1 dp inner radii, rounded wedge rendering, hover color entry and restoration, artwork tinting, interrupted focus transitions, settings slider gestures, both brightness touch modes, legacy imports, and draft persistence.
- Debug lint passed with **0 errors and 14 warnings**. Built **0.5.2-debug, code 18**, with application ID `com.modukey.app.debug`.
- Release tools passed **29 changelog integration checks** and **10 isolated release finalization checks**. The finalization checks simulate merging only inside disposable fixtures.
- Published changelog entries and existing versioned release notes remain unchanged. All former Unreleased changes are covered by the reviewed 0.5.2 section.
- Final signing, signer comparison, and installation checks remain pending. No signed release APK or v0.5.2 tag is created on this feature branch. The finalizer repeats unit tests, full lint, and signed release assembly after the GitHub merge.

## Release baseline

- Previous stable release: [v0.5.1](https://github.com/serogee/Nudge/releases/tag/v0.5.1), code 17. The latest-release page confirms publication at **2026-10-24T00:00:45Z**. The published build metadata and local tag identify commit `601a711ccc0913cf2de447640def4da343255820`.
- This release uses **version 0.5.2, code 18**. Notes were generated with `-FromTag v0.5.1`, then reviewed against the final behavior.
- Build the final signed APK and create v0.5.2 from clean merged main. Follow the [development and release guide](development.md#build-and-tag-after-the-github-merge), retain the existing signing identity, and publish after the signed update and changed features pass the required device checks.
