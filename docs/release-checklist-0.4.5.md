# Nudge 0.4.5 release checklist

Version: **0.4.5**, code **14**. Baseline: published **v0.4.4**, code **13**.

GitHub confirms [v0.4.4](https://github.com/serogee/Nudge/releases/tag/v0.4.4) is stable, published at 2026-10-22T17:51:33Z. This was checked on 2026-10-23.

## Prepared on the feature branch

- Branch: `feat/automatic-brightness-calibration`.
- Commit the version and reviewed [release notes](release-notes-0.4.5.md) before merging.
- All ten development entries in Unreleased are included in the reviewed 0.4.5 notes.
- Preparation checks passed: all 607 unit tests, debug lint with 0 errors and 25 warnings, the 0.4.5-debug build with code 14, 29 changelog checks, and 10 isolated finalization checks.
- The [Nudge 0.4.5 GitHub draft](https://github.com/serogee/Nudge/releases/tag/untagged-ca09ff4f39434c65d69e) contains the reviewed notes. It is unpublished. The final assets and release tag remain pending.
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

Confirm `versionName=0.4.5` and `versionCode=14`. Then run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Finalize-Release.ps1
```

The finalizer fetches `origin/main` and tags. It checks that the current commit is on remote main and that the version matches. It fast-forwards local main. A dirty workspace or divergent local main stops the process.

It runs changelog checks, all app tests, lint, and the signed release build. It verifies the APK signature, version, application ID, and previous signer when the archived 0.4.4 APK is available. It copies the committed notes and exports the checksum and build metadata. After successful checks, it creates annotated local tag **v0.4.5** on the merged main commit.

The scripts do not push or publish. Do not create a second tag, move an existing tag, generate another signing key, or regenerate reviewed notes after merging. If a tagged build needs a source correction, prepare a higher version.

To check committed local preparation without fetching, switching, building, or tagging:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Finalize-Release.ps1 -CheckOnly
```

## Check the signed build

The finalizer creates these files under `releases/0.4.5/`:

- `Nudge-0.4.5.apk`
- `Nudge-0.4.5.apk.sha256`
- `Nudge-0.4.5.json`
- `Nudge-0.4.5-notes.md`

Install over 0.4.4 without uninstalling. With an authorized phone connected through USB debugging:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r ".\releases\0.4.5\Nudge-0.4.5.apk"
```

Check these items on the phone:

- Upgrade retention: saved bindings, manual calibration, accessibility, brightness access, Home shortcuts, and older backup import. Backup schema remains 20.
- Manual calibration: capture a native zero minimum and increasing samples. Reject invalid values. Reset and reload the saved profile.
- Automatic setup: disclosure and Modify system settings access, separate service enablement, return to Home, and explicit Start. Enabling alone must not move the native slider. Setup times out after two minutes.
- Automatic measurement: five native positions, visible progress and Cancel, real native brightness mapping, and placement of the temporary overlay.
- Completion: original brightness and automatic mode return before Save. Save updates the open preview and active button states. Discard keeps the old profile. The calibration service turns off.
- Cancellation: Cancel, screen-off, lock, rotation, service interruption, missing controls, refused progress actions, and unstable readings. Nudge attempts restoration and keeps the previous profile.
- Recovery: interrupted restoration or process death shows Restore previous brightness and Keep current brightness. Other Nudge brightness actions stay blocked until recovery is resolved. A run never resumes automatically after process death.
- Native controls: a minimum that is already selected, a System UI plugin control if exposed, and rejection of unrelated color or dimming controls. Unsupported phones retain Manual calibration.
- Additional options: Slide to adjust below the selected popup, action controls below Action, layout controls below Layout or Direction, nested left lines, small screens, large text, and retained hold and color values.
- Existing gestures: After hold and Immediate volume and brightness adjustment, direction reversal, endpoints, and capture cleanup.
- Matched idle and overnight battery use.

Record actual results in the ignored `plans/compatibility.md`. Automated checks do not establish physical HyperOS behavior.

## Push the tag and publish

The GitHub draft is ready with the reviewed notes. Keep it unpublished until the merged source is built and verified. It uses **v0.4.5** and target **main**. Remote tag **v0.4.5** was confirmed absent after the draft was saved. The finalizer creates the local annotated tag after the merge.

After the finalizer succeeds and the signed build is accepted:

```powershell
git push origin v0.4.5
Start-Process "https://github.com/serogee/Nudge/releases"
```

Open the existing **Nudge 0.4.5** draft. If none exists, create a release using the existing **v0.4.5** tag. Paste `releases/0.4.5/Nudge-0.4.5-notes.md` into the description. Attach all four files listed above.

Leave **Set as a pre-release** off and set the release as latest. Publish after the files are attached and device checks are accepted. Confirm publication before using v0.4.5 as a later release baseline.
