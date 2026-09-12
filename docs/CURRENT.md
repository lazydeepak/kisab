# Current Work

Documentation reconciled 2026-09-05 against clean `docs/context-layer` at `8f3d6fc`. This is the current status router; use the [README index](../README.md) for documentation authority and navigation.

## Implementation and release baseline

- M0–M6.4.1 established offline records, Party → Trade → Settlement accounting projections, calculators and the Android shell. Later M7/UX work redesigned farmer workflows; M9 added grain units/text sizing, M10 farm activities, M11 activity-aware trades, and M12 instrumentation repairs. Their milestone/design records retain scoped acceptance evidence.
- M13 records production-signed v0.2.2/code 5 publication and live same-signer OTA upgrade with data preservation: [validation record](validation/M13_PILOT_RELEASE_OTA_VALIDATION.md). These are repository-recorded results, not a fresh check of the remote release.
- M15 generic cash entry is implemented (`8cccf91`); [validation](validation/M15_GENERIC_CASH_ENTRY_VALIDATION.md) and the later M14 device re-check (`a12457c`) supersede its initial Received-tile re-check limitation.
- Later commits add empty-Khata CTA repairs, sale/purchase labels and spacing, Record theme adjustments, shared button dimensions and rounded styling. These changes are beyond the published artifact evidence even though source version remains 0.2.2/code 5.

## Verification and Pass 3

Normal engineering gate: `./gradlew :app:verifyLocal` (JVM tests, lintDebug, debug APK assembly, Android-test Kotlin compilation). It does **not** run connected instrumentation, human language review, or a release/device campaign. Apply [ADR-0003](decisions/ADR-0003-agent-workflow-and-validation-depth.md) proportionally; documentation changes need documentation checks.

- `580462e` repairs matcher compilation and adds evidence invalidation plus `commitSha` in `app/build/reports/verification/local-ci-evidence.json`.
- `4b770c6` and `8f3d6fc` repair GONE-view assertions; shared button dimensions are referenced by the rounded style/drawable.
- Existing local evidence inspected during this audit reports PASS at `8f3d6fcbec37c31bdd9d4ed1e33066cdee2d8d67`, Gradle 9.6.1 / Java 21.0.12. This is retained local evidence, not a gate rerun in this documentation session. Match report commit and APK digest before reuse; a HEAD field alone does not describe uncommitted source changes.
- Pass 3 added `button_danger_background.xml` with no reference applying it. `08e5f92` wired it to `farmDetailsResetButton`/`farmDetailsDeleteButton` and `verifyLocal` passed; targeted device visual acceptance (danger rest/pressed states, dark mode, keyboard-overlap) is recorded in [validation](validation/STYLING_DANGER_BUTTON_WIRING_VALIDATION.md). The broader styling audit (full screen/dialog sweep, light-mode captures, pressed-state contrast review) remains open.
- Prior M12/M13/RC device PASS records remain valid for their recorded candidates; they do not establish connected-test or visual PASS for later Pass 3 changes.

## Pending workstreams

| Workstream | Remaining work / completion evidence |
| --- | --- |
| Global styling audit | Full-surface sweep (10 screens + sheets/dialogs, light/dark, text scaling, touch targets, insets, keyboard overlap) recorded in [validation](validation/STYLING_DANGER_BUTTON_WIRING_VALIDATION.md). Open defects: disabled-over-light label contrast 1.51:1 and pressed label (`#FF5252`) 3.19:1 from hardcoded white danger-button label; minor Add-Farm IME overlap (CREATE FARM dips under keyboard, scrollable); minor hardcoded small-padding drift vs `spacing_*` scale. Remaining: rework danger label/fill states and address MINOR findings. |
| English/Nepali validation | Automated `LocalizationParityTest` covers resource keys, blanks, duplicates, positional placeholders and plural parity in `values`/`values-ne`. Recheck affected resources with targeted tests/lint; human bilingual review must separately validate meaning, terminology, dates/numbers, truncation and real workflows. Historical localization PASS is scoped, not blanket approval of current copy. |
| Account setup / email login | Local identity/linking and provider-neutral secure-session contracts exist. Real backend/provider integration and email login are not implemented; resolve method and lifecycle before implementation. See [account contract](architecture/Kisab-Online-Account-Session.md). |
| M14 farmer pilot | P01 staged per [session record](validation/PILOT_01_SESSION_P01.md); recorded session count remains 0/3. Await live facilitator sessions with at least three consented participants. Do not fabricate observations or alter the staged device environment. |

Human/device validation continues alongside unrelated engineering. Participant availability blocks pilot completion, not documentation or other independent engineering. A named release gate can still require specific device/human evidence before release approval.

For M14 use the [milestone](milestones/M14_PILOT_01_REAL_FARMER_VALIDATION.md), [protocol](validation/PILOT_01_PROTOCOL_v0.2.2.md) and [session template](validation/PILOT_01_SESSION_TEMPLATE.md). Write observations only from live sessions; consolidate the planned results after ≥3 sessions. Deferred pilot findings remain in [BACKLOG](BACKLOG.md).
