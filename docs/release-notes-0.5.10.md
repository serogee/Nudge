# Nudge 0.5.10

## Fixed

- Fill opaque Home shortcut backgrounds to the launcher's icon shape. **Adaptive** and solid **Choose color** backgrounds no longer draw a smaller rounded container inside that shape. Both **With Container** and **Standalone** retain their artwork size. (2de4ae6)
- Export solid backgrounds as filled adaptive icons. Keep **Transparent** artwork as ordinary bitmaps so Android retains its alpha. Preserve original app colors, saved shortcut IDs, same-ID updates, and refresh retries. (2de4ae6)
- Render the exported icon through Android's drawable in the editor, naming preview, and shortcut list. Include Android's mask for solid backgrounds. The launcher can still apply further styling. (2de4ae6)
- Explain launcher background limits beside the Home shortcut background controls. Keep panel guidance separate. (8caeb29)

## Maintenance

- Correct the finalizer's publication instructions. Use the exported notes as the GitHub release description and attach only the APK, SHA-256 checksum, and JSON build metadata. (17887a5)

## Compatibility and known limits

- Retain **schema 33** and existing configuration. Nudge **0.5.9 and 0.5.10** support these backups. Builds supporting only **schema 32 or earlier cannot import them**. Older supported backups remain importable.
- **Transparent** removes Nudge's added background. It does not erase backgrounds in original app or icon-pack artwork. It cannot remove a background added by the launcher. Panel backgrounds and active or hover colors keep their separate settings.
- After installing this version, use **Pin again** to refresh existing Home icons. Keep their saved IDs and actions. A launcher can cache artwork or add its own shape, background, scaling, or badge.
- The user reported a corner rim with **Adaptive** and **Choose color** on stock **MIUI/HyperOS 1.0**. The corrected export has automated coverage; its final appearance on that launcher remains unverified. Do not treat native graphics tests as physical-device results.
- Physical-device checks remain pending for the signed upgrade over **0.5.9**, retained settings, both styles and all background modes, Home sizing and corners, original app colors, edited shortcut refreshes, and Pin again retries.

## Automated preparation checks

- Passed **45 focused cases across three classes**, with **0 failures, errors, or skipped cases**, for the completed feature source at `8caeb29`. Coverage includes **26 native graphics cases on API 31 and 36**, **14 shortcut cases on API 31 and 36**, and **5 editor cases on API 36**. This is focused coverage, not the complete release suite.
- Confirmed the new filled-edge regression fails against the previous bitmap-only export. The corrected export passes checks for filled source edges, the final Android mask without a contrasting rim, preview consistency, retained artwork size, original app colors, and format changes on the same pinned ID.
- Passed feature-source **lintDebug** with **0 errors and 29 warnings** and **assembleDebug** at **0.5.9-debug / code 25**. Release preparation changes the version and release records afterward.
- Passed prepared-version **assembleDebug**. APK badging and output metadata confirm **0.5.10-debug / code 26**, package `com.modukey.app.debug`, minimum SDK **31**, and target and compile SDK **36**.
- Passed **29 changelog integration checks** and **22 isolated finalization checks**. The fixtures do not build, tag, push, or publish the actual release.
- Run the complete unit suite, full lint, final signed assembly, signer comparison, and tag creation from clean `main` after the GitHub merge. Device upgrade checks remain separate.

## Release baseline

- Previous published stable release: [v0.5.9](https://github.com/serogee/Nudge/releases/tag/v0.5.9), **code 25**, published at **2026-11-02T17:42:07Z**. GitHub releases/latest confirmed publication on **2026-11-03**. Preserve its release, notes, changelog, tag, local artifacts, and all archived builds.
- The remote annotated tag object `0881f3ef0c3b41f399c398c3fd3aef7e9ae971c4` and matching published metadata identify commit `dd26a1537f45031a00f61ea00de0fd4dcb87cec9`. The tag matches the local tag and is an ancestor of this branch.
- The published APK is **3,818,842 bytes**, with package `com.modukey.app`, version **0.5.9 / code 25**, minimum SDK **31**, and target and compile SDK **36**. Android `apksigner` verifies the local APK and certificate SHA-256 `abbf8d31940893e0dacf8280be866b89106ec09dbd89417503a7853ce515eabc`. Retain this signing identity.
- All three local artifact SHA-256 values match GitHub's published asset digests. The APK hash is `3f1e2921c621415271c43061a1e70de649d35114289114e19c697ce09734bf56`. Its checksum and JSON metadata agree with this APK and commit.
- Prepare **0.5.10, code 26**, from **v0.5.9**. Generate with `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Generate-Changelog.ps1 -FromTag v0.5.9 -Write`, then review and commit the notes. Do not regenerate them after finalization.
- Merge with **Create a merge commit**. The finalizer updates clean `main`, runs release checks, builds and verifies the signed APK, compares the prior signer, exports four local files under `releases/0.5.10/`, and creates **v0.5.10**. Publish the notes as the release description and attach only the APK, checksum, and JSON metadata. Use **v0.5.10** as the release title. Confirm publication before treating this version as the next published baseline.
