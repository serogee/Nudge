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
- [ ] Actual invisible area draws nothing; highlight disappears after ten seconds.
- [ ] Tap and hold modes activate once on release; outward, parallel, slow, cancelled and multitouch gestures do not activate swipe mode.
- [ ] Touches outside the rectangle and Back/Home outside its reserved area work normally. Native volume controls remain fully touchable with the chosen placement.
- [ ] Left/right/bottom placement and drag preview agree with the configured touch area. Check cutout/navigation avoidance.
- [ ] Portrait/landscape settings restore correctly; landscape disabling hides the trigger.
- [ ] Excluded launcher apps hide the trigger and returning to another app restores it.
- [ ] Lock, screen off and unlock hide/restore correctly, with no duplicate windows.
- [ ] Revoke permission: service/window disappear; re-enable restores one window.
- [ ] Close settings, remove the app from Recents, clear Recents, reboot and retest. Record which HyperOS settings are required.
- [ ] Leave overnight, unlock and test; record battery settings and any service disconnection.
- [ ] Battery comparison: run matched overnight intervals with the accessibility service disabled, then with the default invisible trigger enabled and no exclusions. Match starting charge, duration, radios, screen use and other apps. Record total drain and Nudge's reported usage; investigate repeatable excess drain of more than one percentage point over an eight-hour baseline.
- [ ] Repeat the battery comparison with exclusions enabled if used. Confirm no app-owned wake locks, periodic jobs or sustained idle CPU activity. Treat coarse battery percentages as an estimate, not proof of zero overhead.
- [ ] Force-stop: no automatic recovery promised. Reopen and follow connection guidance.
- [ ] Install a higher-version APK signed with the same release key over the previous release: preserve customization and recover accessibility as needed.

## Automated coverage

JVM tests cover gesture direction/timing/cancellation, safe geometry across orientations, preferences surviving store reopen, idempotent window ownership, and requesting `ADJUST_SAME` with only `FLAG_SHOW_UI`. Robolectric checks invisible/highlighted native view pixels. Automated results belong in build reports; they do not satisfy the device rows above.

## Scope of compatibility claims

Only the physically tested model, ROM build and configuration may be marked verified. Record failures explicitly. If HyperOS suppresses the native popup, leave that check failed; the app does not substitute a custom panel or change volume as a workaround. Additional Xiaomi permissions should only be investigated based on an observed failure.
