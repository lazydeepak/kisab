# Kisab Competitive Farmer UX and Navigation Redesign

## Document status

- Phase: Product audit, information architecture, and redesign specification.
- Planning branch: `design/competitive-farmer-ux-redesign`.
- Verified baseline: `d3665278dfe1859076ee259347ec1f7c8fa3bf45`.
- Gate B baseline: `f09cb63`.
- M7 status: feature-frozen; Gate A, Gate B, and Gate C are `PASS`.
- Implementation status: not started.
- Domain schema: 12; this specification does not change it.
- Audience: product owner, designer, Android implementer, reviewer, and physical-device validator.

This document defines the target product architecture. It does not authorize implementation until the product-owner decisions at the end are approved.

## 1. Executive assessment

Kisab is a working farmer product presented through an accumulated developer-oriented shell. Its accounting, production, supply, persistence, localization, 36sp, and dark-mode foundations are substantially validated. The redesign is therefore not a rescue and must not rewrite those foundations.

The five largest current UX problems are:

1. **Top-level navigation does not match farmer jobs.** `Home`, `Hisab-Kitab`, and `Hisab` divide the product using overlapping bookkeeping language. `Hisab` contains calculators and period summaries; `Hisab-Kitab` contains parties, trades, settlements, Khata, and financial overviews. A farmer cannot predict ownership from the labels.
2. **Home is both launcher, dashboard, editor host, activity history, and tool drawer.** Seven operational actions, generic entries, balances, Today, Month, recent transactions, and tools compete in one long scroll. Progressive disclosure reduced one symptom but not the architecture.
3. **Frequent workflows are long modal forms.** Sale, purchase, production, allocation, and payment rely on programmatic dialogs with nested create flows, spinners, keyboard management, and delayed context. They work at 36sp but demand concentration and memory.
4. **The same facts are summarized in several incompatible structures.** Home Today, Hisab-Kitab financial overview, party Khata, and Hisab period summaries repeat money facts with different labels and hierarchy. This weakens trust even when calculations are correct.
5. **Production and supplies are treated as Home buttons, not coherent farm-work experiences.** Kisab's strongest differentiation from generic Khata apps is operational context, yet production reconciliation and supply movement are buried behind isolated actions.

The target architecture is:

```text
Today | Khata | Record | Farm Work | More
```

`Record` is a persistent action, not a destination. It opens a task sheet. Today answers the current farm-day questions; Khata answers who owes whom; Farm Work explains production and supplies; More contains review, tools, administration, and settings. The active farm is always visible and switchable from the app bar.

The central product idea is **a farm-day command surface backed by trustworthy Khata**, not a generic accounting dashboard and not a prettier button stack.

## 2. Evidence and non-negotiable domain boundaries

### 2.1 Validated evidence

- Gate A proved `69 L produced -> 57 L sold -> 2 L home use -> 6 L processing -> 3 L animal feed -> 1 L unexplained` through the physical-device UI. The clean evidence is on `validation/m7-gate-a-production-allocation` at `f3a6668` (record commit `73689a4`); the Gate C branch's final summary incorporates its `PASS` disposition without carrying the full Gate A section. Gate A also identified unlabeled allocation selection and stale-in-dialog summary feedback as usability friction.
- Gate B proved a single PURCHASE Trade authority, initial and later supplier settlements, stock/payment independence, no duplicate EXPENSE, schema-12 persistence, and cleanup. It corrected a codec defect at `f09cb63` with regression coverage.
- Gate C proved a populated Nepali farm remains operational at 36sp and Dark mode. Home, Month, Production, Sell, Bought, supplier payment, navigation, rotation, persistence, and Other Entries remained reachable. It made no app-code changes.
- The Product interaction concern was conclusively an automation-coordinate issue, not an app click defect.

### 2.2 Authorities the interface must preserve

| Farmer fact | Authority | UX rule |
| --- | --- | --- |
| General cash income/expense | `FarmTransaction` | Keep as advanced manual money entry; never silently create it from Trade or Settlement. |
| Sale/purchase obligation | `Trade` | A sale or supplier purchase establishes value owed; do not label it as cash movement. |
| Money received/paid | `Settlement` | Show as a payment event; do not merge it into sale/purchase totals. |
| Customer/supplier running Khata | `PartyLedger` | Present directional `लिन बाँकी` and `तिर्न बाँकी`; never net them into one ambiguous balance. |
| Product, quantity, and rate behind a sale | `ProductSaleDetail` | Explain the Trade arithmetic; do not create a parallel sale ledger. |
| Produced output | `ProductionRecord` | Operational fact, not income or supply stock. |
| Where production went | `ProductionAllocation` | Operational explanation, not expense, transformation, or inventory valuation. |
| Supply stock-in source | `SupplyPurchaseDetail` | Physical purchase detail linked to exactly one financial source. |
| Supply stock-out | `SupplyUsage` | Changes quantity only; supplier payment must not alter it. |
| Today/Month summary | `FarmerOverview` | Derived view, never persisted as another authority. |

UX simplification may reduce choices, remember context, and improve labels. It must not change these facts, infer missing unit conversions, hide validation errors, or combine unrelated balances.

## 3. Current-state screen audit

### 3.1 Home

**Farmer job:** start daily work and understand the current farm state.

**Current contents:** farm name, generic balance/income/expense, Other Entries disclosure, Sell, Received Money, Bought, Used, Remaining, Paid Money, Production, Today text block, Month dialog, transaction editor, recent transactions, and farm tools.

**Assessment:** highest-frequency destination but structurally overloaded. The action labels are good; their equal visual weight is not. Generic balance at the top competes with directional Khata. Today is a ten-line text block rather than a decision hierarchy. Recent activity and tools extend the page after the farmer has already passed the key answers.

**Decision:** `RESTRUCTURE` into Today. Remove no capability; move ownership to Khata, Farm Work, Activity, and More.

### 3.2 Hisab-Kitab

**Farmer job:** inspect customers/suppliers, create obligations, record payments, and understand financial position.

**Current contents:** cash/trade overview, parties, trades, settlements, editors, party Khata, directional balances, and chronological rows.

**Assessment:** contains the strategically strongest experience but is named as a module. It mixes a farm-wide finance report with person-level Khata and advanced trade entry. Party Khata itself is valuable: current total followed by chronological explanation is the correct trust model.

**Decision:** `RESTRUCTURE` as top-level Khata. Move farm-wide period review to Overview/Activity. Keep party history and contextual payment/sale actions.

### 3.3 Hisab

**Farmer job:** calculate farm quantities/costs and review a party over a period.

**Current contents:** arithmetic, profit, interest, land, seed, fertilizer, feed, milk, crop-yield calculators, planning tools, and party period Hisab.

**Assessment:** the label overlaps Hisab-Kitab while content is mostly occasional tools. Profit calculators are tools, not an authoritative Kisab profit metric. This destination does not merit prime bottom-navigation space.

**Decision:** `RETIRE` as a top-level surface. Move calculators to More > Farmer Tools. Move party period review into Khata filters.

### 3.4 Farms and Farm Details

**Farmer job:** know which farm is active, switch, create, rename, back up, reset, or delete.

**Assessment:** switching is too hidden for a multi-farm app. Farm Details correctly isolates destructive controls but combines ordinary identity/currency changes with danger-zone actions.

**Decision:** `MOVE` switching to the global farm switcher. Keep a Farms management screen under More. `RESTRUCTURE` Farm Details into identity/data/danger sections.

### 3.5 Settings

**Farmer job:** change language, appearance, notifications, data handling, and app information.

**Assessment:** logically grouped and appropriately infrequent, but too much of it is mixed into one long page. Account-related controls are not part of the frozen redesign scope and must not be elevated.

**Decision:** `KEEP` under More, with child pages for Language & Appearance, Notifications, Data & Backup, Account status, and About. Existing local/linked account presentation remains available but is not expanded. Update checking/install and private-build expiry remain release-critical shell behavior: an update is owned by About, while expiry uses a persistent banner above destination content and must not displace or masquerade as the Today headline. Notification deep links must resolve to the new owning destination.

### 3.6 Production

**Farmer job:** record output by session, correct it, explain use, and see reconciliation.

**Assessment:** correct model, weak presentation. A single dialog combines entry and summary; allocation uses an unlabeled spinner; summary can require reopening to refresh; per-type allocation totals are not visible.

**Decision:** `RESTRUCTURE` as a Farm Work child screen with a day/product summary and full-screen entry flow. Keep session upsert behavior and reconciliation unchanged.

### 3.7 Sell and Received Money

**Farmer job:** record what was sold and what money arrived.

**Assessment:** farmer verbs are strong and accounting separation is correct. Long dialogs ask for several choices at once. Party/product creation nested inside the sale dialog interrupts orientation. Received Money is party-level and appropriately allocates oldest-first, but the farmer should see before/after balance.

**Decision:** `RESTRUCTURE` as short full-screen task flows with visible arithmetic and confirmation. Keep contextual entry from Khata and Today.

### 3.8 Bought, Supply Usage, Remaining, and Supplier Payment

**Farmer job:** bring supplies in, record use, inspect stock, and pay suppliers.

**Assessment:** four Home actions expose the complete model but not one coherent object. `किनेँ` and `पैसा तिरेँ` correctly separate quantity and payment. Remaining is read-only and should not compete as an action.

**Decision:** `MERGE` access under Farm Work > Supplies. Keep Bought and Used as contextual actions; make Remaining the default supply list state; keep Paid Money in both supplier Khata and Record sheet.

### 3.9 Other Entries

**Farmer job:** record general income or expense not represented by a farmer workflow.

**Assessment:** appropriate progressive disclosure, but Home is still not the right permanent owner.

**Decision:** `MOVE` to Record > Other money and More > Records. Preserve the generic editor and all existing records.

### 3.10 Today and Month

**Farmer job:** understand what happened and what remains unresolved.

**Assessment:** metric definitions are excellent; presentation gives ten rows similar emphasis. Flow metrics, current balances, production, unexplained output, and supplies are mixed in one paragraph-like block. Month is a read-only dialog that cannot lead to detail.

**Decision:** `RESTRUCTURE` Today as the Home hierarchy and Month as a proper Overview screen with links to underlying lists.

### 3.11 History surfaces

**Farmer job:** answer “what happened?” and correct a mistaken fact.

**Assessment:** recent transactions, trades, settlements, party ledger entries, production records, and supply movements live in separate sections with inconsistent row language. Their separation is semantically correct, but discovery and row design are inconsistent.

**Decision:** `KEEP` authority-specific histories, unify their visual grammar, and expose a filterable Activity screen under More. Never merge records into one editable data model. Persisted Activity renders one row per authoritative fact. A party-level payment that creates several Settlements may be summarized as one result only in the immediate transient confirmation; after reload it appears as one row per Settlement with its trade-allocation context because no persisted payment-group identifier exists.

### 3.12 Legacy farm entries and release states

The existing farm-local crop/livestock entry primitive predates the frozen farmer workflows. This redesign does not add livestock management and must not imply animal-level capability, but it also must not discard existing records.

- Existing crop/livestock entry list and add-entry flow: `MOVE` to More > Advanced farm records, preserving data and edit behavior.
- No-farm state: `RESTRUCTURE` as a focused create/select farm screen; do not render empty Today, Khata, or Farm Work destinations as if data exists.
- Account/local-only/linked state: `KEEP` under Settings > Account status; no sign-in expansion.
- App update/download/install: `KEEP` under Settings > About with a shell-level update cue when required.
- Private-build warning/expiry: `KEEP` as shell-level release state with current read-only/expiry safeguards.
- Notification deep links: `RESTRUCTURE` routing only; target Today, Khata, Farm Work, or the appropriate More child without changing notification capability.

## 4. Farmer jobs hierarchy

### Daily / high frequency

1. Record production for the current session.
2. Record a sale and whether money arrived.
3. Record money received from a customer.
4. Record a supply purchase and whether money was paid.
5. Record supply use.
6. Record money paid to a supplier.
7. Record a farm expense such as labor, transport, veterinary cost, diesel, or repair.
8. See today's production, sale, cash movement, and unresolved quantities.

### Frequent reference / review

1. Who owes me and how much?
2. Whom do I owe and how much?
3. How much supply remains?
4. Where did today's production go?
5. What happened recently?

### Periodic

1. Understand the current month without a false profit claim.
2. Review a customer's or supplier's Khata over a period.
3. Correct a production session or a currently supported general transaction. Product-sale quantity/rate correction is not exposed because no atomic Trade + ProductSaleDetail update operation currently exists.
4. Switch farms.

### Administrative / advanced

1. General income and infrequent non-work expense review.
2. Farm calculators and planning tools.
3. Create/rename farms and change currency safely.
4. Backup, restore, reset, and delete.
5. Language, text size, appearance, notifications, and About.

Immediate visibility belongs to current status and context-sensitive high-frequency actions. Reference information belongs in top-level Khata and Farm Work. Periodic and administrative work belongs under More or contextual detail.

## 5. Competitive observations and quality bar

Research was performed on 2026-08-16 using current Google Play listings. Listing text and visible review excerpts are treated as observations; unavailable screenshot details are not invented.

Sources: [Karobar Google Play listing](https://play.google.com/store/apps/details?id=com.bytecaretech.merokarobar), [OkCredit Google Play listing](https://play.google.com/store/apps/details?id=in.okcredit.merchant). Ratings, downloads, reviews, and release text are time-sensitive observations from that date.

### 5.1 Directly observed

**Karobar - Nepali Digital Khata**

- Positions itself as a digital khatabook for business accounting and inventory.
- Google Play showed 500K+ downloads, approximately 4.9 stars, 3.7K reviews, in-app purchases, and an update dated 2026-08-10.
- Current release notes mention transaction reports, staff reports, invoice image generation, out-of-stock controls, AD/BS calendar switching, and staff date-lock permissions.
- Review excerpts expose product risks despite breadth: a settled party amount not updating the main balance, no search when transferring between accounts, and destructive party deletion friction.
- Data-safety listing states no third-party sharing, possible collection of personal information/contacts and other data, encryption in transit, and deletion requests.

**OkCredit**

- Positions itself around Ledger/Khata, collection, UPI, billing, stock, and loans.
- Google Play showed 10M+ downloads and an update dated 2026-08-14.
- Its breadth sets expectations for immediate Khata discoverability and connected collection workflows, but its financial and data-collection scope is not Kisab's target.

### 5.2 Inferred principles

- Mature users expect Khata to be directly accessible, searchable, and trustworthy.
- Feature breadth does not compensate for ambiguous balance updates or poor search.
- Strong business apps connect summary values to inspectable detail and immediate next actions.
- Inventory labels create expectations Kisab should avoid until it has true inventory semantics.
- Trust cues include explicit backup state, clear current farm, reversible ordinary edits, and guarded destructive actions.

### 5.3 Proposed Kisab behavior

- Lead with the farmer's day, not a business feature catalog.
- Make Khata directional and searchable from the first screen of that destination.
- Make production and supplies first-class farm work, not generic inventory.
- Preserve offline-first, inspectable arithmetic and backup safeguards as visible trust advantages.
- Use fewer, clearer actions and contextual entry instead of imitating competitor dashboards.

## 6. Kisab differentiation

Kisab can be better for farmers because it can connect three views of the same day without corrupting them:

1. **What the farm produced.** Session-based output and unexplained reconciliation.
2. **Where goods and supplies moved.** Sales, home use, processing, animal feed, waste, purchases, usage, and remaining quantities.
3. **Who paid or still owes.** Customer receivable and supplier payable through inspectable Khata.

Generic bookkeeping apps start with money and optionally bolt on stock. Kisab should start with farm work and explain its money consequences. It must say “produced 69 L, sold 57 L, used 11 L, 1 L unexplained” alongside “sold रु 5,700, received रु 4,000, रु 1,700 still to receive” without calling either pair inventory valuation or profit.

## 7. Proposed information architecture

### 7.1 Global shell

- **App bar:** active farm name with dropdown, current destination title, overflow for help/about only where needed.
- **Bottom navigation:** Today, Khata, central Record action, Farm Work, More.
- **Record:** opens a bottom sheet; it never owns persistent content or selection state.
- **Back:** returns from child screen to owning destination; dirty forms retain existing discard protection.

### 7.2 Top-level destinations

#### Today / `आज`

**Meaning:** what happened on this farm today and what needs attention.

**Contains:** current farm-day headline, production status, money flows, directional balances, unresolved production/supply attention, and recent meaningful events.

**Primary jobs:** scan the day; record production or sale contextually; open a highlighted balance or issue.

**Does not contain:** generic transaction editor, full history, calculators, farm administration, or seven equal buttons.

#### Khata / `खाता`

**Meaning:** people and businesses with money to receive or pay.

**Contains:** search, segmented `लिन बाँकी` / `तिर्न बाँकी` / All, party rows, directional totals, party history, period filter, and contextual payment/sale/purchase actions.

**Primary jobs:** find who owes whom, inspect why, record a payment immediately.

**Does not contain:** farm-wide profit, production, supplies, or unrelated generic cash entries.

#### Record / `लेख्नुहोस्` (action)

**Meaning:** record something that just happened.

**Contains:** task choices grouped by Farm output, Sales & payments, Supplies, and Other money.

**Primary jobs:** launch any common fact in two taps from anywhere.

**Does not contain:** summaries, history, or settings.

#### Farm Work / `फार्मको काम`

**Meaning:** production and supplies on the active farm.

**Contains:** Production and Supplies sections, today's reconciliation, remaining supplies, product/supply lists, and their histories.

**Primary jobs:** record and explain output; buy/use/check supplies.

**Does not contain:** customer/supplier running money history, general income/expense, or inventory valuation.

#### More / `अरू`

**Meaning:** less frequent review, tools, farms, and app administration.

**Contains:** This Month & Activity, Farmer Tools, Advanced farm/money records, Farms, Settings, and a Data & Backup shortcut that deep-links to the single canonical Settings > Data & Backup screen.

**Primary jobs:** periodic review and administration.

**Does not contain:** a second copy of daily actions or Khata.

### 7.3 Farm ownership

- Tapping the farm name opens a compact switcher showing active farm, other farms, and `फार्महरू व्यवस्थापन`.
- Switching requires one selection, not navigation through More.
- Add/rename/currency/reset/delete remain on Farms/Farm Details.
- Backup status may appear in Farm Details and Data & Backup, but backup execution has one authority.

## 8. Proposed navigation and screen map

```text
Kisab
├── Today / आज                                      RESTRUCTURE Home
│   ├── Daily status
│   ├── Attention items
│   ├── Recent farm events
│   └── Month overview                              MOVE from dialog
├── Khata / खाता                                    RESTRUCTURE Hisab-Kitab
│   ├── To receive / लिन बाँकी
│   ├── To pay / तिर्न बाँकी
│   ├── All people and businesses
│   └── Party Khata
│       ├── Directional balance
│       ├── Sale or purchase
│       ├── Receive or pay money
│       └── Period-filtered history
├── Record / लेख्नुहोस्                              NEW shell action, existing capabilities
│   ├── उत्पादन
│   ├── बेचेँ
│   ├── पैसा पाएँ
│   ├── किनेँ
│   ├── प्रयोग गरेँ
│   ├── पैसा तिरेँ
│   └── Other money
│       ├── आम्दानी
│       └── खर्च
├── Farm Work / फार्मको काम                         NEW destination, existing capabilities
│   ├── Production
│   │   ├── Day/product reconciliation
│   │   ├── Session entry/edit
│   │   └── Allocation history/entry
│   └── Supplies
│       ├── Remaining list
│       ├── Supply detail
│       ├── Bought
│       └── Used/history
└── More / अरू
    ├── Month & Activity                            MOVE overview/history
    ├── Farmer Tools                                MOVE current Hisab calculators
    ├── Advanced records
    │   ├── General income/expense                  MOVE Other Entries/history
    │   └── Existing crop/livestock entries         MOVE, no capability expansion
    ├── Farms
    │   ├── Farm switcher
    │   ├── Add Farm
    │   └── Farm Details
    ├── Settings
    │   ├── Account status                          KEEP existing state only
    │   ├── Language & Appearance
    │   ├── Notifications
    │   ├── Data & Backup
    │   └── About
    ├── Data & Backup shortcut                      MOVE, deep-link only
    └── Release state                               KEEP update/expiry shell behavior
```

### 8.1 Current-to-target disposition matrix

| Current surface | Target owner | Disposition | Retirement condition |
| --- | --- | --- | --- |
| Home | Today | `RESTRUCTURE` | UX-02 metric/link parity passes. |
| Home action buttons | Record + contextual actions | `RESTRUCTURE` | Each migrated task passes service-mutation parity. |
| Other Entries disclosure | Record > Other money | `MOVE` | Generic income/expense remains reachable. |
| Recent transactions | More > Activity/Advanced records | `MOVE` | Authority-specific edit/detail routing passes. |
| Farm Tools drawer | More > Farmer Tools/Advanced farm records | `MERGE` | Calculators and existing entries remain reachable. |
| Hisab-Kitab overview | Today/Month/Activity | `RESTRUCTURE` | Every existing factual metric has one owner. |
| Parties and Party Khata | Khata | `RESTRUCTURE` | Direction/BOTH/history/payment gates pass. |
| Generic trade/settlement editors | Contextual Khata/Advanced records | `MOVE` | No unsupported product-detail editing is exposed. |
| Hisab calculators | More > Farmer Tools | `MOVE` | Calculator parity passes. |
| Party period Hisab | Khata party filter | `MERGE` | Same period facts remain available. |
| Production dialog | Farm Work > Production | `RESTRUCTURE` | Gate A repeats on new flow. |
| Allocation dialog | Production detail | `RESTRUCTURE` | Labeled types/per-type totals/reconciliation pass. |
| Bought/Used/Remaining dialogs | Farm Work > Supplies | `RESTRUCTURE` | Gate B quantity/payment isolation repeats. |
| Received/Paid Money dialogs | Khata + Record | `RESTRUCTURE` | Before/after and settlement allocation pass. |
| Month dialog | This Month screen | `RESTRUCTURE` | Existing monthly read model displays unchanged facts. |
| Farms/Farm Details/Add Farm | App-bar switcher + More > Farms | `RESTRUCTURE` | Multi-farm and safeguards pass. |
| Settings | More > Settings child pages | `RESTRUCTURE` | Preferences/account/update/data parity passes. |
| Account state | Settings > Account status | `KEEP` | Not retired in this phase. |
| Update/About/expiry | Settings > About + shell state | `KEEP` | Not retired; routing adapts to shell. |

## 9. Home redesign: Today as a daily work surface

Today is a combination of status and contextual action with strict priority:

1. **Farm and date context.** Active farm and Today.
2. **One headline question.** Use a stable priority: invalid/negative reconciliation first, today's unexplained production second, blocked/zero supply only when it prevents recorded work, customer receivable next, then supplier payable. Show the remaining unresolved states in Attention; do not rotate headlines unpredictably. The headline is not an abstract net balance.
3. **Today's farm work.** Production and reconciliation, with `उत्पादन लेख्नुहोस्` when missing or `हेर्नुहोस्` when present.
4. **Today's sales and money.** Sales value, money received, and general expenses as separate compact metrics; sale value is not described as cash movement.
5. **Directional Khata.** To receive and to pay side by side only when 36sp permits; otherwise stacked.
6. **Supply attention.** Remaining quantities that are relevant, not the full stock list.
7. **Recent events.** Maximum three, with See all.

No “balance” should imply that receivable, payable, cash, sales, and expenses are one number. No Profit label is introduced.

### Home wireframe

```text
[ RC01 Upgrade Farm  v ]                         [⋮]

आज · १६ अगस्ट

लिन बाँकी
रु १,७००                         [खाता हेर्नुहोस्]

फार्मको काम
दूध उत्पादन        ६९ लि.
बेचेको              ५७ लि.
घर/प्रशोधन/दाना     ११ लि.
नखुलेको               १ लि.    [मिलाउनुहोस्]

आजको पैसा
बिक्री रु ५,७००   पैसा आएको रु ४,०००
खर्च रु ०          उधार बिक्री रु २,७००

तिर्न बाँकी रु १५,०००          [सप्लायर खाता]

सामान
दाना १७ बोरा                      [सबै हेर्नुहोस्]

हालै
रामबाट पैसा पाएको              + रु १,०००
दाना प्रयोग गरेको                 ३ बोरा
                                      [सबै गतिविधि]

[आज]   [खाता]   [ + लेख्नुहोस् ]   [काम]   [अरू]
```

At 36sp, metric pairs stack vertically, supporting labels remain visible, and the headline never truncates. The page may be longer; reachability matters more than preserving a one-screen dashboard.

## 10. Common-action entry model

### 10.1 Persistent Record sheet

The central Record action opens a bottom sheet at normal sizes and a full-height sheet/full screen at 36sp.

```text
के भयो?

फार्म
[ उत्पादन ]  [ बेचेँ ]  [ किनेँ ]  [ प्रयोग गरेँ ]

पैसा
[ पैसा पाएँ ]  [ पैसा तिरेँ ]  [ खर्च ]

अन्य
[ आम्दानी ]
```

The sheet uses icons plus labels, not color alone. It remembers no financial assumptions. Every mutating form shows `आज, अहिले · बदल्नुहोस्`; the current date/time is preselected, and the validated date/time picker remains available for backdated entry. Changed dates must survive navigation within the flow and drive the same local-day/month behavior as today.

### 10.2 Contextual shortcuts

- Today Production card -> Production entry/edit.
- Today unresolved production -> Allocation.
- Customer Khata -> Sell / Receive Money.
- Supplier Khata -> Bought / Pay Money.
- Supplies section -> Bought / Used.
- Product detail -> Production / Sell with product preselected.

Contextual entry removes selection steps without creating alternate authorities.

### 10.3 Flow container choice

- Use full-screen flows for Sell, Bought, Production, and allocation because they have multiple dependent choices and must survive keyboard/36sp reliably.
- Use a compact sheet only for choosing the task.
- Use a confirmation sheet/dialog for destructive or irreversible actions.
- Use a short sheet for payment only when party and direction are already known; otherwise use a full-screen flow.
- Nested create dialogs are retired. `नयाँ ग्राहक`, `नयाँ उत्पादन`, or `नयाँ सामान` becomes an inline branch that returns to the parent flow with selection preserved.

### Primary action wireframe

```text
बेचेँ                                             [बन्द]

कसरी बिक्री भयो?
[ नगद बिक्री ]  [ ग्राहकलाई ]

ग्राहकलाई छानेपछि
[ राम प्रसाद                         लिन बाँकी रु ० ]
[ + नयाँ ग्राहक ]

के बेचेँ?
[ दूध v ]

कति?                         प्रति लिटर
[ 57        ] लि.            [ रु 100      ]

जम्मा                         रु ५,७००

अहिले कति पैसा आयो?
( ) पूरै   (•) केही   ( ) आएन
[ रु 3,000 ]

मिति र समय
आज, अहिले                                      [बदल्नुहोस्]

बिक्रीपछि लिन बाँकी           रु २,७००

[ बिक्री सुरक्षित गर्नुहोस् ]
```

Cash sale is an explicit branch. It has no customer, must be paid in full, and uses the existing nullable-party sale contract. Partial or unpaid sale requires a customer. Customer sale preserves the three payment choices and visible resulting receivable.

### 10.4 Bought authority contract

Bought presents one physical purchase form and an explicit payment branch:

```text
किनेँ

के? [ दाना v ]     कति? [ 20 ] बोरा
जम्मा [ रु 40,000 ]

पैसा कसरी तिरियो?
[ नगद / सप्लायर छैन ]  [ सप्लायरबाट ]

सप्लायरबाट छानेपछि
सप्लायर [ Krishna Feed Store v ]
भुक्तानी [ पूरै ] [ केही ] [ तिरेको छैन ]
केही छानेपछि अहिले तिरेको [ रु 15,000 ]

मिति र समय
आज, अहिले                                      [बदल्नुहोस्]

तिर्न बाँकी रु 25,000
[ खरिद सुरक्षित गर्नुहोस् ]
```

- `नगद / सप्लायर छैन` uses exactly one legacy cash/EXPENSE-backed supply purchase authority and no PURCHASE Trade; it is fully paid by definition.
- `सप्लायरबाट` requires a supplier and uses exactly one PURCHASE Trade-backed supplier purchase plus an optional initial Settlement. It supports full, partial, or no opening payment and creates no duplicate EXPENSE even when fully paid.
- Both branches create exactly one `SupplyPurchaseDetail` linked to their mutually exclusive financial source.
- The implementation must call the existing branch-appropriate service operation; it may not create both sources.

## 11. Khata experience

Khata is one destination with explicit directions, not two separate databases and not one netted balance.

### 11.1 List model

- Header totals: `लिन बाँकी` and `तिर्न बाँकी`, never combined by default.
- Segmented filter: To receive, To pay, All.
- Search by name/contact.
- Party row shows name, role in plain language only when useful, directional amount, last event, and one context action.
- A BOTH party may appear in both directional filters with the corresponding amount; the row never silently nets directions.
- Zero-balance parties appear under All/Settled, not mixed ahead of active balances.

### 11.2 Party detail

- Directional headline first.
- Plain arithmetic summary second.
- Primary contextual action: Receive from customer or Pay supplier.
- Secondary action: Sell to customer or Buy from supplier.
- Chronological history uses verbs and signed direction, while expandable detail exposes underlying sale/purchase/payment facts.
- Period filter replaces the current separate party-period Hisab surface.
- Activity/history rows open authority-specific read detail. General transactions may use their supported editor; production sessions use session edit; product-sale quantity/rate remains read-only until an approved atomic update exists. A split party-level payment displays one persisted row per Settlement allocation after reload.

### Khata wireframe

```text
[खोज्नुहोस्...]                         [फिल्टर]

लिन बाँकी रु ३४,५००     तिर्न बाँकी रु १५,०००

[ लिन बाँकी ] [ तिर्न बाँकी ] [ सबै ]

राम प्रसाद
लिन बाँकी रु १,७००
पछिल्लो: पैसा पाएको रु १,०००             [पैसा पाएँ]
----------------------------------------------------
श्याम डेरी
लिन बाँकी रु १२,०००                       [पैसा पाएँ]
```

```text
राम प्रसादको खाता                              [⋮]

अहिले लिन बाँकी
रु १,७००

[ पैसा पाएँ ]          [ बेचेँ ]

यो महिना v
आज      पैसा पाएको                    रु १,०००
आज      दूध ५७ लि. बेचेको             रु ५,७००
         सुरुमा पाएको                   रु ३,०००
         बाँकी                          रु २,७००
```

## 12. Production experience

Production becomes a product/day reconciliation surface.

- Default to Today and last-used product, but always show both.
- Show session rows (Morning, Evening, Other) with quantity and edit affordance.
- Show reconciliation as a vertical equation: produced, sold, each allocation type, unexplained.
- Display per-type totals; Gate A showed that their absence makes validation and farmer understanding harder.
- Allocation type uses labeled choices, not an unlabeled spinner.
- Saving an allocation refreshes the visible summary immediately.
- Keep Morning/Evening upsert semantics visible with `सम्पादन` when an entry already exists.
- Negative or unit-mismatched reconciliation is an attention state, not silently clamped.

### Production wireframe

```text
उत्पादन                                 [आज v] [दूध v]

आज उत्पादन                              ६९ लि.

बिहान        ३८ लि.                       [सम्पादन]
बेलुका       ३१ लि.                       [सम्पादन]
अन्य          --                          [लेख्नुहोस्]

कहाँ गयो?
बेचेको       ५७ लि.                       [बिक्री हेर्नुहोस्]
घरमा          २ लि.
प्रशोधन        ६ लि.
पशुलाई         ३ लि.
नखुलेको        १ लि.                      [प्रयोग लेख्नुहोस्]

हालैका उत्पादन र प्रयोग                    [सबै]
```

This remains reconciliation, not inventory or transformation.

## 13. Supplies experience

Supplies default to the answer “what remains?” rather than three separate Home buttons.

- List by supply with remaining quantity and recent use.
- Supply detail shows bought, used, remaining, and movement history.
- Bought flow captures supply quantity and financial source together through existing service behavior.
- Supplier/payment context is visible but separate from stock quantity.
- Paying a supplier confirms payable before/after and does not imply stock change.
- No low-stock automation, valuation, batches, or unit conversion is introduced.

### Supplies wireframe

```text
सामान                                      [किनेँ]

[खोज्नुहोस्...]

दाना
१७ बोरा बाँकी                              [प्रयोग गरेँ]
किनेको २० · प्रयोग ३
----------------------------------------------------
औषधि
३ bottle बाँकी                             [प्रयोग गरेँ]
```

```text
दाना

बाँकी                  १७ बोरा
किनेको                 २० बोरा
प्रयोग                   ३ बोरा

[ प्रयोग गरेँ ]         [ किनेँ ]

पछिल्लो गतिविधि
आज      प्रयोग                       - ३ बोरा
आज      Krishna Feed Store बाट       + २० बोरा
         जम्मा रु ४०,००० · तिर्न बाँकी रु १५,०००
```

## 14. Today and Month hierarchy

### 14.1 Today

Use four semantic groups rather than ten equal lines:

1. **Attention:** current receivable/payable or unexplained production requiring action.
2. **Farm output:** produced, sold quantity, allocated, unexplained.
3. **Money today:** sales value, money received, expenses, today credit sales.
4. **Current position:** customer receivable, supplier payable, supplies remaining.

Current-position values are labeled as current, not “today flow.” Every metric links to its authority-specific detail.

### 14.2 Month

Month is a full screen under Today and More > Month & Activity.

```text
यो महिना

फार्मको उत्पादन
दूध १,८४० लि.                              [विवरण]

बिक्री र पैसा
बिक्री                     रु १,६५,६००
पैसा आएको                 रु १,३२,०००

खर्च                       रु   ७८,५००

अहिलेको खाता
लिन बाँकी                  रु   ४६,०००
तिर्न बाँकी                 रु   २१,०००

सामान बाँकी                                      [विवरण]
दाना १७ बोरा · औषधि ३ bottle
```

No total combines sales, receipts, expense, receivable, and payable. No monthly credit-created metric is shown because the frozen monthly read model does not expose one and receipts may settle older sales. No arbitrary month selector is implied; this screen is the current local calendar month. No Profit label appears. Expandable explanations define period flow versus current position.

## 15. Terminology review

| Current term | Decision | Proposed use | Reason / validation need |
| --- | --- | --- | --- |
| `बेचेँ` | KEEP | Sale action | Short, first-person farmer verb; test with real farmers. |
| `किनेँ` | KEEP | Supply purchase action | Clear action; context must distinguish supply from generic purchase. |
| `पैसा पाएँ` | KEEP | Customer payment | Directionally clear and validated. |
| `पैसा तिरेँ` | KEEP | Supplier payment | Directionally clear and validated. |
| `उत्पादन` | KEEP | Farm Work section and action | Semantically correct; pair with familiar product/session context. |
| `लिन बाँकी` | KEEP | Customer receivable | Strongest directional phrase; never replace with generic Balance. |
| `तिर्न बाँकी` | KEEP | Supplier payable | Strongest directional phrase; never net with receivable. |
| `आज उधार बिक्री` | IMPROVE | `आजको बिक्रीमा उधार` in explanatory contexts | Current term is correct but compact and accounting-like; compare both with farmers. |
| `प्रयोग गरेँ` | IMPROVE | Keep for supply use; use `कहाँ गयो?` / `प्रयोग लेख्नुहोस्` for production allocation | One verb currently covers two different operational contexts. |
| `बाँकी` | IMPROVE | `सामान बाँकी` or named quantity | Alone it can mean money or quantity. Always attach object. |
| `हिसाब-किताब` | REPLACE as navigation | `खाता` | Current destination is broader and less predictable; underlying reports remain. |
| `हिसाब` | REPLACE as navigation | `फार्म औजार` / `किसान औजार` for calculators; period review moves to Khata | Current label collides with Hisab-Kitab. Final Nepali tool label needs testing. |
| `अन्य अभिलेख` | IMPROVE | `अन्य आम्दानी/खर्च` in Record sheet | Says what can actually be entered and reduces abstraction. |
| `अवलोकन` | REPLACE where present | `आज`, `यो महिना`, or `सारांश` | Generic software noun; time/question labels are clearer. |
| `ग्राहक` | NEEDS FARMER TESTING | Customer filter/helper label | Common but retail-oriented; rows should prioritize names and direction. |
| `आपूर्तिकर्ता` | NEEDS FARMER TESTING | Supplier helper label | Long/formal; test `सप्लायर` and relationship-specific language. |
| `खाता` | NEEDS FARMER TESTING | Top-level destination | Familiar and shorter; verify whether farmers distinguish it from bank account. |
| `फार्मको काम` | NEEDS FARMER TESTING | Production/supplies destination | Concrete but potentially broad; compare with `उत्पादन र सामान`. |
| `लेख्नुहोस्` | NEEDS FARMER TESTING | Central Record action | Explicit action; compare with `थप्नुहोस्` and icon comprehension. |

English should use Today, Khata, Record, Farm Work, and More during farmer-oriented testing. “Ledger,” “Settlement,” “Trade,” and “Party” remain implementation or advanced-detail terms, not primary labels.

## 16. Current versus proposed workflows and taps

Tap counts are approximate and exclude typing. Count one tap for opening a destination/sheet, selecting each choice, focusing a field only when needed, changing date/time, saving, and mandatory confirmation. Typing and scrolling are reported separately in device gates. Safety confirmations remain even when they add taps. Slice acceptance must not regress the proposed primary path by more than one tap without a documented comprehension or safety benefit.

| Job | Current primary path | Current | Proposed path | Proposed |
| --- | --- | ---: | --- | ---: |
| Record Morning milk | Home -> Production -> product -> session -> qty -> save | 5-6 | Today production card -> Morning -> qty -> save | 4 |
| Record Evening milk | Home -> Production -> product -> session -> qty -> save | 5-6 | Today production card -> Evening -> qty -> save | 4 |
| Sell milk | Home -> Sell -> customer -> product -> qty/rate/payment -> save | 6-8 | Record -> Sell -> customer/product -> qty/rate/payment -> save | 6-7; 4-5 from customer/product context |
| Sell milk for cash | Home -> Sell -> no party/product -> qty/rate/full payment -> save | 5-7 | Record -> Sell -> Cash -> product/qty/rate -> save | 6; no fake customer |
| Partial customer payment | Home -> Received -> party -> amount -> save | 4-5 | Khata -> customer -> Receive -> amount -> save | 4; Record path 4-5 |
| Check who owes money | Hisab-Kitab -> scroll/find party | 2 plus search by scrolling | Khata opens To receive, searchable | 1-2 |
| Buy feed partially on credit | Home -> Bought -> supplier/supply/qty/total/payment -> save | 7-9 | Record -> Bought -> supply/supplier -> qty/total/payment -> save | 7; 5 from supplier/supply context |
| Use feed | Home -> Used -> supply -> qty -> save | 4 | Farm Work -> Feed -> Used -> qty -> save | 4; Record path 4 |
| Pay supplier | Home -> Paid -> supplier -> amount -> save | 4-5 | Khata -> To pay -> supplier -> Pay -> amount -> save | 4-5 |
| Check remaining feed | Home -> Remaining -> locate Feed | 2-3 | Farm Work shows remaining list -> Feed | 1-2 |
| Understand today | Home -> read long mixed block | 1 plus scanning | Today grouped hierarchy | 1 |
| Understand month | Home -> Month dialog | 2 | Today -> This Month | 2; proper detail links |
| Switch farm | Menu -> Farms -> farm -> switch | 3-4 | Farm name -> farm | 2 |
| Create farm | Menu -> Farms -> Add -> form -> create | 5+ | Farm name -> Manage -> Add -> form -> create | 5+ |
| Backup farm | Menu -> Backup/Restore -> export | 3 | More -> Data & Backup -> Backup | 3 |
| Backdate an entry | Open action -> change date/time -> choose date/time -> save | +3-4 over action | Same visible change-date row in each task | +3-4 over action |
| Correct a supported mistake | Find authority-specific history -> open -> edit/delete | varies | Activity/owning detail -> supported edit/delete | measured per source; product-sale qty/rate is read-only |

The redesign primarily improves predictability and contextual preselection, not raw tap minimization. A safe purchase or delete must remain explicit.

## 17. Key text wireframes

Home, action entry, Production, Khata, Supplies, and Month are specified above.

### 17.1 Farms

```text
फार्महरू                                      [+ नयाँ फार्म]

RC01 Upgrade Farm
सक्रिय · NPR                                  [खोल्नुहोस्]
----------------------------------------------------
Hill Plot
NPR                                           [सक्रिय बनाउनुहोस्]

फार्म मेटाउन वा डेटा रिसेट गर्न फार्म खोल्नुहोस्।
```

```text
RC01 Upgrade Farm                              [सम्पादन]

मुद्रा                         NPR
ब्याकअप                        १६ अगस्ट २०२६

[ ब्याकअप गर्नुहोस् ]

डेटा नियन्त्रण
[ डेटा रिसेट ]
[ फार्म मेटाउनुहोस् ]
```

### 17.2 Settings

```text
सेटिङहरू

भाषा र देखावट
नेपाली · अक्षर २७ · सिस्टम मोड                 [>]

सूचना
अपडेट र सम्झना                                [>]

डेटा र ब्याकअप
ब्याकअप, पुनःस्थापना                           [>]

एपबारे                                         [>]
```

### 17.3 Today overview detail

```text
आजको विवरण

फार्मको काम                                    [उत्पादन हेर्नुहोस्]
उत्पादन Milk ६९ लि.
बेचेको ५७ · प्रयोग ११ · नखुलेको १

पैसा                                           [गतिविधि हेर्नुहोस्]
बिक्री रु ५,७००
पैसा आएको रु ४,०००
खर्च रु ०
आजको बिक्रीमा उधार रु २,७००

अहिलेको खाता                                  [खाता हेर्नुहोस्]
लिन बाँकी रु १,७००
तिर्न बाँकी रु १५,०००

सामान                                          [सामान हेर्नुहोस्]
Feed १७ बोरा
```

### 17.4 Farm Work and More landing screens

```text
फार्मको काम

उत्पादन
आज दूध ६९ लि. · नखुलेको १ लि.                 [हेर्नुहोस्]
[ उत्पादन लेख्नुहोस् ]

सामान
दाना १७ बोरा · औषधि ३ bottle                  [सबै सामान]
[ किनेँ ]  [ प्रयोग गरेँ ]
```

```text
अरू

यो महिना र गतिविधि                              [>]
किसान औजार                                     [>]
अन्य आम्दानी/खर्च                               [>]
पुराना फार्म अभिलेख                             [>]
फार्महरू                                        [>]
सेटिङहरू                                        [>]
डेटा र ब्याकअप                                  [>]
```

### 17.5 Payment and supply-use contracts

Customer and supplier payments show direction, current balance, entered amount, resulting balance, and date/time. Overpayment remains blocked. Supplier payment never mentions or changes stock.

```text
रामबाट पैसा पाएँ
अहिले लिन बाँकी                 रु २,७००
पाएको रकम                       [ रु 1,000 ]
पाएपछि बाँकी                    रु १,७००
मिति र समय                      आज, अहिले [बदल्नुहोस्]
[ सुरक्षित गर्नुहोस् ]
```

Supply use shows object, current quantity, amount used, resulting quantity, and date/time. It creates no financial fact.

```text
दाना प्रयोग गरेँ
अहिले बाँकी                     २० बोरा
प्रयोग गरेको                    [ 3 ] बोरा
प्रयोगपछि बाँकी                 १७ बोरा
मिति र समय                      आज, अहिले [बदल्नुहोस्]
[ सुरक्षित गर्नुहोस् ]
```

### 17.6 No-farm and release states

```text
Kisab

सुरु गर्न फार्म छान्नुहोस् वा नयाँ बनाउनुहोस्।
[ फार्म छान्नुहोस् ]
[ नयाँ फार्म बनाउनुहोस् ]
```

Update and private-build expiry use a shell banner with one clear action and accessible status. They do not replace Today content. Expired-build behavior remains governed by the existing release policy.

## 18. Visual design principles

### 18.1 Direction

Trustworthy, calm, practical, and distinctly agricultural without decorative farm imagery. The visual character comes from clear farm-day structure, excellent Nepali typography, semantic quantities, and restrained green brand cues. It must not resemble a generic blue finance dashboard or a card-filled SaaS app.

### 18.2 Typography

- Use a Devanagari-capable Android font with tested weight coverage; compare Noto Sans Devanagari/system support before adoption.
- Establish roles, not fixed decoration: destination title, section title, headline amount/quantity, body, supporting metadata, button label.
- Monetary digits use tabular figures where the font supports them; align amount and label in rows without right-edge clipping.
- Keep letter spacing at 0. Do not shrink text to force one line.
- At 36sp, preserve role contrast using weight and spacing rather than disproportionate heading scaling.

### 18.3 Spacing and rhythm

- Base spacing: 4dp micro, 8dp related, 16dp component, 24dp section, 32dp major transition.
- Minimum touch target: 48dp; primary task targets prefer 56dp.
- Stable row heights are not required at 36sp; minimum height and content wrapping are.
- Use dividers or whitespace for lists; do not put every row inside a card.

### 18.4 Cards and surfaces

- One prominent status surface per destination is acceptable.
- Use cards for grouped, actionable summaries or repeated entity items only.
- Use flat section bands/lists for activity and settings.
- Never nest cards. Avoid gradient surfaces and ornamental decoration.

### 18.5 Buttons and icons

- One filled primary action per screen/form.
- Tonal/outlined secondary actions; text actions for low emphasis.
- Familiar Material/lucide-equivalent Android icons paired with Nepali labels for navigation and Record tasks.
- Color never carries direction alone; icon, text, and sign must agree.

### 18.6 Semantic color

- Brand green: navigation selection and primary action, not every container.
- Receivable: distinct positive-attention tone with `लिन बाँकी`; do not imply profit.
- Payable/expense: warm warning tone with `तिर्न बाँकी`; reserve red for errors/destructive action, not ordinary debt.
- Production: green/teal family; supplies: neutral/earth accent. These are category cues, not separate branded themes.
- Dark mode uses semantic tokens with independently verified contrast.

### 18.7 Numeric presentation

- Money always includes currency context at first occurrence and preserves existing locale/grouping behavior.
- Quantity always includes the object and unit: `दाना १७ बोरा`, never `बाँकी १७`.
- Distinguish flow (`आज पैसा आएको`) from position (`अहिले लिन बाँकी`) in labels.
- Negative reconciliation uses explicit “बढी देखियो” / inconsistency language, not only a minus sign.

### 18.8 Empty states and confirmations

- Empty state names the next farmer action: `आज उत्पादन लेखिएको छैन` + `उत्पादन लेख्नुहोस्`.
- Do not explain the whole feature in visible instructional prose.
- Successful entry shows visible arithmetic and next balance, then returns to the owning context.
- Reset/delete preserve existing backup gate and typed confirmation.

## 19. Component inventory

| Component | Purpose | Variants |
| --- | --- | --- |
| App shell | Stable farm and navigation context | Normal, 36sp, no-farm |
| Farm switcher | Show/switch active farm | Compact dropdown, management list |
| Destination header | Title and optional period/filter | Today, Khata, Farm Work, More child |
| Headline status | One most important current fact | Receivable, payable, unexplained, empty |
| Metric row | Label/value with optional detail link | Money flow, current balance, quantity |
| Directional balance | Prevent receivable/payable ambiguity | To receive, to pay, zero |
| Action tile | Choose a Record task | Farm, money, other; icon + label |
| Section header | Group content and one contextual action | Title, supporting line, See all |
| Party row | Find a person/business and act | Receivable, payable, BOTH, settled |
| Product reconciliation | Explain produced/sold/allocated/unexplained | Normal, mismatch, negative |
| Supply row | Show remaining and initiate use | Available, zero, historical |
| Activity row | Shared visual grammar for distinct facts | Sale, purchase, settlement, production, allocation, supply use, manual cash |
| Money amount | Locale/currency-safe display | Flow, directional balance, negative/error |
| Quantity amount | Exact decimal + unit | Production, supply, reconciliation |
| Task form scaffold | Full-screen dependent input flow | Sale, purchase, production, allocation |
| Arithmetic summary | Show before/after consequence | Sale total, payment balance, purchase payable |
| Empty state | Direct next action | No production, no Khata, no supplies, no activity |
| Confirmation sheet | Confirm consequential action | Save summary, discard, reset/delete |
| Filter control | Period/direction/product choice | Segmented control, menu, searchable selector |

This is a practical inventory, not a separate design-system project.

## 20. Accessibility, 36sp, and Dark requirements

Gate C proves reachability, not a permanent exemption from regression testing.

- Every slice must pass normal, 36sp, Light, and Dark on the physical Moto.
- At least one 36sp Dark pass per destination must use populated Nepali data.
- Bottom-navigation labels may wrap to two lines or switch to approved shorter tested terms; they must not shrink below the selected app scale.
- Central Record remains at least 56dp and has a spoken label/state.
- Sheets that cannot show content and actions at 36sp become full-screen flows.
- Focus order follows visual order; dialogs/forms expose labels programmatically, not hints alone.
- TalkBack announces amount direction (`लिन बाँकी`, `तिर्न बाँकी`) and units.
- Selected/unselected navigation, filter, spinner, and radio states remain visible without color alone.
- Dynamic updates, such as new reconciliation or balance, announce a concise result.
- Keyboard never hides the only save action; use scrollable form scaffolds and appropriate IME actions.
- Large amounts and long Nepali labels wrap without overlapping adjacent values.
- Dark-mode contrast is measured for text, controls, dividers, disabled states, and semantic attention surfaces.
- Motion is restrained and respects reduced-motion settings where available.

## 21. Incremental implementation slices

Each slice is a rollback boundary and must preserve schema 12 unless a separately approved domain milestone says otherwise.

### UX-00: terminology and measurement baseline

- **Scope:** prototype/test top-level labels, Record label, Farm Work label, Khata comprehension; capture current tap/time baseline.
- **Likely files later:** strings only after approval; research artifacts/docs first.
- **Untouched:** all domain and persistence.
- **Tests:** localization parity and string-resource checks.
- **Device gate:** five farmer-language comprehension sessions or supervised proxy sessions; 36sp nav label fit.
- **Rollback:** independent terminology commit.

### UX-01: shell and navigation ownership

- **Scope:** new bottom shell, farm switcher, destination containers, More list; route existing surfaces without redesigning their internals.
- **Likely files:** `FarmActivity.kt`, `activity_shell.xml`, navigation drawables/colors, shell menu, strings; consider extracting shell controller/state.
- **Untouched:** services, validators, codecs, money/quantity logic.
- **Tests:** destination restore/back/discard, farm switching, settings deep links.
- **Device gate:** all destinations, rotation, 36sp, Dark, multi-farm, no-farm.
- **Rollback:** shell commit; old destinations remain available until gate passes.

### UX-02: Today hierarchy

- **Scope:** replace Home button wall/text block with grouped Today read model and contextual links; move editors/history without deleting them.
- **Likely files:** Home portion of layout, `renderFarm`, `renderFarmerOverview`, presentation mappers/components.
- **Untouched:** `FarmerOverview` metric definitions and accounting authorities; Month remains current-month only and adds no credit-created metric.
- **Tests:** exact metric mapping, empty/populated states, current-vs-period labels.
- **Device gate:** Gate C facts reproduced at normal/36sp Light/Dark; links reach correct detail.
- **Rollback:** Today-only presentation commit.

### UX-03: Record action and task-form scaffold

- **Scope:** central Record sheet, full-screen form scaffold, migrate Production and Sell first; no behavior change.
- **Likely files:** shell, new task layouts/classes, extracted dialog presentation from `FarmActivity.kt`, strings.
- **Untouched:** `addProductSale`, production upsert, settlement allocation.
- **Tests:** form state, validation, arithmetic summary, cancel/discard, existing domain suites.
- **Device gate:** Morning/Evening production and partial sale at 36sp Dark with keyboard.
- **Rollback:** per-flow migration commits; old invocation can temporarily route to same service but must not remain as duplicate final UI.

### UX-04: Khata

- **Scope:** directional totals, searchable/filterable party list, party detail, contextual receive/pay/sell/buy, period filter.
- **Likely files:** party/ledger row layouts, Khata containers/renderers, settlement/trade entry presentation, strings.
- **Untouched:** `PartyLedger`, `Trade`, `Settlement`, oldest-first rules, BOTH separation.
- **Tests:** direction filters, zero/BOTH parties, history ordering, payment before/after.
- **Device gate:** customer partial payment and supplier later payment, persistence, 36sp Dark.
- **Rollback:** Khata destination commit.

### UX-05: Farm Work - Production

- **Scope:** Production day/product screen, session rows, labeled allocation, per-type totals, immediate reconciliation refresh.
- **Likely files:** production/allocation presentation and layouts, FarmActivity extraction, strings.
- **Untouched:** ProductionRecord/Allocation models, exact-unit checks, financial neutrality.
- **Tests:** session upsert, totals mapping, mismatch/negative/over-allocation states.
- **Device gate:** repeat Gate A exactly, including relaunch and 36sp Dark.
- **Rollback:** Production screen commit.

### UX-06: Farm Work - Supplies

- **Scope:** remaining-first list, detail/movement history, contextual Bought/Used; integrate supplier link without merging payment and quantity.
- **Likely files:** supply presentation/layouts, purchase/payment form presentation, row components.
- **Untouched:** `SupplyPurchaseDetail`, `SupplyUsage`, PURCHASE Trade authority, schema-12 codec.
- **Tests:** purchased-used derivation, no-double-counting, payment/stock isolation, decode regression.
- **Device gate:** repeat Gate B exactly, including cold relaunch and cleanup.
- **Rollback:** Supplies screen commit.

### UX-07: Month, Activity, and advanced records

- **Scope:** current-Month screen, authority-linked details, unified visual activity grammar, relocate generic income/expense, existing crop/livestock entries, and calculators under More. Persisted payment Activity remains one row per Settlement allocation.
- **Likely files:** overview/activity layouts and presentation, current Hisab sections, transaction/trade rows.
- **Untouched:** metric definitions; no Profit calculation.
- **Tests:** period boundary/timezone, row routing, type filters, split-payment settlement rows, and no false grouping or duplicate authority.
- **Device gate:** populated current/month boundary farm at 36sp Light/Dark.
- **Rollback:** review-surfaces commit.

### UX-08: Farms, Settings, and consistency

- **Scope:** farm switcher polish, Farms/Farm Details sections, Settings child pages, final visual/component consistency and accessibility pass.
- **Likely files:** farm/settings layouts/renderers, themes/colors/drawables, text scaling/accessibility helpers.
- **Untouched:** backup/reset/delete safeguards and persistence.
- **Tests:** farm management, backup flows, preference persistence, localization.
- **Device gate:** multi-farm, backup/reset/delete dry run on disposable farm, TalkBack, 36sp, Dark.
- **Rollback:** administrative polish commit.

### Architecture note

`FarmActivity` is already a major implementation risk. UX-01 should establish an extraction strategy: destination-specific controller/presenter classes or fragments/views with explicit state, while keeping domain services unchanged. Do not combine shell redesign with a wholesale framework rewrite. Extract one vertical slice at a time and retain executable behavior gates.

## 22. Validation strategy

- Use focused vertical-slice prompts and device gates, not one giant automated scenario.
- Maintain a deterministic disposable farm per gate and protect `RC01UpgradeFarm`.
- Record expected versus actual UI values; do not use persisted-file injection to make a UI gate pass.
- Derive automation taps from live resource nodes and bounds; never treat coordinate failure as an app defect without evidence.
- Run directly relevant domain, presentation, localization, persistence, and codec tests after every slice.
- Add screenshot baselines for normal and 36sp Light/Dark, but do not use screenshots alone to prove interaction.
- Test English and Nepali; Nepali is the primary acceptance language.
- Include TalkBack/focus order in UX gates, which Gate C did not fully establish.
- Verify no duplicate authority after any entry redesign: one user fact produces the same domain mutations as before.
- Clean up disposable farms through normal UI and verify the protected farm.
- Pilot only after UX-00 terminology decisions and UX-01/02/03/04 core paths pass supervised real-farmer observation.

## 23. Risks and mitigations

| Risk | Mitigation |
| --- | --- |
| A prettier UI destroys familiar workflows | Preserve farmer verbs and service behavior; migrate one vertical slice; observe farmers before retiring old presentation. |
| Simplification hides accounting distinctions | Show visible before/after arithmetic; maintain separate sale/purchase/payment labels and directional balances. |
| Home becomes overloaded again | Enforce Today content contract; one headline, four groups, maximum three recent events, no generic editor. |
| Navigation becomes too deep | Persistent Record gives two-tap global entry; contextual actions preselect known party/product/supply. |
| Nepali labels exceed navigation/components | UX-00 farmer test; allow wrapping/full-screen sheets; no text shrinking. |
| 36sp and Dark regress | Physical gate every slice using populated Nepali data; semantic tokens and responsive stacking. |
| Dialog overload survives under new styling | Migrate dependent tasks to full-screen forms; no nested create dialogs. |
| Old and new UI coexist indefinitely | Define retirement in each slice and remove old entry point after parity gate, never maintain two final paths. |
| `FarmActivity` grows further | Extract destination/task presentation incrementally with explicit ownership; avoid domain refactor. |
| UI refactor changes domain behavior | Snapshot service calls and mutations; run authority-specific regression suites and exact device scenarios. |
| Cash sale or supplier purchase chooses the wrong authority | Encode explicit UI branches and service-call parity tests; cash sale has no party, no-supplier supply purchase uses one EXPENSE source, and any supplier-linked purchase uses one PURCHASE Trade source regardless of opening payment status. |
| Activity falsely groups settlement allocations | Persist one row per Settlement; group only transient service results until a schema-backed group exists. |
| Competitor imitation displaces farmer needs | Use competitor evidence only for quality principles; approve every Kisab behavior against farmer jobs and domain capability. |
| Directional Khata gets netted for simplicity | Separate filters/headlines; BOTH party can appear in both directions; no default net total. |
| “Farm Work” implies unsupported inventory | Explicitly label production reconciliation and supply quantities; no valuation, recipes, or forecasts. |
| Search/filter performance or complexity expands scope | Start local and farm-scoped over existing lists; no backend or indexing architecture milestone. |

## 24. Explicit non-goals

- No backend, sync, cloud, Firebase, account/login expansion, premium, subscriptions, or ads.
- No schema or authority change in this redesign specification.
- No true inventory valuation, batches, warehouse model, unit conversion, low-stock automation, recipes, transformations, or yields.
- No livestock records or animal management.
- No forecasting, reports/charts program, tax, invoices, or profit metric.
- No recurring entries, voice entry, image capture, sharing, or reminders beyond validated current capability.
- No customer advances or unapplied credit model.
- No product-sale quantity/rate correction until an atomic Trade + ProductSaleDetail operation is separately approved.
- No multi-trade payment model beyond existing service behavior.
- No multi-farm consolidated reporting.
- No Karobar layout, branding, wording, iconography, or feature imitation.
- No implementation until product architecture and terminology decisions are approved.

## 25. Decisions requiring product-owner approval

1. **Top-level shell:** approve `Today | Khata | Record | Farm Work | More`, including Record as an action rather than destination.
2. **Nepali destination labels:** approve farmer testing of `आज`, `खाता`, `लेख्नुहोस्`, `फार्मको काम`, and `अरू`; choose alternatives after evidence, not preference alone.
3. **Today headline policy:** approve directional/unresolved priority instead of the current generic farm balance headline.
4. **Khata model:** approve one destination with separate To receive/To pay filters and BOTH parties appearing directionally, never silently netted.
5. **Action architecture:** approve full-screen dependent flows and retirement of nested dialogs after parity gates.
6. **Farm Work ownership:** approve Production and Supplies as one top-level destination with separate sections, without implying true inventory.
7. **Month ownership:** approve a proper Month screen linked from Today and More, replacing the read-only dialog.
8. **Hisab retirement:** approve moving calculators to Farmer Tools and party period review to Khata, retiring both current bookkeeping labels from bottom navigation.
9. **Advanced records:** approve moving manual income/expense and generic trade/transaction review under Record > Other money and More > Advanced records.
10. **Legacy records and release states:** approve preservation ownership for existing crop/livestock entries, account status, update flow, notification routing, and private-build expiry as specified.
11. **Purchase authority branch:** approve the explicit no-supplier cash EXPENSE-backed versus supplier-linked PURCHASE Trade-backed Bought flow, including fully paid supplier purchases in the latter.
12. **Cash sale:** approve explicit cash/full-payment versus customer credit branches.
13. **Real-farmer research gate:** approve UX-00 terminology/comprehension testing before Android implementation begins.

Once these decisions are approved, implementation should begin with UX-00 and UX-01 only. No later slice should be bundled into the navigation-shell change.