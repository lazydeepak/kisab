# Kisab M4-05 — F002 Action-Bar Input Interception — Implementation and Validation Record

Records the implementation and validation evidence for M4-05-F002 (the legacy `DarkActionBar` theme intercepting taps intended for recreated content on API-26/Low-RAM devices), so every claim is traceable to a build state, lane, and test result.

> **Policy:** APKs, keystores, passwords, raw participant data and large screen recordings are **never committed**. Evidence hashes are derived from actual artifacts (`shasum -a 256`), never hand-copied.

## Finding reference

- **ID:** `M4-05-F002`
- **Title:** The app-wide `AppTheme` still used the app-compat legacy action bar (`Theme.AppCompat.Light.DarkActionBar`), which intercepted taps narrowly aimed at recreated content at odd rotations/layout states on low-RAM/API-26 devices, breaking the "Save → keep editing without a listed farm row" interaction.
- **Source:** **instrumented test finding** during the M4-05 RC regression (upgrade/recreation coverage) on the API-26 (Android 8.0) emulator supplemental lane. The same interaction kept failing in `FarmBackupIntegrationTest` until the theme fix.
- **Severity:** MAJOR (functional: a supported daily flow — keep-editing after save — becomes unclickable on low-RAM/older devices). Corrected before the `v0.2.0` RC freeze.

## Problem

Kisab's editor flow intentionally re-displays a transaction editor after Save (keep-editing) and after restore, running against the same layout region. On the API-26 Low-RAM emulator, Espresso's `scrollTo` targets that region kept resolving against the app-compat **action bar** that the theme mounted; the action bar won the hit-window for taps meant for recreated content, so `recordExpenseButton`/editor views could be reported as not matching descendants (`width=0, height=0`, layout-requested) or the tap landed on the chrome instead of the editor.

Until the fix, this reproduced deterministically enough to block the `v0.2.0` RC regression on the supplemental API-26 lane.

## Fix

`app/src/main/res/values/themes.xml` — one line: `AppTheme` parent changes from `Theme.AppCompat.Light.DarkActionBar` to `Theme.AppCompat.Light.NoActionBar`.

Kisab draws its own in-content controls; it never used the app-compat action bar as a product surface. Removing the legacy action bar from the theme eliminates the input-interception surface entirely, with no dependency or layout change.

## Change set (working tree on `feature/m4-05-defect-correction`)

| Lane | File | Change |
| --- | --- | --- |
| Finding record | `docs/validation/M4_05_F002_ACTIONBAR_INPUT_INTERCEPTION.md` | This record |
| Theme | `app/src/main/res/values/themes.xml` | `AppTheme` parent `DarkActionBar` → `NoActionBar` (one line) |

No test code was changed for this fix. `FarmBackupIntegrationTest` and the workflow tests are exercised as-is; the pre-existing `FarmBackupIntegrationTest` upgrade/recreation coverage is the regression net.

## Validation evidence

### Targeted regression — previously failing test, fixed code only

Command: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.susankhya.kisab.FarmBackupIntegrationTest` (API-26 Android 8.0 emulator).

Result: **PASS** with zero test edits — the exact `FarmBackupIntegrationTest` file, including the keep-editing upgrade scenario that previously failed, now passes unchanged.

### Full on-device suite (Moto Edge 60 Fusion, serial `ZA22374XPC`)

Command: `./gradlew :app:connectedDebugAndroidTest`.

Result: **64/64 PASS, 0 failure, 0 skipped** (9 classes). Last failure observed in this record's lineage was `FarmActivityWorkflowTest.recreationPreservesEditorDraft` once (an `Espresso` scroll-to-before-layout flake, `width=0/height=0` pre-measure); the class re-ran green standalone (`BUILD SUCCESSFUL in 1m 16s`) and the subsequent full-suite re-run was fully green. It is tracked separately as a test-robustness note, not a product defect.

### JVM unit tests

Command: `./gradlew :app:testDebugUnitTest` (JDK 21 baseline).

Result: **114/114 PASS**.

## Artifacts

| Type | Path (local) | SHA-256 |
| --- | --- | --- |
| (future signed RC) | `app/build/outputs/apk/release/app-release.apk` | recorded with RC evidence |

## Decisions and deferred items

- **Product surface is clean:** the app-compat action bar was never a product surface; `NoActionBar` loses nothing.
- **Test-robustness note (tracked, not fixed here):** one `scrollTo`-before-layout flake observed in `FarmActivityWorkflowTest` on the Moto (not the API-26 issue), a separate, non-product concern.
- **API-26 design note recorded with this finding:** low-RAM device behavior (activity recreation, sub-`96dp`/low-res weapons, tap arbitration) needs the kind of lean layout the editor already has; the theme fix is the low-risk correction for RC, and the M5-00 screen-wrapper conventions (see `docs/charter/Kisab-Product-Charter.md`) codify the editor/back behavior going forward.