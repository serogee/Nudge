# Changelog

## Unreleased

### Changed

- Replace icon shape choices with one corner control for panels and Wheels. Use 0% for square icons and 100% for circles. Import older shapes as rounded squares and preserve circles. Backups use schema 26.

## 0.5.2 - 2026-10-24

### Added

- Set the inner and outer Wheel radii on one track with two draggable points. Press a point before dragging; touching elsewhere does not move it. Allow a **1 dp** inner radius, a **49 dp** smallest outer radius, and at least **48 dp** of band width. New wheels retain the 100 dp inner and 156 dp outer defaults. Preserve previous wheel sizes when loading older backups. (af6b2c8, 2c7ba2b)
- Choose **No animation**, **Smooth**, or **Spring** for Wheel highlight transitions. Smooth defaults to 140 ms and accepts 80–400 ms. Fade outlines with wedge and icon motion, and retarget transitions when selection changes. Keep selection boundaries independent of animation. (7284a7e)
- Set optional hover colors for Wheel icons, backgrounds, and highlight outlines. Keep current icon and outline colors by default, with a subtle background tint. Reuse focus transitions and restore normal or toggle colors when selection clears. Tint app artwork only when a custom hover icon color is enabled. (73e2339)

### Fixed

- Keep Wheel backgrounds and button containers concentric when rounding corners. Preserve the adjustable 0–48 dp corner setting and apply the corrected shape in previews and live wheels. (09c5722)
- Keep Wheels separate from panels in both orientations. Remove conversion choices, retain Fixed and Scrollable options for panels, and migrate old mixed active layouts to Wheels without deleting buttons or gesture links. Guard inactive legacy modes when enabling landscape overrides. (f4fb593, 5d23739)
- Require thumb dragging for settings value sliders. Ignore track taps and track swipes. Respond immediately when a drag reverses after reaching a limit. Keep keyboard controls, numeric entry, disabled behavior, and right-to-left support. (10e7ea9, fbf9c6b)
- Keep color picker dialogs stable with long titles. Support hexadecimal entry and preset selection. (e1e8e89)

### Changed

- Default the custom brightness popup to **Slide to adjust**, which starts at the current level and ignores stationary taps. Retain **Jump to touch** as a selectable option and preserve saved touch choices. The thumb-only rule applies to settings value sliders, not the brightness popup. (0350c3a)

### Maintenance

- Update the user guide and editor help for radius limits, hover colors, transitions, and the distinction between settings sliders and brightness touch modes.
- Make release tag discovery and isolated test cleanup work on this Linux runner. Keep release checks scoped to their disposable fixtures. Pass 29 changelog and 10 finalization checks. (8ee3a2e)

### Compatibility and known limits

- Backups now use **schema 25**. Older backups remain importable. Builds that support schema 24 or earlier cannot import the new format. Preserve stored radius widths, animation switches, toggle colors, shared links, and overflow buttons.
- Wheels still show up to six buttons. Radius, band width, expansion, and screen space determine capacity. Keep at least 48 dp touch targets; saved overflow remains editable.
- Hover means the highlighted button during Drag to pick. Hover appearance does not change an action's toggle state. The editor preview shows a highlighted button even when expansion is off.
- Preserve the original Wheel hold, drag, quick-release, later-swipe, and release-to-execute behavior. A tiny hollow center does not change the separately configured minimum selection distance.
- Pass 801 unit tests across 120 classes, debug lint with 0 errors and 14 warnings, and the 0.5.2-debug / code 18 build.
- Physical-device checks for Wheel shape and touch ownership, transition smoothness, native popup interaction, rotation, brightness behavior, and a signed update over 0.5.1 remain pending. Automated checks do not establish these results.

## 0.5.1 - 2026-10-24

### Added

- Use a hollow semicircle with separated wedge buttons for Wheels. Set the radius from **40–240 dp**. Keep up to six buttons, with smaller capacities when space or touch targets require them. Retain overflow buttons for editing and reordering. (3d3fc35)
- Expand the highlighted wedge, icon, and angular hit region. Shrink other wedges and icons. Keep the current selection until the finger crosses its expanded boundary, then transfer expansion to the new selection. (3d3fc35)
- Toggle smooth Wheel focus animations. Turning **Wheel animations** off applies shape changes immediately and suppresses Wheel window motion. Respect Android's disabled-animation setting. Keep logical selection independent of animation. (3d3fc35)
- Enable optional **Slider touch completion** for supported Xiaomi brightness controls. Require explicit consent before briefly pressing and releasing the identified native relative slider to save each measured position. Keep this device-local option off by default. (bbf3a3d)

### Fixed

- Keep native slider identity stable while its panel bounds move. Wait for delayed progress updates. Recheck the current control, bounds, and covering windows before touch completion. Reject invalid or covered targets. (bbf3a3d)
- Wait for an active native touch to end before cancellation restores brightness. Refresh an unchanged saved value when needed to clear a temporary native preview during restoration and recovery. (bbf3a3d)
- Diagnose unreadable, decreasing, and flat calibration samples without recording measurement values or screen text. Keep the saved profile when a run fails. (bbf3a3d)

### Maintenance

- Allow more time for slider test configuration to load from disk. Keep gesture timing assertions unchanged. (d05e0cf)
- Update Help, the README, and the user guide for compact Wheels, selection boundaries, animation controls, native slider consent, and the observed Redmi failure. (3d3fc35, e883a21, 926daae)

### Compatibility and known limits

- Backups use schema 22. Older backups remain importable. Schema-21 Wheels retain their radius and enable animation by default. Nudge 0.5.0 and earlier cannot import schema-22 backups.
- Keep the original Wheel drag, hold, quick-release, and swipe-sequence rules. Keep 17 automatic brightness positions and five manual points. Native slider touch consent and calibration data stay on the device.
- The normal handle service retains its existing permissions. Only the separate calibration service gains optional native slider touches. Screenshots remain disabled in both services.
- The new-style Redmi run changed temporary brightness while the saved setting stayed fixed. Native plugin inspection confirmed that saving requires release. The new touch-completion and restoration path still needs a physical-device recheck; the separate above-zero abort remains unconfirmed.
- Signed installation over 0.5.0, physical Wheel touch ownership and smoothness, native popups, final brightness mapping and restoration, and battery comparisons remain pending. Preserve all earlier release notes, artifacts, and tags.

## 0.5.0 - 2026-10-23

### Added

- Create reusable semicircular wheels in the new **Wheels** tab. Show up to six buttons facing inward from a left, right, or bottom handle. Smaller layouts can show fewer buttons. Keep overflow buttons saved, editable, and reorderable. (0a95ca5, 59069b4)
- Tap Wheel buttons normally or enable **Drag to pick** for a non-tap gesture. Configure selection distances, selection expansion, and selected size. Release a highlighted button to run its action once and close the Wheel. Use **Allow later swipes** and its quick-release window to continue a swipe sequence without picking. (0a95ca5, 59069b4)
- Measure 17 automatic brightness positions at 6.25% intervals. Keep manual calibration at five points and retain existing profiles. Dense curves accept repeated hardware levels and wait for stable readings. (0cbb3a7)
- Enable **Keep calibration service enabled** to reuse an idle connection. Select **Disable calibration service** to turn it off manually. Automatic disabling remains the default. The idle service clears event subscriptions and window-query flags. (e6baef8)

### Fixed

- Retain saved handle pauses after app and phone restarts, master-switch changes, and handle disable/re-enable. Restore them before attaching handles and include them in backups. (e0a2111)
- Use one direction for combined pause and visibility actions: pause and hide, or resume and show. Save both states together. Correct mismatches from separate controls while retaining visibility-only actions. (e0a2111)
- Apply the saved background corner radius to the Wheel fan. (76f4339)
- Keep Wheel touch targets at least 48 dp, prevent overlap during expansion, and clamp placement within safe screen bounds. Retain the selected button at a shared boundary until the finger crosses it. Show capacity and disable Add when the Wheel is full. (f91d972, 4ac0026, 88429b3, f6efb4b)
- Include non-interactive brightness containers and known slider wrappers during discovery. Match exact translated native brightness labels when new HyperOS sliders share an ID. Accept Xiaomi plugin controls only from the installed system app. Keep the main handle service permissions unchanged. (5a66d1c)
- Include bounded brightness discovery metadata in **Info → Copy compatibility details**. Exclude screen text, accessibility descriptions, and current brightness values. Reject unrelated controls and ambiguous matches. (5a66d1c)

### Maintenance

- Share original handle touch capture between sliders and Wheels. Cancel the original recognizer and its deadlines after capture. (88d31b0)
- Update Help and the user guide for Wheel controls, swipe timing, calibration density, service reuse, and observed Redmi behavior. (dc5ec91, 559615b)
- Verify highlighted Wheel cancellation through Back, screen-off, interruption, and service loss. (57f8ef5)

### Compatibility and known limits

- Backups use schema 21. Import older backups; earlier Nudge versions cannot import new exports. Keep five manual calibration points and existing device profiles.
- Retain saved pauses across restarts and master or Enable changes. Resume actions and deletion clear pauses. Older backups without pause selections start resumed.
- Old-style Redmi calibration and restoration passed. Final new-style calibration, 17-point sampling, enabled standby, physical Wheel touch ownership, pause restoration after a phone reboot, and update installation still need device checks. See the [reviewed release notes](docs/release-notes-0.5.0.md) for details.

## 0.4.6 - 2026-10-23

### Added

- Tap a settings slider's percentage or dp readout to enter a value. Save or keyboard Done applies the value within that slider's minimum and maximum. Cancel keeps the current value. Disabled controls remain disabled. (754f4e5)

### Fixed

- Wait up to 30 seconds for a supported native brightness control before recording or changing brightness. On Xiaomi, open Control center manually during calibration. Search System UI application windows and the active System UI root while rejecting other apps and duplicate roots. (c55e744)
- Show Xiaomi guidance when the Control panel action opens notifications. Keep gesture injection and screenshot capabilities disabled. (c55e744)
- Use a 75%-opaque background for action-picker settings badges. Keep the gear icon fully opaque and retain the action's touch target. (c78fbf5)

### Maintenance

- License Nudge under **GNU GPL version 3 or later** (`GPL-3.0-or-later`). Add the full license and project grant to APK assets. Document matching-source distribution and third-party notices. (3f100d4)
- Rebuild the README with the app logo, stack badges, contents, demo placeholders, features, privacy, development, and support. Condense the user guide and replace repeated release checklists with one development and release guide. Remove the old panel investigation write-up. (e6deefc)
- Keep `.plans/` local and untracked. Move the approved commit scopes to AGENTS.md so fresh clones retain the rules. Use the same directory pattern as the other ignore rules. (9f2b20a, dff8f1c)

## 0.4.5 - 2026-10-23

### Added

- Add optional automatic brightness calibration in Home through a separate temporary accessibility service. Measure five native slider positions, restore brightness and automatic mode before Save, and retain recovery controls after interruption. Keep manual calibration for unsupported phones. (68a1a8e)

### Fixed

- Accept zero as a native brightness calibration minimum in manual capture and saved profiles. Reject invalid or repeated samples. (bf3a55d)
- Keep completed measurements available after late service interruptions. Refresh an open brightness preview and active states after Save. (94b6c93)
- Accept known brightness control IDs from System UI plugin resources. Reject unrelated color and dimming sliders. Accept positions that are already selected without requiring another native slider action. (87edbe3)
- Place Slide to adjust below its volume or brightness option. Keep action and layout options below their parent controls. Replace nesting arrows with subtle left lines. (f93a1e8)

## 0.4.4 - 2026-10-23

### Added

- Choose **Any direction** or **Inward only** for ordinary swipes. Any direction measures displacement from the first touch. Moving back reduces that distance. This choice is independent of Swipe and hold. New and existing handles default to Inward only. (131f212)
- Find additional options through setting chevrons and action-picker gear badges. Small arrow indicators connect expanded controls to their parent setting. Gear badges share the picker background and remain inside the action's touch target. (f86fb2f)

### Fixed

- Move brightness calibration to **Home**, below the brightness slider controls. Capture instructions now point to Home. (f51d2b2)
- Prevent the end of an opening swipe from immediately changing volume or brightness. The popup opens first. **After hold** waits for a stationary hold, then permits adjustment without lifting your finger. Set **Adjustment hold time** from 100 to 1,000 ms; the default is 200 ms. Select **Immediate** for instant adjustment. (170e52a)

### Compatibility and known limits

- New backups use schema 20. Older backups remain importable. Earlier Nudge versions cannot import schema-20 backups.
- Existing swipe bindings with **Slide to adjust** now default to **After hold**. Each swipe binding saves its activation choice and hold time. Copies and backups retain both settings.
- Movement beyond an 8 dp radius restarts the adjustment hold, including horizontal or diagonal movement. Small jitter does not restart it. Releasing before activation leaves the slider value unchanged.
- When the hold ends, adjustment starts at the current system value and finger position. The first 8 dp of vertical movement is ignored. Further movement is proportional, and reversals respond immediately. A brief ready cue uses the swipe haptic switch and Android's haptic policy.
- **Hold** and **Swipe and hold** use their existing activation deadlines. They do not add this extra hold. An assigned button action still runs when the popup opens; the guard delays only slide adjustment.
- HyperOS can map brightness differently from the default Android curve. Use manual calibration in Home when percentages differ from the native slider.
- Install over 0.4.3 without uninstalling. Check retained bindings, calibration, accessibility, brightness access, backups, and Home shortcuts.
- Physical Redmi and HyperOS hold timing, native popup touch capture, brightness mapping, update installation, and battery comparisons remain pending.

### Automated preparation checks

- All 555 unit tests passed with no failures, errors, or skipped tests.
- Debug lint passed with 0 errors and 25 warnings. The debug build passed with version 0.4.4-debug, code 13.
- Release tools passed 29 changelog integration checks and 10 isolated finalization checks.
- The final signed release build, signer comparison, and installation over 0.4.3 run after the GitHub merge.

## 0.4.3 - 2026-10-22

### Added

- Set **Slide to adjust** for swipe stages, Hold, or Swipe and hold. Keep your finger down, then slide vertically to adjust media volume or brightness. Select exactly one popup. Tap does not offer this option. (6e079ee)
- Set separate volume and brightness travel distances for each handle. The default is 240 dp. Optional editor guides show the full distance and current value without changing system values. (6e079ee)
- Select **All handles**, **This handle**, or **Other handles** for Toggle handle visibility. Visibility stays saved for each handle. (a0dd097)
- Add **Toggle and hide handles** with separate pause/resume and hide/show scopes. Each group toggles independently. An open panel stays usable after its invoking handle is paused. (a0dd097)
- Set active button background and icon colors in each panel. Each effect has automatic and manual colors. Standalone icons use an active outline. There are no individual button color overrides. (5474efb)
- Calibrate brightness at 0%, 25%, 50%, 75%, and 100% of the native slider in Help. Calibration stays on this device and resets after an OS build change. (fe021c9)

### Fixed

- Map Return brightness percentages to perceptual slider positions. Restore previous brightness with its exact system value and mode. (fe021c9)
- Return immediately from a manually selected maximum when no previous value is available. The Maximum brightness button reflects the current manual maximum. (fe021c9)
- Recognize Swipe and hold by displacement from the start. Movement beyond the distance keeps its timer. Retreat below the distance resets it. Choose Any direction or Inward only; Any direction is the default for existing and new handles. (22fd33e)
- Remove the brightness and MIUI background comparison samples. Keep the live brightness preview and appearance controls. (1e2ff9c)
- Keep one handle drawing during window replacement to prevent overlapping translucent handles from appearing brighter when a panel opens. (6973ece)
- Keep swipe sequences reset after their panel closes or a Close panels action runs during continuous adjustment. (625a259)

### Compatibility and known limits

- New backups use schema 18. Older backups remain importable. Older Nudge versions cannot import schema-18 backups.
- Slide to adjust starts at the current value without an initial jump. Horizontal movement after activation does not affect the value. Release ends adjustment without running the action again.
- A continuous swipe stage disables Swipe and hold for that handle. Its saved assignment stays intact. Disable Slide to adjust on all swipe stages to use it again.
- Brightness stays open while held. Native media volume receives an unchanged-value popup request once per second while held. This stops when input ends. There is no idle keepalive.
- Cancellation, multitouch, rotation, unavailable handles, and service cleanup stop adjustment and discard pending writes.
- Existing global visibility hiding migrates to each handle. Pauses remain temporary; visibility remains saved.
- Brightness uses the Android perceptual curve by default. HyperOS can use a different mapping. Use calibration when the native slider differs.
- Install over 0.4.2 without uninstalling. Check retained settings, accessibility, brightness access, backups, and Home shortcuts.
- Physical Redmi and HyperOS touch capture, slider mapping, visible flicker, update installation, and battery comparisons remain pending.

### Maintenance

- Correct handle guide punctuation and check hold deadline, retreat, and recrossing boundaries. (2923f42, 9ea839e)

### Automated preparation checks

- All 533 unit tests passed with no failures, errors, or skipped tests.
- Debug lint passed with 0 errors and 25 warnings. The debug build passed with version 0.4.3-debug, code 12.
- Release tools passed 29 changelog integration checks and 10 isolated finalization checks.
- The final signed release build, signer comparison, and installation over 0.4.2 run after the GitHub merge. They are not completed by preparation checks.

## 0.4.2 - 2026-10-20

### Added

- group gesture selectors above settings (e1325ae)
- show button actions in three columns (a3e0bfa)
- strengthen maximum gesture haptics (09ce5e3)
- replace timing sliders with bounded inputs (776926f)
- animate visible handles during swipes (07fb8af)
- store brightness popup animation choices (9272a13)
- animate brightness popup transitions (fdcabed)
- strengthen handle badge letter (42c0d99)
- collapse detailed gesture settings (3095386)
- compact color controls and compare slider backgrounds (f7c7a26)
- add a master handle animation switch (72c4bec)

### Fixed

- restore regular panel transitions (92936a9)
- keep handles visible while raising windows (26b76e7)
- retain revealed handles during window handoff (6120680)

### Maintenance

- target the panel search input in its dialog (ef74b7d)
- record handle feedback and control updates (e4ab968)
- explain handle and brightness feedback (4e53441)
- record brightness popup updates (433e330)
- prepare Nudge 0.4.2 (1a883c2)
- record collapsed gesture settings (7c17af5)
- explain handle and color preview controls (d99c5fd)
- record handle and color controls (71fdf8b)

## 0.4.1 - 2026-10-19

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

## 0.4.0 - 2026-10-19

Changes since published `v0.3.1`. Version code increases from 8 to 9.

### Added

- Add Increase brightness and Decrease brightness actions with five-percent steps.
- Open a custom MIUI-style brightness slider from gestures, alone or with the native volume popup.
- Set the slider's side, position, width, height, timeout, colors, and opacity from Home. Preview it without changing brightness.
- Create Home screen shortcuts for button actions with custom names and icons. Validate saved actions after unlock.
- Add native Notifications and Control panel actions to panel buttons and direct gestures.
- Reveal the launch icon pills from their centers, then move and rotate them into the N mark.
- Add Maximum brightness and Minimum brightness to panel buttons and direct gesture actions.
- Restore the previous brightness and automatic mode on a second tap, unless brightness changed externally.
- Toggle All handles, This handle, or Other handles without changing saved Enable switches.
- Show active handle-toggle states and provide Resume handles above the handle list.

### Changed

- Show bold handle initials with a small inset pill beside the configured edge.
- Group Cancel and Save inside a pill-shaped control. Keep the editor open and restore rename after saving or cancelling.
- Confirm saves after storage completes and show Save/Cancel toasts.

### Fixed

- Keep the selected handle, panel, or button during reorder scrolling. Make edge scrolling gradual in both directions.
- Center the launch mark with padding. Keep its rounded container inside Android's splash icon safe area.
- Download update APKs through Android Download Manager instead of passing the direct file link to Chrome.
- Show download progress, retry and cancel controls, and retain the download ID after reopening Nudge.
- Check the APK package, version, and signing identity before offering installation. Android requires installation confirmation.

### Compatibility and known limits

- The application ID remains `com.modukey.app`. The final build uses the existing release signing key.
- Install over 0.3.1 without uninstalling to keep saved settings. Recheck accessibility after updating.
- New backups use schema 7. Older backups remain importable. Older Nudge versions cannot import schema-7 backups.
- Brightness actions require Modify system settings access. Nudge requests access when a brightness action needs it.
- The brightness slider is a custom accessibility overlay. Its default 64 by 240 dp size approximates the native volume control.
- The default slider side assumes the native volume popup is on the right. Adjust its side, size, and position for your build.
- Nudge cannot read HyperOS's private volume-panel dimensions or provide an identical native brightness popup.
- Brightness steps switch to manual mode. Min/max recovery values and handle pauses are temporary and are not included in backups.
- Home shortcuts require launcher confirmation and a connected accessibility service. The master gesture switch can remain off.
- App and settings shortcuts use normal system unlock. Cancelling unlock cancels the action.
- Backups include shortcut definitions and icon references. Importing does not add icons to the launcher.
- Notifications and Control panel request Android's system panels. Some phones use the same panel for both actions.
- Downloads use Android Download Manager. Installation requires Android confirmation and may require Install unknown apps access.
- A release APK updates the release app. It cannot update the separate debug installation.
- Automated checks passed: 380 unit/Robolectric tests, lint with zero errors and 22 warnings, and the 0.4.0 debug build.
- Release tooling passed 29 changelog checks and 10 finalization checks. Signed APK verification runs after the GitHub merge.
- Redmi/HyperOS rendering, launcher confirmation, APK installation over 0.3.1, accessibility recovery, and overnight battery comparisons remain untested.
- No physical-device result is claimed. Keep actual results in the ignored `plans/compatibility.md`.
- These changes add no periodic restart jobs, wake locks, separate foreground service, or continuous idle redraw loop.
- Download progress checks run only while Info is resumed and a download is active. Brightness observers stop when no longer needed.

## 0.3.1 - 2026-10-19

Changes since v0.3.0, the latest published release. Version code increases from 7 to 8.

### Release highlights

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

### Fixes

- Truncate long editor titles with an ellipsis. Keep Save, Cancel, and rename controls visible.
- Track handle windows before attachment completes. Remove pending and attached windows during replacement, reconnection, and rotation.
- Apply the selected editor orientation to on-phone guides. Landscape Bottom appears on the left of an upright phone.
- Keep runtime Bottom at the logical bottom in both landscape rotations.
- Rotate panel guide content and use the same safe insets in mini previews and on-phone guides.

### Compatibility and known limits

- Automated checks passed: 279 unit/Robolectric tests, lint with zero errors and 15 existing warnings, and the debug APK build.
- The application ID remains com.modukey.app. Use the existing signing key for the final release build and updates.
- Existing thickness percentages convert once using each handle's saved touch depth. Other settings keep their values. Missing hold-time fields use 500 ms defaults.
- Changing touch depth keeps dp thickness fixed. Drawing is limited to the touch area without changing the saved thickness.
- New backups use schema 5. Older backups remain importable; older Nudge versions cannot import schema-5 backups.
- Pending editor changes stay in memory. Save applies them to normal operation and storage. Cancel restores saved settings.
- Drafts survive rotation and temporary backgrounding. Closing the activity or process discards unsaved edits. Backups contain saved settings.
- The release script verifies the signed APK and compares its signer with the archived 0.3.0 APK when available.
- The attached Nudge-0.3.1.json records signed-build verification, the source commit, signing certificate, and APK checksum.
- Redmi/HyperOS, installation over 0.3.0, screenshot UI, system text sizes, navigation, and overnight battery checks remain pending.
- The reported intermittent duplicate handle was not reproduced on the Redmi. Automated tests cover window ownership and repeated rotation.
- Visible handles can appear in screenshots. Android provides no reliable pre-capture signal for every device screenshot method.
- This release adds no polling, wake locks, periodic jobs, or idle animation loop.

### Added

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

### Fixed

- base Nudge 0.3.0 notes on v0.2.1 (650adcc)
- use the separated pill app icon (a7e3991)
- strengthen handle icon initials (5c0309e)
- identify panel and direct gesture choices (259797f)
- center wrapped action names (57f074a)
- clarify preview and input method labels (7281b3e)
- retain pending handle window ownership (71fdcfb)
- share safe insets across editor previews (85c72e9)

### Maintenance

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
- include dp thickness in 0.3.1 notes (386775a)
- record authorized local release merges (ad5afea)
- explain local merge publication (6ca0d6c)

## 0.3.0 - 2026-10-18

Changes since v0.2.1, the latest published baseline.

### Release highlights

- Add full-screen soft hide. The first gesture reveals the handle with a brief bounce. Later gestures run assigned actions.
- Extend touch length to the usable short edge, with an option for the longer edge. Preserve saved values when rendering is limited.
- Control real editing overlays with sticky Preview switches. Keep temporary preview choices during activity recreation.
- Add independent empty panel buttons with container-only, blank, or slash appearances.
- Add an outside-touch option that passes touches through and keeps the panel open. Back and timeout dismissal remain available.
- Move panel search into a separate selection dialog. Select Show Volume Popup independently for panels and direct actions.
- Set button-background and icon opacity independently. Use one palette and automatic contrast for built-in symbols.
- Keep handle badges beside bold titles. Add orientation-aware notches, an edge-handle tab icon, and the shared N app mark.
- Make positive Follow handle Y offsets move panels upward. Preserve old physical placement and assignments through schema-4 migration.

### Fixes

- Allow page scrolling over display-only mini previews.
- Cancel panel button clicks after drag movement or multiple pointers. Retain valid taps and accessibility actions.
- Keep panels open for initial activity observations, stale events, and ambiguous popup windows. Close on confirmed app transitions.
- Observe required activity events before opening panels. Keep volume controls and sound updates from causing false app changes.
- Record bounded dismissal reasons, package context, event class, and panel age. Copy these details from Info after an incident.
- Report unexpected panel-window removal once and reset its swipe sequence.
- Preserve editor choices while settings load. Keep legacy popup flags limited to media volume steps.
- Keep empty buttons out of direct gesture actions. Retain minimum touch length on small displays and stop the bounce when soft hide ends.

### Compatibility and known limits

- Automated checks passed: 217 unit/Robolectric tests, lint with zero errors and seven warnings, and the signed release build.
- The verified signing certificate matches 0.2.1. Version code increases from 6 to 7. The application ID remains com.modukey.app.
- Panel dismissal regressions cover API 31 and 36. The cause of the reported Redmi incident still needs device diagnostics.
- Physical Redmi/HyperOS, popup coexistence, touch routing, unlock, Recents, battery comparisons, and signed upgrade checks remain pending.
- App and icon-pack images retain their colors. Automatic contrast applies to built-in symbols. Some controls lack individual reset icons.
- Unresolved activity events cannot confirm an app transition. Multi-window and HyperOS reporting still need device checks.

### Added

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

### Fixed

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

### Maintenance

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

## 0.2.1 - 2026-10-18

### Release highlights

- Create and share independent panels. Assign gestures to panels, volume, combined actions, or direct app, settings, sound, lock, and keyboard-picker actions.
- Add Android's native Input method picker with normal unlock, focused requests, stale-request validation, and bounded cleanup.
- Add optional panel auto-close, pause it during held input, and reset swipe progression on expiry. Keep direct-action sequence timing event-driven.
- Retain Follow handle offsets and custom percentages separately. Preserve portrait and landscape settings, Fixed/Scrollable layouts, and hidden overflow buttons.
- Add separate open/close effects, optional volume-popup flags, standalone icon shapes, icon packs, built-in icons, MIUI swatches, and automatic colors.
- Add Home/Handles/Panels/Info navigation, joined tabs, pinned handle titles, detailed permission status, drag grips, panel-card previews, and staged pickers.
- Add reset icons to common settings and optional HyperOS autostart navigation. Preserve clear app text, manual recovery guidance, and the existing Info controls.
- Preserve single-handle preferences and backups through schema-3 migration. Remap copied IDs, retain shared links, and disable imported handles for review.
- Retain lock-screen options, outside-touch policies, Back dismissal, one accessibility service, bounded icon caching, and versioned release folders.

### Compatibility and known limits

- Automated validation passed: 167 unit/Robolectric tests, lint with zero errors and 14 existing warnings, and the debug APK build.
- Physical Redmi/HyperOS, native keyboard and volume popups, touch delivery, unlock, Recents, battery, and signed installation/settings checks remain pending.
- The autostart shortcut depends on the build. App/pack images keep their colors; automatic contrast applies to built-in symbols.
- Icon-background visibility has no automatic container choice. Some controls lack individual reset icons. Handle badges use portrait placement, and Edit shares the bottom action row.

### Added

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

### Fixed

- round customization highlights into half-squircles (e021154)
- apply handle visibility immediately (d26cb05)
- round the launch icon container (40932fb)
- keep configuration storage off the main thread (1239cd8)
- bind settings viewmodels to the current application (79d486e)

### Changed

- define clear handle and panel text (a0f6b04)
- classify update check failures (402db0b)
- remove the legacy runtime writer (adb6fac)

### Maintenance

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

## 0.2.0 - 2026-10-17

### Added

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

### Fixed

- round customization highlights into half-squircles (e021154)
- apply handle visibility immediately (d26cb05)
- round the launch icon container (40932fb)
- keep configuration storage off the main thread (1239cd8)
- bind settings viewmodels to the current application (79d486e)

### Changed

- define clear handle and panel text (a0f6b04)
- classify update check failures (402db0b)
- remove the legacy runtime writer (adb6fac)

### Maintenance

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

## Unreleased

No pending changes. Reviewed development entries are included in the 0.2.1 notes above.

## 0.1.2 - 2026-10-17

### Added

- Set indicator thickness as a continuous percentage of touch depth. Existing thickness settings migrate once and stay proportional when resized. (634e444)
- Check GitHub versions automatically from Behavior & help, with APK and release-note links. Include pre-releases (testing) is saved and defaults off; checks run only when tapped. (9ae4350)

### Fixed

- Round customization guides into half-squircles with a flat side against the screen edge. (e021154)
- Apply Make handle visible immediately in both previews. Grey out and disable handle appearance controls while hidden, retaining saved values. (d26cb05)
- Use the same rounded icon container on launch and inside the app. (40932fb)

### Maintenance

- Recommend Background autostart during HyperOS setup and document menu variations. Battery restriction changes remain recovery steps for disconnections. (2966580)
- Generate changelogs from scoped commits and document commit conventions. (cefeb75, c2aeda2)
- Keep the compatibility checklist local at plans/compatibility.md, excluded from Git. (54cc453)

### Update and compatibility

- Version code 4. Install over 0.1.1 or the earlier local 0.1.2 build without uninstalling to preserve settings; releases use the same application ID and signing key.
- The testing preference starts off and survives customization reset. Update requests run off the main thread and report network or rate-limit failures.
- Debug verification: 53 JVM/Robolectric tests passed; lint reported 0 errors and 14 existing warnings. Release packaging runs signed-build and certificate checks, recorded in the generated build metadata.
- Redmi Note 12 / HyperOS 1 popup behavior, Recents recovery, battery use and installation over a previous release remain pending physical-device testing.

## 0.1.1 — 2026-10-17

- Continuous, non-touchable indicator and touch-area preview while Customize is visible, even with the master switch off. Normal visibility returns on navigation or backgrounding.
- Immediate shared settings state, display-frame coalescing, 150 ms save debounce and final-value flush. Failed saves keep edits visible with a retry action; field changes preserve unrelated settings.
- Center-based position percentages, stable resizing and grab-offset dragging. Indicator length is now a percentage of touch length; saved thickness/radius survive smaller touch areas.
- Default placement is inherited in landscape, including logical bottom. An optional landscape override retains separate placement values.
- Optional awake lock-screen availability, off by default. Screen-off/AOD operation remains disabled; the phone is never woken or unlocked.
- Separate enabled-permission and service-connection status, idempotent cleanup, interruption recovery, bounded local lifecycle/process-exit diagnostics and expanded HyperOS recovery guidance.
- Reuse the launcher icon for in-app branding.
- Preserve application ID, release signing identity and existing preferences, with one-time indicator-length migration. Version code 2.
- Redmi Recents behavior, lock-screen compatibility, battery measurements and installation over 0.1.0 remain pending physical-device testing.

## 0.1.0 — 2026-10-17

- Initial Nudge release: an invisible, configurable accessibility edge trigger for the native volume popup.
- Compose setup, live preview, gesture and appearance controls, landscape settings and app exclusions.
- HyperOS setup/recovery guidance, explicit connection status and session-local native-popup confirmation.
- Battery-conscious service: accessibility window events are disabled unless app exclusions require them; no polling, wake locks, periodic work or ongoing overlay animation.
- Local Git milestones, persistent preferences, signed APK release tooling and device verification checklist.
- Redmi Note 12 / HyperOS 1 compatibility remains pending physical-device testing; see docs/compatibility.md.
