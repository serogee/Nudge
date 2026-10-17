# Commit scopes

Status: approved by the user on 2026-10-17.

Use the narrowest scope that covers the change. Use a scope from this file. If no scope fits, propose a new scope and obtain user approval before using it.

| Scope | Use for |
| --- | --- |
| `trigger` | Coordinated edge-shortcut changes to geometry, activation, visibility, rotation, lock-screen operation, or its live preview. |
| `settings` | Preference storage, migration, defaults, field updates, and save coordination. |
| `accessibility` | Service binding, connection reporting, permission checks, lifecycle cleanup, and service diagnostics. |
| `ui` | Settings screens, controls, help text, previews, and app branding when trigger behavior is unchanged. |
| `release` | Versioning, signing, APK packaging, release scripts, changelog, and compatibility records. |
| `repo` | Git configuration, repository instructions, commit conventions, and scope definitions. |

For coordinated edge-shortcut feature changes such as 0.1.1, use `trigger`. For a change limited to this file and commit instructions, use `repo`.
