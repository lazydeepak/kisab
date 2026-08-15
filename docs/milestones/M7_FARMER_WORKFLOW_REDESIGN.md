# Kisab M7 — Farmer Workflow Redesign: Khata First

## Status

Design record only. No application code, schema, or dependency changes are part of this pass.

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

The underlying per-trade settlement model remains useful, but the farmer should not have to choose which trade received a partial payment unless allocation is genuinely ambiguous. The next design slice must decide and document the allocation rule before implementing `पैसा पाएँ` from a party Khata.

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

For a first slice, `product`, `quantity`, `unit`, and `rate` may be represented as an entry draft that derives the existing `Trade.totalMinor`. Product catalog persistence can be deliberately small and farm-local. The UI must preserve the existing sale/trade authority rather than create an independent sale ledger.

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

The domain currently anchors settlements to one trade. The workflow design must choose one of these before coding:

1. Apply automatically to the oldest outstanding trade for that party and show the resulting allocation.
2. Apply automatically to the most recent outstanding trade and show the allocation.
3. Present a simple outstanding-items chooser only when more than one allocation is plausible.

The choice must be deterministic, visible, reversible through existing settlement editing, and covered by tests.

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
3. Select a farm product, initially allowing a small local product set such as दूध.
4. Enter quantity and unit.
5. Reuse or enter a rate and derive the existing trade total.
6. Choose `पैसा पाएँ` or `उधार`.
7. Save through the existing Trade/Settlement authority.
8. Return to the farmer-facing Khata and show the updated balance plus the arithmetic.

This slice should be intentionally narrow. It should not simultaneously introduce full inventory, voice, photos, recurring schedules, animal records, cloud sync, or a new accounting subsystem.

## Acceptance criteria for that slice

- A new user can record a cash milk sale without seeing “Trade,” “Settlement,” or “Receivable.”
- A new user can record a credit milk sale for a named customer in a short, tap-oriented flow.
- A repeat customer can reuse the last product/rate suggestion.
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
- Multi-trade settlement allocation without a documented rule.
- Profit claims that merge cash activity and trade obligations.
- Replacing the rigorous domain model with a second farmer-only ledger.

## Validation plan before implementation

The next coding task should begin with focused tests for:

1. Product quantity × rate derives the expected minor-unit trade total.
2. Cash and credit sale choices create the correct existing trade/settlement facts.
3. Repeated customer defaults do not silently overwrite the farmer's chosen rate.
4. Partial payment from Khata follows the selected deterministic allocation rule.
5. Khata arithmetic matches the underlying authoritative facts.
6. Backup round-trip preserves product, quantity, unit, and rate if those become persisted facts.
7. EN/NE resource parity and 36sp UI rendering remain valid.

Only after this contract is accepted should the farmer-facing Home and sale flow be implemented.