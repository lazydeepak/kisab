# Kisab M10 — Rich Farm Management + Farm Activity Foundation

## Status

**M10 FINAL DISPOSITION: PASS_WITH_FOLLOWUPS**

Implemented on `feat/farm-activities`, based on `main` at `f1ab23f` (post-M9). This record is the authority for the M10 scope: turning the single-purpose accounting app into a mixed-farm-aware app by adding a **Farm Activity** domain (multiple simultaneous activities per farm with stable persisted identity), a **centralized activity-aware catalog** that governs transaction-category and calculator ordering, an **optional transaction→activity association**, a **safe schema-13 persistence migration**, **activity-level accounting visibility**, and EN/NE localization.

Physical-device validation is **blocked** (no device attached — `adb devices` is empty), so the device test suite was not executed on hardware. This is follow-up validation debt, not a release blocker: all automated gates pass, there is no evidence of a new defect, data-safety problem, accounting corruption, or migration failure. Farmer pilot testing of the new activity surfaces should begin only after the physical-device validation record is satisfied.

## Scope

### 1. Farm Activity domain

`domain/FarmActivities.kt` (new):

- **`FarmActivityType`** — closed enum: `CROPS`, `VEGETABLES`, `FRUITS_ORCHARD`, `POULTRY`, `CATTLE_BUFFALO_DAIRY`, `GOAT_SHEEP`, `PIG`, `FISHERY`, `OTHER`. This is the **stable persisted identity** for an activity. Values may be appended later; name-based persistence (`valueOf`) makes appends backward-compatible, mirroring the existing `TransactionCategory`/`ProductUnit` convention.
- **`FarmActivityCatalog`** — the single centralized policy. Everything activity-aware flows through it so screens never branch on individual activities:
  - `displayOrder` — canonical user-facing order for every picker and breakdown.
  - `relevantExpenseCategories` / `relevantIncomeCategories` — the existing governed category authority is retained; activities *reorder* relevant categories first but never invent new ones. Income is farm-wide (`relevantIncomeCategories` returns empty).
  - `relevantCalculators` — farm-planning calculators relevant to each activity; calculators are reordered (relevant first) but never hidden.
  - `orderedCategories(activities, type)` — the transaction-editor category list for a farm. A farm with no activities gets the exact historical list, so migrated and general farms render identically to before.
  - `orderedCalculators(activities)` — the farm-planning calculator list, relevant-first.
  - `activityChoices(activities, currentActivity)` — editor activity choices; `null` = General (whole farm), and a transaction's existing association (even to a now-disabled activity) is appended so editing never silently drops it.
- **`FarmActivityTotals`** + pure **`farmActivityBreakdown(transactions)`** — per-activity income/expense/balance using exact `Long` minor-unit arithmetic (`Math.addExact`/`Math.subtractExact`). Summing the buckets always equals `FarmTotals` (no double counting); the general bucket is last; a disabled activity's historical transactions stay in their bucket — disabling never removes history.

### 2. FarmState, service, and validation

`domain/FarmSliceService.kt`:

- `FarmState` gains `activities` and `disabledActivities`; `CURRENT_FARM_SCHEMA_VERSION` → **13**.
- `FarmTransaction` and `FarmTransactionDraft` gain `activity: FarmActivityType? = null` (`null` = general/farm-wide).
- `createFarm(name, currencyCode, activities = emptyList())` — activities are validated and ordered by `displayOrder`.
- `setFarmActivities(farmId, enabled)` — the enable/disable rule: activities removed from the set are **disabled, not deleted**. Any with historical transaction references move to `disabledActivities` (history readable, totals correct); those with no records are dropped cleanly. Re-enabling works. Historical data is never erased.
- `addFarmActivity` / `disableFarmActivity` / `reEnableFarmActivity` / `farmActivities` / `farmActivityBreakdown`.

`domain/FarmStateValidator.kt`: uniqueness checks for `activities` and `disabledActivities`, plus a disjointness check (an activity cannot be enabled and disabled at the same time).

### 3. Persistence migration (schema 13)

`persistence/FarmPersistenceCodec.kt`:

- Encode now writes 18 fields (two appended: `activities`, `disabledActivities`) and 7-part transaction records (trailing activity name, empty for general).
- `decodeSchema13` re-decodes transactions from the original 7-part payload and decodes both activity lists; legacy detection and version dispatch extended to `13`.
- Backward compatibility: `decodeSchema3Transactions` accepts both 6-part (pre-M10, `activity = null`) and 7-part records, so pre-M10 payloads decode with a `null` activity and an empty activity set. Unknown activity names fail the decode exactly like unknown category values (consistent with existing behavior).
- Backup envelopes (`FarmBackupCodec`, envelope schema v1) are unchanged and carry the schema-13 payload through existing backups.
- No Room migration involved — Kisab uses this custom delimited codec, not Room.

### 4. Activity-aware UI policy

`ui/FarmActivity.kt` + `res/layout/activity_shell.xml` + `ui/FarmOrdering.kt`:

- **Create-farm quick screen and Add-Farm screen**: an "Activities" summary + "Choose activities" button; a multi-choice dialog (display order) with the canonical labels; summary shows the selected activities or "None — general farm".
- **Farm details**: an activities section listing running and paused activities with Disable / Re-enable buttons, plus an activity-level income/expense/balance breakdown (general bucket last). "Change activities" reopens the chooser.
- **Transaction editor**: an optional activity selector (General + enabled activities; a disabled-but-associated activity is appended for editing). Selecting an activity reorders the category choices relevant-first via the catalog. The saved transaction draft carries the association.
- **Farm planning**: calculator spinner is reordered relevant-first via `orderedCalculators`; all calculators remain selectable.
- **Recent transactions**: rows show the activity label first when the transaction is associated.
- Existing farms (empty activity set) render exactly as before — the activity selector hides, category and calculator lists are unchanged.

### 5. Localization

EN (`values/strings.xml`) and NE (`values-ne/strings.xml`) both gained the full M10 block (section labels, prompts, activity labels, enabled/disabled actions, breakdown formats, editor labels, toasts). Parity is enforced by `LocalizationParityTest`.

## Not implemented

- Transaction → activity association for trades (credit sales / supplier purchases). Deferred: trades are a higher-level operation than the money-in/out ledger this milestone associates.
- Per-activity operational domains (production, inventory, planning inputs) — out of scope; only the accounting projection and UI policy are M10.
- New `TransactionCategory` values. The existing governed 8-category authority is retained; the catalog reorders rather than extends.
- Any change to backup envelope format, release/update channels, or the accounting core.

## Automated evidence

All pass: `./gradlew :app:verifyLocal` (JVM tests, lint, debug assembly, androidTest compilation) — 469 tests, 0 failures, lint 0 errors. Evidence: `app/build/reports/verification/local-ci-evidence.json`.

New unit tests:

- `FarmActivityCatalogTest` (new): canonical display order, general-farm category parity with `FarmOrdering.categoriesFor`, relevant-first category/calculator ordering, never-narrowed income, `activityChoices` General-first + disabled-current append, and `farmActivityBreakdown` reconciliation with `FarmTotals` (exact sums, ordering, empty input).
- `FarmActivityServiceTest` (new): create ordering/dedup, disable-only-with-history rule, drop-without-history, disable→re-enable preserving history and totals, breakdown stability across disable/re-enable, add activity.
- `FarmActivityPersistenceTest` (new): schema-13 round-trip of activities/disabled/associations, schema-12→13 migration, 7-part and 6-part transaction decode, unknown-activity decode failure, schema-13 re-encode of migrated data, breakdown across a codec round-trip.
- Updated existing schema-version assertions (12 → 13) in `FarmSliceServiceTest`, `FarmFinancialOverviewTest`, `PartyLedgerTest`, `PartyHisabCalculatorTest`, and the byte-stable `backupEnvelopeFormatIsByteStable` expected payload.

## Manual/device validation

**BLOCKED — no device attached.** `adb devices` is empty, so the connected suite and physical walkthrough were not run. This is recorded as follow-up validation debt in `docs/validation/M10_FARM_ACTIVITIES_VALIDATION.md`; pilot testing of the activity surfaces must not begin until that record is satisfied.