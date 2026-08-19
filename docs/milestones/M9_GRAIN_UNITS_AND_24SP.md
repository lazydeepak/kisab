# Kisab M9 — Nepali Traditional Grain Units + 24sp Default Text Baseline

## Status

Implemented on `feat/nepali-units-and-24sp`, based on `main` at `f247bfb` (post-M7 pilot hardening). This record is the authority for the M9 scope: adding governed Mana/Pathi/Muri units everywhere the existing unit system applies, and moving the NORMAL text-size default to 24sp while preserving explicitly saved user preferences.

## Scope

### 1. Nepali traditional grain units

Added three governed units to the existing `ProductUnit` authority so they participate in every surface that already renders, stores, and validates units:

- **Mana** (माना) — smallest traditional grain/volume unit.
- **Pathi** (पाठी) — 8 Mana.
- **Muri** (मुरी) — 20 Pathi = 160 Mana.

Relationship basis (volume/traditional quantity, per Nepali convention):

```text
8 Mana = 1 Pathi
20 Pathi = 1 Muri
160 Mana = 1 Muri
```

No fixed kilogram conversion is implemented: the mass of a Mana/Pathi/Muri depends on the grain and packing, so a universal kg factor would be wrong. The app only converts between the three traditional units, never to/from kg.

#### Surfaces updated

- `ProductUnit` enum (`domain/ProductSale.kt`): `MANA`, `PATHI`, `MURI` appended before `CUSTOM`. Name-based persistence (`ProductUnit.valueOf`) means appending enum values is backward-compatible — no schema bump, no migration. Existing saved data with `KILOGRAM`, `LITRE`, etc. loads unchanged.
- `FarmActivity.supplyUnitLabel` and `productUnitLabel` (exhaustive `when`s): localized labels for the three new units.
- Supply creation dialog and product creation dialog unit pickers: both lists extended with MANA/PATHI/MURI.
- `KisanCalculators`: new `TraditionalGrainUnit` enum (MANA=1, PATHI=8, MURI=160 mana-per-unit) and `convertGrain(value, from, to)` — mirrors the existing `convertLand` pattern, `BigDecimal`-exact, rejects negative input.
- `FarmLabels`: `grainUnitRes` + `grainUnit(context, unit)` mapping.
- Kisan toolbox (`view_kisan_calculator_toolbox.xml`): a "Traditional grain converter" section with quantity input, From/To unit spinners, Convert button, and a result line. Guidance text: "8 mana = 1 pathi and 20 pathi = 1 muri."
- EN + NE strings: `supply_unit_mana/pathi/muri`, `product_unit_mana/pathi/muri`, `grain_converter_title`, `grain_converter_body`, `grain_quantity_value_hint`, `grain_result_format`, `grain_unit_mana/pathi/muri`. Localization parity is enforced by `LocalizationParityTest`.

### 2. 24sp NORMAL default text baseline

The farmer-facing readability default moves from 16sp to 24sp for fresh/default installs, while users who explicitly saved a size keep their choice and their rendering unchanged.

#### Mechanism

`ui/AppTextSize.kt`:

```kotlin
const val MIN_SP = 14
const val DEFAULT_SP = 24   // NORMAL default for fresh/default installs
const val BASE_SP = 16       // authored scale divisor
const val MAX_SP = 36
```

The Activity applies text size as a proportional scale: `scale = load() / BASE_SP`. Because `BASE_SP` (the divisor) is unchanged at 16 and `DEFAULT_SP` moved to 24, a fresh/default install renders text at 24/16 = **1.5×** the authored sizes. A user who previously saved 16 (or any value) loads that exact stored value and divides by the same 16, so their rendering is byte-for-byte identical to before the upgrade. Only the *unset* default changed.

All four scale-application sites in `FarmActivity` divide by `BASE_SP`.

#### Overflow handling

The task rule is "prefer wrapping/stacking/scroll over shrinking text." At 24sp the bottom-nav "Farm Work" label (12sp authored → 18sp rendered) no longer fit on one line within its equal-width slot. The five nav label `TextView`s were changed from `ellipsize="end"` + `maxLines="1"` to `maxLines="2"` + centered gravity, so the label wraps to two centered lines instead of truncating. No other surface clipped at 24sp on the verification device.

## Not implemented

- Any fixed Mana/Pathi/Muri ↔ kilogram conversion.
- Recurring production, inventory projections, or rate suggestions.
- Any change to accounting/domain authorities, persistence schema, backup format, or release/update surfaces.

## Automated evidence

Unit tests (all pass, 443 total):

- `KisanCalculatorsTest`: grain conversions (Mana↔Pathi↔Muri), round-trip, negative rejection.
- `AppTextSizeTest`: `DEFAULT_SP=24`, `BASE_SP=16`, 1.5× default scale, in-range coercion.
- `TraditionalGrainUnitPersistenceTest` (new): schema-12 round-trip of MANA/PATHI/MURI across products, supplies, purchases, usages, production; backup-envelope round-trip.
- `FarmLabelsMappingTest`: distinct grain-unit mappings.
- `LocalizationParityTest` / `LocalizedResourceResolutionTest`: EN/NE parity.

Device tests (API-36 physical device):

- `GrainUnitsAndTextSizeDeviceTest` (new): Settings shows "Text size: 24 px" on fresh install; saved 16 survives relaunch and renders 16; supply creation dialog offers and persists MANA; product creation dialog offers and persists PATHI.
- `KisanToolboxDeviceBatteryTest`: extended with a grain-converter battery (1 Pathi=8 Mana, 1 Muri=20 Pathi, 1 Muri=160 Mana, negative rejection).
- `FarmIntegratedPolishTest`: `khataContextualReceiveButton` click now `scrollTo()`s first (at 24sp the button is below the fold but reachable by scroll — app is scrollable, no app regression).
- Full connected suite: 141 tests, 32 failures — the exact same pre-existing baseline failures on `main` (device/environment-related, targeting hidden legacy buttons); zero new failures introduced by this change.

## Manual/device validation (Moto Edge 60 Fusion, API 36)

- Fresh install → Settings shows **Text size: 24 px**; all primary screens render large with no clipped text.
- Bottom nav "Farm Work" wraps to two lines at 24sp; no truncation.
- Supply unit picker shows all 9 units including mana/pathi/muri; a supply created with mana persisted in the farm store.
- Grain converter: 1 Muri = 160 Mana rendered correctly at 24sp.
- Nepali rendering, dark mode, landscape, and protected-farm restore verification were interrupted by a device disconnection; see the M9 validation record for final status.
