# UX-03: Khata Redesign Implementation

## 1. Overview and Intent

UX-03 delivers the third implementation slice of the competitive farmer UX redesign defined in `docs/design/COMPETITIVE_FARMER_UX_REDESIGN.md`. Building upon the 5-element shell established in `UX-01` and the operational daily command surface built in `UX-02`, UX-03 transforms **Khata (`खाता`)** into a person-first, directional relationship surface.

### Product Principle
Khata is NOT an accounting ledger, journal, or balance sheet. It directly answers two simple questions:
1. **Who has money outstanding with me?** (Who owes me, or whom do I owe?)
2. **What happened between me and this person?** (Readable relationship timeline without debits, credits, or double-entry jargon).

---

## 2. Old vs. Redesigned Khata Architecture

### Previous Khata (Hisab-Kitab) Structure
- Heavy accounting overview table (`financialOverviewContainer`) competing with parties and trades.
- Fragmented trade and party lists with generic labels and competing action buttons.
- Party balances were presented with raw plus/minus numbers or ambiguous netting.
- History was rendered with journal-like headers (`SALE`, `PURCHASE`, `DR`, `CR`).

### Redesigned Khata Structure
- **1. Search & Filter Bar (`khataOverviewContainer`)**:
  - `khataSearchInput`: Live person/business name and phone number search.
  - `hisabSummaryContainer`: Two distinct, directional summary cards:
    - `लिन बाँकी (ग्राहकबाट)`: Total to receive from customers (green).
    - `तिर्न बाँकी (आपूर्तिकर्तालाई)`: Total to pay to suppliers (red/coral).
  - `khataFilterRadioGroup`: Segmented filter `[सबै / All] [लिन बाँकी / To receive] [तिर्न बाँकी / To pay]`.
  - `partiesContainer`: Clean party list sorted by outstanding priority (highest balance first), followed by settled parties.
- **2. Party Row Contract (`item_party_row.xml`)**:
  - Person/business name in bold.
  - Direction and amount clearly stated: `लिन बाँकी रु १,५००` / `तिर्न बाँकी रु २,०००` / `हिसाब चुक्ता`.
  - Secondary fact: role badge (`ग्राहक` / `आपूर्तिकर्ता`) and contact number.
- **3. Party Detail Surface (`partyKhataContainer`)**:
  - `partyKhataTitle`: Prominent party name.
  - `partyKhataRoleText`: Role and contact subtitle.
  - `partyKhataHeadlineCard`: Directional headline (`अहिले लिन बाँकी: रु १,०००` / `अहिले तिर्न बाँकी: रु २,०००` / `हिसाब चुक्ता (रु ०)`).
  - Contextual action buttons:
    - `khataContextualReceiveButton`: `[पैसा पाएँ / Received money]` (displayed when toReceive > 0).
    - `khataContextualPayButton`: `[पैसा तिरेँ / Paid money]` (displayed when toPay > 0).
  - Secondary actions: `[बेचेँ / Sell]`, `[किनेँ / Buy]`, `[सम्पादन / Edit party]`.
  - `partyKhataHistoryLabel` & `khataEntriesContainer`: Chronological relationship timeline with farmer verbs.

---

## 3. Data Authority & Financial Integrity

All figures are projected directly from established domain authorities without schema alterations:

| UI Element / Metric | Domain Authority | Computation Method |
|---|---|---|
| **Total to Receive (`लिन बाँकी`)** | `PartyLedger` | `farm.parties.fold(0L) { acc, p -> acc + p.toReceiveMinor }` |
| **Total to Pay (`तिर्न बाँकी`)** | `PartyLedger` | `farm.parties.fold(0L) { acc, p -> acc + p.toPayMinor }` |
| **Party Outstanding Position** | `PartyLedgerSummary` | `partyLedgerSummary(partyId)` (`toReceiveMinor`, `toPayMinor`) |
| **Relationship Timeline** | `PartyLedger` | `partyLedger(partyId).entries` (oldest-to-newest running balances, rendered newest-first) |
| **Product Sale Details** | `ProductSaleDetail` | Linked via `tradeId` to show product name, normalized quantity, unit, and rate |
| **Supply Purchase Details** | `SupplyPurchaseDetail` | Linked via `tradeId` to show supply item name and purchase details |

No net arithmetic is displayed prominently; receivables and payables remain distinct and directional.

---

## 4. Contextual Payment Routing

- Tapping `khataContextualReceiveButton` (`पैसा पाएँ`) from a customer's Khata opens `showReceivedMoneyDialog()` with the active party pre-selected.
- Tapping `khataContextualPayButton` (`पैसा तिरेँ`) from a supplier's Khata opens `showSupplierPaymentDialog()` with the active party pre-selected.
- Recording a payment automatically triggers a refresh of the party ledger and headline immediately.

---

## 5. Today → Khata Continuity

- On Today (`आज`):
  - Tapping `todayViewReceivablesButton` (`लिन बाँकी` card) routes directly to Khata with the `TO_RECEIVE` filter active.
  - Tapping `todayViewPayablesButton` (`तिर्न बाँकी` card) routes directly to Khata with the `TO_PAY` filter active.
- Filter selection is preserved across activity recreation (`onSaveInstanceState` / `STATE_KHATA_FILTER`).

---

## 6. Accessibility, Scaling & Localization

- **Devanagari Safety**: All Nepali strings (`लिन बाँकी`, `तिर्न बाँकी`, `हिसाब चुक्ता`, `पैसा पाएँ`, `पैसा तिरेँ`, `बेचेको`, `किनेको`) use flexible auto-wrapping TextViews without fixed height restrictions.
- **36sp Large Text**: Party rows, summary tiles, and timeline entries stack cleanly at 36sp without clipping.
- **Dark Mode**: Contrast ratio exceeds 4.5:1 across surfaces (`receivableBackground` `#1C2E20`, `payableBackground` `#2E1C1C`).
- **Touch Targets**: All clickable targets meet or exceed the 44/48dp accessibility standard.

---

## 7. Device Verification (Motorola Edge 60 Fusion - Android 16 / ZA22374XPC)

- **Instrumented Test Suites**:
  - `FarmKhataRedesignTest`: **9/9 passed** on device.
  - `FarmTodayDashboardRedesignTest`: **7/7 passed** on device.
  - `FarmActivityShellRedesignTest`: **7/7 passed** on device.
  - `FarmOverviewAndHisabDeviceTest`: **5/5 passed** on device.
  - `KisanToolboxDeviceBatteryTest`: **5/5 passed** on device.
  - `FarmActivityShellInsetsTest`: **3/3 passed** on device.
  - `LocalizedResourceResolutionTest`: **15/15 passed** on device.
  - `FarmActivityLocalizationSmokeTest`: **4/4 passed** on device.
  - Total on-device test count: **55/55 passed**.
- **Local Unit Tests**:
  - `./gradlew test`: **379 tests passed** with 0 failures.
  - `LocalizationParityTest`: 100% parity across English and Nepali resources.
- **Lint**: `./gradlew lint` passed with 0 errors.

---

## 8. Known Compromises & Deferred Work

### Known Compromises
- The generic party creation/editing form remains an inline section at the bottom of Khata; dedicated full-screen creation workflows belong in subsequent milestone passes.
- Settlement/payment adjustments continue to use existing dialogs rather than full-page sheets.

### Deferred to Future Milestones
- Advanced Khata filtering by date/month preset inside party detail.
- Multi-party batch statements or WhatsApp/SMS shareable Khata slips.
- Full-screen sale/purchase recording workflows.
