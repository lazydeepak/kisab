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
| M4-04-D001 | No Transport category for crop-farm costs | Scenario B (M4-04-02), Pixel 7a | Create crop farm, record a transport cost | A dedicated transport category is available | Transport recorded under `Other expense` because only `Feed` / `Supplies` / `Labor` / `Other expense` exist for expenses | MINOR | No | No | No | Screenshot `12b_scenarioB_complete.png`; transaction `Transport 150.00 NPR` as `Other expense` | Consider adding a `Transport` expense category (or renaming) based on pilot need | RESOLVED IN M4-05 — `TRANSPORT` category added (see `M4_05_D001_TRANSPORT_CATEGORY.md`) |
| M4-04-D002 | Date/time picker is multi-step and slow for editing a past date | Scenario B (M4-04-02), Pixel 7a | Tap `CHANGE DATE AND TIME`, then navigate picker | Editing a past date is quick and obvious | Picker requires date select + time confirm across multiple dialogs; friction for editing historical transactions | OBSERVATION | No | No | No | Screenshot `06_date_picker.png` | Consider a faster date preset or shorter flow; evaluate with real users | HANDED TO M4-05 |
| M4-04-D003 | Post-upgrade new transaction missing from backup export | M4-04-04, Moto Edge 60 Fusion | (Original) Upgrade v0.1.0→v0.2.0-pilot.1, record a new transaction, export backup | New transaction present in exported backup | **NOT REPRODUCIBLE** — original record was based on byte-identical stale copies of the pre-upgrade export (`08/09/10` share SHA-256 `6d476816…` and `exported_at` `2026-08-08T05:05:08Z` with `05`). Fresh exports after a new post-upgrade transaction contain all transactions (verified by unit test, on-device instrumentation test, and a replay of the signed upgrade on the Moto). | N/A (not a defect) | No | No | No | `.m4-04-evidence/moto-upgrade/d003-verification/` | None required | RESOLVED — NOT A DEFECT |

## Status values

- `OPEN` — captured, not yet corrected.
- `CONFIRMED` — reproduced with evidence on a physical device.
- `HANDED TO M4-05` — registered for correction in the M4-05 workstream.
- `RESOLVED — NOT A DEFECT` — reproduced investigation disproved the report; closed with evidence.
- `RESOLVED IN M4-05` — corrected in the M4-05 workstream with validation evidence.

## Pilot stop conditions

The pilot stops immediately for any BLOCKING defect (see severity legend). Minor layout or wording issues do not automatically stop unrelated sessions.
