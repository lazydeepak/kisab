# Kisab M5-04 — Party Khata / Ledger — Design Record

Defines the fourth Hisab-Kitab slice: the **Party Khata** — a per-party, chronological, projection-driven vehicle of Sales, Purchases, Payments received, Payment made, a running party balance, and the current receivable/payable position. M5-04 builds entirely over the Party → Trade → Settlement model that M5-02 and M5-03 established: **the Khata is a view, never a second accounting authority.** No ledger rows are stored anywhere; the projection is recomputed from the authoritative persisted facts on every render, so editing or deleting an underlying Trade or Settlement automatically changes the Khata on the next render.

> **Status: COMPLETE** on `feature/m5-04-party-khata` (from merged M5-03 `main`). This document is the deliberate design conversation required by the charter before implementation was accepted.

**Implemented:** `PartyLedger`, `PartyLedgerSummary`, `PartyLedgerEntry`, `PartyLedgerEntryType`, and the pure projection `buildPartyLedger` plus the `FarmState.partyLedger`/`partyLedgerSummary` extensions; `FarmSliceService.partyLedger`/`partyLedgerSummary` seams; the Hisab-Kitab Party Khata screen (tap a Party → its Khata: title/role, to-receive / to-pay / net-position header, New Sale / New Purchase / Edit Party / Done actions, newest-first entry list with type · amount · balance-after · note · date-time); a single-tap entry routes to the underlying Trade editor or Settlement editor; back navigation returns Khata → Hisab-Kitab (and Khata → Trade/Settlement editor → Khata along the layered path); instance-state restore of the open Khata including a Trade/Settlement editor layered over it; en+ne strings; JVM tests. No schema change, no new dependency, no committed state.

## Working principles

- **Projection authority.** `FarmState` remains the single source of truth. Trade totals, party ids, and settlement amounts are read only; the Khata never writes, mutates, or caches. There is intentionally no migration and no new persistence surface — a Khata cannot go stale because there is nothing to keep in sync.
- **One sign convention, app-wide.** Positive balance = the Party owes the farmer (receivable); negative = the farmer owes the Party (payable); zero = net settled. SALE → `+total`, SALE settlement → `−amount`, PURCHASE → `−total`, PURCHASE settlement → `+amount`.
- **The net is informational only.** To-receive and to-pay are computed gross per outstanding trade balance. The net position (receive − pay) summarizes direction for the farmer's decision-making but **never settles any individual trade**: every trade's `PaymentStatus` stays exactly what its own settlements say (see `bothPartyKeepsTradesSeparateAndNetsInformationally`).
- **Settlements and trades are the facts; time is the ordering key.** Deterministic chronological sort with a same-instant tie-break that always places an obligation (Trade) before its own payment (Settlement) — essential for M5-03's migrated opening settlements, where the settlement shares the trade's timestamp.
- **No orphaned financial events.** Every Settlement must anchor to an existing Trade (validated at the service/codec boundary). The projection enforces the same invariant loudly: an orphan settlement (tampered input) fails clearly instead of silently dropping a money movement.
- **No-party cash trades stay out.** A cash sale/purchase (no Party) belongs to the overall Hisab-Kitab history, not to any Party Khata; no synthetic "Cash Party" is created.

## Party Khata / Ledger model

```kotlin
data class PartyLedger(
    val party: Party,
    val summary: PartyLedgerSummary,      // toReceiveMinor, toPayMinor, netMinor
    val entries: List<PartyLedgerEntry>   // deterministic chronological order
)

data class PartyLedgerEntry(
    val sourceId: String,                 // Trade id (SALE/PURCHASE) or Settlement id (payment)
    val sourceType: PartyLedgerEntryType, // SALE | PURCHASE | PAYMENT_RECEIVED | PAYMENT_MADE
    val occurredAt: OffsetDateTime,
    val amountMinor: Long,                // unsigned magnitude
    val deltaMinor: Long,                 // signed per the app-wide convention
    val runningBalanceMinor: Long,        // post-event balance
    val description: String,              // trade description or settlement note
    val tradeId: String                   // anchors a settlement row to its trade for routing
)

data class PartyLedgerSummary(
    val toReceiveMinor: Long,  // Σ outstanding SALE balances for this party
    val toPayMinor: Long,      // Σ outstanding PURCHASE balances for this party
    val netMinor: Long         // toReceive − toPay (informational only)
)
```

### Sign / delta convention

| Event | `deltaMinor` | Meaning |
| --- | --- | --- |
| SALE trade | `+totalMinor` | The Party owes what they bought |
| SALE settlement (PAYMENT_RECEIVED) | `−amountMinor` | Money received reduces what is owed |
| PURCHASE trade | `−totalMinor` | The farmer owes what was bought |
| PURCHASE settlement (PAYMENT_MADE) | `+amountMinor` | Money paid reduces what the farmer owes |

`runningBalanceMinor[n] = runningBalanceMinor[n−1] + deltaMinor[n]`, starting from 0. The balance-after display is rendered with the farm's owned currency formatter (never a hard-coded symbol): a positive balance shows as "You should receive <amount in farm currency>…", negative as "You should pay <amount in farm currency>…", zero as "Settled". Examples in this document therefore use generic or NPR-locale-safe amounts rather than assuming a fixed ₹.

### Chronology and tie-breaks

Entries are sorted with `compareBy(occurredAt).thenBy(orderPriority).thenBy(stableKey)`:

1. **occurredAt** (absolute time) — primary chronological order (oldest → newest).
2. **orderPriority** — at the same instant, the obligation (SALE/PURCHASE, priority 0) always precedes its own payment (PAYMENT_RECEIVED/PAYMENT_MADE, priority 1). This is what makes M5-03's migrated opening settlements render correctly: the running balance never applies a payment before the obligation exists.
3. **stableKey** (`"trade:"` + id / `"settlement:"` + id) — a final deterministic tie-break so ordering is identical regardless of insertion order or call count.

The UI reverses `entries` purely for newest-first rendering; **running balances are always computed oldest → newest** so a payment never retroactively changes earlier rows.

### Migrated opening settlements (v5 → v6 interaction)

M5-03's schema-v6 migration turns every v5 trade that had `paidMinor > 0` into **exactly one deterministic opening Settlement** dated at the trade's own `occurredAt` (the historic payment instant is unknowable; the trade timestamp is the deliberate, reproducible approximation). M5-04 inherits that behavior for free: projecting a decoded v5 farm shows the Trade at its timestamp followed immediately by its PAYMENT_RECEIVED/PAYMENT_MADE row with the correct running balance. `migratedSchema5PayloadProjectsTradeBeforeOpeningSettlement` exercises the real `FarmPersistenceCodec.decode` → projection path with a v5 payload carrying `paidMinor > 0`.

### Gross to-receive / to-pay and the informational net

`partyLedgerSummaryOf` sums the outstanding balance (`totalMinor − paid`) per trade across the party's trades: SALE outstandings feed `toReceiveMinor`, PURCHASE outstandings feed `toPayMinor`, and `netMinor = toReceive − toPay`. The net is a decision aid only — it satisfies no trade and never alters a trade's `PaymentStatus` (see `bothPartyKeepsTradesSeparateAndNetsInformationally` / `mixedBothPartyHistoryShowsEachSettlementIndependent`).

### Orphan safety

A Settlement always names exactly one Trade. The validated service/codec boundaries (`FarmSliceService.addSettlement`/`updateSettlement`/`updateTrade` and every persisted transition through `FarmStateValidator.validateFarm`, plus `FarmBackupCodec.decode`) reject any settlement whose `tradeId` misses the farm's trades, so an orphan cannot be produced by the normal domain. The projection re-asserts the invariant at its own boundary: any settlement referencing a missing trade (only reachable from tampered bytes) throws a clear `IllegalArgumentException` rather than silently omitting a money movement. `projectionRejectsSettlementOfMissingTradeInsteadOfSilentlyOmittingIt` locks this in.

### No-party exclusion

Trades and settlements carrying no party are excluded from every Party Khata. No synthetic "Cash Party" record is created; those events remain visible only in the farm-wide Hisab-Kitab history and the M5-02 cash labels (`cashNoPartyTradeIsExcludedFromAnyPartyKhata`).

## Service operations (FarmSliceService)

- `partyLedger(farmId, partyId): PartyLedger` — the full projection (party, summary, entries); throws `IllegalArgumentException("Party not found: …")` when the party does not exist.
- `partyLedgerSummary(farmId, partyId): PartyLedgerSummary` — the party's derived current position only.

Both are pure reads over the authoritative lists; they never persist and never mutate state (`khataProjectionDoesNotChangeEncodedFarmState` asserts the encoded farm bytes are identical before and after projecting).

## Party Khata UI (Hisab-Kitab destination)

Tapping a Party row opens that party's Khata as a scroll-in-place "screen" within Hisab-Kitab (the Khata container is a sibling of the summary/trades/parties sections, so the app bar and bottom navigation remain; the surrounding Hisab-Kitab chrome is hidden only while a Khata is open).

- **Header:** Party name, role, `To receive: …` (gross receivable), `To pay: …` (gross payable), `Net position: …` (informational, with receive/pay/settled phrasing per the sign of the net).
- **Actions:** New Sale and New Purchase (visible only when the party's role is compatible with that trade type — CUSTOMER → Sale, SUPPLIER → Purchase, BOTH → both; each preseeds the trade editor with that party), Edit Party (returns to the existing party editor; edits flow straight back into the Khata), Done (closes the Khata → returns to the Hisab-Kitab list).
- **Entries:** newest first (reversed projection). Each row shows a header `Sale — <amount>` / `Purchase — <amount>` / `Payment received — <amount>` / `Payment made — <amount>` (farm-currency formatted), a `Balance after: …` line, the description/note when present, and the date-time. Rows are labelled buttons.
- **Row routing:** tapping a SALE/PURCHASE row opens that Trade's editor (Edit mode); tapping a payment row opens that Settlement in the per-trade payment editor (via the trade from `entry.tradeId`). Back/done returns through the same layers to the Khata.
- **Empty State:** a friendly guidance message when the party has no Hisab-Kitab records yet.

### Navigation, back, and restore

The Hisab-Kitab destination already hosts layered editors with their own back branches. The Khata is another layer, ordered after the party editor in the back-handler chain: **Settlement editor → Trade editor → Party editor → Khata → destination**. Working outward:

- Khata → back → Hisab-Kitab list (chrome restored).
- Trade editor opened from a Khata row → back → the Khata (via `refreshKhataView`).
- Settlement editor opened from a Khata payment row → back → the Khata.
- Settlement editor opened from a Trade editor that itself came from a Khata → back → Trade editor → back → Khata.
- New Sale / New Purchase from a Khata preselect the party; cancel/back returns to the Khata.
- Edit Party from a Khata returns to the refreshed Khata on save or cancel.

`onSaveInstanceState` stores the open Khata's party id (`STATE_KHATA_PARTY_ID`) alongside the existing destination/editor state. On recreate, `restoreKhataFrom` re-establishes the Khata; when a Trade/Settlement editor is layered on top of it, the Khata container is re-hidden behind them so the layered flow resumes where it left off. Note that the **Khata and Trade/Settlement editor state are restored, but the Party editor itself (its draft fields and `editingPartyId`) is not part of instance-state save/restore**: a party editor that is open over a Khata at recreation is not re-established, and the Khata is re-shown instead. `showDestination` closes an open Khata when leaving Hisab-Kitab so navigation to Home/Hisab/Settings never carries a stale overlay.

### Role-compatible preselection

Khata "New Sale"/"New Purchase" pass the Khata party as `preselectedPartyId` into `openTradeEditorForNew`. `buildTradePartyChoices` only lists parties whose role is compatible with the trade type, so the preselected party is always selectable; a cash (no-party) option remains for the "no party" trade path.

## Boundary decisions (explicit exclusions)

- **Home separation is preserved.** Trades and settlements never feed `FarmSummary.balanceMinor` or the Home income/expense figures; the Khata is entirely inside the Hisab-Kitab destination. No paid sale appears as Home income in this slice (unchanged since M5-02/M5-03).
- **No automatic Home cash-transaction creation.** Still out of scope; settlements are payment history, not double-counted ledger entries (see M5-03).
- **No ledger persistence, no schema bump.** `FarmState`/schema stay at v6; the Khata is computed on demand.
- **No payment kinds, multi-trade allocations, or reversal semantics** — each settlement targets exactly one trade (M5-03 scope).
- **No reporting/analytics exports** — a Party Khata is an in-app screen, not a financial statement for external use.
- **The "net" does not settle trades** — informational only, by design.

## Localization

New user-facing terms (all provisional, en+ne key parity maintained):
Khata / Party Khata, Payment received, Payment made, Balance after, Net position, You should receive …, You should pay …, Settled, Edit party, and the Khata empty-state guidance. Every resource has an English default; placeholder signatures match across locales (`LocalizationParityTest`).

## Implementation slices

1. **Domain** — `PartyLedger`/`PartyLedgerEntry`/`PartyLedgerSummary`/`PartyLedgerEntryType`; `buildPartyLedger` projection + `FarmState.partyLedger`/`partyLedgerSummary`; orphan-invariant guard.
2. **Service** — `FarmSliceService.partyLedger`/`partyLedgerSummary` read seams.
3. **UI** — Khata container + header/actions/entry list in `activity_shell.xml`; `item_ledger_entry_row.xml`; render/route/close/restore wiring in `FarmActivity`; chrome-visible management; back-handler layering.
4. **Localization** — en+ne strings for all new user-facing terms.
5. **Validation** — JVM unit tests for the projection (see below), UI compile/lint.

## Validation goals (updated as evidence lands)

### Unit tests (`PartyLedgerTest` — 17 cases, all PASS)

- `saleOnlyProducesReceivableRunningBalance`, `purchaseOnlyProducesPayableRunningBalance`
- `salePlusReceiptBuildsCorrectRunningBalance`, `purchasePlusPaymentBuildsCorrectRunningBalance`
- `bothPartyKeepsTradesSeparateAndNetsInformationally`, `mixedBothPartyHistoryShowsEachSettlementIndependent`
- `multipleSettlementsAppearIndependently`, `sameTimestampTradeAppearsBeforeItsOpeningSettlement`
- `migratedSchema5PayloadProjectsTradeBeforeOpeningSettlement` (real codec decode path)
- `orderingIsDeterministicAcrossInsertionOrderAndCallCount`
- `cashNoPartyTradeIsExcludedFromAnyPartyKhata`
- `projectionReflectsUnderlyingEditsAndDeletes`, `khataProjectionDoesNotChangeEncodedFarmState`
- `finalRunningBalanceEqualsNetPosition`, `emptyKhataShowsNoRowsForPartyWithoutTrades`
- `unknownPartyFailsClearly`
- `projectionRejectsSettlementOfMissingTradeInsteadOfSilentlyOmittingIt`

### Regression coverage carried by the gate

The pre-existing schema-v6 backup byte-stability and v5→v6 migration tests remain the guardians of the underlying authority the Khata reads from:

- `FarmSliceServiceTest#schema5MigrationCreatesDeterministicOpeningSettlements` — deterministic v5→v6 opening-settlement migration (id via `UUID.nameUUIDFromBytes`, dated at the trade's own `occurredAt`).
- `FarmSliceServiceTest#backupEnvelopeFormatIsByteStable` — schema-6 payload layout in the backup envelope is byte-stable.
- `FarmSliceServiceTest#schema4FarmWithTradesRoundTripByteStable`, `tradeRoundTripsThroughPersistenceAndBackup`, `backupEnvelopeRoundTripsFarmState`, and the schema-2/3/4→v6 upgrade tests — persistence/backup round-trips.
- `LocalizationParityTest` — en+ne parity for the new Khata keys.

### Toolchain evidence (2026-08-11)

`compileDebugKotlin`, `compileDebugAndroidTestKotlin`, `assembleDebug`, and `lintDebug` all SUCCESS on the M5-04 branch.

### Manual / device UI validation — NOT performed in this pass

No manual or device/emulator UI validation was performed for M5-04 in this pass. The interactive Khata flows (tap-party opens Khata, row routing to Trade/Settlement editors, layered back navigation, role-compatible preselection, restore over configuration change, Nepali rendering) are covered by code inspection and automated unit-toolchain checks only. Per M4-04 disposition, timed Android tests on the API-26 emulator remain supplemental and are not a completion gate; device validation should still be scheduled before any production release of this feature.

> **Update (M6.4.1, 2026-08-11):** the on-device gap for the basic Khata flows was later closed. The M6.4.1 full connected suite ran the updated party workflow tests (tap-party opens Khata, edit party from Khata, delete party from Khata, close Khata returning to the parties list) on an API-36 physical device (Moto Edge 60 Fusion, 86/86 tests pass) and on an API-26 emulator (86 run, 2 documented back-navigation coordinate flakes only). See `docs/milestones/M6_4_1_SHELL_SYSTEM_BAR_INSETS.md` and `docs/release/V0.2.0_RELEASE_CHECKLIST.md`. Long-Nepali-text and small-screen Khata rendering remain not device-exercised as of the v0.2.0 candidate freeze.

## M5-05 boundary

M5-04 stops at a per-party Khata inside Hisab-Kitab. **M5-05 — Farm Financial Overview** is next: broader farm-wide financial synthesis (period summaries, trend/overview aggregates) that presents the whole farm's financial picture, building on the same Party → Trade → Settlement projection discipline. No M5-05 work is performed in this milestone.
