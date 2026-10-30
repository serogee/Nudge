# Handles and panels

This guide describes the current source. For features in a published APK, use its [release notes](../CHANGELOG.md).

## Setup and troubleshooting

1. Install Nudge and open Home.
2. Turn on **Nudge accessibility**. Enable **Nudge handles** under Accessibility → Downloaded apps.
3. If Android blocks access, open Nudge's App info → ⋮ → **Allow restricted settings**, then try again.
4. Enable a handle in **Handles** and **Enable Gestures** on Home.
5. Use **Test volume popup**. Then try the handle from another app.

On MIUI or HyperOS, menu names vary. Enable **Background autostart** if needed. If the service still disconnects, try **Battery saver → No restrictions** or lock Nudge in Recents where supported. Nudge cannot verify these vendor settings. Force-stop and revoked accessibility access require manual recovery. Unlock the phone once after a restart.

Brightness actions need **Modify system settings** access. Some sound actions need **Do Not Disturb** access. Nudge does not require root, device administrator, or the separate **Display over other apps** permission.

The **Nudge accessibility** switch on Home reflects the actual permission or live service connection. Turning it on opens Android settings; enable **Nudge handles** there. Turning it off disables the connected handles service and closes its overlays. It retains your settings and **Enable Gestures** choice. If Android lists the service as enabled but it is disconnected, turning it off opens settings for manual recovery. The separate brightness calibration service and other apps' services are unchanged.

**Notifications** and **Control panel** use Android's native accessibility actions. Redmi Note 12 / HyperOS 1.0 was reported to open notifications for both requests. The old style combines notifications and quick settings. The new style has a separate Control center with brightness and volume sliders. On the connected Redmi, native shell requests opened these as distinct panels. This does not verify the app's accessibility routing. Nudge retains Xiaomi opening guidance and does not inject a top-right swipe.

## Handles

Create, rename, order, duplicate, or delete handles in **Handles**. New and duplicated handles start disabled. The first handle has priority where touch areas overlap.

Each handle owns its touch area, visible appearance, gestures, exclusions, and lock-screen option. Turning off **Make handle visible** hides the bar while its touch area still receives gestures.

In **Appearance**, choose an edge and center position. Adjust touch length and depth separately from visible handle length and thickness. A smaller touch area limits drawing while retaining saved appearance values. Portrait and optional landscape overrides retain separate placement.

**Enable longer touch length** extends the size limit to the longer screen edge. Turning it off retains the saved value. **Soft hide in full screen** hides the bar after its configured delay; the first valid gesture reveals it without running an action. Later gestures run normally.

App exclusions and lock-screen use are configured per handle. Lock-screen use defaults off. App and settings actions request Android's normal unlock step. Nudge does not wake the screen or operate on always-on display.

### Save, preview, and exact values

Handle and panel editors keep a draft. The top **Save** applies it; **Cancel** restores saved settings. Save inside a button or gesture dialog updates the draft. Pending edits survive activity recreation and temporary backgrounding, but not process death.

The **Visual Preview (Non-interactive)** switch shows an editing guide on the phone. Mini previews and phone guides do not run actions or capture touches. Normal handle activation resumes when editing ends.

Press a settings slider's thumb, then drag to adjust. Tapping or swiping elsewhere on the track does not change the value. Dragging back from a limit responds immediately.

Tap a slider's percentage or dp readout to enter a number. **Save** or keyboard **Done** applies it within the slider's limits. **Cancel** keeps the value. Enter 50 for 50%. Disabled controls cannot be edited. A reset icon restores that setting's default.

Chevrons mark additional options. Dependent controls appear below their parent, grouped by a left line. Action-picker gear badges mark action-specific options. Their backgrounds use 75% opacity; the gear stays fully opaque.

In **Info → Interface**, turn off **Additional option info** to hide descriptions below options throughout the app. The setting starts on. Labels, current values, service status, errors, progress, and Help content stay visible. Invalid duration entries still show their allowed range. The choice stays saved across restarts and JSON backups. Adding copies from a backup keeps the current choice; replacing the configuration uses the imported choice.

## Gestures

Each assignment can run a direct action, open a panel or wheel, or do neither. **Show Volume Popup** and **Show brightness slider** can accompany the assignment.

**No action** disables a gesture when both popup options are off. Disabled gestures do not vibrate, run actions, or reset the swipe sequence. Turn on either popup option to use it without an action.

When every gesture is disabled, the visible handle remains, and its touch area lets touches pass through. A handle with active gestures still captures the initial touch to identify them. Unassigned gesture types do not activate.

| Gesture | Behavior |
| --- | --- |
| Tap | Release without significant movement. |
| Hold | Stay still for the selected time. Run at the deadline or after release. |
| Swipe 1, 2, and 3 | Run when the distance is reached. Further swipes advance through assigned stages. |
| Swipe and hold | Reach its distance within the swipe time, then remain beyond it for the hold time. |

Ordinary swipes default to **Inward only**. **Any direction** measures straight-line displacement from the first touch. Moving back reduces it; the traveled path is not accumulated.

Swipe and hold has its own direction, distance, swipe time, and hold time. Its default direction is **Any direction**. Movement beyond the threshold does not restart the hold. Returning below it resets the hold, with tolerance for small jitter.

Hold and Swipe and hold have separate execution settings. Each hold time accepts 300–3,000 ms and defaults to 500 ms. Haptic feedback and strength are separate for each gesture type. With its haptic feedback on, a gesture gives one pulse when it opens a Wheel. Wheel item changes, reentry, and release do not repeat that pulse. Cancelled and multitouch gestures do not run actions.

A panel keeps its invoking handle's swipe sequence active. Closing it resets the sequence. After a direct action or popup alone, another swipe advances only within the configured **Swipe sequence timeout**.

### Continuous volume and brightness adjustment

In a gesture assignment, select exactly one popup and enable **Slide to adjust** below it. Tap does not offer this option.

For ordinary swipes, **Start adjustment → After hold** opens the popup first. Keep your finger still, then slide vertically without lifting it. **Adjustment hold time** accepts 100–1,000 ms and defaults to 200 ms. Movement beyond an 8 dp radius restarts the wait. Releasing before activation leaves the slider unchanged.

When ready, the current system value and finger position become the starting point. The first 8 dp of vertical movement is ignored. Further movement adjusts proportionally; reversals respond immediately. **Immediate** starts adjustment when the popup opens.

Hold and Swipe and hold use their existing activation deadlines, without the extra adjustment hold. A combined button action still runs when the popup opens; the guard delays only adjustment.

**Slider travel** sets separate full-range distances for volume and brightness, from 48–600 dp; both default to 240 dp. Volume follows the media stream's available steps. Brightness uses the current calibration. Values stop at their limits. Optional editor guides show the configured travel distance.

A continuous swipe disables Swipe and hold for that handle while retaining its assignment. Releasing ends adjustment. Cancellation, a second finger, rotation, screen-off, or service loss stops it.

## Independent panels

Create panels in **Panels**, then assign them to handles. Several handles can open the same panel. Each panel owns its buttons, appearance, layout, position, animations, and landscape settings.

| Direction | Fixed layout | Scrollable layout |
| --- | --- | --- |
| Vertical | Columns and maximum rows | Columns and panel height |
| Horizontal | Rows and maximum columns | Rows and panel width |

Fixed panels show only buttons that fit their configured capacity and available screen space. Hidden overflow buttons remain editable. Reorder them into the visible range or choose **Scrollable**.

**Follow handle** places the panel beside its invoking handle. X/Y offsets adjust it. Turn Follow handle off to use absolute center percentages. A selected preview handle affects only the editor preview.

Opening and closing animations are independent. Disabled Android animations skip them. Dragging a button beyond touch slop cancels its click so scrolling does not run an action.

### Closing panels

**Outside touch** offers three choices:

- **Close and block touch** closes on release and consumes the outside gesture.
- **Close and pass touch through** closes and lets Android deliver the touch beneath.
- **Pass touch through and keep open** retains the panel while the touch reaches the window beneath.

Nudge does not replay or inject touches. Back closes the panel. The optional **Close automatically** timeout accepts 1–60 seconds and defaults off. Held input pauses the timer; interaction restarts it.

A confirmed change to another app closes the panel. Native volume popups and unrelated window events should not close it. Screen-off and unavailable saved handles also close runtime panels. A panel stays usable after its handle is paused.

## Wheels

Create wheels in **Wheels**, then assign them to handle gestures. Wheels and panels stay separate and cannot be converted into each other. They share actions, icons, colors, and placement controls.

A Wheel layout shows a hollow semicircle facing inward from its handle. Each button uses a separated wedge container. App icons keep their normal loading and icon-pack support. Side handles order buttons from top to bottom. Bottom handles order them from left to right.

Each wheel shows **up to six buttons**. Inner radius, outer radius, expansion, and screen space can reduce this capacity. With **Autofit** on, Add button checks whether fitted sizes can make room. It is unavailable when the screen cannot fit another button or six buttons are saved. Overflow buttons remain saved, editable, and reorderable.

**Autofit** is beside **Add button** and defaults on for new wheels. Adding or removing a button fits the active layouts to their screen space. Autofit uses **Selected size**, which defaults to **125%**, to calculate the smallest usable outer radius. Each button keeps a 48 dp touch target. The inner radius stays at least **32 dp** and can increase to reduce the outside size. Autofit keeps the selected percentage when space is insufficient. Add button is unavailable when another button cannot fit. Autofit sets **Icon position** to **55%**. Turn Autofit off to retain the fitted sizes. Manual radius changes turn it off automatically for that orientation. Selected size changes keep Autofit on and recalculate the radii for that orientation. Position, color, corner, and animation changes keep it on. Older wheels retain explicit saved sizes until edited or fitted again.

**Wheel radii** uses one track with two points. Press **Inner radius** or **Outer radius**, then drag that point. Pressing the track does not move either point. The inner radius sets the hollow center; the outer radius sets the unselected edge. New wheels start at **32 dp inside and 88 dp outside** before Autofit adjusts them. The shared manual track covers 1–336 dp, with at least 48 dp between the bounds. Manual sizing still permits a 1 dp hole and a 49 dp outer edge. Screen limits can reduce the rendered size or button capacity. Older wheels retain their previous radius and band width until edited.

**Item gap** accepts **0–24 dp** and defaults to **4 dp**. Shared edges stay parallel, so the empty space has the same width along the button edges. Zero joins adjacent edges. Gaps stay constant during highlighting and are saved per orientation. Changing the gap refits the edited orientation when Autofit is on. Large gaps can reduce capacity when sizing manually.

**Corner radius** rounds the outer joins independently from the inner joins. A small inner arc no longer removes the outer rounding. Each corner remains limited by the space in its own arc and the band.

**Wheel hitbox** is in **Layout**, below the radii and gap controls. It controls **Drag to pick** and retains separate portrait and landscape choices. Normal taps continue to use the visible buttons.

- **Automatic** is the default. With Autofit on, selection starts at **32 dp** and ends at the current logical button edge, including highlight expansion. Selection can reach inside the larger visual hole of a compact wheel. With Autofit off, use the gesture's minimum and maximum distances and the limited inward margin.
- **Use Button Size** follows each logical button's inner and outer radii, including highlight expansion. It has no extra inward margin. Gesture distance limits are not applied.
- **Manual** starts with the effective unhighlighted bounds from the previous mode and the selected preview handle. It then uses independent inner and outer radii. Drag either point on the shared track, or tap its readout for numeric entry. The track covers **0–600 dp**, with at least **8 dp** between radii. Reset uses **32 dp** inside and **240 dp** outside. These bounds can extend into the hole or beyond the visible wheel. The exact center has no direction and does not select a button.

Entering Manual from Automatic or Use Button Size replaces the previous manual values with the current effective bounds. Manual radial bounds stay fixed during highlighting; angular selection regions still expand as configured. Hitbox edits preserve visible radii, saved gesture distances, and Autofit. Adjusting a manual bound briefly colors the hitbox over the Wheel for **two seconds after the last change**. The reference appears in the editor preview and the live preview when accessibility preview is available. It does not receive touches. Cancel, editor exit, orientation changes, or switching modes clears it.

**Adapt icon size** defaults on. It sizes artwork from the wheel band and fits the whole square within the rounded wedge, including separator gaps and padding. Icons stay separate from their touch targets. A small icon does not reduce the 48 dp touch target or Wheel capacity.

Turn Adapt icon size off to show **Icon size**, a preferred size from **12–128 dp**, defaulting to **32 dp**. Available space can reduce the rendered size. Turning adaptation on retains your manual value. Highlight expansion can grow icons while keeping them inside the wedge.

**Icon position** is available in both modes and defaults and resets to **55%**, slightly outside the middle of the band. Autofit also applies 55% when it recalculates. Zero percent is the nearest safe inner position; 100% is the nearest safe outer position. The endpoints keep room for visible artwork. Automatic sizing responds to the available width and depth as icons move. Very narrow areas can produce small icons. These controls use thumb dragging and numeric entry. They preserve Wheel Autofit and retain separate portrait and landscape values. Explicit saved positions remain until edited or fitted again.

**Expand selection** defaults on. **Selected size** accepts 100–150% and defaults and resets to **125%**. Autofit uses the selected percentage to calculate the radii. With Autofit off, changing Selected size keeps the manual radii. Saved custom percentages remain unchanged. The highlighted wedge grows in angle and depth. Its icon grows too. Other wedges and icons shrink, while neighboring angular selection regions give space to the highlighted wedge. The hollow center remains empty.

The current selection keeps its expanded hit region until your finger crosses its boundary. The new selection receives the expansion immediately. **Wheel animations** defaults on. **Highlight transition** offers **No animation**, **Smooth**, and **Spring**. Smooth defaults to 140 ms and accepts 80–400 ms. The outline, wedge, and icon transition together, and a new selection interrupts the current motion. No animation applies highlight and shape changes immediately. Transitions also work when expansion is off. Animation does not move the logical selection boundaries or select an action under a stationary finger. Turn the option off for immediate shape changes and no Wheel window motion. Android's disabled-animation setting is also respected. Toggle action colors keep their usual meaning.

### Hover colors

In a Wheel editor, open **Appearance → Wheel hover colors**. Hover follows the button highlighted during Drag to pick. By default, the icon and highlight outline keep their existing colors; only the wedge background changes slightly.

Enable **Custom hover icon color**, **Custom hover background color**, or **Custom hover highlight color** independently to choose their colors. Hover icon colors also tint app artwork while highlighted. Normal artwork returns when selection clears. Existing opacity controls still apply.

Turn **Change colors on hover** off to keep the original icon and background colors and the normal theme highlight. Custom choices remain saved. Hover colors use the selected highlight transition, including No animation. The editor preview shows a highlighted button even when expansion is off. Hover colors do not change a button's saved toggle state; its active colors return after hover ends. Panels do not offer these controls.

### Drag to pick

Without **Drag to pick**, the wheel stays open for normal button taps. Enable this option below a Wheel gesture assignment to select without lifting. Tap does not offer it. Drag to pick and Slide to adjust cannot use the same assignment.

Keep touching after the wheel opens. Move more than 8 dp to highlight a button. Release to run that button and close the wheel. Release with nothing highlighted to close without running an action. The release position must still be inside the highlighted selection area.

Distances start at the visible wheel center. With **Automatic** hitboxes and Autofit on, selection runs from 32 dp to the current button edge. With Autofit off, **Minimum selection distance** accepts 16–160 dp and defaults to 32 dp. **Maximum selection distance** accepts 48–600 dp and defaults to 240 dp. The maximum must exceed the minimum. This unfitted mode can highlight up to 12 dp inside the hollow center, limited to 20% of its radius. **Use Button Size** and **Manual** use the wheel's Layout hitbox settings instead. Space behind the fan and distances outside the chosen bounds do not highlight actions. Move back into range to select again. Release is checked against the same bounds. Normal taps use the visible wedges and exclude the center and separator gaps.

For Hold and Swipe and hold, the wheel opens when the hold time ends. Their saved execution preferences remain unchanged. A drag wheel on an ordinary swipe disables Swipe and hold while retaining its assignment.

**Allow later swipes** defaults off. The first drag wheel with this option off disables later swipe stages. Their assignments remain saved. The next touch starts the sequence again.

When Allow later swipes is on, **Quick-release window** accepts 100–1,000 ms and defaults to 200 ms. Time starts when the wheel opens. Release before the deadline to close without running an action and preserve the next configured stage. The existing Swipe sequence timeout starts from that release. At or after the deadline, fresh movement is required before highlighting. A confirmed pick or later cancellation resets the sequence.

Landscape overrides retain their own Wheel dimensions and placement. A wheel stays a wheel in both orientations. Panels offer only Fixed and Scrollable layouts. Imported items that used a Wheel in either active orientation remain in Wheels, with both orientations using Wheel layouts.

Held input pauses automatic closure. Multitouch, interrupted input, Back, rotation, screen-off, unavailable configuration, or service loss cancels picking. Drag wheels open and close without movement animations so visible selection and touch positions stay aligned. Test physical touch ownership on your phone, especially when native popups accompany a wheel.

New backups use **schema 32** and include the three handle action icons in the built-in library. **Toggle and hide handles** uses a separate icon from **Toggle handle visibility**. Backups retain item gaps, both Wheel radii, hitbox modes and manual bounds, icon size and position choices, highlight transitions, hover colors, drag settings, shared links, and overflow buttons. Older wheels keep their saved sizes and animation choices. Missing gaps use 4 dp; missing icon positions use 55%. Missing transition settings use Smooth at 140 ms. Older backups remain importable. Builds that support only earlier schemas cannot import schema-32 backups. The temporary hitbox highlight is not saved.

## Actions and appearance

**Icon corners** applies to panel and Wheel icons and their button containers. Use 0% for a square, 100% for a circle, or a value between them for rounded corners. The default is 50%. Older circles keep their circular shape. Other old shapes keep their corner amount and use rounded squares.

Panel buttons and direct gestures can open apps or settings, control sound and brightness, lock the screen, open the keyboard chooser, close panels, request a native screenshot, or open notifications and quick settings.

The **Input Method** action opens Android's keyboard chooser. You select the keyboard. **Screenshot** asks Android to capture and save the screen; Nudge does not read the image. Visible handles may appear in screenshots.

Choose built-in icons, installed app icons, or supported icon packs. Missing apps or packs retain their references and use fallback icons. Standalone buttons omit the container. Empty buttons reserve a layout cell.

Saved app and icon-pack artwork is preloaded and cached locally. Reopening a button uses available artwork immediately while its package version is checked. A bounded disk cache supports later app starts. App or icon-pack updates refresh the cached artwork. Android can clear this cache, so the first load can still take time. Backups retain icon references rather than image files.

**Active background** and **Active icon** colors are shared panel appearance settings for supported toggles. Each supports automatic or selected colors. Existing opacity settings still apply. Image icons retain their artwork; standalone icons use an outline for the active background effect.

## Pause and visibility controls

**Pause handles** stops gesture input without changing saved Enable switches. Pauses survive app and phone restarts, master-switch changes, and handle disable/re-enable. A disabled handle keeps its saved pause until you resume it. Turn Pause handles off to clear all pauses, including those on disabled handles. Deleting a handle removes its saved pause.

**Hide visible handles** hides the bars while gestures remain available. Hiding survives restarts and retains appearance values. Editing guides remain available.

**Toggle handles** and **Toggle handle visibility** offer **All handles**, **This handle**, and **Other handles**. They target enabled handles. This handle means the one that invoked the action.

**Toggle handle visibility** uses a slashed eye. The button is active when all targeted handles are hidden. Like **Hide visible handles**, active means hidden. Visible, mixed, and empty target groups stay inactive.

**Toggle and hide handles** has separate pause/resume and hide/show target groups. Each can select None, All handles, This handle, or Other handles. Select at least one group. If any handle in the pause group is paused, the button resumes that group and shows the visibility group. Otherwise, it pauses and hides the selected groups. This corrects differences caused by separate controls. With no pause group, show the visibility group if any target is hidden. Otherwise, hide it.

The combined button is active when all handles in its pause group are paused. Showing or hiding handles separately does not change this state. With no pause group, the button is active when all handles in its visibility group are hidden. The button's accessibility description reports both states.

Combined pause and visibility changes save together. Backups retain saved pauses, including pauses on disabled handles. Older backups without pause selections start resumed. Adding copies creates new handles that start disabled and unpaused. Replacing the configuration retains its saved pauses while the master switch stays off for review.

Home shortcuts using This handle or Other handles require a selected handle. The Handles tab's visibility switch applies to every saved handle.

## Brightness controls

Home contains brightness popup placement, size, timeout, appearance, animation, and touch settings. **Slide to adjust** is the default. It starts at the current brightness and follows swipe travel; stationary taps do not change the level. Choose **Jump to touch** to set brightness at the touched position. Saved touch choices remain unchanged when loading older backups. The thumb-only rule applies to settings sliders, not the brightness popup. Android controls its own native volume popup behavior.

The custom popup approximates MIUI styling. It cannot read Xiaomi's private volume popup bounds, so select the volume side and adjust brightness placement on your phone.

**Maximum brightness** recognizes an already selected manual maximum. Pressing it while active restores an available previous value or immediately uses **Return brightness**. **Minimum brightness** becomes active after Nudge selects the minimum.

Each maximum/minimum action has **Use previous brightness** and its own **Return brightness**, initially 50%. A valid previous value includes its automatic mode. External changes and restarts discard the captured previous value. Return settings stay saved.

### Brightness percentages and calibration

Brightness percentages represent slider positions. Manufacturers can use different curves. On Home, calibration appears below the brightness slider controls. Manual calibration uses five positions: 0%, 25%, 50%, 75%, and 100%. Automatic calibration uses 17 positions at 6.25% intervals. Both produce an interpolated curve. Existing five-point profiles remain valid until you replace or reset them.

**Manual calibration:** turn off automatic brightness. Set each requested native slider position, return to Home, and select **Capture**. Zero is a valid minimum. Negative, repeated, and decreasing samples are rejected. **Reset calibration** restores the default Android curve.

**Automatic calibration:**

1. Select Automatic calibration and read the permission disclosure.
2. Allow **Modify system settings** access and enable the separate **Nudge brightness calibration** accessibility service.
3. Return to Home and select **Start calibration**.
4. On Xiaomi, open Control center manually and keep the brightness slider visible. With the new style, swipe from the top right. With the old style, expand quick settings. Other phones request Android's quick settings panel.
5. Wait for measurement and restoration. Review the samples, then select **Save** or discard them.

Enabling the service alone does not move a slider. Setup expires after two minutes. After Start, discovery waits up to 30 seconds without changing brightness or automatic mode. Once a supported control is found, measurement has a 60-second deadline. The screen becomes dim and bright during measurement. Adjacent automatic samples can share a hardware level. Decreasing or completely flat curves are rejected. Manual capture still rejects repeated levels.

The calibration service uses native brightness controls that expose a valid range and accept accessibility progress actions. It includes non-interactive layout containers so a generic slider can retain its brightness identity. It can also compare a generic slider's accessibility label with the exact translated brightness label from System UI. On Xiaomi, the installed system UI plugin can own the control. A user-installed package with the same plugin name is not accepted. Unrelated controls and ambiguous matches are rejected. The normal handle service still cannot retrieve window content or inject touch gestures. Neither service can take screenshots or record the screen.

**Slider touch completion** is a separate device-local option, off by default. Some Xiaomi Control center sliders treat accessibility progress changes as an unfinished drag. They change temporary display brightness but do not save the measured value. These controls require this option before measurement can start. Read and accept its disclosure. If requested, disable and re-enable the separate calibration service in Accessibility to grant its updated capability.

With this option on, Nudge briefly presses and releases only the identified native relative brightness slider after each progress change. A stationary touch keeps that slider's current value and delivers the release callback that saves it. Keep your hands off the screen during measurement. A covering window or invalid target blocks the touch. The normal handle service gains no touch or content access. The calibration service cannot use this option while idle. Cancellation waits for the finite native touch to release before restoring brightness. Restoration can briefly refresh an unchanged saved value to clear a leftover native preview.

If detection fails again, open **Info → Copy compatibility details** after the run. These details include a bounded discovery report: inspected node counts, resource IDs, class names, ownership, supported ranges, and progress-action availability. They exclude screen text, accessibility descriptions, and the current brightness value. This report can distinguish a missing brightness identifier from a slider that exposes no writable range.

Cancel, screen-off, lock, rotation, service loss, or unstable measurements stop the run. Nudge attempts to restore the original brightness and automatic mode before showing results. A failed run leaves the saved profile unchanged. By default, the calibration service disables itself when the run ends.

Enable **Keep calibration service enabled** to reuse its connection. The service remains idle between runs, with event subscriptions and window-query flags cleared. It does not inspect controls, run discovery timers, or resume measurement after a restart. Select **Automatic calibration**, then **Start calibration** for another run. Use **Disable calibration service** to turn it off manually. Turning the keep-enabled option off also disables an idle connection. Completed measurements remain available to save.

If restoration is interrupted, Home offers **Restore previous brightness** or **Keep current brightness**. Other Nudge brightness writes remain blocked until recovery is resolved. Measurement never resumes after process death.

Profiles, recovery records, and both calibration service preferences stay on the device, outside configuration exports and Android backup. An OS build fingerprint change resets the profile. More samples improve interpolation but do not add hardware brightness levels or prove an exact mapping at every position.

On Redmi Note 12 / HyperOS 1.0 with Android 14, an old-style five-point automatic run completed and was saved. Brightness and mode restoration passed an immediate comparison. A later new-style run found the slider and reached 100%, but the saved value stayed unchanged while temporary brightness reached maximum. Inspection of the installed system plugin confirmed that its progress callback always reports active dragging. The saving callback runs on touch release. The new touch-completion path has automated coverage and still needs a physical-device recheck. The user's separate above-zero abort was not confirmed; moving panel bounds no longer change a slider's identity. If the native slider is unsupported, use Manual calibration.

## Home screen shortcuts

In **Home → Home screen shortcuts**, select **Add shortcut**, configure an action, app, icon, and name, then confirm your launcher's pin request.

Default and Standalone icons have no added background. **With container** adds a rounded background. Imported artwork keeps its own colors and transparency. Artwork can include its own background. The previews use the same container choice. Use **Pin again** to update an existing shortcut's artwork. Your launcher can add an app badge near the icon's edge.

Most shortcuts require the connected Nudge handles service but can run with Enable Gestures off. **Toggle accessibility** also works while disconnected: it opens Android accessibility settings to enable or recover Nudge handles. While connected, it disables the handles service and closes its overlays. A locked phone must be unlocked before a launcher shortcut runs. Their icons are static. Removing a shortcut in Nudge disables its action; remove any remaining launcher icon separately.

Backups retain shortcut definitions. Import does not grant permissions or pin icons.

## Backups and updates

Use **Info → Backup** to export or import JSON. Backups include handles, panels, bindings, icon references, and shortcut definitions. They exclude icon-pack images, permissions, runtime sessions, and brightness calibration.

**Add handles and panels** creates disabled copies with fresh IDs and preserves shared links within the imported group. **Replace configuration** requires confirmation and leaves the master switch off. Older backups migrate on import; older app versions may reject newer backup formats.

In **Info → Updates**, select **Check for updates**. **Include pre-releases (testing)** defaults off. Checks and downloads start only when requested.

**Download APK** uses Android Download Manager. Nudge verifies the package, version, and signing identity before offering **Install update**. Android requires installation confirmation. Debug and release builds install separately; a release APK does not update the debug app.

Install updates over the existing app without uninstalling to retain settings.

## Diagnostics and device checks

For disconnections or unexpected panel closures, note the reproduction steps and select **Info → Copy compatibility details**. Panel dismissal entries include a reason and may include app package names. Opening Nudge can itself produce a later app-change entry; use the entry for the original incident. Review copied details before sharing.

Nudge has no scheduled update checks, wake locks, or idle redraw loop. All handles share one runtime service. Automatic calibration adds a separate opt-in service. An enabled standby connection does not poll controls.

Automated tests cannot establish native popup touch ownership, HyperOS control exposure, compositor behavior, update installation, or battery use. Record actual phone observations separately. See [development and releases](development.md#device-verification) for the device-profile command.
