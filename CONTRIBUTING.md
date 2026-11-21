# Contributing

Open an issue describing the problem and intended behavior, or submit a focused pull request. This is a customization of PortalHub: preserve upstream credit and keep compatibility changes explicit.

Use JDK 17 and the checked-in Gradle wrapper. Before submitting code:

```bash
./gradlew testDebugUnitTest assembleRelease lintRelease --no-daemon
```

Frame Resume is a separate Gradle build. For changes under `apps/frame-resume`, also run:

```bash
cd apps/frame-resume
./gradlew testDebugUnitTest assembleRelease lintDebug detekt ktlintCheck --no-daemon
```

Add tests for meaningful parsing, authorization, data-loss and lifecycle changes. Test UI changes on an actual device in portrait and landscape when possible; state when only compilation/JVM tests were available. Do not claim playback from an app merely opening or signing in.

Use fictional calendar/member data and placeholder URLs. Keep screenshots free of personal events and photos. Never submit APK signing keys, password files, app exports, device backups or third-party account sessions. See [SECURITY.md](SECURITY.md).

Changes to provider integrations should document authentication, permissions, refresh behavior and failure modes. Avoid silently widening LAN access or conflating temporary calendar pairing with persistent shopping access. Multi-Portal syncing needs an explicit authenticated design before it can be re-enabled in this fork.

Describe what changed, why, how it was tested and any relevant limitations. Contributions are under the repository's MIT license; retain applicable third-party notices.
