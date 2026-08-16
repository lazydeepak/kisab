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

## Gate B — Supplier Khata (M7.6) Device Validation

### Baseline and execution

- Validation branch: `validation/m7-gate-b-supplier-khata`.
- Baseline commit: `4eaad74` (integrated device validation record).
- Device: Moto `ZA22374XPC` (`motorola_edge_60_fusion`), Android 16 / API 36, timezone `Asia/Tokyo`.
- Candidate APK: debug build of baseline `4eaad74` installed with `adb install -r`.
- Primary UI language: Nepali (`cmd locale set-app-locales com.susankhya.kisab --locales ne-NP`).
- Protected farm: `RC01UpgradeFarm` was never mutated; baseline captured before the run and re-verified after cleanup.
- Disposable farm: `M7GateB` (`farm-c98b7d91-7350-460d-93fc-28414142d0a7`, NPR), created via UI.

### Live scenario evidence

| Fact | Expected | Actual evidence | Disposition |
| --- | --- | --- | --- |
| Supplier purchase | Feed 20 bags, रु 40,000, paid रु 15,000 → payable रु 25,000 | Purchase dialog summary `जम्मा: रु ४०,०००.०० तिर्न बाँकी: रु २५,०००.००`; Home `तिर्न बाँकी: रु २५,०००.००`, `बाँकी सामान: Feed 20 बोरा`; Hisab-Kitab `खरिद: रु ४०,०००.००`, `गरिएका भुक्तानी: रु १५,०००.००` | `PASS` |
| No-double-counting | Single PURCHASE Trade authority; no duplicate EXPENSE | Persisted `kisab_farm_store.xml` for M7GateB contains one PURCHASE trade `trade-53d9e0a9…` (4000000), one initial settlement (1500000, `isInitialPayment=true`), and **zero** FarmTransactions; Home `खर्च: रु ०.००` while the रु 40,000 purchase is tracked once via Hisab-Kitab trades | `PASS` |
| Supplier stock | 20 bags after purchase | Supply dialog `Feed 20 बाँकी / किनेको: 20 प्रयोग: 0`; persisted `SupplyPurchaseDetail` qty 20 linked to `purchaseTradeId` | `PASS` |
| Supply usage | Use 5 bags → 15 remaining; payable unchanged | After usage, supply dialog `Feed 15 बाँकी / किनेको: 20 प्रयोग: 5`; Home `बाँकी सामान: Feed 15 बोरा` and `तिर्न बाँकी: रु २५,०००.००` unchanged; persisted usage `5 BAG` | `PASS` |
| Supplier payment | Pay रु 10,000 → payable रु 15,000; stock unchanged | Supplier khata `तिर्न बाँकी: रु २५,०००.००` before payment; after payment Home `तिर्न बाँकी: रु १५,०००.००`, stock `Feed 15 बोरा`; Hisab-Kitab `गरिएका भुक्तानी: रु २५,०००.००`, `तिर्न बाँकी: रु १५,०००.००`; khata ledger shows खरिद रु ४०,००० → भुक्तानी रु १५,००० (बाँकी २५,०००) → भुक्तानी रु १०,००० (बाँकी १५,०००); persisted second settlement (1000000, `isInitialPayment=false`) | `PASS` |
| Receivable/payable separation | `लिन बाँकी` unaffected by supplier flow | `लिन बाँकी: रु ०.००` throughout; supplier khata `प्राप्त गर्न बाँकी: रु ०.००`, `तिर्न बाँकी: रु २५,०००.०० → रु १५,०००.००` | `PASS` |
| Persistence | All values survive process death | After force-stop + cold relaunch, M7GateB reloaded with `तिर्न बाँकी: रु १५,०००.००` and `बाँकी सामान: Feed 15 बोरा`; persisted store matched the UI (trades 1, settlements 2, supplyPurchaseDetails 1, supplyUsages 1, transactions 0) | `PASS` |

### Codec defect found and corrected

- A schema-12 codec defect was discovered during this gate: `decodeSchema12` truncated supply-purchase-detail records to 5 parts for the back-compat schema-11 pass, dropping `purchaseTradeId`. The intermediate `decodeSchema8` pass then constructed a `SupplyPurchaseDetail` with no source, `require` failed, `decodeOrNull` returned `null`, and `loadFarm` returned `null` — the app fell back to the create-farm screen after any supplier purchase (confirmed by reproducing the exact on-device payload decode in a unit test and by cold-relaunch behavior on device).
- Correction: `decodeSchema12` now blanks the intermediate supply-purchase-details field and re-decodes the original 6-part payload (`hasPurchaseTradeLink = true`). This is small, local, directly evidenced, and does not change the final decoded state.
- Regression test: `app/src/test/kotlin/com/susankhya/kisab/persistence/DecodeProbeTest.kt` decodes the exact on-device M7GateB payload and asserts the PURCHASE trade, initial settlement, and supply purchase detail.
- Rebuilt APK SHA-256 `0a4eb29858d659c18420d883ecd9dbef4e414b56d4af174fcbd332024138466b` was installed and the full scenario re-verified live after the fix.

### Automated evidence

- Focused Gate B suites: `SupplierKhataTest`, `FarmerOverviewTest`, `FarmSupplyTest`, `FarmFinancialOverviewTest`, `PartyLedgerTest`, `FarmSliceServiceTest`, `MultiFarmStoreTest`, `LocalizationParityTest`, `DecodeProbeTest` — 154 tests, 0 failures.
- Full `:app:testDebugUnitTest`: 434 tests, 0 failures.
- `:app:compileDebugKotlin`, `:app:assembleDebug`, `git diff --check`: all clean.

### Device cleanup

- `M7GateB` deleted through the normal UI deletion flow: warning dialog → backup gate (acknowledged existing backup) → typed `DELETE` confirmation: `PASS`.
- Post-delete store: `farm_ids` contains only `RC01UpgradeFarm`; `current_farm_id` switched to it; `M7GateB` payload removed.
- `RC01UpgradeFarm` restored and re-verified on Home: बाँकी रकम -रु १२,४७,६००.००, आम्दानी रु ३,०५०.००, खर्च रु १२,५०,६५०.००, लिन बाँकी रु १५,०००.००, तिर्न बाँकी रु ०.०० (matches baseline; no supplier/supply leakage): `PASS`.

## Gate C — Populated Overview / 36sp / Dark Mode Device Validation

### Baseline and execution

- Validation branch: `validation/m7-gate-c-overview-visual`.
- Baseline commit: `f09cb63` (Gate B final; includes the schema-12 codec correction). The final commit for Gate C is the same `f09cb63` — validation only, **no application code changes**.
- APK SHA-256: `0a4eb29858d659c18420d883ecd9dbef4e414b56d4af174fcbd332024138466b` (Gate B fixed debug build, installed on device).
- Device: Moto `ZA22374XPC` (`motorola_edge_60_fusion`), Android 16 / API 36, timezone `Asia/Tokyo`, locale `ne-NP` (app set via `cmd locale set-app-locales`). Device system dark mode active (`ui_night_mode=2`).
- Protected farm: `RC01UpgradeFarm` never mutated; store verified before and after.
- Disposable farm: `M7GateC` (NPR), created via UI.
- Pre-Gate-C presentation state (recorded, and restored after): `text_size_sp=27`, `appearance_mode=system`.

### Population facts (deterministic, all created today 2026-08-16)

| Metric (Home label) | Fact | Value |
| --- | --- | --- |
| Production (उत्पादन) | Milk 40 L morning + 30 L evening | 70 लि. / लिटर |
| Unexplained (नखुलेको) | Produced 70 − sold 30, no allocation | Milk 40 लि. / लिटर |
| Sales (बिक्री) | Sold 30 L @ रु 100, paid रु 1,000 at creation | रु ३,०००.०० |
| Money received (पैसा आएको) | Initial रु 1,000 + customer payment रु 500 | रु १,५००.०० |
| Expenses (खर्च) | Legacy EXPENSE (दाना/Parcel) | रु २००.०० |
| Customer receivable (लिन बाँकी) | 3000 − 1000 − 500 | रु १,५००.०० |
| Today credit sales (आज उधार बिक्री) | 3000 − 1000 initial | रु २,०००.०० |
| Supplies remaining (बाँकी सामान) | Bought Feed 10 बोरा @ रु 200 (paid रु 1,000), used 2 | Feed 8 बोरा |
| Supplier payable (तिर्न बाँकी) | 2000 − 1000 | रु १,०००.०० |
| Current month (यो महिना) | Same period covers all of the above | production 70 L, sales रु 3,000, received रु 1,500, expenses रु 200, receivable रु 1,500, payable रु 1,000, supplies 8 |

### Verification steps

| Step | Evidence | Disposition |
| --- | --- | --- |
| Normal-mode Home baseline (27sp) | Full `आज` overview: उत्पादन Milk 70 लि., नखुलेको 40 लि., बिक्री रु ३,०००.००, पैसा आएको रु १,५००.००, खर्च रु २००.००, लिन बाँकी रु १,५००.००, तिर्न बाँकी रु १,०००.००, आज उधार बिक्री रु २,०००.००, बाँकी सामान Feed 8 बोरा; यो महिना dialog shows all 8 lines; Home scrolls to all sections | `PASS` |
| Set 36sp via Settings UI | Text-size seekbar dragged to max → `text_size_sp=36` persisted in `kisab_app_appearance.xml`; label `अक्षरको आकार: ३६ px` | `PASS` |
| Set Dark via Settings UI | `appearance_mode=dark` persisted; Home bg #303030 (48,48,48), green brand header, dark nav — dark theme confirmed | `PASS` |
| Home full-surface audit at 36sp+Dark | Scroll top→bottom: farm name, बाँकी रकम/आम्दानी/खर्च, अन्य अभिलेख disclosure, all 8 action buttons (बेचेँ/पैसा पाएँ/किनेँ/प्रयोग गरेँ/बाँकी/पैसा तिरेँ/उत्पादन), full 10-line आज overview, यो महिना button, हालैका लेनदेनहरू, फार्म उपकरणहरू toggle — every section reachable, no unreachable controls | `PASS` |
| Today overview readability at 36sp | आज text node [68,2089][1152,2389]; all 10 lines present; L-quantities (70/40 लि.) clearly distinct from रु values; scroll reveals entire block, no clipped last line | `PASS` |
| Other Entries disclosure at 36sp+Dark | Toggle `देखाउनुहोस्`→`लुकाउनुहोस्` inserts आम्दानी/खर्च अभिलेख buttons inline (no overlay, no layout break); toggle collapses correctly | `PASS` |
| यो महिना at 36sp+Dark | Dialog opens, message [75,1123][1144,1630] fully visible above सम्पन्न button; readable light-on-dark text; dismisses | `PASS` |
| Representative dialogs at 36sp+Dark | Production (summary line, product spinner, qty 40 prefilled, sessions बिहान/बेलुका/अन्य, all buttons), Sell (customer, लिन बाँकी रु १,५००, product, rate 100.00 prefilled, payment options, buttons), Bought (supplier, Feed, qty/total, payment options, buttons), पैसा तिरेँ (supplier, तिर्न बाँकी रु १,०००, amount, पूरै रकम, buttons) — all fully visible and actionable at 36sp+Dark | `PASS` |
| Navigation audit | Home → Hisab-Kitab (financial overview: प्राप्त गर्न बाँकी रु १,५००, तिर्न बाँकी रु १,०००, नगद आम्दानी रु ०, खर्च रु २००, खुद -रु २००, बिक्री रु ३,०००, खरिद रु २,०००, प्राप्त/गरिएका भुक्तानी, स्थिति — all correct, scrolls to कुल स्थिति रु ५००) → Hisab (calculator tools render) → menu → फार्महरू (both farms, M7GateC सक्रिय) → Back returns to previous screen → Home tab restores Home | `PASS` |
| Rotation / recreation / disclosure state | Rotate to landscape: Home scrolls through all sections; disclosure stays open (`लुकाउनुहोस्` + record buttons). Rotate back: disclosure still open. Force-stop + cold relaunch: M7GateC reloads at 36sp+Dark; disclosure resets to collapsed (`देखाउनुहोस्`, default state — in-memory disclosure resets on fresh process, no crash, no layout corruption) | `PASS` |

### Automated evidence

- Focused Gate C suites: `LocalizationParityTest` (9), `FarmerOverviewTest` (3), `ProductionTest` (3), `ProductionAllocationTest` (4), `SupplierKhataTest` (3), `FarmSupplyTest` (4), `PartyLedgerTest` (17), `FarmFinancialOverviewTest` (24), `DecodeProbeTest` (1) — **67 tests, 0 failures**.
- `:app:compileDebugKotlin`, `:app:assembleDebug`, `git diff --check`: all clean.
- Evidence artifacts: `docs/validation/gateC_baseline_normal_top.png`, `gateC_baseline_normal_top.xml`, `gateC_baseline_normal_bottom.png`, `gateC_baseline_normal_month_area.png`, `gateC_36sp_dark_home_top*.png`, `gateC_36sp_dark_disclosure_open.png`, `gateC_36sp_dark_month_overview.png`, `gateC_36sp_dark_dialog_production.png`, `gateC_36sp_dark_dialog_sell.png`, `gateC_36sp_dark_dialog_bought.png`, `gateC_36sp_dark_dialog_supplier_payment.png`, `gateC_36sp_dark_home_relaunch*.png`, `gateC_post_restore_rc01.png`.

### Corrections

- None. No application code changed during Gate C. All checks passed with the Gate B baseline APK. No A/B/C-class findings required fixing.

### Device cleanup

- `M7GateC` deleted through the normal UI deletion flow: warning dialog → backup gate (acknowledged existing backup) → typed `DELETE` confirmation: `PASS`.
- Post-delete store: `farm_ids` contains only `RC01UpgradeFarm`; `current_farm_id` points to it; no `M7GateC` payload remains.
- `RC01UpgradeFarm` re-verified: schema-6 payload length 2017, Home shows बाँकी रकम -रु १२,४७,६००.००, आम्दानी रु ३,०५०.००, खर्च रु १२,५०,६५०.००, लिन बाँकी रु १५,०००.००, तिर्न बाँकी रु ०.०० (matches baseline; no Gate C leakage): `PASS`.
- Presentation settings restored to pre-Gate-C state and verified in `kisab_app_appearance.xml`: `text_size_sp=27`, `appearance_mode=system` (device system dark remains active, so rendering follows system as it did before Gate C).

## Final Recommendation

**PASS**

M7 focused device validation gates complete: **Gate A (Production Allocation) PASS**, **Gate B (Supplier Khata + codec correction) PASS**, **Gate C (Populated Overview / 36sp / Dark Mode) PASS**. No blocking defects remain for the exercised M7 surfaces at normal, 36sp, or dark presentation; all dispositions above are `PASS` with no code changes required. Per the validation mandate, OpenCode stops here and M7 feature development stays frozen pending the supervised Navigation + Farmer UX redesign.
