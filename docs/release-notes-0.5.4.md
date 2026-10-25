# Nudge 0.5.4

## Added

- Add **Adapt icon size** for Wheels, enabled by default. Fit complete artwork within the rounded wedge, with padding from both arcs and separator gaps. Turn adaptation off to set a preferred **Icon size** from **12–128 dp**, defaulting to **32 dp**. Retain the manual value when adaptation is enabled again. Available space can reduce the displayed size. (28013f1)
- Add **Icon position** from **0–100%**. Zero is the nearest safe inner position, 100 is the nearest safe outer position, and the default **50%** keeps the icon at the middle of the band. Endpoints retain room for visible artwork. Recalculate available size throughout highlight transitions. Save choices separately for portrait and landscape. Icon edits preserve Wheel Autofit. (28013f1)
- Add **Nudge accessibility** on Home and a **Toggle accessibility** action for buttons, direct gestures, and launcher shortcuts. Turning on opens Android settings; enable **Nudge handles** there. Turning off disables the connected handles service and closes its overlays. If enabled but disconnected, open settings for manual recovery. The launcher toggle works without a connected service. Preserve unlock, saved-ID, and changed-shortcut checks. (6b8592e)
- Add **Wheel hitbox** directly below **Wheel radii** in Layout. **Automatic** preserves current gesture distances and the inward margin. **Use Button Size** follows each logical button's inner and outer radii, including highlight expansion. **Manual** uses independent **0–600 dp** radii, with at least **8 dp** between them, on a thumb-only two-point slider with numeric entry. Manual defaults are **32 dp** inside and **240 dp** outside. Retain manual bounds when switching modes, save choices per orientation, and preserve visible radii and Autofit. These modes apply to Drag to pick; normal taps stay unchanged. (cea6e86)

## Fixed

- Remove circular parent clipping from Wheel artwork. Square and rounded-square icons retain their selected corners. Keep icon artwork separate from the minimum **48 dp** touch targets. Wheel capacity depends on usable touch targets rather than oversized artwork. (28013f1)
- Show cached artwork immediately when button views reopen. Preload saved app and icon-pack references, reuse stable image sizes, and retain a bounded local disk cache for later app starts. Refresh artwork after app or icon-pack updates. Missing or cleared caches load normally; missing resources keep safe fallbacks. (93c5847)
- Register protected package updates with the platform receiver API on older Android versions and private receiver flags on Android 13 and later. Verify startup, caching, and launcher actions on Android 12 and 16. (b903045)

## Changed

- Default manual **Selected size**, numeric fallback, and reset to **110%**. Autofit continues to prefer 110%. Preserve explicit saved sizes, including older 125% choices. (7056e2a)

## Maintenance

- Update setup instructions and the user guide for icon sizing, safe positions, accessibility controls, and local artwork caching. (cd3c65a)
- Process main-thread launcher callbacks while the interface test waits for shortcut support. Preserve the timeout and pinning assertions. (ae4543f)
- Document hitbox modes, manual limits, per-orientation choices, and the 110% default. (577b450)

## Compatibility and known limits

- Backups use **schema 30**. Retain per-orientation hitbox modes, manual bounds, icon choices, Wheel radii, Autofit, transitions, hover colors, action references, shared links, and overflow buttons. Older backups remain importable. Missing icon settings use adaptation on, a centered position, and a saved manual size of 32 dp. Older backups use Automatic hitboxes with their saved gesture distances. Preserve older Wheel sizes and explicit highlight sizes. Nudge 0.5.3 and earlier cannot import schema-30 backups.
- Wheels retain up to **six buttons** and minimum **48 dp** touch targets. Screen space and touch geometry can reduce capacity. Icon size and position do not change selection regions. Preferred manual icon sizes can be limited, and narrow positions can produce small artwork. Highlight growth remains subject to the available space. Manual hitbox radii remain fixed during highlight growth; angular selection still follows the logical expanded regions. The exact wheel center has no direction and does not select a button.
- Disabling Nudge handles retains saved configuration and the **Enable Gestures** choice. It does not disable other accessibility services or the separate brightness calibration service. Android requires manual approval to enable accessibility. Enabling through a switch or shortcut does not grant permission automatically.
- Icon caches remain in app storage and can be evicted by Android. JSON backups retain references rather than image files. Launcher shortcut icons remain static until pinned again.
- Physical-device checks remain pending for icon clipping and readability at extreme radii, transitions, hitbox modes and release bounds, touch ownership, left/right/bottom handles, orientation changes, accessibility disable/re-enable, disconnected launcher toggles, package updates, and a signed installation over **0.5.3**. Automated checks do not establish these results.

## Automated preparation checks

- Passed **859 unit tests across 131 classes**, with no failures, errors, or skipped cases, for feature commit `cea6e867d5bbce7b369ac1958f6f3a69b1cb79f4`. Later preparation commits change documentation only. The complete final run passed after correcting the launcher interface test's main-thread synchronization. Coverage includes rounded wedge bounds throughout transitions, full square artwork, manual preferences, orientation persistence, older imports, cache restart and invalidation, Home state, disconnected shortcuts, service cleanup, all hitbox modes, independent manual radii, release validation, editor persistence, and the 110% default/reset.
- Debug lint passed with **0 errors and 17 warnings**. Built and checked **0.5.4-debug, code 20**, application ID `com.modukey.app.debug`, minimum SDK 31, and target SDK 36.
- Release tools passed **29 changelog integration checks** and **10 isolated release finalization checks**. These checks build and tag only disposable fixtures.
- Reviewed the generated notes against the implemented behavior. All covered Unreleased entries are included here. Preserve published changelog entries, prior version notes, release artifacts, and tags.
- Final signed assembly, signer comparison, and device checks remain pending. No signed 0.5.4 APK or v0.5.4 tag is created on this feature branch. The finalizer repeats unit tests, full lint, and signed release assembly after the GitHub merge.

## Release baseline

- Previous stable release: [v0.5.3](https://github.com/serogee/Nudge/releases/tag/v0.5.3), **code 19**, published at **2026-10-24T15:52:34Z**. Rechecked GitHub releases/latest and the published build metadata on **2026-10-25**. They identify commit `bd88a9f1960952ebddd66b9f576f19ef8155f212`, which matches the release tag and main.
- The published 0.5.3 metadata records signing certificate SHA-256 `abbf8d31940893e0dacf8280be866b89106ec09dbd89417503a7853ce515eabc`. Retain that signing identity for the 0.5.4 update. This records the prior signer; the new APK comparison remains pending.
- This preparation uses **version 0.5.4, code 20**. Notes were generated with `-FromTag v0.5.3`, then reviewed. Version 0.5.4 is not yet published.
- Build the final signed APK and create v0.5.4 from clean merged main. Follow the [development and release guide](development.md#build-and-tag-after-the-github-merge), retain the existing signing identity, and verify the signed update and changed features on a device before publication.
