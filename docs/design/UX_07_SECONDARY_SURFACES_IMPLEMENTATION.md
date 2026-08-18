# UX-07: Secondary Surfaces — More, Farms & Settings Implementation

## 1. Overview & Intent

UX-07 delivers the secondary surfaces overhaul for the Kisab application on branch `feature/ux-07-secondary-surfaces`, building upon the verified UX-06 baseline `121458cf680190807b557c6b9bb7cb84a6225cf3`.

The primary objective of UX-07 is to transform the secondary screens (**More**, **Farms**, **Farm Details**, **Settings**, and **Backup/Restore**) into cohesive, first-class experiences aligned with the modern visual design, margin rhythm (20dp), typographic hierarchy, text scaling (up to 36sp), and cross-farm data safety standards established in UX-01 through UX-06.

---

## 2. Findings Inventory & Severity Classification

| Finding ID | Surface | Actual Behavior | Expected Behavior | Severity | Evidence | Resolution |
|---|---|---|---|---|---|---|
| **F-01** | Add Farm Screen | `createFarmFromAddScreen()` did not register the new farm ID in `FarmSliceService.setCurrentFarmId()` and did not clear dirty editor states. | New farm must be active in service and all stale editor state cleared immediately. | **P0** (Data Safety) | Code review in `FarmActivity.kt:5893`. | Added `service.setCurrentFarmId(farm.id)` and reset `khataPartyId`, `editingPartyId`, `tradeEditorState`, `settlementEditorState`. |
| **F-02** | Farm Details Screen | `switchToManagedFarm()` did not reset `khataPartyId` or active party editor state. | Switching farms from farm details must completely clear active party/trade selections from previous farm. | **P0** (Data Safety) | Code review in `FarmActivity.kt:5913`. | Cleared all active Khata/editor state on farm switch. |
| **F-03** | Dialogs / Sizing | Secondary dialogs (`showAboutDialog`, `showRenameFarmDialog`, `showCurrencyChooser`, `showDiscardDialog`, `showResetFarmDataConfirmation`, `showDeleteFarmConfirmation`) lacked universal `scaleDialogContent(dialog)`. | All alert dialogs across secondary surfaces must scale dynamically with app text size up to 36sp. | **P1** (Accessibility) | Dialog inspection in `FarmActivity.kt`. | Added `scaleDialogContent(dialog)` across all secondary dialogs. |
| **F-04** | Farms Screen | Farms list had flat rows without distinct multi-farm highlight card styles. | Active farm must be unmistakably highlighted with active badge and clean card separation. | **P2** (Visual) | `farmsListContainer` rendering in `FarmActivity.kt:5797`. | Modernized farm card layout with `@drawable/bg_today_card` and prominent badge. |
| **F-05** | Test Sync | `KisanToolboxDeviceBatteryTest` navigation lacked idle synchronization in multi-suite test batches. | Instrumentation tests should wait for UI layout idle after navigating from More. | **P2** (Test Stability) | Connected test failure in 70-test batch. | Added `waitForIdle()` after navigation clicks in test helper. |

---

## 3. Detailed Component Implementations

### 3.1 More Hub (`moreScreen`)
- Structured secondary navigation hub with 20dp margin rhythm.
- Organized into clear functional groupings:
  1. **Management & Farm Tools (`फार्म व्यवस्थापन र औजार`)**:
     - `Hisab & Planning Tools (`हिसाब र योजना औजार`)` -> `hisabScreen`
     - `Farms Management (`फार्महरू`)` -> `farmsScreen`
  2. **System & Data (`डाटा र सेटिङ`)**:
     - `Settings (`सेटिङ`)` -> `settingsScreen`
     - `Backup & Restore (`ब्याकअप`)` -> Direct scroll to Backup section in Settings
     - `About Kisab (`किसाबको बारेमा`)` -> Native about dialog with version and offline notice
- Back navigation predictably returns to the previous primary tab or Today.

### 3.2 Farms & Multi-Farm Management (`farmsScreen`)
- List of all local farms with distinct card separation.
- Active farm prominently highlighted with `✓ सक्रिय फार्म` (`✓ Active Farm`) badge and direct "Details" button.
- Other farms display one-tap "Switch to this farm" (`यो फार्म खोल्नुहोस्`) and "Details" (`विवरण`).
- Direct "+ Add Farm" action leading to the streamlined creation flow.
- Complete data isolation enforced upon farm creation or switching.

### 3.3 Farm Details & Danger Zone (`farmDetailsScreen`)
- Top Information Card: Farm name, currency symbol & code, and active status.
- Standard Operations: Rename Farm (`फार्मको नाम फेर्नुहोस्`) and Change Currency (`मुद्रा बदल्नुहोस्`, guarded by trade lockouts).
- **Danger Zone (`खतरनाक क्षेत्र / Danger Zone`)**:
  - Encapsulated in `@drawable/bg_danger_zone_panel` with warning notice.
  - Reset Farm Data (`फार्मको डाटा मेटाउनुहोस्`) and Delete Farm (`फार्म हटाउनुहोस्`).
  - Strict preservation of all backup freshness gates, typed confirmation requirements ("RESET" / "DELETE"), and multi-farm safety rules.

### 3.4 Settings (`settingsScreen`)
- Clean grouping into four standard sections:
  1. **Language (`भाषा / Language`)**: Follow Device, English, Nepali.
  2. **Appearance & Text Scaling (`रुप र अक्षरको आकार`)**: Follow System, Light, Dark mode; App text size slider (14sp - 36sp live scaling); Currency symbol and Number grouping toggles.
  3. **Notifications & Alerts (`सूचनाहरू`)**: Daily reminder toggle and system notification permission status.
  4. **Data & Backup (`डाटा र ब्याकअप`)**: Device-local offline storage note, Export Backup (`ब्याकअप फाइल बनाउनुहोस्`), Import Backup (`ब्याकअपबाट ल्याउनुहोस्`).

---

## 4. Domain & Architecture Safety

- **No Schema Changes**: No database migrations or schema adjustments.
- **Authority Preservation**: Preserved all domain authorities (`FarmSliceService`, `Trade`, `Settlement`, `PartyLedger`, `FarmTransaction`, `FarmSupply`, `SupplyPurchaseDetail`, `SupplyUsage`, `ProductionRecord`, `ProductionAllocation`, `FarmerOverview`, `BackupFreshness`).
- **Data Protection**: `RC01UpgradeFarm` remained untouched throughout all tests and audit passes.

---

## 5. Physical Device Verification (Motorola Edge 60 Fusion / ZA22374XPC)

- **Instrumented Test Suites**:
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
  - **Total on-device tests: 70/70 passed**.
- **Unit Tests**:
  - `./gradlew test`: **379 tests passed** (0 failures).
- **Lint**:
  - `./gradlew lint`: **0 errors**.

---

## 6. Final Disposition

```
UX-07 FINAL DISPOSITION: PASS
```
