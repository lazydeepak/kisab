# UX-08: Hisab, Calculators & Reporting Presentation Implementation

## 1. Overview & Intent

UX-08 delivers the presentation, calculator ergonomics, and review hierarchy overhaul for the Kisab application on branch `feature/ux-08-hisab-reports`, starting from verified UX-07 baseline `e09671a5c6d3648eb1187425f69be8f654b9d0e1`.

### Product Role
Hisab is an on-demand **utility hub**. It is not a competing primary navigation model.
- **Today** answers: *"What matters now?"*
- **Khata** answers: *"Who owes whom?"*
- **Farm Work** answers: *"What happened operationally?"*
- **Record** answers: *"What happened?"*
- **Hisab** answers: *"Help me calculate or review something."*

---

## 2. Findings Inventory & Severity Classification

| Finding ID | Surface | Actual Behavior | Expected Behavior | Severity | Evidence | Resolution |
|---|---|---|---|---|---|---|
| **F-01** | Calculators | Calculator results (`arithmeticResultText`, `profitResultText`, `interestResultText`, `landResultText`, `seedResultText`, `fertilizerResultText`, `feedResultText`, `milkResultText`, `cropYieldResultText`) were plain unbordered text. | Results must have clear visual container hierarchy (`@drawable/bg_metric_tile`), distinct padding, and prominent contrast. | **P2** (Visual) | `view_kisan_calculator_toolbox.xml` and `view_farm_planning.xml`. | Upgraded all 9 calculator result views to use metric tiles with 12dp padding and 16sp bold text. |
| **F-02** | Hisab Screen | Party trade reconciliation container had flat styling without card separation. | Historical party reconciliation should be encapsulated in a distinct card container (`@drawable/bg_today_card`). | **P2** (Visual) | `activity_shell.xml:1740`. | Styled `hisabCalculatorContainer` with `@drawable/bg_today_card` and 16dp padding. |
| **F-03** | Testing | Test suite lacked comprehensive automated coverage for all utility calculators and land converters. | Automated tests should verify all arithmetic, profit/loss, interest, land, and planning calculators. | **P2** (Test Stability) | Test gap analysis. | Implemented `FarmHisabAndReportsRedesignTest.kt`. |

---

## 3. Hierarchy & Tool Grouping

Hisab is organized into three logical functional groups:

### 3.1 General Utility Calculators (`view_kisan_calculator_toolbox.xml`)
- **Arithmetic Calculator**: Fast decimal & signed operations (`+`, `-`, `×`, `÷`, `%`) with zero-division protection.
- **Profit / Loss Calculator**: Cost vs. Revenue margin and markup calculator (strictly isolated as an arithmetic tool without implying general farm accounting profit).
- **Simple Interest Calculator**: Principal, annual interest rate, and months -> Interest amount and Total repayable.
- **Land Converter**: Seamless bidirectional conversion between Nepali Traditional units (Ropani, Aana, Paisa, Daam, Bigha, Kattha, Dhur) and Metric/International units (Square Metres, Square Feet, Acres, Hectares).

### 3.2 Farm Planning Calculators (`view_farm_planning.xml`)
- **Seed Requirement Calculator**: Area, Land Unit, Seed Rate (kg/unit), and Seed Price -> Total Seed Required (kg) and Estimated Cost.
- **Fertilizer Requirement Calculator**: Area, Land Unit, Fertilizer Application Rate, and Price -> Total Fertilizer Required (kg) and Estimated Cost.
- **Feed Requirement Calculator**: Animal count, Feed kg/animal/day, Period days, and Unit price -> Total Feed (kg) and Estimated Cost.
- **Dairy Milk Projection**: Milking animal count, Litres/animal/day, Days, and Price/litre -> Total Production (L) and Projected Revenue.
- **Crop Yield Projection**: Area, Expected Yield Rate, and Selling Price -> Total Expected Yield (kg) and Projected Revenue.

### 3.3 Historical Party Reconciliation & Review (`hisabCalculatorContainer`)
- Deep party-by-party trade and settlement activity review over selected financial periods (`This Month`, `This Year`, `All Time`).
- Position as of today: To Receive (`लिन बाँकी`), To Pay (`तिर्न बाँकी`), Net (`खुद हिसाब`).

---

## 4. Domain & Accounting Boundaries

- **Zero Schema or Persisted Aggregate Changes**: No schema migrations or modifications to existing authorities.
- **Preserved Authorities**: Domain authorities (`FarmSliceService`, `Trade`, `Settlement`, `PartyLedger`, `FarmTransaction`, `FarmSupply`, `SupplyPurchaseDetail`, `SupplyUsage`, `ProductionRecord`, `ProductionAllocation`, `FarmerOverview`) remain authoritative.
- **No False Profit Claims**: The Profit/Loss tool remains strictly an arithmetic scenario calculator and is never conflated with the farm's comprehensive accounting or cash flows on Today.

---

## 5. Physical Device Verification (Motorola Edge 60 Fusion / ZA22374XPC)

- **Instrumented Test Suites**:
  - `FarmHisabAndReportsRedesignTest` (UX-08): **2/2 passed**
  - `FarmSecondarySurfacesRedesignTest` (UX-07): **4/4 passed**
  - `FarmIntegratedPolishTest` (UX-06): **1/1 passed**
  - `FarmRecordFlowsRedesignTest` (UX-05): **5/5 passed**
  - `FarmWorkRedesignTest` (UX-04): **5/5 passed**
  - `FarmKhataRedesignTest` (UX-03): **9/9 passed**
  - `FarmTodayDashboardRedesignTest` (UX-02): **7/7 passed**
  - `FarmActivityShellRedesignTest` (UX-01): **7/7 passed**
  - `FarmOverviewAndHisabDeviceTest`: **5/5 passed**
  - `KisanToolboxDeviceBatteryTest`: **5/5 passed**
  - `FarmActivityShellInsetsTest`: **3/3 passed**
  - `LocalizedResourceResolutionTest`: **15/15 passed**
  - `FarmActivityLocalizationSmokeTest`: **4/4 passed**
  - **Total on-device tests: 72/72 passed**.
- **Unit Tests**:
  - `./gradlew test`: **379 tests passed** (0 failures).
- **Lint**:
  - `./gradlew lint`: **0 errors**.

---

## 6. Final Disposition

```
UX-08 FINAL DISPOSITION: PASS
```
