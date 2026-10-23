# Automatic brightness calibration

Implement on `feat/automatic-brightness-calibration`. Keep development changes under Unreleased. Do not change release versions or tags.

## User flow

1. Keep manual calibration in Home below the brightness slider controls.
2. Add Automatic calibration with disclosure before requesting a separate temporary accessibility service.
3. Require Modify system settings access. The normal handle service keeps its current capabilities.
4. Enable the calibration service in Android, return to Home, and select Start. Enabling alone never starts measurement.
5. Open Control center and set its identified native brightness control to 0%, 25%, 50%, 75%, and 100% through ACTION_SET_PROGRESS.
6. Confirm native progress and stable manual brightness readings. Reject missing, ambiguous, refused, changing, or non-monotonic controls.
7. Restore the original brightness and automatic mode before showing results. Apply a complete profile only after Save.
8. Keep the existing profile on cancellation, failure, or discarded results. Offer manual calibration when unsupported.

## Boundaries and cleanup

- Inspect only System UI control metadata during a requested run. Do not read text, capture images, record the screen, inject coordinate taps, or send calibration data.
- The separate service has no gesture, screenshot, key-filter, or touch-exploration capability. Disable it on completion, cancellation, failure, service loss, or setup timeout.
- Show temporary progress and Cancel in an accessibility overlay while Control center is open.
- Bound setup and measurement time. Do not poll while idle or retry automatically.
- Cancel on screen-off, lock, interruption, service loss, or rotation. Remove callbacks and the overlay on every exit.
- Save the original brightness and mode in device-local no-backup storage before changing them. Keep recovery available if restoration fails.
- After process death, do not resume measurement. Ask the user to restore the saved brightness or keep the current value.
- Block other Nudge brightness actions during measurement or unresolved restoration.
- Store only five validated numeric samples and the device fingerprint. Never include calibration in configuration backups.

## Independent commits and checks

1. Completed in `bf3a55d`: accept a native minimum of zero in manual calibration and saved scales. Check interpolation, validation, and reload.
2. Completed: automatic flow, restoration, native-control adapter, and Home UI with coordinator, service, persistence, and interface checks.

Run relevant unit tests, debug lint, and a debug build. Keep OEM behavior unverified until checked on a device.

The initial feature checks passed 97 tests, debug lint with no errors, and a debug build. Checks cover API 31 and 36 service behavior, native progress actions, cancellation, restoration, and API 36 Home controls. These checks do not establish actual HyperOS support.

Android references: [progress action](https://developer.android.com/reference/android/view/accessibility/AccessibilityNodeInfo.AccessibilityAction#ACTION_SET_PROGRESS), [service capabilities](https://developer.android.com/reference/android/accessibilityservice/AccessibilityServiceInfo), and [service cleanup](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService#disableSelf()).
