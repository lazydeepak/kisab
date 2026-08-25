# PILOT-01: Update Channel Validation & Verification Record

## 1. Ground Truth & Provenance

- **Canonical Main SHA**: `b811c7ff0d0a1ec240ac485fd5fe300306487fe7`
- **Candidate Merged PRs**: [#34](https://github.com/lazydeepak/kisab/pull/34) (manifest URL in workflows), [#35](https://github.com/lazydeepak/kisab/pull/35) (freeze RC-02 SHA), [#36](https://github.com/lazydeepak/kisab/pull/36) (live code-3 manifest), [#37](https://github.com/lazydeepak/kisab/pull/37) (version 0.2.1/code 4), [#38](https://github.com/lazydeepak/kisab/pull/38) (freeze RC-03 SHA), [#39](https://github.com/lazydeepak/kisab/pull/39) (RC sign version checks), [#40](https://github.com/lazydeepak/kisab/pull/40) (live code-4 manifest)
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

## 5. Real On-Device OTA Verification (ZA22374XPC)

End-to-end pilot update channel activation test performed on the physical pilot device.

### 5.1 Signed Artifacts

| Build | Candidate SHA | versionName/Code | APK SHA-256 | Signer Certificate SHA-256 | RC Sign Run |
|---|---|---|---|---|---|
| **RC-02 (base)** | `17b24639b20bf39e7b6303327cf01d53335d3187` | 0.2.0 / 3 | `55d218baff602b7e904ca97c87bab20c8355d9f8a016e7c1372e5800ab0db0a0` | `92a578e8cedad6ea86d2dc27663a3279f07a70794627a280f877ab30b1f89cff` | [#32149962774](https://github.com/lazydeepak/kisab/actions/runs/32149962774) |
| **RC-03 (target)** | `18d5f5f2c89e12d090bf00f5f47031e3db6d40be` | 0.2.1 / 4 | `443e6582b3766348b60c3e608daedd5dfbea0b40f601bb9c661fe88961e417b7` | `92a578e8cedad6ea86d2dc27663a3279f07a70794627a280f877ab30b1f89cff` | [#32155419829](https://github.com/lazydeepak/kisab/actions/runs/32155419829) |

Both builds signed with the same production certificate (`CN=Kisab Release, OU=Susankhya, O=Susankhya, C=NP`; SHA-1 `12ecba985e396d84b2b266ad7262e32742532fc2`), guaranteeing Android OS signer continuity during the in-place upgrade.

### 5.2 Live Channel Endpoints

- Manifest: `https://raw.githubusercontent.com/lazydeepak/kisab/main/docs/release/manifests/pilot-manifest.json` — **HTTP 200**
- APK: `https://github.com/lazydeepak/kisab/releases/download/v0.2.1/kisab-v0.2.1-rc-signed.apk` — **HTTP 200**, downloaded SHA-256 matches manifest (`443e6582...`)

### 5.3 On-Device Upgrade Sequence

| Step | Observation | Status |
|---|---|---|
| Installed base | signed code-3 (0.2.0), `RC01UpgradeFarm` present, Active, NPR | **PASS** |
| Baseline recorded | Today: recent activity `Aug 12, 2026, 20:53:35 | Expense | Feed | Irrigation equipment | रु 1,250,000.00`; Khata: `Ram Kumar` (Settled Customer), `Shyam Agro` (Settled Supplier), `Sale — Cash sale रु 10,000.00 Paid Aug 12, 2026, 21:28:08`, Income `रु 750.00` | **PASS** |
| In-app update check (Settings → About → Check for updates) | Detected `UpdateAvailable` via live HTTPS manifest: dialog `Current version: 0.2.0 / New version: 0.2.1 / Published: 2026-08-18T15:37:00Z` | **PASS** |
| Download + integrity | APK downloaded from GitHub Release asset; SHA-256 verified against manifest before install | **PASS** |
| OS package installer | Android confirmed in-place update (`Do you want to update this app?`), signer continuity enforced | **PASS** |
| Installed result | `versionCode=4`, `versionName=0.2.1`; installed APK SHA-256 `443e6582b3766348b60c3e608daedd5dfbea0b40f601bb9c661fe88961e417b7`; signer cert `92a578e8...` | **PASS** |
| Data preservation | `RC01UpgradeFarm` intact post-update; Today + Khata screens match baseline exactly (irrigation equipment रु 1,250,000.00, parties Ram Kumar/Shyam Agro, Cash sale रु 10,000.00, Income रु 750.00) | **PASS** |

### 5.4 Operational Notes

- **Install permission**: `REQUEST_INSTALL_PACKAGES` (install unknown apps) must be granted for `com.susankhya.kisab` before download/install.
- **Play Protect**: Google Play Protect may flag sideloaded APKs. During this test the device owner allowed the install manually (biometric confirmation). Re-scanning confirmed `Play Protect scanning is on` (restored/active).

---

## 6. Deployment State & Final Disposition

```
PILOT UPDATE CHANNEL DISPOSITION: UPDATE_CHANNEL_ACTIVE
```
The pilot update channel is fully activated and verified end-to-end: a signed code-3 build (`0.2.0`) was upgraded in-place on the physical pilot device (Motorola Edge 60 Fusion `ZA22374XPC`) to the signed code-4 build (`0.2.1`) via the live HTTPS manifest, with SHA-256 integrity verification, Android signer continuity, and complete `RC01UpgradeFarm` data preservation. Live manifest and APK endpoints serve HTTP 200 with matching checksums.
