# Nudge 0.4.3 release checklist

Publication confirmed on 2026-10-23: [v0.4.3](https://github.com/serogee/Nudge/releases/tag/v0.4.3) is stable, published at 2026-10-22T15:05:57Z. The steps below are the original preparation record. Use [the 0.4.4 checklist](release-checklist-0.4.4.md) for the next release. Publication does not establish device test results.

Version: **0.4.3**, code **12**. Baseline: published **v0.4.2**, code **11**.

GitHub confirms v0.4.2 is a stable release, published at 2026-10-20T05:59:39Z. This was checked on 2026-10-22.

## Prepared on the feature branch

- Branch: `feat/brightness-gestures-handle-controls`.
- Commit the version and reviewed [release notes](release-notes-0.4.3.md) before merging.
- All development entries in Unreleased are included in the reviewed 0.4.3 notes.
- Preparation checks passed: 533 unit tests, debug lint, debug build, 29 changelog checks, and 10 isolated finalization checks.
- Keep the original signing key, application ID, published notes, and existing release tags.
- Create the final signed APK and tag after GitHub merges this branch.

## Push and merge on GitHub

```powershell
git push -u origin HEAD
```

Open a pull request into `main`. Select **Create a merge commit** and use the supplied merge message. Keep the independent commits. Squash and rebase merges do not preserve the ancestry required by the finalizer.

## Build and tag after the GitHub merge

From this repository, with a clean workspace:

```powershell
git switch feat/brightness-gestures-handle-controls
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Finalize-Release.ps1
```

The finalizer fetches `origin/main` and tags. It checks that the prepared commit was merged and that the version matches. It switches to local `main` and fast-forwards it. A divergent local `main` stops the process.

It runs changelog checks, app tests, lint, and the signed release build. It verifies the APK signature, version, application ID, and previous signer when the archived APK is available. It copies the committed notes and exports the checksum and build metadata. It creates annotated local tag **v0.4.3** after successful checks.

The scripts do not push or publish. Stop if a command fails. Do not create a second tag, move an existing tag, or generate another signing key. Do not regenerate reviewed notes after merging.

To check local preparation without fetching, switching, building, or tagging:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Finalize-Release.ps1 -CheckOnly
```

## Check the signed build

The finalizer creates these files under `releases/0.4.3/`:

- `Nudge-0.4.3.apk`
- `Nudge-0.4.3.apk.sha256`
- `Nudge-0.4.3.json`
- `Nudge-0.4.3-notes.md`

Install over 0.4.2 without uninstalling. If a phone is connected through USB debugging, use:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r ".\releases\0.4.3\Nudge-0.4.3.apk"
```

Check these items on the phone:

- Retained settings, accessibility, brightness access, older backup import, and Home shortcuts.
- Return brightness at 25%, 50%, and 75%; manual maximum; exact previous values; automatic mode; calibration and reset.
- Continuous media volume and brightness: no initial jump, proportional movement, endpoints, immediate reversal, and no second action on release.
- Held popup lifetime; cancellation, multitouch, rotation, Back, screen-off, and unavailable handles.
- Swipe and hold in Any direction and Inward only, including retreat and recrossing.
- All/This/Other visibility targets and independent Toggle and hide groups, including an invoking panel that stays usable after pause.
- Panel active colors, standalone outlines, image icons, and opacity.
- Handle opacity during panel opening, rapid raises, and full-screen reveal.
- Matched idle and overnight battery use.

Record actual results in the ignored `plans/compatibility.md`. Automated tests do not establish physical HyperOS behavior. If device checks require a source correction, prepare a higher version; preserve an existing release tag.

## Push the tag and publish

After the finalizer succeeds and the signed build is accepted:

```powershell
git push origin v0.4.3
Start-Process "https://github.com/serogee/Nudge/releases/new?tag=v0.4.3"
```

Create a GitHub release using the existing **v0.4.3** tag. Use **Nudge 0.4.3** as the title. Paste `releases/0.4.3/Nudge-0.4.3-notes.md` into the description. Attach all four files listed above.

Leave **Set as a pre-release** off and set the release as latest. Publish only after the files are attached and the notes reflect actual device results. Confirm publication before using v0.4.3 as the baseline for a later release.
