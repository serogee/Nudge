# Nudge 0.5.8

## Fixed

- Keep **Toggle and hide handles** active when all handles in its pause group remain paused after a separate visibility change. Showing the handles no longer clears this highlight. The handles already kept their saved pause; this corrects the displayed state. (9495e3f)
- With no pause group, use the visibility group for the active state. Keep mixed and empty groups inactive for the state that controls the button. Preserve **All handles**, **This handle**, and **Other handles** targets. (9495e3f)
- Keep the existing coordinated action: pressing the combined button pauses and hides, or resumes and shows, its selected groups. Separate visibility actions preserve saved pauses. Continue to report both group states in the accessibility description. Panels and Wheels share this presentation. (9495e3f)

## Maintenance

- Explain which group controls the combined button's active state in the user guide. (f271c6f)

## Compatibility and known limits

- Keep **schema 32** and existing handles, panels, Wheels, gestures, pauses, visibility selections, and appearance settings. No configuration field or backup format changes. Nudge **0.5.7 and 0.5.8** support schema-32 backups; **0.5.6 and earlier cannot import them**. Older supported backups remain importable.
- The combined button follows pauses whenever a pause group is configured. With no pause group, it follows visibility. The separate slashed-eye action stays active when all its targeted handles are hidden.
- Physical-device checks remain pending for the signed upgrade over **0.5.7**, retained settings, panel and Wheel colors after separate visibility changes, and real gesture input. Automated checks do not establish these results.

## Automated preparation checks

- Passed **29 focused tests across four classes**, with **0 failures, errors, or skipped cases**, for the completed fix source before the version change. Coverage includes scoped active states, visibility-only and pause-only configurations, mixed and empty groups, actual visibility actions preserving saved pauses, and coordinated resume/show behavior.
- Runtime regressions cover **API 31 and 36** through Robolectric. Interface checks use **API 36**. These are simulated Android results, not physical-device results or a complete release test run.
- Passed **lintDebug** with **0 errors and 27 warnings** and **assembleDebug** for the completed fix source at **0.5.7-debug / code 23**. Only the version and release preparation files change afterward.
- Passed a new **assembleDebug** after the version change. APK badging and output metadata confirm **0.5.8-debug / code 24**, package `com.modukey.app.debug`, minimum SDK **31**, and target and compile SDK **36**.
- Passed **29 changelog integration checks** and **22 isolated finalization checks** during this preparation. The fixtures do not build, tag, push, or publish the actual release.
- Applied the available VS Code save settings with the installed **Prettier 3.7.4**, four-space indentation, and single quotes where the format permits them. A repeated pass makes no further changes. Preserve the existing properties-file style because no applicable formatter is installed. No additional save actions or project formatter configuration are configured.
- Final signed assembly, the complete unit suite, full lint, and device upgrade checks follow the GitHub merge. No 0.5.8 signed release APK, tag, push, merge, or publication is performed during preparation.

## Release baseline

- Previous published stable release: [v0.5.7](https://github.com/serogee/Nudge/releases/tag/v0.5.7), **code 23**, published at **2026-10-29T06:22:55Z**. GitHub releases/latest reconfirmed publication on **2026-10-30**. This is a published release, not only a local tag.
- The remote annotated tag object `5c37ba81bac38e6bdaaf9794a5ef14bd6c68856f` and published APK metadata identify commit `ac908b8d608823fa5348ae6eef55bc6f1c383a05`. The tag matches the local tag and is reachable from this preparation branch.
- The published APK is **3,814,930 bytes**. Its matching local metadata records package `com.modukey.app`, version **0.5.7 / code 23**, minimum SDK **31**, and target and compile SDK **36**. Android `apksigner` verifies the local APK and certificate SHA-256 `abbf8d31940893e0dacf8280be866b89106ec09dbd89417503a7853ce515eabc`. Retain this signing identity.
- The local APK SHA-256 matches GitHub's published digest: `044ae5c770fd639cca62a88b4ce32c02540bcfae419ec23b94b2972dc0c61c49`. The checksum file digest matches `d51868e4fa8b4d853595b156ecba388ebaaa468689449227054f738ee4180cfc`. The build metadata digest matches `4f9089fc461833e929fe89fc8f102d014e82baa62d91d75acc023f79fc047438`.
- GitHub has the APK, checksum, and build metadata as three assets. Notes are in the release body. Preserve the published 0.5.7 changelog, notes, tag, and local artifacts, including its notes export. Preserve all archived builds.
- Prepare **0.5.8, code 24**, from **v0.5.7**. Generate with `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Generate-Changelog.ps1 -FromTag v0.5.7 -Write`, then review the notes. Do not regenerate them after finalization.
- After the GitHub merge, the finalizer updates clean `main`, validates the release, builds the signed APK, checks the previous signer, exports four files under `releases/0.5.8/`, and creates **v0.5.8**. Publish the exported notes as the release description and attach only the APK, checksum, and build metadata. Confirm publication before marking 0.5.8 as published.
