# Attribution and third-party notices

## Upstream application

This repository derives from [PortalHub / portal-calendar](https://github.com/thefloppytaco/portal-calendar) by **thefloppytaco**, at commit `4642e4e7b41cdbb30d54dd5455da852f2d23ea67`. Its original MIT notice is retained in [LICENSE](LICENSE). The dashboard design, Home/app navigation, phone subscription pairing, separately authorized shopping/photo sharing, AIR radio controller, weather/camera screens and Pandora shortcut reflect this fork's customizations. This is not a claim of original authorship of the underlying PortalHub application.

## Frame Resume companion (source and customized release included)

[`apps/frame-resume`](apps/frame-resume/) is a customization of [Frame / portal-frame](https://github.com/Ishtiaqhossain/portal-frame), copyright **2026 Ishtiaq Hossain**, based on commit `4affee8c57c2b47488ff53755094a4d6e32ad74e` (Frame 1.2.0). Its [MIT license](apps/frame-resume/LICENSE) and [NOTICE](apps/frame-resume/NOTICE) are retained, including attribution for vendored ZXing and upstream sample photos. The custom changes add persistent slideshow position/order and the separate `com.portalhacks.frame.resume` package. The signed Frame Resume binary is distributed as this project's modified build, not as an official upstream or Meta release.

## Other companion applications (not bundled)

- [Transistor](https://github.com/y20k/transistor), by Y20K.org and contributors: separate radio player.
- [Home Assistant Android](https://github.com/home-assistant/android): separate companion app for a Home Assistant server.
- [Immortal](https://github.com/starbrightlab/immortal): separate community Portal launcher/provisioning project.

Those projects have their own licenses and release policies. Pandora, Spotify and Google applications/services are separate products; their binaries and logos are not redistributed in this repository. Broadcaster names identify public streams; the examples do not redistribute audio or imply affiliation.

## Build/runtime dependencies

Gradle resolves NanoHTTPD 2.3.1, ZXing core 3.5.3, biweekly 0.6.8, OkHttp 4.12.0 and their transitive dependencies. JUnit 4.13.2 is used for tests. See `app/build.gradle.kts` for the authoritative dependency list. Preserve the licenses/notices supplied with these dependencies when redistributing builds. The Gradle wrapper and Android build tools remain subject to their own licenses; SDK tools are not copied into this repository.

Weather data uses Open-Meteo. Calendar fetching contacts the user's chosen calendar provider. Optional upstream integrations may contact their configured service providers. No service API credentials are included.

Frame Resume has its own Compose/AndroidX dependencies, version catalog and upstream notices under `apps/frame-resume/`; retain those when redistributing its source or installer.
