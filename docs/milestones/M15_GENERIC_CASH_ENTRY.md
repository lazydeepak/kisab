# Kisab M15 — Generic Cash Income and Expense Entry

## Status

**M15 FINAL DISPOSITION: PASS**

Implemented on `main` on top of the v0.2.2 release line. M15 restores a first-class path for recording ordinary cash income and expenses that are not tied to a Party, Trade, Product, Supply, or Settlement — transport, electricity, day labour, miscellaneous farm income, service income — through the current Record experience, with no schema change and no new accounting authority.

Validation evidence: `docs/validation/M15_GENERIC_CASH_ENTRY_VALIDATION.md` (495 JVM tests / 0 failures including 9 new generic-cash tests, lint 0 errors, connected suite **140 / 0** on `ZA22374XPC`, live device walkthrough of both flows).

## 1. Ground truth

The transaction domain always supported plain income/expense records (`createTransaction` + `FarmTransactionDraft` with type/category/activity), and the inline editor's create mode (`openEditorForNew`) was fully functional — but its only triggers sat inside the inert `gone` compatibility block removed by M12's audit and finally deleted here. The M7+ Record sheet exposed six verbs, every one of which demands Party/Product/Supply context; rehearsal on production v0.2.2 confirmed no free-form cash route exists.

## 2. UX shape

Two new verbs in the Record sheet's existing **Other money** section:

- **Other income** → opens the standard transaction editor in create mode, Income type, category preset to *Other income*, titled **"Record other income"**
- **Other expense** → same for **"Record other expense"**, category preset to *Other expense*

Editor fields are exactly the ordinary ones: type radios, category spinner (switchable after entry), amount, description (existing required-note rule), date/time per existing rules, and the M10 Activity selector. Nothing else is requested: no customer, supplier, product, supply, quantity, or trade details. Dirty-editor discard protection applies, identical to every other editor entry point.

Wording reuses the established category vocabulary ("Other income" / अन्य आम्दानी, "Other expense" / अन्य खर्च) so sheet verbs, editor titles, categories, and the activity breakdown all speak the same language, and stay clearly distinct from RECEIVED MONEY/PAID MONEY (settlements) and SELL/BOUGHT (trades). Final wording remains subject to ongoing M14 farmer validation.

## 3. Accounting semantics (unchanged authorities)

Generic entries create ordinary `FarmTransaction` records: they count once in `FarmTotals` income/expenses/balance, never create Trades/Settlements/receivables/payables, never require a Party, keep stable IDs/timestamps/currency rules, and participate in activity attribution exactly like other transactions (explicit activity buckets; null → General). Schema stays at **14**.

One domain correction became necessary during validation: `FarmerOverview.received()` counted only SALE-trade settlements while `expenses()` counted all expense transactions — asymmetric by history because nothing in the UI had ever produced INCOME transactions. With M15 making them first-class, `received()` now includes generic cash income (settlements + INCOME transactions); `sales()` remains trade-gross. Without this, a farmer recording cash income would see Today's Received tile stay at zero — verified live on device before the fix.

## 4. Legacy cleanup

The entire inert compatibility block in `activity_shell.xml` (16 views: farmNameText, balance/income/expense texts, firstActionPrompt, other entries toggle, legacy accounting actions, quick-sale/received-money/supply buttons, farmerOverviewTodayText) is **removed**, together with all dead Kotlin wiring: field declarations, findViewById calls, listeners, `otherEntriesExpanded` state/save/restore, `toggleOtherEntries`, `updateOtherEntriesExpansion`, and the `renderFarmerOverview` dead write. Two M12-era instrumented tests that asserted text on those gone views were re-targeted at live surfaces (Farm-tools summary; persisted-state assertions).

## 5. Tests

JVM — `GenericCashEntryTest` (8→9 tests): create expense/income without any Party; no Trade/Settlement created; category-type constraint enforced; totals counted exactly once; edit + delete semantics; activity attribution default-General vs explicit bucket with breakdown reconciliation against `FarmTotals`; persistence round-trip; backup envelope round-trip byte-stable; daily overview counts generic income as money received.

Instrumented — `GenericCashEntryWorkflowTest` (2): OTHER EXPENSE end-to-end from the sheet (title, save, ledger row, no trades/settlements/parties) and OTHER INCOME equivalent.

## 6. Gates

| Gate | Result |
|---|---|
| `verifyLocal` | PASS — 494→495 unit tests, 0 failures; lint 0 errors |
| Connected suite (`ZA22374XPC`) | **140 / 0 failures** |
| Device walkthrough | PASS — see below |

Walkthrough (live device, debug build carrying the M15 change): farm created with Poultry activity; Record sheet renders nine verbs including the two new ones; OTHER EXPENSE opened editor titled "Record other expense", saved $12.50 "Van fare" with no supplier — Today Expenses tile `$12.50`, recent row `Expense | Other expense`; OTHER INCOME editor titled "Record other income" with Activity selector set to Poultry and saved. Khata confirmed empty of parties/trades throughout. Nepali labels verified in resources (अन्य आम्दानी / अन्य खर्च) with on-screen EN checks; NE screen check repeats during M14 sessions.

*Device note:* the phone dropped Kisab mid-walkthrough twice (active personal use; package removed externally). The remaining Received-tile verification was completed logically by the new overview unit test plus the green connected suite; a 30-second re-check folds into the next M14 session setup.
