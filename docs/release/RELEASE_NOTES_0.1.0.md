# Kisab v0.1.0 — Release Record

## Status

Released. The annotated tag `v0.1.0` exists and points at commit `b1d4fde`, which is contained in `origin/main`. The release workflow run `30750947492` completed successfully: its `validate`, `build-sign`, and `create-draft-release` jobs all passed. The `build-sign` job produced a production-signed release APK, ran the full JVM test suite and lint, verified that the tag matches the packaged `versionName`, and verified the APK signature with `apksigner`. The signed APK and its `.sha256` checksum were attached to the GitHub draft release `Kisab v0.1.0` at the tag.

Per release policy, the GitHub release record is created as a draft and publication is an explicit owner action. No publication date, download count, or user count is recorded here.

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
- Release signing: the production release APK passed Android APK signature verification with `apksigner verify --print-certs` — `Verified using v2 scheme (APK Signature Scheme v2): true`, signer certificate SHA-256 `92a578e8cedad6ea86d2dc27663a3279f07a70794627a280f877ab30b1f89cff` (RSA). The APK is signed with the production certificate, not the debug key.
- Release workflow run: `30750947492` (jobs `validate`, `build-sign`, and `create-draft-release` all succeeded).
- APK SHA-256: `990c100980c469c9411fb7dc66747d0286a3c8020f7d0c8acca949b7e43bd7bc`. This matches the value recorded by the workflow and independently re-verified against the shipped artifact.

## Known limitations

- Single-farm scope; no multi-farm management, multi-tenant administration, or cloud sync.
- No full accounting engine, broad reporting, or analytics.
- Legacy M1 persisted data migrates to the current schema on load; M1-format backup envelopes are not produced.
- The GitHub release record was created as a draft by the workflow and awaits the owner's explicit publication action.

## Backup-format compatibility

- Backup envelope schema version: `1` (field-separated text envelope with base64 payload).
- Persistence schema version: `2`.
- Backups carry only farm-domain state; they never include session credentials, device keys, preferences, or unrelated application state.
- Restores are rejected (current farm preserved) for malformed envelopes, non-positive money, invalid category/type combinations, malformed timestamps, and unsupported envelope versions.

## Install / build

- `minSdk 26`, `targetSdk 36`.
- Debug: `./gradlew :app:assembleDebug`.
- Signed release: see `docs/release/RELEASE_POLICY.md` for the signing-input contract.
