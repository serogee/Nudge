# Nudge 0.4.0 release checklist

Version: **0.4.0**, code **9**. Published baseline: **v0.3.1**, code **8**.

## Prepared on the feature branch

- Version values and reviewed notes are committed before merging.
- Notes: [release-notes-0.4.0.md](release-notes-0.4.0.md).
- The signing key, application ID, previous release tags, and published changelog entries stay unchanged.
- No feature-branch signed APK or release tag is created.

## Push and merge on GitHub

From this feature branch:

```powershell
git push -u origin HEAD
```

Open a pull request into `main`. Select **Create a merge commit**, using the supplied merge message. Keep the independent commits. The finalizer checks that the prepared commit is an ancestor of merged `main`.

Do not create `v0.4.0` on the feature branch. Do not move an existing release tag.

## Build and tag after the GitHub merge

Return to this repository with the prepared feature branch checked out and no uncommitted changes. Run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Finalize-Release.ps1
```

The script fetches `origin/main` and tags, confirms that GitHub merged the prepared commits, and checks the version. It switches to local `main` and fast-forwards it. It never creates a local merge commit or discards local commits. A divergent local `main` stops the process.

The release script runs changelog checks, unit tests, lint, and the signed release build. It verifies the APK signature, version, application ID, and previous signer when the archived APK is present. It copies the committed notes and exports the checksum and build metadata. It creates local tag **v0.4.0** only after those checks succeed.

No command pushes or publishes. If a check fails, stop and resolve it before continuing. Keep the original `.local/signing/` configuration and key. Do not generate a replacement signing key.

To check local preparation without fetching, switching, building, or tagging:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Finalize-Release.ps1 -CheckOnly
```

Do not regenerate the notes after merging. Changelog generation replaces manual review. `Release.ps1` copies the reviewed notes into the version folder.

## Check the signed build

The successful finalizer leaves these files in `releases/0.4.0/`:

- `Nudge-0.4.0.apk`
- `Nudge-0.4.0.apk.sha256`
- `Nudge-0.4.0.json`
- `Nudge-0.4.0-notes.md`

Install the APK over 0.3.1 without uninstalling. Check saved settings and accessibility recovery. Then check downloads, brightness permission and restore, shortcuts, native shades, handle pauses, rotation, and editor controls on the Redmi.

Exact HyperOS slider sizing, launcher behavior, installation, and battery comparisons require physical-device tests. Record observed results in the ignored `plans/compatibility.md`. Automated checks do not establish those results.

## Push the tag and publish

After the finalizer succeeds and you accept the signed build:

```powershell
git push origin v0.4.0
```

On GitHub, create a release using **v0.4.0**. Use **Nudge 0.4.0** as the title. Paste `releases/0.4.0/Nudge-0.4.0-notes.md` into the description. Attach all four files listed above.

For a stable release, leave **Set as a pre-release** off and set the release as latest. Publish after attaching the APK and other files. GitHub's source archives do not replace the APK.

Confirm GitHub publication before using `v0.4.0` as the next published changelog baseline. A local tag or APK does not confirm publication.
