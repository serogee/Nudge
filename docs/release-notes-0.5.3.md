# Nudge 0.5.3

## Added

- Add **Autofit** beside **Add button** for Wheels. New Wheels enable it by default. Adding or removing buttons fits the active layouts to their screen space. Prefer the smallest inner radius, then **110%** selection size, then the largest outer radius. Selection size can decrease to **100%** when this preserves a smaller inner radius and usable touch targets. (6c26eb6)
- Save Autofit separately for portrait and landscape overrides. Turning it off retains the fitted sizes. Manual radius, selection-size, or expansion changes turn it off for that orientation. Position, color, corner, and animation changes keep it on. Older Wheels retain their saved sizes with Autofit off until enabled. (6c26eb6)

## Changed

- Replace icon shape choices with **Icon corners** for panels and Wheels. Use **0%** for a square, **100%** for a circle, or an intermediate value for rounded corners. The default is **50%**. Older circles remain circular. Other old shapes retain their corner amount and use rounded squares. (45d1515)

## Fixed

- Let **Drag to pick** highlight up to **12 dp** inside the hollow center, limited to **20%** of its radius. Keep configured selection distances independent. Normal taps still use visible wedges and exclude the hollow center and separator gaps. Preserve hold, quick-release, later-swipe, and release-to-execute behavior. (ecd7124)
- Keep icon-corner and Autofit help safe for the shared text formatter. Opening appearance controls or Help does not fail on percentage text. (de2b666, 6c26eb6)

## Compatibility and known limits

- Backups use **schema 27** and retain per-orientation Autofit choices. Older backups remain importable. Builds that support schema 26 or earlier cannot import the new format. Preserve stored radii, transitions, hover and toggle colors, shared links, and overflow buttons.
- Wheels still show up to **six buttons** with at least **48 dp** touch targets. Add button checks whether fitted sizes can make room. Screen space and geometry can reduce capacity. Overflow remains saved, editable, and reorderable.
- Physical-device checks for Autofit, corner clipping, early highlighting, rotation, touch ownership, and a signed update over **0.5.2** remain pending. Automated checks do not establish these results.

## Automated preparation checks

- Passed **813 unit tests across 123 classes**, with no failures, errors, or skipped cases, for feature commit `6c26eb62df7cdb432f89afa7f6a244ed2e894270`. Coverage includes Autofit priorities, fitted touch targets, adding six buttons, manual overrides, saved sizes, legacy imports, corner entry, and early selection.
- The full unit run exhausted local temporary disk space. Retained passing results and repeated the affected classes in **19 isolated batches**. The combined results contain all 813 passing cases. This is an execution limit, not a device compatibility result.
- Debug lint passed with **0 errors and 25 warnings**. Built **0.5.3-debug, code 19**, application ID `com.modukey.app.debug`. Verified the generated APK version metadata.
- Release tools passed **29 changelog integration checks** and **10 isolated release finalization checks**. Finalization tests simulate merging, building, and tagging only inside disposable fixtures.
- All former Unreleased changes are included in the reviewed 0.5.3 section. Preserve published changelog entries, versioned notes, release artifacts, and tags.
- Final signed assembly, signer comparison, and installation checks remain pending. No signed 0.5.3 APK or v0.5.3 tag is created on this feature branch. The finalizer repeats unit tests, full lint, and signed release assembly after the GitHub merge. Free local disk space before that full run.

## Release baseline

- Previous stable release: [v0.5.2](https://github.com/serogee/Nudge/releases/tag/v0.5.2), **code 18**, published at **2026-10-24T12:53:59Z**. GitHub reports a published stable release. Its annotated tag and matching published build metadata identify commit `c2ddfb4cbc966082a050ea1926537517ef623e89`.
- This preparation uses **version 0.5.3, code 19**. Notes were generated with `-FromTag v0.5.2`, then reviewed against the implemented behavior. Version 0.5.3 is not yet published.
- Build the final signed APK and create v0.5.3 from clean merged main. Follow the [development and release guide](development.md#build-and-tag-after-the-github-merge), retain the existing signing identity, and publish after the signed update and changed features pass device checks.
