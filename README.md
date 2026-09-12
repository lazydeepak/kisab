# Kisab

Kisab is an offline-first Android farm ledger for smallholders: record farm work, production, cash income/expenses, sales, purchases, and payments; review Khata and summaries without requiring login. Farm data and business rules belong to Kisab. The app consumes `com.susankhya.foundation:foundation-session-android:0.1.1` from GitHub Packages.

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

It runs JVM tests, lint, debug assembly, and Android-test compilation, then writes machine-readable evidence to `app/build/reports/verification/local-ci-evidence.json`. This compiles instrumentation tests but does not execute them on a device. GitHub CI runs the same task and retains that report with the debug APK.

Before preparing a release tag, install `actionlint` and run the secret-free release preflight from a clean worktree:

```bash
scripts/release-preflight.sh
# Once an annotated version tag exists locally:
scripts/release-preflight.sh v0.2.0
```

## Project overview and documentation index

Use [current status and QA](docs/CURRENT.md) for the implementation baseline, release evidence, and pending work. Code, tests, build configuration, and Git history take precedence over summaries.

| Document | Authority / use |
| --- | --- |
| [Product charter](docs/charter/Kisab-Product-Charter.md) | Goal, current scope, ownership boundary and architecture overview |
| [Current work](docs/CURRENT.md) | Current status, verification limits and active workstreams |
| [Backlog](docs/BACKLOG.md) | Deferred work and pilot observations awaiting evidence |
| [Boundary ADR](docs/decisions/ADR-0001-kisab-v1-boundary.md) | Product/foundation ownership decision |
| [Multiplatform ADR](docs/decisions/ADR-0002-post-v0.2.0-multiplatform-direction.md) | Accepted but frozen direction; not an implemented backend |
| [Validation-depth ADR](docs/decisions/ADR-0003-agent-workflow-and-validation-depth.md) | Maintainer acceptance and proportional validation |
| [Account linking](docs/architecture/Kisab-Account-Linking-Foundation.md), [online session](docs/architecture/Kisab-Online-Account-Session.md), [push foundation](docs/architecture/Kisab-Push-Notification-Foundation.md) | Current client contracts and explicitly deferred integrations |
| [Nepali terminology](docs/localization/NEPALI_TERMINOLOGY.md) | Translation reference; human review remains separate from resource checks |
| [Release policy](docs/release/RELEASE_POLICY.md), [pilot update channel](docs/release/PILOT_UPDATE_CHANNEL.md) | Signing/version rules and Android HTTPS update operations |
| [Agent contract](AGENTS.md) | Repository work and preservation rules |

`docs/milestones/`, `docs/design/`, and `docs/validation/` retain scoped specifications, implementation records and historical evidence. M0–M3 architecture documents describe the initial slices, not the full current feature set. Release notes/checklists describe their named artifacts, not every later commit. Historical acceptance criteria remain in those records; they are not the current task queue.

## Release

- Versioning and release policy: `docs/release/RELEASE_POLICY.md`.
- Published and verified release record for `v0.1.0`: `docs/release/RELEASE_NOTES_0.1.0.md`.
- Latest recorded pilot release: [v0.2.2](docs/release/RELEASE_NOTES_0.2.2.md), with [M13 signed-release/OTA evidence](docs/validation/M13_PILOT_RELEASE_OTA_VALIDATION.md). Current source still declares 0.2.2/code 5 but includes later changes; matching version numbers do not establish artifact identity.
- Earlier v0.2.0/v0.2.1 notes and RC checklists remain historical records.

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
