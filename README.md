<h1 align="center">
  <img src=".github/assets/nudge-header.svg" alt="Nudge" width="310" height="80" />
</h1>

<p align="center">
  <a href="#development">
    <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&amp;logo=kotlin&amp;logoColor=white" alt="Kotlin" />
    <img src="https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&amp;logo=jetpackcompose&amp;logoColor=white" alt="Jetpack Compose" />
    <img src="https://img.shields.io/badge/Material_3-757575?style=for-the-badge&amp;logo=materialdesign&amp;logoColor=white" alt="Material 3" />
    <img src="https://img.shields.io/badge/Android_12%2B-3DDC84?style=for-the-badge&amp;logo=android&amp;logoColor=20251C" alt="Android 12 and later" />
    <img src="https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&amp;logo=gradle&amp;logoColor=white" alt="Gradle" />
  </a>
</p>

Nudge adds configurable edge handles to Android. Use gestures to open volume controls, adjust brightness, run actions, or open custom button panels.

**[Download the latest release](https://github.com/serogee/Nudge/releases/latest)** · [User guide](docs/handles-and-panels.md) · [Release history](CHANGELOG.md)

This README describes the current source. Check each release's notes for the features included in its APK.

## 📑 Contents

- [🚀 1. Quick start](#quick-start)
- [📱 2. Demo](#demo)
- [✨ 3. Features](#features)
- [🔒 4. Privacy](#privacy)
- [🛠️ 5. Development](#development)
- [📜 6. License](#license)
- [💬 7. Contact and support](#contact-and-support)

<a id="quick-start"></a>

## 🚀 1. Quick start

1. Download and install the APK from [GitHub Releases](https://github.com/serogee/Nudge/releases).
2. Open Nudge. Turn on **Nudge accessibility** on Home, then enable **Nudge handles** in Android settings.
3. Enable a handle in **Handles**, then turn on **Enable Gestures** on Home.
4. Swipe inward from the handle to open the native volume popup.

Nudge requires Android 12 or later. Brightness actions need **Modify system settings** access. Some sound actions need **Do Not Disturb** access.

HyperOS setup and native popup behavior vary by build. See [setup and troubleshooting](docs/handles-and-panels.md#setup-and-troubleshooting) if Android blocks accessibility or opens the wrong panel.

<a id="demo"></a>

## 📱 2. Demo

TODO: Replace these descriptions with real screenshots or short recordings.

| Capture | What to show |
| --- | --- |
| Edge gesture recording | Swipe from a handle over another app, open a custom panel, and run an action. |
| Panel screenshot | Show a named panel with app shortcuts, sound buttons, and an active toggle state. |
| Volume and brightness recording | Open a popup, hold, then adjust up and down without lifting your finger. |
| Customization screenshot | Show handle placement, panel appearance, and the non-interactive editing guide. |
| Calibration screenshot | Show Home's manual and automatic calibration options with a supported result or recovery state. |

<a id="features"></a>

## ✨ 3. Features

### Edge handles and gestures

- Create independent handles on the left, right, or bottom edge.
- Adjust touch areas, visible handle size, color, opacity, position, and landscape placement.
- Assign Tap, Hold, Swipe and hold, and up to three successive swipe actions.
- Configure distances, direction, timing, and haptic feedback. Ordinary swipes and Swipe and hold have separate direction settings.
- Exclude selected apps, use optional full-screen soft hide, and allow use on the awake lock screen.

### Custom panels and actions

- Create reusable panels and open the same panel from several handles.
- Create hollow semicircular wheels in the Wheels tab, with up to six wedge buttons. Set both radii on one track, choose a highlight transition, or use Drag to pick. Wheels stay separate from panels.
- Choose fixed or scrollable layouts, vertical or horizontal direction, placement, animations, and outside-touch behavior.
- Add app shortcuts, volume and sound controls, brightness controls, Lock screen, Input Method, Screenshot, and native panel actions.
- Customize colors, opacity, button containers, built-in icons, and supported installed icon packs.
- Fit Wheel icons automatically, or choose their preferred size and position between the inner and outer edges.
- Set panel-wide active background and icon colors for supported toggle actions.
- Pause handles, hide their visible bars, or configure separate toggle and hide groups.

### Volume and brightness

- Open Android's native volume popup and Nudge's custom brightness slider.
- Continue holding after a gesture to adjust volume or brightness with vertical movement.
- Set separate travel distances. Use an adjustable hold before swipe adjustment to prevent accidental changes.
- Use maximum, minimum, automatic, and stepped brightness actions, with configurable return brightness.
- Calibrate brightness manually or use optional automatic calibration when the native slider exposes a supported control.

### Editing, shortcuts, and backups

- Preview placement with non-interactive guides. Save or cancel handle and panel edits.
- Drag slider thumbs to adjust settings. Track taps do not seek. Tap percentage and dp readouts to enter exact values.
- Pin configured actions to the Home screen through your launcher.
- Use the Home accessibility switch or a Toggle accessibility shortcut to open setup or disable Nudge handles.
- Export and import JSON backups, including handles, panels, gesture bindings, and shortcut definitions.
- Check for updates when requested. Download an APK and review Android's installation prompt.

See the [user guide](docs/handles-and-panels.md) for setup, gesture behavior, calibration, backups, and known limits.

<a id="privacy"></a>

## 🔒 4. Privacy

- **No account, ads, or analytics.** Handles, panels, and controls work offline.
- **Updates are requested by you.** Update checks and APK downloads contact GitHub. Nudge does not upload settings or diagnostics.
- **Settings stay in app storage.** You choose where exported JSON backups are saved. Android may back up configuration through your system backup settings.
- **Diagnostics remain local.** Copied compatibility details can include app package names. Review them before sharing.
- **No screen recording.** The optional Screenshot action asks Android to capture and save an image. Nudge does not read that image. The normal handle service cannot inject touch gestures. The separate calibration service offers optional, disclosed presses on identified native brightness sliders to complete their releases.

### Accessibility and permissions

| Access | Purpose |
| --- | --- |
| Nudge handles accessibility service | Receive edge gestures, show overlays, run selected system actions, and use window metadata for app changes and exclusions. It cannot retrieve window content. |
| Optional Nudge brightness calibration service | Find a supported System UI brightness control and set its progress during a run you start. It has separate window-content access. |
| Modify system settings | Change system brightness. |
| Do Not Disturb access | Run supported sound-mode actions. |
| Install unknown apps | Open Android's installer for an APK you downloaded. Android still requires confirmation. |

The calibration service checks native control IDs and exact brightness labels. It does not read notification text, take screenshots, or record the screen. By default, it disables itself when a run ends or setup expires. You can keep it enabled and idle between runs, then disable it manually. It reads controls only during calibration. Profiles, recovery records, and both calibration service preferences are excluded from Android backup and JSON exports. See [calibration and recovery](docs/handles-and-panels.md#brightness-percentages-and-calibration).

<a id="development"></a>

## 🛠️ 5. Development

### Requirements

- Git and Android Studio, or the Android command-line tools.
- Android SDK Platform 36 and Build Tools 36.0.0.
- JDK 17 or a newer version supported by Gradle 9.1. The existing local build uses JDK 24.
- The included Gradle wrapper. A separate Gradle installation is not required.

The project uses Kotlin, Jetpack Compose, Material 3, Coroutines, and DataStore. Dependency versions are in [gradle/libs.versions.toml](gradle/libs.versions.toml). The Android Gradle plugin's [compatibility requirements](https://developer.android.com/build/releases/agp-9-0-0-release-notes#compatibility) cover the SDK, JDK, and Gradle versions.

### Build and run

```powershell
git clone https://github.com/serogee/Nudge.git
cd Nudge
.\gradlew.bat :app:assembleDebug
```

Before building, set `ANDROID_HOME` to the SDK directory, or open the project in Android Studio to create `local.properties`. Set `JAVA_HOME` to your JDK when using the command line.

Open the project in Android Studio, sync Gradle, and run `app` on an Android 12+ device or emulator. The command-line APK is `app/build/outputs/apk/debug/app-debug.apk`.

Debug builds use `com.modukey.app.debug`; release builds use `com.modukey.app`. They install separately.

### Checks

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

On macOS or Linux, replace `.\gradlew.bat` with `bash ./gradlew`. Release scripts use PowerShell and Windows Android tools.

See [development and releases](docs/development.md) for repository conventions, device checks, signing, and publication. Features under development belong in [Unreleased](CHANGELOG.md#unreleased); release notes are stored in [docs](docs).

<a id="license"></a>

## 📜 6. License

Copyright (C) 2026 serogee and Nudge contributors.

Nudge is free software. You can redistribute it and modify it under the GNU General Public License, as published by the Free Software Foundation, either version 3 of the License or, at your option, any later version.

See [LICENSE](LICENSE) for the full terms. SPDX identifier: `GPL-3.0-or-later`.

Nudge is distributed without any warranty. Third-party components retain their own licenses and notices.

<a id="contact-and-support"></a>

## 💬 7. Contact and support

For bug reports, feature requests, or questions, [open a GitHub issue](https://github.com/serogee/Nudge/issues).

For a device issue, include your Nudge version, phone model, Android version, MIUI or HyperOS build, navigation mode, and reproduction steps. Describe what you expected and what happened. You can copy compatibility details from **Info**; review them for private information before posting.
