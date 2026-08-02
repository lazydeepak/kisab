# Kisab v0.1.0 — Draft Release Notes

> Status: DRAFT. This document is prepared for the first Kisab release. It is not yet attached to a tag or GitHub release. Finalize the text, copy it into the GitHub draft release, and then publish the release explicitly.

## Summary

Kisab `v0.1.0` is the first release of the standalone offline farm-management Android application. It records a single farm, its livestock/crop entries, and a transaction history, entirely offline, and supports versioned backup/restore through Android's document picker.

## Milestone features

### M1 — Offline farm management
- Launchable Android app for a single local farm.
- Create and reopen a locally persisted farm.
- Add livestock/crop entries and record signed-amount transactions.
- View entry count, transaction count, and balance.
- Data survives app/process recreation via versioned local persistence.

### M2 — Transaction model hardening
- Stable transaction identifiers and explicit income/expense types.
- Transaction categories constrained by type; money stored in minor units with an ISO currency code.
- Explicit transaction timestamps with full-model validation and edit/delete flows with destructive-action confirmation.
- Transaction history rendered newest-first by timestamp with a deterministic tie-breaker for equal timestamps.
- Versioned persistence migration from the legacy M1 format.

### M3 — Offline backup and restore
- Export and restore a single farm entirely offline.
- Versioned backup envelope with a deterministic UTC export time.
- Android Storage Access Framework/document-picker integration without broad filesystem permissions.
- Full pre-restore validation; the current farm is preserved on cancelled or invalid imports.
- Explicit overwrite confirmation before state replacement.

## Dependencies

- `com.susankhya.foundation:foundation-session-android:0.1.1` (from GitHub Packages; pinned)
- `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0`
- `androidx.appcompat:appcompat:1.7.0`
- `androidx.activity:activity-ktx:1.9.0`

## Validation

- JVM unit tests: 33 tests passing (domain, persistence, migration, and backup regression coverage).
- Android instrumentation tests: 13 tests passing (API 36) covering the full journey, persistence recreation, backup restore survival, and rejected-backup preservation.
- Android Lint: 0 errors (known warning classes: Autofill, GradleDependency, ButtonStyle, ApplySharedPref, MissingApplicationIcon, SetTextI18n).
- Release signing pipeline verified with a disposable test keystore; the permanent production keystore must be created and guarded by the repository owner and is not part of this release.

## Known limitations

- Single-farm scope; no multi-farm management, multi-tenant administration, or cloud sync.
- No full accounting engine, broad reporting, or analytics.
- Legacy M1 persisted data migrates to the current schema on load; M1-format backup envelopes are not produced.
- Release artifacts are signed but the release (tag, draft release, APK publication) is intentionally not created in this preparation step.

## Backup-format compatibility

- Backup envelope schema version: `1` (field-separated text envelope with base64 payload).
- Persistence schema version: `2`.
- Backups carry only farm-domain state; they never include session credentials, device keys, preferences, or unrelated application state.
- Restores are rejected (current farm preserved) for malformed envelopes, non-positive money, invalid category/type combinations, malformed timestamps, and unsupported envelope versions.

## Install / build

- `minSdk 26`, `targetSdk 36`.
- Debug: `./gradlew :app:assembleDebug`.
- Signed release: see `docs/release/RELEASE_POLICY.md` for the signing-input contract.
