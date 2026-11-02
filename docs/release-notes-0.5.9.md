# Nudge 0.5.9

## Added

- Add **Adaptive**, **Transparent**, and **Choose color** background modes to panel buttons and Home shortcuts. Save each button's mode and color separately. Retain the custom color when switching modes. (80d7c10)
- Use the existing default button color for Adaptive. Panel buttons follow their panel's Automatic button color or saved default color. Home shortcuts use Nudge's neutral default. Adaptive does not select Android's adaptive icon format. (80d7c10)
- Add **Edit shortcut** for saved Home definitions. Update the existing pinned ID, label, action, and artwork. Keep other shortcuts unchanged. Editing an unpinned definition does not pin a new icon. Reject stale edits after a shortcut changes or is deleted. (80d7c10)

## Changed

- Show two explicit styles: **With Container** retains padding; **Standalone** uses larger artwork in panels and Home shortcuts. Keep Wheel icon size and position controls separate. Resolve older Panel default choices from their saved panel setting. (80d7c10)
- Render the same exported bitmap in the Home editor, naming preview, and shortcut list. Keep original app and icon-pack artwork colors. Normal background choices remain separate from active colors and Wheel hover colors. (80d7c10)
- Save **schema 33** backups with per-button background modes and colors. Keep older backups importable and retain their existing appearance. (80d7c10)

## Fixed

- Export Home shortcut artwork as ordinary bitmaps. Preserve transparent pixels through the final Android drawable instead of placing the artwork in an adaptive wrapper that fills empty areas with black. Remove the extra doubled canvas padding. (7a57dca)
- Keep saved shortcut settings after a launcher refresh failure. Use **Pin again** to retry the same ID. A failed or rate-limited refresh does not replace the existing pinned artwork with incomplete data. (80d7c10)

## Compatibility and known limits

- Backups use **schema 33**. Builds supporting only **schema 32 or earlier cannot import them**. Older supported backups remain importable. Preserve handles, panels, Wheels, gestures, pauses, visibility groups, actions, icon references, orientation settings, and overflow buttons.
- Older Standalone buttons retain transparent normal backgrounds. Older With Container buttons retain their default background colors. Resolve inherited styles against the owning panel; old Home default artwork remains without an added background. Preserve empty layout cells.
- Transparent removes Nudge's normal button background. It does not erase an opaque background in original app artwork. That artwork can cover the chosen background color. Active colors and hover effects keep their separate settings.
- A Home launcher can add its own background, shape, scaling, or app badge. Actual transparency on stock **MIUI/HyperOS 1.0** remains unverified. Automated drawable tests do not establish the launcher's final appearance.
- Physical-device checks remain pending for the signed upgrade over **0.5.8**, retained settings, both styles and all background modes, source app colors, panel and Wheel active/hover colors, Home sizing and corners, edited shortcut refreshes, and Pin again retries. No Android device was connected during feature verification.

## Automated preparation checks

- Verified **107 unique regression cases across 12 classes**, with **0 failures, errors, or skipped cases**, across the broad run and focused shortcut rerun for source commit `80d7c10`. Coverage includes background modes, saved colors, independent shortcuts, legacy imports, backup round trips, Save and Cancel, stale edits, original artwork colors, same-ID updates, and failed-refresh retries. This is focused coverage, not a complete release test run.
- Native graphics checks render the final `Icon.loadDrawable()` result on **API 31 and 36**. They verify transparency and artwork size beyond the source bitmap. Interface checks use **API 36**. These are simulated Android checks, not physical-device results.
- Passed **lintDebug** with **0 errors and 29 warnings** and **assembleDebug** for the completed feature source at **0.5.8-debug / code 24**. Release preparation changes only version and release records afterward.
- Passed **29 changelog integration checks** and **22 isolated finalization checks**. The fixtures do not build, tag, push, or publish the actual release.
- Passed prepared-version **assembleDebug**. APK badging and output metadata confirm **0.5.9-debug / code 25**, package `com.modukey.app.debug`, minimum SDK **31**, and target and compile SDK **36**.
- Applied available VS Code save settings with installed **Prettier 3.7.4**, four-space indentation, and single quotes where supported. A repeated pass makes no further changes. Preserve the properties-file style because no applicable formatter is installed. No additional save actions or project formatter configuration are configured.
- Final signed assembly, the complete unit suite, full lint, and device upgrade checks follow the GitHub merge. Do not build the final signed APK, tag, push, merge, or publish during preparation.

## Release baseline

- Previous published stable release: [v0.5.8](https://github.com/serogee/Nudge/releases/tag/v0.5.8), **code 24**, published at **2026-10-30T10:51:09Z**. GitHub releases/latest confirmed publication on **2026-11-03**. This is a published release, not only a local tag.
- The remote annotated tag object `a2d61463ec2541d80788a5b0ef4bfe772fc40937` and matching published APK metadata identify commit `c3077eb86c3e80b8435514236c867d9ee9f3c729`. The tag matches the local tag and is reachable from this feature branch.
- The published APK is **3,814,930 bytes**. Its matching local metadata records package `com.modukey.app`, version **0.5.8 / code 24**, minimum SDK **31**, and target and compile SDK **36**. Android `apksigner` verifies the local APK and certificate SHA-256 `abbf8d31940893e0dacf8280be866b89106ec09dbd89417503a7853ce515eabc`. Retain this signing identity.
- The local APK SHA-256 matches GitHub's published digest: `cd8c735c7c4b867950e50d428652a31c418e6d6d281b7ff1b125deee17efaf12`. The checksum file digest matches `50369d7f8d07be77b56ea5b7d5af9e91d1970b692e665d0f0e0d0c68c262de5d`. The build metadata digest matches `6f9dcb42b54a96514bde58860c6d2dc7ecca6d3c2a2c2628010b141b8ef5d302`.
- GitHub provides the APK, checksum, and build metadata as three assets. Notes are in the release body. Preserve the published 0.5.8 changelog, notes, tag, local artifacts, and all archived builds.
- Prepare **0.5.9, code 25**, from **v0.5.8**. Generate with `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Generate-Changelog.ps1 -FromTag v0.5.8 -Write`, then review the notes. Do not regenerate them after finalization.
- After the GitHub merge, the finalizer updates clean `main`, validates the release, builds and verifies the signed APK, checks the prior signer, exports four files under `releases/0.5.9/`, and creates **v0.5.9**. Publish the exported notes as the release description and attach only the APK, checksum, and build metadata. Confirm publication before using 0.5.9 as a published baseline.
