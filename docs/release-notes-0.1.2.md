# Nudge 0.1.2

## Added

- Set handle thickness as a percentage of the touch area's depth. Length and thickness stay proportional while resizing.
- Check for updates from Behavior & help. Nudge compares published GitHub versions automatically and offers APK and release-note links.
- Include pre-releases (testing) is a saved option, off by default. Off checks stable releases; on also checks test versions. Checks run only when you tap the button.

## Fixed

- Make handle visible now shows or hides the handle immediately in both previews. Appearance controls are grey and disabled while it is off, with their values retained.
- Customization guides have rounded half-squircle corners and a flat side against the screen edge.
- The launch screen and in-app icon use the same rounded container.

## Maintenance

- Recommend Background autostart in HyperOS setup, with alternative menu paths. Try battery setting changes if Nudge disconnects.
- Generate changelogs from scoped commits and keep the device compatibility checklist local.

## Updating

Version code **4**. Install `Nudge-0.1.2.apk` over 0.1.1 or the earlier local 0.1.2 build, without uninstalling, to keep settings. Recheck Accessibility after updating. Debug and release installations are separate.

Existing handle thickness converts once to its previous proportion of touch depth. The testing option starts off and survives customization reset. GitHub update checks need Internet access; the edge shortcut works offline. APK download and installation require your action.

## Validation

53 JVM/Robolectric tests passed in development; lint reported 0 errors and 14 existing warnings. Release packaging runs the signed build and certificate checks; the attached build metadata records those results.

Physical Redmi Note 12 / HyperOS 1 verification, including native popup behavior, Recents recovery, battery measurements and installation over an earlier APK, remains pending.
