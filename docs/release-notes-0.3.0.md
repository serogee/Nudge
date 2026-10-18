# Nudge 0.3.0

Changes since v0.2.1, the latest published baseline.

## Release highlights

- Add full-screen soft hide. The first gesture reveals the handle with a brief bounce. Later gestures run assigned actions.
- Extend touch length to the usable short edge, with an option for the longer edge. Preserve saved values when rendering is limited.
- Control real editing overlays with sticky Preview switches. Keep temporary preview choices during activity recreation.
- Add independent empty panel buttons with container-only, blank, or slash appearances.
- Add an outside-touch option that passes touches through and keeps the panel open. Back and timeout dismissal remain available.
- Move panel search into a separate selection dialog. Select Show Volume Popup independently for panels and direct actions.
- Set button-background and icon opacity independently. Use one palette and automatic contrast for built-in symbols.
- Keep handle badges beside bold titles. Add orientation-aware notches, an edge-handle tab icon, and the shared N app mark.
- Make positive Follow handle Y offsets move panels upward. Preserve old physical placement and assignments through schema-4 migration.

## Fixes

- Allow page scrolling over display-only mini previews.
- Cancel panel button clicks after drag movement or multiple pointers. Retain valid taps and accessibility actions.
- Keep panels open for initial activity observations, stale events, and ambiguous popup windows. Close on confirmed app transitions.
- Observe required activity events before opening panels. Keep volume controls and sound updates from causing false app changes.
- Record bounded dismissal reasons, package context, event class, and panel age. Copy these details from Info after an incident.
- Report unexpected panel-window removal once and reset its swipe sequence.
- Preserve editor choices while settings load. Keep legacy popup flags limited to media volume steps.
- Keep empty buttons out of direct gesture actions. Retain minimum touch length on small displays and stop the bounce when soft hide ends.

## Compatibility and known limits

- Automated checks passed: 217 unit/Robolectric tests, lint with zero errors and seven warnings, and the signed release build.
- The verified signing certificate matches 0.2.1. Version code increases from 6 to 7. The application ID remains com.modukey.app.
- Panel dismissal regressions cover API 31 and 36. The cause of the reported Redmi incident still needs device diagnostics.
- Physical Redmi/HyperOS, popup coexistence, touch routing, unlock, Recents, battery comparisons, and signed upgrade checks remain pending.
- App and icon-pack images retain their colors. Automatic contrast applies to built-in symbols. Some controls lack individual reset icons.
- Unresolved activity events cannot confirm an app transition. Multi-window and HyperOS reporting still need device checks.

## Added

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

## Maintenance

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
- record the Nudge 0.3.0 release workflow (42c7697)
- prepare Nudge 0.3.0 (1419b37)
- describe the post-merge packaging steps (faa69e8)
