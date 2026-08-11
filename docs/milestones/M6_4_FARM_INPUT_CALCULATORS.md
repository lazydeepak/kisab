# Kisab M6.4 — Farm Input Calculators — Design Record

> **Status: COMPLETE (automated); manual/device validation deferred** on `feature/m6-4-farm-input-calculators`, based on merged M6.3 `main` at `78925e1`.

## Product boundary

M6.4 extends the offline Hisab toolbox with five farmer-entered planning calculators. These are temporary projections, not agronomy recommendations or accounting authority. No result is persisted, exported, synchronized, or used to create or modify a Farm, Entry, Transaction, Party, Trade, Settlement, Khata, balance, schema, migration, or backup.

The selector exposes one form at a time:

- seed quantity and cost;
- fertilizer quantity and cost;
- feed requirement and cost;
- milk production and revenue;
- crop yield and revenue.

All rates, durations, counts, and prices come from the farmer. The app supplies no crop-specific rate, dose, feeding, yield, health, price, or legal advice.

## Exact calculations

All decimal operations use `BigDecimal` with `DECIMAL128`; binary floating point is not used.

```text
seed kg = area × seed kg per selected land unit
seed cost = seed kg × price per kg

fertilizer kg = area × fertilizer kg per selected land unit
fertilizer cost = fertilizer kg × price per kg

feed kg = whole animal count × kg per animal per day × days
feed cost = feed kg × price per kg

milk litres = whole milking-animal count × litres per animal per day × days
milk revenue = milk litres × price per litre

crop yield kg = area × expected kg per selected land unit
crop revenue = crop yield kg × price per kg
```

Every numeric input must be non-negative. Animal counts must additionally be whole numbers within the supported integer range. English and Nepali digits are parsed using the presentation locale. A selected land unit names the unit used by the farmer-entered per-unit rate; the calculator does not silently convert the rate.

## UI and accessibility

Farm planning appears after the M6.3 general-purpose tools in the scrollable Hisab destination and remains usable without a farm. Changing the selector only changes the visible form; calculation and validation occur only after the Calculate action. Actions meet the 48dp minimum, invalid fields receive localized inline errors and focus, and results use polite accessibility live regions. The selected form and normal Android input view state survive Activity recreation.

## Automated evidence

- `KisanCalculatorsTest`: representative decimal formulas, zero inputs, and negative guards for all five calculators.
- `DecimalValueFormatterTest`: localized English/Nepali whole-count parsing plus fraction, negative, and overflow rejection.
- `FarmLabelsMappingTest` and `FarmOrderingTest`: exhaustive selector labels and stable product order.
- localization parity and on-device resource-resolution coverage for English and Nepali strings.
- `FarmActivityWorkflowTest`: compiled no-farm seed-calculator smoke flow.
- `:app:verifyLocal`: JVM tests, lint, debug assembly, and Android-test compilation, with JSON evidence and APK checksum.
- `:app:verifyReleaseMetadata`, workflow syntax checks, and `git diff --check` remain delivery gates.

The completion run passed 221 JVM tests, Debug lint with the unchanged 56-warning baseline and no errors, Debug assembly, Android-test compilation, release-metadata validation, workflow lint, shell syntax validation, evidence/APK checksum agreement, and `git diff --check`.

## Manual/device validation

Manual emulator/device validation is deferred under the current instruction. Before production release, verify Nepali keyboard entry, selector restoration, small-screen scrolling, long translated labels, inline error focus, and TalkBack announcements for all five result types.
