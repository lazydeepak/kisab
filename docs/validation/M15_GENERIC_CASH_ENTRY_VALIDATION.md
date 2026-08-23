# M15 Validation Record — Generic Cash Income and Expense Entry

## Candidate identity

- **Branch**: `main` (working tree, committed at completion)
- **Baseline**: `3951e8c` (post-M14-kit; released v0.2.2 line)
- **Version**: 0.2.2 / versionCode 5 during development (version bump is a release act, not part of M15)
- **Device**: `ZA22374XPC` — Motorola Edge 60 Fusion, Android 16, API 36
- **Schema**: unchanged — **v14**

## Disposition

**PASS.** Generic cash income/expense is reachable from the Record sheet with zero Party/Product/Supply requirements, creates ordinary `FarmTransaction` records counted exactly once in authoritative totals, participates correctly in activity attribution, persists and backs up round-trip, and the obsolete legacy controls are fully removed.

## Automated gates

| Gate | Result |
|---|---|
| `:app:testDebugUnitTest` | **495 tests, 0 failures** (486 pre-M15 + 9 new) |
| `:app:lintDebug` | 0 errors |
| `:app:verifyLocal` | PASS |
| `:app:connectedDebugAndroidTest` (`ZA22374XPC`) | **140 tests, 0 failures** (138 baseline + 2 new workflow tests) |

## New automated coverage

`GenericCashEntryTest` (JVM):
1. `genericExpenseCreatesPlainTransactionWithoutPartyTradeOrSettlement`
2. `genericIncomeRequiresNoCustomerAndNeverCreatesReceivable`
3. `categoryMustMatchTransactionType` (SALES category on EXPENSE type rejected)
4. `totalsCountGenericEntriesExactlyOnce`
5. `editAndDeleteFollowOrdinaryTransactionSemantics` (category change to LABOR; delete reverts totals)
6. `activityAttributionDefaultGeneralExplicitBucketed` (+ reconciliation with `FarmTotals`; trade columns untouched)
7. `persistenceRoundTripPreservesGenericEntries` (schema-14 codec)
8. `backupRoundTripKeepsGenericEntriesStable` (byte-stable envelope)
9. `dailyOverviewCountsGenericIncomeAsMoneyReceived`

`GenericCashEntryWorkflowTest` (device): OTHER EXPENSE and OTHER INCOME end-to-end from the Record sheet — distinct editor titles, save, single transaction persisted with correct type/category/amount, no trades/settlements/parties.

## Defect found and fixed during validation

`FarmerOverview.received()` counted only settlements on SALE trades while `expenses()` counted every expense transaction — an asymmetry that was invisible until M15 created the first UI-reachable INCOME transactions. Verified live: after saving $30 generic income, the recent row existed but Today's Received tile stayed `$0.00`. Fixed by including generic INCOME transactions in the received total (`FarmerOverview.kt`); sales remains trade-gross; credit-sales untouched. Covered by `dailyOverviewCountsGenericIncomeAsMoneyReceived`; full connected suite re-run green afterwards.

## Live walkthrough evidence (debug build carrying the M15 change)

- Record sheet renders nine verbs: PRODUCTION · SELL · RECEIVED MONEY · BOUGHT · USED · **OTHER INCOME** · **OTHER EXPENSE** · PAID MONEY · CANCEL.
- OTHER EXPENSE → title "Record other expense" → $12.50 "Van fare" saved with no supplier/customer → Today Expenses tile `$12.50`, recent row `Expense | Other expense`.
- OTHER INCOME → title "Record other income" → Activity set to Poultry → saved; recent row shows `Poultry | Income | Other income…`; Khata confirmed free of parties/trades throughout.
- NE strings present with parity (`अन्य आम्दानी`, `अन्य खर्च`, titles `अन्य आम्दानी राख्नुहोस्` / `अन्य खर्च राख्नुहोस्`); LocalizationParityTest enforces key parity.

*Interruption note resolved:* the physical device was twice removed from Kisab mid-walkthrough by activity outside this milestone, deferring one visual check. **Re-check PASS (M14 setup pass, 2026-08-23):** on a freshly installed build at `37c4099`, OTHER INCOME $30 "ManureSale" with Activity=Poultry saved from the Record sheet, the ledger row rendered `Poultry | Income | Other income | ManureSale | $30.00`, and **Today's Received tile updated live to `$30.00`**; OTHER EXPENSE $12.50 "VanFare" likewise updated the Expenses tile; Khata showed zero parties/trades. Device then reset to the clean first-run state for P01.

## M14 interaction (accurate record)

M15's justification is source-grounded and rehearsal-confirmed, not pilot-derived: the capability gap was proven in code (create-mode editor reachable only from dead controls) and by live rehearsal on production v0.2.2 before any farmer session ran. PILOT-01 remains an ongoing parallel track; its participants have not seen this feature, and no M14 finding may be cited as having requested it. Future session evidence may still reshape wording or flow.

## Remaining notes

- Legacy string resources that only served deleted views were left in place to keep NE drift out of M15 scope; they can ride any later copy cleanup.
- The suggested-backup-filename staleness and other P3s remain tracked under M14 findings.
