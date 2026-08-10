# Kisab M4-05 — D001 Transport Category — Implementation and Validation Record

Records the implementation and validation evidence for M4-05-D001 (add a dedicated `TRANSPORT` expense category), so every claim is traceable to a build state, lane, artifact hash, and test result.

> **Policy:** APKs, keystores, passwords, raw participant data and large screen recordings are **never committed**. Evidence hashes are derived from actual artifacts (`shasum -a 256`), never hand-copied.

## Defect reference

- Source: `M4-04-D001` in `M4_04_DEFECT_REGISTER.md` (MINOR, HANDED TO M4-05). Scenario B (crop/vegetable farm) recorded a transport cost under `Other expense` because no transport category existed.
- Resolution scope (this change, in line with M4-05): add a dedicated expense-only `TRANSPORT` category, an explicit ordering position, English/Nepali labels, and focused model and UI coverage. **No D002 change is included; D002 is evaluated separately.**

## Change set (working tree on `feature/m4-05-defect-correction`, base `main` @ `a1bb91c`)

| Lane | File | Change |
| --- | --- | --- |
| Domain | `app/src/main/kotlin/com/susankhya/kisab/domain/FarmSliceService.kt` | Add `TRANSPORT(TransactionType.EXPENSE)` to `TransactionCategory` |
| Ordering | `app/src/main/kotlin/com/susankhya/kisab/ui/FarmOrdering.kt` | Expense order becomes `FEED, SUPPLIES, LABOR, TRANSPORT, OTHER_EXPENSE` |
| Labels | `app/src/main/kotlin/com/susankhya/kisab/ui/FarmLabels.kt` | Exhaustive resource mapping for `TRANSPORT` |
| EN string | `app/src/main/res/values/strings.xml` | `transaction_category_transport` = `Transport` |
| NE string | `app/src/main/res/values-ne/strings.xml` | `transaction_category_transport` = `यातायात` (yātāyāt) |
| Unit test | `app/src/test/kotlin/com/susankhya/kisab/FarmOrderingTest.kt` | `expenseShowsFeedSuppliesLaborTransportOtherExpense` asserts 5 expense entries incl. TRANSPORT |
| Unit test | `app/src/test/kotlin/com/susankhya/kisab/domain/FarmSliceServiceTest.kt` | `transportExpenseTransactionRoundTripsThroughPersistenceAndBackup`; `transportCategoryIsExpenseOnlyAndRejectedWhenTypeMismatches` |
| UI test | `app/src/androidTest/kotlin/com/susankhya/kisab/FarmActivityWorkflowTest.kt` | `transportExpenseCategorySelectableAndPersisted` — expense flow selects TRANSPORT and persists |
| Localization | `docs/localization/NEPALI_TERMINOLOGY.md` | New `Transport` row; corrected the `Other expense` obsolete reference to D001 |

Persistence/backup compatibility: `FarmPersistenceCodec.remove / FarmBackupCodec` encode `TransactionCategory.name`, so the new enum value is forward-compatible. The byte-stable backup golden (`backupEnvelopeFormatIsByteStable`, FEED-based) is unchanged and passes.

## Validation evidence

### JVM unit tests

Command: `./gradlew :app:testDebugUnitTest`

Result: **109 tests completed, 1 failed**; the single failure is the pre-existing baseline `TimePresentationTest#displaysStoredInstantInDeviceZoneWithoutUtcLiteral` (JDK-17 locale/Timezone behavior, unrelated to D001). Relevant M4-04 targeted classes:

| Class | Tests | Failed | Notes |
| --- | --- | --- | --- |
| `FarmSliceServiceTest` | 36 | 0 | Includes both new transport tests |
| `FarmOrderingTest` | 6 | 0 | Includes renamed ordering test |
| Full suite | 109 | 1 | Pre-existing `TimePresentationTest` only |

### On device instrumentation — focused D001 UI test

Command: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class='com.susankhya.kisab.FarmActivityWorkflowTest#transportExpenseCategorySelectableAndPersisted'`

Device: Moto Edge 60 Fusion, serial `ZA22374XPC` (use the debug-signed after the previous production-signed install; the disposable Moto farm is already backed up under `.m4-04-evidence/moto-second-gate/`)

Result: **1/1 PASS** (`transportExpenseCategorySelectableAndPersisted`, time ~4 s). The test creates a farm, opens the expense editor, selects TRANSPORT in the category spinner, saves `1500.00`, and asserts `TransactionType.EXPENSE` + `TransactionCategory.TRANSPORT` + `150000` minor units + `Van hire` description persisted.

### On device instrumentation — localization resolution

Command: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class='com.susankhya.kisab.LocalizedResourceResolutionTest'`

Result: **11 tests, 0 failures**. Iterates all `TransactionCategory.values()`, including `TRANSPORT`, asserting non-blank English and Nepali labels, non-raw enum names, and EN ≠ NE.

### Lint and build

| Task | Result | Notes |
| --- | --- | --- |
| `:app:lintDebug` | BUILD SUCCESSFUL | 19 warnings, 0 errors — warnings: `ApplySharedPref` x3, `GradleDependency` x2, `MissingApplicationIcon` x1, `ButtonOrder` x1, `ButtonStyle` x7,... (all pre-existing, no D001-relevant issues) |
| `:app:assembleDebug` | BUILD SUCCESSFUL | APK SHA-256 `a4e2bbe73ffe4da382a206d8ac5cdca6ca43eb5901f36ef0170bcf8ad0c7ae2a` (see artifact row) |

## Artifacts

| Type | Path (local) | SHA-256 |
| --- | --- | --- |
| Debug APK (not committed) | `app/build/outputs/apk/debug/app-debug.apk` | `a4e2bbe73ffe4da382a206d8ac5cdca6ca43eb5901f36ef0170bcf8ad0c7ae2a` |

## Decisions and deferred items

- **NOT included in this change:** D002 (past-date picker) — evaluated separately per M4-05 work order.
- **Device coverage caveat:** the only physical device exercised this cycle is the Moto Edge 60 Fusion (serial `ZA22374XPC`, API 36). This is the second physical device recorded in M4-04 and is **not** an older/lower-resource API-26-class phone. Coverage on older/lower-resource hardware remains **NOT YET PROVEN ON PHYSICAL HARDWARE**, consistent with the residual M4 exit-gate item. Emulators remain supplemental/non-physical.
- The `-ne` term `यातायात (yatayat)` is a loanword; it is the natural term in the same form used in Nepal. Marked `Provisional`; final approval is decided at the signed-RC/pilot stage per the terminology rules.