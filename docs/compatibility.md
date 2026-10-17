# Compatibility record

## Target device — pending

No physical Redmi Note 12 was connected when implementation began. Do not interpret a passing build, requested volume action or rendered preview as verified HyperOS behavior.

| Field | Recorded value |
| --- | --- |
| Product | Redmi Note 12 (4G/5G variant pending) |
| OS family | HyperOS 1 (reported by user) |
| Full HyperOS build / ROM region | Pending |
| Android release / API level | Pending |
| Navigation mode | Pending |
| Nudge APK version / SHA-256 | Copy from release metadata |
| Accessibility / autostart / battery settings | Pending |
| Tester / date | Pending |

Run `scripts/Device-Profile.ps1` to collect available device identifiers. In-app **Copy compatibility details** provides build and connection state without ADB. Copy any missing ROM fields from About phone.

## Acceptance checklist

Record PASS/FAIL, observed behavior, configuration and build for each check. These rows remain **NOT TESTED on Redmi** until completed.

- [ ] Sideloading and restricted-settings recovery allow the service to be enabled.
- [ ] Permission enabled, service connected and trigger attached match actual state.
- [ ] Native volume popup opens from Home's test button.
- [ ] Edge swipe opens the same popup from launcher, browser and a media app.
- [ ] Record volume before/after activation: no change, including minimum/maximum volume.
- [ ] Verify media, Bluetooth/headphones and active-call behavior (where available).
- [ ] Actual invisible area draws nothing outside Customize. The indicator and faint guide stay visible for more than ten seconds throughout Customize, even with the master off. Editing passes touches through and cannot activate volume.
- [ ] The guide has half-squircle inward corners and a flat side against the screen edge. Verify matching overlay/preview appearance for left, right and bottom placement in portrait and both landscape rotations.
- [ ] Navigate away, background, rotate and recreate the activity: editing visibility ends/restores correctly without changing saved visibility. Disconnected Accessibility leaves the in-app preview usable with an explanation.
- [ ] Sliders and dragging respond continuously, with no stale rollback while rapidly alternating controls. Final values survive restart; save errors retain edits and offer retry.
- [ ] Tap and hold modes activate once on release; outward, parallel, slow, cancelled and multitouch gestures do not activate swipe mode.
- [ ] Touches outside the rectangle and Back/Home outside its reserved area work normally. Native volume controls remain fully touchable with the chosen placement.
- [ ] Left/right/bottom placement and grab-offset drag preview agree with the real rectangle. Check cutouts/navigation insets and center positions at 0%, 50%, 100%. Resizing preserves center except corner clamping.
- [ ] Indicator length/thickness percentages stay constant when touch length/depth changes, on all three edges and after rotation. Zero opacity/length/thickness leaves the editing guide visible. Saved radius returns when the indicator is enlarged.
- [ ] Upgrade from 0.1.2 to 0.1.3: thickness is converted once from dp to its previous proportion of touch depth. Changing depth and restarting preserves the new percentage and all unrelated preferences.
- [ ] Bottom stays bottom in both landscape rotations with override off. Enable/change/disable/re-enable the override: values are retained. Landscape disabling still hides the normal trigger.
- [ ] Excluded launcher apps hide the trigger and returning to another app restores it.
- [ ] With lock-screen availability off, locking hides the trigger. With it on, the awake lock screen permits activation and ignores exclusions behind it, preserving normal indicator/landscape settings. Native volume action does not change volume or unlock the phone. Screen-off/AOD always hides it; verify first-unlock requirement after reboot.
- [ ] Revoke permission: service/window disappear; re-enable restores one window.
- [ ] Test ordinary activity closure, single Recents removal, clear-all, reboot and force-stop separately. Capture process-exit and crash evidence as described below, including any “malfunctioning” state. Record which HyperOS settings are required; do not infer crash vs system kill from the label alone.
- [ ] Leave overnight, unlock and test; record battery settings and any service disconnection.
- [ ] Battery comparison: run matched overnight intervals with the accessibility service disabled, then with the default invisible trigger enabled and no exclusions. Match starting charge, duration, radios, screen use and other apps. Record total drain and Nudge's reported usage; investigate repeatable excess drain of more than one percentage point over an eight-hour baseline.
- [ ] Repeat battery comparisons with exclusions and with lock-screen availability enabled. Confirm no app-owned wake locks, periodic jobs, continuously scheduled frames or sustained idle CPU activity. Treat coarse battery percentages as an estimate, not proof of zero overhead.
- [ ] Force-stop: no automatic recovery promised. Reopen and follow connection guidance.
- [ ] Install signed 0.1.0, save distinct settings, then install 0.1.1 using `adb install -r releases/Nudge-0.1.1.apk` (or open the APK normally), without uninstalling. Preserve position percentages, landscape availability/values, exclusions, visibility and gesture settings; indicator fraction equals the previous dp ratio. New flags start off. Recheck accessibility, activate and verify settings survive a restart.

## Automated coverage

JVM tests cover gesture cancellation/direction/timing; center geometry, corner clamping, density/insets, drag offsets and landscape inheritance; independent render constraints; migration and persistence across store reopen; slow/failed saves and rapid edits without stale rollback; session ownership; one-frame update coalescing and idle behavior; idempotent window cleanup; unchanged-volume API arguments. Robolectric checks invisible/editing pixels, editing activation suppression, appearance changes during gestures, and enabled-component status independent of service connection. Automated results do not satisfy the device rows above.

## Recents evidence procedure — pending on Redmi

1. Record the device profile, navigation mode and HyperOS autostart/battery/app-lock settings. Enable the shortcut and verify it from another app.
2. Copy compatibility details before testing. Keep `adb logcat -b crash -v time` running in a terminal while testing, or capture the crash buffer immediately afterward with `adb logcat -b crash -d -v time`. Save evidence locally under `.local/device-checks`.
3. Close the activity normally and test the shortcut. Separately swipe Nudge out of Recents, test again, then reopen and copy compatibility details **before** toggling Accessibility. Note whether permission is still enabled, whether the service is connected, and any recorded crash or user-requested exit. Android's user-requested exit reason can include Recents removal or force-stop; it is not proof of a crash.
4. Repeat clear-all and force-stop as separate cases, recovering manually between them. Repeat after Background autostart, No restrictions, and Security → Settings → Boost speed → Lock apps where available. A service can be killed without receiving a destruction callback.
5. Attach timestamps and exact observations to this record. Do not mark the reported malfunction fixed solely because cleanup tests pass. The implementation does not poll for its own death or attempt to resurrect a force-stopped package.

## Local 0.1.1 validation record

- Local development checks: 34 JVM/Robolectric tests passed; debug APK built; lint reported 0 errors and 14 tooling/dependency-version warnings. Release checks run again against the committed source.
- Physical device: none connected at implementation time. All device rows remain pending.
- The signed-release script verifies APK identity/version, signature and (when the archived 0.1.0 APK is available) matching signer identity. This is a prerequisite for an update, not an installation-over-old-version result.
- Matched idle battery measurements, OEM popup visibility and Recents reproduction require the phone.

## Scope of compatibility claims

Only the physically tested model, ROM build and configuration may be marked verified. Record failures explicitly. If HyperOS suppresses the native popup, leave that check failed; the app does not substitute a custom panel or change volume as a workaround. Additional Xiaomi permissions should only be investigated based on an observed failure.
