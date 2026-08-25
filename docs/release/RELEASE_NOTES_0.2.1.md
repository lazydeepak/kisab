# Kisab v0.2.1 — Release Notes & First Pilot OTA Target

> **Pilot OTA Target.** Governed by `docs/release/RELEASE_POLICY.md` and published through the Kisab Pilot Update Channel. Delivered as an in-place over-the-air upgrade from v0.2.0 with signer continuity and data preservation.

## User-visible changes since v0.2.0

- **Active Pilot Update Channel**: The signed v0.2.1 build embeds the real public HTTPS update manifest URL. The app periodically checks the channel over HTTPS, verifies the SHA-256 of the downloaded APK, and performs an in-place signed upgrade with Android OS signer continuity.
- **Over-the-Air Upgrade Path**: First OTA target delivered to pilot devices on `ZA22374XPC`; existing pilot data (farms, Khata parties, transactions, settings, language, appearance) preserved across the upgrade.

## Build and delivery changes

- versionCode bumped to `4`, versionName to `0.2.1` (explicit, reviewed change per `RELEASE_POLICY.md`).
- Release signing via the protected `RC sign` workflow against the frozen candidate commit.
- Signed APK hosted as a GitHub Releases asset; update manifest hosted as a static HTTPS JSON document.

## Data compatibility

- Current farm persistence schema: v12 (unchanged from v0.2.0).
- Backup envelope compatibility unchanged; no schema migration required for this drop.

## Verification evidence

- **Unit Tests**: 379/379 tests passing.
- **Lint**: 0 errors.
- **On-Device OTA**: In-place upgrade from v0.2.0 (code 3) to v0.2.1 (code 4) verified on Motorola Edge 60 Fusion (`ZA22374XPC` / Android 16), including SHA-256 integrity and data preservation.
- **Protected State**: `RC01UpgradeFarm` verified untouched and intact after the upgrade.