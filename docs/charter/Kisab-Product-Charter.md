# Kisab Product Charter

## Purpose
Kisab is the first product-facing application in the Susankhya ecosystem to make farm operations manageable in low-connectivity environments. Its role is to help a farm operator record simple on-farm facts, transactions, and summaries without introducing product logic into the shared foundation.

## Non-goals for v1
- Build a full accounting engine.
- Support broad reporting, analytics, or multi-tenant administration.
- Introduce reusable foundation abstractions for farm operations.
- Replace the foundation with product-specific infrastructure.

## Primary user
A smallholder or farm operator who needs to record a farm, one or more livestock/crop entries, and a simple transaction history while offline or with intermittent connectivity.
## First journey

1. Create a farm.
2. Add an inventory or livestock/crop entry.
3. Record a transaction.
4. View the farm summary.

## Roadmap

- **M4-05** — currency/settings ownership correction, RC validation, `v0.2.0`.
- **M5-00** — Kisab Application Shell: top app bar, bottom navigation, Settings destination (Language, Currency), navigation/back-stack rules, editor/save/discard behavior across navigation, screen wrapper conventions, responsive behavior. Deliberate UI/UX design conversation precedes implementation. The currency domain contract locked in M4-05 moves here from its temporary Farm Tools location; the domain does not change again for that capability. **Status: COMPLETE.**
- **M5-01** — Hisab-Kitab party domain: the **Party** model (role, contact, notes) and its persistence evolution, with the party list/editor on the Hisab-Kitab destination. **Status: COMPLETE.**
- **M5-02** — Sale/Purchase (Trade) records with payment status: trades and their derived Paid/Due/Status, trade editor/list, farm-schema v5, farm-wide to-receive/to-pay totals. **Status: COMPLETE.**
- **M5-03** — Settlements (receivable/payable): Trade = obligation, Settlement = payment/receipt event, Paid/Due/Status = projections from settlements; per-trade payment ledger UI; schema v6 with deterministic v5→v6 opening-settlement migration. **Status: COMPLETE** (see `docs/milestones/M5_03_SETTLEMENTS_RECEIVABLE_PAYABLE.md`).
- **M5-04** — Party Khata / Ledger: per-party chronological projection of sales, purchases, payments received/made, running balance, and current receivable/payable position — an additional projection over the Party → Trade → Settlement model, not a second accounting authority. **Status: COMPLETE** (see `docs/milestones/M5_04_PARTY_KHATA_LEDGER.md`).
- **M5-05** — Farm Financial Overview: farm-wide financial synthesis over the same Party → Trade → Settlement projection discipline — period summaries, trend/overview aggregates, and a whole-farm financial picture. **Status: NEXT.**
- **M6** — Farmer Hisab calculators.

## Product boundary
Kisab owns farm concepts, user journeys, offline persistence strategy, and product-specific presentation. It reuses the shared foundation only for technical concerns such as secure session persistence.

## Foundation reuse
Kisab will use the published foundation artifact for app-session storage and platform integration. It will not request the foundation to own farm, crop, livestock, transaction, or accounting semantics.
