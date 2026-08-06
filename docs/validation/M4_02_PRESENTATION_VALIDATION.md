# Kisab M4-02 — Nepal-Oriented Currency, Number, Date and Time Presentation Validation

Validation record for the **Nepal-oriented currency, number, date and time presentation** workstream (M4-02). This workstream makes money, numbers, and timestamps deterministic and locale-aware — NPR by default for new farms, major-unit money entry with exact parsing, device-local time presentation with UTC storage — without changing the `v0.1.0` persistence schema, backup envelope, domain model, or published release.

> **Status:** IMPLEMENTED, REVIEW PENDING. M4-02 is not canonical until this branch merges and is reviewed. Terminology added by this record remains **Provisional/Pending**.

## Base state

| Field | Value |
| --- | --- |
| Base commit | `4a83912b82df217eb2b1410837bc1bf57622dc31` (merge of PR #9, `main`) |
| Branch | `feature/m4-02-nepal-presentation-rules` |
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

All presentation was hand-rolled and locale-naive in the UI/domain:

- **Money display was raw minor units.** `FarmActivity` rendered `%d`-formatted `amountMinor` (e.g. `3000`) with a manually appended `" USD"` suffix in the summary (`farm_summary_format`), transaction rows (`transaction_row_format`), transaction selection rows (`transaction_selection_row_format`), and the imported-farm summary (`imported_farm_summary_format`). No currency symbol, no fraction digits, no grouping, no `NPR` awareness.
- **Money entry was minor units.** The amount `EditText` was `inputType="number"` with hint "Amount minor units"; `saveTransaction` parsed a raw `Long` and the edit prefill wrote `String.format(Locale.US, "%d", amountMinor)`. A user would have to enter `12345` for what should be displayed as `123.45`.
- **Time display forced UTC text.** Domain `FarmTransaction.displayDateTime()` (since removed) rendered `occurredAt` as `"yyyy-MM-dd HH:mm:ss 'UTC'"` — a fixed format in a fixed zone with a literal `UTC` label, ignoring the device timezone and locale.
- **Timestamps were edited as raw stored ISO strings.** `fillTransactionForm` set the `occurredAt` field from the stored UTC value, so a Nepal user editing a transaction saw UTC wall-clock text rather than their local time.
- **No NPR default and no currency protection.** New/empty farms defaulted the currency field to empty; the user could enter any 3-letter code on any farm, silently producing mixed-currency farms (the domain rejected them only on `summary`/backup validation).
- **Counts and quantities used unformatted integers** in every format string.

## Presentation contract

### Locale normalization — `ui/PresentationLocale.kt`

- Nepali UI resolves to `ne-NP`; English UI to `en`; any other language keeps its own `Locale` so number/date formatting follows that language's conventions.
- Time presentation always uses the **device timezone** regardless of the presentation locale.

### Money display — `ui/MoneyFormatter.kt`

- Values are derived from `amountMinor` exclusively through `BigDecimal` (no `Double`/`Float`, no silent rounding).
- Rendering uses `NumberFormat.getNumberInstance(locale)` configured with the currency's ISO fraction digits, plus grouping, followed by an explicit ISO code: `123.45 NPR`, `1,234.56 USD`, `1,500 JPY`, `1.500 KWD`.
- Fraction digits follow `Currency.getInstance(code).defaultFractionDigits` (NPR/USD 2, JPY 0, KWD 3); an unknown-but-valid 3-letter code falls back to 2 fraction digits and still displays its ISO code.
- Negative and zero balances render correctly (`-1,234.56 USD`, `0.00 USD`).
- The edit field value (`toEditFieldValue`) uses the same fraction digits **without** grouping so it round-trips losslessly: `1234.56`, `1500`, `1.500`, `१२३४.५६`.

### Money entry — `ui/MoneyInputParser.kt`

- Accepts the locale's decimal separator and digits (localized Devanagari or ASCII) plus valid grouping separators (standard and Indian-style accepted).
- Rejects, with a distinct outcome: blank (`Missing`), zero/negative (`NotPositive`), malformed/exponent/trailing garbage (`Invalid`), more fraction digits than the currency allows (`TooPrecise`, no rounding), and `Long` overflow (`TooLarge`).
- Converts exactly to minor units by padding the fraction to the currency's digit count (`12.3` with NPR → `1230` minor; `12.345` with NPR → rejected).
- `amountMinor` is produced exactly, so stored minor → displayed major → parsed back → identical minor (verified round-trip tests).

### Integers — `ui/NumberFormatter.kt`

- Locale-aware integer rendering (`NumberFormat.getIntegerInstance`) for entry counts, transaction counts, and entry quantities (`3`, `१,२३४`).

### Time — `ui/TimePresentation.kt`

- Display: `DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)` in the presentation locale, converted to the device zone — no literal `UTC`, no fixed pattern.
- Edit field: ISO-8601 offset date-time of the device-local instant (e.g. stored `2024-01-01T12:00:00Z`, device zone `Asia/Kathmandu` → `2024-01-01T17:45:00+05:45`). Saving an unchanged value preserves the exact stored instant.
- Historical instants use the historical offset for the zone (DST-aware where applicable).

### NPR default and currency protection — `FarmActivity`

- New/currency-empty farms (including a farm left empty after deletes) default the transaction form to `NPR`, editable before first save.
- Existing farms derive the currency from persisted transactions, prefill the form, and never silently replace it (USD farms stay USD).
- The UI rejects an entered currency that differs from the farm's existing currency (excluding the transaction being edited) with a localized, actionable error, before the domain is called. Domain validation is unchanged.
- No hidden currency persistence: an empty farm after deletion re-derives the `NPR` default (documented limitation — the farm has no currency state of its own).

## Implementation

- Removed `FarmTransaction.displayDateTime()` from the domain; the domain now carries data only. Ordering still uses the stored UTC instant. No other domain change.
- `FarmActivity` wires the presentation layer at every site identified in the audit: `buildSummaryText`, `buildImportedFarmSummary`, `renderFarm` (entries, transactions), `fillTransactionForm`, `clearTransactionForm`, `saveTransaction`, `populateTransactionSelection`.
- `FarmUiError` gained `AMOUNT_REQUIRED`, `AMOUNT_INVALID`, `AMOUNT_NOT_POSITIVE`, `AMOUNT_TOO_PRECISE`, `AMOUNT_TOO_LARGE`, `CURRENCY_MISMATCH`.
- Resources: amount hint now "Amount"/रकम; new localized error strings; format strings switched to `%s` where the formatter owns localization (`farm_summary_format`, `entry_row_format`, `transaction_row_format`, `transaction_selection_row_format`, `imported_farm_summary_format`) with matching placeholder signatures across English/Nepali.
- Layout: the amount `EditText` input type changed from `number` to `numberDecimal`.

## API 26 compatibility fix (backup exported-at parsing)

The M4-02 validation matrix runs the full suite on API 26. Those runs exposed a pre-existing defect in `FarmBackupCodec`: `OffsetDateTime.parse(exportedAt, UTC_FORMATTER)` relied on the formatter's `.withZone(ZoneOffset.UTC)` to resolve an offset while parsing a literal-`Z` string. Newer Android resolves the formatter zone during parse; **API 26 does not**, so every backup decode threw on the minimum supported API.

Fix (no format change): the exported-at field is now parsed as a `LocalDateTime` and the UTC offset attached explicitly (`LocalDateTime.parse(text, "yyyy-MM-dd'T'HH:mm:ss'Z'").atOffset(ZoneOffset.UTC)`). The encoded envelope string is byte-identical to before — pinned by the new `backupEnvelopeFormatIsByteStable` JVM test — and decodes correctly on all supported API levels.

## Tests

### JVM (`:app:testDebugUnitTest`) — 86 tests, 0 failures

| Suite | Tests | Result |
| --- | --- | --- |
| `FarmSliceServiceTest` (incl. backup byte-stable golden) | 33 | pass |
| `LocalizationParityTest` | 9 | pass |
| `FarmLabelsMappingTest` / `FarmOrderingTest` / `FarmUiErrorMappingTest` / `KisabSessionAppJvmTest` | 15 | pass |
| `MoneyFormatterTest` (new) | 10 | pass |
| `MoneyInputParserTest` (new) | 11 | pass |
| `NumberFormatterTest` (new) | 3 | pass |
| `TimePresentationTest` (new) | 4 | pass |
| **Total** | **86** | **0 failures** |

New JVM tests always inject explicit `Locale`/`ZoneId` and never depend on the host default locale/timezone. Representative deterministic outputs asserted on JDK 21 (the CI JVM): `123.45 NPR`, `१,२३४.५६ NPR`, `1,500 JPY`, `1.500 KWD`, `Jan 1, 2024, 5:45:00 PM` (en, Kathmandu), `2024 जनवरी 1, 17:45:00` (ne, Kathmandu), edit `2024-01-01T17:45:00+05:45`.

### Instrumentation (`:app:connectedDebugAndroidTest`) — 32 tests per device, 0 failures

| Device | ABI | API | Full suite | Result |
| --- | --- | --- | --- | --- |
| Pixel 7a (physical, USB) | arm64-v8a | 37 | 32 | pass |
| Emulator `kisab_api36_x86_64` | x86_64 | 36 | 32 | pass |
| Emulator `api26` | x86_64 | 26 | 32 | pass |

New `FarmActivityPresentationTest` (6 tests) covers: major-unit entry + edit-prefill round trip; Nepali locale NPR default + Devanagari-digit rendering; USD farm prefill + mixed-currency rejection; localized amount-validation errors (blank/zero/negative/too-precise/invalid/too-large); device-local time edit round trip + no `UTC` literal in history; backup export/import preserving NPR presentation. Existing suites (`FarmPersistenceIntegrationTest`, `FarmBackupIntegrationTest`, `FarmActivityLocalizationSmokeTest`, `LocalizedResourceResolutionTest`, session tests) were updated where they asserted raw-minor-unit or `" UTC"` presentation and pass unchanged on all three devices. No sleeps/retries; dialog interactions are root-scoped (`inRoot(isDialog())`); locales and store state are cleared and restored between tests; every `ActivityScenario` is closed.

## Static analysis

- `:app:lintDebug`: BUILD SUCCESSFUL. Zero `Error` findings. Zero `HardcodedText`, `SetTextI18n`, `MissingTranslation`, `ExtraTranslation`, `StringFormatMatches`, `SimpleDateFormat`, `DefaultLocale`, or new-lint findings. Remaining warnings are pre-existing only (`ApplySharedPref`, `Autofill`, `ButtonStyle`, `GradleDependency`, `MissingApplicationIcon`); no lint baseline or suppressions were added.
- `:app:assembleDebug`: BUILD SUCCESSFUL.
- No `Double`/`Float` money, no direct `amountMinor` rendering, no literal `UTC` display, no resource access from the domain, no locale-less `String.format`.

## Compatibility

- Persistence schema 2 and backup envelope schema 1 are byte-identical; `v0.1.0` data and backups remain readable (golden string test pins the envelope bytes).
- Domain model, validation, ordering, and `decodeOrNull` behavior are unchanged.
- USD (and any single-currency) farms are preserved: form prefill derives the existing currency and mixed currencies are rejected at the UI boundary.
- Foundation dependency and application identity (`com.susankhya.kisab`, `0.1.0`, versionCode `1`) are unchanged.
- No changes to the published `v0.1.0` release, tag, or release workflow.

## Deferred to later M4 workstreams

- First-run and daily-entry workflow changes (NPR default UX, amount keypad behavior) — **M4-03**.
- Physical-device pilot and guided farmer scenarios — **M4-04**.
- Defect correction and release-candidate validation — **M4-05**.
- Bikram Sambat calendar, currency symbol rendering, and date/time pickers remain out of scope.
