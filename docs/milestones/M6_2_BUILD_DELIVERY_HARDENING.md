# Kisab M6.2 — Build and Delivery Hardening — Design Record

M6.2 makes local verification and GitHub CI run the same authoritative source gates, removes a non-hermetic SDK dependency, and adds secret-free release preflight. It changes no product behavior, domain facts, persistence, secrets, tags, or published releases.

> **Status: COMPLETE** on `feature/m6-2-build-delivery-hardening`, based on merged M6.1 `main`.

## Implemented

- Upgraded from AGP 8.7/external Kotlin to AGP 9.3 built-in Kotlin, matching the existing Gradle 9.6 wrapper and officially supporting API 36/37.
- Added `:app:verifyLocal` as the single local CI-equivalent Gradle gate: JVM tests, lint, debug APK, and Android-test compilation.
- Added deterministic-path JSON evidence with version, toolchain, executed gates, APK size, and SHA-256.
- Added `:app:verifyReleaseMetadata` to validate semantic version form, positive version code, and version-matched release notes without reading signing inputs.
- Added `scripts/release-preflight.sh`: requires `actionlint`, a clean worktree, release metadata, and `verifyLocal`; an optional tag is required to be annotated, version-matched, and contained in `origin/main`.
- GitHub CI explicitly installs API 36 and Build Tools 36, runs `actionlint`, executes `verifyLocal`, and uploads the evidence plus debug APK.
- Release signing now runs the same local gate and metadata validation, uses Build Tools 36 for signature verification, and refuses to create an empty-body draft when release notes are missing.
- Added draft `v0.2.0` notes and reconciled the README with the merged M4–M6.1 state.

## Boundaries

- No connected/emulator or physical-device test is added; manual/device validation remains separately deferred.
- No signing secret is read by local verification or release preflight.
- No tag, signed APK, draft release, or published release is created by M6.2.
- The protected `release-signing` environment and human approval remain mandatory.

## Toolchain decision

AGP 8.x uses a Gradle internal Problems API removed in Gradle 9.6. AGP 9.3 is the stable compatible line for the repository's Gradle 9.6.1 wrapper and supports API level 37, while built-in Kotlin removes the now-redundant `org.jetbrains.kotlin.android` plugin. See the official [AGP 9.3 compatibility record](https://developer.android.com/build/releases/agp-9-3-0-release-notes) and [built-in Kotlin migration guide](https://developer.android.com/build/migrate-to-built-in-kotlin).

## Completion gates

- `actionlint .github/workflows/*.yml`
- `./gradlew :app:verifyLocal`
- `./gradlew :app:verifyReleaseMetadata`
- `bash -n scripts/release-preflight.sh`
- JSON evidence parses and its SHA-256 matches the debug APK.
- `git diff --check`
- GitHub PR CI passes and uploads both expected artifacts.
