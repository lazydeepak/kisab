# M10 Validation Record — Rich Farm Management + Farm Activity Foundation

## Candidate identity

- **Branch**: `feat/farm-activities`
- **Baseline**: `main` at `f1ab23f` (post-M9)
- **Application ID**: `com.susankhya.kisab`
- **Version Name / Code**: `0.2.1` / `4` (debug)
- **Device**: `ZA22374XPC` — Motorola Edge 60 Fusion (`scout_g`), Android 16, API 36, density 450 (2.8×), 1220×2712
- **Debug APK SHA-256**: `fc2f725532a7b74627cdeae814747a08c9abe9c56ffb2167606f47b901f40639` (from `local-ci-evidence.json`)

## M10 FINAL DISPOSITION: PASS

M10 is **PASS**. Physical-device validation was executed against a real Motorola Edge 60 Fusion running Android 16 (API 36). Every automated gate passes, the connected device suite introduces **zero new failures** versus baseline (the 33 device failures are pre-existing baseline failures documented below), and the complete physical-device walkthrough — including a real schema-12 → schema-13 migration round-trip — passed with **no defects found**. No data-safety problem, accounting corruption, or migration failure was observed.

## Automated gates

| Gate | Result | Evidence |
|---|---|---|
| `:app:testDebugUnitTest` | 469 tests, 0 failures | PASS |
| `:app:lintDebug` | 0 errors | PASS |
| `:app:verifyLocal` | passed | `app/build/reports/verification/local-ci-evidence.json` |
| `:app:connectedDebugAndroidTest` | 141 tests, 33 failures — **all pre-existing baseline** | PASS (no new failures) |

### Connected-suite failure baseline

The connected suite reports 141 tests / 33 failures. All 33 are the documented M9 baseline class: tests that target legacy compatibility views hidden in the M7+ shell (`recordIncomeButton`, `recordExpenseButton` inside a `visibility="gone"` container) or that assume a focused-window condition. The one failure not enumerated in the M9 record, `FarmActivityShellRedesignTest.farmWorkDestinationExposesProductionAndSupplies`, was re-run against baseline `main` (`f1ab23f`) in an isolated worktree and **fails identically there** (90%-visibility constraint on `farmWorkRemainingButton`), confirming it is pre-existing and not introduced by M10. M10 therefore adds **zero** new device-test failures.

Failing classes (all baseline): `FarmActivityWorkflowTest` (15), `FarmBackupIntegrationTest` (9), `FarmActivityPresentationTest` (5), `FarmPersistenceIntegrationTest` (2), `D002DateTimePickerEvaluationTest` (1), `FarmActivityShellRedesignTest` (1). Representative cause: `Error performing 'scroll to' on view '.../recordExpenseButton'` (view hidden).

## Physical-device walkthrough (executed)

All flows below were driven live on `ZA22374XPC` and verified via view-hierarchy dumps and persisted-store inspection.

### Migration (schema 12 → 13) — real upgrade path

1. Seeded a schema-12 store (`kisab_farm_store`, layout v2, `farm_ids`/`farm_payload_<id>`/`current_farm_id`) containing a 16-field payload with 2 transactions, a party, a product, a supply, a production record, and entries, via the app's SharedPreferences XML.
2. Launched the M10 build: the farm opened; both transactions rendered newest-first with correct values; the migrated party/product/supply appeared in their flows; store remained schema 12 on disk (in-memory decode).
3. Edited the migrated transaction in the UI and saved: the store **re-encoded to schema 13** (18 fields, `activities=""`, `disabledActivities=""`) with the transaction preserved.

### Activity chooser (create + add farm)

- Quick-create and Add-Farm choosers list all 9 activities; multi-selection updates the summary live (`Poultry\nGoat / Sheep`); confirming persists activities to the store (`activities="POULTRY…GOAT_SHEEP"`).

### Transaction editor activity selector

- On an activity farm the editor shows the **Activity** selector: options are `General (whole farm)` first, then `Poultry`, `Goat / Sheep` (catalog order); selection updates the stored association; expense category list renders canonical order (`Feed, Supplies, Labor, Transport, Other expense`).
- On a general farm (empty activity set) the selector is hidden (`show = hasEnabled || currentActivity != null`), preserving general-only behavior.

### Activity-tagged recent rows

- A transaction associated with `POULTRY` renders `- Poultry | … | $ 50.00` (activity prefix); a general transaction renders without a prefix. Editing a tagged transaction preserves its association.

### Farm Details — disable / re-enable and breakdown

- Activity rows render `POULTRY — RUNNING NOW` / `GOAT / SHEEP — RUNNING NOW`.
- Disabling `POULTRY` flips it to `POULTRY — PAUSED`; the breakdown **retains** the poultry history (`Income: $ 50.00 Expense: $ 0.00 Balance: $ 50.00`) while the general bucket shows the farm-wide remainder (`Income: $ 0.00 Expense: $ 20.00 Balance: -$ 20.00`).
- Re-enabling restores `RUNNING NOW`; the store persists `activities="POULTRY…GOAT_SHEEP"`, `disabledActivities=""` throughout the cycle.

### Whole-farm parity and calculations

- Khata/Hisab totals reconcile with the activity breakdown: `Income: $ 50.00, Expenses: $ 20.00, Net: $ 30.00`; Farm Tools balance `$ 30.00`.
- Farm-planning calculator list is reordered relevant-first for the poultry/goat-sheep farm: `Feed requirement and cost` first, then Seed, Fertilizer, Milk, Crop yield.

### Localization (नेपाली)

- Language switch to Nepali renders all new M10 strings: Farm Details (`फार्म विवरण`, `फार्मका क्रियाकलापहरू`, `कुखुरा पालन — चालू छ`, `क्रियाकलाप अनुसार आम्दानी र खर्च`), breakdown rows with localized digits (`आम्दानी: $ ५०.०० … बाँकी: $ ५०.००`), the general label (`सामान्य (पूरै फार्म)`), the editor activity selector options, recent-row type/category (`खर्च`, `दाना`), and currency names (`अमेरिकी डलर (USD)`). English restored after the check.

## Defects found

None. Every M10 behavior exercised on-device behaved as specified. No new connected-test failures were introduced.

## Scope under test

The full M10 scope is defined in `docs/milestones/M10_FARM_ACTIVITIES.md`. Key behaviors: activity identity/ordering, disable-with-history rule, schema-13 migration, activity-aware category/calculator ordering, transaction→activity association, activity breakdown reconciling with whole-farm totals, and EN/NE parity. Deferred items remain out of scope (trades→activity association, per-activity operational domains, new `TransactionCategory` values) and are tracked in the milestone document.