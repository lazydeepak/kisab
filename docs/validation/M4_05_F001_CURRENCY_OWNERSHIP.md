# Kisab M4-05 — F001 Currency Ownership — Implementation and Validation Record

Records the implementation and validation evidence for M4-05-F001 (transaction-level currency ownership conflicts with the farm-wide single-currency invariant), so every claim is traceable to a build state, lane, artifact hash, and test result.

> **Policy:** APKs, keystores, passwords, raw participant data and large screen recordings are **never committed**. Evidence hashes are derived from actual artifacts (`shasum -a 256`), never hand-copied.

## Finding reference

- **ID:** `M4-05-F001`
- **Title:** Transaction-level currency ownership conflicts with farm-wide single-currency invariant
- **Source:** **pre-RC design/architecture review** during M4-05 (not an M4-04 pilot defect). Discovered while the signed `v0.2.0` candidate was being validated.
- **Severity:** MAJOR (architecture/consistency; not data-loss). Corrected before the `v0.2.0` RC freeze.

## Problem

The domain made `currency` a property of every `FarmTransaction` and `FarmTransactionDraft`, while `FarmState` itself carried no currency setting. The validator then required all transactions in a farm to use at most one currency (`FarmStateValidator.validateFarm`: `currencies.size <= 1`). That is two competing ownership models: "currency belongs to each transaction" and "a farm can only have one currency." The UI inherited the contradiction (per-transaction currency shown/edited in the transaction editor: `changeCurrencyButton`, `showCurrencyChooser()`, `updateCurrencyDisplay()`, currency in dirty-state recreation, ISO free-type dialog, etc.), and instrumentation tests pinned the contradictory behavior (`emptyFarmDefaultsToNprAndAllowsCurrencyChange`, `soleTransactionKind`... currency-change tests).

## Corrected ownership model

Two kinds of global setting are distinguished conceptually:

1. **Farm/accounting setting — Currency.** Affects the meaning of persisted monetary data; belongs to the farm/accounting dataset (`FarmState.currencyCode`), not to individual transactions. Every transaction automatically uses the farm currency. Exported backups carry the farm currency automatically. Default `NPR` for Nepal.
2. **User/application setting — Language.** Does not change the meaning of farm data; belongs to application preferences, not to farms/transactions and normally not to the farm backup. Today Kisab derives the presentation locale from the Android resource configuration; a dedicated Settings UI is M5-00 scope.

### Currency rule

- Before any monetary transaction exists: currency is freely configurable.
- Once monetary transactions exist: currency is **locked** (`FarmSliceService.setFarmCurrency` is rejected on a farm with transactions). Switching a live accounting record's currency would reinterpret amounts without changing numbers — that is an explicit, governed migration operation, not a Settings toggle.
- Default: `NPR`.

## Change set (working tree on `feature/m4-05-defect-correction`)

| Lane | File | Change |
| --- | --- | --- |
| Finding record | `docs/validation/M4_05_F001_CURRENCY_OWNERSHIP.md` | This record |
| Milestones | `docs/milestones/M4_FIELD_VALIDATION_AND_NEPAL_USABILITY.md` | M4-05 scope add F001; exit-gate note |
| Domain | `app/src/main/kotlin/com/susankhya/kisab/domain/FarmSliceService.kt` | Add `currencyCode` to `FarmState`; remove `currency` from `FarmTransaction`/`FarmTransactionDraft`; `createTransaction`/`updateTransaction` attach farm currency; `setCurrency` gated on empty transactions; `summary.currencyCode = farm.currencyCode` |
| Domain | `app/src/main/kotlin/com/susankhya/kisab/domain/FarmStateValidator.kt` | Validate farm-level currency code (ISO 3-letter); drop transaction-level currency / multi-currency set check |
| Persistence | `app/src/main/kotlin/com/susankhya/kisab/persistence/FarmPersistenceCodec.kt` | Schema 3: top-level `currencyCode`; transaction rows drop currency; decode schema-2 + legacy with correct inference; re-encode as schema 3 |
| Persistence | backup envelope (`FarmBackupCodec`) | **Unchanged** (schema 1) — it wraps the farm-persistence payload |
| UI | `app/src/main/kotlin/com/susankhya/kisab/ui/FarmActivity.kt` | Farm Tools currency row (label + Change gated on empty transactions + locked note); remove editor currency block/state/methods/dialog/bundle |
| UI | `app/src/main/res/layout/activity_farm.xml` | Add currency row in Farm Tools; remove editor currency section |
| IDs | `app/src/main/res/values/ids.xml` | Remove `currencyInput`, `currencyErrorText` |
| Strings | `res/values/{strings, values-ne}` | Add Farm Tools currency strings (locked note); remove editor currency strings |
| UI | `app/src/main/kotlin/com/susankhya/kisab/ui/TransactionEditorState.kt` | Remove `currency` |
| UI | `app/src/main/kotlin/com/susankhya/kisab/ui/FarmUiError.kt` | Remove `CURRENCY_ISO_THREE_LETTERS`, `CURRENCY_MISMATCH` (replaced by farm-tools currency validation error if any) |
| Tests | various | See below |

Persistence/backup compatibility: schema 3 is written from now on; schema 2 payloads and legacy payloads still decode through the infer-and-upgrade path; the backup envelope remains schema 1 and old backups decode.

## Validation evidence

### JVM unit tests

Command: `./gradlew :app:testDebugUnitTest` (JDK 21 baseline).

Result: **114/114 tests, 0 failures** (full suite on JDK 21).

| Class | Tests | Failed | Notes |
| --- | --- | --- | --- |
| `FarmSliceServiceTest` | 41 | 0 | Includes currency-ownership + schema-2/legacy migration tests |
| `FarmTotalsTest` | 8 | 0 | transaction is no longer currency-bearing |
| All other classes | 65 | 0 | labels, ordering, UI error mapping, editors, money, time |
| Full suite | 114 | 0 | on JDK 21 |

### On-device instrumentation

Command: `./gradlew :app:connectedDebugAndroidTest` (Moto Edge 60 Fusion, serial `ZA22374XPC`), plus API-26 emulator supplemental.

Result: **64/64 PASS** on Moto (superset run; last full-suite build state), 0 failures.

### Lint and build

| Task | Result | Notes |
| --- | --- | --- |
| `:app:lintDebug` | BUILD SUCCESSFUL | 0 errors, 19 pre-existing warnings |
| `:app:lintRelease` | BUILD SUCCESSFUL | same |
| `:app:assembleRelease` | BUILD SUCCESSFUL | signed `v0.2.0` RC |

## Artifacts

| Type | Path (local) | SHA-256 |
| --- | --- | --- |
| Signed release APK (not committed) | `app/build/outputs/apk/release/app-release.apk` | to be recorded in RC evidence |

## Decisions and deferred items

- **Schemas:** the persisted farm schema moves to **3** (farm-level currency). The backup envelope stays **schema 1**, because it already wraps the versioned farm-persistence payload; the gate is that old backups continue decoding correctly.
- **Migration fallback:** schema-2 farms infer one currency from existing transactions (USD+USD → USD, NPR+NPR → NPR, etc.); empty farmer → `NPR`. This preserves v0.1.0/schema-2 data readability.
- **Legacy format fallback:** migrated legacy rows always decode as `USD` (historical decoder semantics); a legacy farm with no monetary transactions defaults to **`NPR`** (never an arbitrary `USD` empty-farm default).
- **Currency is locked after transactions exist.** No new multi-currency support in M4-05.
- **Editor does not expose currency.** Daily-entry workflow is Amount → Category → Description → Date & time → Save.
- **UI location is temporary, domain is not:** the Farm Tools currency row is a temporary presentation entry point. The identical domain contract moves to a proper Settings destination in M5-00 (Application Shell / Navigation / Settings). The domain does not change again for this capability.
- **Noted for M5-00/UX review (no code change now):** the income/expense radio group and transaction-type editing of an existing record (behavior: changing a record's type has accounting implications) — flagged, not expanded here.