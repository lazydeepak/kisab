# Kisab M6.4 — Farm Input Calculators — Design Record

> **Status: COMPLETE (automated and physical/emulator device validation)** on `feature/m6-4-farm-input-calculators`, based on merged M6.3 `main` at `78925e1`. Evidence below was recorded from the exact validated head `9d52b2d` APK on an API-36 physical device and an API-26 emulator.

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

## Manual/device validation evidence

Executed for head `9d52b2d` using the installed APK `app-debug.apk`
(SHA-256 `813edcb91796e7911638f5e6cd62113fc8cb1879a38d78661774c731f1516f95`), pulled back from each device and re-hashed to confirm identity.

### Environment A — Motorola Edge 60 Fusion (physical), Android 16 / API 36, 1220×2712 @ 450 dpi

All five calculators computed correctly for representative farmer-entered values, each independently recomputed by hand:

| Calculator | Inputs | Result on device | Manual check |
| --- | --- | --- | --- |
| Seed | area 2.5, rate 12, price/kg 80 | Quantity: 30 kg; Total cost: 2,400 | 2.5×12=30; 30×80=2,400 |
| Fertilizer | area 3, rate 7.5, price/kg 50 | Quantity: 22.5 kg; Total cost: 1,125 | 3×7.5=22.5; 22.5×50=1,125 |
| Feed | 4 animals, 2.5 kg/day, 30 days, price/kg 45 | Total feed: 300 kg; Total cost: 13,500 | 4×2.5×30=300; 300×45=13,500 |
| Milk | 3 animals, 6.5 L/day, 10 days, price/L 90 | Total milk: 195 L; Revenue: 17,550 | 3×6.5×10=195; 195×90=17,550 |
| Crop yield | area 1.5, yield 1200, price/kg 35 | Total yield: 1,800 kg; Revenue: 63,000 | 1.5×1200=1,800; 1800×35=63,000 |

Other scenarios:

- **Decimal values** accepted in every permitted decimal field (area/rates/days/prices all exercise fraction input).
- **Zero inputs** accepted: feed 0/0/0/0 → "Total feed: 0 kg; Total cost: 0".
- **Fractional animal count**: the integer `inputType="number"` count field filters the decimal separator at the input layer (probe typed `2.5`, field text became `25`); the `parseNonNegativeWhole` guard additionally rejects fractions (unit-covered).
- **Blank count**: error set, field receives focus (`focused=true`), no result produced.
- **Out-of-range count**: `999999999999` rejected by `intValueExact` (13 digits), field error+focus, previous result retained.
- **Negative input**: platform numeric keypads (device keyboard and the installed Hamro Nepali keyboard) do not offer a minus key on `number`/`numberDecimal` fields; the negative guard is exercised at the formatter/domain layer by JVM tests. Regional-keyboard-specific entry remains a human-in-loop release-candidate item.
- **Nepali UI**: farm-planning section renders "फार्म योजना", "गणक", and calculator labels in Nepali; results render Devanagari digits, e.g. "परिमाण: ३० किलो / कुल लागत: २,४००" and "कुल दुध: १९५ लीटर / राजस्व: १७,५५०".
- **Nepali-digit entry**: numeric keypads commit ASCII digits for these fields; Devanagari-digit parsing is unit-tested (`parseNonNegativeWhole(ne, "१२") == 12`) and non-ASCII shell injection is not possible (`adb input text` raises on non-ASCII). Confirmation with a specific Nepali keyboard remains a human-in-loop item.
- **Land-unit selector**: dropdown lists all 8 units (Square metre → Dhur); switching to Ropani leaves the rate un-converted (area 1 × rate 100 → 100 kg, cost 1,000), per the "no silent conversion" rule.
- **Activity recreation**: force-rotation on the Milk form restored the selected calculator and inputs (3 / 6.5 / 10 / 90); recompute reproduced 195 L / 17,550. Computed result text itself is transient by design (resets hidden; the same inputs recompute identically).
- **Away/back navigation**: Milk calculator stayed selected with inputs and visible result.
- **Long Nepali labels + keyboard-open scrolling**: Calculate action and results remain reachable while the IME is open.
- **TalkBack**: service enabled and active; double-tap activated Calculate and TalkBack emitted announcements on the accessibility audio stream (`AUDIO_USAGE_ASSISTANCE_ACCESSIBILITY`) at activation; result TextViews carry `android:accessibilityLiveRegion="polite"`.
- **No persistence/accounting side effects**: full app-data tree before and after the calculator battery contains only `shared_prefs/kisab_app_language.xml` and the foundation `files/profileInstalled`; **no** `kisab_farm_store.xml`, databases, or backup files exist, and a launch-free calculate produced identical pref/file hashes (`kisab_app_language.xml` md5 `c8f4888530b677ababbf4835b3033ea5` unchanged; `profileInstalled` changes only on process launch, not on calculation). Home still offers create-farm and Hisab still shows the no-farm prompt — no Farm, Entry, Transaction, Trade, Settlement, Khata, or balance was created or modified.

### Environment B — Android emulator, API 26 (Android 8.0), 1080×1920; small-screen pass at 420 dpi (~411 dp wide)

- Seed 2.5×12×80 → "Quantity: 30 kg; Total cost: 2,400"; Nepali render with Devanagari digits ("परिमाण: ३० किलो / कुल लागत: २,४००").
- No crashes on API 26; scrolling reaches the farm-planning section; rotation recreation restores inputs and calculator selection.
- Results below the fold on the compact display require scrolling (expected for the ScrollView layout).

### Disposition

Manual and device validation **passed** for every exercised scenario on both the API-36 physical device and the API-26 emulator; **no functional defect** was found in the review or on-device runs. Verification of the exact TalkBack spoken transcript and entry through a user's specific regional keyboard remain release-candidate human-in-loop checks as noted above.
