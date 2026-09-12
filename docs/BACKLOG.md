# Backlog

Deferred/future work. Active styling, English/Nepali validation, account planning and QA status are maintained in [CURRENT](CURRENT.md). Human pilot availability does not block unrelated engineering.

## Pilot rehearsal findings — pending session evidence (M15 candidates-in-waiting)

These are facilitator briefings, not approved fixes. Each must be confirmed or refuted by observed participant behavior during PILOT-01 sessions before any code changes.

- **F1** — IME covers SAVE button in record sheets. (Origin: M14 §5 / `PILOT_01_REHEARSAL_v0.2.2.md`)
- **F2** — ADD PRODUCT ↔ DELETE neutral-button swap on the production dialog. (Origin: M14 §5)
- **F3** — Duplicate product rejected silently, no user feedback. (Origin: M14 §5)
- **F4** — Supplier is mandatory for cash buys; friction unverified with real users. (Origin: M14 §5)
- **F5** — Supplier-present cash purchases invisible in Today expenses tile. (Origin: M14 §5)
- **F6** — Over-production shown as "All accounted for" in allocation summary. (Origin: M14 §5)

Source of truth for scope and disposition: `docs/milestones/M14_PILOT_01_REAL_FARMER_VALIDATION.md` §5.

## Global styling audit (2026-09-12 sweep) — accepted minor items

Accepted as-is for the current release; revisit with the next styling pass.
Source: `docs/validation/STYLING_DANGER_BUTTON_WIRING_VALIDATION.md`.

- **S1** — Add Farm screen: CREATE FARM dips below the IME top while the
  farm-name field is focused. The form is a scrollable `ScrollView`, so the
  button is reachable; not fixed because it matches standard scrollable-form
  UX. Revisit if a farmer reports it.
- **S2** — A handful of icon/utility views in `activity_shell.xml` use
  hardcoded small paddings (4/6/8/9dp) instead of the `spacing_*` scale.
  Clean-up candidate for a dedicated styling pass; no functional impact.
