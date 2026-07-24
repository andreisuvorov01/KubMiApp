# Kiosk acceptance checklist

Run this checklist on every supported device model and Android/OEM version before deployment.

## Provisioning

- Factory-reset the device and install the release APK.
- Assign `com.example.kubmi/.receiver.DeviceAdminReceiver` as Device Owner.
- Open the app before public access and set a unique administrator password.
- Confirm `adb shell dpm list-owners` reports `com.example.kubmi`.
- Confirm `dumpsys activity` reports Lock Task state `LOCKED`, not `PINNED`.

## Escape resistance

- Press Back, Home, Recents, Menu, Settings, Search and Assistant.
- Test USB/Bluetooth keyboards: Alt+Tab, Alt+F4, Escape and Meta shortcuts.
- Long-press Power and confirm Android global actions are not shown.
- Try opening notifications, quick settings, another app, Play Store and Settings.
- Try uninstalling the app, clearing its data, adding a user and entering safe mode.

## Administrator maintenance

- Verify a wrong password does not open maintenance.
- Verify lockout begins after five failed attempts and increases after later failures.
- Test 1, 2 and 5 minute maintenance windows.
- Change the wall clock during maintenance and confirm it does not extend the window.
- Tap the notification action “Вернуться в kiosk” and confirm immediate relock.
- Let the timeout expire with Settings in front and confirm automatic relock.
- Kill the app process during maintenance and confirm the alarm/watchdog restores it.

## Recovery and lifecycle

- Reboot during normal kiosk operation.
- Reboot during maintenance and confirm maintenance is cancelled.
- Force-stop or crash the activity and confirm recovery.
- Update the APK in place and confirm Device Owner, HOME and Lock Task remain active.
- Disconnect network and power-cycle the device.

## OEM and physical controls

- Test all remote-control, panel-toolbar and hardware buttons.
- Test vendor-specific sidebars, floating tools, input-source menus and gestures.
- Document unavoidable hardware recovery/reflash paths and protect physical access.
