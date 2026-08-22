# Kisab M12 — Instrumentation Baseline Repair

## Status

**M12 FINAL DISPOSITION: PASS**

Implemented on `feat/farm-activities` on top of M11 at `5630e97`. This record is the authority for the M12 scope: restoring the Android connected-test suite as a trustworthy regression gate by eliminating the 33 stale instrumentation failures inherited unchanged from the M7-era UI transition (documented as accepted baseline debt in the M9, M10, and M11 records), without hiding flaky tests and without changing product behavior.

Validation evidence: `docs/validation/M12_INSTRUMENTATION_BASELINE_REPAIR_VALIDATION.md` (484 unit tests / 0 failures, lint 0 errors, `verifyLocal` passed, connected suite **138 tests / 0 failures** on `ZA22374XPC`).

## 1. Problem being solved

Since the M7 farmer-workflow redesign, the device suite carried 33 failures in every run. Because the set was stable and pre-dated current work, each milestone recorded it as "zero new failures versus baseline" — but a suite that always fails is not a gate: real regressions hide inside known noise, and the retired-UI tests no longer described the product. M12's objective was to classify every failure (STALE UI TEST / OBSOLETE DUPLICATE / GENUINE PRODUCT DEFECT / ENVIRONMENT) and repair the suite against the current shell.

## 2. Scope boundary

This milestone changes **no product logic**. All edits live in `app/src/androidTest/` plus two rider strings (EN/NE breakdown copy) in `app/src/main/res/values*/strings.xml`. The domain layer, persistence schema (14), UI behavior, and backup format are untouched.

## 3. Classification outcome

Every one of the 33 failures was classified; none was a genuine product defect:

- **29 rewritten** — re-targeted at reachable current-shell flows (edit-mode transaction editor from recent rows, record-sheet verbs, add/update import preview dialog, adapter-backed chooser via `onData`, instant-based time assertions).
- **3 deleted** with rationale comments left in source (`firstIncomeQuickActionRecordsWithoutCurrencyInput`, `repeatedExpenseDerivesCurrencyAndSuppliesCurrentTime`, `recordButtonWhileDirtyConfirmsDiscard`) — their subjects were retired UI or superseded coverage.
- **1 adapted** — the currency chooser test now asserts dialog options through `onData` because the chooser is an adapter view.

## 4. Infrastructure defects fixed (the non-obvious 20%)

The bulk of repairs are mechanical re-targeting, but five latent defects were diagnosed and fixed properly rather than retried around:

1. **Per-app locale relaunch race.** Setting `LocaleManager.applicationLocales` asynchronously relaunches activities; eleven `NoActivityResumedException` failures mapped exactly to the three classes mutating it in setup/teardown. New shared barrier helpers (`LocalizationTestContexts.kt`) poll the system service until locales settle, then drain the main thread.
2. **Stale home after service-side seeding.** Store writes bypass the activity renderer. Tests seed pre-launch where possible; otherwise they use `awaitEditorForTransaction` (bounded retry for an interactive row) instead of racing `recreate()`.
3. **Timezone-sensitive string assertion.** Wall-clock strings differ per device zone (this device runs JST); the round-trip assertion now compares `Instant`s.
4. **Adapter-dialog addressing.** Single-choice dialogs expose items only through their adapters.
5. **Row-order coupling.** Post-upgrade rendering asserts scan all recent rows instead of assuming row 0.

## 5. Product-contract clarifications documented

Two test files still encoded the pre-M10 replace-all import semantics. The verified contract — `replaceFarmWith` is multi-farm safe ("Other local farms are never wiped") — is now asserted explicitly, and the validation record states it as the reference behavior. The permanently-gone legacy compatibility block in `activity_shell.xml` is likewise documented: its text views remain populated but unreachable, which explains why text assertions passed while visibility assertions failed historically.

## 6. Rider delivered

Breakdown section/empty-state copy updated for EN/NE parity ("Records by activity" / records-or-trades phrasing) so the section title reflects trade-aware content added in M11.

## 7. Acceptance criteria (from the directive)

| Criterion | Result |
|---|---|
| Every baseline failure classified with evidence | PASS — taxonomy table in the validation record |
| Connected suite reports zero unexpected failures | PASS — 138/0 |
| No test hidden as flaky without root cause | PASS — three deletions carry rationale comments; all others repaired |
| Physical close-out flows validated on `ZA22374XPC` | PASS — full suite executed end-to-end on the physical device; supplier/customer/settlement, production allocation, and activity attribution covered by the repaired integration classes running on-device |
| `verifyLocal` passes | PASS — evidence JSON status `passed` |
| EN/NE parity rider | DELIVERED — two string pairs updated, smoke suites green |

## 8. Explicit non-goals

- No new product features; no schema change; no refactor of production code.
- No CI infrastructure changes beyond shared test helpers.
- The legacy gone-block remains in the layout (it is inert); removal would be a separate cleanup decision.

## 9. Files touched

- `app/src/androidTest/kotlin/com/susankhya/kisab/` — 7 test classes plus shared `LocalizationTestContexts.kt` (+373/−379 lines).
- `app/src/main/res/values/strings.xml`, `values-ne/strings.xml` — 2 string pairs (rider).
- `docs/milestones/M12_INSTRUMENTATION_BASELINE_REPAIR.md`, `docs/validation/M12_INSTRUMENTATION_BASELINE_REPAIR_VALIDATION.md`.
