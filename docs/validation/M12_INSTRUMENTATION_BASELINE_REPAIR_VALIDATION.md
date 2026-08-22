# M12 Validation Record — Instrumentation Baseline Repair

## Candidate identity

- **Branch**: `feat/farm-activities`
- **Baseline**: `5630e97` (post-M11)
- **Application ID**: `com.susankhya.kisab`
- **Version Name / Code**: `0.2.1` / `4` (debug)
- **Device**: `ZA22374XPC` — Motorola Edge 60 Fusion (`scout_g`), Android 16, API 36, density 450 (2.8×), 1220×2712, Asia/Tokyo timezone
- **Debug APK SHA-256**: `0bf2bfd98c25c2de757cc4a6764a47c1eeed18e2d8836250cc3379132d7d62c3` (from `local-ci-evidence.json`)

## M12 FINAL DISPOSITION: PASS

M12 restores the connected-test suite as a trustworthy regression gate. The inherited failure set of **33 stale instrumentation failures** (documented unchanged across the M9/M10/M11 records) is eliminated: the device suite now reports **138 tests / 0 failures** on `ZA22374XPC`, with no test disabled as flaky and no product regression introduced. Every disposition is a classification-backed repair: tests were re-targeted at the current M7+ shell semantics, two genuine product-behavior assumptions in test code were corrected to match the documented multi-farm import contract, and one latent cross-class infrastructure defect (async per-app locale relaunch racing activity launches) was fixed with a deterministic settlement barrier shared by all locale-touching suites.

## Automated gates

| Gate | Result | Evidence |
|---|---|---|
| `:app:testDebugUnitTest` | 484 tests, 0 failures | PASS |
| `:app:lintDebug` | 0 errors | PASS |
| `:app:verifyLocal` | passed | `app/build/reports/verification/local-ci-evidence.json` |
| `:app:connectedDebugAndroidTest` | **138 tests, 0 failures** | PASS (baseline debt cleared) |

Connected delta versus M11 record: 141 → 138 tests (3 obsolete tests deleted with rationale, see taxonomy), 33 → 0 failures.

## Failure taxonomy and dispositions

Original baseline of 33, classified against the current shell:

| Class | Count | Disposition |
|---|---|---|
| `scrollTo` on retired home record buttons (`recordExpenseButton` ×21, `recordIncomeButton` ×2) | 23 | STALE UI TEST — rewritten to seed transactions pre-launch and open the editor from recent-row edit mode (the only reachable path since the M7+ shell retired create-mode cash editors) |
| NoMatchingView `action_replace_farm` in backup import flows | 3 | OBSOLETE SEMANTICS — M10+ import shows an add/update preview dialog (`action_add_imported_farm` / `action_update_farm`, `FarmImportPreviewFactory`); assertions updated accordingly |
| assert-textmatch on import dialogs | 4 | Same dialog-semantics rewrite plus dynamic currency expectation (`FarmCurrencies.defaultFor(Locale.getDefault())`) instead of hardcoded USD/NPR literals |
| `click` on `farmWorkRemaining` (ShellRedesign) | 1 | STALE UI TEST — re-targeted at current Farm Work destination affordances |
| `typeText` into `entryLabel` after recreation | 1 | Rewritten against the live entry-input id with recreation preserved |
| visibility check on `firstIncomeQuickAction…` | 1 | OBSOLETE DUPLICATE — quick-action coverage superseded by record-sheet flows; deleted with rationale comment |

### Deleted tests (with rationale comments left in source)

1. `firstIncomeQuickActionRecordsWithoutCurrencyInput` — asserted the retired first-run quick actions.
2. `repeatedExpenseDerivesCurrencyAndSuppliesCurrentTime` — asserted the retired expense button's implicit currency derivation; covered equivalently by seeded-currency editor tests.
3. `recordButtonWhileDirtyConfirmsDiscard` — dirty-guard coverage moved to destination navigation and recent-row switching (both retained).

## Root causes found (test-infrastructure level)

1. **Async per-app locale relaunch race** — all 11 `NoActivityResumedException` failures clustered exactly in the three classes that mutate `LocaleManager.applicationLocales` in setup/teardown. The setter is asynchronous: the system broadcasts a configuration change and can relaunch activities while the next launch/assert is already running. Fix: `setApplicationLocalesAndWait` / `resetApplicationLocalesAndWait` in `LocalizationTestContexts.kt` poll until the system service acknowledges the requested locales, then drain the main thread — used by `FarmActivityLocalizationSmokeTest`, `FarmActivityPresentationTest`, and `D002DateTimePickerEvaluationTest`.
2. **Service-side seeding does not re-render home** — writing through `FarmSliceService` after launch leaves the recent-rows container stale. Fix: seed before launch wherever possible; where a mid-test write is inherent (JPY/KWD fraction-digit scenarios), reload deterministically via `awaitEditorForTransaction`, a bounded retry that waits for the row to be laid out and interactive.
3. **Timezone-sensitive assertion** — `majorUnitAmountEntryAndPrefillRoundTrip` compared a wall-clock string rendered in device-local time; on this JST device it failed. Fix: compare `Instant` values (zone-independent).
4. **Adapter-backed chooser not view-matchable** — the single-choice currency dialog is a `RecycleListView`; `onView(withText(...))` cannot address items. Fix: `onData(equalTo(label)).inRoot(isDialog())` per supported currency.
5. **Row-order assumption** — post-upgrade export test read row 0 only; rows are sort-dependent. Fix: scan every recent row for the edited description.

## Product-contract clarifications surfaced (no product change required)

- `replaceFarmWith` is **multi-farm safe**: "Other local farms are never wiped." Two tests still asserted the pre-M10 replace-all behavior (`loadFarm(originalId) == null`); they now assert the old farm remains stored but is no longer current.
- The legacy compatibility block in `activity_shell.xml` (`farmNameText`, balance/income/expense texts, retired action buttons) has no id and hard-coded `gone`; its text views are still populated by `renderHome`, which is why text-content assertions passed while visibility assertions failed. Tests now target only reachable UI.

## Rider: breakdown copy parity (EN/NE)

- `farm_activity_breakdown_section`: EN "Income & expenses by activity" → "Records by activity"; NE aligned ("क्रियाकलाप अनुसार अभिलेख") — section includes trade rows, so the income/expense-only phrasing was misleading.
- `farm_activity_breakdown_empty_text`: EN/NE empty-state copy now mentions linking transactions **or trades**.
- No automated test referenced these strings; localization smoke suites pass on device in both languages.

## Device-level verification notes

- Full connected suite executed twice-plus on `ZA22374XPC` during repair iterations; final run: `Finished 138 tests on motorola edge 60 fusion - 16` with 0 skipped / 0 failed, BUILD SUCCESSFUL in 5m 9s.
- No interactive manual walkthrough was repeated for M12: the milestone changes no product logic (test-only code plus the two rider strings). Supplier lifecycle, customer settlement, production allocation, and activity attribution remain covered end-to-end on the physical device through the repaired suite (trade/settlement/persistence/backup integration classes run on-device against the real store).
