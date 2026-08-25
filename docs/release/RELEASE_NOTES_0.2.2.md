# Kisab v0.2.2 — Release Notes (Draft)

> **Status: DRAFT — not published.** This candidate becomes releasable only after the protected `Release`/`RC sign` pipeline produces and validates the production-signed APK. Governed by `docs/release/RELEASE_POLICY.md`.

## User-visible changes since v0.2.1

- **Update integrity hardening**: APK downloads are now checksum-verified in a single streaming pass while being written to disk, instead of loading the whole installer into memory before verification. Behavior is unchanged for farmers: a mismatched download is discarded and never reaches the installer.
- No new farming/accounting features in this drop; v0.2.2 is a release-hardening pilot target.

## Build and delivery changes

- versionCode bumped to `5`, versionName to `0.2.2` (explicit, reviewed change per `RELEASE_POLICY.md`; strictly increasing from code 4).
- M12 instrumentation baseline repair included: the connected device suite is fully green (was 33 known failures), so release gating now covers on-device behavior end to end.

## Data compatibility

- Farm persistence schema: v14 (unchanged within this drop). Upgrades from the signed v0.2.1 pilot build (schema v12) exercise the existing idempotent v12 → v13 → v14 store migrations at first launch; no record is discarded.
- Backup envelope compatibility unchanged.

## Verification evidence

- Filled from M13 evidence when complete: see `docs/validation/M13_PILOT_RELEASE_OTA_VALIDATION.md`.
