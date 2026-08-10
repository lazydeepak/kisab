# Kisab M5-03 — Settlements (Receivable / Payable) — Design Record

Defines the third Hisab-Kitab slice: the **Settlement** — the actual payment or receipt event applied against a single sale/purchase. M5-03 completes the architectural transition that M5-02's limited `paidMinor` aggregate was always heading toward: a **Trade is the obligation/business event**, a **Settlement is the payment/receipt event**, and **Paid / Due / Payment status are projections derived from settlements** — never stored on the trade itself. The Hisab-Kitab destination gains a per-trade payment ledger (list + new/edit/delete form) with a derived "Paid · Due" line, and the to-receive/to-pay summary now totals settlement projections. No aggregate per-party ledger (M5-04) and no automatic Home cash-transaction generation are added; each is deliberately out of scope (see "Boundary decisions").

> **Status: COMPLETE** on `feature/m5-03-settlements` (from merged M5-02 `main`). This document is the deliberate design conversation required by the charter before implementation.

**Implemented (2026-08-11):** `Settlement`, `SettlementDraft`, `TradePaymentSummary`, and settlement projections (`paidMinorFor`, `outstandingMinorFor`, `paymentStatusFor`, `paymentSummaryFor`); `FarmState.settlements` → schema v6 codec with a deterministic v5→v6 migration (one opening settlement per v5 trade that had `paidMinor > 0`, dated at the trade's own `occurredAt`, id derived via `UUID.nameUUIDFromBytes`) and backup round-trip; `FarmSliceService` settlement CRUD (`addSettlement`/`updateSettlement`/`deleteSettlement`/`settlement`/`settlements`/`settlementsForTrade`/`tradePaymentSummary`) plus `addTradeWithInitialSettlement` for the create-with-initial-payment path; `FarmStateValidator` rules for settlement money and the outstanding-requires-party invariant; trade delete blocked while payment records exist; the per-trade payment editor (list + form) with dirty-discard, back handling, and instance-state restore; the trade editor's Edit mode now shows a read-only Paid/Due line and a **Payments** button while Create mode keeps the initial-payment radio controls; en+ne strings. Summary/balance excludes trades and settlements (see "Boundary decisions").

## Working principles

- **Minimal dependency budget.** Continues the AppCompat view discipline; no new libraries.
- **A trade is an obligation; a settlement is a money movement; status is a projection.** A SALE/trade is "what the farmer is owed", a PURCHASE/trade is "what the farmer owes". The actual receipt (sale) or payout (purchase) is a separate `Settlement` record. `Paid`, `Due`, and `PaymentStatus` are *always* recomputed from the trade's settlements and are never persisted — so there is no way for three stored values to disagree, and no migration surface for payment status itself.
- **Settlements are history, not balances.** They are append-only in spirit: each payment record has its own id, amount, timestamp, and optional note, and only the *sum* for a trade feeds the projections. This preserves an auditable payment trail for the future per-party ledger (M5-04).
- **Persistence evolution, not a rewrite.** Farm schema advances from v5 to v6 by dropping the now-derived `paidMinor` from the trade row and appending a `settlements` list. Migration turns a v5 trade's `paidMinor` into exactly one deterministic opening settlement, so re-encoding the same v5 payload always yields the same state.
- **Single source of truth stays.** `FarmSliceService` and `FarmStore` remain the domain/persistence seams; the UI never touches the codec directly.

## Settlement model

```kotlin
data class Settlement(
    val id: String,            // "settlement-<uuid>" (native), deterministic UUID (v5→v6 migration)
    val tradeId: String,       // anchor to the single trade being settled
    val amountMinor: Long,     // > 0; the sum for a trade never exceeds trade.totalMinor
    val occurredAt: OffsetDateTime, // normalized to UTC like trades/transactions
    val note: String = ""      // optional free text
)

data class SettlementDraft(
    val tradeId: String,
    val amountMinor: Long,
    val occurredAt: String,    // ISO-8601; toSettlement(id) normalizes to UTC
    val note: String = ""
)
```

Direction is not stored on the settlement: a settlement against a SALE is money **received**, against a PURCHASE money **paid**, derived through the trade's `type`. The settlement never carries a currency (owned by `FarmState`) or a repeated party id (reached through the trade, so later party corrections stay consistent with history).

### Projections (never persisted)

```kotlin
fun List<Settlement>.paidMinorFor(tradeId: String): Long            // sum for the trade; 0 when none
fun List<Settlement>.outstandingMinorFor(trade: Trade): Long        // totalMinor - paid
fun List<Settlement>.paymentStatusFor(trade: Trade): PaymentStatus  // paymentStatusOf(total, paid)
fun List<Settlement>.paymentSummaryFor(trade: Trade): TradePaymentSummary

data class TradePaymentSummary(
    val paidMinor: Long,
    val outstandingMinor: Long,
    val status: PaymentStatus  // UNPAID | PARTIAL | PAID
)
```

`Trade` itself therefore keeps only `totalMinor` (+ party + description + occurredAt); M5-02's persisted `paidMinor` is gone.

## Service operations (FarmSliceService)

- `addTrade(farmId, draft): Trade` — creates a trade with **no** payments (unpaid; requires a party when outstanding > 0).
- `addTradeWithInitialSettlement(farmId, draft, initialSettlementMinor: Long?): Trade` — one atomic create: if `initialSettlementMinor > 0` it also writes a first settlement dated at the trade's own `occurredAt` (the M5-02 equivalent of "paid at the time of the trade"). The full-total value reproduces the old "Paid" choice; `null`/0 reproduces "Unpaid"; values in `(0, total)` reproduce "Partially paid".
- `updateTrade(farmId, tradeId, draft)` — total may be reduced only down to the already-settled amount ("Trade total cannot be less than the settled amount").
- `deleteTrade(farmId, tradeId)` — **blocked while any settlement references the trade** ("Trade cannot be deleted while payment records exist"); delete the payments first.
- `addSettlement(farmId, draft)` / `updateSettlement(farmId, settlementId, draft)` / `deleteSettlement(farmId, settlementId)` — full CRUD against a trade.
- `settlement` / `settlements` / `settlementsForTrade(farmId, tradeId)` — the per-trade history, newest first (occurredAt desc, then insertion order).
- `tradePaymentSummary(farmId, trade): TradePaymentSummary` — the derived Paid/Due/status for one trade (what the trade list, editor line, and Hisab-Kitab summary all render).
- `trades(farmId)` / `trade(farmId, tradeId)` unchanged from M5-02.

Validation (`FarmStateValidator`): a settlement's `amountMinor > 0`, its `tradeId` exists, and the trade's accumulated settlements never exceed the total; an outstanding trade (paid < total) still requires a party (unchanged invariant) — so a settlement can never leave the farm holding a party-less unpaid trade. `validateFarm` runs every trade and every settlement through their rules on every persisted transition.

## Persistence evolution (schema v6)

`FarmState` gains `settlements: MutableList<Settlement> = mutableListOf()` and `CURRENT_FARM_SCHEMA_VERSION` becomes **6**.

`FarmPersistenceCodec` encoding (schema 6):

```
6 \u001F <id> \u001F <name> \u001F <entries> \u001F <currency> \u001F <transactions> \u001F <parties> \u001F <trades> \u001F <settlements>
```

- Each trade encodes as `id \u001D type \u001D partyId (blank for cash) \u001D totalMinor \u001D description \u001D occurredAt` — the `paidMinor` field is **gone**.
- Each settlement encodes as `id \u001D tradeId \u001D amountMinor \u001D note \u001D occurredAt`.

Decode rules:
- **Schema 6** — reads all 9 fields; both lists decoded directly.
- **Schema 5** — reads the v5 trades (7-field rows including `paidMinor`), builds trades, and migrates each `paidMinor > 0` into exactly one **deterministic opening settlement**: `id = UUID.nameUUIDFromBytes("kisab:v5-opening-settlement:" + tradeId)`, `amountMinor = paidMinor`, `occurredAt = trade.occurredAt` (the true payment instant is unknowable; the trade timestamp is an intentional approximation — never "now", so re-decoding is reproducible).
- **Schema 4 and below** — decode as before with empty `trades` and `settlements`. Upgrade is implicit; the next `saveFarm` rewrites at v6.
- **Backup envelope unchanged.** `FarmBackupCodec` (envelope schema 1) wraps `FarmPersistenceCodec`, so settlements flow through existing backups; old backups restore with empty lists.

## Trade editor / settlement editor boundary

- **Trade editor (Edit mode)** — payment status is now read-only and derived: a **Paid/Due** line plus a **Payments** button that opens that trade's payment ledger. The old tri-state radios and amount-paid field are hidden; editing a trade changes total/party/description/date only.
- **Trade editor (Create mode)** — unchanged M5-02 UX: Paid / Partially paid / Unpaid + amount-paid. Save calls `addTradeWithInitialSettlement` so the initial payment becomes a proper settlement record.
- **Settlement editor** — a per-trade screen showing the trade summary (type · party · total), the derived Paid/Due line, the payment history list (amount, note, date), and an **Add payment** action (labels differ by direction: "Receive payment" for sales, "Record payment" for purchases). New/Edit form captures amount, date/time (existing picker pattern), and optional note; Delete is available while editing. Dirty-discard and back-handling follow the established editor conventions, and the open screen survives configuration changes via `onSaveInstanceState`.
- **Delete guards** — deleting a trade with payments is blocked with a clear message and the user must remove the payments first; reducing a trade total below already-settled money is rejected.

## Boundary decisions (explicit exclusions)

- **Home cashflow separation is preserved.** Trades and settlements never feed `FarmSummary.balanceMinor` or the Home income/expense figures. A paid sale does **not** appear as income on Home in this slice, so the M1–M4 cash balance stays exactly its historical definition.
- **No automatic Home cash-transaction creation.** Generating Home income/expense records from settlements would double-count against the manual ledger; that linkage is deliberately **out of scope** and will be designed separately (not by contaminating the clean settlement ledger).
- **No per-party ledger yet.** Aggregating a party's sales, purchases, payments received/made, running balance, and current receivable/payable position is **M5-04 (Party Khata / Ledger)** — mostly a projection over the Party → Trade → Settlement model this milestone establishes.
- **No payment *kinds*** (advance, partial credit, adjustment, reversal) and no payment-to-multiple-trades grid allocation. Each settlement targets exactly one trade; multi-trade and adjustment semantics are future decisions.

## Localization

New user-facing terms added to `docs/localization/NEPALI_TERMINOLOGY.md` before shipping: Payments (भुक्तानीहरू), Receive payment (भुक्तानी लिनुहोस्), Record payment (भुक्तानी रेकर्ड गर्नुहोस्), Add payment (भुक्तानी थप्नुहोस्), New payment (नयाँ भुक्तानी), Edit payment (भुक्तानी सम्पादन गर्नुहोस्), Update payment (भुक्तानी अद्यावधिक गर्नुहोस्), Delete payment (भुक्तानी मेटाउनुहोस्), Payment amount (भुक्तानी रकम), Payment note (टिप्पणी), Payments received (प्राप्त भुक्तानीहरू), Payments made (गरिएका भुक्तानीहरू), No payments recorded yet (अहिलेसम्म कुनै भुक्तानी रेकर्ड छैन), Paid (भुक्तानी), Due (बाँकी). Every Nepali key has an English default (M4 glossary rule), and placeholder signatures match across locales.

## Implementation slices

1. **Domain** — `Settlement`, `SettlementDraft`, `TradePaymentSummary`, projections; `Trade` drops `paidMinor`.
2. **Persistence** — `FarmState.settlements`, schema v6 codec, deterministic v5→v6 opening-settlement migration, backup round-trip.
3. **Service** — settlement CRUD + per-trade queries + `tradePaymentSummary`; `addTradeWithInitialSettlement`; trade-delete/total guards.
4. **UI** — settlement editor (list + form) with dirty-discard/back/restore; trade editor Edit-mode Paid/Due + Payments button; create-with-initial-payment save; en+ne strings.
5. **Validation** — JVM unit tests (settlement CRUD/ordering/over-remaining, delete-blocked trade, schema-5 → v6 deterministic migration, schema-version and byte-stable updates), lint Debug + Release, AndroidTest compile.

## Validation goals (updated as evidence lands)

- `FarmSliceServiceTest` settlement CRUD, ordering, over-remaining rejection, trade-delete-blocked, deterministic v5→v6 opening settlements, schema-2/3/4→v6 and round-trips, byte-stable envelope (schema-6 payload layout); all trade tests migrated off the removed `paidMinor` API to settlement projections; `LocalizationParityTest` covers the new en+ne keys and placeholders; full JVM `testDebugUnitTest`; lint Debug + Release.

**Evidence:** full JVM suite **142 tests PASS** (`testDebugUnitTest`) — including the new settlement CRUD/ordering/over-remaining/delete-blocked tests, the deterministic schema-5→v6 migration test, migrated trade tests, and the earlier known pre-existing `TimePresentationTest#displaysStoredInstantInDeviceZoneWithoutUtcLiteral` failure is now **fixed as test robustness**: the assertion normalizes the JDK-CLDR whitespace variant (narrow no-break space vs plain space) that differs across JDK builds, so the display behavior it checks is unchanged — no product feature was touched. `compileDebugKotlin`, `compileDebugAndroidTestKotlin`, `assembleDebug`, and `lintDebug` all SUCCESS. Timed Android tests on the API-26 emulator remain supplemental per M4-04 disposition (not a completion gate).

## M5-04 boundary

M5-03 deliberately stops at per-trade payments and the farm-wide receivable/payable totals. **M5-04 — Party Khata / Ledger** is next: a chronological, per-party projection of Sales, Purchases, Payments received, Payments made, running party balance, and the current receivable/payable position — built over the Party → Trade → Settlement model as an additional projection, not a second accounting authority.