# Kisab

A standalone Android application repository that consumes `com.susankhya.foundation:foundation-session-android:0.1.1` from GitHub Packages.

## Build and test

Prerequisites:
- JDK 21
- Android SDK with API 35 installed
- a GitHub token with `read:packages` access for `https://maven.pkg.github.com/lazydeepak/susankhya-app-foundation`

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
- Preserve local farm data through versioned persistence migration and cover the model with unit, persistence, migration, and Android integration tests.

See the documentation in `docs/charter/`, `docs/architecture/`, and `docs/decisions/` for the charter, v1 boundary, and architecture decision record.
