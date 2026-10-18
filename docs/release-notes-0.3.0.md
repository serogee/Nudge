# Nudge 0.3.0

Changes use v0.1.1 as the last confirmed published baseline. Earlier local builds and tags remain preserved.

## Release highlights

- Create independent handles and shared panels. Add, name, duplicate, order, enable, and delete handles. Configure each panel separately.
- Assign six gestures to a panel, a direct action, or No action. Select Show Volume Popup independently for each assignment.
- Run app, settings, sound, lock, and native Input method picker actions. Request normal unlock when an action needs it.
- Use Fixed or Scrollable grids, retained overflow buttons, icon packs, library icons, and standalone or container icons.
- Select separate open and close effects. Set an optional panel timeout that pauses during held input and resets swipe progression on expiry.
- Select three outside-touch policies: close and block, close and pass through, or pass through and keep open. Back closes the panel.
- Use full-screen soft hide. The first gesture reveals the handle with a brief bounce. Later gestures run assigned actions.
- Extend touch length to the usable short edge, or enable the longer edge. Screen limits preserve saved values.
- Control real editing overlays with sticky Preview switches. Mini previews allow page scrolling and do not accept position gestures.
- Add independent empty panel buttons with container-only, blank, or slash appearances.
- Make positive Follow handle Y offsets move panels upward. Preserve old physical placement through schema-4 migration.
- Keep panel search inside its selection dialog. Retain action drafts when a nested picker is canceled.
- Set button and icon opacity independently. Use one palette and automatic contrast for built-in symbols.
- Keep handle badges beside bold titles. Use orientation-aware notches, an edge-handle tab icon, and the shared N app mark.
- Prevent panel button activation after drag movement. Retain valid taps and accessibility actions.
- Keep panels open for initial activity observations, stale events, and ambiguous popup windows. Close on confirmed app transitions.
- Record bounded dismissal reasons and event context. Copy these details from Info after an unexpected close.
- Preserve saved preferences, imports, landscape placement, and lock-screen options. Imported copies receive new IDs and start disabled.
- Keep one accessibility service without polling, wake locks, periodic restart jobs, or a separate foreground service.

## Compatibility and known limits

- Automated checks passed: 217 unit/Robolectric tests, lint with zero errors and seven warnings, and the debug build.
- Panel dismissal regressions cover API 31 and 36. They establish code defects and fixes, not the cause of the Redmi incident.
- Physical Redmi/HyperOS, native popup coexistence, keyboard selection, touch routing, unlock, Recents, and battery comparisons remain pending.
- The signed APK build and tag must use the merged main commit. Signed installation over an earlier release still needs a phone.
- The autostart shortcut depends on the HyperOS build. Manual instructions remain available.
- App and icon-pack images retain their colors. Automatic contrast applies to built-in symbols. Some controls lack individual reset icons.
- Unresolved activity events cannot confirm an app transition. Multi-window and HyperOS reporting still need device checks.
- Version 0.3.0 uses code 7 and the existing application and signing identities. Existing release tags remain unchanged.

## Added

- make indicator thickness a percentage (634e444)
- check GitHub updates automatically on request (9ae4350)
- add per-handle configuration and panel models (4e4e5af)
- define editing sessions and handle eligibility (d1c7f74)
- reconcile ordered handle windows (33bc560)
- add native handle input and status (dc15e8d)
- load static icon pack resources (6267535)
- add panel actions and unlock continuation (de093e1)
- share controls and render panel grids (5a3e82e)
- host panels with touch dismissal (30f6451)
- connect independent handle windows (709fc59)
- preview panel placement and handle overlaps (396deac)
- connect handle and panel settings (819d64f)
- add an event-driven panel timeout (315e1aa)
- add a HyperOS autostart shortcut (cfed5ed)
- report the active panel ID (55a29d1)
- add shared panels and direct gesture actions (c9f0600)
- open the native input method picker (ed56829)
- preserve expanded handle and panel controls (1cf6b20)
- keep panels open after outside touches (00b63fd)
- add independent color opacity controls (ade7175)
- separate gesture targets from volume popup requests (0bf5d72)
- add independent empty panel buttons (a11edca)
- expose screen-based touch length limits (9e3e271)
- shape the shared app mark into an N (a799493)
- show handle placement with notched badges (acb2de9)
- retain app and handle icons in headers (7d87ca5)
- define an event-driven soft hide session (1e9c1d1)
- control editing overlays with sticky preview switches (0003c27)
- reveal full-screen handles before activation (4314309)

## Fixed

- round customization highlights into half-squircles (e021154)
- apply handle visibility immediately (d26cb05)
- round the launch icon container (40932fb)
- keep configuration storage off the main thread (1239cd8)
- bind settings viewmodels to the current application (79d486e)
- let page scroll through mini previews (b052d9a)
- cancel panel clicks after drag movement (fb83956)
- ignore unused legacy popup flags (55ed09c)
- retain minimum length on tiny displays (b36ac02)
- stop the bounce when soft hide ends (27b1d75)
- reject empty direct gesture actions (0cc76f7)
- retain editor choices while configuration loads (6e6f25c)
- distinguish app activities from popup events (4ea4d31)
- report panel window dismissal reasons (b9a15b5)
- preserve panels across ambiguous window events (235ca63)

## Changed

- define clear handle and panel text (a0f6b04)
- classify update check failures (402db0b)
- remove the legacy runtime writer (adb6fac)

## Maintenance

- define commit scopes and message rules (c2aeda2)
- generate changelogs from scoped commits (cefeb75)
- prepare Nudge 0.1.2 (25b6ad1)
- prepare Nudge 0.1.3 (c84ca0a)
- require explicit release requests (7f02c30)
- keep the compatibility checklist local (54cc453)
- recommend HyperOS background autostart (2966580)
- finalize Nudge 0.1.2 (942146b)
- define app text writing rules (45fd487)
- verify handle and panel customization (b22e8c2)
- document handle and panel controls (229cfff)
- group artifacts by version (013382e)
- update the 0.2.0 preparation context (18b34c4)
- finalize Nudge 0.2.0 (d577022)
- verify panel offsets and screen bounds (319d261)
- verify panel and action persistence (ad14a52)
- verify volume flags and unlock sources (3c2312e)
- verify panel touch completion (64865ea)
- verify panel window cleanup (df95eaf)
- verify directional panel effects (292cce5)
- describe independent panels and gesture actions (66d36b6)
- update the app setup and feature guide (d9325fc)
- prepare Nudge 0.2.1 (3b0e746)
- remove obsolete customization instructions (cac94f3)
- describe the new handle and panel controls (28766af)
- record handle and panel changes under Unreleased (06c8b96)
- clarify configuration and timeout ranges (72213ee)
- verify sticky preview and empty button controls (c172b08)
- verify preview scrolling and drag cancellation (a88a134)
- verify native handle soft hide (d5b549c)
- verify combined gesture popup requests (9b36c6c)
- remove trailing action whitespace (f473906)
- bound both foreground metadata caches (f038c3b)
- retain panels during sound updates (89bcc4c)
- use independent picker button IDs (8ba830e)
- record panel dismissal findings (9a51440)
- link panel dismissal diagnostics (3d77e15)
- record unreleased panel dismissal fixes (9343f50)
