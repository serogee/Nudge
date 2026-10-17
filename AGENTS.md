# Repository instructions

## Commit messages

- Use `type(scope): description` for the title, with concise, imperative wording that follows the existing commit style.
- Choose the type for the change, such as `fix`, `feat`, `build`, `docs`, `test`, or `chore`.
- Read `.plans/scopes.md` and use the narrowest applicable approved scope. If none fits, suggest a new scope for user approval before using it.
- Follow ASD-STE100 English loosely for the body only. Use short sentences, clear verbs, and consistent technical terms.
- Start every body item with `- `.
- Do not include blank lines, a `[merge]` label, or a PR number.
- When asked only for a commit message, return only that message.
- Apply these rules to future commits. Do not rewrite existing commits or release tags to change their format unless the user explicitly asks.

## Release notes

- Make a release only when the user explicitly requests one. A feature or fix request does not authorize version bumps, versioned release notes, release APK builds, tags, or publishing.
- During development, keep changes under Unreleased and use tests, lint, or debug builds for verification. Do not invoke `scripts/Release.ps1` automatically.
- Use the latest user-confirmed published release as the changelog baseline. As of 2026-10-17, only 0.1.1 is confirmed published. The user requested the current changes as 0.1.2; this build keeps that version name and increases the version code to 4. The earlier unpublished code-3 APK and exact tag object are preserved under `releases/archive/0.1.2-build.3/` and `archive/v0.1.2-build.3`. The old tag was never pushed. The 0.1.3 build was canceled before export or tagging.
- Commit completed features and fixes before preparing release notes. Increase both version values in `version.properties` for a new distributed APK.
- Run `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Generate-Changelog.ps1 -Write` to prepare the new version's changelog and release notes from commit titles.
- Review the notes and add observed compatibility results after the final generation. Commit the version and notes before running `scripts/Release.ps1`.
- After an authorized release is prepared, remove entries from Unreleased only after confirming they are included in its reviewed notes. Do not treat a local build tag as proof of publication.
- Preserve released changelog entries and tags. Changelog generation must not claim physical-device tests passed without device evidence.
