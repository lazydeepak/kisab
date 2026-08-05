# Kisab

A standalone Android application repository that consumes `com.susankhya.foundation:foundation-session-android:0.1.1` from GitHub Packages.

## Build and test

Prerequisites:
- JDK 21
- Android SDK with API 36 installed
- a GitHub token with `read:packages` access for `https://maven.pkg.github.com/lazydeepak/susankhya-app-foundation`

Create a machine-local `local.properties` from `local.properties.example` and point `sdk.dir` at your Android SDK. The file is gitignored and never committed; CI generates its own copy from `ANDROID_SDK_ROOT`.

Set the token locally before building:

```bash
export GITHUB_ACTOR=lazydeepak
export GITHUB_TOKEN=<read:packages-token>
```

Run the validation suite:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
./gradlew :app:dependencies --configuration debugRuntimeClasspath
```

## Milestone status
- Kisab M0 is complete. The repository now contains the documented product scope, architecture, and a farm-domain vertical slice grounded in the product boundary.
- Kisab M1 is complete. It implements an offline farm-management flow with a launchable Android UI, local persistence, deterministic domain operations, and automated coverage for the complete journey.
- Kisab M2 is complete. It hardens the transaction model with stable IDs, explicit transaction types and categories, minor-unit money with currency codes, explicit timestamps, validation, and create/edit/delete flows.
- Kisab M3 is complete. It adds fully offline single-farm backup/restore using a versioned backup envelope, Android document picker integration, full pre-restore validation, and destructive overwrite confirmation.
- Kisab `v0.1.0` is published and verified. The annotated tag points to a commit contained in `main`, and release workflow run `30750947492` produced the production-signed APK that passed tests, lint, tag/version validation, Android APK signature verification using v2 signing, and independent checksum verification. The GitHub release is published. See `docs/release/RELEASE_NOTES_0.1.0.md` for the full record.
- Kisab M4 is the active milestone: **IN PROGRESS**. M4 turns the technically correct offline `v0.1.0` application into a product that Nepali farmers can understand and use with minimal developer assistance — Android string-resource and Nepali localization, Nepal-oriented currency/number/date/time presentation, first-run and daily-entry usability, and physical-device field validation. M4-01 (all user-facing text resource-backed plus an initial Nepali `values-ne/` set) is complete; M4 does not add cloud, accounts, multi-farm, or payments. See `docs/milestones/M4_FIELD_VALIDATION_AND_NEPAL_USABILITY.md` for the full definition and `docs/validation/M4_01_LOCALIZATION_VALIDATION.md` for the M4-01 validation record.

## Kisab M1 acceptance criteria
- Launch a usable Android app from a launcher activity.
- Create and reopen one locally stored farm without depending on the foundation for farm-domain semantics.
- Add livestock or crop entries and record signed-amount transactions.
- View entry count, transaction count, and balance.
- Preserve farm data across app/process recreation.
- Cover the complete journey with unit and Android integration tests.

## Kisab M2 acceptance criteria
- Support stable transaction identifiers and explicit income/expense types.
- Constrain categories by transaction type and store money in minor units with an ISO currency code.
- Store explicit transaction timestamps, validate the complete transaction model, and support edit/delete flows with destructive-action confirmation.
- Present transaction history newest-first by timestamp with a deterministic tie-breaker for equal timestamps.
- Preserve local farm data through versioned persistence migration and cover the model with unit, persistence, migration, and Android integration tests.

## Kisab M3 acceptance criteria
- Export and restore a single farm entirely offline with a versioned backup envelope.
- Use Android's Storage Access Framework/document picker for backup files without broad filesystem permissions.
- Validate the complete backup before replacing current state and confirm overwrites explicitly.
- Preserve the current farm on cancelled or invalid imports and cover the flow with unit and Android integration tests.

## Release

- Versioning and release policy: `docs/release/RELEASE_POLICY.md`.
- Published and verified release record for `v0.1.0`: `docs/release/RELEASE_NOTES_0.1.0.md`.

Building a signed release locally requires four environment variables — `KISAB_KEYSTORE_PATH`, `KISAB_KEYSTORE_PASSWORD`, `KISAB_KEY_ALIAS`, and `KISAB_KEY_PASSWORD` — pointing at your release keystore. These are never committed or logged; `assembleRelease` fails clearly if any is missing. Debug builds do not require them.

```bash
export KISAB_KEYSTORE_PATH=/absolute/path/to/release.keystore
export KISAB_KEYSTORE_PASSWORD=...
export KISAB_KEY_ALIAS=...
export KISAB_KEY_PASSWORD=...
./gradlew :app:assembleRelease
```

CI signs releases through the `Release` workflow, which runs on `repository_dispatch` (never on tag push, and never on manual `workflow_dispatch` — a manual run could be started from a selected branch). GitHub sources `repository_dispatch` runs from the default branch `main` and sets `GITHUB_REF` to `main`, so the workflow that reaches signing secrets is always the trusted, reviewed `main` version. Start a release from the secret-free `Release launcher` workflow ("Run workflow"), which emits the event. A secret-free `validate` job verifies the supplied tag is annotated and points at a commit contained in `origin/main`; the `build-sign` job then checks out that validated commit SHA and signs using the `KISAB_KEYSTORE_B64` and password/alias secrets stored on the protected `release-signing` GitHub Environment (configured to permit only the `main` branch, with required reviewers). The keystore is never committed to the repository, never echoed or logged, and is reconstructed only into a temporary runner-local path that is deleted when the job ends. Its base64 form is stored as a GitHub Actions environment secret, which is guarded by the repository owner.

See the documentation in `docs/charter/`, `docs/architecture/`, `docs/decisions/`, and `docs/release/` for the charter, v1 boundary, architecture decision record, and release policy.
