# Kisab M6.3 — Kisan Calculator Toolbox — Design Record

M6.3 broadens the **Hisab** destination from Party reconciliation into a small, practical, offline farmer toolbox. It preserves the existing Party Hisab calculator and adds four independent utilities that work even before a farm is created.

> **Status: COMPLETE** on `feature/m6-3-kisan-calculator-toolbox`, based on merged M6.2 `main` at `a774687`.

## Product boundary

All calculator results are temporary presentation values. They are not persisted, do not alter a Farm, Trade, Settlement, Transaction, Khata, balance, schema, migration, or backup byte, and do not introduce a second accounting authority. No network or new dependency is required.

Implemented:

- Money arithmetic: addition, subtraction, multiplication, division, and percentage-of.
- Profit/loss: amount, sales margin, and cost markup.
- Simple interest: interest and total repayment from principal, annual percentage rate, and months.
- Nepali land conversion across square metre, Ropani/Aana/Paisa/Daam, and Bigha/Kattha/Dhur.
- Locale-aware parsing of English or Nepali digits and locale-aware result formatting.
- English and Nepali labels, input errors, results, and provisional glossary terms.
- An always-available toolbox above the existing Party Hisab section in the scrollable Hisab destination.

Not implemented:

- Compound interest, repayment schedules, tax, invoice, or accounting advice.
- Seed, fertilizer, feed, milk, yield, or crop-specific agronomy recommendations.
- Saved calculator history, automatic Transactions/Trades/Settlements, or sharing/export.

## Exact calculations

All operations use `BigDecimal` with `DECIMAL128`; binary floating-point is never used.

```text
profit = sale amount - total cost
margin % = profit / sale amount × 100       (undefined when sale amount is zero)
markup % = profit / total cost × 100         (undefined when total cost is zero)

simple interest = principal × annual rate % × months / (100 × 12)
total repayment = principal + simple interest
```

Division by zero is rejected. Cost, sale amount, principal, rate, months, and land area must be non-negative. Arithmetic operands may be negative.

## Land constants

Conversion uses square metres as the common base:

| Unit | Square metres | Relationship |
|---|---:|---|
| Ropani | 508.73704704 | 16 Aana |
| Aana | 31.79606544 | 4 Paisa |
| Paisa | 7.94901636 | 4 Daam |
| Daam | 1.98725409 | — |
| Bigha | 6772.631616 | 20 Kattha |
| Kattha | 338.6315808 | 20 Dhur |
| Dhur | 16.93157904 | — |

The UI describes these as conventional land-unit conversions, not a legal survey. Display values are rounded to at most six fractional digits; calculations retain `DECIMAL128` precision.

The relationships and rounded metric equivalents were checked against Nepal government references: the [PLGSP building-bylaw reference book](https://plgsp.gov.np/sites/default/files/2023-09/Resource_Book_on_Building_Bylaws%20and%20Building%20Permit%20System%20in_Nepal.pdf) and the [National Statistics Office agricultural survey metadata](https://microdata.nsonepal.gov.np/index.php/catalog/10/variable/F2/V30?name=area_hector). The implementation derives the higher-precision square-metre constants from the referenced square-foot standards.

## UI and accessibility

- The toolbox works without a farm; only the existing Party Hisab section shows its no-farm/no-party state.
- Every action is at least 48dp high and results use a polite accessibility live region.
- Invalid fields receive an inline localized error and focus.
- Calculator EditTexts and Spinners retain their normal Android view state across Activity recreation.
- Existing shell navigation, Party selection, period selection, and Party Hisab calculations are unchanged.

## Automated evidence

- `KisanCalculatorsTest`: every operation, divide-by-zero, profit and loss, undefined percentages, negative-input guards, simple interest, both Nepali land systems, and land round-trip.
- `DecimalValueFormatterTest`: English/Nepali digit parsing, whole-input validation, and bounded display precision.
- `FarmLabelsMappingTest`: exhaustive, distinct operation and land-unit resource mappings.
- `LocalizationParityTest` and `LocalizedResourceResolutionTest`: English/Nepali key and label coverage.
- `FarmActivityWorkflowTest`: toolbox visibility and arithmetic use without creating a farm (compiled by the local CI gate; device execution remains deferred).

Completion gates remain `:app:verifyLocal`, `:app:verifyReleaseMetadata`, release-preflight syntax, localization parity, schema/migration and backup regressions, evidence/APK checksum agreement, and `git diff --check`.

## Manual/device validation

Manual emulator/device validation remains deferred under the current instruction. Before production release, validate Nepali keyboard entry, long labels and results on a small screen, TalkBack result announcements, focus/error behavior, and representative land conversions against a trusted local reference.

## Post-M6.3 boundary

M6.3 completes the currently authorized calculator toolbox. Seed/fertilizer or other agronomy-specific tools require a separately bounded milestone because their recommendations need crop, unit, and regional product decisions. M7 remains unprioritized.
