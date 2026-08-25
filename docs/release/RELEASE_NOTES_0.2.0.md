# Kisab v0.2.0 — Release Notes & Farmer Pilot Candidate

> **Release Candidate & Pilot Drop.** Governed by `docs/release/RELEASE_POLICY.md` and verified through UX-00 to UX-09 and PILOT-01.

## User-visible changes since v0.1.0

- **5-Element Farmer Navigation Shell**: Intuitive, high-contrast bottom navigation (`आज / Today`, `खाता / Khata`, `लेख्नुहोस् / Record`, `फार्मको काम / Farm Work`, `अरू / More`) with prominent central Record action.
- **Actionable Today Dashboard**: Instant operational view with high-priority attention badges (`लिन बाँकी / To Receive`, `तिर्न बाँकी / To Pay`), today's production summary, and recent activity timeline.
- **Directional Khata Ledger**: Dual-semantic debt indicators (text prefix + contrast border), instant search/filter, chronological transaction timeline, and one-tap payment shortcuts (`[पैसा पाएँ] / [पैसा तिरेँ]`).
- **6 Farmer Record Flows**: Fast entry for `उत्पादन` (Production), `बेचेँ` (Sell), `पैसा पाएँ` (Received Money), `किनेँ` (Bought), `प्रयोग गरेँ` (Used), `पैसा तिरेँ` (Paid Money) with live arithmetic equation tiles and native date/time pickers.
- **Farm Work Operational Management**: Product production tracking by session (Morning/Evening), production reconciliation equations (`उत्पादन = बिक्री + प्रयोग + बाँकी`), and physical supplies stock tracking with overuse guards.
- **Multi-Farm Management & State Isolation**: Seamless switching between farms via app-bar dropdown or More -> Farms, with full data isolation and backup-gated danger zones.
- **Offline Kisan Toolbox & Calculators**: Land unit converter (Nepali Traditional Ropani/Aana/Kattha to Metric/International) and farm planning estimators (Seed, Fertilizer, Feed, Milk, Crop Yield).
- **Accessibility & Nepali Typography**: First-class Devanagari typography with 1.15x line spacing, 36sp large text support without clipping, and contrast-verified light and dark themes.
- **Data Safety & Versioned Backup**: JSON backup export/import with format compatibility through schema v12.

## Build and delivery changes

- Android Gradle Plugin 9.3 with built-in Kotlin, Gradle 9.6.1, compileSdk/targetSdk 36, and Build Tools 36.
- `:app:verifyLocal` runs unit tests, lint, debug assembly, and Android-test compilation and writes JSON evidence with the debug APK SHA-256.
- GitHub CI runs the local-equivalent gate on API 36 SDK, lints workflows, and uploads verification artifacts.
- Release workflow strictly enforces trusted `main` signing, annotated `v<versionName>` tags, and GitHub Environment secret protection.

## Data compatibility

- Current farm persistence schema: v12.
- Older supported schemas migrate deterministically through the existing migration chain.
- Backup envelope compatibility and deterministic re-encoding remain fully covered by automated suites.

## Verification evidence

- **Unit Tests**: 379/379 tests passing (`./gradlew test`).
- **Lint**: 0 errors (`./gradlew lint`).
- **On-Device Instrumentation**: 73/73 tests passing on Motorola Edge 60 Fusion (`ZA22374XPC` / Android 16).
- **Physical Walkthrough**: Full daily journey and multi-farm isolation verified on device (`Pilot01Farm`).
- **Protected State**: `RC01UpgradeFarm` verified untouched and intact.
