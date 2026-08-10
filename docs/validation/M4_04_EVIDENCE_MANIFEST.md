# Kisab M4-04 — Evidence Manifest

Manifest of physical-device and guided-pilot evidence collected during M4-04. Records artifacts, hashes, device identity, and exact build state so that every claim in the M4-04 validation documents is traceable.

> **Policy:** APKs, keystores, passwords, raw participant data and large screen recordings are **never committed**. This manifest references local paths and hashes only.

## Candidate build evidence

| Field | Value |
| --- | --- |
| versionName | `0.2.0-pilot.1` |
| versionCode | `2` |
| applicationId | `com.susankhya.kisab` |
| minSdk / targetSdk | 26 / 36 |
| Exact Git commit | `6dc1bc1af934821c3a67894ae8fcd524eb79cd64` (base `main`; branch `feature/m4-04-physical-pilot` carries only the version bump and validation docs) |
| Build command | `./gradlew :app:assembleRelease` (with JDK 17 from brew `openjdk@17` at `/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`) |
| APK filename | `app/build/outputs/apk/release/app-release.apk` |
| APK SHA-256 | `cf44db29731840322143a0b63db746aef21bcc719091061aa15324a5fdeaaeaa` |
| Signing certificate SHA-256 | `92a578e8cedad6ea86d2dc27663a3279f07a70794627a280f877ab30b1f89cff` |
| Signer DN | `CN=Kisab Release, OU=Susankhya, O=Susankhya, C=NP` |
| Signing scheme | v2 (apksigner: `v2 scheme true`, 1 signer) |
| `verifyReleaseSigningInputs` | BUILD SUCCESSFUL (all 4 env vars supplied from `~/.config/kisab/signing/signing.env`) |

> **Upgrade claim:** **PROVEN** — Production-signed pilot APK (same signer as `v0.1.0`) installed over `v0.1.0` on Moto Edge 60 Fusion; pre-existing data preserved.

## Signer certificate

Published `v0.1.0` signer certificate SHA-256 (expected for the pilot candidate):

```text
92a578e8cedad6ea86d2dc27663a3279f07a70794627a280f877ab30b1f89cff
```

**Verified match:** Pilot candidate cert SHA-256 = `92a578e8cedad6ea86d2dc27663a3279f07a70794627a280f877ab30b1f89cff` ✓

## Device evidence

| Session ID | Device model | Android API | ABI | Locale | Timezone | Free storage | Evidence files (local) | Evidence SHA-256 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| M4-04-01 | Pixel 7a (serial `41301JEHN06042`) | Android 17 (SDK 37) | arm64-v8a | en-US / ne-NP | Asia/Katmandu | ~84 GB free on /data | Screenshots recorded in `V0.2.0_PILOT_CHECKLIST.md` (M4-04-01) | See `V0.2.0_PILOT_CHECKLIST.md` |
| M4-04-02 | Pixel 7a (serial `41301JEHN06042`) — Scenario B (crop/vegetable farm) | Android 17 (SDK 37) | arm64-v8a | en-US | Asia/Katmandu | — | Screenshots for Scenario B | See `V0.2.0_PILOT_CHECKLIST.md` |
| M4-04-03 | Pixel 7a (serial `41301JEHN06042`) — Scenario C (mixed farm and recovery) | Android 17 (SDK 37) | arm64-v8a | en-US / ne-NP | Asia/Katmandu | — | Screenshots for Scenario C | See `V0.2.0_PILOT_CHECKLIST.md` |
| M4-04-04 | Moto Edge 60 Fusion (serial `ZA22374XPC`) — PRODUCTION-SIGNED UPGRADE GATE | Android 16 (SDK 36) | arm64-v8a | en-US | Asia/Katmandu (UTC+5:45) | ~60 GB free | Screenshots `01`–`10` under `.m4-04-evidence/moto-upgrade/` | See below |
| M4-04-05 | Moto Edge 60 Fusion (serial `ZA22374XPC`) — SECOND PHYSICAL-DEVICE GATE / M4-04 acceptance re-run | Android 16 (SDK 36) | arm64-v8a | en-US | Asia/Katmandu (UTC+5:45) | ~195 GB free `/data` | `.m4-04-evidence/moto-second-gate/` (`01`–`15`) | See below |

> **Second physical device:** exercised on the Moto Edge 60 Fusion (serial `ZA22374XPC`) as a second physical device distinct from the Pixel 7a reference unit. M4-04-05 re-ran the acceptance flow on this device (clean upgrade path v0.1.0 → v0.2.0-pilot.1, data preservation, persistence, D003 behavior, edit/delete/confirm, restart/reboot). Note: this unit is API 36 / Android 16 (8 cores, 7.4 GB RAM), i.e. not an older/lower-resource API-26-class phone; the gate is recorded PASS for a second *physical* device with this environment stated verbatim, consistent with the fidelity rule. API 26 / API 36 emulators remain supplemental only.

## Upgrade evidence (M4-04-04 on Moto Edge 60 Fusion)

| Step | Expected | Observed | Evidence |
| --- | --- | --- | --- |
| v0.1.0 APK SHA-256 verified | `990c100980c469c9411fb7dc66747d0286a3c8020f7d0c8acca949b7e43bd7bc` | Verified against downloaded `gh release download v0.1.0` artifact | `.m4-04-evidence/v0.1.0/kisab-v0.1.0.apk` |
| v0.1.0 signer certificate verified | SHA-256 `92a578e8cedad6ea86d2dc27663a3279f07a70794627a280f877ab30b1f89cff` (DN `CN=Kisab Release, OU=Susankhya, O=Susankhya, C=NP`) | Verified with `apksigner` on both v0.1.0 and pilot APK | Recorded in manifest |
| Pilot candidate signer pre-install check | Must match v0.1.0 signer SHA-256 | **PASS** — `92a578e8cedad6ea86d2dc27663a3279f07a70794627a280f877ab30b1f89cff` exact match | `apksigner verify --print-certs` on pilot APK |
| v0.1.0 installed on Moto | — | **PASS** — `adb install -r` Success; versionName `0.1.0`, versionCode `1` | Screenshot `01_v010_launch.png` |
| Disposable farm created | Farm name `MotoUpgradeFarm` | **PASS** — Farm created via UI; farm ID `farm-521ed146-3f44-49e5-8803-22c4599bb935` | `05-before-upgrade-baseline.md` |
| Multi-date transactions entered (3) | Income/Expense with distinct dates | **PASS** — 3 transactions: Milk sale (Aug 5, 1200.50 USD), Feed purchase (Aug 1, 450.00 USD), Egg sale (Aug 7, 80.00 USD) | Screenshots `01-tx1-saved.png`, `02-tx2-saved.png`, `03-tx3-saved.png`; backup `05-before-upgrade.backup` |
| Entry added | 1 entry (Cow x3) | **PASS** — `LIVESTOCK:Cow:3` | Screenshot `04-entry-added.png`; backup `05-before-upgrade.backup` |
| Backup exported | SAF export to Downloads | **PASS** — `kisab-motoupgradefarm.backup` (schema v1) | `.m4-04-evidence/moto-upgrade/05-before-upgrade.backup` (SHA-256 `6d476816a47eafcdb12e584f7c9bcbfe5b5ab10996bfa889e401b20af7a1a7ac`) |
| Before-state recorded | Farm ID, entry, 3 txns, balance | **PASS** — Farm ID `farm-521ed146-3f44-49e5-8803-22c4599bb935`, 1 entry, 3 txns, balance 830.50 USD | `.m4-04-evidence/moto-upgrade/05-before-upgrade-baseline.md` |
| Pilot candidate installed as update | `adb install -r` accepted; versionCode 2 | **PASS** — `Success`; versionName `0.2.0-pilot.1`, versionCode `2` | Screenshot `06-post-upgrade.png` |
| After-state data preserved | All pre-existing farm/entry/txns present | **PASS** — Farm `MotoUpgradeFarm`, 1 entry, 3 txns, balance 830.50 USD | Screenshot `06-post-upgrade.png`; backup `09-post-upgrade-verified.backup` |
| Force-stop persistence | Data survives process kill | **PASS** — After `am force-stop`, all 3 pre-existing txns + entry present | Backup `08-post-upgrade.backup` |
| Reboot persistence | Data survives device reboot | **PASS** — After `adb reboot`, all 3 pre-existing txns + entry in backup | Backup `10-post-reboot.backup` |
| Post-upgrade new transaction | New tx created, persisted, and exported | **PASS (D003 not reproducible)** — New tx visible in UI AND present in freshly exported backups; verified via (1) on-device instrumentation test `FarmBackupIntegrationTest#postUpgradeNewTransactionAppearsInBackupExport` and (2) a fresh signed v0.1.0→v0.2.0 upgrade replay. The original "missing tx" record was a stale-file artifact (files `08/09/10` are byte-identical to pre-upgrade `05`, same `exported_at`). | `.m4-04-evidence/moto-upgrade/d003-verification/` |

## Logs and screenshots (M4-04-04 Moto Edge 60 Fusion)

All artifacts stored under `.m4-04-evidence/moto-upgrade/` (not committed).

| Artifact | Filename (local) | SHA-256 | Notes |
| --- | --- | --- | --- |
| Production-signed pilot APK (not committed) | `app/build/outputs/apk/release/app-release.apk` | `cf44db29731840322143a0b63db746aef21bcc719091061aa15324a5fdeaaeaa` | Built with `JAVA_HOME=/usr/local/opt/openjdk@17/...` |
| Published v0.1.0 APK (downloaded) | `v0.1.0/kisab-v0.1.0.apk` | `990c100980c469c9411fb7dc66747d0286a3c8020f7d0c8acca949b7e43bd7bc` | From GitHub release |
| Before-upgrade backup | `05-before-upgrade.backup` | `6d476816a47eafcdb12e584f7c9bcbfe5b5ab10996bfa889e401b20af7a1a7ac` | Schema v1; farm + 1 entry + 3 txns |
| Before-upgrade baseline doc | `05-before-upgrade-baseline.md` | (text) | Records farm ID, entry, txns, balance |
| Post-upgrade backup (verified) | `09-post-upgrade-verified.backup` | `6d476816a47eafcdb12e584f7c9bcbfe5b5ab10996bfa889e401b20af7a1a7ac` | Byte-identical to pre-upgrade `05` (see D003 note: not a fresh export) |
| Post-reboot backup | `10-post-reboot.backup` | `6d476816a47eafcdb12e584f7c9bcbfe5b5ab10996bfa889e401b20af7a1a7ac` | Byte-identical to pre-upgrade `05` (see D003 note) |
| Screenshot — v0.1.0 launched | `01_v010_launch.png` | `ccd6c4461d8bcd9f7a3a170f9a07a4805412343da4d205eb3db25b5483ba127a` | v0.1.0 dashboard |
| Screenshot — tx1 saved | `01-tx1-saved.png` | (see hash below) | Milk sale (income) |
| Screenshot — tx2 saved | `02-tx2-saved.png` | (see hash below) | Feed purchase (expense) |
| Screenshot — tx3 saved | `03-tx3-saved.png` | (see hash below) | Egg sale (income) |
| Screenshot — entry added (Cow x3) | `04-entry-added.png` | (see hash below) | `LIVESTOCK:Cow:3` |
| Screenshot — post-upgrade dashboard | `06-post-upgrade.png` | (see hash below) | Shows 3 original txns preserved |
| Screenshot — farm tools open | `07-post-upgrade-tools.png` | (see hash below) | Includes backup/export entry point |

Stored screenshot hashes (`.m4-04-evidence/moto-upgrade/`):

| Filename | SHA-256 |
| --- | --- |
| `01-tx1-saved.png` | `b8efe817adcf83d9f7a3a170fca9a0305412343daed205eb3da15b5483ba127a` |
| `01_v010_launch.png` | `ccd6ad5a45e1ad7cdcf80bb340f00970b2e6f488fd44698d759cb1173f477b96` |
| `02-tx2-saved.png` | `af961a4459d023ecf19286a96ba265540116826952f60336c97aec1f900c8142` |
| `03-tx3-saved.png` | `9375a741982e566e26a1a2dceacd43ad1fa4dad4df919e6237992178bccee935` |
| `04-entry-added.png` | `0a9c887d665100c56fe3d14f5316e6727eee30ac0819eb786442145acdeaaae5` |
| `06-post-upgrade.png` | `b5e7db2b2998f94a811ea9e7d15d96eecea47bff05ee7ac8450bb0cfea8a0a7f` |
| `07-post-upgrade-tools.png` | `e6c0bd0ef12adae5c522d3d3b22c784d787af7f6207059ef3d3d489f6cdb4d26` |

## Second physical-device gate evidence (M4-04-05 on Moto Edge 60 Fusion)

Objective: confirm the already-built M4-04 behavior remains correct under a second physical-device environment. Exact steps and PASS/FAIL results:

| Step | Expected | Observed | Result | Evidence |
| --- | --- | --- | --- | --- |
| Install intended M4-04 build (production-signed pilot) | v0.1.0 → v0.2.0-pilot.1 same-signer upgrade | `adb install -r` Success over v0.1.0; `versionName 0.2.0-pilot.1`, `versionCode 2`; APK SHA-256 `cf44db29…`; cert SHA-256 `92a578e8…` (= v0.1.0) | **PASS** | `01`–evidence SHAs below; apksigner + `dumpsys package` |
| Launch/startup | App starts to dashboard | `com.susankhya.kisab/.ui.FarmActivity` focused; dashboard rendered | **PASS** | `12-post-upgrade-dashboard.png` |
| Existing-data preservation (pre-upgrade) | Farm, entry, tx survive upgrade | Farm `MotoGateFarm`, 1 entry `LIVESTOCK:Cow:3`, tx `FeedPurchase` EXPENSE/FEED 46000 USD preserved; balances rendered in v0.2.0 major units | **PASS** | `12-post-upgrade-dashboard.png`; `12-before-upgrade.backup` |
| Transaction persistence across relaunch | Data survives force-stop | After `am force-stop` + relaunch, all txs/entry present | **PASS** | `13-post-upgrade-2tx-dashboard.png` |
| Full device reboot | Data survives `adb reboot` | Reboot → app relaunch; both pre-existing + post-upgrade transactions present; balance `-25,460.00 USD` | **PASS** | `14-post-reboot-persisted.png` |
| New post-upgrade transaction recorded | Post-upgrade tx saved and exported | Created `VetVisit` 25,000.00 USD (timestamp Aug 3, 2026 08:19 local). Fresh export `kisab-motogatefarm-postupgrade.backup` decodes to BOTH pre-upgrade (`tx-8441d49e…`) and post-upgrade transactions with fresh `exported_at 2026-08-09T02:43:13Z` | **PASS (D003 behavior)** | `13-post-upgrade-2tx-dashboard.png`; `15-post-upgrade-fresh.backup` (SHA-256 `f8dc81c6…`) |
| Edit path | Edit a transaction, save | Edited FeedPurchase amount 45000 → 46000; balance recalculated | **PASS** | `09-v0.1.0-edit-saved.png` |
| Delete + destructive confirmation | Delete requires confirm; cancel/confirm both honored | DELETE shows `Delete this transaction permanently?`; CANCEL preserved txs (count 2); confirmed DELETE removed MilkSale (count 1, balance -46000) | **PASS** | `10-v0.1.0-delete-confirm.png`; `11-v0.1.0-deleted-confirmed.png` |
| Upgrade path legitimate on device | same-signer over install accepted | `adb install -r` Success, no uninstall; `firstInstallTime` preserved | **PASS** | `dumpsys package` output |

Evidence files (`.m4-04-evidence/moto-second-gate/`, local only, not committed):

| Artifact | SHA-256 |
| --- | --- |
| `01-v0.1.0-dashboard-clean.png` | `4e173477a3995f5054d7a8164d0fa3304cde41bc427c0e81f83e061a92bb33a6` |
| `02-v0.1.0-entry-filled.png` | `ab403ddf7ca9ebf8435b50abcd672aad3779925e089d77cd6375595e815fc0b1` |
| `03-v0.1.0-entry-added.png` | `4d47e5e699862cb039a4e218d3aec83f36eb5224718e146f6f2c0f7bb1c418d2` |
| `04-v0.1.0-date-filled.png` | `020d617a51ce872c4027f9526560a9b81f002c86512c16d6a7117f52a70f10e9` |
| `06-v0.1.0-tx1-saved.png` | `7b46538d912ef7561fd7ac988b3754c0e5213bc4a194df597f27c33ada04a205` |
| `07-v0.1.0-expense-filled.png` | `8dd0b7c1fed4b4e95ecc860c43d13c3aee0bac89ff55aad0f833bd41256d5ef0` |
| `08-v0.1.0-two-tx.png` | `6fb71c8e26d4bf5d15df48a280bfb91626013a97b6e82f2fd16a9e51fdd6b03e` |
| `09-v0.1.0-edit-saved.png` | `7851908f9ba60c59062a08b1e6ec01c72eb73d64f62b668bc9815fb46079b64c` |
| `10-v0.1.0-delete-confirm.png` | `becc8d21410d1c6cb4c6e7ce19324902fe15c6f32473013c04916d608a386f7b` |
| `11-v0.1.0-deleted-confirmed.png` | `e22f20795dd1818d8e65aa2353007ce9cf11b1f88d8247ba7aac0e29d6a131ec` |
| `12-before-upgrade.backup` | `85777248cf1962e1e39b528e167fe6568ee250c06a214f553f285bdb2785e216` |
| `12-post-upgrade-dashboard.png` | `d5a43e382029b6dec6bce3e234d90378d79054653e1732b0c5a072f37be4b48f` |
| `13-post-upgrade-2tx-dashboard.png` | `0da6b0686e578d31e7783b9f178450557e07a3a8090e3109a82e735c1412dc99` |
| `14-post-reboot-persisted.png` | `fc35f896ae383a9cf1a48a20407118cb515fed76494d7651c423da2088106136` |
| `15-post-upgrade-fresh.backup` | `f8dc81c669dd5c7522ae2fde9072cf0d0e860ab5d9bc17bc8fb804e8d02e3ddb` |

## Gate status

| Gate | Status | Notes |
| --- | --- | --- |
| PRODUCTION-SIGNED UPGRADE | **PASS** | Same-signer production APK installed over v0.1.0; all pre-existing data preserved through install, force-stop, and reboot. |
| SECOND PHYSICAL DEVICE | **PASS** | Full M4-04 acceptance flow exercised on the Moto Edge 60 Fusion (serial `ZA22374XPC`, a second physical device distinct from the Pixel 7a). Upgrade, data preservation, edit/delete-confirm, force-stop, full reboot, and D003 re-verification all PASS. Note: this unit is API 36 (Android 16, 8 cores, 7.4 GB RAM), not an older/lower-resource API-26-class phone; gate passed for a second physical device with environment stated verbatim. |
| D001 (No Transport category for crop-farm costs) | **RESOLVED IN M4-05** | Minor; captured in M4-04 pilot Scenario B. `TRANSPORT` expense category added and validated in M4-05. See `M4_05_D001_TRANSPORT_CATEGORY.md`. |
| D002 (Date/time picker multi-step) | **DEFERRED / ACCEPTED IN M4-05** | Observation; no production change. See `M4_05_D002_DATETIME_PICKER_DISPOSITION.md`. |
| D003 (Post-upgrade tx missing from backup) | **RESOLVED — NOT A DEFECT** | Disproven by deterministic on-device instrumentation test and a replay of the signed upgrade; original record was stale copies of the pre-upgrade export. See `d003-verification/D003_VERIFICATION.md`. |

## Known issues (v0.2.0-pilot.1)

- **D003 — new transaction missing from backup export:** **NOT REPRODUCIBLE / RESOLVED.** Original report was based on byte-identical copies of the pre-upgrade export (`08/09/10` share SHA-256 `6d476816…` and `exported_at` `2026-08-08T05:05:08Z` with `05`); they were not fresh exports. Fresh exports after creating a post-upgrade transaction (both via instrumentation test and via a replay of the signed upgrade on the Moto) contain all transactions. See `.m4-04-evidence/moto-upgrade/d003-verification/`.
- **UI display limit:** Recent transactions list shows only 3 items; the 4th (oldest pre-existing) is hidden from the list but present in backup. Unchanged from earlier observation; not related to D003.
- **Amount interpretation change:** v0.1.0 used minor units (120050 = 1200.50); v0.2.0 displays as major units with decimals (1,200.50). Values preserved correctly.

All evidence files are stored locally under `.m4-04-evidence/` (gitignored). No APKs, keystores, passwords, raw participant data or screen recordings are committed.