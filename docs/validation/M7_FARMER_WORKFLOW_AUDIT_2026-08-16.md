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
