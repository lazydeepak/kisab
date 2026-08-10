# Kisab M5-02 — Sale/Purchase (Trades) with Payment Status — Design Record

Defines the second Hisab-Kitab slice: the **Sale/Purchase (Trade)** record — the actual hisab/khata line item. A trade captures money *received* by the farmer (a sale) or money *paid out* (a purchase) against a party or as cash, plus how much of it has been settled (`paidMinor`). M5-02 extends the M5-01 party foundation with a trade editor/list on the Hisab-Kitab destination and a farm-schema evolution that persists trades, so that a future hisab/khata summary can compute "to receive" vs "to pay" without re-entry. No aggregate balances/is-a-and/or ledger screens are added in this slice; only the per-trade record and lists.

> **Status: COMPLETE** on `feature/m5-02-sale-purchase` (from merged M5-01 `main`). This document is the deliberate design conversation required by the charter before implementation.

**Implemented (2026-08-10):** `TradeType`, `PaymentStatus`, `Trade`, `TradeDraft` with validator rules and derived `paymentStatus()`/`outstandingMinor()`; `FarmState.trades` → schema v5 codec with v4-and-below migration and backup round-trip; `FarmSliceService` trade CRUD (`addTrade`/`updateTrade`/`deleteTrade`/`trade`/`trades`) plus the party guards (delete blocked while referenced, role-change blocked while incompatible, `BOTH` role added and compatible with both trade types); trade list + editor (type-fixed, party, total, payment tri-state, amount-paid) in the Hisab-Kitab destination with dirty-discard protection and en+ne strings. Summary/balance intentionally excludes trades (see "Boundary decisions").

## Working principles

- **Minimal dependency budget.** Continues the AppCompat view discipline; no new libraries.
- **A trade is a numbered, dated, monetary line, not a transaction.** Trades deliberately reuse the transaction shape — id, amount, date, description — but keep a different field set and different validation, because they answer a different question ("who owes/owes me?"). They are stored beside transactions, never merged into them, so the farm cash balance (Home summary) stays exactly the M1–M4 definition.
- **Payment is user-facing state; amount is a derived fact.** The editor asks for a tri-state payment status (Paid / Partially paid / Unpaid) plus an amount for the PARTIAL case. The model stores **only** `totalMinor` and `paidMinor`; `PaymentStatus` and `outstandingMinor()` are derived values, never persisted — so there is no way for the three to disagree and no migration surface for them.
- **Party linking is role-checked and reference-safe.** A sale may link a `CUSTOMER` (or `BOTH`), a purchase a `SUPPLIER` (or `BOTH`). Partially-paid or unpaid trades *require* a party (that is the udhar the farmer is tracking); fully-paid trades may be cash with no party. Delete/role-change of a referenced party is blocked with a clear message rather than silently orphaning a trade.
- **Persistence evolution, not a rewrite.** Farm schema advances from v4 to v5 by appending a `trades` list. Existing v1–v4 payloads and v1 backup envelopes keep decoding (empty trades) — the same compatibility discipline as M5-01 and M4-05 F001.
- **Single source of truth stays.** `FarmSliceService` and `FarmStore` remain the domain/persistence seams; the UI never touches the codec directly.

## Trade model

```kotlin
enum class TradeType { SALE, PURCHASE }

enum class PaymentStatus { PAID, PARTIAL, UNPAID }

data class Trade(
    val id: String,
    val type: TradeType,
    val partyId: String?,          // null => cash trade
    val totalMinor: Long,          // whole money units; > 0
    val paidMinor: Long,           // in [0, totalMinor]
    val description: String = "",  // free text, optional
    val occurredAt: OffsetDateTime // normalized to UTC like transactions
)

data class TradeDraft(
    val type: TradeType,
    val partyId: String?,
    val totalMinor: Long,
    val paidMinor: Long,
    val description: String = "",
    val occurredAt: String
) // toTrade(id) normalizes the timestamp to UTC
```

### Field semantics

| Field | Required | Rule |
| --- | --- | --- |
| `id` | yes (service-assigned) | unique within a farm; `trade-<uuid>` |
| `type` | yes | fixed at creation; a Sale stays a Sale |
| `partyId` | no | must exist in the farm; role must be compatible with `type`; **required** when `paidMinor < totalMinor`; null means a cash trade |
| `totalMinor` | yes | `> 0` |
| `paidMinor` | yes | `0 <= paidMinor <= totalMinor` |
| `description` | no | free text; blank stored as `""` |
| `occurredAt` | yes | ISO-8601; stored UTC like transactions |

### Derived values (never persisted)

```kotlin
fun Trade.paymentStatus(): PaymentStatus = when {
    paidMinor <= 0 -> PaymentStatus.UNPAID
    paidMinor >= totalMinor -> PaymentStatus.PAID
    else -> PaymentStatus.PARTIAL
}

fun Trade.outstandingMinor(): Long = totalMinor - paidMinor
```

### Role compatibility (extended)

`compatibleWith(tradeType)` is added to `PartyRole`. `BOTH` (ग्राहक र आपूर्तिकर्ता दुवै) is a new third-choice role, defined so a party that truly deals on both sides is not forced into a false dichotomy and does not get blocked from trades:

| Role | SALE | PURCHASE |
| --- | --- | --- |
| CUSTOMER | ✅ | ❌ |
| SUPPLIER | ❌ | ✅ |
| BOTH | ✅ | ✅ |
| OTHER | ❌ | ❌ |

Role order in the picker: CUSTOMER, SUPPLIER, BOTH, OTHER (display order in `FarmOrdering.partyRoles`).

## Service operations (FarmSliceService)

- `addTrade(farmId, draft): Trade` — validates against the farm (party exists, role compatible, money range), assigns id, appends, persists.
- `updateTrade(farmId, tradeId, draft): Trade` — replaces fields in place; id unchanged; same validation.
- `deleteTrade(farmId, tradeId)` — removes; missing id rejected.
- `trade(farmId, tradeId): Trade?` — direct lookup (list/editor boundary).
- `trades(farmId): List<Trade>` — newest first (occurredAt desc, then insertion order), mirroring `transactionsNewestFirst`.

Party guards added on top of M5-01:
- `deleteParty` — rejected while any trade references the party: "Party cannot be deleted while sales or purchases reference it".
- `updateParty` — the role may change only to a role compatible **with all** trade types that reference the party (e.g. a CUSTOMER-party with an existing SALE may be widened to BOTH but not narrowed to SUPPLIER, and a damage broadening is allowed).
- `FriendPartyRole.BOTH` exists so a compatible-role change is always reachable without deleting trades.

Validation (`FarmStateValidator.validateTrade` + `validateFarm`): total positive; paid within range; partial/unpaid requires party; party exists; party role compatible; trade ids unique per farm; and `validateFarm` runs every trade through `validateTrade` so a loaded farm can never hold an inconsistent trade.

## Persistence evolution (schema v5)

`FarmState` gains `trades: MutableList<Trade> = mutableListOf()` and `CURRENT_FARM_SCHEMA_VERSION` becomes **5**.

`FarmPersistenceCodec` encoding (schema 5) appends a trades field after parties:

```
5 \u001F <id> \u001F <name> \u001F <entries> \u001F <currency> \u001F <transactions> \u001F <parties> \u001F <trades>
```

Each trade encodes as `id \u001D type \u001D partyId (blank for cash) \u001D totalMinor \u001D paidMinor \u001D description \u001D occurredAt` (transaction field separator reused; an absent party is encoded as an empty field and reconstructed as `null`).

Decode rules:
- **Schema 5** — reads all 8 fields; trades decoded from field 7; a trade with `partyId` blank yields `null`.
- **Schema 4 and below** — decode exactly as today, then set `trades = []`. Upgrade is implicit: the next `saveFarm` rewrites at v5.
- **Backup envelope unchanged.** `FarmBackupCodec` (envelope schema 1) wraps `FarmPersistenceCodec`, so trades serialize through existing backups automatically; old backups with no trades restore an empty list.

`FarmStateValidator.validateFarm` additionally validates each trade and requires unique trade ids.

## Trade list/editor boundary

The Hisab-Kitab destination shows, in order: **Recent sales and purchases** (newest-first, empty state), then **Parties**. The trade list coexists with the party list from M5-01; opening the trade editor swaps the Hisab-Kitab content between list and editor the same way the party form does.

- **Trade editor** — type is implied by the launch action (New sale / New purchase) and shown in the title; fields: Party (spinner with "Cash — no party" first, then compatible-farm parties), Total amount, Payment (Paid / Partially paid / Unpaid), Amount paid (enabled only for Partially paid), description, date/time via the existing editor button pattern. Save / Cancel; Delete is available when editing.
- **List rows** — type prefix, party (or "Cash sale"/"Cash purchase"), money, and either "Paid" or "N amount due".
- **Amounts** — currency is a farm-level setting (`FarmState.currencyCode`); trades never own a currency.

### Boundary decisions (explicit exclusions)

- **Home summary/balance excludes trades.** Trades do not touch `FarmSummary.balanceMinor`; to-receive/to-pay aggregation is a *future* hisab/khata feature and is not computed in this slice (only the per-trade "amount due" is shown inline). This keeps the financial definition stable and additive.
- **No unpaid list, no partner ledger, no settlement/adjacency operations, no "settle up" on a partial payment.** Those are M5-03+/hisab slices; the model already supports them (paid/total is the settlement primitive) but the UI does not add screens for them.
- **Payment status is a tri-state at save time only for PARTIAL entry.** The `paidText` for PARTIAL is the amount actually paid; `PaymentStatus.PAID` and `UNPAID` carry a derived paid amount (full/zero), so the amount-paid field is only shown for PARTIAL.
- **Trade categories are not transactions categories.** Trades use farm parties and money only; linking a trade to a TransactionCategory or to actual cash transactions (a paid sale also appearing in the Income ledger) is deliberately **out of scope** to avoid double counting in the M1–M4 balance during this slice.

## Localization

New user-facing terms added to `docs/localization/NEPALI_TERMINOLOGY.md` before shipping: Sale (बिक्री), Purchase (खरिद), Total amount (कुल रकम), Payment (भुक्तानी), Paid (भुक्तानी भयो), Partially paid (आंशिक भुक्तानी), Unpaid (भुक्तानी बाँकी), Amount paid (भुक्तानी गरिएको रकम), Amount due (बाँकी रकम), To receive (प्राप्त गर्न बाँकी), To pay (तिर्न बाँकी), Cash sale/purchase (नगद बिक्री/खरिद), Customer and supplier (ग्राहक र आपूर्तिकर्ता दुवै), Recent sales and purchases (हालैका बिक्री र खरिदहरू). Every Nepali key has an English default (M4 glossary rule).

## Implementation slices

1. **Domain** — `TradeType`, `PaymentStatus`, `Trade`, `TradeDraft`, `compatibleWith`, derived `paymentStatus()`/`outstandingMinor()`, validator rules.
2. **Persistence** — `FarmState.trades`, schema v5 codec with v4-and-below migration, backup round-trip.
3. **Service** — trade CRUD + listing; party delete/role-change guards; `BOTH` role.
4. **UI** — trade list + editor in the Hisab-Kitab destination, en+ne strings, dirty-discard + back behavior.
5. **Validation** — JVM unit tests (service, codec migration, validator, derived values), instrumentation for the list/editor flow, lint Debug + Release.

## Validation goals (updated as evidence lands)

- `FarmSliceServiceTest` trade CRUD, validation (money range, party required for partial/unpaid, missing/incompatible party), party-delete/role-change guards, role compatibility matrix, v4→v5 migration + backup round-trip; `LocalizedResourceResolutionTest` trade-string coverage; full JVM `testDebugUnitTest` on JDK 21; lint Debug + Release.

**Evidence:** `FarmSliceServiceTest` (60 tests incl. trade CRUD, guard, migration, backup) PASS on JDK 21: schema-3/2 and schema-4 migration to v5, byte-stable envelope (payload now carries the empty v5 parties+trades tails), full trade round-trip through persistence and backup, party guards, and derived payment/outstanding values all green. `compileDebugAndroidTestKotlin` SUCCESS (`LocalizedResourceResolutionTest` extended with the trade strings and the new `party_role_both` key). `compileDebugKotlin` SUCCESS. Full JVM suite retains the single known pre-existing baseline failure `TimePresentationTest#displaysStoredInstantInDeviceZoneWithoutUtcLiteral` (JVM JDK/timezone data, unrelated to this slice, reproduced on the parent base before M5-02 changes). Timed Android tests on the API-26 emulator remain supplemental per M4-04 disposition (not a completion gate).