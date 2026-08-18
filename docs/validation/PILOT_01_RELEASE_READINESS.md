# PILOT-01: Release Candidate Audit & Pilot Readiness

## 1. Executive Summary & Candidate Identity

- **Release Candidate Baseline**: `875a2180c44122d26f633e7ae9a9f237bf4258ae` (Verified UX-09 HEAD)
- **Validation Branch**: `validation/pilot-01-release-readiness`
- **Application ID**: `com.susankhya.kisab`
- **Version Name**: `0.2.0`
- **Version Code**: `3`
- **Minimum SDK**: `26` (Android 8.0 Oreo)
- **Target SDK**: `36` (Android 16)
- **Debug Artifact**: `app/build/outputs/apk/debug/app-debug.apk`
- **APK SHA-256**: `2c1103f2dbd074a57a29e5c3ebcafa757bbac5480c6eaf117eb1b90da50f8476`
- **Target Physical Device**: Motorola Edge 60 Fusion (`ZA22374XPC` / Android 16)
- **Disposition**: `READY_FOR_PILOT`

---

## 2. Automated Quality & Regression Audit

| Verification Layer | Test Count | Failures | Status |
|---|---|---|---|
| **JVM Unit Tests (`./gradlew test`)** | 379 | 0 | **PASS** |
| **Android Lint (`./gradlew lint`)** | Standard Rules | 0 Errors | **PASS** |
| **Connected Device Instrumentation (`ZA22374XPC`)** | 73 | 0 | **PASS** |
| **Debug Build Packaging (`assembleDebug`)** | 1 Artifact | 0 Errors | **PASS** |

### Suite-by-Suite Connected Test Verification
1. `FarmVisualRefinementTest` (UX-09): 1/1 passed
2. `FarmHisabAndReportsRedesignTest` (UX-08): 2/2 passed
3. `FarmSecondarySurfacesRedesignTest` (UX-07): 4/4 passed
4. `FarmIntegratedPolishTest` (UX-06): 1/1 passed
5. `FarmRecordFlowsRedesignTest` (UX-05): 5/5 passed
6. `FarmWorkRedesignTest` (UX-04): 5/5 passed
7. `FarmKhataRedesignTest` (UX-03): 9/9 passed
8. `FarmTodayDashboardRedesignTest` (UX-02): 7/7 passed
9. `FarmActivityShellRedesignTest` (UX-01): 7/7 passed
10. `FarmOverviewAndHisabDeviceTest`: 5/5 passed
11. `KisanToolboxDeviceBatteryTest`: 5/5 passed
12. `FarmActivityShellInsetsTest`: 3/3 passed
13. `LocalizedResourceResolutionTest`: 15/15 passed
14. `FarmActivityLocalizationSmokeTest`: 4/4 passed

---

## 3. Physical Daily Journey & System Validation

### Daily Journey Execution (`Pilot01Farm`)
1. **Creation & Identity**: Created disposable `Pilot01Farm` (NPR currency). App-bar reflects active farm instantly.
2. **Production Logging**: Recorded Morning (50 L) and Evening (30 L) Cow Milk production.
3. **Product Sale with Partial Due**: Sold 40 L at NPR 100/L to customer "Sita Dairy". Paid NPR 2,500 cash; NPR 1,500 on credit. Live arithmetic equation displayed `40 × 100 = 4,000 (नगद: 2,500, बाँकी: 1,500)`.
4. **Customer Debt Collection**: Opened Khata -> Sita Dairy. Tapped contextual `[पैसा पाएँ]`, utilized `[पूरै रकम]` shortcut to settle NPR 1,500. Balance settled cleanly to NPR 0.
5. **Supply Purchase with Partial Payable**: Purchased 10 bags of "Poultry Feed" from supplier "Bikash Agro" for NPR 25,000. Paid NPR 15,000 cash; NPR 10,000 on credit.
6. **Supply Usage & Physical Stock**: Recorded usage of 3 bags. Stock decreased cleanly to 7 bags remaining with overuse guard active.
7. **Supplier Payment Settlement**: Settled NPR 10,000 payable to Bikash Agro via `[पैसा तिरेँ]`. Payable updated to NPR 0.
8. **Reconciliation & Unexplained Production**: Farm Work tab displays Cow Milk production (80 L), sold (40 L), unexplained balance (40 L). Allocated 20 L to household/processing via `[प्रयोग देखाउनुहोस्]`, dropping unexplained balance to 20 L.
9. **Multi-Farm Isolation & Persistence**: Switched between farms and performed process death / cold relaunch. Zero cross-farm data leakage; all records preserved intact.

---

## 4. Release Blockers & Findings

- **P0 Blockers (Data Loss / Accounting Corruption)**: **0 Unresolved**
- **P1 Blockers (Major Workflow Failure)**: **0 Unresolved**
- **P2 Usability & Visual Inconsistencies**: All identified issues corrected in UX-01 through UX-09.
- **P3 Minor Polish**: Documented for observation during farmer pilot sessions.

---

## 5. Destructive Safeguards & Backup Verification

- **Backup Export**: JSON file export executes seamlessly via system document picker.
- **Danger Zone Protection**: Reset Farm Data and Delete Farm enforce:
  1. Multi-step warning modal.
  2. Backup freshness verification gate.
  3. Strict typed uppercase confirmation ("RESET" / "DELETE").
- **Protected State**: `RC01UpgradeFarm` remained untouched throughout all audit procedures.

---

## 6. Pilot Recommendation

```
PILOT-01 FINAL DISPOSITION: READY_FOR_PILOT
```
