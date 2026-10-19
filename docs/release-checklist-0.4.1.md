# Nudge 0.4.1 release checklist

Version: **0.4.1**, code **10**. Baseline: **v0.4.0**, code **9**.

## Prepared on the feature branch

- Version values and reviewed notes are committed before merging.
- Notes: [release-notes-0.4.1.md](release-notes-0.4.1.md).
- The signing key, application ID, previous release tags, and released changelog entries stay unchanged.
- No feature-branch signed APK or release tag is created.

## Push and merge on GitHub

From this feature branch:

```powershell
git push -u origin HEAD
```

Open a pull request into `main`. Select **Create a merge commit** and use the supplied merge message. Keep the independent commits. The finalizer checks that the prepared commit is an ancestor of merged `main`.

Do not create `v0.4.1` on the feature branch. Do not move an existing release tag.

## Build and tag after the GitHub merge

Return to this repository with the prepared feature branch checked out and no uncommitted changes. Run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Finalize-Release.ps1
```

The script fetches `origin/main` and tags, confirms that GitHub merged the prepared commits, and checks the version. It switches to local `main` and fast-forwards it. It never creates a local merge commit or discards local commits. A divergent local `main` stops the process.

The release script runs changelog checks, unit tests, lint, and the signed release build. It verifies the APK signature, version, application ID, and previous signer when the archived APK is present. It copies the committed notes and exports the checksum and build metadata. It creates local tag **v0.4.1** only after those checks succeed.

No command pushes or publishes. If a check fails, stop and resolve it before continuing. Keep the original `.local/signing/` configuration and key. Do not generate a replacement signing key.

To check local preparation without fetching, switching, building, or tagging:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Finalize-Release.ps1 -CheckOnly
```

Do not regenerate the notes after merging. Changelog generation replaces manual review. `Release.ps1` copies the reviewed notes into the version folder.

## Check the signed build

The successful finalizer leaves these files in `releases/0.4.1/`:

- `Nudge-0.4.1.apk`
- `Nudge-0.4.1.apk.sha256`
- `Nudge-0.4.1.json`
- `Nudge-0.4.1-notes.md`

Install the APK over 0.4.0 without uninstalling. Check saved settings, accessibility recovery, brightness permissions and return behavior, haptics, handle visibility, reorder scrolling, rotation, and panel actions on the Redmi.

Exact HyperOS slider sizing, installation, and battery comparisons require physical-device tests. Record observed results in the ignored `plans/compatibility.md`. Automated checks do not establish those results.

## Push the tag and publish

After the finalizer succeeds and you accept the signed build:

```powershell
git push origin v0.4.1
```

On GitHub, create a release using **v0.4.1**. Use **Nudge 0.4.1** as the title. Paste `releases/0.4.1/Nudge-0.4.1-notes.md` into the description. Attach all four files listed above.

For a stable release, leave **Set as a pre-release** off and set the release as latest. Confirm publication before using `v0.4.1` as the next published changelog baseline.
