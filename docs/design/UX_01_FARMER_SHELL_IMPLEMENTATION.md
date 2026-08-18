# UX-01: Farmer Shell and Information Architecture Implementation

## 1. Overview and Intent

UX-01 delivers the first implementation slice of the competitive farmer UX redesign defined in `docs/design/COMPETITIVE_FARMER_UX_REDESIGN.md`. It establishes the new 5-element primary shell:

```text
Today (आज) | Khata (खाता) | + Record (लेख्नुहोस्) | Farm Work (फार्मको काम) | More (अरू)
```

The shell shifts the application from a multi-screen accounting utility into a day-first farmer workflow platform.

### Physical Terminology Baseline
All terms adhere strictly to the validated `UX-00` terminology baseline:
- **Today (`आज`)**: Validated physical time baseline.
- **Khata (`खाता`)**: Validated physical ledger baseline.
- **Record (`लेख्नुहोस्`)**: Provisional action verb (distinct action, easily adaptable).
- **Farm Work (`फार्मको काम`)**: Provisional operational destination.
- **More (`अरू`)**: Validated navigation bucket for tools, management, and settings.

---

## 2. Old vs. New Shell Architecture

### Old Shell (`Home | Hisab-Kitab | Hisab` + Overflow Menu)
- **Home**: Overloaded scrollview mixing farm status, generic income/expense actions, 7 quick operational buttons, today's summary text block, transaction editor, recent transaction list, and farm tools drawer.
- **Hisab-Kitab**: Mixed financial overview and party ledger.
- **Hisab**: Toolbox calculators alongside party period reconciliation.
- **Overflow Menu**: Entry point for Settings, Farms, Backup/Restore, and About.

### New Shell (`Today | Khata | + Record | Farm Work | More`)
- **Today (`navTodayItem`)**: Top-level farm status, farmer overview summary, and day-to-day context.
- **Khata (`navKhataItem`)**: Clean party ledger authority for customer and supplier balances (`लिन बाँकी` / `तिर्न बाँकी`), party histories, and trade entries.
- **Record (`navRecordItem`)**: Central elevated action button opening a bottom-anchored action sheet with 6 farmer verbs. It is an **action**, not a destination, and never holds selected navigation state.
- **Farm Work (`navFarmWorkItem`)**: Dedicated operational surface for production recording, production allocation/usage, and supplies management (bought, used, remaining).
- **More (`navMoreItem`)**: Unified hub for secondary and administrative surfaces:
  - **Hisab**: Kisan calculators (arithmetic, profit, interest, land converter, seed, fertilizer, feed, milk, crop yield) and planning tools.
  - **Farms**: Multi-farm management, farm switcher, and farm details.
  - **Settings**: Appearance (theme, font scaling, currency formatting), language choice, and notification controls.
  - **Backup / Restore**: Deep link to canonical Data & Backup section in Settings.
  - **About Kisab**: Version and build details dialog.

---

## 3. Destination Ownership Matrix

| Destination | Resource ID | Meaning & Primary Responsibility | Excluded Responsibilities |
|---|---|---|---|
| **Today** (`आज`) | `@id/navTodayItem` | What happened today and what needs attention; daily overview. | No calculator tools or deep administrative controls. |
| **Khata** (`खाता`) | `@id/navKhataItem` | People and businesses with money to receive or pay. | No production records or inventory stock. |
| **Record** (`लेख्नुहोस्`) | `@id/navRecordItem` | Immediate 2-tap entry point for common farmer events. | Owns no screens, no persistent content, no selection. |
| **Farm Work** (`फार्मको काम`) | `@id/navFarmWorkItem` | Physical farm output and inputs: Production & Supplies. | No financial position, net profit, or ledger balances. |
| **More** (`अरू`) | `@id/navMoreItem` | Review, calculation tools, multi-farm administration, app settings. | No duplicate daily actions. |

---

## 4. Record Action Contract & Workflow Routing

Tapping the central **Record (`+`)** button displays a custom bottom-anchored modal dialog (`R.layout.record_action_sheet`) with 6 distinct farmer verbs categorized by work domain:

```text
[के भयो? / What happened?]

फार्मको उत्पादन (Farm output)
  [ उत्पादन ] (Production)             -> showProductionDialog()

बिक्री र भुक्तानी (Sales & payments)
  [ बेचेँ ] (Sell)                    -> showQuickSaleDialog()
  [ पैसा पाएँ ] (Received money)      -> showReceivedMoneyDialog()

सामान (Supplies)
  [ किनेँ ] (Bought)                  -> showSupplierPurchaseDialog()
  [ प्रयोग गरेँ ] (Used)              -> showSupplyUsageDialog()

अन्य पैसा (Other money)
  [ पैसा तिरेँ ] (Paid money)         -> showSupplierPaymentDialog()

[ रद्द गर्नुहोस् / Cancel ]
```

### Domain Authority Invariance
All 6 verbs route directly into existing, battle-tested domain and dialog entry points without altering underlying schema or domain authorities:
- `Trade` & `Settlement`
- `PartyLedger`
- `FarmTransaction`
- `ProductSaleDetail`
- `ProductionRecord` & `ProductionAllocation`
- `FarmSupply` & `SupplyPurchaseDetail`
- `FarmerOverview`

---

## 5. Navigation, Back-Stack, and Recreation Rules

1. **Primary Destination Root**: `TODAY` is the home root. Pressing back from `TODAY` finishes the activity.
2. **Tab Switching Back-Stack**: Pressing back from primary tabs `KHATA`, `FARM_WORK`, or `MORE` returns to `TODAY`.
3. **Secondary Surfaces**: Opening child screens (`HISAB`, `SETTINGS`, `FARMS`) sets their back target to `lastPrimaryDestination` (or `MORE` for Hisab).
4. **Child Farm Screens**: `FARM_DETAILS` and `ADD_FARM` return to `FARMS`.
5. **Discard Protection**: Any uncommitted edits in transaction, trade, party, or settlement editors prompt the user for discard confirmation before navigating away.
6. **Recreation (`onSaveInstanceState`)**:
   - `Destination` enum (`TODAY`, `KHATA`, `FARM_WORK`, `MORE`, `HISAB`, `SETTINGS`, `FARMS`, `FARM_DETAILS`, `ADD_FARM`) is saved and restored across process death and orientation changes.
   - `lastPrimaryDestination` is persisted so returning from Settings/Hisab correctly restores the active primary tab.
   - Selected navigation highlight accurately mirrors the active destination; `navRecordItem` is never selected.

---

## 6. Accessibility & Visual Design

- **Touch Targets**: Standard bottom navigation items have `minHeight="64dp"`. The central Record button is elevated with `minHeight="72dp"` and an enlarged `44dp` circular action affordance.
- **Devanagari Support**: Text sizes and line heights are tested to prevent glyph clipping for conjuncts, matras, and complex Devanagari characters (`आज`, `खाता`, `लेख्नुहोस्`, `फार्मको काम`, `अरू`).
- **Text Scaling**: All shell components and dialogs respect `AppTextSizePreferences` via `applyAppTextSize()` and `applyTextScale()`, scaling up to `36sp`.
- **System Bar Insets**: `bottomNavigation` spans edge-to-edge and clears gesture and navigation bar insets cleanly.
- **Theme Support**: Dedicated color palettes for light (`#f1f8e9` background, `#2E7D32` record) and dark modes (`#162a1b` background, `#66BB6A` record, `#2E4A35` selected pill).

---

## 7. Device Verification (Motorola Edge 60 Fusion - Android 16 / ZA22374XPC)

Automated and on-device validation confirmed:
1. **Instrumented Test Suite**:
   - `FarmActivityShellRedesignTest`: 7/7 tests passed on device.
   - `FarmOverviewAndHisabDeviceTest`: 5/5 tests passed on device.
   - `KisanToolboxDeviceBatteryTest`: 5/5 tests passed on device.
   - `FarmActivityShellInsetsTest`: 3/3 tests passed on device.
   - `LocalizedResourceResolutionTest`: 15/15 tests passed on device.
2. **Local Unit Tests**:
   - `LocalizationParityTest`: All English and Nepali resource keys match 1:1 with zero untranslated or missing keys.
3. **Interactive Device Validation**:
   - Installed debug build via `./gradlew installDebug`.
   - Verified 5-item navigation, active state pill highlights, and touch target bounds in portrait and landscape (`1080x2400` / `2400x1080`).
   - Verified Record bottom sheet opens, presents 6 verbs in English and Nepali, and launches dialogs.
   - Verified language toggling (English <-> Nepali) and theme switching.

---

## 8. Rollback Boundary & Deferred Work (UX-02)

### Rollback Boundary
All changes are isolated on branch `feature/ux-01-farmer-shell` based on `d1d5db7`. Underlying domain logic, databases, shared preferences keys, and calculation authorities remain 100% backward-compatible.

### Deferred to UX-02
- Restructuring the Today destination scroll into modern overview cards.
- Multi-farm quick-switcher dropdown directly inside the app bar.
- Tabbed/segmented filters (`लिन बाँकी` / `तिर्न बाँकी` / `सबै`) inside Khata.
- Full-screen task flow redesign for product sale, supply purchase, and production allocation dialogs.
