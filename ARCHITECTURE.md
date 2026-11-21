# Architecture

Portal Smart Display is a single Android application derived from PortalHub. It runs on the Portal, with no project-hosted cloud backend. Calendar providers, forecasts, radio streams and companion apps remain external dependencies.

## Application structure

The Gradle project has one module, `app`, and Kotlin package `com.portal.calendar`.

| Area | Main files | Responsibility |
| --- | --- | --- |
| Lifecycle | `App.kt`, `MainActivity.kt`, `KeepAliveService.kt`, `BootReceiver.kt` | Start the dashboard, maintain services and handle idle handoff |
| Dashboard | `BoardController.kt`, `DayTimeline.kt`, `Palette.kt`, `HubGlyph.kt` | Clock, calendar views, weather summary, theme and navigation |
| Radio | `RadioActivity.kt` | Browse stations and control the separately installed Transistor service |
| Weather | `Weather.kt`, `WeatherActivity.kt` | Open-Meteo data, location search and forecast UI |
| Camera | `CameraActivity.kt`, `PhotoSharing.kt` | Local still capture, gallery and scoped temporary photo sharing |
| App shortcuts | `HubAppActivity.kt` | Launch installed companion apps or web destinations |
| Local management | `ConfigServer.kt`, `ConfigStore.kt`, `ConfigBundle.kt` | Settings UI/API, preferences and configuration import/export |
| Phone access | `PhoneSetupServer.kt`, `PhonePairing.kt`, `ShoppingAccess.kt` | Separate calendar pairing, shared lists and photo endpoints |
| Calendar data | `SyncManager.kt`, `SubscriptionLink.kt`, `CalDav*.kt`, `GoogleCal.kt`, `Writers.kt` | Fetch/parse feeds and inherited optional account integrations |
| Family data | `Data.kt`, `Members.kt`, `Chores.kt`, `Routines.kt`, `FamilyLists.kt`, `Meals.kt` | Local JSON persistence and family-board features |
| Idle behavior | `Screensaver.kt` and lifecycle code | Yield to an optional external screensaver |

Files live in `app/src/main/java/com/portal/calendar/`. HTML/CSS/JavaScript for management, pairing, shopping and photo download live in `app/src/main/assets/`.

## Network boundaries

- `ConfigServer` binds to **127.0.0.1:8090**. It exposes the complete management API without a separate login. A trusted operator accesses it through an ADB forward; it must not be forwarded publicly.
- `PhoneSetupServer` listens on **8091** on the local network. Its static pages are public; protected APIs require scoped random tokens. Calendar pairing and selected-photo access expire. Shared-list access has its own persistent token.
- Phone traffic uses HTTP, so tokens and submitted data are not encrypted in transit. Use a trusted local network. See [SECURITY.md](SECURITY.md).
- Calendar URLs are validated against supported providers before subscription. Providers and redirect targets are checked by `SubscriptionLink`.
- Outbound calendar and weather requests go to their respective services. Radio audio is played by Transistor, not by a server in this repository.

The inherited `FamilySync.kt` expects a management API reachable across the LAN. Because this fork restricts that API to loopback, **multi-Portal sync is unsupported and should remain disabled**.

## Storage and backups

Preferences and family JSON files live in Android app-private storage. Calendar URLs and optional account credentials can be sensitive. `ConfigBundle` exports selected preferences and family data as a base64 JSON bundle; this is not encryption. Camera media and companion-app settings are outside that bundle. See [backup and restore](docs/BACKUP-RESTORE.md) for exact scope.

The source repository contains examples only. Device data, signing keys and installed companion APKs are not part of the repository.

## Tests and build

`app/src/test/` contains JVM tests for calendar handling, pairing, scoped sharing and other pure logic. Optional Radicale integration tests require a separately configured fixture; they are skipped without it. Python tests under `scripts/tests/` verify backup safety. CI builds an unsigned release APK, runs unit tests and Android lint. It does not prove compatibility with every Portal model or third-party service.

The published app uses the same Android package as upstream PortalHub. Its signing identity determines whether it can update an existing installation. See [build and signing](docs/BUILD.md).

## Scope of inherited features

PortalHub also contains CalDAV/Google write support, Gemini integration, chores, routines, meals and other features. Their presence in source is not a claim that each integration was tested on this customized device. The verified scope and known failures are listed in [README.md](README.md).
