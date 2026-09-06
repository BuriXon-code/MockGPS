# MockGPS

A minimal Android mock-location service controlled from Termux or ADB.

MockGPS has no visible activity and no launcher interface. It runs as a foreground service and injects test locations through Android's `gps` and `network` location providers.

The project is intentionally small and currently focuses on reliable location mocking rather than exposing a graphical interface.

## Features

* Android 15+.
* No visible activity.
* No launcher interface.
* Foreground location service.
* Android `gps` and `network` test providers.
* Last location stored inside the Android application.
* Automatic restoration after reboot.
* Small ongoing notification showing the current coordinates.
* Controlled from Termux with simple shell commands.
* GPL-3.0 licensed.

## Requirements

* Android 15 or newer.
* Developer options enabled.
* Termux, if you want to use the companion command-line tool.
* MockGPS selected as the system mock-location application.

## Installation

Build the debug APK:

```sh
./gradlew assembleDebug
```

The APK will be generated in:

```text
app/build/outputs/apk/debug/
```

Install it with ADB:

```sh
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or install the APK using any Android package installer.

## Required Android settings

MockGPS has no graphical interface, so its permissions must currently be configured through Android settings.

### 1. Allow location access

Open:

```text
Settings
→ Apps
→ MockGPS
→ Permissions
→ Location
```

Grant location access.

On Android versions that offer the choice, allow at least:

```text
Allow while using the app
```

### 2. Allow notifications

Open:

```text
Settings
→ Apps
→ MockGPS
→ Notifications
```

Enable notifications.

MockGPS uses a foreground service, which requires a foreground-service notification.

### 3. Select MockGPS as the mock-location application

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

Without this step Android will not allow MockGPS to act as the system mock-location provider.

## Using MockGPS from Termux

The companion command-line application is:

[termux-mockgps](https://github.com/BuriXon-code/termux-mockgps)

Install that tool and then use:

```sh
termux-mockgps start
```

MockGPS starts using the last location stored by the Android application.

If no location has been stored yet, MockGPS starts at:

```text
0.0, 0.0
```

Change the location with:

```sh
termux-mockgps set 50.06143 19.93658
```

Stop mocking:

```sh
termux-mockgps stop
```

Coordinates use the order:

```text
latitude longitude
```

Explicit coordinate options are also supported by `set`:

```sh
termux-mockgps set -lat:50.06143 -lon:19.93658
```

The `start` command does not accept coordinates. Use `set` to change the location.

## Using ADB directly

Start MockGPS using the previously stored location:

```sh
adb shell am startservice \
	-n dev.burixon.mockgps/.MockLocationService \
	--es command on
```

Change the stored location:

```sh
adb shell am broadcast \
	-n dev.burixon.mockgps/.MockGpsReceiver \
	--es command set \
	--es lat 50.06143 \
	--es lon 19.93658
```

Stop MockGPS:

```sh
adb shell am broadcast \
	-n dev.burixon.mockgps/.MockGpsReceiver \
	--es command off
```

```sh
adb shell am stopservice \
	-n dev.burixon.mockgps/.MockLocationService
```

## Notification

While MockGPS is running, Android displays a small foreground-service notification:

```text
MockGPS
Location: 50.06143, 19.93658
```

The notification is intentionally configured as a low-importance ongoing notification so that it remains quiet and compact.

Android controls the exact presentation of ongoing notifications. On modern Android versions, users may still be able to dismiss some foreground-service notifications while the device is unlocked. This is system behavior and cannot be completely overridden by a normal third-party application.

The notification is removed when the foreground service actually stops.

## Providers

MockGPS creates test providers named:

```text
gps
network
```

Both providers receive the same mock coordinates.

The `gps` provider uses fine accuracy metadata, while the `network` provider uses coarse accuracy metadata.

MockGPS does not attempt to replace Google's fused location provider.

Applications using another location source may therefore behave differently.

## Reboot

When MockGPS is running, the application remembers that mocking should remain enabled.

After a device reboot, the application attempts to restore the foreground service and the last stored coordinates.

Some Android vendors apply additional background restrictions. Battery-management, autostart, or background-execution settings may therefore affect automatic restoration.

## Updating the application

The project is still under active development.

At this stage, an application update may leave an old service/process state behind on some Android devices.

After installing a new development build, you may need to:

1. Stop MockGPS.
2. Uninstall the previous APK completely.
3. Install the new APK.
4. Select MockGPS again under `Select mock location app`.
5. Re-grant required permissions.
6. Reboot the device.

A complete reinstall and reboot should currently be considered a normal troubleshooting step during development.

This limitation is expected to be removed as the service lifecycle becomes more mature.

## Troubleshooting

### MockGPS starts but the location does not change

Check that:

```text
Developer options
→ Select mock location app
→ MockGPS
```

is still selected.

Also verify that location access has been granted to MockGPS.

### The notification is missing

Check:

```text
Settings
→ Apps
→ MockGPS
→ Notifications
```

and enable notifications.

### MockGPS stops working after several start/stop cycles

During the current development stage, completely uninstall the APK, reinstall it, and reboot the device.

This is a known development-stage limitation.

## License

GPL-3.0.

See the `LICENSE` file for the full license text.
