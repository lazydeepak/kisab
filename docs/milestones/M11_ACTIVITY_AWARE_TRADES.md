# Kisab M11 — Activity-Aware Trades

## Status

**M11 FINAL DISPOSITION: PASS**

Implemented on `feat/farm-activities` on top of M10 at `ed12a61`. This record is the authority for the M11 scope: extending the M10 Farm Activity model into the trade domains (product sales / customer-side flows and supply purchases / supplier-side flows) so activity-level financial reporting attributes trade-derived amounts correctly, without expanding into new operational domains or redesigning the accounting architecture.

Validation evidence: `docs/validation/M11_ACTIVITY_AWARE_TRADES_VALIDATION.md` (484 unit tests / 0 failures, lint 0 errors, connected suite at baseline with zero new failures, physical-device walkthrough including a real schema-13 → schema-14 migration on `ZA22374XPC`).

## 1. Problem being solved

M10 associates ordinary farm transactions (the money-in/out ledger) with activities, but trade-domain flows are not activity-aware:

- **Product sales** (`addProductSale`) create a `Trade` + optional initial `Settlement` — no activity association exists.
- **Credit supply purchases** (`addSupplierPurchase` with a supplier) create a `Trade` + optional initial `Settlement` — no activity association exists.
- **Supplier/customer payments** (`recordSupplierPayment`, `recordCustomerPayment`, `addSettlement`) are `Settlement` records anchored to trades — no activity exists anywhere in the trade graph.

The M10 activity breakdown (`farmActivityBreakdown`) is computed over `FarmTransaction` records only. As a result a poultry egg sale recorded as a product sale is invisible to the Poultry activity bucket and falls into no activity at all, making the Farm Details breakdown incomplete for mixed farms.

## 2. Domain boundary

This milestone extends activity association **only** to the existing trade domain (`Trade` / `Settlement` / `TradeDraft`). It does not touch:

- the money-in/out ledger beyond what M10 already does (`FarmTransaction.activity` is unchanged);
- quantity/unit facts (`ProductSaleDetail`, `SupplyPurchaseDetail`) — they are not financial authorities;
- the Party → Trade → Settlement projection architecture (`PartyLedger`, `FarmFinancialOverview`, `PartyHisabCalculator` are unchanged and must keep passing unchanged);
- production, inventory/planning domains, or any new `FarmActivityType` / `TransactionCategory`.

### Activity-ownership decision (ground-truth resolution)

**Activity ownership lives on the `Trade` object** (`Trade.activity: FarmActivityType? = null`). This is the single persisted location, and it is justified by the existing source-of-truth architecture:

1. **A trade is the financial record of a trade flow.** `Trade` carries the obligation (`totalMinor`); `Settlement` is payment history anchored to a trade; `ProductSaleDetail`/`SupplyPurchaseDetail` are quantity/unit facts. There is no separate "financial transaction representation" of a trade — the trade itself is the authority, so activity belongs on it.
2. **No duplicated state.** `Settlement` records derive their activity from their trade (a settlement never stores activity). This mirrors the existing invariant in `Settlement.kt`: settlements never repeat `Trade.partyId` because "the relationship reaches the party through the trade, so a later party correction on the trade stays consistent with history." Activity follows the identical single-source pattern, so there is no secondary copy to drift.
3. **Cash-vs-trade separation is preserved.** A cash supply purchase (`addSupplyPurchase`, no supplier) already produces a `FarmTransaction` that is activity-aware through M10; a trade flow produces a `Trade`. The two flows are mutually exclusive per event and represent different authorities, so a `FarmTransaction.activity` and a `Trade.activity` never describe the same money and never need to be kept synchronized.
4. **Disable-with-history works.** A trade keeps its activity even after the farm disables the activity; settlements inherit it, so historical payments stay in their activity bucket.

## 3. Accounting semantics

Trace the full financial effect per flow (all amounts exact `Long` minor units):

| Flow | Record created | Activity attribution |
|---|---|---|
| Cash sale | `Trade(SALE)` + initial `Settlement(totalMinor)` | Sale obligation → `trade.activity`; received payment → `trade.activity` |
| Credit sale | `Trade(SALE)`, no settlement | Sale obligation → `trade.activity` |
| Partially paid sale | `Trade(SALE)` + initial `Settlement(paid)` | Both → `trade.activity` |
| Customer settlement later | `Settlement(amount)` on existing SALE trade | → activity of the settled trade |
| Cash supply purchase | `FarmTransaction(EXPENSE)` (M10 flow) | → transaction activity (unchanged) |
| Credit supply purchase | `Trade(PURCHASE)` + optional initial settlement | → `trade.activity` |
| Partially paid purchase | `Trade(PURCHASE)` + initial settlement | → `trade.activity` |
| Supplier payment later | `Settlement(amount)` on existing PURCHASE trade | → activity of the settled trade |
| Edit | `updateTrade` replaces the trade | New activity replaces old; settlements unchanged and derive from the new trade activity |
| Delete | `deleteTrade` (blocked while payments exist) | Removed with the trade; no remapping |

Explicit rules:

- **Receivable/payable creation is not cash movement.** A SALE trade adds to gross sales / receivable, never to cash income. The activity breakdown keeps cash and trade projections in separate columns that are never summed.
- **Settlements never carry their own activity**; they derive it from the trade they settle. A later settlement therefore cannot be "incorrectly attributed": it always follows the trade.
- **Historical records without an activity decode as `null` = General** and behave exactly as before. Activity is never inferred from product/category names (no manufacturing of history).
- **Disabled-activity history is preserved.** Trades and their settlements referencing a disabled activity keep that activity. Disabling is governed by the same M10 rule (see section 5).
- **The reconcile invariant.** Every activity bucket partitions the farm's monetary facts exactly once:

  - Σ `incomeMinor` over buckets = `FarmTotals.incomeMinor` (cash only, unchanged)
  - Σ `expenseMinor` over buckets = `FarmTotals.expensesMinor` (cash only, unchanged)
  - Σ `balanceMinor` over buckets = `FarmTotals.balanceMinor`
  - Σ `grossSalesMinor` over buckets = Σ over SALE trades of `totalMinor`
  - Σ `grossPurchasesMinor` over buckets = Σ over PURCHASE trades of `totalMinor`
  - Σ `paymentsReceivedMinor` over buckets = Σ settlements on SALE trades
  - Σ `paymentsMadeMinor` over buckets = Σ settlements on PURCHASE trades

  These are the canonical farm totals the breakdown represents: cash (`FarmTotals`) and trade (`FarmFinancialOverview.tradeTotals` at ALL_TIME). Cash and trade are reported as separate totals, preserving the M5-05 rule that they are never combined into one figure.

## 4. Persistence and migration strategy (schema 14)

- `Trade` gains `activity: FarmActivityType? = null`; `TradeDraft` mirrors it.
- Schema version bumps `13 → 14`. Encoding writes the activity as a **trailing 7th part of each trade record** (empty = general), exactly mirroring how M10 appended activity to transaction records.
- `decodeSchema14` accepts **both 6-part (pre-M11) and 7-part trade records**: legacy trades decode with `activity = null` (General). Unknown activity names fail the decode exactly like unknown category/activity values today (governed codec policy).
- Migration is decode-time tolerant, matching M10: a schema-13 payload decodes into a schema-14 `FarmState` (in-memory), and the next edit/save re-encodes as schema 14 with 7-part trade records. No rewrite is forced on load.
- The backup envelope (`FarmBackupCodec`, schema v1) is **unchanged**: it wraps the versioned payload Base64, so schema-13 backups and the byte-stable test contract stay intact.
- `FarmState.CURRENT_FARM_SCHEMA_VERSION` → `14`; `FarmPersistenceCodec.CURRENT_SCHEMA_VERSION` → `14`.

## 5. Disabled-activity semantics

Consistent with M10 ordinary transactions:

- `setFarmActivities` / `disableFarmActivity` must consider **trade references** (not just transaction references) when deciding which removed activities move to `disabledActivities`. Without this, disabling an activity referenced only by trades would drop it entirely (history becomes unreachable and the breakdown bucket vanishes).
- Disabled activities **may not be selected for new trades**; `FarmActivityCatalog.activityChoices` provides General + enabled activities only, and appends a disabled current association only when **editing** an existing trade so the selection is never silently dropped.
- Historical trades and settlements referencing a disabled activity keep their bucket. Never silently remap disabled historical activities to General.

## 6. UI changes

- **Trade editor** (Sale/Purchase inline editor, `activity_shell.xml` `tradeEditorContainer`): add an Activity selector (label + `Spinner`) between the party spinner and the total field. Options = `FarmActivityCatalog.activityChoices(farm.activities, tradeEditorState.activity)` with the General option first; labels via `FarmLabels.activityType` / the existing General string. Hidden on farms with no activities (General-only), matching the transaction editor's `show = hasEnabled || currentActivity != null` rule. Editing an existing trade preserves the association (appended disabled choice). `TradeEditorState` gains `activity`, persisted across recreation in `onSaveInstanceState` like the other editor fields.
- **Quick sale dialog**: add the same selector (only when the farm has activities); the chosen activity flows to `addProductSale`.
- **Supplier purchase dialog**: add the same selector; flows to `addSupplierPurchase`.
- **Recent trade rows**: activity prefix shown where it helps read trade history (SALE/PURCHASE rows), consistent with M10's `transaction_row_with_activity_format` style.
- **Farm Details breakdown** (`renderFarmActivityBreakdown`): render the extended per-activity row with the trade projection (gross sales / gross purchases / payments received / payments made) in addition to cash income/expense/balance, using existing currency formatting. The cash and trade figures stay visually separated.
- **EN/NE parity is mandatory** for all new strings (`LocalizationParityTest` enforces it).

Avoid unrelated visual redesign.

## 7. Compatibility requirements

- Pre-M11 payloads (all schemas ≤ 13) decode unchanged; trades without activity are General; activity lists untouched.
- `FarmFinancialOverview`, `PartyLedger`, `PartyHisabCalculator`, `FarmTotals`, `FarmerOverview`, backup/restore, and the byte-stable backup contract must keep passing **unchanged** — M11 must not weaken or alter their behavior.
- Unknown activity values in trade records fail decode like unknown categories (existing governed policy), not silently ignored.

## 8. Acceptance criteria

1. `addProductSale`, `addSupplierPurchase` (supplier form), `addTrade`, and `addTradeWithInitialSettlement` accept an optional activity and persist it.
2. `updateTrade` preserves or changes the activity; delete removes the trade (no activity remapping).
3. Historical trades without activity decode as General; schema-13 payloads load; saving upgrades to schema 14; activity-aware trades survive process recreation and backup/restore round-trips.
4. Settlements attribute to the settled trade's activity.
5. The extended `farmActivityBreakdown` reconciles exactly: cash columns sum to `FarmTotals`; trade columns sum to the trade-domain totals. Mixed scenarios (tagged income/expense, General, sales, purchases, credit, partial payments, settlements, disabled historical activities) hold the invariant.
6. Disabled activities with trade history are preserved and their records stay readable; disabled activities are not selectable for new trades; editing keeps a disabled current association.
7. EN/NE parity passes; `LocalizationParityTest` green.
8. `./gradlew :app:verifyLocal` passes (JVM tests, lint, debug assembly, androidTest compilation).

## 9. Explicit non-goals

Do **not** expand M11 into:

- per-activity production/operational domains;
- new `FarmActivityType` values;
- new `TransactionCategory` values;
- Farm Management visual polish;
- push-delivery backend;
- account system;
- premium/subscriptions;
- ads;
- cloud/sync/backend infrastructure;
- unrelated refactoring.

Those remain separate work.