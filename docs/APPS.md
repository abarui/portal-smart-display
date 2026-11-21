# Apps and daily use

The dashboard's **Home** returns to the calendar. **Settings** controls the dashboard; **Apps** opens the stock Portal launcher. The Radio, Weather and Camera screens include their own navigation. Music/video shortcuts require the corresponding app or browser to be present.

## AIR / Akashvani radio

Install **[Transistor](https://github.com/y20k/transistor/releases)** separately. The dashboard is a native controller for Transistor's media service, not an independent streaming engine. Transistor 4.3.11 was tested. Stream availability and addresses can change; consult [Akashvani live radio](https://akashvani.gov.in/radio/live.php) if a station fails.

The `examples/radio/` folder contains one M3U per station and a clean Transistor collection with ten stations: AIR Bangla, Kolkata Geetanjali, Kolkata Sanchayita, Maitree, FM Gold Kolkata, FM Rainbow Kolkata, Raagam, FM Gold Delhi, FM Rainbow Delhi and Live News 24x7. These are public stream links, not redistributed radio audio or logos.

For an existing Transistor setup, add the individual stream URLs through its normal Add station interface or import **one station M3U at a time**. A multi-station M3U is not equivalent to a Transistor collection: the tested parser treats several URLs as alternatives for one station.

For a new/empty Transistor installation only, the supplied collection can be seeded over ADB after launching Transistor once:

```bash
adb -s YOUR_SERIAL shell am force-stop org.y20k.transistor
adb -s YOUR_SERIAL shell mkdir -p /sdcard/Android/data/org.y20k.transistor/files/collection
adb -s YOUR_SERIAL push examples/radio/collection.json /sdcard/Android/data/org.y20k.transistor/files/collection/collection.json
```

This replaces that collection file. If you have existing stations, export them in Transistor first or add stations individually instead. Reopen Transistor, then open Radio on the dashboard. Select **Bengali** to filter stations, use the star to add favorites, and adjust the media-volume slider. AIR Bangla is the initial suggested favorite when available.

## Photos while idle: Frame Resume (new in v3.14.0)

**[Frame Resume](FRAME-RESUME.md)** is the new companion photo app included in this project's releases. It remembers the last successfully displayed photo and preserves its shuffle queue after reopening or restarting. Follow its guide to install, reconnect shared albums, use its own phone-upload QR page, select it for idle photos and switch back to original Frame if desired.

The package is `com.portalhacks.frame.resume`. Original Frame can stay installed; photos uploaded directly to it remain there and need reuploading into Frame Resume. Video playback remains unsupported in both.

## Original Frame (optional alternative)

Use these commands only when choosing original Frame. Its guard competes with Frame Resume's guard; disable the new app's guard first using the [switching instructions](FRAME-RESUME.md#switch-screensavers).


Install **[Frame](https://github.com/Ishtiaqhossain/portal-frame/releases)** separately; 1.2.0 was tested. Open Frame and add your own Google Photos/iCloud shared-album links, or scan its photo-upload QR code from a phone on the same Wi-Fi. Media stays managed by Frame, not this dashboard.

To let Frame retain the screensaver selection on the tested Android firmware:

```bash
adb -s YOUR_SERIAL shell pm grant com.portalhacks.frame android.permission.WRITE_SECURE_SETTINGS
adb -s YOUR_SERIAL shell am broadcast -n com.portalhacks.frame/.ConfigReceiver --ez guard true
adb -s YOUR_SERIAL shell settings put secure screensaver_components com.portalhacks.frame/.FrameDreamService
adb -s YOUR_SERIAL shell settings put secure screensaver_enabled 1
adb -s YOUR_SERIAL shell settings put secure screensaver_activate_on_sleep 1
```

First record the original settings if you want to undo them later. In dashboard Settings, choose the idle behavior that yields to the screensaver after about two minutes. A 15-second Android screen timeout was used on the tested setup. Avoid simultaneously enabling other launchers' competing screensaver guards.

Frame 1.2.0 is **photo-only**. It skips videos or shows still thumbnails in Google Photos albums. Frame Resume also does not add video playback. PicDock, Fotoo and local-video players are possible alternatives, but have not been validated on this Portal configuration.

## Shared shopping lists

Open Lists, create/select a list and use its phone-sharing QR code. That link is separate from temporary calendar pairing and persists until renewed/revoked in the app. Anyone who holds it and can reach the Portal can edit the shared lists. If the Portal's local IP changes, scan the code again. Wi-Fi client isolation or a guest network can prevent access.

## Weather

Open Weather to select a city, view current/hourly/seven-day conditions, and choose °C or °F. Forecasts are from [Open-Meteo](https://open-meteo.com/); respect its service terms and availability. This is not an emergency weather-alert system. City selection is stored only in your own app configuration.

## Camera

Open the physical camera shutter and allow camera access. The app takes still JPEG photos; it does not record video. Timer delays capture by three seconds, Mirror affects the preview, and Gallery shows saved photos. **Export photo** creates a temporary link to that one photo; closing the dialog revokes it, and it expires after ten minutes. Photos remain in app-private storage unless exported. Clearing app data or uninstalling deletes them. No photos are included in this repository or in the configuration-backup script.

## Music and other apps

| App | Integration and observed limits |
| --- | --- |
| Pandora | Home shortcut opens an independently installed Pandora app. Version 2611.3 streamed on the tested device after sign-in. Account, country and service limits apply. |
| Spotify | Version 9.2.2.2589 signed in, but playback returned unavailable/cannot-stream errors. Do not assume installing it makes streaming work. |
| YouTube | Shortcut opens the official website in the Portal browser. Browser age and Google's sign-in rules may affect it. |
| YouTube Music | The tested Android app required unavailable Google services and was removed. |
| Home Assistant | The [official companion app](https://github.com/home-assistant/android/releases) requires a separate Home Assistant server. Alexa, Google Home and an Apple TV home hub do not themselves constitute that server. |
| Hey Muse | Optional separate [community voice-assistant project](https://github.com/pgodlews/hey-muse-portal). Its pairing, microphone permissions and runtime requirements are independent of this dashboard. |

Obtain other apps from their developers or reputable distribution channels and check compatibility/signatures. This repository does not bundle commercial APKs, subscriptions, Google Play services or account credentials.
