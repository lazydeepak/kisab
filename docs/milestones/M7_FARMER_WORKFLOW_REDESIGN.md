# Kisab M7 — Farmer Workflow Redesign: Khata First

## Status

The design baseline is implemented on `feature/m7-quick-sale-khata`. M7.1 usability refinement is implemented on `feature/m7-1-quick-sale-usability`. This record remains the authority for the narrow Quick Sale + Received Money slice. Inventory, production, livestock, recurring sales, and rate-suggestion persistence remain out of scope.

## Product center

Kisab is centered on the farmer's daily question:

> मैले कसैलाई केही दिएँ। उसले सबै पैसा तिरेको छैन। अहिले उसले मलाई कति तिर्न बाँकी छ?

The buffalo farmer is not a sample persona. His repeated milk sale, credit, partial-payment, expense, stock, and production questions define the farmer-facing product boundary.

The internal model may remain rigorous: parties, trades, settlements, projections, persistence, backups, and financial overviews are valuable foundations. The interface must translate those concepts into a small vocabulary of recognizable objects and actions.

## Design principles

### 1. Questions before modules

The home surface should answer:

- आज कति आयो?
- आज कति गयो?
- कसले तिर्न बाँकी छ?
- मैले कसलाई तिर्न बाँकी छ?
- मसँग के कति बाँकी छ?
- आज कति उत्पादन भयो?

The farmer should not need to navigate through Transactions, Trades, Settlements, Parties, or Reports to answer those questions.

### 2. Farmer language at the boundary

Preferred action vocabulary:

| Internal concept | Farmer-facing action | Nepali direction |
| --- | --- | --- |
| Sale/trade | Sell / `बेचेँ` | बेचेँ |
| Settlement on a sale | Receive money / `पैसा पाएँ` | पैसा पाएँ |
| Expense transaction | Expense / `खर्च भयो` | खर्च भयो |
| Party khata | Khata / `खाता` | खाता |
| Receivable | Money to receive | लिन बाँकी |
| Payable | Money to pay | तिर्न बाँकी |
| Stock consumption | Used / `प्रयोग गरेँ` | प्रयोग गरेँ |

Accounting names remain available in code, tests, and advanced detail surfaces where they improve precision. They should not be the primary labels of the farmer workflow.

### 3. Recognition before typing

The first interaction should use large, familiar actions and choices. Names, products, units, and remembered prices should be reusable. Free-text notes and advanced fields belong behind `थप विवरण` / More details.

### 4. Trust through visible arithmetic

Every balance must be inspectable. A farmer should be able to open Ram's Khata and see:

```text
पुरानो बाँकी       रु 4,000
दूध 5 L            रु   450
पैसा पाएको         रु 1,000
अहिले बाँकी        रु 3,450
```

The projection remains authoritative; the UI must make its inputs understandable rather than presenting an unexplained total.

### 5. One fact, one entry

The same event must not be entered once in Home and again in Khata. The next slice must define how a product sale becomes both a sale fact and a party balance without creating duplicate cash or settlement records.

## Current foundation and current gap

The existing model already provides:

- `Party` with customer/supplier semantics.
- `Trade` for sale and purchase obligations.
- `Settlement` for partial and completed payments.
- `PartyLedger` as a derived Khata projection.
- Farm-wide financial overview projections.
- Backup and schema migration support.

The current gap is the farmer-facing composition. The UI exposes the model's structure: party selection, trade type, payment status, settlement editor, and financial sections. That is correct architecture but the wrong first mental model for a semi-literate farmer.

## Model audit: generic sale primitive

The current Trade/Settlement/PartyLedger model already supports most of the financial behavior:

| Requirement | Current support | Audit result |
| --- | --- | --- |
| Customer or cash counterparty | `Trade.partyId` plus compatible `PartyRole` | Reuse |
| Sale versus purchase direction | `Trade.type` | Reuse; the first slice creates SALE facts |
| Total money owed | `Trade.totalMinor` | Reuse as the derived quantity × rate total |
| Paid / unpaid / partial at creation | `addTradeWithInitialSettlement` | Reuse |
| Later partial payments | `Settlement` records | Reuse as the source of truth |
| Continuous customer balance | `PartyLedger` projection | Reuse |
| Product | None | Add a small product/detail boundary |
| Quantity and unit | None | Add to the product/detail boundary |
| Rate | None; only total is stored | Add to the product/detail boundary |
| Party-level payment without invoice choice | None; Settlement currently requires one trade | Add an allocation service operation |

The conclusion is deliberate: do not replace `Trade` with a farmer-only sale object, and do not put stock quantities or production readings on `Trade`. `Trade` remains the financial obligation. A product-sale detail explains how that obligation was calculated.

### Recommended generic data boundary

For the first product sale slice, introduce these concepts conceptually:

```kotlin
data class FarmProduct(
	val id: String,
	val name: String,
	val unit: ProductUnit
)

data class ProductSaleDetail(
	val tradeId: String,
	val productId: String,
	val quantity: BigDecimal,
	val unit: ProductUnit,
	val rateMinor: Long
)
```

The exact Kotlin representation remains an implementation decision, but these invariants should be fixed now:

- `FarmProduct` is a farm-local catalog item, not an inventory balance.
- `ProductUnit` supports `L`, `kg`, `बोरा`, `packet`, `bottle`, `वटा`, `piece`, `bundle`, and a custom-label path.
- `quantity` is positive and normalized with enough precision for common farm quantities; it is not forced to an integer merely because milk examples use whole litres.
- `rateMinor` is the price for one unit in the farm currency and must be positive.
- `Trade.totalMinor` remains the authoritative money total and is calculated once from quantity × rate using explicit rounding rules.
- `ProductSaleDetail.tradeId` anchors the detail to exactly one SALE `Trade`; deletion must not leave an orphan.
- No stock-on-hand, purchase receipt, production reading, or automatic inventory mutation is created by this slice.

Milk is simply `दूध` with unit `L`. गोबर, घिउ, eggs, and vegetables use the same primitive with different products and units. Rate suggestions are derived from prior sale details and are suggestions only; choosing one must never silently overwrite the farmer's entered rate.

### Persistence and service boundary

The current persistence schema is v6. The smallest safe evolution is a new schema append for farm-local products and product-sale details. Existing v6 farms decode with empty product/detail lists; existing Trade, Settlement, and PartyLedger facts remain valid. Backup round-trips must preserve both the financial fact and its product explanation.

Do not encode product data into `Trade.description`: that would make product identity, quantity, unit, and rate unqueryable and make later stock or production work harder. Do not add stock fields to `FarmProduct` in this slice.

The farmer-facing service seam should conceptually be:

```text
addProductSale(
	farmId,
	partyId?,
	productId,
	quantity,
	rateMinor,
	initialPaymentMinor?,
	occurredAt
) -> Trade
```

It validates the product, calculates the exact `totalMinor`, creates the SALE `Trade`, creates the linked `ProductSaleDetail`, optionally creates the opening `Settlement`, validates the complete resulting farm state once, and persists atomically. The existing lower-level `addTradeWithInitialSettlement` remains available to advanced/internal flows.

## Ten-scenario audit

These scenarios are the discriminating check for the redesign. A future implementation slice is not ready until each scenario can be completed without accounting knowledge and without duplicate entry.

| # | Daily scenario | Current conceptual path | Main friction to remove | Target farmer path |
| --- | --- | --- | --- | --- |
| 1 | Sell milk for cash | Hisab-Kitab → sale/trade → party or cash → total → payment choice | Product and quantity are absent; cash sale is expressed as a trade total | `बेचेँ → दूध → मात्रा → पैसा पाएँ` |
| 2 | Sell milk on credit | Sale/trade → customer → total → unpaid | Farmer must know trade/payment terminology; litres and rate are not first-class | `बेचेँ → राम → दूध → 2 L → उधार` |
| 3 | Existing customer buys again | Reopen party and create another trade | Repeated customer/product/rate data is not reused | `बेचेँ → राम → दूध → 2 → ✓` |
| 4 | Customer partially pays old debt | Open trade → Payments → add settlement | Payment is anchored to a trade, while the farmer thinks in the customer's total Khata | `खाता → राम → पैसा पाएँ → रु 1,000` with allocation guidance |
| 5 | Buy feed for cash | Home expense transaction with generic category/description | Too many generic fields; feed stock is not affected | `खर्च भयो → दाना → रु 1,500` |
| 6 | Buy feed on credit | Purchase trade → supplier → payment status | Purchase obligation and stock receipt are separate mental steps | `किनेँ → दाना → 20 बोरा → Krishna Feed Store → उधार` |
| 7 | Use feed from stock | No dedicated stock movement flow | Current model cannot answer remaining feed quantity | `प्रयोग गरेँ → दाना → 1 बोरा` |
| 8 | Buy medicine | Generic expense transaction | Medicine identity, unit, and remaining quantity disappear into description | `किनेँ → औषधि → Calcium → 3 bottle → रु ...` |
| 9 | Record today's milk production | No farmer-centered production entry | Production is not represented as a daily fact | `आजको दूध → बिहान 38 L → साँझ 34 L` |
| 10 | Ask monthly earnings | Financial overview exists but separates cash/trade by design | The farmer needs a plain-language answer with transparent boundaries | `यो महिना → आयो / गयो / पाउन बाँकी / तिर्न बाँकी`, with explanation |

## Target home surface

The first screen should be organized around today's answers, not domain modules.

```text
आज

रु 8,450             आज बिक्री
रु 2,300             आज खर्च
रु 34,500            लिन बाँकी

[ बेचेँ ] [ खर्च भयो ]
[ पैसा पाएँ ] [ खाता ]

आजको काम
दूध: 72 L
दाना: 4 बोरा बाँकी
7 जनाबाट पैसा लिन बाँकी
```

This is a direction, not a commitment to these exact figures or layout. The key constraint is that the four primary actions must be visible without entering a software module.

## Khata as the primary object

The party list should feel like a set of familiar khata cards or rows:

```text
राम प्रसाद
लिन बाँकी: रु 3,450

सीता
लिन बाँकी: रु 0

Krishna Feed Store
तिर्न बाँकी: रु 22,000
```

Opening a party shows the current total first, then a chronological explanation. The first-class actions are:

- `बेचेँ` — add a sale for this party.
- `पैसा पाएँ` — record money received against this party's outstanding balance.
- `खाता मिल्यो` / settled — only where the projection proves no balance remains; this is not a new accounting mutation.
- Share Khata is future scope, but the row structure should leave room for a later text/image export.

The underlying per-trade settlement model remains useful, but the farmer should not have to choose which trade received a partial payment. The allocation rule is oldest outstanding SALE first, with trade timestamp ascending and trade id ascending as a stable tie-break.

## Fast entry contracts

### Sale

Minimum path:

```text
बेचेँ
→ customer or cash
→ product
→ quantity + unit
→ remembered/current rate
→ पैसा पाएँ or उधार
→ पुष्टि
```

For a first slice, `product`, `quantity`, `unit`, and `rate` are represented by the ProductSaleDetail boundary above and derive the existing `Trade.totalMinor`. Product catalog persistence is deliberately small and farm-local. The UI must preserve the existing sale/trade authority rather than create an independent sale ledger.

### Expense

Minimum path:

```text
खर्च भयो
→ category
→ amount
→ पुष्टि
```

Categories should begin with farmer-recognizable choices: दाना, पराल/घाँस, औषधि, कामदार, यातायात, बिजुली, पानी, मर्मत, प्रजनन, पशु, अन्य. More details remain optional.

### Payment received

Minimum path:

```text
पैसा पाएँ
→ party
→ amount
→ optional note
→ पुष्टि
```

The domain currently anchors settlements to one trade. A new party-level service operation should walk outstanding SALE trades for the party in oldest-first order and create one or more existing Settlement records until the entered amount is exhausted. The farmer sees one continuous payment event in Khata; the trade-level allocations remain underneath for auditability.

Rules for the first implementation:

- Only outstanding SALE trades for that party are eligible for `पैसा पाएँ`.
- The amount must be positive and must not exceed total outstanding receivable.
- Overpayment is rejected atomically; no unapplied-money model is introduced yet.
- All allocation settlements are written in one validated persistence transition or none are written.
- A `BOTH` party keeps receive and pay directions separate; `पैसा पाएँ` never silently nets a purchase payable against a sale receivable.

The choice is deterministic, visible in an expanded Khata detail, and covered by tests.

## Stock and production boundaries

Stock and production are important pillars, but they should not be bolted onto the next sale form without a model boundary.

### Essential stock direction

Start with farmer-friendly movements:

- `किनेँ` → increase stock.
- `प्रयोग गरेँ` → decrease stock.
- Optional `बिग्रियो` / spoiled → decrease stock with a reason.

Units must support kg, बोरा, packet, bottle, litre, piece, bundle, and a farm-defined unit. The first stock model should answer quantity-on-hand and low-stock status; it should not expose warehouse terminology.

### Production direction

Production should be a configurable farm activity/product, not a dairy-only field:

- Dairy: milk morning/evening litres.
- Vegetable: tomato kilograms.
- Poultry: eggs count.
- Goat: milk, kids, or saleable animals as later configured products.

The first production slice should establish a generic activity/product boundary and one simple daily quantity flow. Animal-level management remains later scope.

## Scope decision for the next implementation slice

The first build slice after this design record should be:

### Product-and-quantity sale flow tied to Khata

1. Add a farmer-facing `बेचेँ` entry point from Home.
2. Select or create a customer using familiar party rows.
3. Select a generic farm-local product, initially allowing a small local product set such as दूध.
4. Enter quantity and unit.
5. Reuse or enter a rate and derive the existing trade total.
6. Choose `पैसा पाएँ` or `उधार`.
7. Save through the existing Trade/Settlement authority.
8. Return to the farmer-facing Khata and show the updated balance plus the arithmetic.
9. Add `पैसा पाएँ` from Khata using oldest-outstanding-first allocation.

This slice should be intentionally narrow. It should not simultaneously introduce full inventory, voice, photos, recurring schedules, animal records, cloud sync, or a new accounting subsystem.

## Implemented slice decisions

- Farm schema advances from v6 to v7 by appending farm-local products and linked product-sale details.
- Existing v6 and older farms decode with empty product/detail collections; historical Trade descriptions are not rewritten.
- Reset Farm Data preserves the product catalog but clears product-sale details with the operational/accounting records.
- Delete Farm removes the catalog and details with the owning FarmState.
- Backup export/import carries products and product-sale details through the existing FarmPersistenceCodec envelope.
- Customer-level `पैसा पाएँ` allocates oldest outstanding SALE trades first and rejects overpayment atomically.
- Customer advances/unapplied credit remain a known future requirement, not a permanent business rule.
- Quick Sale is generic across products and units; no dairy-specific branch, stock mutation, or production record is created.
- Rate suggestions are deliberately deferred until a small, safe lookup can be added without changing the accounting authority.

## M7.3 Production Recording

M7.3 adds a small operational production record. It reuses `FarmProduct` and `ProductUnit`: milk, eggs, tomatoes, grain, and other outputs use the same product identity already available to sales. No duplicate dairy-only output model is introduced.

`ProductionRecord` stores product id, exact decimal quantity, matching unit, timestamp, optional session (`MORNING`, `EVENING`, `OTHER`), and optional note. Production is not a transaction, sale, settlement, supply movement, income, expense, or stock balance.

The Production Home action defaults to today and now. Morning and Evening are convenient named sessions; Other permits generic records. For a product + local day + Morning/Evening session, a second entry updates the existing record instead of silently creating a duplicate. Other records may occur multiple times. Local-day grouping uses the existing device timezone conventions.

Today's summary totals records by product and shows session-independent quantities. Production does not affect Financial Overview, Home cash, PartyLedger, receivables, payables, or supply stock. Production minus sales is explicitly deferred because home use, processing, spoilage, feeding, and transfers are not modeled yet.

Farm schema advances from v8 to v9 by appending production records. Older farms decode with an empty production collection; backup round-trips preserve records; reset clears production while preserving reusable products; deleting a farm removes production with the farm.

Editing a Morning/Evening record uses the same session upsert behavior. Erroneous records can be deleted with confirmation. Charts, reconciliation, processing, waste, home consumption, animal feeding, forecasting, livestock, and production reports remain future work.

## M7.4 Production Allocation

M7.4 explains where produced output went without turning production into inventory. `ProductionAllocation` references the same `FarmProduct` and `ProductUnit` and records a quantity, local timestamp, type, and optional note. Types are generic: `HOME_USE`, `PROCESSING`, `ANIMAL_FEED`, `WASTE`, and `OTHER`.

Daily reconciliation is derived only when units match exactly:

```text
produced
- sold from matching ProductSaleDetail + Trade facts
- home use
- processing
- animal feed
- waste
- other
= unexplained
```

Sales are never duplicated as allocations and are never inferred from descriptions. Unit conversion is not attempted. A mismatch is surfaced as non-reconcilable rather than calculated falsely. Existing sales may make the result negative; the UI reports the inconsistency instead of clamping or rewriting sale history. New non-OTHER allocation entry is rejected when it exceeds current unexplained quantity.

The farmer-facing Production dialog includes today's total and a `प्रयोग` path for allocation. Processing does not create ghee/yogurt outputs, animal feed does not create livestock records, and waste has no financial-loss calculation. Allocations are operational and financially neutral: they do not affect cash, Trade, Settlement, PartyLedger, or Financial Overview.

Farm schema advances from v9 to v10 by appending allocation records. Older farms decode with empty allocations; backup round-trips preserve them; reset clears production and allocations while preserving products; delete removes them with the farm. Multiple allocations of the same type on a day are allowed.

True inventory, produced-versus-sold stock, transformation recipes/yields, spoilage accounting, home-consumption accounting, animal feeding, forecasting, charts, and reports remain deferred.

## M7.5 Farmer Daily / Monthly Overview

M7.5 adds `FarmerOverview` as a pure, non-persisted read model over existing FarmState authorities. No schema change was made: FarmState remains schema 10, and no daily/monthly totals, cached balances, or summary records are stored.

### Metric definitions

- **Sales**: SALE Trade totals whose Trade timestamp falls in the local day/month.
- **Money received**: Settlement amounts linked to SALE Trades whose Settlement timestamp falls in the local day/month.
- **Expenses**: EXPENSE FarmTransaction amounts in the local day/month. Supply purchases are counted once through their existing expense transaction.
- **Customer receivable**: current sum of PartyLedger SALE outstanding balances across customer-compatible parties. This is a current point-in-time balance, not a monthly flow.
- **आज उधार बिक्री**: today's SALE total minus settlements recorded at the Trade's creation timestamp. This is credit created by today's sale events, not a misleading receivable delta.
- **Production**: ProductionRecord quantities grouped by product for the local day/month.
- **Unexplained production**: today's ProductionReconciliation unexplained quantity when present; negative values remain visible as inconsistency.
- **Supplies remaining**: derived supply purchase quantity minus usage quantity.

The overview never shows profit, margin, or a combined earnings claim. Sales, cash received, expenses, and current receivable remain separate facts.

### Farmer-facing surface

Home shows a compact Today block with production, sales, money received, expenses, current `लिन बाँकी`, today's credit sales, unexplained production, and remaining supplies when relevant. `यो महिना` opens the current local-calendar-month summary with production, sales, money received, expenses, receivable, and supplies.

Daily and monthly boundaries use the existing device timezone conventions. No custom-date report builder, chart, PDF, tax report, or accounting statement was added.

Device evidence: the final APK installed on ZA22374XPC with `adb install -r`; the Nepali Home surface showed `उत्पादन`, derived Today metrics, and `यो महिना`. The full temporary-data overview mutation scenario was covered by focused read-model tests; the real farm was not mutated.

## M7.2 Farm Supplies & Simple Stock

M7.2 extends the farmer workflow with `किनेँ`, `प्रयोग गरेँ`, and `बाँकी` for generic farm supplies. It deliberately does not introduce warehouse or ERP concepts.

### Final domain decision

- `FarmSupply` is a farm-local reusable supply definition with a name and governed unit.
- `SupplyPurchaseDetail` explains stock-in and links to exactly one existing EXPENSE `FarmTransaction`.
- `SupplyUsage` is a stock-out movement with quantity, time, and optional note.
- Current stock is derived as total valid purchase quantity minus total valid usage quantity.
- No mutable current quantity is stored and no unit conversion is attempted; 10 kg and 2 bags remain different quantities.

### Accounting integration

One farmer purchase action atomically creates:

1. one existing EXPENSE transaction, which remains the financial authority; and
2. one linked `SupplyPurchaseDetail`, which records physical stock-in.

The same purchase is never entered twice. The default first-slice expense category is the existing `SUPPLIES` category. Supplier credit/purchase-trade workflows remain deferred.

Usage creates only a `SupplyUsage`; it does not create an expense. Usage greater than derived available stock is rejected before persistence, so stock cannot become negative.

### Schema and migration

Farm persistence advances from v7 to v8 by appending supplies, purchase details, and usage movements. Existing v7 and older farms decode with empty supply collections. Existing expenses, trades, settlements, products, and product-sale details remain unchanged. Backup round-trips include the new farm-owned data. Reset Farm Data preserves reusable supply definitions but clears purchase details and usages; Delete Farm removes everything with the farm.

### Farmer-facing surface

Home adds compact daily actions:

- `किनेँ` / Bought
- `प्रयोग गरेँ` / Used
- `बाँकी` / Remaining

The Remaining view shows each supply's derived quantity and the bought/used totals. It does not expose SKU, inventory valuation, procurement, stock-ledger, or warehouse terminology.

### Explicit M7.2 deferrals

No stock categories, batch/lot tracking, expiry dates, unit conversion, inventory valuation, purchase orders, reorder automation, barcode, recurring purchase, voice, production, livestock, supplier-management redesign, account, cloud, sync, premium, ads, or Firebase/backend work is included.

## M7.1 usability decisions

- Recent customer and product ordering is derived from existing SALE Trade/ProductSaleDetail history; no analytics or recommendation state is stored.
- Rate suggestion resolution is exact customer + product newest rate, then product newest rate, then blank. Legacy Trades without ProductSaleDetail are ignored.
- Quick Sale includes a minimal inline customer form for name and optional phone. It creates a CUSTOMER Party and returns to the open sale with the new customer selected.
- The selected customer's current `PartyLedger` balance appears before saving a sale.
- The payment summary updates in the same dialog: total, paid in full/credit, or received now plus remaining balance.
- After a successful save, `Sell again` reopens Quick Sale with the same customer, product, and rate context but an empty quantity, preventing accidental duplicate saves.
- Received Money shows the selected customer's current outstanding amount and offers a Full amount action. The existing oldest-first allocation and overpayment rejection remain unchanged.
- Customer advance/unapplied credit remains deferred.

The M7.1 UI intentionally keeps the existing full Party editor in Hisab-Kitab. The inline form is only a fast customer-creation path for Quick Sale.

The repository still has unrelated Android-test source debt: older settings tests reference removed ids such as `settingsCurrencyText`, `changeSettingsCurrencyButton`, and `settingsNoFarmText`. M7.1 does not rewrite those tests.

## Acceptance criteria for that slice

- A new user can record a cash milk sale without seeing “Trade,” “Settlement,” or “Receivable.”
- A new user can record a credit milk sale for a named customer in a short, tap-oriented flow.
- A repeat customer can reuse the last product/rate suggestion.
- Milk, eggs, vegetables, गोबर, and घिउ use the same sale primitive; none requires a dairy-specific code path.
- A product sale persists queryable product, quantity, unit, and rate detail linked to the Trade.
- A party payment creates deterministic oldest-first Settlement allocations without invoice selection in the primary UI.
- The customer's Khata updates immediately and shows the sale in chronological history.
- A partial payment remains visible as a payment event and produces a transparent current balance.
- Existing `Trade`, `Settlement`, `PartyLedger`, backup, and migration tests remain authoritative and pass.
- EN/NE strings have parity and use farmer-facing terminology.
- The flow works with 36sp text and does not require dense multi-field forms.
- No Home cash double-entry is introduced accidentally.
- No account, premium, ads, sync, cloud, or Firebase backend work is included.

## Explicit non-goals

- Full livestock management.
- Voice interpretation.
- Photo identity matching.
- Automated recurring delivery schedules.
- Customer messaging or shared Khata export.
- A general unapplied-payment or multi-direction settlement model.
- Profit claims that merge cash activity and trade obligations.
- Replacing the rigorous domain model with a second farmer-only ledger.

## Validation plan before implementation

The next coding task should begin with focused tests for:

1. Product quantity × rate derives the expected minor-unit trade total.
2. Cash and credit sale choices create the correct existing trade/settlement facts.
3. Repeated customer defaults do not silently overwrite the farmer's chosen rate.
4. Partial payment from Khata allocates oldest outstanding SALE trades first across multiple sales.
5. Overpayment is rejected atomically with no partial settlements written.
6. Khata arithmetic matches the underlying authoritative facts.
7. Backup round-trip preserves product, quantity, unit, and rate.
8. Existing v6 farms and old backups decode with empty product/detail lists.
9. EN/NE resource parity and 36sp UI rendering remain valid.

Only after this contract is accepted should the farmer-facing Home and sale flow be implemented.