# OS and app updates

Installing this dashboard does not change the Portal's firmware. This project does not root the device, flash an OS or promise that future Meta updates will preserve sideloading.

On the tested Android 10 Portal, the following **optional partial controls** disabled two OTA-related Android packages and set Android's automatic-update preference:

```bash
adb -s YOUR_SERIAL shell pm disable-user --user 0 com.facebook.aloha.alohaotasetup
adb -s YOUR_SERIAL shell pm disable-user --user 0 com.facebook.aloha.otaui
adb -s YOUR_SERIAL shell settings put global ota_disable_automatic_update 1
```

Record each package's current state and the original value of the setting before changing it. These commands are not run by the installer or any script in this public project.

**These do not fully block firmware updates.** The native `otad`/update-engine path still reported firmware updates enabled on the tested device after applying these controls. The Android flag may not control that native path. No reboot-persistence test or root-level firmware block is claimed here. Disabling updates also means fixes delivered through those update paths may be missed.

Read-only checks on compatible firmware:

```bash
adb -s YOUR_SERIAL shell pm list packages -d
adb -s YOUR_SERIAL shell settings get global ota_disable_automatic_update
adb -s YOUR_SERIAL shell dumpsys facebook.aloha.otaservice
```

The OTA diagnostic dump can contain device identifiers; redact it before sharing. A currently idle updater is not evidence that future updates are blocked.

To reverse the example when both packages were originally enabled and the setting originally absent:

```bash
adb -s YOUR_SERIAL shell pm enable --user 0 com.facebook.aloha.alohaotasetup
adb -s YOUR_SERIAL shell pm enable --user 0 com.facebook.aloha.otaui
adb -s YOUR_SERIAL shell settings delete global ota_disable_automatic_update
```

If your recorded initial state differs, restore that state instead. For dashboard updates, retain the matching signing key and increase version codes; use `adb install -r` to preserve app data when Android accepts the update. Keep a private configuration export and source checkpoint first.
