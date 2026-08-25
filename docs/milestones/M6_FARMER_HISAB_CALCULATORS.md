# Kisab M6 — Farmer Hisab Calculators — Design Record

M6 completes the previously reserved **Hisab** destination with a focused Party Hisab calculator. The calculator answers a bounded farmer question:

> For this party and this period, what business happened, and what was still receivable or payable at the period end?

The result is a pure projection over the existing Party → Trade → Settlement facts. It is never persisted and never becomes a second balance or accounting authority.

> **Status: COMPLETE** on `feature/m6-farmer-hisab-calculators`, based on merged M5-05 `main`.

## Scope

Implemented:

- `PartyHisabActivity` — period sales, purchases, payments received, and payments made.
- `PartyHisabPosition` — gross to-receive, gross to-pay, and informational net at the cutoff.
- `PartyHisabResult` and `FarmState.partyHisab` — the pure calculator projection.
- `FarmSliceService.partyHisab` — a read-only service seam.
- Party and period selectors on the Hisab destination.
- Empty states for no farm, no parties, no period activity, and no outstanding position.
- English and Nepali resources and provisional terminology.
- Saved Party/period selection across Activity recreation.

Not implemented:

- No persisted calculator rows or totals.
- No automatic settlements, allocation across trades, or netting one trade against another.
- No profit, tax, interest, invoice, or general-purpose arithmetic calculator.
- No schema, migration, backup-envelope, dependency, or Home cash-flow change.

## Authoritative inputs

The projection reads:

- the selected `Party`;
- `Trade` rows whose `partyId` matches that Party;
- `Settlement` rows anchored to those Trades;
- the existing `FinancialPeriodPreset` and `FarmFinancialPeriods.periodFor` boundary rules;
- `FarmState.currencyCode` for presentation only.

Settlement direction comes from the anchored `Trade.type`: SALE settlement → payment received; PURCHASE settlement → payment made. Party role is displayed as context but is not used as an amount authority.

## Period semantics

Every activity calculation uses the established inclusive-start/exclusive-end interval:

```text
startInclusive <= occurredAt < endExclusive
```

The available presets remain This month, Last 30 days, and All time. Their clock and time-zone behavior is shared with M5-05 rather than reimplemented.

The position is independent of the period start. It includes the selected Party's Trades strictly before `endExclusive`, reduced only by their Settlements strictly before the same cutoff. A Settlement exactly at the cutoff is excluded.

## Formulas

All amounts use checked minor-unit `Long` arithmetic.

### Period activity

```text
sales             = sum(selected Party SALE totals in period)
purchases         = sum(selected Party PURCHASE totals in period)
paymentsReceived  = sum(selected Party SALE settlements in period)
paymentsMade      = sum(selected Party PURCHASE settlements in period)
```

### Position at period end

For each selected-Party Trade before the cutoff:

```text
settled     = sum(Trade settlements before cutoff)
outstanding = Trade total - settled

SALE outstanding     -> toReceive
PURCHASE outstanding -> toPay
net                   = toReceive - toPay
```

Only positive outstanding amounts contribute. To-receive and to-pay remain gross. Net is informational and never settles, reallocates, or changes an underlying Trade.

## Integrity and arithmetic

- Every Settlement must reference a farm Trade; an orphan fails clearly instead of disappearing from Hisab.
- `Math.addExact` and `Math.subtractExact` prevent silent overflow.
- Missing Party ids are rejected.
- No-party cash Trades are not attributed to a synthetic Party.
- Re-running the calculator with the same facts, clock, and zone returns the same result.

## Hisab UI

The former Hisab placeholder now contains:

1. Party selector, sorted by localized name and stable id.
2. Party role context.
3. Period selector using the shared period ordering and localized labels.
4. Activity in this period: Sales, Purchases, Payments received, Payments made.
5. Position at period end: last-included "As of" timestamp, To receive, To pay, Net position.

The first business-capable Party (CUSTOMER, SUPPLIER, or BOTH) is selected when no valid restored selection exists. OTHER parties are excluded because the domain does not permit them to own Trades, so they could never produce a meaningful reconciliation. If the selected Party is later unavailable, the UI safely falls back to the first current eligible Party. Party and period ids are stored in `onSaveInstanceState`; unknown or absent preset values fall back to This month.

All money uses the farm-owned currency and existing formatter. Empty sections show guidance rather than a misleading wall of zeros.

## Tests

`PartyHisabCalculatorTest` covers:

- empty Party Hisab;
- missing Party rejection;
- Party and period isolation;
- exact inclusive-start/exclusive-end boundaries;
- pre-period obligations and before/at-cutoff Settlements;
- BOTH-role gross receive/pay sides and informational net;
- fully settled Trades;
- orphan rejection;
- checked Trade, Settlement, and position overflow;
- deterministic results;
- schema-v6 persistence and backup decode → project → re-encode stability.

The full JVM, build, lint, Android-test compilation, localization parity, schema migration, and backup regression suites remain completion gates.

## Manual/device validation

Manual and device/emulator UI validation is intentionally deferred in this pass, consistent with the user's instruction. It should still be performed before a production release, especially Party/period switching, recreation restore, long Nepali text, and small-screen scrolling.

> **Update (RC-01, 2026-08-14):** Party/period switching and recreation restore were later exercised on the API-36 physical device by `FarmOverviewAndHisabDeviceTest`, with evidence recorded in `docs/release/V0.2.0_RELEASE_CHECKLIST.md`. Long Nepali text and small-screen Party Hisab scrolling remain outstanding manual/device checks.

## Post-M6 boundary

M6 completes the currently defined roadmap through Farmer Hisab calculators. No M7 scope is invented here. Any further milestone requires an explicit product decision.

> **Update:** M6.3 and M6.4 were later authorized as bounded calculator/toolbox milestones. M7 remains unprioritized unless a new product decision records it.
