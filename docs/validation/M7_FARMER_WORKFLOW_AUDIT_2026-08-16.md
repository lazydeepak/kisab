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
- The full multi-step mutation sequence was not completed live because the dynamically positioned Production dialog did not open reliably from coordinate automation during this pass.
- Domain tests already cover the accounting scenario and were not treated as a substitute for the pending physical allocation evidence.

## Observations

### High — Home is overcrowded

Home currently exposes all of these as separate primary controls:

- legacy Record income
- legacy Record expense
- `बेचेँ`
- `पैसा पाएँ`
- `किनेँ`
- `प्रयोग गरेँ`
- `बाँकी`
- `पैसा तिरेँ`
- `उत्पादन`
- Today overview
- This month

This is a tall, dense action stack for a semi-literate farmer. The actions are individually understandable in Nepali, but the hierarchy between daily actions and legacy accounting actions is weak. No speculative redesign was applied during this audit.

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

No application correction was made in this audit pass. There was no observed correctness defect requiring code changes, and the Home overcrowding issue needs a deliberate product decision rather than a local label tweak.

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

The required full mutation sequence could not be completed live: the `उत्पादन` button was present and enabled in the UI hierarchy, but repeated coordinate taps did not dispatch its click on the Moto during this pass. There was no application crash in logcat. Because production is the first required fact for the cross-feature scenario, continuing with fabricated or shell-injected records would not be valid evidence.

The disposable farm was deleted through the normal backup gate and typed confirmation. `RC01UpgradeFarm` remained present and untouched.

### Tap/friction table

| Workflow | Approx taps/typing observed or expected | Main friction | Severity |
| --- | --- | --- | --- |
| Morning production | Blocked at visible Production action during device tap automation | Click dispatch could not be completed live | High |
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

The primary daily action set is `बेचेँ`, `पैसा पाएँ`, `किनेँ`, `पैसा तिरेँ`, and `उत्पादन`. Farm-use actions are `प्रयोग गरेँ` and `बाँकी`. Record income, Record expense, and Hisab-Kitab are advanced/legacy actions. This remains a correction hypothesis; no Home redesign was implemented.

### Accounting evidence disposition

The integrated cross-feature facts were not created on-device, so this audit does not claim live proof of the complete combined scenario. Focused domain suites continue to prove the individual authorities and invariants. The remaining live evidence gap is the joined Production → Sale → Payment → Supplier → Stock → Allocation → Overview path.

## Recommendation

**NEEDS USABILITY CORRECTION PASS**

This is based on incomplete physical evidence, not a correctness failure. The app has coherent accounting authorities and clear farmer verbs, but the whole-product day could not be completed because the Production action did not dispatch from the Moto during this pass. Home density is also a concrete High usability issue. A contained follow-up should first resolve the device interaction/testability blocker, then reassess action grouping with a populated farm before broader farmer pilot.
