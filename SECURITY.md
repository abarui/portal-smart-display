# Security and privacy

This is a community application for older Portal hardware. It has not had an independent security audit. Use it on a trusted home network; do not expose its HTTP services through public port forwarding or an unauthenticated reverse proxy.

## Data boundaries

- `127.0.0.1:8090`: full management/export/import API, unauthenticated but bound to the Portal's loopback interface. Access from a computer uses an explicitly authorized ADB forward. Other software running on the Portal can potentially reach loopback services.
- Port `8091`: a limited phone-facing HTTP interface. Calendar pairing and individual photo export use temporary bearer capabilities; shopping uses a separate persistent, revocable token. A person with a valid token and network access has its corresponding capability. HTTP on the LAN is not end-to-end encrypted against a network observer.
- Calendar feeds, optional account credentials and family data live in app-private storage. The configuration export includes sensitive values and is **not encrypted**. Android automatic app backup is disabled.
- Camera photos are app-private and exported only through the selected-photo mechanism. The supplied backup helper excludes photo files.
- Weather uses the chosen city coordinates. Subscription fetching sends requests to the selected provider. Optional AI/voice/account integrations have their own external data flows and should be configured only deliberately.

The QR subscription interface restricts providers and URL shapes. This is not a complete guarantee against every possible redirect/provider/network attack; the more powerful local management API is intended only for trusted operators. A dashboard editing PIN protects UI actions, not all backend or Android-level access.

## Reporting a vulnerability

Use GitHub's **private vulnerability reporting** control if it is available on this repository. If not, open an issue asking for a private reporting channel without including exploit details or personal data. Do not post calendar URLs, photo/album links, exports, auth tokens, private images, credentials or signing keys in issues or discussions. Use synthetic data for reproductions.

## Maintaining public releases

Publish only reviewed source, generic documentation, the intended app APK and its checksum. Keep private device backups and signing material outside the repository and its history. An APK signature does not include the private signing key; never upload the keystore itself. Do not add analytics or broader data access without explaining the resulting data flows to users.
