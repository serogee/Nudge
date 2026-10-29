# Nudge 0.5.7

## Added

- Give **Toggle and hide handles** a separate icon. Add **Toggle handles**, **Toggle handle visibility**, and **Toggle and hide handles** to the built-in icon picker. Use the same artwork in panels, Wheels, previews, and new or refreshed Home shortcuts. (7262407)
- Add **Additional option info** in **Info → Interface**, enabled by default. Turn it off to hide descriptions below options throughout the app, including editor dialogs. Keep labels, current values, live service status, errors, progress, and Help content visible. Invalid duration entries still show their allowed range. (3e8c793)
- Save the option-info choice across restarts and JSON backups. Older configurations default to showing descriptions. Adding copies from a backup keeps the current choice; replacing the configuration uses the imported choice. (3e8c793)

## Fixed

- Keep enabled gesture feedback to one pulse when **Swipe**, **Hold**, or **Swipe and hold** opens a Wheel. Changing the highlighted item, leaving and reentering the Wheel, and releasing an item do not repeat that pulse. Keep item highlighting and the selected action. (d5275d5)
- Use a slashed eye for **Toggle handle visibility** and its built-in icon. Show the button as active when all targeted handles are hidden. As with **Hide visible handles**, active means hidden. Visible, mixed, and empty target groups remain inactive. Keep **All handles**, **This handle**, and **Other handles** scopes. (a3aeaee)

## Compatibility and known limits

- New backups use **schema 32** for the added built-in icon references and the saved option-info choice. Older backups remain importable. **Nudge 0.5.6 and earlier cannot import schema-32 backups.**
- Preserve existing handles, panels, Wheels, gestures, shortcut definitions, saved percentages, manual radii, appearance, and per-orientation settings. The visibility action's active state changes its presentation; its target selection and saved hide/show behavior stay the same.
- Use **Pin again** to refresh an existing Home shortcut's artwork. Saved shortcut IDs and actions stay the same. Launcher rendering can vary by device.
- Physical-device checks remain pending for the signed upgrade over **0.5.6**, retained settings, visibility scopes and colors, icon readability, launcher refresh, option descriptions, backup import, and Wheel haptic feel. Automated checks do not establish these results.

## Automated preparation checks

- Passed **136 focused tests across 13 classes**, with **0 failures, errors, or skipped cases**, for the prepared 0.5.7 source. Coverage includes backup defaults and disk reopening, editor drafts, option descriptions and validation, handle states, action routing, icon rendering, shortcut artwork, Wheel selection, and gesture feedback.
- Service, haptic, and artwork regressions cover **API 31 and 36** through Robolectric. The option-info setting also has interface checks on both APIs. These are simulated Android results, not physical-device results or a complete release test run.
- Passed **lintDebug** with **0 errors and 27 warnings** and **assembleDebug**. APK badging and output metadata confirm **0.5.7-debug / code 23**, package `com.modukey.app.debug`, minimum SDK **31**, and target and compile SDK **36**.
- Passed **29 changelog integration checks** and **22 isolated finalization checks**. The first finalization fixture run was blocked by a Windows Git-helper permission error; the rerun passed. These tests use disposable repositories and do not build or publish the real release.
- Final signed assembly, the complete unit suite, full lint, and device upgrade checks follow the GitHub merge. No 0.5.7 release APK, tag, push, merge, or publication was performed during preparation.

## Release baseline

- Previous published stable release: [v0.5.6](https://github.com/serogee/Nudge/releases/tag/v0.5.6), **code 22**, published at **2026-10-27T03:24:22Z**. GitHub releases/latest reconfirmed publication on **2026-10-29**. This is a published release, not only a local working tag.
- The remote annotated tag object `9e9ceafdf25fa95fd6a7b6005aa60a283d98dd9f` and published APK metadata identify commit `5778bf358894e041ab8a5c9dc049a76faf099d56`. That commit is reachable from this preparation branch.
- The archived APK is **3,813,638 bytes**, package `com.modukey.app`, version **0.5.6 / code 22**, minimum SDK **31**, target and compile SDK **36**. Android `apksigner` verifies the APK and certificate SHA-256 `abbf8d31940893e0dacf8280be866b89106ec09dbd89417503a7853ce515eabc`. Retain this signing identity.
- The local APK SHA-256 matches the published asset digest: `5f5297cc96a2cde294706214654e513535efa73ea49c4ea963f895b502797ed4`. The checksum file digest matches `602469a1a2154ee358db384bdff58d3c85ed1b3981cc4dba86a30f90b6b08535`. The build metadata digest matches `754db4eb768156276453b5b6472c61567b3e1073e8cad24fd78325f604d46756`.
- GitHub has the APK, checksum, and build metadata as three assets. Reviewed notes are in the release body; no separate notes asset was published for 0.5.6. Preserve its existing local notes export and published history.
- Prepare **0.5.7, code 23**, from **v0.5.6**. Generate with `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Generate-Changelog.ps1 -FromTag v0.5.6 -Write`, then review the notes. Do not regenerate them after finalization.
- After the GitHub merge, run `scripts/Finalize-Release.ps1` from clean merged `main`. It runs the complete unit suite, full lint, and signed release assembly, checks the previous APK signer, exports four files under `releases/0.5.7/`, and creates **v0.5.7**. Verify those files and the signed upgrade before pushing the existing tag and publishing. Confirm GitHub publication before marking 0.5.7 as published.
