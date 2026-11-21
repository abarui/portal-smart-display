# Changelog

## v3.14.0 — Frame Resume edition (21 November 2026)

This is the new Portal Smart Display release. It adds **Frame Resume**, a separate photo screensaver that remembers the last successfully displayed photo and continues its saved shuffle queue after returning Home, reopening the slideshow or restarting the app.

- Publish Frame Resume 1.2.0-resume.1 (version code 15), package `com.portalhacks.frame.resume`, alongside its source and signed installer.
- Preserve original Frame and its uploads; shared albums can be linked to the new app, while direct uploads use its own QR page.
- Keep photo order across restarts, append newly discovered photos and skip removed photos; avoid saving failed or cancelled image loads.
- Add installation, album/upload migration, idle setup, update, rollback, build/signing and troubleshooting instructions.
- Add an independent Frame Resume CI job with six JVM tests, Android lint, detekt and ktlint.
- Retain the tested Home Hub installer unchanged: 3.13-pandora-button (version code 35). The project release number and individual app versions are distinct.

No personal photos, calendar/album links, device backups, account sessions or signing keys are included. Video playback remains unsupported. See [Frame Resume](docs/FRAME-RESUME.md) for setup and the limits of verification.

## v3.13.1 — Community release (21 November 2026)

Published the customized PortalHub dashboard, setup/backup/privacy guides and signed Home Hub installer. Fixed Android SDK setup in CI and linked the public release. App version: 3.13-pandora-button (35).

## v3.13.0 — Initial community release (21 November 2026)

Initial public source and guides for the calendar dashboard, phone QR setup, shopping lists, AIR radio, Weather, Camera, Pandora shortcut and optional Frame idle handoff.
