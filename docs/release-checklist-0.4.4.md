# Nudge 0.4.4 release checklist

Version: **0.4.4**, code **13**. Baseline: published **v0.4.3**, code **12**.

GitHub confirms [v0.4.3](https://github.com/serogee/Nudge/releases/tag/v0.4.3) is stable, published at 2026-10-22T15:05:57Z. This was checked on 2026-10-23.

## Prepared on the feature branch

- Branch: `feat/brightness-gesture-settings`.
- Commit the version and reviewed [release notes](release-notes-0.4.4.md) before merging.
- All four development entries in Unreleased are included in the reviewed 0.4.4 notes.
- Preparation checks passed: 555 unit tests, debug lint with 0 errors and 25 warnings, debug build, 29 changelog checks, and 10 isolated finalization checks.
- Keep the original signing key, application ID, published notes, and existing release tags.
- Build the final signed APK and create its tag from clean merged main.

## Push and merge on GitHub

```powershell
git push -u origin HEAD
```

Open a pull request into `main`. Select **Create a merge commit** and use the supplied merge message. Keep the independent commits.

## Build and tag after the GitHub merge

From this repository, with a clean workspace, run each command in order. Stop if a command fails:

```powershell
git switch main
git pull --ff-only origin main
Get-Content version.properties
```

Confirm `versionName=0.4.4` and `versionCode=13`. Then run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Finalize-Release.ps1
```

The finalizer fetches `origin/main` and tags. It checks that the current commit is on remote main and that the version matches. It fast-forwards local main. A dirty workspace or divergent local main stops the process.

It runs changelog checks, all app tests, lint, and the signed release build. It verifies the APK signature, version, application ID, and previous signer when the archived 0.4.3 APK is available. It copies the committed notes and exports the checksum and build metadata. After successful checks, it creates annotated local tag **v0.4.4** on the merged main commit.

The scripts do not push or publish. Do not create a second tag, move an existing tag, generate another signing key, or regenerate reviewed notes after merging. If a tagged build needs a source correction, prepare a higher version.

To check committed local preparation without fetching, switching, building, or tagging:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Finalize-Release.ps1 -CheckOnly
```

## Check the signed build

The finalizer creates these files under `releases/0.4.4/`:

- `Nudge-0.4.4.apk`
- `Nudge-0.4.4.apk.sha256`
- `Nudge-0.4.4.json`
- `Nudge-0.4.4-notes.md`

Install over 0.4.3 without uninstalling. With an authorized phone connected through USB debugging:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r ".\releases\0.4.4\Nudge-0.4.4.apk"
```

Check these items on the phone:

- Upgrade retention: saved bindings, calibration, accessibility, brightness access, older backup import, and Home shortcuts.
- Swipe adjustment: the popup opens immediately; completing a diagonal swipe leaves the value unchanged. Early release does not change it.
- Hold time: test 100, 200, and 1,000 ms. Movement beyond the tolerance restarts the timer on either axis. Small jitter does not restart it.
- After activation: the ready cue follows the swipe haptic switch. The first small movement is ignored. Further volume and brightness movement is proportional, including reversal and endpoints.
- Immediate: both volume and brightness adjust without the extra hold. Hold and Swipe and hold retain their own existing deadlines.
- Capture cleanup: release, a second finger, rotation, screen-off, settings changes, unavailable handles, and service restart.
- Ordinary swipes: Any direction accepts diagonal and parallel displacement; retreat reduces distance. Inward only rejects outward swipes. Swipe and hold keeps its separate direction choice.
- Calibration: open it below Home's brightness slider controls. Capture and reset samples. Check native brightness at 25%, 50%, and 75% with automatic brightness off.
- Additional options: chevrons, parent arrows, picker badge overlap, action selection, small screens, large text, and retained color values.
- Matched idle and overnight battery use.

Record actual results in the ignored `plans/compatibility.md`. Automated checks do not establish physical HyperOS behavior.

## Push the tag and publish

After the finalizer succeeds and the signed build is accepted:

```powershell
git push origin v0.4.4
Start-Process "https://github.com/serogee/Nudge/releases/new?tag=v0.4.4"
```

Create the GitHub release using the existing **v0.4.4** tag. Use **Nudge 0.4.4** as the title. Paste `releases/0.4.4/Nudge-0.4.4-notes.md` into the description. Attach all four files listed above.

Leave **Set as a pre-release** off and set the release as latest. Publish after the files are attached and device checks are accepted. Confirm publication before using v0.4.4 as a later release baseline.
