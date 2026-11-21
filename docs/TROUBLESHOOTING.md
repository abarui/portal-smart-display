# Troubleshooting

## App cannot install

Check Android API level (minimum 28), free space and the APK signature. `INSTALL_FAILED_UPDATE_INCOMPATIBLE` is usually a signing-key mismatch with an existing `com.portal.calendar` app. Export your configuration and review the migration before any uninstall. `INSTALL_FAILED_VERSION_DOWNGRADE` means the installed version code is newer. This project does not force either case.

## QR code opens nothing / phone cannot connect

Keep the phone and Portal on the same trusted Wi-Fi. Disable phone VPN routing only if appropriate for your network, and check guest-network/client-isolation rules. Rescan if the Portal's IP changed. Open a new calendar/photo code if expired. Calendar pairing, shopping sharing and photo download use different capabilities; one code does not authorize the others. The management page is deliberately USB-only through `adb forward`, not a Wi-Fi setup URL.

## Calendar missing or empty

Use the ICS/webcal subscription URL, not an HTML calendar view. Check provider publishing permission, link revocation and the selected detail level. Work/school policies may disallow publishing. Allow time for refresh; check source event timezone if events are offset. Do not post your feed URL in a public issue.

## Photos always start over

Use **Frame Resume**, package `com.portalhacks.frame.resume`, and enable **Playback → Resume where I left off**. Original Frame is a separate app and retains its original behavior. Confirm the screensaver component is `com.portalhacks.frame.resume/com.portalhacks.frame.FrameDreamService`, and keep only that app's guard enabled. See [setup and switching](FRAME-RESUME.md).

If the saved photo was removed, the next surviving photo is selected. Clearing app data removes the bookmark; routine updates should use `adb install -r`. If the room is dark and only a clock appears, check the separate low-light clock setting.

## Original Frame uploads are missing in Frame Resume

Each app has separate private storage. Re-add your shared-album links and upload direct photos through Frame Resume's own QR code. The new app cannot read original Frame's private uploads; original Frame still retains them.

## Frame shows a video thumbnail but will not play it

Frame 1.2.0 does not implement video playback. Google Photos videos are skipped or appear as still thumbnails. The Frame Resume companion adds photo-position memory, not video support. Other slideshow apps may support videos, but compatibility and Google sign-in must be tested separately on the Portal.

## Radio says it cannot connect / there are no stations

Install and launch Transistor, add stations, then reopen Radio. This UI connects to Transistor's `PlayerService`; a future upstream package/component change can affect it. If only one station fails, test another and check the broadcaster's current stream. A running UI does not guarantee the remote stream is available. Do not import a combined multi-station M3U as a single Transistor station.

## Spotify or YouTube Music fails

Spotify sign-in worked in testing but songs did not stream. The same account working on a phone does not establish Portal compatibility. YouTube Music failed due to missing Google services. Pandora streamed successfully in the tested account/region. No Google services installation or subscription purchase is part of this project.

## Camera is black or unavailable

Open the physical shutter, verify the CAMERA permission and close other camera-using apps. Rotate/reopen the Camera screen if the surface needs rebuilding. The implementation takes still photos only and is hardware-dependent. Photos deleted through Android app-data reset or uninstall are not recoverable from the configuration export.

## Home or screensaver keeps changing

Select the desired Home component again and review competing launcher/screensaver guards. Frame's guard can preserve the photo dream component; another launcher may overwrite it. Dashboard idle settings can keep the screen awake or yield after a delay. Check both those controls before changing the Android timeout. Keep the original component/settings values for reversal.

## Advanced features from upstream do not behave as its README describes

This fork binds the full configuration server to `127.0.0.1` and uses a separate limited phone server. Multi-Portal LAN mirroring from upstream consequently cannot work through that endpoint and is unsupported here. Leave family sync disabled. Advanced CalDAV, Google-writing, Tasks and optional AI paths remain in source but were not part of this fork's full device-validation scope. Use the current guides in this repository rather than assuming every upstream deployment instruction is unchanged.

## Useful bug reports

Include model, Android version, app version, screen orientation, expected/actual behavior and minimal reproduction steps. Use invented calendar events and redact screenshots/logs. Never attach configuration exports, account tokens, signing keys, device serials or private calendar/album links. See [SECURITY.md](../SECURITY.md) for private security reports.
