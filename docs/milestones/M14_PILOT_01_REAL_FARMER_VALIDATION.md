# Kisab M14 — PILOT-01 Structured Real-Farmer Validation

## Status

**M14 DISPOSITION: IN PROGRESS — pilot kit complete, rehearsal PASS, facilitated sessions pending**

M14 is a field-validation milestone, not a development milestone. Released **v0.2.2 (code 5)** is the immutable baseline. This record defines the scope; execution evidence accumulates in:

- `docs/validation/PILOT_01_PROTOCOL_v0.2.2.md` — refreshed protocol (supersedes the v0.2.0 draft)
- `docs/validation/PILOT_01_SESSION_TEMPLATE.md` — per-participant anonymized session record
- `docs/validation/PILOT_01_REHEARSAL_v0.2.2.md` — facilitator dry-run against the released build (PASS, with findings F1–F6)
- `docs/validation/PILOT_01_RESULTS_v0.2.2.md` — consolidated results (**to be created after ≥3 sessions**)

## 1. Why this milestone

The repository's own records state that no real user has ever participated in validation (`V0.2.0_PILOT_CHECKLIST.md`: glossary terms remain Provisional/Pending; "human comprehension remains unverified"), while engineering validation reached 486 JVM tests + a fully green 138-test connected baseline + verified production OTA. The M13 audit concluded further feature work risks designing from assumptions.

## 2. Scope

1. Protocol refresh for v0.2.2 workflows (activities in trade sheets, activity breakdown, मन/पाथी/मुरी units, backup comprehension, generic-cash probe T14/T15).
2. Session instruments: consent script, observation template, terminology evidence table with SUPPORTED / PROBLEMATIC / INCONCLUSIVE / REVISED states.
3. Facilitator rehearsal on ZA22374XPC against production-signed v0.2.2 (done — see rehearsal record).
4. ≥3 consented farmer sessions per the configuration matrix (Nepali/dark/landscape/large-text/grain-units distributed).
5. Aggregated findings, P0–P3 triage, evidence-ranked M15 candidates in the results record.

## 3. Explicit non-goals

- No feature code during observation (P0 data-safety exception per directive §12).
- No analytics/telemetry/cloud; no participant PII in the repository.
- No mass glossary conversion to Confirmed without per-term evidence.
- No new subsystem because the offline core is mature.

## 4. Completion gates

| Gate | Status |
|---|---|
| Protocol refreshed & committed | DONE |
| Session instruments committed | DONE |
| Standardized starting-state + reset procedure defined (clean-start; UI-delete preferred, reinstall fallback) | DONE |
| Facilitator runbook committed | DONE |
| Rehearsal on released build | PASS |
| ≥3 completed consented sessions | PENDING (facilitator) |
| Generic-cash gap probed every session | PENDING |
| Terminology evidence recorded | PENDING |
| Findings triaged P0–P3 | PENDING |
| M9 physical debt closed/rescheduled | PARTIAL — mechanism checks executed in rehearsal (dark mode, landscape, saved-size relaunch, Nepali render, mana workflow); human-legibility confirmation moves into sessions |
| Results record + ranked M15 candidates | PENDING |

M14 reaches final PASS only when the pending rows are satisfied by genuine sessions recorded in `PILOT_01_RESULTS_v0.2.2.md`.

## 5. Known pre-session observations carried forward

Rehearsal findings F1–F6 are facilitator briefings and M15 candidates-in-waiting, not fixes: IME covering SAVE (F1), ADD PRODUCT ↔ DELETE neutral-button swap on the production dialog (F2), silent duplicate-product rejection (F3), mandatory supplier for cash buys (F4), supplier-present cash purchases invisible in Today expenses (F5), over-production shown as "All accounted for" (F6). Each gets confirmed/refuted by observed participant behavior before any code changes.

## 6. Files

- `docs/milestones/M14_PILOT_01_REAL_FARMER_VALIDATION.md` (this record)
- `docs/validation/PILOT_01_PROTOCOL_v0.2.2.md`
- `docs/validation/PILOT_01_SESSION_TEMPLATE.md`
- `docs/validation/PILOT_01_REHEARSAL_v0.2.2.md`
