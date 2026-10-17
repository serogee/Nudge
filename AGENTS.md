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

- Commit completed features and fixes before preparing release notes. Increase both version values in `version.properties` for a new distributed APK.
- Run `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Generate-Changelog.ps1 -Write` to prepare the new version's changelog and release notes from commit titles.
- Review the notes and add observed compatibility results after the final generation. Commit the version and notes before running `scripts/Release.ps1`.
- Preserve released changelog entries and tags. Changelog generation must not claim physical-device tests passed without device evidence.
