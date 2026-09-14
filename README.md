# MockGPS 🗺️

![Banner](/banner.png)

MockGPS is an Android application for testing and simulating device location. It provides a foreground location-mocking service and exposes a dedicated **Android broadcast interface** for external control.

The broadcast interface is a central part of the project: MockGPS can be controlled from scripts, Termux, ADB, automation tools, or another Android application without requiring the MockGPS UI to be open.

Companion command-line project:

**[BuriXon-code/termux-mockgps](https://github.com/BuriXon-code/termux-mockgps)**

## Features

- Interactive Android UI with an OpenFreeMap/MapLibre map.
- Select a mock location directly on the map.
- Start and stop location mocking from the application.
- Stores the last selected mock location locally.
- Optional location drift for small, realistic movement around the selected point.
- Foreground location service using Android's `gps` and `network` test providers.
- Persistent state and automatic restoration after reboot when enabled.
- Optional external broadcast control.
- Companion POSIX shell command for Termux.
- Quiet ongoing foreground-service notification showing the current coordinates.
- GPL-3.0 licensed.
- **COMPLETELY FREE AND AD-FREE!** (this is probably the most important information)

## Compatibility

- **Minimum Android version:** Android 10 (API 29).
- **Target SDK:** 36.
- **Compile SDK:** 36.
- **Java:** 17.
- **Tested:** Android 16.

The project currently has only been tested on a limited number of devices. OEM-specific background restrictions, battery-management policies, and mock-location behavior can therefore vary between devices.

## Requirements

Before MockGPS can inject locations, Android must allow it to operate as the mock-location application.

You need:

- Android 10 or newer.
- Developer options enabled.
- MockGPS selected as the system mock-location application.
- Location permission granted to MockGPS.
- Notification permission enabled on Android versions that require it.

## Installation

### Official release

Check existing [releases](https://github.com/BuriXon-code/MockGPS/releases/), select any version and download the prepared APK installer.

### Build with Gradle Wrapper

Clone the repository and enter the project directory:

```sh
git clone https://github.com/BuriXon-code/MockGPS.git
cd MockGPS
```

Build a debug APK using the included Gradle Wrapper:

```sh
./gradlew assembleDebug
```

The APK is generated under:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install it with ADB:

```sh
adb install app/build/outputs/apk/debug/app-debug.apk
```

You can also install the APK using an Android package installer.

### Android Studio

Open the cloned `MockGPS` directory in [Android Studio](https://developer.android.com/studio).

Allow Android Studio to synchronize Gradle, then use **Run** to build and install the application on a connected device, or use the standard **Build** actions to generate an APK.

The project already contains its Gradle Wrapper and required project configuration. Android Studio still needs a compatible Android SDK installation.

## Android configuration

### 1. Grant location permission

Open:

```text
Settings
→ Apps
→ MockGPS
→ Permissions
→ Location
```

Grant location access.

### 2. Allow notifications

Open:

```text
Settings
→ Apps
→ MockGPS
→ Notifications
```

Enable notifications.

MockGPS runs its location injector as a foreground service and therefore uses an ongoing notification while the service is active.

### 3. Select the mock-location application

Open:

```text
Settings
→ Developer options
→ Select mock location app
```

Select:

```text
MockGPS
```

Without this setting Android will not accept the injected test locations.

## External broadcast control

**External broadcasts are one of the most important features of MockGPS.**

The application exposes the exported receiver:

```text
dev.burixon.mockgps.MockGpsReceiver
```

External tools can send commands directly to this receiver. The companion Termux project uses exactly this interface.

The broadcast command is supplied through the string extra:

```text
command
```

Supported commands are:

| Command | Purpose |
|---|---|
| `on` | Start mocking using the stored coordinates. |
| `off` | Stop mocking and disable persistent operation. |
| `set` | Change the stored mock coordinates. If mocking is active, the new location is applied automatically. |
| `drift` | Enable or disable location drift. |

### Coordinate extras

The `set` command accepts:

```text
lat
lon
```

Both values are decimal degrees.

Example:

```sh
adb shell am broadcast \
  -n dev.burixon.mockgps/.MockGpsReceiver \
  --es command set \
  --es lat 50.06143 \
  --es lon 19.93658
```

### Start

```sh
adb shell am broadcast \
  -n dev.burixon.mockgps/.MockGpsReceiver \
  --es command on
```

### Stop

```sh
adb shell am broadcast \
  -n dev.burixon.mockgps/.MockGpsReceiver \
  --es command off
```

### Drift

The `drift` command uses the string extra:

```text
drifting
```

Accepted values include `enable`, `on`, `true`, `disable`, `off`, and `false`.

Example:

```sh
adb shell am broadcast \
  -n dev.burixon.mockgps/.MockGpsReceiver \
  --es command drift \
  --es drifting enable
```

### Boot behavior

Boot behavior is controlled through the `boot` extra. Accepted values include `enable`, `on`, `disable`, and `off`.

Example:

```sh
adb shell am broadcast \
  -n dev.burixon.mockgps/.MockGpsReceiver \
  --es boot enable
```

MockGPS stores this preference and attempts to restore the foreground service after boot when enabled.

### Notification / toast behavior

External commands may provide:

```text
toast
```

The current companion Termux script uses the `toast` extra to control whether command-related toast messages are shown.

### Broadcast control switch

The application's **Broadcast commands** setting can disable handling of external broadcasts.

When this option is disabled, commands sent to `MockGpsReceiver` are ignored. Direct Android service operations are separate from this switch.

## Companion Termux command

The recommended command-line frontend is:

**[BuriXon-code/termux-mockgps](https://github.com/BuriXon-code/termux-mockgps)**

It wraps the broadcast/service interface into simple shell commands such as:

```sh
termux-mockgps start
termux-mockgps set 50.06143 19.93658
termux-mockgps stop
```

See the companion repository for installation and complete command documentation.

## Location providers

MockGPS creates Android test providers named:

```text
gps
network
```

Both providers receive the selected mock coordinates.

The `gps` provider uses fine-accuracy metadata, while the `network` provider uses coarse-accuracy metadata.

MockGPS does not attempt to replace Google's fused location provider. Applications using other location sources may therefore behave differently.

## Location drift

When enabled, location drift adds small movement around the selected location. The current implementation uses periodic updates and keeps the simulated movement within a small radius.

Drift can be enabled from the application UI or through the external `drift` broadcast command.

## Reboot and persistence

MockGPS stores:

- the last mock latitude and longitude,
- whether mocking should remain enabled,
- the boot-autostart preference,
- the external broadcast setting,
- the location-drift setting,
- the last real device location used by the UI.

When boot restoration is enabled, MockGPS attempts to start the foreground service automatically after device boot.

Some Android vendors may impose additional battery or background-execution restrictions.

## Notification

While mocking is active, Android displays a foreground-service notification containing the current location, for example:

```text
Running...
Location: 50.06143, 19.93658
```

The application uses a low-importance channel to keep the notification as unobtrusive as possible. Exact notification presentation remains controlled by Android and the device manufacturer.

## Updating during development

MockGPS is still under active development.

On some devices, repeated development installs or service restarts may leave stale application/service state behind. When a new development build behaves unexpectedly, a clean reinstall is a useful troubleshooting step:

```text
1. Stop MockGPS.
2. Uninstall the existing MockGPS installation.
3. Install the new APK.
4. Select MockGPS again as the mock-location app.
5. Re-grant required permissions.
6. Reboot the device.
```

## Troubleshooting

### MockGPS starts but applications do not see the mock location

Check:

```text
Developer options
→ Select mock location app
→ MockGPS
```

Also verify that location permission has been granted.

### External commands do nothing

Check the **Broadcast commands** switch inside MockGPS.

If it is disabled, `MockGpsReceiver` intentionally ignores external broadcast commands.

### Automatic restoration does not happen after reboot

Check the **Start on boot** setting in MockGPS.

OEM battery-management or autostart restrictions can also interfere with background execution.

### A new development build behaves strangely

Try a complete uninstall/reinstall followed by a reboot, as described above.

## Project structure

The Android application contains the main activity, the exported broadcast receiver, the foreground location service, and persistent application state handling under:

```text
app/src/main/java/dev/burixon/mockgps/
```

The application UI and map are implemented using Android Views and MapLibre Android SDK.

## Links

- **MockGPS:** https://github.com/BuriXon-code/MockGPS
- **termux-mockgps:** https://github.com/BuriXon-code/termux-mockgps
- **Website:** https://burixon.dev/MockGPS/
- **OpenFreeMap style:** https://tiles.openfreemap.org/styles/liberty
- **Donations:** https://buycoffee.to/burixon-code

## License

GPL-3.0.

See [`LICENSE`](LICENSE) for the full license text.

## Support
### Contact me:
For any issues, suggestions, or questions, reach out via:

- *Email:* support@burixon.dev
- *Contact form:* [Click here](https://burixon.dev/contact/)
- *Bug reports:* [Click here](https://burixon.dev/bugreport/#MockGPS)

### Support me:
If you find this script useful, consider supporting my work by making a donation:

[**Donations**](https://burixon.dev/donate/)

Your contributions help in developing new projects and improving existing tools!
