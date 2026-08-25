# UX-02: Today Dashboard Redesign Implementation

## 1. Overview and Intent

UX-02 delivers the second implementation slice of the competitive farmer UX redesign defined in `docs/design/COMPETITIVE_FARMER_UX_REDESIGN.md`. Building upon the 5-element shell established in `UX-01` (`Today | Khata | + Record | Farm Work | More`), UX-02 transforms **Today (`आज`)** from an overloaded bookkeeping utility into the farmer's daily operational command surface.

### Product Principle
Today answers the farmer's essential daily questions within seconds:
1. **Which farm am I looking at?** Obvious app-bar farm identity with a one-tap farm switcher.
2. **What happened today?** Clean hero answering today's production output, sales, received money, and expenses without accounting jargon.
3. **Is anybody supposed to pay me?** Dedicated directional `लिन बाँकी` (customer receivables) card with instant Khata routing.
4. **Do I owe anybody?** Dedicated directional `तिर्न बाँकी` (supplier payables) card with instant Khata routing (never netted into a deceptive single balance).
5. **What farm goods/products/supplies matter right now?** Clear Farm Status card summarizing today's production output and remaining key supplies.
6. **What was recorded recently?** Clean, focused stream of the 3–5 newest events with one-tap editing.

---

## 2. Old vs. Redesigned Today Architecture

### Previous Today Structure
- Heavy text dump in `farmerOverviewTodayText`.
- Competing generic balances (`balanceText`, `incomeText`, `expensesText`) at the top conflicting with directional ledger balances.
- A "button wall" of 7 equal-weight action buttons (`quickSaleButton`, `receivedMoneyButton`, `supplyPurchaseButton`, `supplyUsageButton`, `supplyStockButton`, `supplierPaymentButton`, `productionButton`).
- Long unsegmented scroll where tools, transaction editor, and daily summary competed for attention.

### Redesigned Today Structure
- **App Bar Switcher (`shellAppBar`)**: Clickable farm name + dropdown icon (`shellFarmSwitchIcon`) displaying an instant switcher modal for multi-farm switching.
- **Date Header Bar (`todayHeaderBar`)**: Localized date (e.g. `2026 अगस्ट 17` / `Aug 17, 2026`) paired with `THIS MONTH` (`farmerOverviewMonthButton`) overview action.
- **1. Today Hero Card (`todayHeroCard`)**:
  - `todayEmptyStateText`: Intentional greeting and prompt when no work is recorded today.
  - `todayProductionContainer`: Production headline (e.g., `दूध उत्पादन: ५० लि.`) + unexplained production warning with one-tap `Reconcile` (`मिलाउनुहोस्`) shortcut.
  - `todayMoneyMetricsContainer`: 4 distinct metric tiles: Sales (`बिक्री`), Received (`पैसा आयो`), Expenses (`खर्च`), and Credit Sales (`उधार बिक्री`).
- **2. Money Needing Attention (`todayMoneyAttentionCard`)**:
  - `todayKhataSettledText`: Concise settled confirmation when both receivables and payables are 0.
  - `todayReceivableContainer`: Directional `लिन बाँकी (ग्राहकबाट)` with green accent and `खाता हेर्नुहोस्` button.
  - `todayPayableContainer`: Directional `तिर्न बाँकी (आपूर्तिकर्तालाई)` with red/coral accent and `खाता हेर्नुहोस्` button.
- **3. Farm Status Card (`todayFarmStatusCard`)**:
  - Summarizes current production state and remaining supplies (e.g. `दाना: १० बोरा, मल: २ बोरा`) with a shortcut to `Farm Work` (`फार्मको काम हेर्नुहोस्`).
- **4. Recent Activity (`todayRecentActivityCard`)**:
  - Top 3–5 newest events with human-readable timestamp, category, description, and localized amount.
- **5. Collapsed Farm Tools**: Retained at bottom behind `SHOW FARM TOOLS` toggle for advanced backup and crop/livestock records.

---

## 3. Data Authority & Metric Derivation

All metrics are derived directly from established domain authorities without schema alterations:

| UI Metric / Section | Domain Authority | Computation Method |
|---|---|---|
| **Today Production** | `ProductionRecord` | `FarmState.farmerOverview().daily.production` filtered by device-local day. |
| **Unexplained Production** | `ProductionAllocation` | `FarmState.productionReconciliation().unexplained` against today's allocations and sales. |
| **Today Sales** | `Trade` | `trades.filter { type == SALE && inDay() }.sumBy(totalMinor)`. |
| **Money Received Today** | `Settlement` | `settlements.filter { inDay() && trade.type == SALE }.sumBy(amountMinor)`. |
| **Expenses Today** | `FarmTransaction` | `transactions.filter { type == EXPENSE && inDay() }.sumBy(amountMinor)`. |
| **Credit Sales Today** | `Trade` & `Settlement` | `trades.filter(SALE).sum(totalMinor - initialPayment)`. |
| **To Receive (`लिन बाँकी`)** | `PartyLedger` | Sum of `partyLedgerSummary(partyId).toReceiveMinor` across customer parties. |
| **To Pay (`तिर्न बाँकी`)** | `PartyLedger` | Sum of `partyLedgerSummary(partyId).toPayMinor` across supplier parties. |
| **Supplies Remaining** | `FarmSupply` & `SupplyPurchaseDetail` | `supplyQuantityAvailable(supplyId) > 0`. |
| **Recent Activity** | `FarmTransaction` | `transactionsNewestFirst().take(5)`. |

---

## 4. Farm Switcher Contract

- Tapping `shellTitle` or `shellFarmSwitchIcon` in the top app bar opens `showFarmSwitcherDialog()`.
- The dialog lists all active and accessible farms from `LocalUserService`.
- The active farm is marked with `Active` (`सक्रिय`).
- Selecting another farm immediately invokes `service.setCurrentFarmId(farmId)`, updates `currentFarmId`, re-renders Today, and displays a confirmation toast.
- Includes a direct action button `Manage farms` (`फार्महरू व्यवस्थापन`) routing to `Destination.FARMS`.

---

## 5. Accessibility, Scaling & Localization

- **Devanagari Rendering**: All labels (`आजको काम र पैसा`, `लिन बाँकी`, `तिर्न बाँकी`, `फार्मको स्थिति`, `हालैको गतिविधि`) use flexible layouts without rigid height limits to prevent matra/glyph clipping.
- **36sp Large Text**: Metrics and cards stack gracefully when font size is set to maximum (`36sp`).
- **Dark Mode**: High-contrast dark palette (`#1A241C` card surface, `#1C2E20` green receivable card, `#2E1C1C` red payable card) preserves visual hierarchy in low-light conditions.
- **Directional Color Safety**: Receivables and payables use distinct labels, containers, and icons—never relying on color alone to communicate direction.

---

## 6. Device Verification (Motorola Edge 60 Fusion - Android 16 / ZA22374XPC)

- **Instrumented Test Suites**:
  - `FarmTodayDashboardRedesignTest`: **7/7 passed** on device.
  - `FarmActivityShellRedesignTest`: **7/7 passed** on device.
  - `FarmOverviewAndHisabDeviceTest`: **5/5 passed** on device.
  - `KisanToolboxDeviceBatteryTest`: **5/5 passed** on device.
  - `FarmActivityShellInsetsTest`: **3/3 passed** on device.
  - `LocalizedResourceResolutionTest`: **15/15 passed** on device.
  - `FarmActivityLocalizationSmokeTest`: **4/4 passed** on device.
  - Total on-device test count: **46/46 passed**.
- **Local Unit Tests**:
  - `./gradlew test`: **379 tests passed** across 46 test suites with 0 failures.
  - `LocalizationParityTest`: 100% parity across English and Nepali resources.
- **Lint**: `./gradlew lint` passed with 0 errors.

---

## 7. Known Compromises & Deferred Work

### Known Compromises
- The generic transaction editor remains an inline form on Today when editing recent transactions; full-screen flows will replace this in future transaction UX slices.
- The Month overview dialog is retained as an informational modal summary prior to dedicated monthly analytics screens.

### Deferred to UX-03 / Future Milestones
- Khata tabbed filters (`लिन बाँकी` / `तिर्न बाँकी` / `सबै`).
- Full-screen task flows for sales, purchases, and production allocation.
- Detailed activity feed pagination.
