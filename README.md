# Portal Smart Display

A community customization of **[PortalHub](https://github.com/thefloppytaco/portal-calendar)** that turns a compatible Meta Portal into a calendar dashboard with weather, shared shopping lists, internet radio, a still camera, and shortcuts to music and video apps.

This is an Android app and setup guide, **not a replacement operating system**. It requires an already working Portal with authorized USB debugging. It is not affiliated with Meta, Apple, Google, Microsoft, Pandora, Spotify or Akashvani.

## What you get

- A large clock, modern translucent panels, portrait/landscape layouts and Home / Settings / Apps navigation.
- Google Calendar, iCloud and Outlook subscription calendars, added from a phone using a temporary QR code.
- Shared shopping lists with a separate persistent phone link.
- A native radio interface with favorites, Bengali station filtering, volume and navigation, using the separately installed Transistor player.
- Current weather, hourly and seven-day forecasts, city search and Celsius/Fahrenheit.
- A still-camera screen with a three-second timer, mirrored preview, gallery and temporary QR download of one selected photo.
- A YouTube website shortcut and a Pandora app shortcut.
- Optional idle handoff to the separately installed Frame photo screensaver.
- The underlying PortalHub family-board features and configuration export/import.

No personal calendar feeds, account sessions, photos, device identifiers or signing secrets are included. All examples are placeholders or public broadcaster URLs.

## Start here

1. Read [compatibility and installation](docs/INSTALL.md).
2. Download the project's signed APK from [Releases](https://github.com/abarui/portal-smart-display/releases), or [build it yourself](docs/BUILD.md).
3. Connect your own [calendars and phone](docs/CALENDARS.md).
4. Set up [radio, music, Frame and other apps](docs/APPS.md).
5. Keep a private [configuration backup](docs/BACKUP-RESTORE.md).

| Guide | Covers |
| --- | --- |
| [Installation](docs/INSTALL.md) | ADB, APK installation, Home selection, permissions and returning to Meta Home |
| [Calendars and phone setup](docs/CALENDARS.md) | Google, iCloud, Outlook, QR pairing and subscription limitations |
| [Apps and everyday use](docs/APPS.md) | AIR radio, Frame, shopping lists, weather, camera and music |
| [Build and signing](docs/BUILD.md) | JDK/SDK requirements, tests, reproducible source and your own signing key |
| [Backup and restore](docs/BACKUP-RESTORE.md) | Photo-free configuration export, recovery and what is not captured |
| [Update controls](docs/UPDATES.md) | Partial OTA controls, verification and undo |
| [Troubleshooting](docs/TROUBLESHOOTING.md) | Known failures and practical recovery steps |
| [Privacy and security](SECURITY.md) | LAN endpoints, QR links, credentials and safe issue reports |
| [Contributing](CONTRIBUTING.md) | Development, testing and useful bug reports |
| [Attribution](NOTICE.md) | Upstream authors, libraries and external services |

## Tested scope and known limits

The customized build was exercised on a **Portal second-generation 10-inch, Android 10 / API 29**, in portrait and landscape. Other Portal models are not verified by this fork. Minimum Android API is 28; that alone does not guarantee compatibility with another model or firmware.

| Feature | Observed status |
| --- | --- |
| Dashboard, subscription calendars, QR setup and shared lists | Working on the tested device |
| AIR radio through Transistor 4.3.11 | Working, including Bangla stations |
| Weather and still camera | Working; camera photos were 1280 × 720 on the tested device |
| Frame 1.2.0 photos / idle handoff | Working with separate configuration |
| Frame videos | **Unsupported by Frame 1.2.0**, including Google Photos album videos |
| Pandora 2611.3 | Sign-in and streaming worked on the tested device; region/account restrictions still apply |
| Spotify 9.2.2.2589 | Sign-in worked, but streaming failed on the tested device |
| YouTube Music 9.44.53 | Failed because required Google services were missing; not included |
| Home Assistant | Companion app can be installed; a separate Home Assistant server is required and was not configured |
| Multi-Portal family sync | Not supported by this fork's loopback-only management server; leave disabled |
| Firmware update blocking | **Partial only**; native updater may still update the OS |

Third-party apps and services can change independently. No third-party APKs are redistributed here. The calendar package remains `com.portal.calendar`: upstream PortalHub and builds signed with another key cannot update each other in place. Read the signing notes before replacing an existing installation.

## Development

```bash
git clone https://github.com/abarui/portal-smart-display.git
cd portal-smart-display
./gradlew testDebugUnitTest assembleRelease lintRelease
```

Use JDK 17, Android SDK platform 36 and build-tools 36.0.0. The default release task produces an **unsigned** APK; [BUILD.md](docs/BUILD.md) explains signing. No device is modified by the build.

## Credits and license

Based on [thefloppytaco/portal-calendar](https://github.com/thefloppytaco/portal-calendar), upstream commit `4642e4e7b41cdbb30d54dd5455da852f2d23ea67`. The original PortalHub copyright and MIT license are preserved. Customizations and these guides are also provided under [MIT](LICENSE). See [NOTICE.md](NOTICE.md) for dependencies and companion projects.
