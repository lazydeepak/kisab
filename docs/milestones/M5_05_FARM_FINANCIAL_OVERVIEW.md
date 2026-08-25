# Kisab M5-05 — Farm Financial Overview — Design Record

Defines the fifth Hisab-Kitab slice: the **Farm Financial Overview** — a farm-wide financial synthesis of period summaries (cash activity, trade/payment flows, current position) and a compact monthly trend, presented over the same Party → Trade → Settlement projection discipline that M5-02, M5-03, and M5-04 established. M5-05 is **a derived read model, never a second accounting authority**: the overview is recomputed purely from the authoritative persisted facts on every render, so editing or deleting any underlying fact automatically changes the next overview and nothing can go stale.

> **Status: COMPLETE** on `feature/m5-05-farm-financial-overview` (from merged M5-04 `main`). This document is the deliberate design conversation required by the charter before implementation was accepted.

**Implemented:** `FinancialPeriodPreset` (THIS_MONTH / LAST_30_DAYS / ALL_TIME), `FinancialPeriod` (inclusive-start / exclusive-end), `FarmFinancialPeriods.periodFor` (pure, stable-clock period derivation), `FinancialCashTotals`, `FinancialTradeTotals`, `FinancialPosition`, `FinancialTrendRow`, `FarmFinancialOverview`, and the pure projection `buildFarmFinancialOverview` plus the `FarmState.financialOverview` extension; `FarmSliceService.farmFinancialOverview` read seam; the Financial Overview section inside Hisab-Kitab (period Spinner, Cash activity, Trade and payments, Current position with its as-of line, Monthly trend, per-section empty states, overflow error handling); en+ne strings; JVM tests. **No schema change, no new dependency, no committed state** — schema and backup bytes stay exactly at v6, verified byte-stable across a decode → project → re-encode round-trip.

## Working principles

- **Projection authority.** `FarmState` remains the single source of truth. Transactions, trades, settlements, and parties are read only; the overview never writes, mutates, or caches. There is intentionally no migration and no new persistence surface (`overviewDoesNotChangeEncodedFarmState`, `schemaV6RoundTripsThroughPersistenceAndBackupByteStableAfterProjection`).
- **Stable clock, reproducible periods.** Period bounds are derived purely from an injected `OffsetDateTime` `now` and `ZoneId` via `FarmFinancialPeriods.periodFor`, so every preset is deterministic across renders and fully reproducible in tests (`now` is a fixed instant, not `System.currentTimeMillis()`).
- **Inclusive-start / exclusive-end periods.** A fact with `occurredAt` at or after `startInclusive` and strictly before `endExclusive` is in the period; `startInclusive.minusNanos(1)` and `endExclusive` itself are out (`thisMonthPeriodIsStartInclusiveEndExclusiveInFixedNonUtcZone`, `last30DaysHasInclusiveStartAndExclusiveEnd`, `exactInstantAtLast30DaysBoundariesAreRespected`).
- **Exact arithmetic only.** All money aggregation uses minor-unit `Long` via `Math.addExact`/`Math.subtractExact`; overflow throws `ArithmeticException` instead of silently wrapping, and the UI maps that to the generic unexpected-error message (see Error state).
- **Home separation is preserved.** Cash totals come only from `FarmTransaction` records; trade activity and the position come only from trades and settlements. A paid sale never appears as Home income (`paidTradeHasNoPositionAndStaysOutOfCash`, `cashAndPartySaleAreNeverCombined`).
- **No orphaned financial events.** The projection re-asserts that every Settlement anchors to an existing Trade, throwing clearly on tampered input instead of silently dropping a money movement (`orphanSettlementIsRejectedByProjectionInsteadOfSilentlyDropped`).
- **The overview never claims profit.** Cash income/expense (Home) and trade/payment flows (Hisab-Kitab) are reported as separate activity totals and are never combined into a single net figure.

## Authoritative inputs

The projection reads, per farm:

- `FarmState.transactions` — Home cash activity (type INCOME/EXPENSE, amount, occurredAt).
- `FarmState.trades` — obligations (type SALE/PURCHASE, partyId, totalMinor, occurredAt).
- `FarmState.settlements` — payment/receipt events (tradeId, amountMinor, occurredAt).
- `FarmState.parties` — existing authoritative context for the farm; the overview aggregation does not read parties. Settlement direction is classified from the anchored trade's `Trade.type` (see Trade and payments / Current position), never from a party's role.
- `FarmState.currencyCode` — presentation only, via the existing farm currency formatter; the projection itself is currency-agnostic minor-unit arithmetic.

## Periods

```kotlin
enum class FinancialPeriodPreset { THIS_MONTH, LAST_30_DAYS, ALL_TIME }

data class FinancialPeriod(
    val preset: FinancialPeriodPreset,
    val startInclusive: OffsetDateTime,
    val endExclusive: OffsetDateTime
)
```

| Preset | startInclusive | endExclusive |
| --- | --- | --- |
| THIS_MONTH | first day of the local month at 00:00 (`now` in `zone`) | `start.plusMonths(1)` |
| LAST_30_DAYS | `now.minusDays(30)` | `now` |
| ALL_TIME | year 1 UTC (before every realistic persisted event) | `now` |

The selected preset is the single UI control over all four sections at once (cash, trade, position-as-of, and trend window source); the same preset drives each section's period filter.

## Formulas / sign conventions

All values are minor-unit `Long`. Activity totals are period-scoped (`inPeriod` = `!occurredAt.isBefore(startInclusive) && occurredAt.isBefore(endExclusive)`). The position is **as-of** the period's exclusive end, not the period's start.

### Cash activity (`FinancialCashTotals`)

```
income  = Σ amountMinor over transactions with type = INCOME in period
expense = Σ amountMinor over transactions with type = EXPENSE in period
net     = income − expense        // Math.subtractExact
```

### Trade and payments (`FinancialTradeTotals`)

```
grossSales         = Σ totalMinor over trades with type = SALE in period
grossPurchases     = Σ totalMinor over trades with type = PURCHASE in period
paymentsReceived   = Σ amountMinor over SALE-trade settlements in period
paymentsMade       = Σ amountMinor over PURCHASE-trade settlements in period
```

Settlement direction follows the **trade** it anchors: a settlement of a SALE trade is money received; a settlement of a PURCHASE trade is money paid.

### Current position (`FinancialPosition`)

Position is **as of `endExclusive`**: obligations that existed before the cutoff, net of settlements recorded before the cutoff — independent of the period's start (`positionAsOfEndExclusiveCountsPreCutoffTradesAndSettlementsOnly`).

```
for each trade where trade.occurredAt.isBefore(endExclusive):
    paid = Σ settlement.amountMinor for this trade where settlement.occurredAt.isBefore(endExclusive)
    outstanding = trade.totalMinor − paid          // Math.subtractExact
    if outstanding > 0:
        SALE     → receivable += outstanding       // Math.addExact
        PURCHASE → payable   += outstanding        // Math.addExact

positionNet = receivable − payable                  // Math.subtractExact, informational only
```

- Only **strictly positive** outstanding contributes; a fully settled trade contributes zero (`paidTradeHasNoPositionAndStaysOutOfCash`).
- A settlement at exactly `endExclusive` or after does not reduce the position.
- `netMinor` is informational only — it never settles any individual trade (mirrors M5-04's net-position rule; every trade's `PaymentStatus` stays exactly what its own settlements say).

### Monthly trend (`FinancialTrendRow`)

Facts are bucketed by their local-month `YearMonth` in the injected `zone`. Each row carries the six activity magnitudes for that month:

```
cashIncomeMinor, cashExpenseMinor,          // from FarmTransaction in that month
salesMinor, purchasesMinor,                 // from Trades in that month
paymentsReceivedMinor, paymentsMadeMinor    // from Settlements in that month (by anchored trade direction)
```

Rendering (see below) then presents zero-filled continuous months, capped to the newest 12, ordered oldest → newest.

## Cash-versus-trade separation

M5-05 deliberately reports three independent views and never merges them:

1. **Cash activity** — Home `FarmTransaction` income/expense only. Settlements are payment history, not Home cash events; a paid sale never adds income to the cash totals (unchanged since M5-02/M5-03).
2. **Trade and payments** — trade gross flows and settlement-driven payment flows. A cash sale/purchase (no party) still counts here as gross sales/purchases; only its cash proceeds would additionally appear as a separate Home cash transaction if the farmer records one — never automatically.
3. **Current position** — outstanding receivable/payable as of the period end, over all trades regardless of the period start.

`paidTradeHasNoPositionAndStaysOutOfCash` and `cashAndPartySaleAreNeverCombined` lock the two sides of the separation: trade/settlement activity never feeds cash totals, and a Home income of the same theme is never added to a trade's gross.

## As-of cutoff and the last-included display choice

The position's cutoff is **exclusive**: a trade or settlement at exactly `endExclusive` is not counted. The UI labels the position with an "As of:" timestamp, and it must not present the excluded boundary instant as if it were included.

**Display choice (documented):** the "As of:" line renders the last *included* instant — `period.endExclusive.minusNanos(1)` — not `endExclusive` itself. The localized MEDIUM formatter drops sub-second precision, so this reads naturally as the final second of the period (e.g. a THIS_MONTH period ending at the next month's midnight shows "…11:59:59 PM" of the current month's last day). The line therefore always states a timestamp whose facts are actually reflected in the position. The choice is documented in the `FinancialPosition` KDoc and implemented in `renderFinancialOverview` (`FarmActivity`). It is a presentation decision only: the projection's cutoff semantics are unchanged and remain `endExclusive`.

## Monthly trend: zero-fill, order, and the 12-row cap

`buildTrendRows` builds a compact, honest trend:

1. **Zero-fill.** Rows cover a **continuous** month range from the oldest present month to the newest present month; months with no facts are included as zero rows so gaps are visible rather than silently skipped (`monthlyTrendGroupsByMonthAndZeroFillsContinuousRange`).
2. **12-row cap.** When the continuous range exceeds `FarmFinancialPeriods.MAX_TREND_ROWS` (12), only the **newest 12** months are kept — the cap keeps the trend compact with no chart. Months outside the cap are dropped, so the first kept row may be zero-filled (`monthlyTrendCapsAtTwelveRowsAndOrdersOldestToNewest`).
3. **Oldest → newest.** Rows are ordered ascending by year/month; the UI renders them top-down in that order, with a strict-ascending assertion in the cap test.

The trend is deterministic: identical inputs produce identical rows across calls and insertion orders (`monthlyTrendIsDeterministicAcrossCalls`).

## Service operations (FarmSliceService)

- `farmFinancialOverview(farmId, preset, now, zone): FarmFinancialOverview` — the full derived read model; a pure read that never persists and never mutates state.

## Financial Overview UI (Hisab-Kitab section)

The Financial Overview is a section within the Hisab-Kitab destination (a sibling of the summary/trades/parties sections), always visible while a farm is open.

- **Period Spinner** — the preset selector (This month / Last 30 days / All time). Changing it re-renders all four sections for the new period. Labels come from `FarmLabels.financialPeriodPresetRes`; ordering from `FarmOrdering.financialPeriodPresets`.
- **Cash activity** — `Income: …`, `Expense: …`, `Net: …` (farm-currency formatted).
- **Trade and payments** — `Sales: …`, `Purchases: …`, `Payments received: …`, `Payments made: …`.
- **Current position** — an "As of:" line (last-included instant, see above), then `Receivable: …`, `Payable: …`, and a bold `Net position: …` (informational).
- **Monthly trend** — one line per row: `MMM yyyy | in … | out … | sales … | purchases … | received … | paid …`, all amounts farm-currency formatted.
- **Empty states.** Each section hides its figures and shows its own guidance line when the section has nothing to show (`No cash income or expenses in this period.`, `No sales, purchases, or payments in this period.`, `No money owed or owing at the end of this period.`, `No monthly activity in this period yet.`). Sections are shown/hidden independently by their own "has data" predicate.
- **Error state.** If the projection overflows (`ArithmeticException`), the render is caught, logged under the activity log tag, and the generic `FarmUiError.UNEXPECTED` localized message is shown; the section stays at its previous state rather than rendering garbage.

### Save/restore behavior

The selected period is part of instance-state save/restore: `onSaveInstanceState` stores `STATE_OVERVIEW_PERIOD_PRESET` (`overviewPeriodPreset.name`); `restoreOverviewPeriodFrom` reads it, falls back to the declaration default (`THIS_MONTH`) on any missing/unknown/unreadable value, and sets the field *before* synchronizing the Spinner selection so the selection listener does not re-render prematurely. On recreation the overview re-renders for the restored preset.

## Boundary decisions (explicit exclusions)

- **No schema change, no new persistence.** `FarmState`/schema stay at v6; the overview is computed on demand. Backup envelope bytes are unchanged (byte-stable decode → project → re-encode verified).
- **No second accounting authority.** The overview is a view over the facts; it never writes, migrates, or settles. The net position is informational and never settles a trade.
- **No profit figure.** Cash and trade flows are never combined into a single earnings number.
- **No trend chart, exports, or reports.** The trend is a compact textual month list; no analytics/reporting surface (M5-05 is an in-app screen, not a financial statement for external use).
- **Position as-of is period-end, not period-start.** Deliberate: the "current position" answers "what do I owe / am owed at the end of this period," including obligations that predate the window. Activity totals remain period-scoped.
- **No automatic Home cash-transaction creation** — unchanged since M5-03; a paid sale never double-appears as Home income.
- **No payment kinds, multi-trade allocations, or reversal semantics** — unchanged (M5-03 scope).

## Localization

New user-facing terms (all provisional, en+ne key parity maintained via `LocalizationParityTest`; 22 new keys per locale): Financial overview, Period, This month, Last 30 days, All time, Cash activity, Trade and payments, Current position, Monthly trend, Net, Sales, Purchases, Payments received, Payments made, Receivable, Payable, As of, and the four empty-state lines. Every resource has an English default; placeholder signatures match across locales.

## Implementation slices

1. **Domain** — `FarmFinancialOverview.kt`: preset/period types, `FarmFinancialPeriods.periodFor`, the four read-model types, `buildFarmFinancialOverview` projection + `FarmState.financialOverview` extension, `buildTrendRows` (continuous zero-fill, newest-12 cap, ascending), orphan-invariant guard.
2. **Service** — `FarmSliceService.farmFinancialOverview` read seam.
3. **UI** — Financial Overview container + Spinner + four sections in `activity_shell.xml`; `renderFinancialOverview`, period wiring, empty/error states, and save/restore in `FarmActivity`; `FarmLabels.financialPeriodPreset*`, `FarmOrdering.financialPeriodPresets`.
4. **Localization** — en+ne strings for all new user-facing terms.
5. **Validation** — JVM unit tests (see below), UI compile/lint.

## Validation goals (updated as evidence lands)

### Unit tests (`FarmFinancialOverviewTest` — 24 cases, all PASS)

- `emptyFarmShowsZeroTotalsAndEmptyTrend`, `cashActivitySumsIncomeAndExpenseInPeriod`, `thisMonthExcludesTransactionsOutsideTheMonth`
- `thisMonthPeriodIsStartInclusiveEndExclusiveInFixedNonUtcZone`, `last30DaysHasInclusiveStartAndExclusiveEnd`, `exactInstantAtLast30DaysBoundariesAreRespected`
- `tradeActivityCountsSalesPurchasesAndSettlementsInPeriod`
- `currentPositionCountsOutstandingOnlyAtEndOfPeriod`, `positionAsOfEndExclusiveCountsPreCutoffTradesAndSettlementsOnly`
- `partialAndFullySettledSalesAndPurchasesAcrossMixedParties`, `paidTradeHasNoPositionAndStaysOutOfCash`, `cashAndPartySaleAreNeverCombined`
- `editingFactsChangesNextOverview`
- `monthlyTrendGroupsByMonthAndZeroFillsContinuousRange`, `monthlyTrendIsDeterministicAcrossCalls`, `monthlyTrendCapsAtTwelveRowsAndOrdersOldestToNewest`
- `cashTotalsOverflowThrowsInsteadOfWrapping`, `grossTradeTotalsOverflowThrowsInsteadOfWrapping`, `settlementTotalsOverflowThrowsInsteadOfWrapping`, `receivablePositionAggregationOverflowThrowsInsteadOfWrapping`, `netPositionSubtractionDoesNotOverflowForMaximumNonNegativeInputs`
- `orphanSettlementIsRejectedByProjectionInsteadOfSilentlyDropped`, `overviewDoesNotChangeEncodedFarmState`, `schemaV6RoundTripsThroughPersistenceAndBackupByteStableAfterProjection`

Supporting mapping/ordering tests: `FarmLabelsMappingTest#everyFinancialPeriodPresetHasADistinctMapping`, `FarmOrderingTest#everyFinancialPeriodPresetIsPresentOnce`.

### Regression coverage carried by the gate

The pre-existing schema-v6 backup byte-stability and v5→v6 migration tests remain the guardians of the underlying authority the overview reads from, and are re-asserted inside M5-05's own byte-stability test:

- `FarmSliceServiceTest#schema5MigrationCreatesDeterministicOpeningSettlements`, `FarmSliceServiceTest#backupEnvelopeFormatIsByteStable`, `FarmSliceServiceTest#schema4FarmWithTradesRoundTripByteStable`, `tradeRoundTripsThroughPersistenceAndBackup`, `backupEnvelopeRoundTripsFarmState`, and the schema-2/3/4→v6 upgrade tests.
- `LocalizationParityTest` — en+ne parity for the 22 new overview keys.

### Toolchain evidence (2026-08-11)

`testDebugUnitTest` (185 tests, 0 failures/errors), `assembleDebug`, `lintDebug`, and `compileDebugAndroidTestKotlin` all SUCCESS on the `feature/m5-05-farm-financial-overview` branch; `git diff --check` clean.

### Manual / device UI validation — NOT performed

No manual or device/emulator UI validation was performed for M5-05 in this pass. The interactive overview flows (period switching re-renders all four sections, empty-state toggling, overflow error handling, period restore over configuration change, Nepali rendering) are covered by code inspection and automated unit-toolchain checks only. Per M4-04 disposition, timed Android tests on the API-26 emulator remain supplemental and are not a completion gate; device validation should still be scheduled before any production release of this feature.

> **Update (RC-01, 2026-08-14):** the main on-device gap for Financial Overview was later closed on the API-36 physical device. `FarmOverviewAndHisabDeviceTest` exercises period switching, empty states, and recreation restore as part of the connected RC-01 evidence recorded in `docs/release/V0.2.0_RELEASE_CHECKLIST.md`. Overflow handling and Nepali rendering remain unit-covered rather than separately manual/device-proven.

## M6 boundary

M5-05 stops at a farm-wide financial overview (period summaries, position-as-of, compact monthly trend). **M6 — Farmer Hisab calculators** is next: farmer hisab lookups and calculations built over the same Party → Trade → Settlement projection discipline (for example per-party or per-period hisab reconciliation). No M6 work is performed in this milestone.
