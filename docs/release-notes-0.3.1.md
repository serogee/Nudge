# Nudge 0.3.1

Changes since v0.3.0, the latest published release. Version code increases from 7 to 8.

## Release highlights

- Set visible handle thickness in dp, from 0 to 32 dp, with a 4 dp default. Length remains a percentage.
- Save or cancel all handle and panel edits from the header. Live previews show pending changes immediately.
- Choose Save, Discard, or Keep editing before leaving an editor. Names, gestures, buttons, and layout changes share its draft.
- Set Hold time and Swipe and hold time independently for each handle, from 300 to 3000 ms.
- Assign Close panels or Screenshot to a panel button or direct gesture. Close panels resets the owning swipe sequence.
- Request Android's screenshot UI after interactive Nudge panels are removed. Nudge does not read or store the image.
- Select a background-free Nudge mark from the built-in icon library. It uses the panel icon color and supports Standalone.
- Use the separated pill app icon throughout Nudge and ExtraBold initials in handle badges.
- Identify gesture choices with a four-square icon and Panel: or Action: prefixes. Panel options have clear gaps.
- Keep wrapped action names centered. Rename Input method picker to Input Method.
- Show icons in Excluded apps. Selected apps move to the top; clearing a selection restores its original order.
- Label both editor previews Visual Preview (Non-interactive).

## Fixes

- Truncate long editor titles with an ellipsis. Keep Save, Cancel, and rename controls visible.
- Track handle windows before attachment completes. Remove pending and attached windows during replacement, reconnection, and rotation.
- Apply the selected editor orientation to on-phone guides. Landscape Bottom appears on the left of an upright phone.
- Keep runtime Bottom at the logical bottom in both landscape rotations.
- Rotate panel guide content and use the same safe insets in mini previews and on-phone guides.

## Compatibility and known limits

- Automated checks passed: 279 unit/Robolectric tests, lint with zero errors and 15 existing warnings, and the debug APK build.
- The application ID remains com.modukey.app. Use the existing signing key for the final release build and updates.
- Existing thickness percentages convert once using each handle's saved touch depth. Other settings keep their values. Missing hold-time fields use 500 ms defaults.
- Changing touch depth keeps dp thickness fixed. Drawing is limited to the touch area without changing the saved thickness.
- New backups use schema 5. Older backups remain importable; older Nudge versions cannot import schema-5 backups.
- Pending editor changes stay in memory. Save applies them to normal operation and storage. Cancel restores saved settings.
- Drafts survive rotation and temporary backgrounding. Closing the activity or process discards unsaved edits. Backups contain saved settings.
- The release script verifies the signed APK and compares its signer with the archived 0.3.0 APK when available.
- Redmi/HyperOS, installation over 0.3.0, screenshot UI, system text sizes, navigation, and overnight battery checks remain pending.
- The reported intermittent duplicate handle was not reproduced on the Redmi. Automated tests cover window ownership and repeated rotation.
- Visible handles can appear in screenshots. Android provides no reliable pre-capture signal for every device screenshot method.
- This release adds no polling, wake locks, periodic jobs, or idle animation loop.

## Added

- persist independent hold times (3448963)
- recognize configurable hold intervals (d9dc37f)
- apply hold delays to native handle input (069775d)
- expose hold timing beside gesture assignments (674b33a)
- add the Nudge mark to the icon library (2f128d1)
- map editor geometry across rotations (375afb6)
- defer native screenshots until panels clear (9fe8ce1)
- define panel utility and preview labels (8ac2fac)
- add panel closing and screenshot actions (60851f8)
- rotate non-touchable panel guides (11a6578)
- connect panel utilities and rotated editing guides (152186e)
- show icons and pin excluded apps (bc6b48a)
- stage handle and panel edits (849ed18)
- preview pending editor changes (d6340c4)
- save or cancel handle and panel edits (a4439cf)
- use dp for visible handle thickness (f109c90)

## Fixed

- base Nudge 0.3.0 notes on v0.2.1 (650adcc)
- use the separated pill app icon (a7e3991)
- strengthen handle icon initials (5c0309e)
- identify panel and direct gesture choices (259797f)
- center wrapped action names (57f074a)
- clarify preview and input method labels (7281b3e)
- retain pending handle window ownership (71fdcfb)
- share safe insets across editor previews (85c72e9)

## Maintenance

- distinguish working tags from published releases (727f213)
- explain published and working tag baselines (12fe475)
- describe hold controls and icon choices (aa5150c)
- record pending gesture and icon changes (5341c3c)
- verify gesture timing and picker controls (f1f059f)
- verify panel utilities and rotation cleanup (8827850)
- verify utility choices and preview orientation (f082c36)
- explain panel utilities and rotated previews (3685f66)
- record panel utilities and rotation fixes (ceb5775)
- record excluded app list improvements (d395dc8)
- explain editor drafts and save controls (b6d3043)
- record editor save controls (42b16b0)
- mark Nudge 0.3.0 as published (2bd9b7f)
- explain the 0.3.1 publication steps (c2c2db7)
- prepare Nudge 0.3.1 (b2dfe37)
- explain fixed handle thickness (0a4fe5f)
- clarify the thickness follow-up (4f6540a)
