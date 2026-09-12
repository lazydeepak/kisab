# Localization recheck — private-build expiry day strings

> **Scope:** English/Nepali validation workstream (2026-09-12). Rechecked
> current `values`/`values-ne` resources with the automated parity suite and
> lint, and fixed the defects lint surfaced. Human bilingual review remains a
> separate gate (see "Open items" below).

## Automated checks

- `LocalizationParityTest` (10 tests: parseability, duplicates, key parity in
  both directions, empty values, placeholder signatures, plural-quantity
  parity, non-translatable exclusions): **PASS** via
  `./gradlew :app:testDebugUnitTest --tests "*LocalizationParityTest"`.
- `lintDebug` translation rules: `MissingTranslation`, `ExtraTranslation`,
  `MissingQuantity` — **none present**.
- `PluralsCandidate` — **3 defects found and fixed** (see below); count now 0.

## Defect fixed: bare "%1$d days" strings

Three private-build expiry strings used `%1$d days`, so "expires in 1 days"
rendered in English when a single day remained:

- `private_build_expiry_banner_critical_format`
- `private_build_expiry_dialog_warning_message_format`
- `private_build_expiry_dialog_critical_message_format`

Converted each to `<plurals>` (`one` / `other`) in `values/strings.xml` and
`values-ne/strings.xml` (Nepali keeps the same text in both quantities, per
Nepali grammar; placeholder signatures and plural-quantity parity preserved).
`FarmActivity.kt` switched the four call sites to
`resources.getQuantityString(...)` passing the day count as both the quantity
selector and a format argument.

Verified: `./gradlew :app:verifyLocal` **PASS** (JVM tests incl. parity,
compileDebug with new `R.plurals` references, lintDebug with
`PluralsCandidate` count 0, lint, debug APK, android-test compile).

## Open items (human bilingual review)

These need a bilingual maintainer/reviewer; mechanical tooling cannot
adjudicate them:

- Meaning/terminology pass over `values-ne` for farming terms and the
  private-build expiry copy.
- `private_build_expiry_dialog_critical_title` and its warning counterpart
  share the same Nepali text (`चाँडै अद्यावधिक गर्नुहोस्`) although the
  English titles differ ("Update soon" vs "Update available soon"); confirm
  the shared Nepali wording is intentional.
- Date/number formatting in expiry dialogs uses `presentationLocale` /
  `deviceZone`; confirm Nepali date rendering at runtime on a real device.
- The four remaining `SetTextI18n` sites (`FarmActivity.kt:3244, 3388, 4410,
  5287`) concatenate runtime strings (party name + role/contact, product
  name, multi-line message bodies). Lint flags them but the patterns are
  order-neutral formatting; confirm no reorder-driven meaning loss in the
  Nepali UI.