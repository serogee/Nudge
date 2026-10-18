# Panel dismissal investigation

## Findings

The previous service closed a runtime panel when most window-state events used a different package. It did not check activity identity or event age. With no app exclusions, it started observing events only after opening a panel. The first observed package could therefore look like an app change.

Automated tests reproduced these false dismissals on API 31 and 36:

- Open a panel before the first activity observation. Deliver an event from the app already beneath it.
- Open a panel. Deliver a queued activity event created before opening.
- Adjust media volume in a panel. Deliver a window event from a separate vendor component.

These tests establish code defects. They do not establish which event caused the reported Redmi behavior. No phone was connected during this investigation.

Android window-state events also describe panes and dialogs. They do not guarantee an app transition. See the [AccessibilityEvent reference](https://developer.android.com/reference/android/view/accessibility/AccessibilityEvent) and Android's [Activity](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/app/Activity.java) and [Dialog](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/app/Dialog.java) event implementations.

## Changes

- Observe window-state events while enabled handles need panels, exclusions, or full-screen soft hide. Stop the subscription when those features are unused.
- Use the first credible activity event as the baseline. Do not dismiss a panel for baseline initialization.
- Resolve activity components with PackageManager. Cache at most 64 component results and 32 application results.
- Require a launcher entry or the current Home activity for external app packages. Ignore Nudge's own overlays and internal unlock activity.
- Ignore pane changes, system popups, unresolved components, and out-of-order events. Ignore events created before the current panel opened.
- Compare event timestamps with uptime, matching AccessibilityManager. Screen-off time does not change that comparison.
- Close runtime panels on a confirmed transition to another app. Reset the invoking handle's swipe sequence.
- Keep media controls, popup requests, sound observers, and service interruption from dismissing a panel.
- Retain the configured timeout. Pause it during held input. Restart it after interaction. Ordinary refreshes retain the deadline.
- Report unexpected runtime-window removal once. Keep preview disposal and normal exit cleanup silent.

Activity identity remains conservative. If Android does not expose a component, or an app reports a custom window class, that event cannot confirm a transition. Such an event leaves the existing baseline and panel unchanged. Multi-window and HyperOS activity reporting need device checks.

The service still requests no screen-content retrieval, gesture injection, or accessibility key interception. The classifier does not poll or schedule work. Metadata caches are bounded. Existing panel timeouts and short animations remain finite.

The earlier controls work also canceled button activation after drag movement. The complete regression run includes that behavior.

## Automated verification

The complete run passed 217 unit and Robolectric tests. Android lint reported zero errors and seven warnings. The debug APK built successfully. Service and window tests cover API 31 and 36.

The full run also found an existing keyboard-picker fixture that reused a direct-action button ID in a panel. The fixture now uses an independent ID and checks configuration round trips. No production storage rules changed.

Version remains 0.2.1, code 6. No release APK, tag, merge, push, or publication was made for this work.

## Local diagnostics

Each interactive panel dismissal records its reason, panel and handle IDs, panel age, previous package, incoming package, and event class. Package and class fields are available for app-change dismissals. Other dismissals use `unknown` for absent event fields.

Reasons include `app_changed`, `timeout`, `back`, `outside_touch`, `window_detached`, `gesture_replaced`, `handle_unavailable`, `panel_removed`, `editor_changed`, `action_completed`, and service or window failures.

Diagnostics remain in app-private storage. They contain no screen text or continuous app-switch history. The existing log retains at most 64 entries and 32 KiB. Writes use the existing conflated background writer. A dismissal can contain app package names; review copied details before sharing.

To collect a device result:

1. Record the Redmi model, Android version, HyperOS build, and navigation mode.
2. Note whether panel auto-close is enabled and its interval.
3. Open a panel from the launcher or browser.
4. Use volume buttons with Show Volume Popup off and on.
5. Use the phone's volume keys. Compare the result.
6. Repeat with media playing and with headphones connected.
7. Switch to another app. Confirm that the panel closes.
8. Test Back, each outside-touch option, held input, and timeout expiry.
9. After an unexpected close, open Info. Select Copy compatibility details.
10. Find the matching `Panel dismissed` entry. Keep the reason and event context with the observation.

Opening Nudge to copy details can itself cause an `app_changed` dismissal if a panel is still open. Use the earlier entry for the original incident.

Record actual results in the ignored `plans/compatibility.md`. Physical touch routing, native-popup coexistence, multi-window behavior, and battery comparisons remain pending until tested on the phone.
