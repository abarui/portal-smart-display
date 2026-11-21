# Portal Smart Display

A community customization of **[PortalHub](https://github.com/thefloppytaco/portal-calendar)** that turns a compatible Meta Portal into a calendar dashboard with weather, shared shopping lists, internet radio, a still camera, and shortcuts to music and video apps.

This is an Android app and setup guide, **not a replacement operating system**. It requires an already working Portal with authorized USB debugging. It is not affiliated with Meta, Apple, Google, Microsoft, Pandora, Spotify or Akashvani.

## New version: v3.14.0 — Frame Resume edition

**[Download v3.14.0](https://github.com/abarui/portal-smart-display/releases/tag/v3.14.0)** adds **Frame Resume**, which remembers the last displayed photo and preserves its shuffle order after leaving Home, reopening the slideshow or restarting the app.

- **[Frame-Resume.apk](https://github.com/abarui/portal-smart-display/releases/latest/download/Frame-Resume.apk)** — new photo app, version 1.2.0-resume.1 (15).
- **[Portal-Family-Home.apk](https://github.com/abarui/portal-smart-display/releases/latest/download/Portal-Family-Home.apk)** — the existing Home Hub dashboard, version 3.13-pandora-button (35), unchanged in this release.
- [Frame Resume setup and migration](docs/FRAME-RESUME.md) · [What changed](CHANGELOG.md) · [Source](apps/frame-resume/).

Existing dashboard users only need to add Frame Resume. It installs alongside original Frame, reuses your own shared-album links, and has its own QR upload page. Direct uploads in original Frame need uploading again into the new app.

## What you get

- A large clock, modern translucent panels, portrait/landscape layouts and Home / Settings / Apps navigation.
- Google Calendar, iCloud and Outlook subscription calendars, added from a phone using a temporary QR code.
- Shared shopping lists with a separate persistent phone link.
- A native radio interface with favorites, Bengali station filtering, volume and navigation, using the separately installed Transistor player.
- Current weather, hourly and seven-day forecasts, city search and Celsius/Fahrenheit.
- A still-camera screen with a three-second timer, mirrored preview, gallery and temporary QR download of one selected photo.
- A YouTube website shortcut and a Pandora app shortcut.
- Idle handoff to **Frame Resume**, with a saved photo position and shuffle queue; original Frame can remain installed.
- The underlying PortalHub family-board features and configuration export/import.

No personal calendar feeds, account sessions, photos, device identifiers or signing secrets are included. All examples are placeholders or public broadcaster URLs.

## Start here

1. Read [compatibility and installation](docs/INSTALL.md).
2. Download the signed Home Hub and optional Frame Resume APKs from [Releases](https://github.com/abarui/portal-smart-display/releases), or [build it yourself](docs/BUILD.md).
3. Connect your own [calendars and phone](docs/CALENDARS.md).
4. Set up [radio, music, Frame and other apps](docs/APPS.md).
5. Keep a private [configuration backup](docs/BACKUP-RESTORE.md).

| Guide | Covers |
| --- | --- |
| [Installation](docs/INSTALL.md) | ADB, APK installation, Home selection, permissions and returning to Meta Home |
| [Calendars and phone setup](docs/CALENDARS.md) | Google, iCloud, Outlook, QR pairing and subscription limitations |
| [Frame Resume — new in v3.14.0](docs/FRAME-RESUME.md) | Resume photos, installation, album migration, QR uploads, idle behavior and rollback |
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
| Frame Resume 1.2.0-resume.1 | Exact photo/queue resume after Home, process restart and idle handoff verified |
| Original Frame 1.2.0 | Can remain installed alongside Frame Resume |
| Frame / Frame Resume videos | **Unsupported**, including Google Photos album videos |
| Pandora 2611.3 | Sign-in and streaming worked on the tested device; region/account restrictions still apply |
| Spotify 9.2.2.2589 | Sign-in worked, but streaming failed on the tested device |
| YouTube Music 9.44.53 | Failed because required Google services were missing; not included |
| Home Assistant | Companion app can be installed; a separate Home Assistant server is required and was not configured |
| Multi-Portal family sync | Not supported by this fork's loopback-only management server; leave disabled |
| Firmware update blocking | **Partial only**; native updater may still update the OS |

Third-party apps and services can change independently. Only this project's customized Home Hub and Frame Resume installers are published; external music/radio/home-automation APKs are not bundled. The calendar package remains `com.portal.calendar`: upstream PortalHub and builds signed with another key cannot update each other in place. Read the signing notes before replacing an existing installation.

## Development

```bash
git clone https://github.com/abarui/portal-smart-display.git
cd portal-smart-display
./gradlew testDebugUnitTest assembleRelease lintRelease
```

Use JDK 17, Android SDK platform 36 and build-tools 36.0.0. The default release task produces an **unsigned** APK; [BUILD.md](docs/BUILD.md) explains signing. No device is modified by the build. Frame Resume is an independent Gradle project under `apps/frame-resume`; follow [its build guide](apps/frame-resume/RELEASING.md).

## Credits and license

Based on [thefloppytaco/portal-calendar](https://github.com/thefloppytaco/portal-calendar), upstream commit `4642e4e7b41cdbb30d54dd5455da852f2d23ea67`. The original PortalHub copyright and MIT license are preserved. Customizations and these guides are also provided under [MIT](LICENSE). See [NOTICE.md](NOTICE.md) for dependencies and companion projects.

Frame Resume derives from [Ishtiaqhossain/portal-frame](https://github.com/Ishtiaqhossain/portal-frame), commit `4affee8c57c2b47488ff53755094a4d6e32ad74e`. Its original [MIT license](apps/frame-resume/LICENSE) and [third-party notices](apps/frame-resume/NOTICE) are preserved.
