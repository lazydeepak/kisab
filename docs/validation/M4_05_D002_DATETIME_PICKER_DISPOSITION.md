# Kisab M4-05 — D002 Date/Time Picker Evaluation and Disposition

Disposition of M4-04-D002 (date/time picker is multi-step), decided from on-device evidence in M4-05 rather than from the original observation alone.

## Disposition

**DEFERRED / ACCEPTED — no production change.**

The past-date editing flow is correct and understandable under realistic use. The flow is two sequential platform dialogs (date, then time); the extra confirm step is a tolerable interaction cost, not a source of confusion or data-risk. No meaningful friction was demonstrated by device exercise.

## Why DEFERRED (evidence, not expectation)

The interaction works correctly on the physical device in every realistic case exercised:

| Case | Behavior observed | Result |
| --- | --- | --- |
| Create a transaction for today, then change it to a previous date | Editor defaults to now; picker opens pre-set to that current timestamp; changing to Jan 5, 2024 persists exactly `2024-01-05T09:30:00` in device time and renders the changed local date/time in the recent row after saving | PASS |
| Edit an existing older transaction (both date and time) | Old feed `2024-01-10T12:00Z` edited to `2024-02-03T08:45`; both components changed; recent row shows the edited local date/time | PASS |
| Cancel at the date picker (midway) | Back before confirming: editor draft (amount) and the original timestamp are both unchanged | PASS |
| Cancel at the time picker after choosing a date | Changing the date does **not** commit until the time picker confirms; backing out of the time picker leaves the original timestamp and draft untouched — no partial/state corruption | PASS |
| Repeat in English and Nepali | Same workflow completes under `ne-NP`, button label resolves to `मिति र समय परिवर्तन गर्नुहोस्`, values persist identically | PASS |

Test lane: `D002DateTimePickerEvaluationTest` — 5/5 PASS on Moto Edge 60 Fusion (serial `ZA22374XPC`, API 36).

## Why it is not confusing or error-prone

- The two-step flow maps one-step-per-concern (date, then time) with clear dialog titles; the platform pickers render in the device locale.
- The single commit point is exactly "OK on the time picker"; nowhere else mutates state, so partial/cancelled flows are impossible by construction (`showDateTimePickers` in `FarmActivity.kt` only calls `copy(occurredAt=…)` in the time-picker callback, and only *after* the time picker confirms).
- Existing M4-03 workflow instrumentation (`FarmActivityWorkflowTest#repeatedExpenseDerivesCurrencyAndSuppliesCurrentTime`, `#editPreservesIdAndRecalculatesTotals`) exercises this picker path, and all M4-04 pilot steps completed without participant-reported difficulty on this interaction.

## Not selected reasons

- **RESOLVED** is not met: no meaningful friction beyond "two steps" was demonstrated, and there is no small, bounded correction that would improve correctness.
- Candidate alternative (a quick "recent past date" preset or single combined dialog) would add UI surface for a flow users accomplish reliably; risk/benefit favors deferring.

## Re-open criteria

Re-evaluate D002 if: (a) a future user session shows repeated correction to a past date at scale, or (b) farmers in pilot evidence materially mis-record timestamps because of the picker structure. Otherwise D002 stays deferred and is not required for the M4 exit gate.

## Artifacts

- Test: `app/src/androidTest/kotlin/com/susankhya/kisab/D002DateTimePickerEvaluationTest.kt` (5/5 on Moto).
- Disposition recorded: `docs/validation/M4_05_D002_DATE_TIME_PICKER_DISPOSITION.md` (this file).
- No production code changed for D002.