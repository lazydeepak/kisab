# Kisab Release Policy

## Status

Adopted in preparation for the first Kisab release. Applies to the `com.susankhya.kisab` application and governs every future Kisab release.

## Versioning

- Kisab uses [Semantic Versioning](https://semver.org/) for `versionName`.
- The Android `versionCode` is a strictly increasing, non-negative integer that encodes release order. It is independent of `versionName` and must never decrease between releases.
- The mapping for the first release is `versionCode = 1`, `versionName = "0.1.0"`.
- `0.1.0` remains appropriate for the first Kisab release: no production tags exist yet, all milestones (M0-M3) and release-readiness hardening are merged to `main`, and the pre-1.0 major version signals the documented v1 boundary and known limitations.
- Any future change to `versionCode`/`versionName` must be made explicitly in `app/build.gradle.kts` and reviewed; the values must never change implicitly as a side effect of another change.

## Tags

- Annotated release tags follow the format `v<versionName>`, for example `v0.1.0`.
- Tags are created only from an explicit, reviewed, release-preparation commit, and only after the signing inputs are verified and CI passes on the commit.
- Release tags must point at the exact commit whose contents are being published. No tag is created for a candidate build.
- The release workflow is never triggered directly by a tag push. A `push` event sources the workflow file from the pushed ref itself, so an unmerged tag could carry its own modified workflow and defeat the release gates.
- The signing workflow runs on `repository_dispatch` only. GitHub sources a `repository_dispatch` run from the default branch (`main`), sets `GITHUB_REF`/`GITHUB_SHA` to `main`, and triggers it only if the workflow exists on `main`. A secret-free `Release launcher` workflow provides the manual "Run workflow" UI button and emits the event. Manual `workflow_dispatch` is deliberately not used to reach signing secrets, because a manual run can be started from a selected ref other than `main`.

## Release notes

- Every release ships draft release notes capturing: user-visible milestone features, dependency versions, validation evidence, known limitations, and any data-format compatibility notes.
- Draft notes live under `docs/release/` and are finalized in the GitHub draft release.

## Signing policy

- Production releases are signed with a dedicated release keystore. The Android debug key is never used for release signing.
- Signing inputs are supplied at build time only, from environment variables (locally) or repository secrets (CI). They are never committed, never logged, and never printed.
- The four required inputs are:
  - `KISAB_KEYSTORE_PATH` — path to the release keystore file (locally) or the reconstructed path in CI.
  - `KISAB_KEYSTORE_PASSWORD` — keystore password.
  - `KISAB_KEY_ALIAS` — key alias inside the keystore.
  - `KISAB_KEY_PASSWORD` — private-key password for the alias.
- A release build fails clearly when any of the four inputs is missing or blank. Debug builds never require signing inputs.
- CI runs a three-job flow triggered by `repository_dispatch` (normally via the secret-free `Release launcher` workflow): a secret-free `validate` job first resolves the supplied tag, requires an annotated tag, peels it to a commit, and verifies that commit is contained in `origin/main`; only then does the `build-sign` job (which references the protected `release-signing` GitHub Environment with required approval) check out the validated immutable commit SHA, run tests and version validation, and sign. The validated commit SHA is passed between jobs; the tag is never re-resolved after validation. Because `build-sign` depends on `validate`, no environment approval is ever requested for an invalid or unmerged tag.
- The `release-signing` GitHub Environment must be configured with **Selected branches and tags** permitting only the branch `main` and no tags, administrator bypass disabled, required reviewers enabled, and self-review prevented where possible. GitHub evaluates these deployment restrictions against the run's `GITHUB_REF` (which is `main` for `repository_dispatch`) before the job can start or receive its secrets. `main` itself must be branch-protected so the workflow source cannot be modified outside a reviewed pull request.
- CI decodes the keystore from the `KISAB_KEYSTORE_B64` secret (base64 of the keystore) into a temporary, runner-local path and deletes it at the end of the job.
- The production keystore and its passwords are guarded by the repository owner. They are never committed to the repository, never echoed or logged, and never persisted on CI runners beyond the temporary runner-local keystore path. The base64 keystore form is stored only as a GitHub Actions environment secret.

See `docs/release/RELEASE_NOTES_v0.1.0.md` for the drafted notes and `README.md` for the local signing workflow.
