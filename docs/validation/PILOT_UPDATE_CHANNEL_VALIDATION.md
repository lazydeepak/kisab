# PILOT-01: Update Channel Validation & Verification Record

## 1. Ground Truth & Provenance

- **Canonical Main SHA**: `d1c250cb9bb0e1c43d6cc9f675202b952f921d56`
- **Candidate Merged PR**: [#32: feat(ux): competitive farmer UX redesign (UX-00 through UX-09) and pilot readiness](https://github.com/lazydeepak/kisab/pull/32)
- **Main CI Run**: [#32145642369](https://github.com/lazydeepak/kisab/actions/runs/32145642369) — **SUCCESS**
- **Release Channel Branch**: `release/pilot-update-channel`
- **Application ID**: `com.susankhya.kisab`
- **Base Pilot Version**: `0.2.0` (versionCode `3`)
- **Next Pilot Update Target**: `0.2.1` (versionCode `4`)

---

## 2. Automated Quality & Security Gate Verification

| Gate | Target | Result | Status |
|---|---|---|---|
| **JVM Unit Tests** | `./gradlew test` | 379/379 passed | **PASS** |
| **Android Lint** | `./gradlew lint` | 0 errors | **PASS** |
| **Local CI Verification** | `./gradlew :app:verifyLocal` | Complete evidence generated | **PASS** |
| **Release Metadata Gate** | `./gradlew verifyReleaseMetadata` | v0.2.0 notes verified | **PASS** |
| **Connected Device Suite** | Motorola Edge 60 Fusion (`ZA22374XPC`) | 73/73 passed across all 14 test suites | **PASS** |

---

## 3. Update Decision & Logic Verification Matrix

| Test Scenario | Input Condition | Expected Behavior | Verification Status |
|---|---|---|---|
| **Higher Remote Version** | Installed: `(3, "0.2.0")`, Remote: `(4, "0.2.1")` | Evaluates to `UpdateAvailable` | **PASS** (`PrivateApkUpdateLogicTest`) |
| **Same Version Code** | Installed: `(3, "0.2.0")`, Remote: `(3, "0.2.0")` | Evaluates to `NoUpdate` | **PASS** (`PrivateApkUpdateLogicTest`) |
| **Lower Version Code** | Installed: `(3, "0.2.0")`, Remote: `(2, "0.1.0")` | Rejects downgrade (`NoUpdate`) | **PASS** (`PrivateApkUpdateLogicTest`) |
| **Non-HTTPS Manifest URL** | URL: `http://...` or `ftp://...` | Rejects manifest (`null`) | **PASS** (`PrivateApkUpdateLogicTest`) |
| **Missing SHA-256 Checksum** | Manifest lacks `sha256` field | Rejects manifest (`null`) | **PASS** (`PrivateApkUpdateLogicTest`) |
| **Checksum Mismatch** | Target APK digest != manifest `sha256` | Rejects installation | **PASS** (`PrivateApkUpdateLogicTest`) |
| **Unconfigured Manifest URL** | `BuildConfig.PRIVATE_UPDATE_MANIFEST_URL = ""` | Graceful fallback without network poll | **PASS** (`FarmActivity.kt:5697`) |

---

## 4. Protected State & Signer Integrity

- **Signer Continuity**: Android OS enforces matching signing certificate during package updates.
- **Data Preservation**: In-place package updates preserve all SQLite databases and SharedPreferences under `/data/data/com.susankhya.kisab/`.
- **Protected State**: `RC01UpgradeFarm` remains completely untouched.

---

## 5. Deployment State & Final Disposition

```
PILOT UPDATE CHANNEL DISPOSITION: UPDATE_CHANNEL_READY_TO_PUBLISH
```
All repository-side build configurations, manifest schemas, updater logic, release signing workflows, and validation gates are complete and verified. Production hosting activation awaits final release artifact deployment.
