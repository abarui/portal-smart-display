# Verification — 21 November 2026

Version 1.2.0-resume.1 (15), a separate Frame Resume installation on a Meta Portal running Android 10.

## Build and automated checks

`testDebugUnitTest assembleRelease lintDebug detekt ktlintCheck` passed.

Six JVM tests cover an exact shuffle resume, continuing to the next photo, removed photos, wraparound, album insertions/reordering, empty/replaced albums, duplicate entries and the first run. Android lint reported no errors; existing upstream warnings remain. The upstream Kotlin/Android build-tool combination also emits R8 metadata warnings; the release APK built, passed signature verification and ran on the Portal.

The ktlint baseline's line numbers were remapped only for unchanged upstream source lines. Entries for edited/deleted lines were removed. No new finding was added to the baseline.

## Checks on the Portal

- Several different linked-album photos rendered and advanced normally.
- Leaving for Home and returning resumed the exact photo fingerprint and queue position.
- The next displayed photo was the next queue position.
- Force-stopping and restarting the app resumed the exact photo and queue position.
- The bookmark did not advance while the app was in the background.
- Original Frame remains installed at version 1.2.0 (14), with its private photo storage untouched.
- New Frame Resume is version 1.2.0-resume.1 (15); its screensaver guard and upload service are running.
- Home Hub automatically yielded to Frame Resume after its normal idle delay.
- The automatic screensaver launch resumed the saved photo, too.
- The temporary low-light override used for playback checks was restored afterward.

Tests compared one-way fingerprints recorded after successful display. No personal photos or screenshots were copied to the Mac. Album modifications/removals were tested with synthetic identifiers in JVM tests, rather than changing the user's albums. A full device reboot, factory reset and destructive restore were not performed.
