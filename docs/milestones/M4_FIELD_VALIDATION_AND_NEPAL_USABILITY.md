# Kisab M4 — Field Validation and Nepal Usability

## Status

**IN PROGRESS.** This document defines the M4 milestone. M4-00 is complete; M4-01 (Android string-resource and Nepali localization foundation) is implemented and pending review on `feature/m4-01-localization-foundation`; the remaining workstreams (M4-02 … M4-05) are not started. See `docs/validation/M4_01_LOCALIZATION_VALIDATION.md` for the M4-01 validation record.

## Purpose

Convert the technically correct offline `v0.1.0` application into a product that can be understood and used by Nepali farmers with minimal developer assistance. M0–M3 built a correct, offline, single-farm management application; M4 makes it usable and comprehensible for its primary audience in Nepal, and validates that claim on physical devices and with guided farmer scenarios before the next release.

## Scope

M4 is organized into the following workstreams:

- **M4-00 — Post-release documentation and backlog reconciliation.** Align repository documentation with the published `v0.1.0` release state, reconcile the roadmap, and establish the M4 working backlog.
- **M4-01 — Android string-resource and Nepali localization foundation.** Move all user-facing text into Android string resources and add the initial Nepali (`values-ne/`) resource set governed by the terminology glossary.
- **M4-02 — Nepal-oriented currency, number, date and time presentation.** Deterministic NPR currency, number, date and time formatting and display rules, without breaking existing persistence or backup compatibility.
- **M4-03 — First-run and daily transaction-entry usability.** Improve the first-run experience and the daily transaction-entry flow so the core tasks (record income/expense, review balance) are fast and unambiguous for a farmer.
- **M4-04 — Physical-device and guided farmer-pilot validation.** Execute the physical-device validation matrix and a small guided pilot on representative devices and farmer scenarios, capturing evidence for the M4 exit gate.
- **M4-05 — Defect correction, release-candidate validation and closeout.** Correct defects found in M4-04, validate a signed `v0.2.0` release candidate, and close out the milestone.

## Localization rules

The following initial decisions govern M4 localization:

- Use standard Android string resources (`res/values/strings.xml` and locale-specific variants).
- Default English resources remain mandatory and complete.
- General Nepali resources use `values-ne/`.
- Every Nepali key must have an English default counterpart in `res/values/strings.xml`.
- No new cross-platform localization framework is introduced in M4.
- Layouts must tolerate longer Nepali labels; fixed-width assumptions based on English text are not acceptable.
- User-facing terminology is governed through the glossary at `docs/localization/NEPALI_TERMINOLOGY.md`; new terms must be added there before they are shipped.

## Nepal-oriented data rules

- New farms should default to `NPR` (Nepali rupee), subject to implementation review during M4.
- Money remains stored in integer minor units.
- Currency display must be locale-aware.
- Persist timestamps in UTC.
- Present timestamps using the device's local timezone.
- Bikram Sambat (B.S.) calendar support is deferred until pilot evidence establishes a real requirement.
- Existing persistence and backup compatibility must not be broken: the `v0.1.0` persistence schema and backup envelope remain readable, and existing farm data must upgrade without loss.

## Physical validation matrix

M4 requires validated results for:

- clean installation;
- upgrade over `v0.1.0`;
- force-stop and cold relaunch;
- full device restart;
- transaction create/edit/delete;
- backup export;
- invalid backup rejection;
- restore and immediate recreation;
- English locale;
- Nepali locale;
- Pixel 7a as the reference device;
- at least one older or lower-resource Android device;
- realistic multi-day farm data.

## Pilot expectations

M4 defines a small guided pilot with approximately three representative users or realistic farmer scenarios. The pilot is conducted with explicit, informed participant consent and without collecting analytics, cloud telemetry, or personal data.

For each scenario, the pilot captures:

- terminology confusion;
- failed or slow tasks;
- incorrect assumptions;
- transaction-category gaps;
- backup discoverability;
- amount/date entry friction;
- defects and severity.

Evidence is recorded on `docs/validation/V0.1.0_PILOT_CHECKLIST.md` (and its successor for `v0.2.0`).

## M4 exit gate

M4 must not be marked complete until all of the following hold:

1. All visible application text is resource-backed.
2. English and Nepali resource resolution is tested.
3. NPR, number and local-time presentation rules are deterministic.
4. Existing `v0.1.0` data upgrades without loss.
5. Backup compatibility remains valid.
6. Unit tests, lint, debug build and instrumentation tests pass.
7. Required physical-device validation passes.
8. No unresolved data-loss, workflow-blocking or terminology-critical defects remain.
9. A signed `v0.2.0` release candidate is independently verified.

## Explicit exclusions

The following remain outside M4:

- cloud synchronization;
- accounts and authentication;
- multi-tenant administration;
- multi-farm management;
- full double-entry accounting;
- web dashboard;
- subscriptions and payments;
- broad analytics;
- speculative Foundation abstractions.

## References

- Product scope and roadmap: `docs/charter/Kisab-Product-Charter.md`, `docs/architecture/`.
- Terminology glossary: `docs/localization/NEPALI_TERMINOLOGY.md`.
- Pilot checklist: `docs/validation/V0.1.0_PILOT_CHECKLIST.md`.
- Published release record: `docs/release/RELEASE_NOTES_0.1.0.md`.
