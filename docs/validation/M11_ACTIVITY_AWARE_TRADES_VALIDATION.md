# M11 Validation Record — Activity-Aware Trades

## Candidate identity

- **Branch**: `feat/farm-activities`
- **Baseline**: `ed12a61` (post-M10)
- **Application ID**: `com.susankhya.kisab`
- **Version Name / Code**: `0.2.1` / `4` (debug)
- **Device**: `ZA22374XPC` — Motorola Edge 60 Fusion (`scout_g`), Android 16, API 36, density 450 (2.8×), 1220×2712
- **Debug APK SHA-256**: `61d898f116495d5727148860d16139774ae4fb8cba5b8199e639e66d4ff1386a` (from `local-ci-evidence.json`)

## M11 FINAL DISPOSITION: PASS

M11 is **PASS**. Every automated gate passes: the JVM unit suite grew from 469 to **484 tests with 0 failures**, lint reports 0 errors, and the connected device suite introduces **zero new failures** versus baseline (the same 33 pre-existing M9-class failures documented in the M10 record). The physical-device walkthrough — including a real schema-13 → schema-14 store migration round-trip on `ZA22374XPC` — passed with no defects: activity-tagged sales and supply purchases persist, settle, disable, and localize correctly, and legacy untagged trades remain General with no inferred history.

## Automated gates

| Gate | Result | Evidence |
|---|---|---|
| `:app:testDebugUnitTest` | 484 tests, 0 failures | PASS |
| `:app:lintDebug` | 0 errors | PASS |
| `:app:verifyLocal` | passed | `app/build/reports/verification/local-ci-evidence.json` |
| `:app:connectedDebugAndroidTest` | 141 tests, 33 failures — **all pre-existing baseline** | PASS (no new failures) |

### Connected-suite failure baseline

The connected suite reports 141 tests / 33 failures — byte-for-byte the same failure set as the M10 record: `FarmActivityWorkflowTest` (15), `FarmBackupIntegrationTest` (9), `FarmActivityPresentationTest` (5), `FarmPersistenceIntegrationTest` (2), `D002DateTimePickerEvaluationTest` (1), `FarmActivityShellRedesignTest` (1). All target legacy compatibility views hidden in the M7+ shell or assume a focused-window condition. M11 adds **zero** new device-test failures.

### New/updated automated coverage (M11)

- `TradeActivityTest` (10 tests): product-sale and supply-purchase activity association (+ persistence survival), General for untagged trades, edit changes activity while preserving settlements, settlement attribution to the settled trade's activity (customer and supplier side), disable-with-history keeping the trade bucket, disabled activity not selectable for new trades, editing keeps a disabled current selection, and the full mixed-scenario reconciliation: per-bucket cash figures sum exactly to `FarmTotals`, per-bucket trade figures sum exactly to trade-domain totals, buckets ordered by catalog display order with General last.
- `FarmActivityPersistenceTest` (+5 tests): schema-14 round trip preserving trade activity and settlement attribution; schema-13 payload with 6-part trades decodes as General and re-encodes as schema 14 with the trailing blank activity part; schema-14 payload with 7-part trades decodes the activity; unknown trade activity value fails decode (mirroring unknown categories); backup envelope round trip preserves trade activity byte-stably.
- Existing schema-version assertions updated 13 → 14 (`FarmSliceServiceTest`, `FarmFinancialOverviewTest`, `PartyLedgerTest`, `PartyHisabCalculatorTest`, byte-stable envelope fixtures).

## Physical-device walkthrough (executed)

All flows below were driven live on `ZA22374XPC` and verified via view-hierarchy dumps and persisted-store inspection.

### Setup

Created farm `M11Dev` (USD) with the Poultry activity; added customer `Ram`, supplier `Sita`, product `Eggs` (piece), supply `Feed` (kg). Farm state persisted across process restarts.

### Supplier purchase with activity (Bought sheet)

- BUY SUPPLY flow created supply `Feed` and supplier `Sita`, then rendered the purchase sheet with the new **Activity** selector showing `General (whole farm)` default.
- Selected `Poultry`, quantity 10 kg, total cost $3,000, Credit → saved. Farm Work shows `Feed · 10 kg remaining`; Khata shows Sita "To pay $ 3,000.00".

### Quick sale with activity (Sell sheet)

- Record → SELL opened Quick sale with the new **Activity** selector (`General (whole farm)` default).
- Selected `Poultry` (spinner lists General first, then enabled activities in catalog order), customer Ram, product Eggs, qty 10 × $500 = $5,000 total, Credit → "Sale saved". Khata: Ram "To receive $ 5,000.00".

### Activity-tagged trade rows

- Khata recent list renders tagged trades with the activity prefix: `Poultry | Sale — Ram · $ 5,000.00 due` and `Poultry | Purchase — Sita · $ 3,000.00 due`; after migration, legacy trades render without any prefix (General).

### Settlement attribution through the trade

- RECEIVED MONEY: paid $2,000 against Ram's poultry sale → Farm Details breakdown updated to `Poultry … Sales: $ 5,000.00 Purchases: $ 3,000.00 Received: $ 2,000.00 Paid: $ 0.00`.
- Paid $100 against Sita's **untagged** purchase → the payment stayed out of the Poultry bucket (General attribution) and payable dropped to $2,900.

### Farm Details — disable-with-history and breakdown

- Disabling Poultry flips the row to `POULTRY — PAUSED`; the breakdown **retains** the full trade bucket (`Sales: $ 5,000.00 Purchases: $ 3,000.00 Received: $ 2,000.00 Paid: $ 0.00`) under the paused activity.
- Breakdown renders the cash line plus the new M11 trade line per bucket; buckets with no records are omitted.

### Localization (नेपाली)

- Nepali renders the new trade line with localized digits: `बिक्री: $ ५,०००.०० किनेको: $ ३,०००.०० प्राप्त: $ २,०००.०० तिरेको: $ ०.००`, plus the paused label (`कुखुरा पालन — रोकिएको`). English restored after the check.

### Migration (schema 13 → 14) — real upgrade path

1. Force-stopped the app and replaced the `kisab_farm_store` payload with an equivalent **schema-13** payload (version field `13`, both trade records downgraded from 7 parts to 6 parts by stripping the trailing activity part).
2. Launched the app: the farm opened with all money intact (Sales $5,000, Received $2,000, receivable/payable unchanged); both legacy trades decoded as **General** — rendered without the activity prefix and with no activity inferred from product/supplier names.
3. Recorded a $100 supplier payment (a save-path mutation) and re-inspected the store: re-encoded as **schema 14** (18 fields) with both trade records upgraded to 7-part form carrying an empty trailing activity part — proving decode-time tolerant migration and encode-on-next-save behavior with no data loss and no invented associations.

## Defects found

None. One test-authoring fix during development (an incorrect expected-outstanding assertion in `TradeActivityTest.settlementsAttributeToTheSettledTradeActivity`) was corrected before the final green run; it was a test bug, not a product defect.

## Scope under test

The full M11 scope is defined in `docs/milestones/M11_ACTIVITY_AWARE_TRADES.md`. Key behaviors: optional activity association on product sales and supply purchases (`null` = General), governed `FarmActivityType` vocabulary with stable IDs, disable-with-history for trade-referenced activities, settlement attribution derived from the settled trade (no duplicated state), extended activity breakdown reconciling exactly with canonical totals (cash vs trade kept separate), schema-14 persistence with tolerant schema-13 migration, unchanged backup envelope format, EN/NE parity, and the explicit non-goals (no per-activity operational domains, no new activity types/categories).
