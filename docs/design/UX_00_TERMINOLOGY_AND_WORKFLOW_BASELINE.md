# UX-00 Terminology and Workflow Baseline

## 1. Baseline

| Item | Value |
| --- | --- |
| Repository | `lazydeepak/kisab` |
| Canonical worktree/branch | `design/competitive-farmer-ux-redesign` |
| Approved design SHA | `9a4a8fbe4e30652c26419fcaeaa09182ae5a2abd` |
| Design authority | `docs/design/COMPETITIVE_FARMER_UX_REDESIGN.md` |
| Current branch status | clean after probe cleanup; A–J physical evidence committed in UX-00 closeout |
| Physical Moto reference | Moto Edge 60 Fusion, serial `ZA22374XPC` |
| Current session physical device | Moto Edge 60 Fusion, Android 16, 1220x2712 @ 450 dpi |
| Installed app version (physical) | 0.2.0 (versionCode 3) |

The redesign spec exists at the approved SHA and matches the requested architecture: `Today | Khata | Record | Farm Work | More`, with `Record` as a central action, not a destination.

## 2. Scope / non-goals

### Scope
- farmer-facing terminology only;
- current workflow/tap baseline only;
- navigation-label viability at large text;
- implementation-ready UX-01 terminology recommendations;
- supervised farmer-test script preparation.

### Non-goals
- no new navigation shell;
- no Today redesign;
- no UX-01 implementation;
- no domain, persistence, schema, accounting, supply, release, or infra changes;
- no fake farmer validation;
- no new competitor UI claims.

## 3. Terminology inventory

### Already validated farmer verbs
- बेचेँ
- किनेँ
- प्रयोग गरेँ
- पैसा पाएँ
- पैसा तिरेँ
- उत्पादन

### Directional distinction to preserve
- लिन बाँकी
- तिर्न बाँकी

### Current top-level and home labels
- current shell nav: `Home`, `Hisab-Kitab`, `Hisab`
- shell overflow: `Settings`, `Backup`, `Farms`, `About`
- home actions: `Sell` / `बेचेँ`, `Received money` / `पैसा पाएँ`, `Bought` / `किनेँ`, `Used` / `प्रयोग गरेँ`, `Remaining` / `बाँकी`, `Paid money` / `पैसा तिरेँ`, `Production` / `उत्पादन`, `Other entries`
- current home summary label: `Today` / `आज`

## 4. Candidate evaluation matrix

Scores: 1 = weak, 5 = strong.

| Surface | Candidate | Comprehension | Farmer language | Ambiguity | Action/destination clarity | Length | 36sp viability | TalkBack clarity | Decision |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| Today | आज | 5 | 5 | 5 | 5 | 5 | 5 | 5 | RECOMMENDED FOR UX-01 PROVISIONAL USE |
| Khata | खाता | 5 | 4 | 4 | 5 | 5 | 5 | 4 | RECOMMENDED FOR UX-01 PROVISIONAL USE |
| Record | लेख्नुहोस् | 4 | 5 | 4 | 5 | 3 | 3 | 4 | PENDING FARMER VALIDATION |
| Record | थप्नुहोस् | 3 | 4 | 3 | 4 | 4 | 4 | 4 | REJECTED as primary term |
| Farm Work | फार्मको काम | 4 | 4 | 4 | 5 | 2 | 3 | 4 | PENDING FARMER VALIDATION |
| Farm Work | फार्म काम | 3 | 4 | 3 | 4 | 3 | 4 | 4 | fallback only |
| Farm Work | काम | 2 | 5 | 1 | 3 | 5 | 5 | 3 | too broad |
| More | अरू | 5 | 5 | 4 | 5 | 5 | 5 | 5 | RECOMMENDED FOR UX-01 PROVISIONAL USE |

## 5. 36sp / device findings

### Current implementation facts
- App text size is user-controlled in settings, from `14sp` to `36sp` (`AppTextSize.MIN_SP` / `MAX_SP`).
- The activity rescales all `TextView` content proportionally from the selected base size.
- Popup-menu labels are also rescaled.
- The shell bottom nav items are fixed at one line (`maxLines=1`, `ellipsize=end`) and use `12sp` base text.
- The shell bottom nav has only three destinations today; the requested five-part shell is not implemented yet.

### Practical implication
- Short labels are safest at large text.
- Long labels need either wrapping or a wider control.
- `आज`, `खाता`, and `अरू` are the safest candidates.
- `लेख्नुहोस्` is semantically strong but should not be forced into a narrow one-line tab.
- `फार्मको काम` is borderline and likely needs a two-line or wider affordance.

### Device note
Fresh physical Moto measurement was completed in this session on `ZA22374XPC`.

### Physical-fit probe results

| Surface | Candidate | Normal text | 36sp text | Width / height | Result |
| --- | --- | --- | --- | --- | --- |
| Today | आज | 1 line | 1 line | 90.7dp / 64.0dp | safe |
| Khata | खाता | 1 line | 1 line | 90.7dp / 64.0dp | safe |
| Record | लेख्नुहोस् | 1 line | 1 line | 185.2dp / 71.8dp | safe, semantically pending |
| Record | थप्नुहोस् | 1 line | 1 line | 185.2dp / 71.8dp | safe, weaker meaning |
| Farm Work | फार्मको काम | 1 line | 3 lines | 90.7dp / 96.7dp | fits, but crowded at 36sp |
| More | अरू | 1 line | 1 line | 90.7dp / 64.0dp | safe |
| Directional | लिन बाँकी | 1 line | 1 line | 185.2dp / 64.0dp | safe |
| Directional | तिर्न बाँकी | 1 line | 1 line | 185.2dp / 64.0dp | safe |

The measured labels had no ellipsis on the live Moto. `फार्मको काम` is the only candidate that materially expands at 36sp, but it remains readable.

## 6. Current workflow measurement table

Current-state baseline for the live app on the Moto. The routes are the existing pre-redesign flows; none of the workflows were modified in this pass.

| ID | Workflow | Start location | Current route / taps | Dialogs / screens | Repeated selections / nesting | Scroll / keyboard friction | Context loss | Terminology hesitation | Final destination | Time |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| A | Record production | Home | tap `Production` -> choose product -> enter quantity -> choose session -> save | 1 alert dialog; optional product-create dialog if empty | product may be reselected; session radio always shown | dense dialog; keyboard can crowd save on large text | none on save | low | Home | qualitative, ~15-30s |
| B | Sell product with customer and partial payment | Home | tap `Sell` -> choose customer -> choose product -> enter quantity/rate -> choose payment state -> maybe partial amount -> save | 1 alert dialog; optional create-customer/create-product dialogs | customer/product preselection repeats on return; nested add-customer/product can recurse | highest keyboard pressure of the common flows | possible if add-customer/product path opens | medium around `Sell` vs `Bought` and payment state | Home + post-save confirmation | qualitative, ~30-60s |
| C | Receive customer payment | Home | tap `Received money` -> choose customer -> enter amount -> save | 1 alert dialog | customer selection repeated if more than one receivable party | moderate keyboard pressure | none | low | Home | qualitative, <20s |
| D | Buy supply from supplier with partial payment | Home | tap `Bought` -> choose supplier -> choose supply -> enter quantity/cost -> choose payment state -> maybe partial amount -> save | 1 alert dialog; optional supply/supplier-create dialogs | supplier/supply selection repeats if user backs out | dense dialog; keyboard can hide the payment controls | possible if create-supply/supplier path opens | medium around `Bought`, `supplier payment`, and supply units | Home | qualitative, ~30-60s |
| E | Record supply usage | Home | tap `Used` -> choose supply -> enter quantity -> optional note -> save | 1 alert dialog | supply selection repeated on retry | moderate | none | low | Home | qualitative, <20s |
| F | Pay supplier | Home | tap `Paid money` -> choose supplier -> enter amount or use full-amount button -> save | 1 alert dialog | supplier selection repeated; full-amount helper is a second interaction | moderate | none | low | Home | qualitative, <20s |
| G | Find a customer with receivable and inspect Khata | Home -> Hisab-Kitab | tap `Hisab-Kitab` -> scroll/select party row -> open Khata | list screen + party Khata panel | parties must be scanned visually; no search in current view | list scroll likely; no keyboard unless editing from Khata | context shifts from ledger list to party detail | medium because `Hisab-Kitab` does not clearly say who owes what | Khata panel inside Hisab-Kitab | qualitative, ~20-40s |
| H | Find remaining supply quantity | Home | tap `Remaining` -> read stock dialog | 1 alert dialog | none | low | none | low | dialog closes back to Home | qualitative, <10s |
| I | Switch farm | overflow menu -> Farms | tap menu button -> `Farms` -> open farm details/list -> switch | menu + farms screen + farm details screen | one extra detour through management surface | some scrolling if farm list is long | context re-entered from management flow | medium; current wording is admin-first | Farms/Farm Details/Home | qualitative, ~20-45s |
| J | Record general expense | Home | tap `Other entries` -> expand -> tap `Record expense` -> fill editor -> save | disclosure + transaction editor | one extra disclosure tap; editing is hidden behind generic label | keyboard can crowd date/save at large text | none | high because `Other entries` is vague | Home | qualitative, ~20-30s |

## 6.1 Physical A–E workflow execution evidence (Moto ZA22374XPC, 2026-08-17)

Executed live on the Moto through the current app UI in the English locale, on the disposable `UX00PhysicalAE` farm (NPR), closed earlier. These facts are the established baseline and must not be redefined.

| ID | Workflow | Physical result | Disposition |
| --- | --- | --- | --- |
| A | Record milk production 10 L | Recorded production of `Milk` 10 L on the disposable farm. Farmer Overview showed production 10 L / litre. | PHYSICAL_PASS |
| B | Sell milk 5 L × रु 100 = रु 500, partial | Sold 5 L milk at रु 100/L = रु 500; customer received रु 200, receivable रु 300. | PHYSICAL_PASS |
| C | Receive customer payment रु 100 | Received रु 100 toward the receivable; receivable changed रु 300 → रु 200. | PHYSICAL_PASS |
| D | Buy feed 10 KG, रु 10,000, partial | Purchased Feed 10 KG for रु 10,000; paid रु 4,000, payable रु 6,000. | PHYSICAL_PASS |
| E | Record feed usage 2 KG | Used Feed 2 KG; stock changed 10 KG → 8 KG. | PHYSICAL_PASS |

## 6.2 Physical F–J workflow execution evidence (Moto ZA22374XPC, 2026-08-17)

Executed live on the Moto through the current app UI in the English locale. A disposable `UX00PhysicalFJ` farm (NPR) was created for this pass and deleted through the normal UI deletion flow afterward. `RC01UpgradeFarm` was never mutated and was re-verified after cleanup. Workflows A–E were already closed and were not changed; this pass did not redefine A–E.

| ID | Workflow | Physical result | Disposition |
| --- | --- | --- | --- |
| F | Record Medicine expense रु 500 | Opened Home → `SHOW OTHER ENTRIES` → `RECORD EXPENSE`, entered amount `500` and description `Medicine` (Expense radio pre-selected), saved. Home persisted and displayed `Expenses: रु 500.00` and recent transaction `- Aug 17, 2026, 21:01:33 \| Expense \| Feed \| Medicine \| रु 500.00`. The legacy expense editor is reachable only behind the generic `Other entries` disclosure. | PHYSICAL_PASS |
| G | Milk sale to FJCustomer 5 L × रु 100 = रु 500, partial रु 200 | Created customer `FJCustomer` and product `Milk` (L / litre) via the nested dialogs in the Sell flow. Entered quantity `5`, rate `100`; dialog computed `Total: रु 500.00`. Selected `Some money received` and entered `200`; summary showed `Received now: रु 200.00, Remaining: रु 300.00`. Saved. Home then displayed `Sales: रु 500.00, Money received: रु 200.00, To receive: रु 300.00, Credit sales today: रु 300.00`. The sale dialog shows only Quantity, Rate per unit, and payment choices — **no fat %, SNF %, or animal fields appear on-device**. | PHYSICAL_PASS |
| H | Dung sale to FJCustomer 5 × रु 50 = रु 250 | Created product `Dung`; its unit spinner offered `L / litre`, `kg / kilogram`, `piece`, `bag`, `packet`, `bottle` (selected `piece`). Selected Dung in the product chooser, entered quantity `5`, rate `50` → `Total: रु 250.00`, chose `Credit` (unpaid). Saved; Home displayed `Sales: रु 750.00, To receive: रु 550.00`. Fields differ from Milk only in unit label (`piece` vs `L / litre`); the form structure is identical. | PHYSICAL_PASS |
| I | Ledger / Khata view for FJCustomer | Navigated via bottom-nav `Hisab-Kitab` → financial overview (`Receivable: रु 550.00`) → scrolled to `Parties` → `FJCustomer — Customer` → opened Khata panel. Panel showed `To receive: रु 550.00`, `To pay: रु 0.00`, `Net position: You should receive रु 550.00`, with chronological entries: Milk sale रु 500 (After रु 500), payment received रु 200 (After रु 300), Dung sale रु 250 (After रु 550). Navigation labels on-device are `Hisab-Kitab`, `Khata`, `Parties` (English); **`खाता` does not appear in this locale**. | PHYSICAL_PASS |
| J | Settle रु 100 of the displayed outstanding balance | Outstanding balance physically displayed: Home `To receive: रु 550.00` and Received-money dialog `Outstanding: रु 550.00` (BEFORE). Entered amount `100`, saved payment. Home then displayed `Money received: रु 300.00, To receive: रु 450.00` (AFTER); Hisab-Kitab recent-sale line for Milk changed to `रु 200.00 due`; FJCustomer Khata showed newest entry `Payment received — रु 100.00, After: You should receive रु 450.00` and `To receive: रु 450.00`. | PHYSICAL_PASS |

No Hindi labels were invented or observed; the device/app ran in the English locale (`en-US`). This evidence is a physical execution record and does not replace the farmer comprehension sessions required by UX-00's device gate.

## 6.3 Device cleanup

- `UX00PhysicalFJ` deleted through the normal UI safeguards: Farm details → `DELETE FARM` → delete-warning dialog → backup gate (`I ALREADY HAVE A BACKUP`) → typed `DELETE` confirmation. `PASS`.
- Post-delete Farms screen lists only `RC01UpgradeFarm` (marked `Active`, NPR). `UX00PhysicalFJ` absent. `PASS`.
- `RC01UpgradeFarm` re-verified on Home: `To receive: रु 15,000.00`, `To pay: रु 0.00`, and its three baseline transactions (Irrigation equipment रु 1,250,000; Goat sale रु 750; Harvest labor रु 250) intact. No leakage from the disposable farm. `PASS`.

## 6.4 UX-00 disposition after A–J

- A, B, C, D, E all `PHYSICAL_PASS` (closed earlier, recorded in section 6.1).
- F, G, H, I, J all `PHYSICAL_PASS`.
- Cleanup `PASS` (`UX00PhysicalFJ` absent; `RC01UpgradeFarm` intact).
- UX-00 current-state physical execution: **DONE** for the A–J workflow evidence.
- UX-01 readiness remains gated on the provisional terminology (section 9) and the real-farmer comprehension sessions; this pass does not change that.

## 7. Major cognitive-friction findings

1. Top-level labels still encode internal modules, not farmer jobs.
2. `Other entries` hides a common expense path behind a generic abstraction.
3. Production, sale, purchase, and payment are all dense modal forms.
4. The same farm facts are spread across Home, Hisab-Kitab, Khata, and the month summary.
5. Khata is useful, but the current name `Hisab-Kitab` makes the destination harder to predict.
6. `Farm Work` is not yet a current surface, but the spec’s risk is real: the label must not imply inventory or admin.
7. Large text is handled by scaling, not font reduction; wide labels therefore need explicit geometry support.

## 8. Real-farmer comprehension test script

Keep it short. Show the label and ask: “What do you expect will open here?”

### Prompts
- आज
- खाता
- लेख्नुहोस्
- फार्मको काम
- अरू

### Directional terms
- लिन बाँकी
- तिर्न बाँकी

### Pass criteria
- `आज` -> daily/current-day overview
- `खाता` -> party/farmer ledger, who owes whom
- `लेख्नुहोस्` -> record something that happened, not “database record”
- `फार्मको काम` -> production/supply work, not generic farm admin or inventory system
- `अरू` -> more options / other actions
- `लिन बाँकी` -> money to receive
- `तिर्न बाँकी` -> money to pay

### Confusion signals
- says “bank account” for `खाता`
- says “add something” for `लेख्नुहोस्`
- says “store/inventory/barn” for `फार्मको काम`
- says “settings only” for `अरू`
- mixes up `लिन बाँकी` and `तिर्न बाँकी`

## 9. Recommended terminology for UX-01

| Surface | Recommended term | Status |
| --- | --- | --- |
| Today | आज | RECOMMENDED FOR UX-01 PROVISIONAL USE |
| Khata | खाता | RECOMMENDED FOR UX-01 PROVISIONAL USE |
| Record | लेख्नुहोस् | PENDING FARMER VALIDATION |
| Farm Work | फार्मको काम | PENDING FARMER VALIDATION |
| More | अरू | RECOMMENDED FOR UX-01 PROVISIONAL USE |
| Directional receivable | लिन बाँकी | RECOMMENDED FOR UX-01 PROVISIONAL USE |
| Directional payable | तिर्न बाँकी | RECOMMENDED FOR UX-01 PROVISIONAL USE |

## 10. Unresolved terminology requiring real-farmer evidence

- `लेख्नुहोस्` vs `थप्नुहोस्` for the central Record action.
- `फार्मको काम` vs any shorter/firmer variant that still means production + supplies.
- Whether `खाता` is preferred over a longer explanatory label when shown in a shell tab.
- Whether `अरू` is enough for More, or if the action needs a slightly more explicit farmer word in context.
- Whether supply/admin helper labels in deeper screens should stay formal Nepali or mix in common loanword forms.

## 11. UX-01 readiness disposition

**READY WITH PROVISIONAL TERMINOLOGY**

Reason:
- the requested Moto label-fit pass is complete;
- the workflow baseline is documented against the current app state;
- provisional terminology is clear;
- real-farmer validation is still required for the central action and farm-work labels.

Net:
- UX-00 is complete as a research baseline;
- UX-01 may proceed only with the provisional terms flagged here;
- `लेख्नुहोस्` and `फार्मको काम` remain pending farmer validation.
