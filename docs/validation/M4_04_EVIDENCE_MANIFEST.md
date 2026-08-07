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
| Build command | `./gradlew :app:assembleDebug` (debug pilot candidate); `./gradlew :app:assembleRelease` blocked — see Signing state |
| APK filename | `app/build/outputs/apk/debug/app-debug.apk` |
| APK SHA-256 | `908a91089a1b3568398ab9966d48eb9d7a83a220ee517ce467aa606e26d9c8ee` |
| Signing certificate SHA-256 | `4215b6215ebb5b1c8d3f17ae85e005c5237d81418da9c52389e1786053814ff1` (debug certificate) |
| Signing state | Debug-signed only. `assembleRelease` blocked by `verifyReleaseSigningInputs` because `KISAB_KEYSTORE_PATH`, `KISAB_KEYSTORE_PASSWORD`, `KISAB_KEY_ALIAS`, `KISAB_KEY_PASSWORD` are unset. The stale `app/build/outputs/apk/release/app-release.apk` is a pre-production test-signed artifact (cert SHA-256 `e65ff37b0e3f2bbcf74b448497bf2dfc8733e7765e56784404f9eca11652678d`, DN `CN=Kisab Release Test`) and is **not** usable for upgrade claims. |

> **Upgrade claim:** The upgrade test from published `v0.1.0` is `BLOCKED — PRODUCTION-SIGNED PILOT APK REQUIRED`. No production-signed pilot APK exists for this workstream; the debug APK must not be used for the upgrade claim.

## Signer certificate

Published `v0.1.0` signer certificate SHA-256 (expected for the pilot candidate):

```text
92a578e8cedad6ea86d2dc27663a3279f07a70794627a280f877ab30b1f89cff
```

## Device evidence

| Session ID | Device model | Android API | ABI | Locale | Timezone | Free storage | Evidence files (local) | Evidence SHA-256 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| M4-04-01 | Pixel 7a (serial `41301JEHN06042`) | Android 17 (SDK 37) | arm64-v8a | en-US / ne-NP | Asia/Katmandu | ~84 GB free on /data | Screenshots `01`–`14` (see Logs and screenshots) | See Logs and screenshots |

> **Second physical device:** No second Android device was present on the adb bus during M4-04. The older/lower-resource physical-device matrix gate is `BLOCKED — SECOND PHYSICAL DEVICE UNAVAILABLE`. API 26 / API 36 emulators are supplemental only and do not satisfy the requirement.

## Upgrade evidence

| Step | Expected | Observed | Evidence |
| --- | --- | --- | --- |
| v0.1.0 APK SHA-256 verified (`990c100980c469c9411fb7dc66747d0286a3c8020f7d0c8acca949b7e43bd7bc`) | Match | Verified against downloaded `gh release download v0.1.0` artifact | Manifest entry `kisab-v0.1.0.apk` in Logs and screenshots |
| v0.1.0 signer certificate verified | SHA-256 `92a578e8cedad6ea86d2dc27663a3279f07a70794627a280f877ab30b1f89cff` (DN `CN=Kisab Release, OU=Susankhya, O=Susankhya, C=NP`) | Verified with `apksigner` | Recorded in PR description |
| v0.1.0 installed | — | Not performed — upgrade blocked at pilot APK prerequisite | — |
| Disposable farm + multi-date entries recorded | — | Not performed — upgrade blocked at pilot APK prerequisite | — |
| Backup exported | — | Not performed — upgrade blocked at pilot APK prerequisite | — |
| Before-state totals/IDs/values recorded | — | Not performed — upgrade blocked at pilot APK prerequisite | — |
| Pilot candidate installed as update (no data clear) | — | **BLOCKED — PRODUCTION-SIGNED PILOT APK REQUIRED.** The debug APK (cert `4215b6…`) cannot update the production-signed `v0.1.0` (cert `92a578…`) without a signature mismatch; using it would not prove a genuine production upgrade. | `verifyReleaseSigningInputs` failure message |
| After-state totals/IDs/values recorded | — | Not performed | — |

## Logs and screenshots

Screenshots and logcat extracts are stored locally under the M4-04 evidence directory (not committed). Relevant filenames and hashes are recorded here as they are produced.

| Artifact | Filename (local) | SHA-256 | Session ID |
| --- | --- | --- | --- |
| Published v0.1.0 APK (downloaded, not committed) | `v0.1.0/kisab-v0.1.0.apk` (temp dir) | `990c100980c469c9411fb7dc66747d0286a3c8020f7d0c8acca949b7e43bd7bc` | M4-04-01 |
| Debug pilot candidate APK (not committed) | `app/build/outputs/apk/debug/app-debug.apk` | `908a91089a1b3568398ab9966d48eb9d7a83a220ee517ce467aa606e26d9c8ee` | M4-04-01 |
| Backup export — Scenario A (disposable test farm) | `kisab-disposabletestfarm.backup` (temp dir) | `590a68e0cab917a09e10d18079c30b69e17186100a97927bb1ceb7bcf8a66ef6` | M4-04-01 |
| Backup export — Scenario B (CropFarm) | `kisab-cropfarm.backup` (temp dir) | `4e6421734883be4aa521939ffec692a995cfea32556c92ecc667b92a836207c8` | M4-04-02 |
| Screenshot — clean launch | `01_clean_launch.png` | `5ee922c2bacd041eb6dc29574e19d60d7ad65d122881150fe2df7e04053b0833` | M4-04-01 |
| Screenshot — farm created | `02_farm_created.png` | `b6ac72f2cf0a3e07b6d86fc68049057664fcbfa927ada8e961f6256df0185a43` | M4-04-01 |
| Screenshot — income editor | `03_income_editor.png` | `9019979fc42830abd819740d1f51c9161955da21e1f6c73d073d686d39687934` | M4-04-01 |
| Screenshot — income saved | `04_income_saved.png` | `bb4253f961faf9fd21a36e8eb388cc25629afe6ae8f846a7bcede918963442ec` | M4-04-01 |
| Screenshot — editor state after restart | `05_stuck_editor.png` | `6c8e00a9bda1a6bf6d0766f91af0fa7dc5bf1056cdba7f06a6b132edff10719e` | M4-04-01 |
| Screenshot — date picker | `06_date_picker.png` | `813551e45ab52b4729cb93dc881e069da5505e24cd8b9059c6cf759988b7849a` | M4-04-01 |
| Screenshot — dirty draft before rotation | `07_dirty_before_rotate.png` | `5f1668ca81c95f9174be4fb8cb96369cbd107f993d046aaaf5c9feff8dfbdcfb` | M4-04-01 |
| Screenshot — Nepali farm creation | `08_nepali_create.png` | `51c1a740b18d215b4b7c371ec8f1242538cb5cff8c4adf65ebc53bd141a78ee5` | M4-04-01 |
| Screenshot — Nepali transaction | `09_nepali_transaction.png` | `1beae2b51522b50b2f6e0520f3d8c37edf63b677ece1c0d4cb82e4a7b8524d1b` | M4-04-01 |
| Screenshot — font scale 1.5 | `10_font_scale_15.png` | `3f7cf1ff20e38234a47868288c2c93e75d6714c7848ee1057e39d83205afc9be` | M4-04-01 |
| Screenshot — Scenario A complete | `11_scenarioA_complete.png` | `79c9b223fb1f89f97f780748658cff06fc3778e4a41937b60f3c23d1e955c0dd` | M4-04-01 |
| Screenshot — entry-kind dropdown | `12_kind_dropdown.png` | `89fb7f37fc319ecc3b71568270a3cdf5239c01c5cc4f7a0c2b0f2f03abbf66e7` | M4-04-02 |
| Screenshot — Scenario B complete | `12b_scenarioB_complete.png` | `4b77d73933c5ca91cc9ec50b36f157b33d5a313930ba02c5dfaa816b9bdbb6c9` | M4-04-02 |
| Screenshot — Scenario C restored | `13_scenarioC_restored.png` | `62efb32f0be7eb0ddbe97480a2f47162eaab5a1ef80a9fc4f448e76dc00be7fe` | M4-04-03 |
| Screenshot — Scenario C Nepali | `14_scenarioC_nepali.png` | `56fcdb556133f797a172921c195ddb40c39cdb3615bacce3997907291935b383` | M4-04-03 |

All screenshots and backup files are stored locally under the M4-04 evidence directory (not committed). No APKs, keystores, passwords, raw participant data or screen recordings are committed.
