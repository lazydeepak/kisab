# Kisab v0.2.0 — Draft Release Notes

> **Draft only.** `v0.2.0` has not been tagged, signed, or published. Manual/device validation for several M5–M6 screens remains deferred, so this document must not be presented as a production-readiness claim.

## User-visible changes since v0.1.0

- Complete English/Nepali resource localization and Nepal-oriented money, number, date, and time presentation.
- Faster Home income/expense entry with edit, delete, category, timestamp, and discard protection.
- Application shell with branded Kisab logo, top overflow menu, Settings, and accessible selected bottom navigation.
- Party management for customers, suppliers, and dual-role parties.
- Sale and purchase records with first-class payment/settlement history.
- Receivable/payable summaries and chronological per-party Khata ledger.
- Farm-wide financial overview with period activity, position, and monthly trends.
- Party Hisab calculator for period sales, purchases, payments, and period-end position.
- Offline Kisan tools for money arithmetic, profit/loss and margin, simple interest, and Nepali Hill/Terai land-unit conversion.
- Versioned offline backup/restore remains compatible through schema v6.

## Build and delivery changes

- Android Gradle Plugin 9.3 with built-in Kotlin, Gradle 9.6.1, compileSdk/targetSdk 36, and Build Tools 36.
- `:app:verifyLocal` runs unit tests, lint, debug assembly, and Android-test compilation and writes JSON evidence with the debug APK SHA-256.
- Hisab now includes temporary farm-planning calculators for seed, fertilizer, feed, milk, and crop yield using farmer-entered rates and prices; results are not saved and do not change accounting records.
- GitHub CI runs the local-equivalent gate on an explicitly installed API 36 SDK, lints workflows, and uploads verification artifacts.
- Secret-free local release preflight validates source gates, release metadata, worktree cleanliness, and optional annotated-tag containment before any signing workflow is started.

## Data compatibility

- Current farm persistence schema: v6.
- Older supported schemas migrate deterministically through the existing migration chain.
- Backup envelope compatibility and deterministic re-encoding remain covered by the JVM suite.

## Known limitations before release

- Manual/device validation remains outstanding for the M5 financial flows, M6 Party Hisab interactions, M6.1 small-screen/TalkBack presentation, and M6.3/M6.4 calculator keyboard, selector, small-screen, and long-result behavior.
- Home cash transactions and Trade Settlements remain deliberately separate to prevent accidental double counting.
- The release workflow requires an annotated `v0.2.0` tag contained in `main`, protected-environment approval, and the existing production signing secrets. It creates a draft release only.
