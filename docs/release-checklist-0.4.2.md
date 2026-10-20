# Nudge 0.4.2 release checklist

Version: **0.4.2**, code **11**. Baseline: published **v0.4.1**, code **10**.

## Prepared on the feature branch

- Commit version values and reviewed notes before merging.
- Notes: [release-notes-0.4.2.md](release-notes-0.4.2.md).
- The signing key, application ID, previous release tags, and released changelog entries stay unchanged.
- No feature-branch signed APK or release tag is created.

## Push and merge on GitHub

```powershell
git push -u origin HEAD
```

Open a pull request into `main`. Select **Create a merge commit** and use the supplied merge message. Keep the independent commits. The finalizer checks that the prepared commit is an ancestor of merged `main`.

Do not create `v0.4.2` on the feature branch. Do not move an existing release tag.

## Build and tag after the GitHub merge

Return to this repository with the prepared feature branch checked out and no uncommitted changes. Run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Finalize-Release.ps1
```

The script fetches `origin/main` and tags, confirms that GitHub merged the prepared commits, and checks the version. It switches to local `main` and fast-forwards it. It then runs release checks, builds the signed APK, verifies its signer and version, exports the checksum and build metadata, and creates local tag **v0.4.2**.

Install the APK over 0.4.1 without uninstalling. Check saved settings, accessibility recovery, brightness, haptics, handle visibility and animation toggle, color selection, rotation, and panel actions. Confirm that backup schema 14 imports older backups. Record actual phone results in the ignored `plans/compatibility.md`.

## Push the tag and publish

```powershell
git push origin v0.4.2
```

On GitHub, create a stable release using **v0.4.2**. Use **Nudge 0.4.2** as the title. Paste `releases/0.4.2/Nudge-0.4.2-notes.md` into the description. Attach these files:

- `Nudge-0.4.2.apk`
- `Nudge-0.4.2.apk.sha256`
- `Nudge-0.4.2.json`
- `Nudge-0.4.2-notes.md`

Leave **Set as a pre-release** off and set the release as latest. Confirm publication before using `v0.4.2` as the next baseline.
