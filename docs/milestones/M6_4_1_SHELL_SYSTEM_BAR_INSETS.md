# Kisab M6.4.1 — Shell System-Bar Insets — Design Record

M6.4.1 corrects the loss of the top status-bar and bottom navigation-bar insets on
the Android shell when `targetSdk=36` forces edge-to-edge rendering, so the app bar
(title, menu button) and bottom navigation stay clear of the system bars. It also
fixes two pre-existing defects surfaced by the full device suite during triage:
the party editor never showed inline validation errors, and the EN/NE row-format
guards asserted equality on placeholder-only format strings.

> **Status: COMPLETE — merged to `main` at `b66a191` via PR #25 (squash).** Validated on `feature/m6-4-1-shell-insets` head `63ac13b`, based on merged M6.4 `main` at `185721a`.

## Defect

Android 15+ (`targetSdk=36`) applies edge-to-edge by default. Kisab previously
consumed **no window insets**: the app bar pinned its title and menu button to the
top of the window (under the status bar), and the bottom navigation could overlap
the gesture/navigation bar. Instrumented tests that tapped the menu button actually
hit the status bar, and UI in M6.1 "top strip" ordering was affected.

## Root cause

- `activity_shell.xml` had no insets listener and no inset-aware padding on the app
  bar or bottom navigation; under edge-to-edge the window content extended into the
  system-bar areas.
- On the API-36 physical device (128px status bar), the menu button occupied y=11–146
  (inside the status bar), so `onView(shellMenuButton).perform(click())` tapped the
  status bar — reproducing as ~21 workflow-test failures, not an environmental flake.

## Fix

- Applied `ViewCompat.setOnApplyWindowInsetsListener` on the shell root
  (`shellRoot`), capturing the layout's base paddings once.
- Added `ShellInsets.appBarTopPadding(base, statusInset)` and
  `ShellInsets.bottomNavigationBottomPadding(base, navInset)` to add the status-bar
  top inset to the app bar and the navigation-bar bottom inset to the bottom nav,
  keeping both operatable and outside the system-bar areas.
- Extended `activity_shell.xml`: `shellRoot` id added; no layout geometry changed.

## String-guard correction

`LocalizedResourceResolutionTest` asserted EN != NE for every content string. Two
(or three) row formats are **byte-identical placeholder templates** in both locales:

- `party_row_format` = `%1$s — %2$s`
- `trade_row_format` = `%1$s — %2$s · %3$s`
- `trade_row_time_format` = `%1$s · %2$s`

These carry no language content, so `assertNotEquals` can never pass. Added
`assertContentStringsResolveInBothLocales(keys, placeholderOnlyKeys = ...)`; the
placeholder-only keys are still required to resolve and be non-blank in both
locales. No product strings were changed.

## Party-editor validation fix

`saveParty()` routed three errors (`PARTY_NAME_REQUIRED`, `PARTY_ROLE_INCOMPATIBLE`,
`PARTY_HAS_TRADES`) through `showValidationMessage()`, which writes into
`validationMessageText` inside the `transactionEditorContainer` — `GONE` in the
party editor. The inline message could never be seen (only the Toast fired).
Added a `partyValidationMessageText` TextView to the party editor layout and
`showPartyValidationMessage()` so inline errors render in the visible party editor;
the message hides when the editor closes. The Toast remains.

## Test corrections (stale pre-khata flows)

The full-suite reruns exposed tests written before the M5.04 khata flow: a
`partyRow` click now opens the party khata, and editing/deleting happens from the
`khataEditPartyButton`, and after a khata-launched save the list requires closing
the khata before it re-renders. Updated:

- `partyListShowsEmptyStateThenAddsAndEditsParty`: select the SUPPLIER role in the
  spinner (the test asserted SUPPLIER but never set it — latent break), `scrollTo()`
  the contact input, open the editor via `khataEditPartyButton`, and close the khata
  after saving before asserting the renamed row.
- `partyEditorDeleteConfirmsAndRemovesParty`: open the editor via
  `khataEditPartyButton` before deleting.

## Automated evidence

- `ShellInsetsTest` (5 JVM tests) and `FarmActivityShellInsetsTest` (3
  device-invariant instrumented tests): app-bar top inset excludes the status bar,
  bottom-nav inset excludes the navigation bar; pass on both API-36 and API-26
  devices.
- `:app:verifyLocal` passes: JVM tests, lint, debug assembly, Android-test
  compilation, JSON evidence + APK SHA-256 (versionName `0.2.0`, versionCode 3).
- Full connected suite, fresh APKs on both devices:
  - API-36 physical (Moto Edge 60 Fusion): **86/86 pass**.
  - API-26 emulator: two failures only, both reproducible pre-existing flakes in
    `backFromSettingsRestoresPriorPrimaryDestination` and
    `backFromPrimaryDestinationReturnsToHome` (Espresso coordinate back-tap lands
    below the nav bar on the small-screen emulator). Both are documented as flakes;
    they also failed the pre-change baseline.

## Boundaries

- No layout geometry, product behavior, domain facts, persistence, strings,
  secrets, tags, version numbers, or releases changed.
- Emulator back-navigation coordinate flakes are documented, not fixed, in M6.4.1.

## Completion gates

- `./gradlew :app:verifyLocal`
- `./gradlew :app:connectedDebugAndroidTest` with the results above
- JSON evidence parses and its SHA-256 matches the debug APK
- `git diff --check`
- GitHub PR CI passes