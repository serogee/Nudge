# Nudge 0.5.5

## Added

- Add **Item gap** from **0–24 dp**, with a **4 dp** default. Keep facing Wheel edges parallel along the band, including highlight transitions. Save the gap separately for portrait and landscape. (e46bfad)
- Show the edited **Manual** hitbox above the Wheel for **two seconds** after dragging, numeric entry, or reset. Refresh the timer after each adjustment. Show the same reference in the editor and live accessibility preview. The reference does not receive input. Clear it on expiry, Cancel, editor exit, orientation change, or a hitbox mode change. (53a2c61)

## Changed

- Make **Autofit** choose the smallest usable overall outer radius for the included buttons and their **48 dp** touch targets. Keep the visual inner radius at least **32 dp**. Allow a larger hole when it reduces the outside size. Prefer **110%** highlighting and reduce toward 100% only when screen space requires it. With six buttons, a 4 dp gap, and enough space, fit **88 dp** inside and **136 dp** outside. New unfitted Wheels use **32/88 dp**. Manual inner radii can still reach **1 dp**. Preserve explicit older sizes. (9771504)
- Default and reset **Icon position** to **55%**. Apply 55% when Autofit recalculates. Keep complete icons within the rounded wedge. Preserve explicit imported positions until edited or fitted again. (9771504)
- With Autofit on, start **Automatic** hitboxes at **32 dp** and follow the current logical outer edge, including highlighting. The larger fitted visual hole does not push selection outward. With Autofit off, retain saved gesture distances and the existing inward margin. (9771504)
- Copy the previous hitbox mode's effective, unhighlighted bounds when entering **Manual**. Use the selected preview handle's gesture distances where applicable. Keep manual bounds fixed during highlight growth. Preserve visual sizes, Autofit, and saved gesture settings. (53a2c61)

## Fixed

- Keep outer corner rounding independent from the shorter inner arc. Small holes no longer reduce both corner limits together. Preserve concentric arcs and the empty gaps used by normal taps. (e46bfad)
- Rebuild release checks from clean sources without the build cache or incremental Kotlin compilation. Keep the complete property argument quoted in PowerShell. Disable the generated problems report to avoid the observed Windows replacement failure. Unit tests, their reports, full lint, and signed assembly remain required. Failed checks stop artifact export and tag creation. (50b4d74)

## Maintenance

- Update the user guide and README for gaps, compact Autofit, independent corners, 55% icon positions, and copied Manual hitboxes. Document release checks and recovery. (07e30ae, 50b4d74)

## Compatibility and known limits

- Backups use **schema 31**. Save gaps per orientation and retain hitbox modes, manual bounds, icon preferences, radii, Autofit, actions, shared references, and overflow buttons. Older backups remain importable and use a 4 dp gap when absent. Preserve their explicit sizes and settings. **Nudge 0.5.4 and earlier cannot import schema-31 backups.**
- Wheels retain up to **six buttons**. Screen space, gap size, and touch geometry can reduce visible capacity. Retain overflow buttons. Normal taps exclude the hollow center and shared gaps. Hitbox modes apply to **Drag to pick**; angular selection follows the logical highlighted regions.
- Manual hitbox bounds remain **0–600 dp**, with at least **8 dp** between them. Switching to Manual copies the current source bounds; later adjustments remain independent. The exact wheel center has no direction and does not select a button.
- Physical-device checks remain pending for uniform gaps, tiny-hole corner rendering, compact layouts, icon readability, hitbox reach, repeated slider edits, preview cleanup, all edges, orientation changes, and retained settings after a signed upgrade over **0.5.4**. Automated results do not establish these checks.

## Automated preparation checks

- Passed a **clean full run of 872 unit tests across 135 classes**, with no failures, errors, or skipped cases, after updating to version 0.5.5 and code 21. Coverage includes constant gaps, independent corner limits, compact fitting, safe icon positions, automatic selection reach, copied Manual bounds, two-second references, cleanup, orientation storage, older imports, and retained overflow buttons.
- Ran `clean :app:testDebugUnitTest :app:lint :app:assembleDebug` with `--no-build-cache`, `--no-problems-report`, and the quoted `-Pkotlin.incremental=false` argument. All **57 tasks** executed successfully. Lint reported **0 errors and 27 warnings**. Debug metadata confirms **0.5.5-debug, code 21**, application ID `com.modukey.app.debug`, minimum SDK 31, and target SDK 36.
- Passed **29 changelog integration checks** and **22 isolated finalization checks**. The latter exercise merged-source selection, clean argument handling, and build-failure protection using disposable fixtures. They do not build or publish a real release.
- Compared the prior APK with the published digest and verified its signature with Android `apksigner`. Confirmed the published metadata, local metadata, and v0.5.4 tag agree. Confirmed all former Unreleased entries are covered here and published changelog history remains unchanged.
- Final signed assembly, new APK signer comparison, and device checks remain pending. The finalizer repeats unit tests, lint, and signed release assembly after the GitHub merge. Create the 0.5.5 tag only from clean merged `main`, then publish after verification.

## Release baseline

- Previous stable release: [v0.5.4](https://github.com/serogee/Nudge/releases/tag/v0.5.4), **code 20**, published at **2026-10-25T05:31:02Z**. Rechecked GitHub releases/latest and the published build metadata on **2026-10-25**. They agree with the release tag and local metadata at commit `727224dd415ea029bc857f9772605c7d4f93fa9f`.
- The local 0.5.4 APK SHA-256 matches the published asset digest: `6a324e3eb031b87960e9653e0419400205a7700ed428c2a6ce25234126bfa0c3`. The published and local metadata agree on signing certificate SHA-256 `abbf8d31940893e0dacf8280be866b89106ec09dbd89417503a7853ce515eabc`. Retain this signing identity for the update.
- Prepare **version 0.5.5, code 21** from **v0.5.4**. Generate the notes with `-FromTag v0.5.4`, then review them. Version 0.5.5 is not yet published. Preserve released notes, changelog history, APKs, and tags.
- Build the final signed APK and create **v0.5.5** from clean merged `main`. Follow the [development and release guide](development.md#build-and-tag-after-the-github-merge). Verify the signed upgrade and changed features on a device, then publish the GitHub release using the existing tag and all four exported files.
