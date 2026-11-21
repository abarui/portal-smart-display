# Build and sign Frame Resume

Use JDK 17, Android SDK platform 36, build-tools 36.0.0 and platform-tools. This app has its own Gradle 9.2.1 wrapper; the dashboard at the repository root uses a different build. Run the following inside `apps/frame-resume`:

```sh
./gradlew testDebugUnitTest assembleRelease lintDebug detekt ktlintCheck --no-daemon
```

Without signing configuration, the output is `app/build/outputs/apk/release/app-release-unsigned.apk`. The build and tests do not modify a Portal. Existing upstream R8/Kotlin metadata warnings are documented in [VERIFICATION.md](VERIFICATION.md).

For a stable self-signed build, create and back up your own private key using [the project signing guide](../../docs/BUILD.md). The local, ignored `keystore.properties` file is relative to this directory:

```properties
storeFile=/absolute/private/path/frame-resume-release.p12
storePassword=YOUR_PRIVATE_PASSWORD
keyAlias=YOUR_KEY_ALIAS
keyPassword=YOUR_PRIVATE_PASSWORD
```

Keep that file owner-readable only and outside commits. Alternatively the build accepts `FRAME_KEYSTORE`, `FRAME_KEYSTORE_PASSWORD`, `FRAME_KEY_ALIAS`, and `FRAME_KEY_PASSWORD` environment variables. Never echo their secret values in logs.

With a valid signing configuration, `assembleRelease` creates `app/build/outputs/apk/release/app-release.apk`. Verify it using your SDK's `build-tools/36.0.0/apksigner verify --verbose` command. Only distribute the APK, checksums and reviewed source, never signing inputs or device exports.

Android requires the same signing key for updates to a package. A local debug/self-signed APK cannot update this project's published signed installer unless the keys match. Do not uninstall a populated app or clear its data to bypass that check. The published APK uses a private maintainer key that is not included in the repository, CI or downloads.

When app code changes, increment its `versionCode` and update `versionName` in `app/build.gradle.kts`; rerun the checks and device validation. A project release version can change while this app and the dashboard retain their own component versions. Document all three in the main release notes.
