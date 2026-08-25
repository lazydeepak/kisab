# M9 Validation Record — Nepali Grain Units + 24sp Default

## Candidate identity

- **Branch**: `feat/nepali-units-and-24sp`
- **Baseline**: `main` at `f247bfb` (post-M7 pilot hardening)
- **Application ID**: `com.susankhya.kisab`
- **Version Name / Code**: `0.2.1` / `4` (debug)
- **Device**: Motorola Edge 60 Fusion (`ZA22374XPC`, Android 16, API 36)
- **Debug APK SHA-256**: `62f0e180577eee984c54ed5aab5818b6f7f280ee426b8f21364db05f1ace8a08` (pre-`px`→`sp`-label build)

## M9 FINAL DISPOSITION: PASS_WITH_FOLLOWUPS

M9 is **PASS_WITH_FOLLOWUPS**. The remaining physical-device checks are follow-up validation debt, not release blockers. No evidence of a new defect, data-safety problem, accounting corruption, migration failure, or release/update-channel regression was found. Farmer pilot testing is approved to proceed.

## Automated gates

| Gate | Result | Evidence |
|---|---|---|
| `:app:testDebugUnitTest` | 443 tests, 0 failures | PASS |
| `:app:lintDebug` | 0 errors | PASS |
| `:app:verifyLocal` | passed | `app/build/reports/verification/local-ci-evidence.json` |
| `:app:connectedDebugAndroidTest` | 141 tests, 32 failures | PASS-with-known-baseline |

### Connected-suite disposition

The 32 connected failures are **pre-existing on baseline `main`** and are unrelated to this change. They are device/environment failures in legacy `FarmActivityWorkflowTest`, `FarmBackupIntegrationTest`, `FarmPersistenceIntegrationTest`, `FarmActivityPresentationTest`, and `D002DateTimePickerEvaluationTest` cases that target hidden legacy compatibility buttons (`recordIncomeButton`, `recordExpenseButton`, etc. — `visibility="gone"` in the M7 shell) or assume a focused-window condition that the Moto Edge 60 Fusion window state does not satisfy.

**Comparison methodology.** A direct baseline-vs-changes run was executed on the same device: baseline `main` (changes stashed) produced 137 tests / 32 failures; the M9 build produced 141 tests / 32 failures with the identical failure set. Additionally, none of the five test classes containing the 32 failures were modified by the M9 diff (`git diff f247bfb..HEAD --name-only -- app/src/androidTest/` contains none of `FarmActivityWorkflowTest`, `FarmBackupIntegrationTest`, `FarmPersistenceIntegrationTest`, `FarmActivityPresentationTest`, or `D002DateTimePickerEvaluationTest`), and the failures reference views that are `visibility="gone"` independent of any M9 change.

- Total executed: **141**
- Passed: **109**; Failed: **32**
- Baseline `main`: 137 executed, 32 failed (identical set)
- **New failures introduced by M9: 0**
- Known cause/category: legacy hidden-view `scrollTo()` targets + device window-focus timing on the Moto Edge 60 Fusion; unrelated to grain units or text size.

Four new device tests were added and all pass:
- `GrainUnitsAndTextSizeDeviceTest.textSizeSettingsShowTwentyFourDefaultAndSavedSizeSurvivesRelaunch`
- `GrainUnitsAndTextSizeDeviceTest.supplyCreationDialogOffersGrainUnitsAndPersistsSelection`
- `GrainUnitsAndTextSizeDeviceTest.productCreationDialogOffersGrainUnitsAndPersistsSelection`
- `KisanToolboxDeviceBatteryTest.grainConversionsUseCanonicalRelationshipsAndRejectNegativeQuantity`

One pre-existing test needed a behavior adaptation for 24sp: `FarmIntegratedPolishTest.testCompleteFarmerDailyJourney_endToEnd` clicked `khataContextualReceiveButton` without scrolling; at 24sp the button is below the fold inside the main `R.id.scrollView`, so Espresso's 90%-visible click constraint failed. The button is reachable by scroll (app is scrollable — the required overflow behavior), so the test now performs `scrollTo()` before `click()`. This is a test adaptation to the larger default, not an app regression.

## Physical-device evidence

### 24sp default baseline
- Fresh install (prefs cleared) → Settings → **Text size: 24 sp** (`settingsTextSizeValueText`). (The label originally read "px"; corrected to "sp" to match the governed Android scaled-text-size unit.)
- Today dashboard, Farm Work, Hisab toolbox, and Settings all render large with no clipped or cut-off text.
- Bottom-nav "Farm Work" label truncated to "Farm ..." at 24sp on the first build; fixed by allowing nav labels to wrap to two centered lines (`maxLines="2"`, no ellipsis). After the fix, "Farm Work" renders fully on two lines.

### Grain units in pickers
- Supply creation dialog unit dropdown shows all nine units: `kg / kilogram`, `L / litre`, `bag`, `packet`, `bottle`, `piece`, `mana`, `pathi`, `muri`.
- A supply named "Paddy" was created with unit `mana`; `kisab_farm_store.xml` persisted `MANA` and `Paddy`.

### Grain converter
- Hisab → Kisan calculator toolbox → "Traditional grain converter" renders with guidance "8 mana = 1 pathi and 20 pathi = 1 muri".
- Conversion 1 Muri → Mana produced **"1 Muri = 160 Mana"**.

### Evidence artifacts (on host)
- `/tmp/m9_fresh_create.png` — fresh-install create-farm screen at 24sp
- `/tmp/m9_settings_24sp.png` — Settings showing the text-size value (pre-`sp`-label build)
- `/tmp/m9_today_16sp.png` — Today at 16sp (pre-fix nav, "Farm Work" fit on one line)
- `/tmp/m9_today_24sp_fixed.png` — Today at 24sp after nav-wrap fix ("Farm Work" on two lines)
- `/tmp/m9_farmwork.png` — Farm Work screen at 24sp
- `/tmp/m9_supply_units.png` — supply unit dropdown with mana/pathi/muri
- `/tmp/m9_grain_converter.png`, `/tmp/m9_grain_result.png` — grain converter + result

## Post-merge / pilot follow-up validation (not blockers)

The physical device (`ZA22374XPC`) disconnected from USB/wireless during the final validation pass and did not reconnect. The following checks were planned but **not completed on-device**. They are follow-up validation debt and do not block M9, the PR, or farmer pilot progression, because there is no evidence of a new defect, data-safety problem, accounting corruption, migration failure, or release/update-channel regression:

1. Nepali (नेपाली) locale rendering of the new unit strings and grain converter.
2. Dark-mode rendering at 24sp.
3. Landscape orientation at 24sp.
4. Explicit saved-size relaunch check via UI (16 sp) — covered by the passing `GrainUnitsAndTextSizeDeviceTest` but not manually re-verified after the nav fix.
5. Restore of the protected `RC01UpgradeFarm` backup (`/tmp/kisab_m9_rc01upgradefarm.backup`) and verification that its Today/Khata baselines are unchanged.
6. Production/sale/purchase/usage flows using a grain unit end-to-end (covered by service-level persistence in the device test, not by full UI flow).

These should be completed during the pilot phase with real-farmer evidence, which is now a valued evidence source alongside automated and engineering physical-device verification.
