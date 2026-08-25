# Kisab

A standalone Android application repository that consumes `com.susankhya.foundation:foundation-session-android:0.1.1` from GitHub Packages.

![Kisab ledger-and-sprout logo](docs/brand/kisab-logo.svg)

## Build and test

Prerequisites:
- JDK 17 or newer (CI uses JDK 21)
- Android SDK with API 36 installed
- a GitHub token with `read:packages` access for `https://maven.pkg.github.com/lazydeepak/susankhya-app-foundation`

Create a machine-local `local.properties` from `local.properties.example` and point `sdk.dir` at your Android SDK. The file is gitignored and never committed; CI generates its own copy from `ANDROID_SDK_ROOT`.

Set the token locally before building:

```bash
export GITHUB_ACTOR=lazydeepak
export GITHUB_TOKEN=<read:packages-token>
```

Run the complete local CI-equivalent gate:

```bash
./gradlew :app:verifyLocal
```

It runs JVM tests, lint, debug assembly, and Android-test compilation, then writes machine-readable evidence to `app/build/reports/verification/local-ci-evidence.json`. GitHub CI runs the same task and retains that report with the debug APK.

Before preparing a release tag, install `actionlint` and run the secret-free release preflight from a clean worktree:

```bash
scripts/release-preflight.sh
# Once an annotated version tag exists locally:
scripts/release-preflight.sh v0.2.0
```

## Project status

Current work and stage: `docs/CURRENT.md`. Milestone records and dispositions: `docs/milestones/`, with validation evidence under `docs/validation/`. Deferred work: `docs/BACKLOG.md`. Agent operating contract: `AGENTS.md`.

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
- Draft notes for the next candidate: `docs/release/RELEASE_NOTES_0.2.0.md`.

Building a signed release locally requires four environment variables — `KISAB_KEYSTORE_PATH`, `KISAB_KEYSTORE_PASSWORD`, `KISAB_KEY_ALIAS`, and `KISAB_KEY_PASSWORD` — pointing at your release keystore. These are never committed or logged; `assembleRelease` fails clearly if any is missing. Debug builds do not require them.

```bash
export KISAB_KEYSTORE_PATH=/absolute/path/to/release.keystore
export KISAB_KEYSTORE_PASSWORD=...
export KISAB_KEY_ALIAS=...
export KISAB_KEY_PASSWORD=...
./gradlew :app:assembleRelease
```

CI signs releases through the `Release` workflow, which runs on `repository_dispatch` (never on tag push, and never on manual `workflow_dispatch` — a manual run could be started from a selected branch). GitHub sources `repository_dispatch` runs from the default branch `main` and sets `GITHUB_REF` to `main`, so the workflow that reaches signing secrets is always the trusted, reviewed `main` version. Start a release from the secret-free `Release launcher` workflow ("Run workflow"), which emits the event. A secret-free `validate` job verifies the supplied tag is annotated and points at a commit contained in `origin/main`; the `build-sign` job then checks out that validated commit SHA and signs using the `KISAB_KEYSTORE_B64` and password/alias secrets stored on the protected `release-signing` GitHub Environment (configured to permit only the `main` branch, with required reviewers). The keystore is never committed to the repository, never echoed or logged, and is reconstructed only into a temporary runner-local path that is deleted when the job ends. Its base64 form is stored as a GitHub Actions environment secret, which is guarded by the repository owner.

See the documentation in `docs/charter/`, `docs/architecture/`, `docs/decisions/`, and `docs/release/` for the charter, v1 boundary, architecture decision records, and release policy. The accepted-but-frozen post-`v0.2.0` multiplatform direction is recorded in `docs/decisions/ADR-0002-post-v0.2.0-multiplatform-direction.md`; agent workflow and validation-depth rules are recorded in `docs/decisions/ADR-0003-agent-workflow-and-validation-depth.md`.
