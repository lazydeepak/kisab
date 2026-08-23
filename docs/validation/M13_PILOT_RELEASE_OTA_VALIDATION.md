# M13 Validation Record — Pilot Release and OTA Validation

## M13 FINAL DISPOSITION: PASS

Every gate is green including the production-signed release path: annotated tag `v0.2.2` → protected GitHub `Release` workflow produced a production-signed APK that passed independent verification (signer certificate matches the frozen production identity), the published GitHub Release asset digest equals the live manifest's `sha256`, and the genuine pilot device upgraded in place from production-signed 0.2.1/4 to 0.2.2/5 over the **live** channel with full data preservation and "Up to date" status afterward.

## Candidate identity

- **Branch**: `feat/farm-activities`
- **Starting commit**: `3526e97` (post-M12); M13 changes applied on top, uncommitted at validation time
- **Application ID**: `com.susankhya.kisab`
- **Version before M13**: `0.2.1` / versionCode `4`
- **M13 target version**: `0.2.2` / versionCode `5` (bumped in `app/build.gradle.kts`; rationale below)
- **Debug APK SHA-256 after gates** (`local-ci-evidence.json`, status `passed`, 0.2.2/5): recorded at gate time
- **Physical device**: `ZA22374XPC` — Motorola Edge 60 Fusion (`scout_g`), Android 16, API 36, Asia/Tokyo timezone
- **Emulator**: AVD `api26` — arm64-v8a, Android 8.0, API 26 (the app's minSdk boundary), AOSP image without Play Protect, `adb root` available

## Version decision

Repository HEAD is many commits past the signed v0.2.1 RC-03 (`18d5f5f2…`, confirmed ancestor of HEAD). Release policy requires a strictly increasing `versionCode`, and upgrade semantics require NEW > OLD(4). The smallest justified step is **versionName 0.2.2 / versionCode 5**, applied explicitly with draft release notes at `docs/release/RELEASE_NOTES_0.2.2.md` (required by `:app:verifyReleaseMetadata`). 0.2.2 is NOT called released anywhere; it becomes releasable only when the protected signing pipeline produces and validates the production-signed APK.

## Build identities and signer verification

| Build | Identity | SHA-256 | Signer |
|---|---|---|---|
| OLD — production-signed pilot | v0.2.1 RC-03, candidate `18d5f5f2c89e12d090bf00f5f47031e3db6d40be`, downloaded from the GitHub release asset | `443e6582b3766348b60c3e608daedd5dfbea0b40f601bb9c661fe88961e417b7` (matches live manifest and prior validation record) | certificate SHA-256 `92a578e8cedad6ea86d2dc27663a3279f07a70794627a280f877ab30b1f89cff`, DN `CN=Kisab Release, OU=Susankhya, O=Susankhya, C=NP` — verified locally via `apksigner verify --print-certs`; matches the frozen v0.1.0 identity enforced in CI |
| OLD — debug rebuild of the same source | assembled from a git worktree pinned to `18d5f5f2…`; `aapt dump badging`: `versionCode='4' versionName='0.2.1'` | n/a (test artifact) | debug keystore |
| NEW — M13 target (debug) | HEAD + M13 changes; `versionCode='5' versionName='0.2.2'` | `2196ffc2f5e6bf7d14d0fd8708b212025e788d51f19553d4711e99632d5fe455` (this exact file was the OTA download target) | debug keystore |
| NEW — production-signed (final) | built by the protected `Release` workflow from tag `v0.2.2` → commit `c11f4bb8977c8c2e7b77b0d6f265a93814ac0234`; `aapt dump badging`: `versionCode='5' versionName='0.2.2'`, package `com.susankhya.kisab` | **`6463fe8660b6c9eb59764ad864a2581285bcbf47c6517184c2587e64398a7a94`** (workflow-reported, artifact sidecar, release asset, and anonymous re-download all agree) | certificate SHA-256 `92a578e8cedad6ea86d2dc27663a3279f07a70794627a280f877ab30b1f89cff` — production identity |

Per the M13 execution rule set by the repository owner: no keystore material or passwords were copied into the local environment; all mechanism validation used debug builds. The owner subsequently authorized the production gate, which was executed through the repository's tag-based `Release` pipeline (see "Production-signed release gate" below) — no signing policy was weakened and no keystore left CI.

## Test update channel (validation infrastructure only)

- Local static origin (`python3 http.server`, bound to `127.0.0.1:8443`) exposed through a Cloudflare quick tunnel: `https://investigations-convenient-discipline-norman.trycloudflare.com`. The edge presents a publicly trusted TLS certificate, so the device exercised normal TLS with **no** cleartext fallback, **no** certificate bypass, and **no** app security change. The manifest URL was supplied through the existing governed build-time seam `-Pkisab.privateUpdateManifestUrl=…`; the production default remains unchanged.
- Exact positive-case manifest served:

```json
{
  "versionCode": 5,
  "versionName": "0.2.2",
  "apkUrl": "https://investigations-convenient-discipline-norman.trycloudflare.com/kisab-v0.2.2-target.apk",
  "sha256": "2196ffc2f5e6bf7d14d0fd8708b212025e788d51f19553d4711e99632d5fe455",
  "releaseNotes": "Kisab v0.2.2 M13 validation target: release-hardening drop with streaming update-integrity verification and repaired on-device test baseline.",
  "publishedAt": "2026-08-22T07:05:11Z"
}
```

Negative variants (equal code 4, lower code 3, truncated JSON, all-zero sha256) were swapped onto the same URL between checks; the client re-fetches on every check.

## Automated gates

| Gate | Result | Evidence |
|---|---|---|
| `:app:testDebugUnitTest` | **486 tests, 0 failures** (+2 new streaming-checksum tests) | PASS |
| `:app:lintDebug` | 0 errors | PASS |
| `:app:verifyLocal` | passed; evidence JSON reports status `passed`, versionName `0.2.2`, versionCode `5` | `app/build/reports/verification/local-ci-evidence.json` |
| `:app:verifyReleaseMetadata` | passed (requires `docs/release/RELEASE_NOTES_0.2.2.md`) | PASS |
| `:app:connectedDebugAndroidTest` on `ZA22374XPC` | **138 tests, 0 failures**, BUILD SUCCESSFUL in 5m 9s — M12's clean baseline preserved | PASS |
| `actionlint` over `.github/workflows/*` (5 workflows) | exit 0, no findings | PASS |

Full `scripts/release-preflight.sh` is not runnable end-to-end before commit by design: it requires a clean worktree (the intentionally preserved unrelated files `.gitignore`, `.DS_Store`, `site/` make `git status --porcelain` non-empty) and its tag leg requires an annotated `v0.2.2` tag that only exists post-authorization. Its component gates were executed individually as listed above.

## OTA positive path — full chain (emulator, explicit and documented)

Executed on `emulator-5554` (API 26) because Play Protect hard-blocks debuggable sideloads on the physical device (see negative-path notes); every client-side mechanism is identical there and Android's package-installer enforcement is the real OS component.

1. **Installed base**: OLD debug build (code 4 / 0.2.1) launched; compact farm seeded through the real UI (farm `M13Farm` USD; production Eggs 10 L; credit sale to customer Ram 12 L @ $10 = $120 total, $50 received now, $70 outstanding).
2. **Pre-state capture**: `/data/data/com.susankhya.kisab/shared_prefs/kisab_farm_store.xml` pulled via root — 1212 bytes.
3. Settings → About shows `Kisab 0.2.1`.
4. **CHECK FOR UPDATES tapped**: dialog rendered `Current version: 0.2.1 / New version: 0.2.2 / Published: 2026-08-22T07:05:11Z` plus the data-stays-on-device note and the manifest release notes — proving HTTPS fetch, parse, and strictly-greater comparison (5 > 4).
5. **DOWNLOAD tapped**: 4.4 MB APK streamed over TLS through the tunnel; SHA-256 matched the manifest (client kept the file only on match).
6. **Android package installer** (API 26): "Do you want to install an update to this existing application? Your existing data will not be lost." — **Install tapped as the documented user action.**
7. Installer reported **"App installed."**; `dumpsys package` confirmed in-place upgrade to `versionCode=5 versionName=0.2.2` (same applicationId, same signer → continuity accepted by the OS).
8. App relaunched: farm `M13Farm` intact; Today tiles `$ 120.00 / $ 50.00 / $ 0.00 / $ 70.00 / $ 70.00` and `Production: Eggs: 10 L` identical to pre-state.
9. Post-state store pull: **byte-identical payload** to pre-upgrade (all record IDs equal: farm/party/trade/settlement/product/sale-detail/supply/production set unchanged). The store remained at schema 12 bytes until a write occurs — migration is applied idempotently on write, never destructively on read.

## Physical-device corroboration (Android 16)

- During earlier automated UI driving on `ZA22374XPC`, the same OLD→NEW pair completed a **real in-place OTA upgrade** (all chain steps including installer consent): `dumpsys` showed `versionCode=5`, `lastUpdateTime` matching that moment. On next launch the store migrated **schema 12 → 14** and every record survived with identical IDs and amounts: farm `M13UpgradeFarm` USD; customers/suppliers Ram and Shyam Agro; SALE trade 12000 minor (Eggs) with settlements 5000 + 3000 minor; PURCHASE trade 15000 minor ("Feed stock") with settlement 5000 minor; product Eggs LITRE with sale detail 12 L @ 1000 minor; supply Feed KILOGRAM; production record 24 L MORNING; Poultry activity enabled. This exercises the true pilot migration path (signed v0.2.1 ships schema 12).
- The later **explicit rerun** on the device reached the system installer confirmation ("Do you want to update this app?" → **Update** tapped as documented user action) but Google Play Protect then hard-blocked this specific **debuggable** artifact ("Harmful app blocked"; an "Install anyway" affordance appeared once, yet the re-scan verdict still blocked installation). Two facts were preserved from this attempt:
  - **Fail-safety**: the failed install left the app fully functional at 0.2.1 with the farm store byte-for-byte unchanged (store md5 `2a85e741f290c7b10d7e3b81e1a00588` before and after).
  - The production-signed v0.2.1 artifact historically installs with user consent under Play Protect (see `docs/validation/PILOT_UPDATE_CHANNEL_VALIDATION.md` §5.4); the block observed here applies to the non-production debuggable test artifact, not to the production-signed gate, which remains pending authorization.

## Negative-path results (device, each followed by store-md5 verification)

| Case | Manifest served | Observed behavior | State impact |
|---|---|---|---|
| Equal version | `versionCode 4` | `Update status: Up to date`; no install offered | none (md5 unchanged) |
| Older version | `versionCode 3` | `Up to date`; downgrade refused | none |
| Malformed JSON | truncated garbage | `Unable to check`; process alive (pid captured) — fail-safe parse returns null | none |
| Unreachable source | origin server stopped → tunnel returned HTTP 502 | `Unable to check`; recoverable by retrying once origin returned | none |
| Bad checksum | valid manifest advertising all-zero sha256 | update offered → full download → digest mismatch → `Update failed`; **installer never invoked** | none |

Install-permission behavior was also exercised: a fresh install lacks "install unknown apps"; tapping DOWNLOAD routes the user to the system page for the app (`ACTION_MANAGE_UNKNOWN_APP_SOURCES`) with `Update status: Install permission required`, and the flow proceeds once granted.

## Expiry + update interaction (device, compile-time policy variants of code 5)

Built from HEAD with the sanctioned Gradle overrides (`-Pkisab.privateBuildExpiryEnabled=true -Pkisab.privateBuildExpiresAtEpochMillis=…`); device clock never touched.

- **WARNING (+10 days)**: startup dialog "This Kisab version expires in 9 days (on Sep 2, 2026). Please update…" (whole-UTC-day floor math); **no persistent banner**; mutations allowed; data intact.
- **EXPIRED (−1 day)**:
  - Startup dialog "This version has expired" with **HOW TO UPDATE** → opens the real update check dialog (completed: channel offers code 5 == installed → "up to date").
  - Persistent banner: "This Kisab version has expired. You can still view and back up your data. Install a newer…".
  - **Blocked mutation**: a $5 settlement attempt left Outstanding `$ 70.00` unchanged; guard copy: "This Kisab version has expired. You can view and back up data, but editing is turned off until you update."
  - **Backup/export reachable**: EXPORT BACKUP opened the system SAF save picker (documentsui) — cancelled without writing.
  - **Expired + unavailable update**: with the origin down, the check yields "Unable to check" — recoverable; export/view remain available so the farmer is never trapped away from data.
  - Store md5 constant across the entire expired phase.
- **VALID restore** (default far-future expiry build reinstalled): banner gone, normal operation restored immediately.

No deadlock between expiry and update paths was observed: update checks run inside the expired state, and expiry evaluation itself performs no network work.

## Defects found and fixes made during M13

1. **Documentation drift** — `PILOT_UPDATE_CHANNEL.md` claimed the unconfigured-channel fallback reads "Unknown / Local build"; the shipped string is "Update status: checking is not configured yet." Documentation corrected to match code.
2. **Update integrity hardening** — `ApkDownloader` buffered the whole APK into memory twice (copy to file, then `readBytes()`) before checksumming. Replaced with single-pass streaming verification (`UpdateIntegrityVerifier.copyVerifyingSha256`) that digests while writing and keeps the file only on match; the empty-download edge case now fails the digest match instead of taking a separate branch. Covered by two new JVM tests (`streamingCopyVerifiesMatchingDigestAndWritesEveryByte`, `streamingCopyRejectsMismatchedDigestAndInvalidHex`).

Architecture audit found no further defects: HTTPS-only URL gating, strict manifest validation, strictly-greater version comparison, checksum-before-install ordering, FileProvider authorities/path coverage, archive package-name verification, mutation-guard scoping (27 call sites; backup/export/update never gated), separate clock-floor persistence, deterministic whole-day expiry math, and device-zone day keys were all verified from source during this milestone.

## Production-signed release gate (executed)

### Release topology

- Branch protection requires reviewed PRs with the `build` status check, so the documentation commit reached `main` through PR #45 (`release/v0.2.2-m13`, build PASS 2m59s) → merge commit **`c11f4bb8977c8c2e7b77b0d6f265a93814ac0234`**, which is the single unambiguous release source: its tree contains `appVersionName = "0.2.2"` / `appVersionCode = 5` and `docs/release/RELEASE_NOTES_0.2.2.md`.
- Annotated tag **`v0.2.2`** → `c11f4bb…` (tag object `3370a06e553fbb8be6767bc23f7f337312680e69`), pushed after local verification and a full `scripts/release-preflight.sh v0.2.2` pass (`versionName=0.2.2 versionCode=5 tag=v0.2.2`).
- The tag-based `Release` workflow was used instead of editing any frozen-SHA constant: its validate job resolves the annotated tag, peels it to a commit, requires containment in `origin/main`, and re-checks immutability — so signed source, tag target, validated SHA, trusted workflow revision (main), and artifact checkout are all provably the same commit without weakening any trust boundary.

### Protected signing run

- Launcher run `32611322928` (workflow_dispatch `tag=v0.2.2`) emitted the `release-signing-request` dispatch.
- Release run **`32611327982`**: `validate` SUCCESS → `build-sign` SUCCESS after owner approval of the protected `release-signing` environment deployment → `create-draft-release` SUCCESS.
- `build-sign` re-ran `verifyLocal` + `verifyReleaseMetadata` from the checked-out validated commit, verified tag == packaged versionName, signed, and recorded the digest of the **final signed APK**: `6463fe8660b6c9eb59764ad864a2581285bcbf47c6517184c2587e64398a7a94`.

### Independent artifact verification (local)

- Workflow artifact `kisab-v0.2.2-signed-apk`: sidecar `.sha256` matches computed SHA-256 of `app-release.apk`.
- `aapt dump badging`: `package: name='com.susankhya.kisab' versionCode='5' versionName='0.2.2'`.
- `apksigner verify --print-certs`: `CN=Kisab Release, OU=Susankhya, O=Susankhya, C=NP`; certificate SHA-256 `92a578e8cedad6ea86d2dc27663a3279f07a70794627a280f877ab30b1f89cff` — identical to the production identity enforced since v0.1.0.
- Draft release assets downloaded separately; asset digest equals artifact digest.

### Live channel publication

- Draft release published as latest (`v0.2.2`); asset URL `https://github.com/lazydeepak/kisab/releases/download/v0.2.2/app-release.apk` returns HTTP 302 → 200 anonymously.
- Manifest update went through PR #46 (branch protection again): live manifest now advertises `versionCode 5 / versionName 0.2.2`, that exact `apkUrl`, and `sha256 6463fe86…`.
- Out-of-repo verification: `curl` of `raw.githubusercontent.com/.../pilot-manifest.json` returned the new content, and an anonymous full download of the advertised `apkUrl` hashed to exactly `6463fe86…` — no interval where manifest and artifact disagreed.

### Physical production-signed OTA (the missing M13 gate)

On `ZA22374XPC`, starting from the genuine production-signed v0.2.1 (code 4) with UI-seeded representative state (farm `PilotFarm` USD; production Eggs 10 L; credit sale Ram 12 L @ $10 = $120 total, $50 received, $70 outstanding):

1. Settings → About showed `Kisab 0.2.1`; CHECK FOR UPDATES hit the **live** GitHub manifest.
2. Dialog: "Current version: 0.2.1 / New version: 0.2.2 / Published: 2026-08-23T01:58:36Z" + data-stays-on-device note + release notes.
3. DOWNLOAD: in-app streaming SHA-256 verification passed against the live manifest before installer handoff.
4. Android package installer reached; explicit on-device user consent performed at the installer prompt (performed physically by the owner during the run).
5. Result: in-place upgrade accepted — `dumpsys package`: `versionCode=5 versionName=0.2.2`, same applicationId/signer (continuity enforced by the OS), no uninstall/reinstall.
6. Launch: farm `PilotFarm` intact — Today tiles `$120.00 sales / $50.00 received / $0.00 expenses / $70.00 credit sales`, receivable `$70.00`, `Production: Eggs: 10 L` — all equal to the recorded pre-upgrade baseline; Khata shows customer Ram `$70.00` due with the sale trade row intact.
7. Settings → About now reads `Kisab 0.2.2` and `Update status: Up to date`.

Evidence captures: `prodgate/pre_today.xml|.png`, `update_dialog.xml`, `post_today.xml|.png`, `post_about.xml` (session evidence directory).

### Connected suite after upgrade

Deliberately not executed post-upgrade: `connectedDebugAndroidTest` would replace the production-signed installation with the debug-signed test build (signature conflict forces uninstall, wiping the upgraded pilot store) and thereby destroy the real-upgrade evidence it is meant to extend. The suite already ran green this milestone on this device (**138 tests, 0 failures**) against an identical code tree.



## Remaining risks

1. **RESOLVED — production gate executed** as documented above (was the STOP point): tag-based `Release` pipeline used; no frozen-SHA workflow edit was needed.
2. The live manifest now advertises v0.2.2/5; future drops must follow the same sequence (PR → merge → tag → protected sign → publish release → verify digest → manifest PR).
3. Play Protect verdicts apply per-artifact and per-scan: this production-signed drop installed with on-device consent; keep documenting any consent prompt in future pilot updates.
4. `RELEASE_NOTES_0.2.2.md` was published with the draft release body; it intentionally omits final hashes (the manifest + release assets carry authoritative digests).
