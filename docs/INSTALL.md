# Install on a compatible Portal

## Requirements

- A functioning, already configured Meta Portal with USB debugging available and authorized.
- A USB data cable and a computer with [Android platform-tools](https://developer.android.com/tools/releases/platform-tools).
- The Portal and your phone on the same trusted Wi-Fi for QR setup, lists and photo transfer.
- A signed APK from this project's Releases, or your own signed/debug build.

This guide does not activate a factory-reset Portal, flash firmware, unlock the bootloader or obtain root. The tested device was a second-generation 10-inch Portal on Android 10, build `QKQ1.210213.001.3051355900018050`. Upstream's [Immortal provisioning guide](https://github.com/starbrightlab/immortal/blob/main/docs/provisioning.md) describes a separate community setup path; review its model/firmware requirements before considering it. Installing Immortal is not a prerequisite for these commands on an already accessible stock Portal.

## 1. Identify the device

Enable the device's available ADB/USB debugging option, connect USB and accept the authorization prompt on your own Portal.

```bash
adb devices
adb -s YOUR_SERIAL shell getprop ro.product.model
adb -s YOUR_SERIAL shell getprop ro.build.version.sdk
```

Replace `YOUR_SERIAL` in every command. An `unauthorized` result means the Portal must approve the computer. `offline` usually requires reconnecting the cable or restarting ADB. An absent debugging option is a device/firmware access issue this app cannot solve.

## 2. Preserve an existing installation

If PortalHub is already installed, export its configuration first using [BACKUP-RESTORE.md](BACKUP-RESTORE.md). Keep any camera photos you want separately. This project does not back up photos.

The package is `com.portal.calendar`. Android requires the same signing key for an in-place update. **Do not uninstall or clear app data just to bypass a signing error:** those actions erase local lists, settings and camera photos. A source build signed by you will not match an upstream release or this project's release key.

## 3. Install and open

```bash
adb -s YOUR_SERIAL install --no-incremental -r Portal-Family-Home.apk
adb -s YOUR_SERIAL shell am start -n com.portal.calendar/.MainActivity
```

If `--no-incremental` is unrecognized, update platform-tools. An `INSTALL_FAILED_UPDATE_INCOMPATIBLE` error means the signing identity differs. `INSTALL_FAILED_VERSION_DOWNGRADE` means a newer version is installed; review a migration rather than forcing a downgrade.

## 4. Choose the dashboard as Home

```bash
adb -s YOUR_SERIAL shell cmd package set-home-activity com.portal.calendar/.MainActivity
adb -s YOUR_SERIAL shell input keyevent KEYCODE_HOME
```

Open Settings to choose theme, calendar view, scale and orientation. Auto rotation works on the tested device; the orientation-swap option handles differently mounted sensors on other models. The tested layout used dark theme, Week view and automatic orientation. Configure these yourself; no personal setup is bundled.

Camera access is optional. Grant it when requested, or explicitly enable it for this package:

```bash
adb -s YOUR_SERIAL shell pm grant com.portal.calendar android.permission.CAMERA
```

No Google Play services are installed by this project. The core subscription-calendar, weather, radio UI and camera features do not need them. Voice/AI and account integrations inherited from PortalHub require separate configuration and were not part of the verified baseline.

## 5. Open the full configuration page over USB

The management server is intentionally local to the Portal. With the app running:

```bash
adb -s YOUR_SERIAL forward tcp:8090 tcp:8090
```

Open `http://127.0.0.1:8090` on the computer. After editing, remove the forward:

```bash
adb -s YOUR_SERIAL forward --remove tcp:8090
```

Phone QR setup is a separate, limited interface on port 8091. Do not change the management server to listen on all interfaces or expose it through a router/tunnel.

## Return to Meta Home

On the tested stock firmware:

```bash
adb -s YOUR_SERIAL shell cmd package set-home-activity com.facebook.alohaapps.launcher/com.facebook.aloha.app.home.touch.HomeActivity
adb -s YOUR_SERIAL shell input keyevent KEYCODE_HOME
```

This changes Home selection without deleting the custom app or its data. The stock component name may differ on another model or firmware; inspect the actual launcher before applying it there.
