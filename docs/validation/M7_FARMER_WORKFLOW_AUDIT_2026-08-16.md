# Kisab Farmer Workflow Audit — 2026-08-16

## Baseline

- Audit branch: `feature/farmer-workflow-audit`
- Baseline commit: `271b27d510df74b75a85d30f079062e783bb12a5` (completed M7.6)
- Working tree was clean before the audit.
- The supplied prompt named `a5b617d` as the required starting commit, but that commit predates M7.6. The audit branch was corrected to `271b27d` so Supplier Khata and `पैसा तिरेँ` were included.
- Farm schema: 12.

## Scenario attempted

Primary UI language was Nepali. A disposable `M76AuditFarm` was created on Moto `ZA22374XPC`; the real `RC01UpgradeFarm` was not mutated.

The audit inspected the Home surface and began the realistic farmer-day path:

- Home action labels and overview block were inspected.
- Disposable farm creation completed.
- Production, sale, received-money, Bought, Used, Remaining, Paid Money, and overview entry points were visible on Home.
- The full multi-step mutation sequence was not completed live during the original audit because the dynamically positioned Production dialog did not open reliably from coordinate automation.
- A follow-up device check on the correction branch dumped the current UI hierarchy, derived the enabled `productionButton` bounds, and tapped its current center after a clean launch. The Production dialog opened with `उत्पादन थप्नुहोस्`, `उत्पादनको नाम`, and `लि. / लिटर`, proving the earlier failure was an automation-state/coordinate issue rather than a confirmed app touch defect.
- Domain tests already cover the accounting scenario and were not treated as a substitute for the pending physical allocation evidence.

## Observations

### High — Home is overcrowded

Home exposes the core daily actions directly:

- `बेचेँ`
- `पैसा पाएँ`
- `किनेँ`
- `प्रयोग गरेँ`
- `बाँकी`
- `पैसा तिरेँ`
- `उत्पादन`

Today overview and This month remain available below the action area. Legacy Record income and Record expense are still available, but are now behind `अन्य अभिलेख देखाउनुहोस्`; the disclosure is adjacent to the hidden row and its expanded state is restored across recreation. This is a contained hierarchy correction, not a removal of the legacy accounting authority.

### Medium — Two accounting vocabularies coexist

The new farmer actions use ordinary Nepali verbs, while the existing Hisab-Kitab and legacy editors still expose module-oriented labels. This is useful for advanced access but creates a transition cost when the farmer moves between Home and Hisab-Kitab.

### Medium — Production allocation evidence remains pending

The M7.4 allocation domain tests prove reconciliation, unit guards, over-allocation rejection, and financial isolation. The full live `प्रयोग` flow was not completed on the Moto in this audit either. It remains release-validation evidence to schedule separately.

### Medium — Supplier flow was surface-checked, not fully exercised live

The M7.6 APK was installed previously and Home showed `किनेँ`, `पैसा तिरेँ`, and `तिर्न बाँकी`. The complete supplier purchase → partial payment → stock use → later supplier payment sequence remains primarily domain-test proven and should receive a dedicated connected-device pass before release.

### Low — Overview is factual but visually dense

The Today overview is compact and readable in Nepali, but it is text-heavy when multiple products, supplies, receivables, and payable figures are present. It should be evaluated again at 36sp with a populated farm before final pilot approval.

## Accounting invariants checked

- Sale value and money received remain separate.
- Settlement remains payment authority.
- PartyLedger remains customer/supplier Khata authority.
- New supplier purchases use PURCHASE Trade authority and do not add a duplicate EXPENSE.
- Supplier payment does not change purchased stock quantity.
- Supply quantity remains purchases minus usage.
- Production remains operational, not income or stock.
- Production allocations remain operational.
- FarmerOverview remains derived and non-persisted.
- Customer `लिन बाँकी` and supplier `तिर्न बाँकी` remain directionally separate.

## Corrections

A contained usability correction was applied on `feature/farmer-workflow-usability-correction`:

- Kept the five daily farmer workflows and supply actions directly available on Home.
- Moved legacy Record income and Record expense behind `अन्य अभिलेख देखाउनुहोस्` / `Show other entries`; the actions remain available and unchanged.
- Made the disclosure adjacent to the legacy row and restored its expanded state across recreation.
- Updated the empty-state prompt so it points to visible daily actions instead of hidden legacy controls.
- Made no schema, accounting-authority, or transaction behavior changes.

The Production interaction was not patched: resource-ID/UI hierarchy inspection showed an enabled, clickable button, and a center tap derived from its current bounds opened the dialog successfully.

## Automated evidence

Previously completed focused suites cover the current milestone stack, including:

- `SupplierKhataTest`
- `FarmerOverviewTest`
- `CreditSalesMetricTest`
- `ProductSaleTest`
- `ProductSaleHistoryTest`
- `FarmSupplyTest`
- `ProductionTest`
- `ProductionAllocationTest`
- `PartyLedgerTest`
- `FarmFinancialOverviewTest`
- `MultiFarmStoreTest`
- `LocalizationParityTest`
- `:app:compileDebugKotlin`
- `git diff --check`

No broad suite, release validation, signing, or connected test suite was run.

## Android-test debt

The Android instrumentation source set still contains stale Settings references:

- `settingsCurrencyText`
- `changeSettingsCurrencyButton`
- `settingsNoFarmText`

This is a contained follow-up maintenance task, not part of the audit correction pass.

## Device cleanup

The disposable farm was created for the audit and must be deleted through the normal Farm Management backup gate and typed confirmation before commit. `RC01UpgradeFarm` must remain active and intact.

## Integrated second-pass evidence

The audit was continued on the current update-installed APK with a new disposable `M7FullAuditFarm` in Nepali. Home, Farm Management, and the full set of daily actions were inspected again.

The required full mutation sequence was not completed live during this audit. The original coordinate-tap failure was investigated separately: the current UI node was `clickable=true`, `enabled=true`, and had live bounds `[68,1392][1152,1550]` on the corrected build; tapping its derived center opened the Production dialog. There was no application crash in logcat. No fabricated or shell-injected records were used, so the joined scenario remains an evidence gap rather than an implied pass.

The disposable farm was deleted through the normal backup gate and typed confirmation. `RC01UpgradeFarm` remained present and untouched.

### Tap/friction table

| Workflow | Approx taps/typing observed or expected | Main friction | Severity |
| --- | --- | --- | --- |
| Morning production | Home Production action, then product and quantity | Direct center tap from current UI bounds opened the dialog; original coordinate attempt was stale/automated incorrectly | Medium |
| Cash sale | Home action then customer/product/quantity/rate/payment | Long dialog; no integrated evidence in this pass | Medium |
| Credit/partial sale | Quick Sale plus payment choice and amount | Summary is useful but dialog is vertically dense | Medium |
| Receive money | Home action, party selection, amount | Requires selecting party before amount; integrated evidence pending | Medium |
| Supplier purchase | Home `किनेँ`, supplier, supply, quantity, cost, payment state | Many fields in one dialog | Medium |
| Use feed | Home `प्रयोग गरेँ`, supply, quantity | Compact but requires navigating back to Home action | Low |
| Pay supplier | Home `पैसा तिरेँ`, supplier, amount | Correct mental model; integrated evidence pending | Medium |
| Evening production | Production action, product, session, quantity | Same Production click blocker | High |
| Production allocation | Production → `प्रयोग` → product/type/quantity | Full live allocation evidence remains pending | High |
| Today overview | Home block visible immediately | Dense vertical text when populated | High |
| This month | Home `यो महिना` action | Progressive disclosure is clear; populated evidence pending | Medium |

### Terminology and hierarchy

- **Medium:** Home farmer verbs are clear: `बेचेँ`, `पैसा पाएँ`, `किनेँ`, `प्रयोग गरेँ`, `पैसा तिरेँ`, `उत्पादन`.
- **Medium:** Hisab-Kitab and legacy income/expense editors still expose software-shaped concepts after navigation.
- **Low:** The overview uses factual labels but can become text-dense with several products and balances.

The primary daily action set is `बेचेँ`, `पैसा पाएँ`, `किनेँ`, `पैसा तिरेँ`, and `उत्पादन`. Farm-use actions are `प्रयोग गरेँ` and `बाँकी`. Record income and Record expense are now explicitly advanced/legacy actions behind `अन्य अभिलेख देखाउनुहोस्`; Hisab-Kitab remains available through its existing navigation.

### Accounting evidence disposition

The integrated cross-feature facts were not created on-device, so this audit does not claim live proof of the complete combined scenario. Focused domain suites continue to prove the individual authorities and invariants. The remaining live evidence gap is the joined Production → Sale → Payment → Supplier → Stock → Allocation → Overview path.

## Recommendation

**NEEDS INTEGRATED DEVICE VALIDATION**

The Production interaction concern is resolved as an automation observation, not an app touch defect. A contained Home hierarchy correction is implemented without changing accounting authorities: daily farmer actions remain prominent and legacy manual entries are progressively disclosed. The disposition remains open because the populated joined Production → Sale → Payment → Supplier → Stock → Allocation → Overview scenario, plus 36sp/dark-mode inspection and disposable-farm cleanup evidence, still needs to be completed on-device before broader farmer pilot.

## Final Integrated Device Validation

### Baseline and execution

- Status: `BLOCKED` for final integrated validation disposition; unexecuted checks are not treated as passes.
- Validation branch: `feature/m7-integrated-device-validation`.
- Commit tested: `65666c5949acb258a312b82caa3e992665c804bc`.
- Device: Moto `ZA22374XPC` (`motorola_edge_60_fusion`).
- Candidate APK: current debug APK, installed with `adb install -r`.
- Primary UI language: Nepali.
- Protected farm: `RC01UpgradeFarm` was not mutated.

### Live scenario evidence

| Fact | Expected | Actual evidence | Disposition |
| --- | --- | --- | --- |
| Milk production | Morning 38 L + Evening 31 L = 69 L | Production displayed `Milk उत्पादन: 69 लि. / लिटर`; reopening the morning session showed the existing `38` value, confirming session upsert/edit behavior | `PASS` |
| Milk sale | 57 L for रु 5,700, paid रु 3,000 | Sale confirmation displayed total रु 5,700, initial payment रु 3,000, and balance रु 2,700 | `PASS` |
| Customer payment | Receive रु 1,000; current balance रु 1,700 | Home displayed sale रु 5,700, money received रु 4,000, customer balance रु 1,700, and today credit sale रु 2,700 | `PASS` |
| Production allocation | Home 2 L, processing 6 L, animal feed 3 L; unexplained 1 L | Allocation UI opened and rejected an over-allocation when only 2 L remained. Earlier exploratory taps also created duplicate home-use entries; the disposable farm was discarded rather than treating this contaminated state as proof | `BLOCKED` |
| Supplier purchase | Feed 20 bags, रु 40,000, paid रु 15,000 | Not reached on a clean exact-scenario farm | `NOT EXERCISED` |
| Supplier usage | Use 3 bags; remaining 17 bags | Not reached | `NOT EXERCISED` |
| Supplier payment | Pay रु 10,000; payable रु 15,000; stock remains 17 bags | Not reached | `NOT EXERCISED` |

### M7.4 live allocation disposition

`BLOCKED`: the Production allocation surface was reachable and its over-allocation guard was observed, but the exact requested 2/6/3 allocation set was not established on a clean farm. The earlier Production tap concern remains resolved as an automation-coordinate issue, not an application touch defect.

### M7.6 live supplier disposition

`NOT EXERCISED`: the clean integrated run did not reach supplier purchase, usage, or supplier payment. No supplier accounting conclusion is claimed from this device pass.

### Farmer Overview

- Today overview: customer-side sale and payment values matched the expectations above; production appeared as 69 L.
- Unexplained Milk: intermediate exploratory states displayed 12 L, 8 L, and 2 L as allocations were attempted; this is contaminated evidence and is not a pass for the required 1 L result.
- Feed remaining and supplier payable: `NOT EXERCISED`.
- Expenses: `NOT EXERCISED`; no expense value is invented and no duplicate-treatment conclusion is claimed for the रु40,000 PURCHASE trade.
- This Month: `NOT EXERCISED`.
- No value was called profit.

### Other Entries, 36sp, and dark mode

- Other Entries disclosure, hide behavior, and recreation restoration: `NOT EXERCISED`.
- 36sp Home, Today overview, This Month, Production, allocation, supplier, and customer-payment inspection: `NOT EXERCISED`.
- Dark mode, including a 36sp pass and selected/unselected controls: `NOT EXERCISED`.
- No accessibility or visual defect was established because those checks were not reached.

### Defects and cleanup

- Corrections: none. No application defect was established; no schema, accounting authority, or Home redesign change was made.
- Temporary `M7IntegratedValidation` was deleted through the normal UI deletion flow, backup gate, and typed `DELETE` confirmation: `PASS`.
- Farm management then showed only `RC01UpgradeFarm`: `PASS`.
- Remaining known debt: stale Settings androidTest references listed above; connected supplier/allocation/monthly/36sp/dark-mode evidence remains incomplete.

## Final Recommendation

**NEEDS ANOTHER CONTAINED CORRECTION PASS**

## Gate A — Production Allocation (2026-08-16)

### Context and baseline

- Validation branch: `validation/m7-gate-a-production-allocation`.
- Baseline SHA: `4eaad74d542dd70295abcf653a4bd611781b663e` (integrated device validation record), on top of `65666c5949acb258a312b82caa3e992665c804bc` (farmer workflow usability correction).
- Final SHA (this record's commit): filled below after commit.
- Working tree was clean before validation. No application code was changed; the validated build is the exact branch debug APK.
- Prior evidence is preserved and not overwritten: the integrated run established Production 69 L and Sale 57 L live, but the exact 2/6/3 allocation set was `BLOCKED` because exploratory taps had created duplicate home-use entries and the contaminated farm was discarded. Gate A re-runs the complete chain on a clean disposable farm.

### Device and build

- Physical device: Moto `ZA22374XPC` (`motorola_edge_60_fusion`, USB).
- Android 16 / API 36.
- App: Kisab `0.2.0` (versionCode 3), debug build installed with `adb install -r` from the branch `app-debug.apk`.
- APK SHA-256: `80faac560b157d2c58dbeb7214f719fb2d83af867a7793d671d6fe7739652efc`.
- Primary UI language: Nepali (per-app locale `ne-NP`); device timezone `Asia/Tokyo`.
- Protected farm `RC01UpgradeFarm` was not mutated.
- Disposable farm: `M7GateA` (NPR), created and later deleted through the normal UI.

### Scenario and expected values

Daily scenario on `M7GateA`, farmer-facing Nepali UI only:

| Fact | Expected |
| --- | --- |
| Milk production | Morning 38 L + Evening 31 L = 69 L |
| Milk sale | 57 L via normal `बेचेँ` Quick Sale path |
| Home use | 2 L |
| Processing | 6 L |
| Animal feed | 3 L |
| Unexplained | 1 L |

### Actual displayed values (from the app UI)

All values below were read from the current UI hierarchy on the device; every tap coordinate was derived from live node bounds, never reused hard-coded coordinates.

| Step | Expected | Actual displayed value | Disposition |
| --- | --- | --- | --- |
| Morning production | 38 L | Production dialog: quantity input pre-filled `38` for `बिहान`; saved | `PASS` |
| Evening production | 31 L | Production dialog: quantity input pre-filled `31` for `बेलुका`; saved | `PASS` |
| Production total | 69 L | Production dialog summary: `Milk उत्पादन: 69 लि. / लिटर बेचेको: 0 लि. / लिटर नखुलेको: 69 लि. / लिटर` | `PASS` |
| Milk sale | 57 L | Quick Sale summary: `जम्मा: रु ५,७००.०० पूरै पैसा आयो` (quantity 57, rate 100, paid in full) | `PASS` |
| Sold derived from sale facts | 57 L | Production dialog summary after sale: `बेचेको: 57 लि. / लिटर नखुलेको: 12 लि. / लिटर` | `PASS` |
| Home use | 2 L | Allocation dialog after save: `नखुलेको: 10 लि. / लिटर` | `PASS` |
| Processing | 6 L | Allocation dialog after save: `नखुलेको: 4 लि. / लिटर` | `PASS` |
| Animal feed | 3 L | Allocation dialog after save: `नखुलेको: 1 लि. / लिटर` | `PASS` |
| Final reconciliation | 69 − 57 − 2 − 6 − 3 = 1 | Production dialog summary: `Milk उत्पादन: 69 लि. / लिटर बेचेको: 57 लि. / लिटर नखुलेको: 1 लि. / लिटर` | `PASS` |
| Home Today overview | produced 69, unexplained 1, no phantom finance | `आज` block: `उत्पादन: Milk: 69 लि. / लिटर`, `नखुलेको: Milk 1 लि. / लिटर`, `बिक्री: रु ५,७००.००`, `पैसा आएको: रु ५,७००.००`, `खर्च: रु ०.००`, `लिन बाँकी: रु ०.००`, `तिर्न बाँकी: रु ०.००`, `आज उधार बिक्री: रु ०.००` | `PASS` |

The per-type allocation evidence is the stepwise `नखुलेको` decrement observed in the allocation dialog for each type (`12 → 10 → 4 → 1`) plus the type spinner selection (`घरमा`, `प्रशोधन`, `पशुलाई`) at each save. The app does not expose a per-type totals list; the derived unexplained values prove each allocation's quantity. This matches the previous gate's evidence approach.

### Invariants

- Production remained operational-only: no FarmTransaction income, no FarmTransaction expense, no settlement money received, no receivable/payable was created by production or allocation. Confirmed by the Today overview (`खर्च: रु ०.००`, `लिन बाँकी: रु ०.००`, `तिर्न बाँकी: रु ०.००`).
- Sold quantity derived from matching `ProductSaleDetail` + `SALE` Trade facts (57 L appeared in reconciliation only after the normal sale). No persistence was injected; no file/database was edited; no description parsing was used.
- Allocation did not become inventory accounting: supplies/stock were never touched (no supplies exist on the farm).
- The negative inconsistency path was not triggered (unexplained stayed positive); no clamping observed.
- No transformations, recipes, yields, livestock records, or financial valuation were introduced.
- The sale itself is the only financial mutation, via the existing `addProductSale` authority (SALE Trade + ProductSaleDetail + opening Settlement for the paid-in-full sale). Production/allocation did not create or alter it.

### Persistence check

- Force-stop + cold relaunch landed on `M7GateA`.
- Production dialog after relaunch: `Milk उत्पादन: 69 लि. / लिटर बेचेको: 57 लि. / लिटर नखुलेको: 1 लि. / लिटर` (identical to pre-relaunch).
- Allocation dialog after relaunch: `नखुलेको: 1 लि. / लिटर`.
- Session edit/correction behavior: reopening the dialog pre-filled `बिहान` = `38` and `बेलुका` = `31` (existing record upsert/edit), confirming normal correction of an existing record works as designed.
- Disposition: `PASS`.

### Focused automated verification

Ran on this branch: `:app:testDebugUnitTest` with `--tests` filters, plus `:app:compileDebugKotlin`, `:app:assembleDebug`, `git diff --check`.

| Suite | Tests | Result |
| --- | --- | --- |
| ProductionTest | 3 | 0 failures |
| ProductionAllocationTest | 4 | 0 failures |
| ProductSaleTest | 7 | 0 failures |
| FarmerOverviewTest | 3 | 0 failures |
| FarmFinancialOverviewTest | 24 | 0 failures |
| PartyLedgerTest | 17 | 0 failures |
| LocalizationParityTest | 9 | 0 failures |
| `:app:compileDebugKotlin` | — | OK |
| `:app:assembleDebug` | — | OK |
| `git diff --check` | — | OK |

Total: 67 tests, 0 failures. Disposition: `PASS`.

### Cleanup

- `M7GateA` deleted through the normal application UI: Farm Management → farm details → `फार्म मेटाउनुहोस्` → confirmation → danger backup gate (`मसँग पहिले नै ब्याकअप छ`) → typed `DELETE` confirmation.
- After deletion the Farms list showed only `RC01UpgradeFarm` (सक्रिय). `M7GateA` was gone.
- `RC01UpgradeFarm` Home confirmed intact: `बाँकी रकम: -रु १२,४७,६००.००`, `आम्दानी: रु ३,०५०.००`, `खर्च: रु १२,५०,६५०.००`, `लिन बाँकी: रु १५,०००.००` — unchanged from the pre-gate state.
- No other validation farm or real farm was modified. Disposition: `PASS`.

### Interaction friction

- No application touch defect was found on this pass. All taps derived from live hierarchy bounds succeeded; each transition was confirmed by a hierarchy dump before continuing.
- Medium: the Production dialog summary does not auto-refresh after an allocation save while the dialog stays open; it requires a content tap or reopening to show the updated reconciliation. Usability friction, not a correctness defect.
- Medium: the allocation type control is a second unlabeled spinner; selecting a type requires opening the dropdown. Discoverable but not labeled.
- Low: with the software keyboard open, lower dialog controls can be pushed off-screen; the keyboard must be dismissed before reaching the save button.
- No per-type allocation totals surface exists in the UI; per-type verification relies on the unexplained decrement and the type spinner selection.

### Corrections

None. No application defect was established; no schema, accounting-authority, or Home redesign change was made.

### Dispositions

| Check | Disposition |
| --- | --- |
| Production recording (69 L) | `PASS` |
| Sale-derived sold quantity (57 L) | `PASS` |
| Home use (2 L) | `PASS` |
| Processing (6 L) | `PASS` |
| Animal feed (3 L) | `PASS` |
| Unexplained (1 L) | `PASS` |
| Financial invariants | `PASS` |
| Persistence (force-stop/cold relaunch) | `PASS` |
| Session record edit/correction | `PASS` |
| Cleanup | `PASS` |
| Focused automated tests | `PASS` |

### Gate A disposition

`PASS`

The complete chain `69 L produced → 57 L sold → 2 L home use → 6 L processing → 3 L animal feed → 1 L unexplained` was proven end-to-end by normal farmer-facing UI interaction on the physical Moto, and the state survived force-stop/cold relaunch. Domain tests alone were not used as the pass basis; they only corroborate.

### Remaining blockers

None for Gate A. Known unrelated debt remains: stale androidTest `Settings` references (`settingsCurrencyText`, `changeSettingsCurrencyButton`, `settingsNoFarmText`), which is a separate follow-up maintenance task and was not touched here.

Per the gate instructions, no Gate B work was started.
