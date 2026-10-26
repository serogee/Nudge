# Nudge 0.5.6

## Changed

- Default and reset Wheel **Selected size** to **125%**. Keep the existing **100–150%** range and preserve saved custom percentages. (0ff5627)
- Keep **Autofit** on when Selected size changes. Use that percentage to calculate the smallest usable outer radius. Keep the inner radius at least **32 dp** and retain **48 dp** touch targets. Refit the edited orientation. Adding or removing buttons and changing Item gap use the same selected percentage. (0ff5627)
- With Autofit off, changing Selected size keeps the manual radii. If the chosen percentage cannot fit another button, retain it and make Add button unavailable. Overflow buttons remain saved. (0ff5627)

## Fixed

- Keep Home shortcut artwork transparent by default. **Standalone** also has no added background. Add a rounded background only for **With container**. Use the same container choice in the editor, naming preview, and shortcut list. (08c53c7)
- Export transparent adaptive icons with safe padding. Keep artwork inside the launcher's corner mask and avoid legacy background wrappers. Smooth imported icon corners and retain complete vector strokes. (08c53c7)
- Update an existing pinned shortcut's artwork when **Pin again** is used. Keep its saved ID and action. (08c53c7)

## Compatibility and known limits

- Backups remain at **schema 31**. No preference or backup format changes are required. Preserve explicit percentages, manual radii, Autofit choices, orientation settings, icon references, actions, and overflow buttons. Nudge 0.5.5 can read this schema; 0.5.4 and earlier cannot.
- Existing percentages, including **110%**, remain saved. Use Reset Selected size to apply the new **125%** default to an existing Wheel.
- Use **Pin again** to refresh existing Home icons. Imported artwork can contain its own background. Launchers can add an app badge near an icon's edge.
- Physical-device checks remain pending for the signed upgrade over **0.5.5**, retained settings, Autofit edits in both orientations, icon corners, and launcher refresh behavior. Automated checks do not establish these results.

## Automated preparation checks

- Verified **170 relevant tests across 29 classes**, with no remaining failures, errors, or skipped cases, across the broad regression run and focused reruns for feature commit `08c53c7`. This is focused coverage, not a complete release test run. Checks include fitting priorities, selected percentages, reset, save and Cancel, manual distance limits, backups, orientation settings, icon transparency, adaptive safe bounds, and pinned updates.
- Passed **lintDebug** with **0 errors and 27 warnings** and a debug build for the feature source before the version increase. Native icon checks cover **API 31 and 36** through Robolectric; they are not physical-device checks.
- Passed the prepared-version **assembleDebug** build. Debug metadata confirms **0.5.6-debug, code 22**, application ID `com.modukey.app.debug`, minimum SDK 31, and target SDK 36.
- Passed **29 changelog integration checks** and **22 isolated finalization checks**. The finalization fixtures use disposable local Git repositories; they do not build, tag, or publish a real release.
- Final signed assembly and device upgrade checks follow the GitHub merge. The finalizer repeats the complete unit suite, full lint, and signed release assembly from clean merged `main` before exporting files or creating the tag.

## Release baseline

- Previous stable release: [v0.5.5](https://github.com/serogee/Nudge/releases/tag/v0.5.5), **code 21**, published at **2026-10-25T08:34:05Z**. GitHub releases/latest confirmed publication on **2026-10-26**. The release tag and published build metadata identify commit `70b5ac7344da49200a9277ea8e782bbbf8f7623a`.
- The local archived APK SHA-256 matches GitHub's published asset digest: `b98cb95a67caae013ea7d8d0d363a073d86755523cf7d99a99656255dc29217b`. The local build metadata SHA-256 also matches its published asset digest: `b8456f53c6f3814356d49d9e784a8c50df84a3606be7343fe10194d46c440ae3`.
- Verified the archived APK with Android `apksigner`. Its signing certificate SHA-256 is `abbf8d31940893e0dacf8280be866b89106ec09dbd89417503a7853ce515eabc`. Retain this signing identity. The finalizer compares the new APK against the archived 0.5.5 APK.
- Prepare **version 0.5.6, code 22** from **v0.5.5**. Generated these notes with `-FromTag v0.5.5`, then reviewed them. Publication follows the signed build and upgrade checks. Preserve earlier release notes, changelog entries, APKs, and tags.
- After the GitHub merge, run `scripts/Finalize-Release.ps1` from a clean workspace. It builds and verifies the APK, exports four files to `releases/0.5.6/`, and creates **v0.5.6** from merged `main`. Verify the signed upgrade and changed features, then push the existing tag and create the GitHub release. Follow the [release guide](development.md#build-and-tag-after-the-github-merge).
