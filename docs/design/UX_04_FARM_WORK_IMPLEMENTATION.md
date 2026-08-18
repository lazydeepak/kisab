# UX-04: Farm Work Redesign Implementation

## 1. Overview and Intent

UX-04 delivers the fourth implementation slice of the competitive farmer UX redesign defined in `docs/design/COMPETITIVE_FARMER_UX_REDESIGN.md`. Building upon the 5-element shell (`UX-01`), the Today dashboard (`UX-02`), and the Khata relationship surface (`UX-03`), UX-04 transforms **Farm Work (`फार्मको काम`)** into a farmer-first operational surface answering:
1. **What did the farm produce today?** (Total produced, Morning vs. Evening sessions).
2. **What happened to that production?** (Sold, home use, processing, animal feed, and unexplained remainder).
3. **What supplies do I still have physically available?** (Remaining bags, litres, kg, or bottles).
4. **What was bought or used recently?**

### Product Principle
Farm Work is **operational work management**, NOT warehouse inventory or enterprise ERP. The farmer does not manage valuations, batch numbers, or depreciation. They manage daily outputs, physical uses, and available supplies.

---

## 2. Old vs. Redesigned Farm Work Architecture

### Previous Farm Work Structure
- A plain vertical stack of 5 full-width action buttons (`farmWorkProductionButton`, `farmWorkAllocationButton`, `farmWorkBoughtButton`, `farmWorkUsedButton`, `farmWorkRemainingButton`).
- Zero operational facts visible without tapping into multiple nested dialogs.

### Redesigned Farm Work Structure
- **1. Production Section (`farmWorkProductionContainer`)**:
  - `farmWorkProductionHeaderContainer`: Prominent section header (`उत्पादन / Production`) with contextual `[उत्पादन लेख्नुहोस् / Record production]` action button.
  - `farmWorkNoProductsText`: Welcoming empty state guiding the farmer to register their first farm product (milk, vegetables, crops).
  - For each product, a card (`bg_today_card`):
    - **Headline**: Product Name + Today context (e.g. `Cow Milk · Today` / `दूध · आज`) with bold produced quantity (`Today: 50 L / litre`).
    - **Session breakdown**: `Morning: 30 L  ·  Evening: 20 L` (if recorded).
    - **Reconciliation equation ("Where did it go? / कहाँ गयो?")**:
      - Sold: `Sold: 25 L` (`बेचेको: २५ लि.`).
      - Allocated: `At home: 5 L`, `Processing: 10 L` (`घरमा: ५ लि.`, `प्रशोधन: १० लि.`).
      - **Unexplained remainder**: Highlighted attention tile (`10 L unexplained` / `१० लि. नखुलेको`) with instant `[Reconcile / मिलाउनुहोस्]` action button.
      - **Settled state**: `All production accounted for` (`सबै उत्पादनको हिसाब मिल्यो`) when unexplained = 0.
    - **Contextual actions**: `[Record production]` and `[Explain use]` buttons on each product card.
- **2. Supplies Section (`farmWorkSuppliesContainer`)**:
  - `farmWorkSuppliesHeaderContainer`: Prominent section header (`सामान / Supplies`) with contextual `[सामान किनेँ / Buy supply]` action button.
  - `farmWorkNoSuppliesText`: Welcoming empty state guiding the farmer to register supplies (feed, fertilizer, medicine).
  - For each supply, a card (`bg_today_card`):
    - **Row Header**: Supply name in bold + remaining quantity (e.g. `Dairy Feed`, `8 bag remaining` / `८ बोरा बाँकी` with green accent).
    - **Movement summary**: `Bought: 10 bag · Used: 2 bag` (`किनेको: १० बोरा · प्रयोग: २ बोरा`).
    - **Contextual actions**: `[Use / प्रयोग गरेँ]` and `[Buy / किनेँ]` buttons preselecting this supply item.
- **3. Retained Compatibility Buttons**:
  - Retained at the bottom for test backwards compatibility.

---

## 3. Data Authority & Financial Independence

All operational metrics derive directly from existing domain authorities:

| UI Metric / Element | Domain Authority | Derivation Method |
|---|---|---|
| **Today Production** | `ProductionRecord` | `farm.productionForDay(today, zone).filter { it.productId == product.id }` |
| **Morning / Evening Sessions** | `ProductionRecord` | `record.session == ProductionSession.MORNING / EVENING` |
| **Sold Quantity** | `ProductSaleDetail` | Linked `ProductSaleDetail` for today's SALE trades for this product |
| **Allocations** | `ProductionAllocation` | `farm.productionAllocations` filtered by product, unit, and day |
| **Unexplained Production** | `ProductionReconciliation` | `produced - sold - sum(allocations)` via `FarmState.productionReconciliation` |
| **Remaining Supply** | `SupplyPurchaseDetail` & `SupplyUsage` | `sum(purchases) - sum(usages)` for each supply |

Physical supply quantities remain completely independent of supplier debts (`PartyLedger`), avoiding misleading combinations of physical units with rupee balances.

---

## 4. Contextual Workflow Routing

- Tapping `[उत्पादन लेख्नुहोस्]` opens `showProductionDialog(productId)` with the active product pre-selected.
- Tapping `[प्रयोग देखाउनुहोस्]` or `[मिलाउनुहोस्]` opens `showProductionAllocationDialog(productId)` with the active product pre-selected.
- Tapping `[प्रयोग गरेँ]` opens `showSupplyUsageDialog(supplyId)` with the active supply pre-selected.
- Tapping `[सामान किनेँ]` opens `showSupplierPurchaseDialog(supplyId)` with the active supply pre-selected.

---

## 5. Today → Farm Work Continuity

- On Today (`आज`):
  - Tapping `todayViewFarmWorkButton` on the `Farm Status` card navigates directly to Farm Work (`Destination.FARM_WORK`).
  - Production and supply states synchronize immediately between Today and Farm Work.

---

## 6. Accessibility, Scaling & Localization

- **Devanagari Safety**: All Nepali strings (`उत्पादन`, `सामान`, `कहाँ गयो?`, `बेचेको`, `प्रयोग`, `नखुलेको`, `सबै उत्पादनको हिसाब मिल्यो`, `बाँकी`) use flexible auto-wrapping layouts without fixed height limits.
- **36sp Large Text**: Production equation lines, session badges, and supply tiles stack comfortably without text clipping at 36sp font scaling.
- **Dark Mode**: High contrast surfaces (`bg_today_card`, `bg_metric_tile`) maintain clarity under dark theme.
- **Touch Targets**: All clickable action buttons meet or exceed the 44/48dp target standard.

---

## 7. Device Verification (Motorola Edge 60 Fusion - Android 16 / ZA22374XPC)

- **Instrumented Test Suites**:
  - `FarmWorkRedesignTest`: **5/5 passed** on device.
  - `FarmKhataRedesignTest`: **9/9 passed** on device.
  - `FarmTodayDashboardRedesignTest`: **7/7 passed** on device.
  - `FarmActivityShellRedesignTest`: **7/7 passed** on device.
  - `FarmOverviewAndHisabDeviceTest`: **5/5 passed** on device.
  - `KisanToolboxDeviceBatteryTest`: **5/5 passed** on device.
  - `FarmActivityShellInsetsTest`: **3/3 passed** on device.
  - `LocalizedResourceResolutionTest`: **15/15 passed** on device.
  - `FarmActivityLocalizationSmokeTest`: **4/4 passed** on device.
  - Total on-device test count: **60/60 passed**.
- **Local Unit Tests**:
  - `./gradlew test`: **379 tests passed** with 0 failures.
  - `LocalizationParityTest`: 100% parity across English and Nepali resources.
- **Lint**: `./gradlew lint` passed with 0 errors.

---

## 8. Known Compromises & Deferred Work

### Known Compromises
- Production and allocation recording dialogs remain modals; full-screen flows will be introduced during transaction/workflow redesign slices.
- Supply movement history is presented as a high-level summary (`Bought: X · Used: Y`) on the card, with full details accessible via dialogs.

### Deferred to Future Milestones
- Multi-day production historical charts.
- Crop field and livestock batch mapping.
- Full-screen production and supply entry wizards.
