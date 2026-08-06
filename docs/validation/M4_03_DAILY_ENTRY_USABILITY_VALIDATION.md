# Kisab M4-03 — First-Run and Daily Transaction-Entry Usability Validation

Validation record for the **first-run and daily transaction-entry usability** workstream (M4-03). This workstream replaces the transaction-selection flow with an overview and inline transaction editor so the core daily tasks (record income/expense, review balance) are fast and unambiguous — derived currency with locked normal flow, default-now time with native date/time pickers, an explicit dirty-editor contract with discard protection, and a collapsed farm-tools section — without changing the `v0.1.0` persistence schema, backup envelope, domain model, or published release.

> **Status:** IMPLEMENTED, REVIEW PENDING. M4-03 is implemented on the `feature/m4-03-daily-entry-usability` branch and submitted for review as a draft pull request. It is not canonical until reviewed and merged into `main`. Terminology added by this record remains **Provisional/Pending**.

## Base state

| Field | Value |
| --- | --- |
| Base commit | `bdbd1967e9e90b531a82652c7be73ff74b1a05bb` (merge of PR #10, `main`) |
| Branch | `feature/m4-03-daily-entry-usability` |
| App package | `com.susankhya.kisab` (unchanged) |
| versionName / versionCode | `0.1.0` / `1` (unchanged) |
| Foundation dependency | `com.susankhya.foundation:foundation-session-android:0.1.1` (unchanged) |
| Persistence schema | 2 (unchanged, byte-stable) |
| Backup envelope schema | 1 (unchanged, byte-stable) |
| minSdk / targetSdk / compileSdk | 26 / 36 / 36 |
| Money representation | integer minor units (`amountMinor: Long`) in the domain and storage (unchanged) |
| Time storage | UTC (`OffsetDateTime` normalized to UTC on write; unchanged) |
| CI gate | `build` check runs `testDebugUnitTest`, `lintDebug`, `assembleDebug`, `dependencies`; connected tests are local-only |

## Audit (before changes)

The pre-M4-03 flow made the daily entry task indirect:

- **Transaction entry required two hops and a selection step.** `FarmActivity` rendered a "Transactions" list and an "Add transaction" section with a transaction-type spinner plus a selection spinner that the user had to populate before the amount/currency/date-time fields appeared (`transaction_selection_create`/`transaction_selection_row_format`).
- **Editor state was implicit.** Form state lived in widgets and a `fillTransactionForm`/`clearTransactionForm` pair; there was no explicit dirty-tracking, so cancel/back always closed silently and an accidental back press could discard work without warning.
- **Currency was a free-text input.** The form exposed an editable currency field with a hint and a dedicated "required"/"invalid" date-time error set; the user could type any ISO code and create mixed-currency farms through the UI.
- **Date/time was raw stored ISO text.** The editor presented `occurredAt` as editable ISO-8601 text (e.g. `2024-01-01T12:00:00Z`) rather than pickers or a human-readable value.
- **Balance was not immediately visible.** The first-action guidance was absent; new-farm users saw an empty selection spinner rather than a prompt to record their first income or expense.
- **Farm tools (entries, backup) were always expanded** in the activity, pushing transaction entry off-screen.

## Screen hierarchy contract (after)

Single `FarmActivity` screen, top to bottom:

1. Farm identity (`farmNameText`).
2. Balance / income / expenses overview (`balanceText`, `incomeText`, `expensesText`) computed from pure totals.
3. `Record income` | `Record expense` quick actions.
4. First-action guidance prompt (`firstActionPrompt`) — visible only while the farm has no transactions; hidden automatically after the first transaction.
5. Inline transaction editor (`transactionEditorContainer`) — opened by Record income/expense or by tapping a recent row; closed by Save/Cancel/Delete/back.
6. Recent transactions (`recentTransactionsTitle`, `recentTransactionsContainer`, newest first, one tappable row per transaction).
7. Collapsed Farm tools (`farmToolsToggleButton`, `farmToolsContainer`) — expanded on demand, contains farm summary, entries list + add-entry, and export/import backup.

## Editor state contract

- `ui/TransactionEditorState.kt` defines `TransactionEditorMode` (`CREATE`/`EDIT`) and `TransactionEditorState(mode, transactionId, type, category, amountText, description, occurredAt, currency)`.
- `TransactionEditorState.create(type, currency, occurredAt)` supplies the CREATE defaults: first category for the type via `FarmOrdering.categoriesFor(type).first()`, current device time, and the derived/fallback currency.
- **Dirty** is defined as any current value differing from the editor's baseline (open-time state). Create/record while dirty, recent-row taps while dirty, cancel while dirty, and back while dirty all route through `confirmDiscardIfNeeded`, which shows a discard confirmation dialog; the draft survives `onSaveInstanceState`/restore and Activity recreation.
- `onSaveInstanceState` preserves the editor state and the farm-tools expansion state; restore re-renders without data loss.

## Time and date entry

- The editor shows a human-readable, device-local date/time (`transactionDateTimeText`, e.g. `Today, 12:07 PM` or `Jan 1, 2024, 5:45:00 PM`) — never editable ISO text.
- `Change date and time` opens native `DatePickerDialog` then `TimePickerDialog`; `ui/EditorDateTime.kt` resolves the picked wall-clock values in the device zone through `ZonedDateTime.of` (DST gap → forward shift, DST overlap → earlier offset, both verified).
- Saving preserves the exact stored instant when the value is unchanged (edit-without-date-change test).

## Currency rules

- Currency is **derived**, never typed: an established farm's currency comes from its persisted transactions; a new/empty farm defaults to `NPR`.
- In the normal flow the currency is locked (`changeCurrencyButton` hidden) once any other transaction establishes the farm currency.
- The currency may be changed (via a `Currency` dialog) only in the sole-transaction / no-established-currency case (new empty farm, or the only transaction is the one being edited).
- `saveTransaction` still rejects a currency that differs from the farm's established currency before calling the domain, with the localized `CURRENCY_MISMATCH` error.

## Totals

- `domain/FarmTotals.kt` is a pure helper computing income, expense, and balance with exact `Long` arithmetic via `Math.addExact`/`Math.subtractExact`; overflow throws `ArithmeticException` (caught as an unexpected failure) rather than silently wrapping.
- The overview and farm-tools summary render only through `MoneyFormatter`/`NumberFormatter` — no raw minor-unit output.

## Form order and validation

Record income/expense → amount (focused, `imeOptions` `actionNext`) → category (spinner, first category defaulted) → description (`actionDone`) → date/time (label + value + Change) → currency (label + value + Change) → validation → Save / Cancel (Delete shown only in EDIT mode). Validation failures keep the editor open with the message in `validationMessageText`.

## Strings

- `res/values/strings.xml` and `res/values-ne/strings.xml` gained the new key set (quick actions, overview formats, first-action prompt, editor sections, date/time and currency labels, save/update/cancel actions, today label, currency dialog, recent-transactions section + accessibility format, farm-tools section/toggle/summary, backup section, delete/discard dialogs, imported-farm summary, toasts). Removed stale keys: `transactions_section`, `add_transaction_section`, `transaction_currency_hint`, `transaction_occurred_at_hint`, `save_transaction_action`, `transaction_selection_create`, `transaction_selection_row_format`, `error_transaction_date_time_required`, `error_transaction_date_time_invalid`, `error_transaction_selection_required`.
- EN = 92 keys; NE = 89 keys. The three EN-only keys (`app_name`, `backup_filename_format`, `backup_filename_fallback`) remain Latin by design (technical tokens per M4-01 rules). Every other key has an English default and a Nepali counterpart with matching placeholder signatures.
- `FarmUiError` dropped the now-unreachable `TRANSACTION_DATE_TIME_REQUIRED`, `TRANSACTION_DATE_TIME_INVALID`, and `TRANSACTION_SELECTION_REQUIRED` codes; `LocalizationParityTest` and `LocalizedResourceResolutionTest` were updated to the new key set.

## Tests

### JVM (`:app:testDebugUnitTest`) — 106 tests, 0 failures

| Suite | Tests | Result |
| --- | --- | --- |
| `FarmSliceServiceTest` (incl. backup byte-stable golden) | 33 | pass |
| `FarmTotalsTest` (new) | 8 | pass |
| `LocalizationParityTest` | 9 | pass |
| `FarmLabelsMappingTest` / `FarmOrderingTest` / `FarmUiErrorMappingTest` / `KisabSessionAppJvmTest` | 15 | pass |
| `MoneyFormatterTest` / `MoneyInputParserTest` / `NumberFormatterTest` | 30 | pass |
| `EditorDateTimeTest` (new) | 4 | pass |
| `TimePresentationTest` (extended: `shortTime`, `isToday`) | 7 | pass |
| **Total** | **106** | **0 failures** |

`FarmTotalsTest` covers empty, income-only, expense-only, mixed, negative-balanced, and large-value farms plus exact `Long` overflow. `EditorDateTimeTest` covers Kathmandu, UTC, and New York DST gap/overlap resolution deterministically with explicit zones (host locale/zone independent).

### Instrumentation (`:app:connectedDebugAndroidTest`) — 45 tests per device, 0 failures

45 tests, zero failures, zero unexpected skips, and no crash or ANR on every device:

| Device | ABI | API | Full suite | Result |
| --- | --- | --- | --- | --- |
| Pixel 7a (physical, USB) | arm64-v8a | 37 | 45 | pass |
| Emulator `kisab_api36_x86_64` | x86_64 | 36 | 45 | pass |
| Emulator `api26` | x86_64 | 26 | 45 | pass |

New `FarmActivityWorkflowTest` (13 tests) covers the daily-entry workflow: first income with no currency/ISO input and default-now time; repeated expense derives currency and supplies current time; edit preserves transaction identity; edit without date change preserves the exact instant; recreation preserves the editor draft; cancel and back require discard confirmation; switching to another transaction while dirty confirms; record while dirty confirms; empty-farm NPR default with currency choice and post-save lock; established farm currency lock; sole-transaction currency change; human-readable date/time with picker (no ISO typing); farm-tools expansion/collapse. Existing suites (`FarmActivityPresentationTest`, `FarmPersistenceIntegrationTest`, `FarmBackupIntegrationTest`, `FarmActivityLocalizationSmokeTest`, `LocalizedResourceResolutionTest`, session tests) were migrated to the new views and key set (e.g. `farm_tools_summary_format` with 3 args, `recentTransactionsContainer` rows) and pass unchanged in behavior. No sleeps/retries; dialog interactions are root-scoped (`inRoot(isDialog())`); locales and store state are cleared and restored between tests; every `ActivityScenario` is closed.

## Static analysis

- `:app:lintDebug`: BUILD SUCCESSFUL. Zero `Error` findings. No lint baseline or suppressions were added.
- `:app:assembleDebug`: BUILD SUCCESSFUL.
- No `Double`/`Float` money, no raw `amountMinor` rendering, no editable ISO date-time text, no resource access from the domain.

## Compatibility

- Persistence schema 2 and backup envelope schema 1 are byte-identical; `v0.1.0` data and backups remain readable.
- Domain model, validation, ordering, and `decodeOrNull` behavior are unchanged. Ordering still uses the stored UTC instant (newest first in the recent list).
- Established single-currency farms are preserved: currency is derived and locked, and mixed currencies are rejected at the UI boundary.
- Foundation dependency and application identity (`com.susankhya.kisab`, `0.1.0`, versionCode `1`) are unchanged.
- No changes to the published `v0.1.0` release, tag, or release workflow.

## Validation commands

```bash
export JAVA_HOME=/usr/local/opt/openjdk@21; export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug \
  :app:dependencies --configuration debugRuntimeClasspath
./gradlew :app:connectedDebugAndroidTest   # Pixel 7a API 37, API 26, API 36
git diff --check
```

## Deferred to later M4 workstreams

- Physical-device pilot and guided farmer scenarios — **M4-04**.
- Defect correction and release-candidate validation — **M4-05**.
- Bikram Sambat calendar and currency symbol rendering remain out of scope.
