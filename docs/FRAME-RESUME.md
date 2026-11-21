# Frame Resume — new in v3.14.0

Frame Resume remembers the last successfully displayed photo and keeps the remaining shuffle order when the slideshow opens again. It is a separate app based on [Frame 1.2.0](https://github.com/Ishtiaqhossain/portal-frame), with its own package and photo storage.

| Component | Version in this release | Package |
| --- | --- | --- |
| Portal Smart Display project release | **v3.14.0 — Frame Resume edition** | Two separate Android apps |
| Home Hub / PortalHub dashboard | 3.13-pandora-button, version code 35 | `com.portal.calendar` |
| Frame Resume | **1.2.0-resume.1, version code 15** | `com.portalhacks.frame.resume` |

The dashboard installer is unchanged from v3.13.1. Existing dashboard users can add Frame Resume without reinstalling Home Hub. Original Frame (`com.portalhacks.frame`) can remain installed.

## 1. Download and identify the Portal

Download **Frame-Resume.apk** and **SHA256SUMS** from [v3.14.0](https://github.com/abarui/portal-smart-display/releases/tag/v3.14.0), or use [the latest Frame Resume installer](https://github.com/abarui/portal-smart-display/releases/latest/download/Frame-Resume.apk). Compare the APK's SHA-256 with the entry in SHA256SUMS. All listed files are needed if you use `shasum -a 256 -c SHA256SUMS` to check the entire release.

Use an already working Portal with authorized USB debugging and current Android platform-tools. The tested device is a second-generation 10-inch Portal running Android 10. This does not unlock, provision or replace its firmware. See [installation requirements](INSTALL.md).

```sh
adb devices
adb -s YOUR_SERIAL shell settings get secure screensaver_components
adb -s YOUR_SERIAL shell settings get secure screensaver_enabled
adb -s YOUR_SERIAL shell settings get secure screensaver_activate_on_sleep
adb -s YOUR_SERIAL shell settings get system screen_off_timeout
```

Replace `YOUR_SERIAL` with your device serial and save those settings privately before changing them. Keep a note of any existing screensaver guard that is enabled.

## 2. Install alongside original Frame

From the repository root, the helper shows a plan first and does not contact the device:

```sh
python3 apps/frame-resume/scripts/install_resume.py --serial YOUR_SERIAL --apk /path/to/Frame-Resume.apk
```

Add `--apply` to install and configure:

```sh
python3 apps/frame-resume/scripts/install_resume.py --serial YOUR_SERIAL --apk /path/to/Frame-Resume.apk --apply
```

Use `--adb /path/to/adb` if ADB is not on your PATH. The helper installs or updates only `com.portalhacks.frame.resume`, grants its screensaver and camera permissions, enables resume, sets six-second photos with Fast transitions, disables original Frame's guard if installed, enables Frame Resume's guard, and opens the new slideshow. Camera permission supports the album QR scanner. It never uninstalls original Frame or copies/deletes its uploads.

For a basic manual install and setup screen instead:

```sh
adb -s YOUR_SERIAL install --no-incremental -r Frame-Resume.apk
adb -s YOUR_SERIAL shell am start -n com.portalhacks.frame.resume/com.portalhacks.frame.SettingsActivity
```

The full class names after `/` are intentional: the new package still uses the original source namespace. The manual install alone does not select a screensaver; either use the helper or configure the guard with the commands under “Switch screensavers” below.

## 3. Add your own albums and uploads

Open Frame Resume settings using the command above, or press and hold a photo while the slideshow is showing. Some Portal launchers do not list sideloaded apps, so the ADB command is the reliable setup entry point.

- Add your own Google Photos or iCloud shared-album link in the setup screen. Original Frame's album configuration is not automatically readable by the new app; reuse the same links manually.
- For direct uploads, scan the **Add photos from a phone** QR code displayed by **Frame Resume** with a phone on the same trusted Wi-Fi. Use this new app's QR code: original Frame's existing upload page still belongs to original Frame.
- Photos already uploaded directly to original Frame remain there. Upload them again into Frame Resume if you want them in the new slideshow.

For a private configuration import, copy [configuration.example.json](../examples/frame-resume/configuration.example.json) into a `private/` folder, replace the placeholder with your own share link, and pass `--config private/frame-resume.json` to the helper before `--apply`. Each album has a JSON boolean `enabled`; the import adds/updates the listed links and does not delete unlisted albums. Keep this file out of Git, issues and release attachments.

## 4. Set the idle display

The helper selects Frame Resume and enables its guard. To allow idle activation on the tested firmware:

```sh
adb -s YOUR_SERIAL shell settings put secure screensaver_activate_on_sleep 1
```

In **Home Hub → Settings**, select the behavior that yields to the screensaver after your chosen delay (about two minutes in the tested setup). Android's screen timeout also applies. If desired, the tested timeout was 15 seconds:

```sh
adb -s YOUR_SERIAL shell settings put system screen_off_timeout 15000
```

Keep only the chosen screensaver's guard enabled; competing launchers can otherwise reclaim the slot. Home Hub remains the Home app. Its Apps button opens the stock Portal launcher.

## What resume remembers

**Playback → Resume where I left off** is on by default. The app saves a private photo fingerprint and queue on the Portal after an image successfully displays. Returning from Home or restarting the app resumes that image, then advances normally. Failed or cancelled loads do not advance the saved bookmark. In shuffle mode, remaining photos keep their order and new photos join the queue. If the saved photo is removed, the next surviving photo in the old queue is selected.

A live phone upload still appears immediately and becomes the current photo. Normal app updates preserve the bookmark; clearing app data or uninstalling removes it. This is a photo position, not a video timestamp or the remaining seconds for an image. If a provider changes a photo's identifier, it may be treated as a new image.

**Only show clock in low light** can pause the photos in a dark room. Adjust that separate setting if you want photos at night. Resuming was tested after Home/reopen, a full app-process restart, and automatic Home Hub idle handoff. A device reboot was not performed as part of validation. See [verification details](../apps/frame-resume/VERIFICATION.md).

## Updating Frame Resume

Use the same signing identity and update in place:

```sh
adb -s YOUR_SERIAL install --no-incremental -r Frame-Resume.apk
```

A self-signed/debug APK and the project's published installer may have different signatures. Do not uninstall a populated app or force a downgrade to bypass an Android installation error. The root dashboard and Frame Resume have independent version codes and packages.

## Switch screensavers

To choose Frame Resume manually, disable the original guard **if original Frame is installed**, then enable the new one:

```sh
adb -s YOUR_SERIAL shell am broadcast -n com.portalhacks.frame/.ConfigReceiver --ez guard false
adb -s YOUR_SERIAL shell pm grant com.portalhacks.frame.resume android.permission.WRITE_SECURE_SETTINGS
adb -s YOUR_SERIAL shell am broadcast -n com.portalhacks.frame.resume/com.portalhacks.frame.ConfigReceiver --ez guard true
```

To return to original Frame when it is installed:

```sh
adb -s YOUR_SERIAL shell am broadcast -n com.portalhacks.frame.resume/com.portalhacks.frame.ConfigReceiver --ez guard false
adb -s YOUR_SERIAL shell am force-stop com.portalhacks.frame.resume
adb -s YOUR_SERIAL shell pm grant com.portalhacks.frame android.permission.WRITE_SECURE_SETTINGS
adb -s YOUR_SERIAL shell am broadcast -n com.portalhacks.frame/.ConfigReceiver --ez guard true
adb -s YOUR_SERIAL shell input keyevent KEYCODE_HOME
```

This retains both apps and their private photos. To return to a different previous screensaver, disable Frame Resume's guard and restore the component/enable/timeout settings you recorded in step 1. Do not enable both guards together.

## Build, backup and limitations

Source lives in [`apps/frame-resume/`](../apps/frame-resume/README.md), with its own wrapper, tests, MIT license and third-party notices. See [build/signing instructions](../apps/frame-resume/RELEASING.md). Published signed APKs contain no personal album links or photos; they include the upstream demonstration images credited in NOTICE.

The dashboard's backup script does not export Frame Resume's private bookmarks or directly uploaded photos. Keep your own album links and settings privately if needed; a fresh installation starts a new rotation. Public releases never include a user's recovery bundle, signing key or photo files.

Frame Resume retains Frame's existing shared-album provider limitations. **It does not add video playback**, including Google Photos album videos. This community fork is not an official Meta or upstream Frame release.
