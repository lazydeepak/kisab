# Kisab M4-01 — Localization Foundation Validation

Validation record for the **Android string-resource and Nepali localization foundation** (M4-01). This milestone workstream moves all user-facing application text into Android string resources and adds an initial Nepali (`values-ne/`) resource set governed by the terminology glossary.

> **Status:** Recorded for review. M4-01 is not canonical until this branch merges and is reviewed. Terminology in this record remains **Provisional/Pending** until confirmed by pilot evidence in M4-04.

## Base state

| Field | Value |
| --- | --- |
| Base commit | `db500c4a3dacec324529964b0696a61596a1fc83` (merge of PR #7, `main`) |
| Branch | `feature/m4-01-localization-foundation` |
| App package | `com.susankhya.kisab` |
| Foundation dependency | `com.susankhya.foundation:foundation-session-android:0.1.1` (unchanged, verified via `:app:dependencies --configuration debugRuntimeClasspath`) |
| Persistence schema | 2 (unchanged) |
| Backup envelope schema | 1 (unchanged) |
| minSdk / targetSdk | 26 / 36 |
| CI gate | `build` check runs `testDebugUnitTest`, `lintDebug`, `assembleDebug`, and `dependencies`; connected tests are local-only |

## Audit

Before changes, `FarmActivity` (the only activity) surfaced user-visible text from three sources:

- **41 hardcoded string literals** passed directly to `setText`, `Toast.makeText`, and `AlertDialog` (button titles, dialogs, messages, toasts).
- **2 enum-name-derived labels** (`enum.name.lowercase().replaceFirstChar(...)`) for entry-kind and transaction-type spinners, which rendered English and would not localize.
- **16 domain/persistence exception messages** surfaced to the user through `exception.message` (validation failures and backup rejection messages written in English by domain code).

All three sources were removed: the app now resolves every user-visible string from Android resources (`values/` English default + `values-ne/`), spinners are built from explicit domain orderings, and errors are mapped to resource ids.

## Resource set

| Set | Keys | Notes |
| --- | --- | --- |
| `app/src/main/res/values/strings.xml` | **69** | 66 translatable + 3 `translatable="false"` (`app_name`, `backup_filename_format`, `backup_filename_fallback`) |
| `app/src/main/res/values-ne/strings.xml` | **66** | All translatable English keys, placeholder signatures identical; no `values-np` variant |

Non-translatable English keys (`backup_filename_format`, `backup_filename_fallback`) exist only in the default `values/` set; format strings are never translated so that generated file names remain ASCII-safe on all devices.

## Architecture

### Resource mapping

- `ui/FarmLabels.kt` — exhaustive `when` mapping of every domain enum (`FarmEntryKind`, `TransactionType`, `TransactionCategory`) to a `@StringRes` id. An unhandled enum value is a compile-time error, so the mapping cannot silently drift from the domain model.
- `ui/FarmOrdering.kt` — explicit domain lists for entry kinds, transaction types, and category lists constrained by transaction type. Spinner positions index these lists directly; labels are never parsed back into domain values, so localization cannot corrupt selections.
- `ui/FarmUiError.kt` — `enum class` with a `@StringRes` per failure and a `fromBackupFailure(reason)` mapping.

### Error boundary

- `persistence/FarmBackupException.kt` — typed backup rejection (`BackupRejectionReason`: `INVALID_ENVELOPE`, `UNSUPPORTED_VERSION`, `TOO_LARGE`, `UNREADABLE`). Extends `IllegalArgumentException`, so existing catch sites (`catch (exception: RuntimeException)`) remain valid.
- `persistence/FarmBackupCodec.kt` and `persistence/FarmBackupFileAdapter.kt` throw the typed reasons; `decodeOrNull` semantics are unchanged.
- `FarmActivity` performs explicit UI validation before calling the domain (farm name required, entry label required, quantity a positive whole number, description required, amount a positive whole number, currency `^[A-Z]{3}$`, ISO-8601 parsed via `OffsetDateTime.parse`) and shows resource-backed validation messages.
- `exception.message` is only written to the log inside `showUnexpectedFailure`; the user is shown the generic `error_unexpected` resource. No English text can reach the UI through an exception.

### Layout tolerance

- The save/delete and export/import button rows use `layout_width="0dp"` with `layout_weight="1"` so longer Nepali labels do not truncate.

## Terminology

Nepali translations follow the provisional glossary at `docs/localization/NEPALI_TERMINOLOGY.md` (e.g. फार्म, पशुधन, बाली, लेनदेन, आम्दानी, खर्च, बाँकी रकम, रकम, मिति, समय, थप्नुहोस्, मेटाउनुहोस्, पुष्टि गर्नुहोस्, रद्द गर्नुहोस्, ब्याकअप, निर्यात, आयात, पुनर्स्थापना, अमान्य फाइल, मुद्रा). Technical tokens that are ASCII-safe and locale-independent (ISO-8601, ISO, currency codes, file names) remain Latin by design. No `Confirmed` status is claimed; all terms remain Provisional/Pending until the M4 pilot.

## Tests

### JVM (`:app:testDebugUnitTest`)

| Suite | Tests | Result |
| --- | --- | --- |
| `FarmSliceServiceTest` (pre-existing) | 32 | pass |
| `LocalizationParityTest` | 9 | pass |
| `FarmLabelsMappingTest` | 3 | pass |
| `FarmOrderingTest` | 6 | pass |
| `FarmUiErrorMappingTest` | 5 | pass |
| `KisabSessionAppJvmTest` (pre-existing) | 1 | pass |
| **Total** | **56** | **0 failures** |

`LocalizationParityTest` enforces: repository-relative resource parsing, no duplicate names, no `values-np`, every Nepali key exists in English, every translatable English key exists in Nepali, no empty values, matching placeholder signatures, matching plural quantities, and correct non-translatable exclusions.

### Instrumentation (`:app:connectedDebugAndroidTest`)

Final gate runs on Android 17 (API 37, Pixel 7a physical device) with a regression pass on Android 16 (API 36, emulator `kisab_api36_x86_64`):

| Device | Suite | Tests | Result |
| --- | --- | --- | --- |
| Pixel 7a (API 37, arm64-v8a) | Full connected suite | 26 | pass (2 consecutive runs) |
| Pixel 7a (API 37, arm64-v8a) | `FarmBackupIntegrationTest` | 5 | pass (10 consecutive runs) |
| Emulator (API 36, x86_64) | Full connected suite | 26 | pass (1 regression run) |

Per-class (both devices): `FarmActivityLocalizationSmokeTest` 4, `LocalizedResourceResolutionTest` 9, `FarmPersistenceIntegrationTest` 3, `KisabSessionAppTest` 2, `KisabSessionStorageAdapterTest` 3, `FarmBackupIntegrationTest` 5 — all pass, zero failures, zero skipped, no Kisab crashes or ANRs.

Smoke tests apply the app-level locale with the system `android.app.LocaleManager` API (API 33+; the emulator is API 36) and restore it afterward; the app-level locale is active process-wide, so the English baseline is resolved via a locale-forced configuration context (`createConfigurationContext`), not the default app context.

**Android 17 dialog-root race corrected:** a pre-existing `FarmBackupIntegrationTest` dialog-root race was exposed deterministically on Android 17. The tests now scope dialog interactions with `inRoot(isDialog())`, use localized resource identifiers, and close every `ActivityScenario`. No production behavior, persistence, or backup format changed.

**Environment note:** the freshly wiped emulator intermittently displayed a `System UI isn't responding` ANR dialog that steals window focus, causing occasional `RootViewWithoutFocusException` failures across pre-existing instrumentation tests. This was mitigated with `settings put global hide_error_dialogs 1` and animation-disabling settings (emulator-only; nothing in the repository references these settings).

## Static analysis

- `:app:lintDebug`: BUILD SUCCESSFUL. Zero findings for `HardcodedText`, `SetTextI18n`, `MissingTranslation`, `ExtraTranslation`, and `StringFormatMatches`. Remaining warnings are pre-existing only (`ApplySharedPref`, `Autofill`, `ButtonStyle`, `GradleDependency`, `MissingApplicationIcon`).
- `:app:assembleDebug`: BUILD SUCCESSFUL.
- `git diff --check`: clean.

## Compatibility

- Persistence schema 2 and backup envelope schema 1 are unchanged; `v0.1.0` data and backups remain readable.
- `FarmBackupException` extends `IllegalArgumentException`; existing `RuntimeException` catch sites are unaffected.
- Newest-first transaction ordering, transaction serialization, and `decodeOrNull` behavior are unchanged.
- Foundation dependency and application identity (`com.susankhya.kisab`, versionName `0.1.0`, versionCode `1`) are unchanged.
- No changes to the published `v0.1.0` release, tag, or release workflow.

## Deferred to later M4 workstreams

- NPR default for new farms, NPR/currency presentation, number/date/time formatting rules — **M4-02**.
- First-run and daily-entry workflow changes — **M4-03**.
- Physical-device matrix and guided pilot (including Pixel 7a) — **M4-04**.
- Defect correction and release-candidate validation — **M4-05**.

## M4-01 not complete until merged

This record describes work on `feature/m4-01-localization-foundation`. M4-01 is implemented and pending review; it does not become canonical, and the milestone status does not change, until the branch is reviewed and merged into `main` and the milestone document is updated.
