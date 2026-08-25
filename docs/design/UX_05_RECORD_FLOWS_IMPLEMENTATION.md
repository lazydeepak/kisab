# UX-05: Record & Entry Flows Redesign Implementation

## 1. Overview and Intent

UX-05 delivers the fifth implementation slice of the competitive farmer UX redesign defined in `docs/design/COMPETITIVE_FARMER_UX_REDESIGN.md`. Building upon the 5-element shell (`UX-01`), the Today dashboard (`UX-02`), Khata (`UX-03`), and Farm Work (`UX-04`), UX-05 redesigns the central **Record (`+`)** experience and all six core transactional task flows:
1. **Sell — `बेचेँ`** (Product sale with live equation, customer rate suggestion, payment methods, and Sell Again)
2. **Bought — `किनेँ`** (Supply purchase with cash vs. supplier support, live balance, and stock update)
3. **Received Money — `पैसा पाएँ`** (Customer debt collection with full-amount shortcut and overpayment guards)
4. **Paid Money — `पैसा तिरेँ`** (Supplier debt settlement with full-amount shortcut)
5. **Used — `प्रयोग गरेँ`** (Physical supply usage with remaining stock verification and overuse guard)
6. **Production — `उत्पादन`** (Morning / Evening / Other session production with today reconciliation equation)

### Product Principle
The Record experience feels like answering **"What happened on the farm?"** rather than filling out an accounting journal. The farmer chooses an action, enters minimal necessary operational facts, sees the live arithmetic result before saving, and receives immediate confirmation.

---

## 2. Old Workflow Friction vs. Redesigned Task Flows

### Previous Workflow Friction
- Nested alert dialogs where adding a new customer or product inline required closing or losing the in-progress sale form.
- Hardcoded dialog layout heights that were obscured or unscrollable when the Android soft keyboard opened.
- Missing live arithmetic: farmers had to calculate `Quantity × Rate` in their head or guess the remaining credit balance.
- No convenient backdating affordance in entry dialogs.

### Redesigned Interaction Architecture
- **1. ScrollView Wrapped Containers**: All entry dialogs wrap their content in full-viewport `ScrollView` with generous 20dp padding, allowing complete scrollability even with tall IME software keyboards open.
- **2. Live Arithmetic Summary Tiles (`bg_metric_tile`)**: As the farmer types quantities, rates, or partial amounts, a live calculation tile updates instantly (e.g. `५ लि. × रु १०० = रु ५००.०० (नगद: रु ३००, बाँकी: रु २००)`).
- **3. Context-Preserving Inline Creation**: Adding a new Customer, Supplier, Product, or Supply inline keeps the parent form's entered quantity and rate intact and automatically selects the newly created entity upon return.
- **4. Explicit Date/Time Adjustment (`[मिति वा समय बदल्नुहोस्]`)**: Secondary action button defaulting to current time with native date/time pickers for backdated entries.
- **5. Specific Action Button Labels**: Clear task-based primary actions (`बिक्री राख्नुहोस्`, `खरिद राख्नुहोस्`, `पैसा राख्नुहोस्`, `भुक्तानी राख्नुहोस्`, `प्रयोग राख्नुहोस्`, `उत्पादन राख्नुहोस्`).

---

## 3. Detailed Specification of the 6 Flows

### 3.1 Sell — `बेचेँ` (`showQuickSaleDialog`)
- **Field Sequence**:
  1. Customer selector (Spinner + `[+ थप्नुहोस्]` button + live receivable balance warning).
  2. Product selector (Spinner with unit labels + `[+ थप्नुहोस्]` button).
  3. Quantity input (Decimal numeric keyboard).
  4. Rate input (Prefilled with customer/product historical suggestion via `ProductSaleHistory`, editable).
  5. Live equation summary: `Quantity × Rate = Total`.
  6. Payment method: `[पूरै पैसा आयो / Full cash]`, `[उधार / Credit]`, `[केही पैसा आयो / Partial]` with live remaining balance calculation.
  7. Date/time adjustment button.
- **Primary Action**: `बिक्री राख्नुहोस्` (`Save sale`).
- **Repetitive Entry**: Shows `Sell Again` modal for rapid subsequent sales to the same customer.

### 3.2 Bought — `किनेँ` (`showSupplierPurchaseDialog`)
- **Field Sequence**:
  1. Supplier selector (Spinner including `Cash (no supplier)` + `[+ थप्नुहोस्]` button).
  2. Supply selector (Spinner with unit labels + `[+ थप्नुहोस्]` button).
  3. Quantity input (Decimal numeric).
  4. Total cost input (Decimal numeric).
  5. Payment method: `[पूरै तिरेँ / Paid in full]`, `[उधार / Credit]`, `[केही तिरेँ / Partial]` with remaining payable calculation.
  6. Date/time adjustment button.
- **Primary Action**: `खरिद राख्नुहोस्` (`Save purchase`).
- **Authority Integrity**: Cash purchases create an EXPENSE `FarmTransaction` + `SupplyPurchaseDetail`; supplier purchases create a PURCHASE `Trade` + `Settlement` + `SupplyPurchaseDetail`.

### 3.3 Received Money — `पैसा पाएँ` (`showReceivedMoneyDialog`)
- **Field Sequence**:
  1. Customer selector (Spinner with customers).
  2. Current outstanding balance badge: `लिन बाँकी: रु ...`.
  3. Amount input (Decimal numeric) + `[पूरै रकम / Full amount]` shortcut button.
  4. Date/time adjustment button.
- **Primary Action**: `पैसा राख्नुहोस्` (`Save received money`).
- **Validation**: Enforces positive amount <= outstanding receivable with clear error feedback.

### 3.4 Paid Money — `पैसा तिरेँ` (`showSupplierPaymentDialog`)
- **Field Sequence**:
  1. Supplier selector (Spinner with suppliers).
  2. Current outstanding payable badge: `तिर्न बाँकी: रु ...`.
  3. Amount input (Decimal numeric) + `[पूरै रकम / Full amount]` shortcut button.
  4. Date/time adjustment button.
- **Primary Action**: `भुक्तानी राख्नुहोस्` (`Save payment`).
- **Validation**: Enforces positive amount <= outstanding payable.

### 3.5 Used — `प्रयोग गरेँ` (`showSupplyUsageDialog`)
- **Field Sequence**:
  1. Supply selector (Spinner).
  2. Available stock indicator: `बाँकी: X बोरा`.
  3. Quantity used input (Decimal numeric).
  4. Optional note input.
  5. Date/time adjustment button.
- **Primary Action**: `प्रयोग राख्नुहोस्` (`Save usage`).
- **Validation**: Guards against using more quantity than physically available (`supply_usage_too_high`).

### 3.6 Production — `उत्पादन` (`showProductionDialog`)
- **Field Sequence**:
  1. Product selector (Spinner + `[+ थप्नुहोस्]` button).
  2. Session RadioGroup: `[बिहान / Morning]`, `[बेलुका / Evening]`, `[अन्य / Other]`.
  3. Quantity produced input (Decimal numeric).
  4. Today product reconciliation summary equation.
  5. Date/time adjustment button.
- **Primary Action**: `उत्पादन राख्नुहोस्` (`Save production`).
- **Upsert Semantics**: Morning/Evening entries upsert existing session records; Other entries create distinct timestamped records.

---

## 4. Accessibility, Scaling & Keyboard Ergonomics

- **Keyboard Resilience**: Form fields remain fully visible and scrollable above soft keyboard.
- **36sp Large Text**: All dialogs call `scaleDialogContent(dialog)`, preserving typography hierarchy without clipping.
- **Devanagari Rendering**: Auto-wrapping text containers prevent Nepali matra clipping.
- **Touch Targets**: All interactive elements (Spinners, RadioButtons, Action Buttons) exceed 44/48dp target minimums.

---

## 5. Device Verification (Motorola Edge 60 Fusion - Android 16 / ZA22374XPC)

- **Instrumented Test Suites**:
  - `FarmRecordFlowsRedesignTest`: **5/5 passed** on device.
  - `FarmWorkRedesignTest`: **5/5 passed** on device.
  - `FarmKhataRedesignTest`: **9/9 passed** on device.
  - `FarmTodayDashboardRedesignTest`: **7/7 passed** on device.
  - `FarmActivityShellRedesignTest`: **7/7 passed** on device.
  - `FarmOverviewAndHisabDeviceTest`: **5/5 passed** on device.
  - `KisanToolboxDeviceBatteryTest`: **5/5 passed** on device.
  - `FarmActivityShellInsetsTest`: **3/3 passed** on device.
  - `LocalizedResourceResolutionTest`: **15/15 passed** on device.
  - `FarmActivityLocalizationSmokeTest`: **4/4 passed** on device.
  - Total on-device test count: **65/65 passed**.
- **Local Unit Tests**:
  - `./gradlew test`: **379 tests passed** with 0 failures.
  - `LocalizationParityTest`: 100% parity across English and Nepali resources.
- **Lint**: `./gradlew lint` passed with 0 errors.

---

## 6. Known Compromises & Deferred Work

### Known Compromises
- Full-screen wizard navigation flows remain modal sheets/dialogs for backward test compatibility.

### Deferred to Future Milestones
- OCR receipt scanning or barcode entry.
- Multi-currency transaction splits.
- Voice-assisted recording shortcuts.
