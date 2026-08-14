# Kisab v0.2.0 — Draft Release Notes

> **Draft only.** `v0.2.0` has not been tagged, signed, or published. Several manual/human device-validation items remain deferred (see "Known limitations"), so this document must not be presented as a production-readiness claim.

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

- Manual/device validation **completed at the v0.2.0 candidate freeze** (RC-01, Moto API-36 physical): M5-05 Financial Overview (period switching, empty states, recreation restore), M6 Party Hisab (party/period switching, recreation restore), and the M6.3 calculator battery (all arithmetic operations, divide-by-zero, blank input, profit/loss/margin/markup, negative guards, simple interest, Hill/Terai land conversions) — covered by `FarmOverviewAndHisabDeviceTest` and `KisanToolboxDeviceBatteryTest`, with the connected instrumentation suite at **96/96** (86 baseline + 10 new). Overflow handling and Nepali rendering remain unit-covered. See `docs/release/V0.2.0_RELEASE_CHECKLIST.md` for the release-candidate gate record.
- Still outstanding **manual/human** validation: M6.1 shell visual/accessibility presentation (small-screen EN/NE labels, TalkBack order, popup placement, icon contrast), M6.3 keyboard behavior + TalkBack announcements, long-Nepali-text and small-screen rendering for Khata and Party Hisab, and the API-26 emulator smoke pass. See `docs/release/V0.2.0_RELEASE_CHECKLIST.md` (Sections C/E/F).
- Device validation already completed: M6.4 farm-planning calculators (keyboard, selector, small-screen, long-result, Nepali rendering, and persistence isolation) on an API-36 physical device and an API-26 emulator (`docs/milestones/M6_4_FARM_INPUT_CALCULATORS.md`); M6.4.1 shell/system-bar insets including the full connected instrumentation suite (**96/96** on the API-36 physical device at RC-01 head; API-26 emulator passes except two documented back-navigation coordinate flakes reproduced on the pre-change baseline, `docs/milestones/M6_4_1_SHELL_SYSTEM_BAR_INSETS.md`).
- Home cash transactions and Trade Settlements remain deliberately separate to prevent accidental double counting.
- The release workflow requires an annotated `v0.2.0` tag contained in `main`, protected-environment approval, and the existing production signing secrets. It creates a draft release only.
