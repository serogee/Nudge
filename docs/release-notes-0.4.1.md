# Nudge 0.4.1

Changes since `v0.4.0`. Version code increases from 9 to 10.

## Added

- Set slider corner radius, track background, and Automatic brightness visibility from Home.
- Add Automatic brightness to panels, direct gestures, and Home shortcuts.
- Set separate haptic feedback and strength for Swipe, Swipe and hold, Hold, and Tap.
- Set separate Swipe and Swipe and hold timing and distance. Run hold actions at the deadline or after release.
- Add a small pull and return effect while a panel gesture is held.
- Set Pause handles and Hide visible handles above Add handle. Hide visible handles survives restarts without disabling gestures.
- Set a return brightness for Maximum brightness and Minimum brightness. Choose a saved previous level or a fixed return level.
- Choose Jump to touch or proportional Slide to adjust for the brightness slider.

## Changed

- Measure brightness slider position from the bottom edge.
- Store the new controls in backup schema 12. Preserve earlier values during migration and import.
- Put Toggle handles directly after Close panels in action pickers.

## Fixed

- Use Android's public brightness range. Keep Min and Max state only while Nudge owns the change.
- Clear brightness preset state after manual changes, including manual endpoint values.
- Keep completed Swipe and hold intervals through small drift and release jitter.
- Send haptic feedback through the vibrator with a supported strength.
- Keep the editor header height stable while Save and Cancel are visible.
- Keep handle editing reachable during reorder scrolling on small displays.

## Compatibility and known limits

- Install over 0.4.0 without uninstalling to retain settings. Recheck accessibility after updating.
- New backups use schema 12. Older backups remain importable. Older Nudge versions cannot import schema-12 backups.
- Brightness actions require Modify system settings access. Automatic brightness availability depends on the device.
- Pause handles is temporary and clears when Nudge restarts. Hide visible handles is saved and survives restarts.
- The brightness slider remains a custom accessibility overlay. It cannot match HyperOS private volume-panel dimensions exactly.
- Physical Redmi/HyperOS checks, installation over 0.4.0, accessibility recovery, and overnight battery comparisons remain pending.
- The finalizer reruns release checks, signs the APK, verifies its identity, and creates the tag after the GitHub merge.
- This release adds no polling, wake locks, foreground service, periodic job, or idle redraw loop.

## Commit log

### Added

- define brightness appearance and automatic mode controls (30c95ef)
- store slider appearance and bottom-based position (c9b59eb)
- add the automatic brightness action (2e4ac75)
- render brightness appearance and mode controls (9a45fc6)
- store independent gesture feedback and hold modes (f6d2269)
- separate gesture timing and feedback controls (7a60581)
- apply independent gesture vibration (9539b7b)
- run native hold actions at the selected time (d960af2)
- preview panel pulls and settle on release (bd46a12)
- store per-action brightness return values (b9566c5)
- store brightness slider touch behavior (088b86b)
- apply brightness returns and proportional slide input (e1e88ce)
- expose brightness return and touch controls (a090687)
- set explicit global pause states (f5b30f2)
- hide handle bars without removing input windows (4f2b3b4)
- add a handle visibility icon (e46b375)
- add global pause and visibility controls (b8031bc)

### Fixed

- scale brightness and retain preset ownership (a0f53c8)
- keep editor headers stable while saving (97426d5)
- stabilize hold gesture recognition (c4f1293)
- preserve brightness popup interactions (f9c962e)
- clear brightness presets after manual input (91c8299)
- keep handles reachable below global controls (49cc6ef)

### Changed

- remove unused shared gesture labels (2e411ad)

### Maintenance

- explain the automatic brightness action (b12e202)
- retain presets during synchronous status reads (d19b5b2)
- explain slider controls and hold feedback (5bd1cd3)
- record slider and gesture improvements (38de611)
- explain brightness return and touch behavior (f1bdab2)
- record brightness interaction improvements (b07b4bb)
- explain the global handle switches (5f80971)
- record global handle controls (136f69a)
- describe scrollable global handle controls (7c994a3)
- record small-screen handle navigation (7e5f92d)
