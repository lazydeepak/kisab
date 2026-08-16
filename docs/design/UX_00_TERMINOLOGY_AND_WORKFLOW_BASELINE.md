# UX-00 Terminology and Workflow Baseline

## 1. Baseline

| Item | Value |
| --- | --- |
| Repository | `lazydeepak/kisab` |
| Worktree branch | `design/ux-00-terminology-baseline` |
| Approved design SHA | `9a4a8fbe4e30652c26419fcaeaa09182ae5a2abd` |
| Design authority | `docs/design/COMPETITIVE_FARMER_UX_REDESIGN.md` |
| Current branch status | clean, no code changes in this pass |
| Physical Moto reference in prior evidence | Moto Edge 60 Fusion, serial `ZA22374XPC` |
| Current session physical device | none attached in this session |

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
Fresh physical Moto measurement was not possible in this session because no adb-connected device was available. The assessment above combines current code inspection with prior physical-device evidence from the same Moto model/serial in M4-04 docs.

## 6. Current workflow measurement table

Qualitative baseline only where this session could not rerun the physical Moto.

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

**BLOCKED** for fresh physical-device verification in this session.

Reason:
- terminology recommendations are usable as provisional inputs;
- however, the requested fresh Moto label-fit pass was not rerun here because no adb-connected Moto was available.

Net:
- UX-00 research baseline is complete as a document;
- UX-01 implementation should not start from this session alone.
