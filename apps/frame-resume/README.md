# Frame Resume

**New in Portal Smart Display v3.14.0.** A separate customization of [Frame](https://github.com/Ishtiaqhossain/portal-frame) that remembers the last displayed photo and preserves its shuffle queue across leaving Home, reopening the slideshow and restarting the app.

- App version: **1.2.0-resume.1**, Android version code **15**.
- Package: `com.portalhacks.frame.resume`; namespace: `com.portalhacks.frame`.
- Based on Frame 1.2.0, upstream commit `4affee8c57c2b47488ff53755094a4d6e32ad74e`.
- [Install, connect albums, use resume and switch back](../../docs/FRAME-RESUME.md).
- [Download the signed installer](https://github.com/abarui/portal-smart-display/releases/latest/download/Frame-Resume.apk).
- [Build and sign](RELEASING.md) · [Validation](VERIFICATION.md) · [License](LICENSE) · [Notices](NOTICE).

The app installs alongside original Frame. Each has its own uploads and configuration. Shared-album links can be added to both; directly uploaded photos must be uploaded again through Frame Resume's own QR page. No personal albums, uploads or settings are bundled. The sample images are the upstream demonstration assets credited in NOTICE.

Run this directory's Gradle wrapper, independently of the PortalHub project at the repository root:

```sh
./gradlew testDebugUnitTest assembleRelease lintDebug detekt ktlintCheck --no-daemon
```

CI builds an unsigned artifact. Public signed release installers are produced separately without putting the private signing key in GitHub.
