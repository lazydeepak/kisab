# Kisab Production Release Setup

## Purpose

This guide walks the repository owner through the one-time setup required before the first production release (`v0.1.0`). It covers the protected GitHub Environment, the permanent production keystore, the environment secrets, and the exact steps to run a signed release.

It contains **no real passwords, secret values, or keystore material**. Never paste a real keystore, password, or alias into this file, into an issue, or into a pull request.

Assumptions:

- The `Release` workflow and the secret-free `Release launcher` workflow are already merged on `main`.
- `main` is branch-protected with required pull-request review.
- `versionName = 0.1.0`, `versionCode = 1` in `app/build.gradle.kts`.

## 1. Create the `release-signing` environment

1. Open the repository on GitHub: `https://github.com/lazydeepak/kisab`.
2. Click **Settings** at the top of the repository.
3. In the left sidebar, under **General**, scroll to the **Environments** section and click **Environments** (or click **Settings → Environments**).
4. Click **New environment**.
5. Name it exactly: `release-signing` (case-sensitive; must match the `environment:` name in `.github/workflows/release.yml`).
6. Click **Configure environment**.

## 2. Restrict deployment branches and tags to `main` only

1. In the environment configuration page, find **Deployment branches and tags**.
2. Click **Choose branches or tags** (or the equivalent dropdown).
3. Select **Selected branches and tags**.
4. Add exactly one entry: **main** (the branch).
   - Do **not** add any tag patterns such as `v*`.
5. Click **Save protection rules** (or the page's save button).

Result: GitHub evaluates these restrictions against the run's `GITHUB_REF`, which is `main` for a `repository_dispatch` run. Only runs sourced from `main` can enter this environment.

## 3. Allow no release tags directly through the environment policy

- In the **Deployment branches and tags** rule, ensure only the branch `main` is listed and that **no tag patterns** are present.
- Release tags (`v0.1.0`, ...) are consumed by the workflow's `validate` job (via `repository_dispatch` payload), never via a deployment-branch rule. The environment must not be reachable by a tag push.

## 4. Disable administrator bypass where supported

1. In the environment configuration page, find the **protection rules** section.
2. Locate the **Allow administrators to bypass required approvals** option.
3. Disable it (unchecked / **off**).
4. If the UI exposes a "prevent administrators" or "no admin bypass" toggle, enable it.

Result: even an administrator must pass the environment approval gate before signing secrets are issued.

## 5. Add required reviewers

1. In the same **protection rules** section, find **Required reviewers**.
2. Click **Add reviewers**.
3. Add the release approvers (typically the repository owner and at least one other trusted account).
4. Click **Save protection rules**.

Result: every `build-sign` run requires an explicit manual approval before it can start and receive signing secrets.

## 6. Prevent self-review where supported

- In the required-reviewers area, if the "prevent the last approver from approving their own environment deployment" (self-review) option is available, enable it.
- Self-review is not universally supported in GitHub's UI; if unavailable, compensate by requiring two reviewers so no single owner can both trigger and approve.

## 7. Create the permanent production keystore locally

Use `keytool` (JDK 21) on a trusted machine. This is a **one-time** step; the keystore never changes during the project's lifetime unless rotated deliberately.

```bash
keytool -genkeypair \
  -keystore kisab-release.jks \
  -alias kisab-release \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -dname "CN=Kisab Release, OU=Susankhya, O=Susankhya, C=NP"
```

- The certificate distinguished name becomes part of the permanent signing identity. Use the owner's accurate legal or organizational identity (here a Nepal-based release identity, `C=NP`); do not publish knowingly false placeholders such as `L=Unknown` or `ST=Unknown`. If in doubt, omit `-dname` and let `keytool` prompt interactively for each field.
- Use a **strong, unique** keystore password and a **strong, unique** key password. Do not reuse the debug keystore or the debug key.
- Record the alias (`kisab-release` in the example) and both passwords in a password manager.
- The key password may equal the keystore password, but a separate, strong password is preferred.

## 8. Back up the keystore and passwords safely

1. Copy `kisab-release.jks` to cold storage or an encrypted backup (password manager, encrypted USB drive, offline archive). The keystore **is not recoverable** if lost — a lost keystore means a lost upgrade identity.
2. Store the keystore password, key alias, and key password in the password manager alongside the keystore file.
3. Never commit the keystore, never email it, never paste it into chat/CI logs, and never store it on a shared runner beyond the temporary runner-local path used during a single `build-sign` run.

## 9. Encode the keystore for the environment secret

Generate a base64 form of the keystore. This base64 string becomes the `KISAB_KEYSTORE_B64` environment secret. Treat it like the keystore itself.

macOS / Linux:

```bash
base64 < kisab-release.jks > kisab-release.jks.b64
# inspect without leaking to logs: wc -c kisab-release.jks.b64
```

Windows PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\path\to\kisab-release.jks")) | Out-File -Encoding ascii -NoNewline kisab-release.jks.b64
```

Do not print the base64 value into a terminal you share or into any repository file.

## 10. Add the environment secrets

On the `release-signing` environment configuration page:

1. Scroll to **Environment secrets**.
2. Click **Add secret** (or **New secret**).
3. Add each of these four secrets (names are case-sensitive):

   | Secret | Value |
   | --- | --- |
   | `KISAB_KEYSTORE_B64` | The full base64 string produced in step 9 (paste the single-line value). |
   | `KISAB_KEYSTORE_PASSWORD` | The keystore password. |
   | `KISAB_KEY_ALIAS` | The alias inside the keystore, e.g. `kisab-release`. |
   | `KISAB_KEY_PASSWORD` | The key (private-key) password for the alias. |

4. Save each secret.
5. Confirm the secrets appear under **Environment secrets** and are not visible as plaintext.

The `Release` workflow passes these into the `build-sign` job through `${{ secrets.KISAB_* }}`; they are scoped to the `release-signing` environment and are never echoed or logged.

## 11. Create and push the annotated `v0.1.0` tag

Create the tag **only** after the signing setup is complete and CI is green on the exact commit to be published.

```bash
git fetch origin main
git checkout origin/main
git tag -a v0.1.0 -m "Kisab 0.1.0"
git push origin v0.1.0
```

- Use `git tag -a` (annotated). The workflow's `validate` job **rejects** lightweight tags.
- The tag must point at a commit already contained in `origin/main`; the workflow refuses to sign an unreviewed commit.

## 12. Run the `Release launcher` workflow from `main`

1. On GitHub, open **Actions**.
2. In the left sidebar, select the **Release launcher** workflow.
3. Click **Run workflow**.
4. In **Branch**, select `main` (the launcher must be run from `main`).
5. In **Annotated release tag to build and sign**, enter `v0.1.0`.
6. Click **Run workflow**.

The launcher (secret-free) emits a `repository_dispatch` event. The `Release` workflow:

- `validate` — resolves `v0.1.0`, requires it to be annotated, peels it to a commit, verifies it is contained in `origin/main`, and confirms the tag did not move. No code from the tag is executed.
- `build-sign` — requests environment approval (required reviewers) on the `release-signing` environment, then checks out the validated immutable commit, runs tests/lint/version checks, signs the APK, verifies the signature, and computes the SHA-256.
- `create-draft-release` — creates a **draft** GitHub release at `v0.1.0` with the release notes and the signed APK plus checksum.

## 13. Review before publishing

In the **draft** release:

1. Confirm the **tag** is `v0.1.0` and the **title** reads `Kisab v0.1.0`.
2. Confirm the **target** is the exact commit used to sign.
3. Confirm the **release notes** match `docs/release/RELEASE_NOTES_0.1.0.md`.
4. Download the attached `*-release.apk` and verify the checksum against the attached `.sha256` file.
5. Verify the certificate is the production release certificate (not the debug key), e.g. with:

   ```bash
   $ANDROID_SDK_ROOT/build-tools/35.0.0/apksigner verify --verbose --print-certs app-release.apk
   ```

Only then click **Publish release**. The workflow never auto-publishes; publication is a deliberate owner action.

## Stop conditions

Abort the release (do not approve `build-sign`, do not publish) if any of these occur:

- The tag is a lightweight tag (not annotated).
- The tag does not point at a commit contained in `origin/main`.
- The tag name does not match `v<versionName>` from the packaged APK (i.e. not `v0.1.0`).
- Tests fail or lint reports errors during the `build-sign` job.
- The signed APK is unsigned, or its certificate is not the production certificate.
- The reported SHA-256 does not match the attached `.sha256` file.
- The workflow runs from a non-`main` trust context (e.g. a manual dispatch from another branch, which the design forbids).

See `docs/release/RELEASE_POLICY.md` for the versioning, tag, and signing policy, and `docs/release/V0.1.0_RELEASE_CHECKLIST.md` for the pre-release checklist. Draft notes live at `docs/release/RELEASE_NOTES_0.1.0.md`.
