# Build, test and sign

## Toolchain

- JDK 17 (used for the verified build).
- Android SDK platform 36, build-tools 36.0.0 and platform-tools.
- The included Gradle 9.4.1 wrapper and Android Gradle Plugin 9.2.1.
- Internet access for dependencies on the first build, or an existing dependency cache.

Point `ANDROID_HOME` at your SDK and `JAVA_HOME` at JDK 17. Alternatively set `sdk.dir` in a local, ignored `local.properties`. On macOS, `export JAVA_HOME=$(/usr/libexec/java_home -v 17)` selects an installed JDK 17. On other systems use your JDK installation path.

```bash
./gradlew clean testDebugUnitTest assembleRelease lintRelease --no-daemon
```

The release output is `app/build/outputs/apk/release/app-release-unsigned.apk`. The app uses API 28 as its minimum/target baseline for the older Portal environment and compiles against API 36. It is distributed by sideloading, not as a current Google Play submission.

Unit tests cover subscription URL validation, pairing expiration, separate shopping access, single-photo sharing/validation and inherited CalDAV parsing/HTTP behavior. Tests requiring an external Radicale fixture server skip unless that environment is supplied. A passing JVM suite does not prove camera, audio or orientation behavior on every Portal model.

## Personal development builds

```bash
./gradlew assembleDebug --no-daemon
```

The debug APK is signed with your local Android debug key. It is suitable for local development, not a stable public release identity. It will not update an app signed with a different key. Do not uninstall a populated installation merely to test a debug build.

## Stable release signing

Create a key **outside Git**, keep it privately backed up and reuse it for every update you distribute. Example (keytool prompts for the passwords):

```bash
mkdir -p private
keytool -genkeypair -keystore private/release.p12 -storetype PKCS12 -alias portal-release -keyalg RSA -keysize 3072 -validity 10000 -dname "CN=Portal Smart Display Community"
```

Place the password in an owner-readable file such as `private/release-password.txt`; do not paste it into an issue or commit it. Then:

```bash
python3 scripts/sign-release.py --keystore private/release.p12 --password-file private/release-password.txt --alias portal-release
```

The script aligns and signs the unsigned build into `dist/Portal-Family-Home.apk`, verifies its signature and writes `dist/SHA256SUMS`. It never installs to a device. The script requires Python 3.11+ and the SDK build-tools path from `ANDROID_HOME` or `ANDROID_SDK_ROOT`.

The project's published installer, when present, has its own retained private signing identity. Its key is not in the repository, CI or release downloads. A self-signed build and the project's binary cannot update each other unless they share the same key; Android checks that by design.

## Frame Resume build

Frame Resume lives in `apps/frame-resume` and uses its own Gradle 9.2.1 wrapper and Android Gradle Plugin 8.13.2. Run its wrapper from that directory. See [Frame Resume build/signing](../apps/frame-resume/RELEASING.md) for all checks and stable signing. The root signing helper above is for the Home Hub APK only.

## Maintain a release

1. Increment `versionCode` and set `versionName` in each changed app's `app/build.gradle.kts`. Keep unchanged component versions stable and distinguish them from the project release/tag in the release notes.
2. Run the tests, release build and lint; record which device features were exercised.
3. Review all staged content for private calendar URLs, tokens, credentials, photos, device IDs and local paths. Never package a user's backup directory.
4. Sign locally, verify the certificate and APK checksum, then attach only the intended APK and checksum to the corresponding GitHub release.
5. Keep the source commit/tag and release notes aligned with the binary. Do not include third-party commercial installers.

CI builds and tests source without access to the maintainer's release key. Its unsigned artifact is not a signed installable release.
