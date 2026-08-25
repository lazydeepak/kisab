# Kisab Pilot Update Channel Specification & Operations

## 1. Architecture & Trust Boundary

The Kisab Pilot Update Channel provides a secure, HTTPS-driven over-the-air update mechanism for Android pilot deployments without third-party app store dependencies.

```
┌─────────────────────────────────┐
│  Kisab Client App (Android)     │
│  - Reads manifest over HTTPS    │
│  - Evaluates remote versionCode │
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│  Update Verification            │
│  - Downloads APK to cache       │
│  - Checks SHA-256 integrity     │
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│  Android OS Package Installer   │
│  - FileProvider content URI     │
│  - Enforces Signer Continuity   │
│  - Performs In-Place Upgrade    │
└─────────────────────────────────┘
```

### Trust Boundary Rules:
1. **HTTPS Enforcement**: All update manifest URLs and APK download endpoints must strictly use `https://`. Unencrypted `http://` or non-standard schemes are immediately rejected by `UpdateManifest.parse()`.
2. **Cryptographic SHA-256 Verification**: Every downloaded APK is verified against the 64-character hexadecimal SHA-256 checksum in the manifest before invoking the package installer. If the digest fails, the downloaded file is discarded and never passed to the Android OS installer.
3. **Android OS Signer Continuity**: The operating system guarantees that an existing app cannot be overwritten by an APK signed with a different key certificate. Android verifies `SIGNATURE_MATCH` before performing the package update.
4. **Data Isolation & Preservation**: SQLite databases and SharedPreferences under `/data/data/com.susankhya.kisab/` survive in-place updates automatically without data loss or reset.

---

## 2. Update Manifest Specification

The update manifest is a static JSON file served over HTTPS.

### Schema:
```json
{
  "versionCode": 4,
  "versionName": "0.2.1",
  "apkUrl": "https://github.com/lazydeepak/kisab/releases/download/v0.2.1/kisab-v0.2.1-rc-signed.apk",
  "sha256": "443e6582b3766348b60c3e608daedd5dfbea0b40f601bb9c661fe88961e417b7",
  "releaseNotes": "Kisab v0.2.1 first pilot OTA target: active update channel delivering an in-place signed upgrade with data preservation.",
  "publishedAt": "2026-08-18T15:37:00Z"
}
```

### Field Definitions:
- `versionCode` (Required, integer > 0): Android package version code. Update is triggered when `remote.versionCode > installed.versionCode`.
- `versionName` (Required, string): Semantic version string displayed to the farmer.
- `apkUrl` (Required, HTTPS URL): Direct HTTPS download link to the signed APK artifact.
- `sha256` (Required, 64 hex characters): Full cryptographic SHA-256 hash of the target APK.
- `releaseNotes` (Optional, string): Localized release highlights.
- `publishedAt` (Optional, ISO-8601 string): Publication timestamp.

---

## 3. Build Configuration

To configure a pilot build with an active update channel:

```bash
# Pass the manifest URL property during Gradle packaging
./gradlew assembleRelease -Pkisab.privateUpdateManifestUrl="https://raw.githubusercontent.com/lazydeepak/kisab/main/docs/release/manifests/pilot-manifest.json"
```

### Live Manifest URL:
- **Manifest**: `https://raw.githubusercontent.com/lazydeepak/kisab/main/docs/release/manifests/pilot-manifest.json`
- **Hosting**: GitHub raw content (public repository) serving the JSON manifest over HTTPS; APKs hosted as GitHub Releases assets under `https://github.com/lazydeepak/kisab/releases/download/v0.2.x/kisab-v0.2.x-rc-signed.apk`.
- **Redirect handling**: GitHub Release asset URLs 302-redirect to signed object storage; the update client follows HTTPS redirects transparently.

### Fallback Behavior:
- If `kisab.privateUpdateManifestUrl` is omitted or blank (default), `BuildConfig.PRIVATE_UPDATE_MANIFEST_URL` is set to `""`.
- The app gracefully reports `Update status: checking is not configured yet.` in Settings -> About and avoids background network polling.

---

## 4. Operational Publishing Procedure

When releasing a new pilot drop:

1. **Build & Sign APK**: Produce the signed APK via the trusted `RC sign` workflow or local release keystore.
2. **Compute SHA-256**:
   ```bash
   shasum -a 256 app-release-signed.apk
   ```
3. **Upload Signed APK**: Host the APK on an authorized HTTPS endpoint (e.g., GitHub Releases asset or dedicated secure static storage).
4. **Publish `manifest.json`**: Update the hosted manifest JSON with the incremented `versionCode`, target `versionName`, public HTTPS `apkUrl`, and exact `sha256`.
5. **Validation**: Verify that the manifest and APK URLs return HTTP 200 over HTTPS.
