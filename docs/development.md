# Development and releases

Start with the README's [requirements and build steps](../README.md#development). This guide contains maintained repository and release instructions.

## Repository conventions

Use [AGENTS.md](../AGENTS.md) for approved commit scopes, message style, app text, and release rules.

Keep personal plans in the ignored `.plans/` directory. Signing material, local verification output, and device profiles belong under the ignored `.local/` directory. Never commit signing keys or passwords.

The documentation has distinct purposes:

| File | Purpose |
| --- | --- |
| [README](../README.md) | Project overview, main features, privacy, and first build. |
| [User guide](handles-and-panels.md) | Setup, detailed controls, recovery, and known limits. |
| [Changelog](../CHANGELOG.md) | Unreleased changes and a searchable version history. |
| `docs/release-notes-<version>.md` | Reviewed release details, compatibility results, and known limits. |
| This guide | Development conventions, device checks, and one reusable release process. |

The release scripts require the changelog and the current version's reviewed notes. Preserve published entries and tags. Versioned release checklists and completed investigation write-ups are not needed; use this guide and Git history.

## Development checks

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Run checks appropriate to the change. For release-tool changes, also run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Test-Changelog.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Test-Finalize-Release.ps1
```

These integration checks use isolated fixtures. They do not build or publish a real release.

## Licensing

Nudge uses [GNU GPL version 3 or later](../LICENSE), identified as `GPL-3.0-or-later`. The README states the project's license grant. The APK includes the same grant and full license text in `assets/licenses/nudge.txt`.

When distributing an APK, provide its matching Corresponding Source under the license. Include the source files and build scripts needed to make that version. Preserve third-party licenses and required notices. Keep the bundled license text identical to the root LICENSE file after its project notice.

## Device verification

With Android platform-tools on PATH, connect and authorize a phone through USB debugging:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Device-Profile.ps1
```

Use `-Serial <adb-serial>` if several devices are connected. Debug installations need `-ApplicationId com.modukey.app.debug`. Profiles are saved under `.local/device-checks/`.

Record the model, Android version, MIUI or HyperOS build, navigation mode, app version, steps, and observed result. Keep private notes under `.local/device-checks/` or `.plans/`. The older ignored `plans/compatibility.md` remains usable if it exists locally.

Check changed features on the real device. Common checks include gesture routing, native popups, panel dismissal, rotation, locked use, brightness restoration, shortcuts, and retained settings after an update. Use matched conditions for battery comparisons.

Automated passes do not establish physical-device compatibility. Add only observed device results to release notes.

## Prepare an explicitly requested release

Feature and fix work stays under **Unreleased**. Do not bump versions, create versioned notes, build a release APK, tag, or publish unless a release is requested.

1. Commit completed features and fixes on the feature branch.
2. Confirm the latest earlier published release on GitHub. A working local tag does not prove publication.
3. Increase both `versionName` and `versionCode` in `version.properties`.
4. Generate notes from that published baseline:

```powershell
$publishedTag = Read-Host 'Previous published tag, for example v0.5.0'
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Generate-Changelog.ps1 -FromTag $publishedTag -Write
```

Review the generated changelog entry and `docs/release-notes-<version>.md`. Add compatibility evidence and known limits after the final generation; another generation replaces manual edits.

Remove Unreleased entries only after confirming they are included in the reviewed notes. Commit the version and notes before the signed build.

Push the feature branch and open a pull request into `main`. Select **Create a merge commit** to retain independent commits. Build and tag the final release from clean merged `main`.

## Build and tag after the GitHub merge

Run these commands from a clean workspace after the prepared commits are merged. Stop if a command fails.

```powershell
git switch main
git pull --ff-only origin main
Get-Content version.properties
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Finalize-Release.ps1
```

Confirm the version before running the finalizer. It checks that the prepared commit is on remote main, fast-forwards local main, runs release checks, and builds the signed APK. It verifies package identity, version, signature, and the earlier archived signer when available. It exports artifacts and creates the annotated local version tag. It does not push or publish.

For a local preparation check without fetching, switching, building, or tagging, use:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Finalize-Release.ps1 -CheckOnly
```

The original `.local/signing/` configuration is required. For the first release only, `scripts/Initialize-Signing.ps1` creates it and refuses to replace an existing key. Back up the key and properties securely outside the repository. Restore them for later builds; do not generate a replacement key.

Artifacts are saved under `releases/<version>/`:

- `Nudge-<version>.apk`
- `Nudge-<version>.apk.sha256`
- `Nudge-<version>.json`
- `Nudge-<version>-notes.md`

Keep archived builds under `releases/archive/`. Preserve the earlier unpublished code-3 build and its archive tag as specified in AGENTS.md.

Install the signed APK over the preceding release without uninstalling. Check retained configuration, accessibility and brightness access, then the changed features. Record device results and unresolved limits. A debug installation does not establish release upgrade compatibility.

### Recover from failed release checks

A failed unit test stops the signed release build. Inspect `app/build/reports/tests/testDebugUnitTest/index.html` for the failed assertion. Deprecation and native-access warnings do not explain an assertion failure.

If tests still expect an older backup schema after pulling a schema change, rebuild the compiled tests from the repository root:

```powershell
.\gradlew.bat clean :app:testDebugUnitTest --no-build-cache --no-problems-report "-Pkotlin.incremental=false"
```

Quote the complete `-P` argument in PowerShell. Without quotes, PowerShell can split the property into separate arguments. `--no-problems-report` avoids failures while replacing the generated HTML problems report; test execution and test reports remain enabled.

If a test still fails after the clean build, correct the test or application behavior. Commit source fixes on a feature branch and merge them through a pull request. Then update clean `main` and run the finalizer again.

## Publish after verification

After the signed build and device checks are accepted, push the tag created by the finalizer:

```powershell
$releaseVersion = (Get-Content version.properties | Where-Object { $_ -like 'versionName=*' }) -replace '^versionName=', ''
git push origin "v$releaseVersion"
```

Open an existing GitHub draft for that tag or create a release using the existing tag. Use **Nudge <version>** as the title. Copy the exported notes into the description and attach all four artifacts.

For a stable release, leave pre-release off and set it as latest. Publish only after verifying the attachments. Confirm publication before using that tag as a future changelog baseline.

You can use GitHub CLI instead of the release editor. Run `gh auth status` first. After pushing the finalizer's tag, create a draft with all four exported files:

```powershell
$assetRoot = "releases/$releaseVersion"
gh release create "v$releaseVersion" `
  --verify-tag `
  --draft `
  --title "Nudge $releaseVersion" `
  --notes-file "$assetRoot/Nudge-$releaseVersion-notes.md" `
  "$assetRoot/Nudge-$releaseVersion.apk" `
  "$assetRoot/Nudge-$releaseVersion.apk.sha256" `
  "$assetRoot/Nudge-$releaseVersion.json" `
  "$assetRoot/Nudge-$releaseVersion-notes.md"
gh release view "v$releaseVersion" --web
```

This creates an unpublished draft. Review the APK, checksum, metadata, notes, and upgrade results before publishing. When ready, publish it as the latest stable release:

```powershell
gh release edit "v$releaseVersion" --draft=false --latest
gh release view "v$releaseVersion" --web
```

Do not create the tag manually or rerun changelog generation after finalization. The finalizer creates the tag and exports the committed, reviewed notes. To enable automatic signer comparison without a connected device, retain the previous published APK at `releases/<previous-version>/Nudge-<previous-version>.apk` before running the finalizer.

Do not move existing release tags or replace published notes to describe a source correction. Prepare a new version with a higher version code.
