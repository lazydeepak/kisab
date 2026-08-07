# Kisab M4-04 — Defect Register

Stable defect register for findings captured during the M4-04 physical-device and guided farmer-pilot validation. M4-04 captures defects; M4-05 corrects them. No production behavior is changed on the validation branch when a pilot defect is found.

## Severity legend

- **BLOCKING** — data loss, backup corruption, incorrect monetary values, transaction identity corruption, unrecoverable crash loop, destructive action without confirmation, or terminology causing the opposite transaction type. Stops the pilot.
- **MAJOR** — workflow-blocking but recoverable; significant mis-interpretation risk.
- **MINOR** — cosmetic or low-impact usability issue.
- **OBSERVATION** — wording preference or non-blocking observation (not blocking unless it prevents task completion or creates a materially incorrect interpretation).

## Register

| ID | Summary | Session/Device | Reproduction steps | Expected | Observed | Severity | Data-loss risk | Workflow-blocking | Terminology-critical | Evidence | Recommended M4-05 action | Status |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| M4-04-D001 | No Transport category for crop-farm costs | Scenario B (M4-04-02), Pixel 7a | Create crop farm, record a transport cost | A dedicated transport category is available | Transport recorded under `Other expense` because only `Feed` / `Supplies` / `Labor` / `Other expense` exist for expenses | MINOR | No | No | No | Screenshot `12b_scenarioB_complete.png`; transaction `Transport 150.00 NPR` as `Other expense` | Consider adding a `Transport` expense category (or renaming) based on pilot need | OPEN |
| M4-04-D002 | Date/time picker is multi-step and slow for editing a past date | Scenario B (M4-04-02), Pixel 7a | Tap `CHANGE DATE AND TIME`, then navigate picker | Editing a past date is quick and obvious | Picker requires date select + time confirm across multiple dialogs; friction for editing historical transactions | OBSERVATION | No | No | No | Screenshot `06_date_picker.png` | Consider a faster date preset or shorter flow; evaluate with real users | OPEN |

## Status values

- `OPEN` — captured, not yet corrected.
- `CONFIRMED` — reproduced with evidence on a physical device.
- `HANDED TO M4-05` — registered for correction in the M4-05 workstream.

## Pilot stop conditions

The pilot stops immediately for any BLOCKING defect (see severity legend). Minor layout or wording issues do not automatically stop unrelated sessions.
