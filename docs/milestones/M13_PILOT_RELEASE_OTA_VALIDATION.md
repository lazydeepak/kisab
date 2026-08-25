# Kisab M13 — Pilot Release and OTA Validation

## Status

**M13 FINAL DISPOSITION: PASS**

Implemented on `main` on top of M12 at `3526e97` (code hardening landed via `fd37aed`, evidence records via `2410be3`). This record is the authority for the M13 scope: proving the complete private-release lifecycle end to end — release artifact identity, OTA update pipeline, data preservation across real in-place upgrades, negative-path fail-safety, expiry/update interaction, and release workflow/preflight audit — as a **release-hardening milestone** with no product features. The production-signed gate was executed under owner authorization: tag `v0.2.2` → protected `Release` workflow run `32611327982` → verified APK `6463fe86…7a94` (cert `92a578e8…`) → live manifest published → physical 0.2.1→0.2.2 OTA with full preservation.

Validation evidence: `docs/validation/M13_PILOT_RELEASE_OTA_VALIDATION.md`.

## 1. Problem being solved

The pilot update channel existed on paper and was validated once (v0.2.0→v0.2.1), but nothing re-proved it against current code, no upgrade path existed for the next drop, and the release/expiry interaction had never been exercised as a matrix. M13 turns the lifecycle into repeatable evidence.

## 2. Ground-truth findings (audited before changing anything)

- Release signing fails safely without the four `KISAB_*` inputs; CI signs only via `repository_dispatch` from a frozen candidate SHA through the protected `release-signing` environment, verifies signer continuity against the published v0.1.0 certificate (`92a578e8…`), and writes the checksum of the final signed APK.
- The OTA client enforces HTTPS-only URLs, strict manifest validation (positive versionCode, non-blank name, HTTPS apkUrl, required 64-hex sha256 → otherwise fail-safe null), strictly-greater version comparison, download-rejected-without-checksum, digest verified before installer handoff with the file deleted on mismatch, FileProvider content URI with matching authorities/paths, and an archive package-name check.
- Expiry blocks exactly the mutation surface (27 guard call sites); backup/export and the update path are never gated; the clock floor persists separately from farm data; day math is whole UTC days with device-zone display keys.
- One documentation drift found: the unconfigured-channel fallback copy in `PILOT_UPDATE_CHANNEL.md` did not match the shipped string — corrected.

## 3. Version decision

OLD = signed v0.2.1 (code 4). Upgrade requires NEW > 4; smallest justified step per policy: **v0.2.2 / code 5**, explicit in `app/build.gradle.kts`, with draft notes required by `verifyReleaseMetadata`. RC-03's commit `18d5f5f2…` was confirmed an ancestor of HEAD, so the OLD debug rebuild could be assembled from the exact historical source via a pinned git worktree.

## 4. Security boundary honored

Per the owner's execution rule: no keystore material or passwords entered this environment. All mechanism validation used debug builds (debug-key continuity still exercises real Android signature enforcement). The production-signed gate is deliberately deferred: push → reviewed merge to main → freeze candidate SHA (pattern of PRs #35/#38) → annotated tag `v0.2.2` → `release-preflight v0.2.2` → secret-free launcher → protected workflow → verify cert + digest → publish manifest → physical-device production OTA.

## 5. Validation transport

A Cloudflare quick tunnel exposed a local static origin over publicly trusted TLS; manifest variants were swapped server-side between checks. No cleartext fallback, no certificate bypass, no app security change; the manifest URL used the existing `-Pkisab.privateUpdateManifestUrl` build-time seam; the production endpoint remains untouched. The exact served manifests are recorded in the validation document.

## 6. Results

- **Positive path**: full chain executed explicitly on the API-26 emulator (HTTPS fetch → parse → compare → download → SHA-256 → installer consent "Your existing data will not be lost" → in-place install → relaunch): store payload **byte-identical** pre/post, all UI values equal, About shows 0.2.2 "Up to date". On the physical Android 16 device, the same pair had earlier completed a real in-place upgrade during automated driving, migrating store schema 12 → 14 with every record preserved (two parties, two trades, three settlements, product/sale-detail, supply, production record, activity tag).
- **Negative paths**: equal/lower versionCode → "Up to date" (no downgrade); malformed manifest → "Unable to check", process alive; unreachable source (HTTP 502 through tunnel) → recoverable "Unable to check"; bad checksum → full download then rejection with the installer never invoked. Every case verified with unchanged store md5.
- **Fail-safety**: a Play Protect-blocked install attempt left the app fully functional at 0.2.1 with byte-for-byte unchanged farm data.
- **Expiry matrix**: WARNING (+10 d) dialog without banner, mutations allowed; EXPIRED (−1 d) persistent banner + blocked mutations (verified by unchanged balance after a save attempt) + reachable export (SAF picker opened) + reachable update path + recoverable "Unable to check"; VALID restore returns normal operation. Clock floor persistence and whole-day math behaved deterministically; the device clock was never altered.
- **Gates**: unit 486/0 (+2 new streaming-integrity tests), lint 0 errors, `verifyLocal` passed at 0.2.2/5, connected suite 138/138 on `ZA22374XPC`, `actionlint` clean over all five workflows. Full preflight script remains runnable only post-commit/post-tag by design; its component gates were run individually.

## 7. Defects fixed within M13

1. Pilot-channel doc drift corrected (fallback status string).
2. `ApkDownloader` now verifies the manifest SHA-256 in a single streaming pass instead of buffering the entire APK into memory twice; empty-download edge fails safely; covered by two new JVM tests.

## 8. Acceptance criteria

| Criterion | Result |
|---|---|
| `verifyLocal` green | PASS (486/0, lint 0, 0.2.2/5 metadata) |
| Connected suite remains green | PASS (138/0) |
| Real signed older → newer upgrade succeeds | PASS — production-signed 0.2.1/4 → production-signed 0.2.2/5 over the **live** channel on `ZA22374XPC`; mechanism additionally proven end-to-end on the API-26 emulator with the debug pair |
| Android accepts signer continuity | PASS (same-signer in-place upgrades accepted; production identity verified via apksigner against frozen value) |
| Manifest identifies correct target version | PASS (exact manifest recorded) |
| Checksum corresponds to final artifact | PASS (target sha256 = downloaded file digest) |
| Data survives the real upgrade | PASS (byte-identical payloads; schema 12→14 migration preserved every record on-device) |
| Bad checksum rejected | PASS |
| Malformed/unreachable manifest fails safely | PASS |
| Downgrade/equal does not install | PASS |
| Expiry warning behavior verified | PASS |
| Expired-build restrictions verified | PASS (mutations blocked; view intact) |
| Backup/export available after expiry | PASS (SAF picker reached) |
| Update reachable after expiry | PASS (check completes even when expired) |
| Valid upgrade exits expired condition | PASS (policy-valid reinstall restored normal operation; expired build also proved able to run the check itself) |
| Documentation matches artifacts/workflows | PASS (drift fixed; exact identities recorded) |
| No unrelated files included | Enforced at commit time |

## 9. Explicit non-goals

No accounts, subscriptions, ads, cloud sync, Firebase, push infrastructure, new farming/accounting features, or UI redesign. No TLS/checksum relaxation. No keystore handling outside the protected pipeline.

## 10. Files touched

- `app/build.gradle.kts` — version 0.2.2 / code 5
- `app/src/main/kotlin/com/susankhya/kisab/update/PrivateApkUpdate.kt` — streaming integrity verification
- `app/src/test/kotlin/com/susankhya/kisab/update/PrivateApkUpdateLogicTest.kt` — +2 tests
- `docs/release/RELEASE_NOTES_0.2.2.md` — draft notes (metadata gate requirement)
- `docs/release/PILOT_UPDATE_CHANNEL.md` — drift fix
- `docs/milestones/M13_PILOT_RELEASE_OTA_VALIDATION.md`, `docs/validation/M13_PILOT_RELEASE_OTA_VALIDATION.md`
