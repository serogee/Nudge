# Repository instructions

## Commit messages

- Use `type(scope): description` for the title, with concise, imperative wording that follows the existing commit style.
- Choose the type for the change, such as `fix`, `feat`, `build`, `docs`, `test`, or `chore`.
- Use the narrowest applicable approved scope from the table below. If none fits, suggest a new scope for user approval before using it.
- Follow ASD-STE100 English loosely for the body only. Use short sentences, clear verbs, and consistent technical terms.
- Start every body item with `- `.
- Do not include blank lines, a `[merge]` label, or a PR number.
- When asked only for a commit message, return only that message.
- Apply these rules to future commits. Do not rewrite existing commits or release tags to change their format unless the user explicitly asks.

### Approved scopes

Approved by the user on 2026-10-17. Keep local planning files in the ignored `.plans/` directory.

| Scope | Use for |
| --- | --- |
| `trigger` | Coordinated edge-shortcut changes to geometry, activation, visibility, rotation, lock-screen operation, or its live preview. |
| `settings` | Preference storage, migration, defaults, field updates, and save coordination. |
| `accessibility` | Service binding, connection reporting, permission checks, lifecycle cleanup, and service diagnostics. |
| `ui` | Settings screens, controls, help text, previews, and app branding when trigger behavior is unchanged. |
| `release` | Versioning, signing, APK packaging, release scripts, changelog, and compatibility records. |
| `repo` | Git configuration, repository instructions, commit conventions, and scope definitions. |

## App text

- Use ASD-STE100 as the style target for approximately 90% of app-generated English. Keep Android menu names, app names, and necessary technical terms.
- Use short, active sentences. Give one instruction per sentence and one topic per paragraph.
- Aim for at most 20 words in instructions and 25 words in explanations. Labels can be short phrases.
- Use consistent terms: handle, touch area, visible handle, panel, button, and volume popup.
- Remove slogans, metaphors, repeated explanations, and implementation details that do not help the user act.
- Put essential instructions beside controls. Put longer setup and recovery steps in Help.
- Keep app-generated labels, explanations, errors, and accessibility descriptions in string resources. User-supplied names and external release content keep their original text.

## Release notes

- Make a release only when the user explicitly requests one. A feature or fix request does not authorize version bumps, versioned release notes, release APK builds, tags, or publishing.
- During development, keep changes under Unreleased and use tests, lint, or debug builds for verification. Do not invoke `scripts/Release.ps1` automatically.
- Store each version's APK, checksum, exported notes, and build metadata in `releases/<version>/`. Keep archived builds under `releases/archive/`.
- Treat established local version tags (`v<major>.<minor>.<patch>`) as published releases. The newest tag can remain a working release until the user marks it ready. Do not use a working tag as the published baseline merely because it exists.
- Use the latest earlier, reachable published tag as the changelog baseline. On 2026-10-23, GitHub releases/latest confirmed `v0.4.4`, code 13, is published as a stable release (published at 2026-10-22T17:51:33Z). `0.4.5`, code 14, is the current working release. Use `-FromTag v0.4.4` for its notes. Do not treat `0.4.5` as published until publication is confirmed.
- Prepare the version and reviewed notes on the feature branch. Build the final signed APK and create its tag from clean `main` after a GitHub merge or a user-authorized local merge. Do not merge, push, or publish without the user's instruction.
- Preserve the earlier unpublished code-3 APK and exact tag object under `releases/archive/0.1.2-build.3/` and `archive/v0.1.2-build.3`. The old tag was never pushed. The 0.1.3 build was canceled before export or tagging.
- Commit completed features and fixes before preparing release notes. Increase both version values in `version.properties` for a new distributed APK.
- Run `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Generate-Changelog.ps1 -Write` to prepare the new version's changelog and release notes from commit titles.
- Review the notes and add observed compatibility results after the final generation. Commit the version and notes before running `scripts/Release.ps1`.
- After an authorized release is prepared, remove entries from Unreleased only after confirming they are included in its reviewed notes. Do not treat a local build tag as proof of publication.
- Preserve released changelog entries and tags. Changelog generation must not claim physical-device tests passed without device evidence.
