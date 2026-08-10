# Kisab M5-00 — Application Shell / Navigation / Settings — Design Record

Defines the Application Shell for Kisab: an intentional top app bar, a small set of destinations, bottom navigation, back-stack rules, and a Settings destination that takes over currency (from the temporary Farm Tools row) and adds language selection. M5-00 is the wrapper foundation for the Hisab-Kitab (M5-01+) and Hisab (future) feature work, and it removes the single-screen-growth problem before those features land.

> **Status: IN PROGRESS** on `feature/m5-00-app-shell` (from validated M4-05 `main`). M4 is implementation-complete; release validation deferred. This document is the "deliberate UI/UX design conversation" that the charter requires before implementation.

## Implemented (current state)

- `activity_shell.xml` replaces `activity_farm.xml` as the single shell: app bar (title + Settings action), content container, bottom nav.
- Destinations: **Home**, **Hisab-Kitab** (placeholder), **Hisab** (placeholder) as primary bottom-nav destinations; **Settings** reached via the app-bar action (not on bottom nav).
- Back-stack: Settings → previous primary destination; any primary destination → Home; Home → finish. Discard prompt fires when leaving Home with a dirty editor (via nav or back).
- Destination and last-primary survive configuration change (`onSaveInstanceState` restore + `showDestination(currentDestination)` after `render()`).
- Settings hosts farm currency (moved out of Farm Tools) and language selection; currency gated on no-transactions, "no farm" neutral state supported.
- Farm Tools no longer carries the currency row.
- Strings locale-ized (en + ne) for the new nav items, app bar, placeholders, and settings section.
- Verified: `compileDebugKotlin`, `compileDebugAndroidTestKotlin`, `lintDebug` green. Instrumentation on the API 26 emulator shows the click-through nav and Settings paths working; bottom-nav tap injection and `pressBack` delivery are flaky on this AVD (Espresso `InjectEventSecurityException`), flagged for a more reliable device before M5-00 validation is closed.

> **API-26 emulator instrumentation is not a completion gate for this feature-first slice** due to intermittent system input injection failures near the navigation-bar boundary (Espresso `InjectEventSecurityException` on bottom-nav taps and flaky `pressBack()` delivery). Navigation behavior is covered by successful isolated/recreation cases and should be re-run on a reliable physical device during later stabilization.

## Working principles

- **Minimal dependency budget.** Kisab is hand-built on AppCompat views with no Compose, no Material library, no Navigation component, no fragment framework. The shell follows the same discipline: hand-built top bar and bottom navigation, view-swapped destinations inside one activity. No new dependencies for the shell.
- **Home is a farm tool, not a menu.** The core fast actions on Home are **Record Income** and **Record Expense**. The shell must not turn Kisab into a generic enterprise-style menu; additional destinations are deliberately few: three primary destinations on bottom navigation (Home, Hisab-Kitab placeholder, Hisab placeholder) plus a Settings destination reached from the app bar.
- **Inset/spacing convention.** Every destination renders inside the same content area with a uniform padding/inset rule (16dp horizontal, 16dp top-of-content), so screens feel consistent without a framework.
- **Single source of truth stays.** Farm persistence (schema 3), backup envelope (schema 1), and the domain (`FarmSliceService`) are unchanged by the shell. Currency remains a **farm** setting; Settings exposes the same domain contract that Farm Tools exposed, now in a dedicated destination.

## Destinations

| Destination | Id (resource) | Purpose | Fast action |
| --- | --- | --- | --- |
| Home | `navHomeItem` | Farm overview (balance/income/expenses), Record Income / Record Expense, transaction editor, recent transactions, Farm Tools (entries, backup, summary) | Record Income / Record Expense |
| Hisab-Kitab | `navHisabKitabItem` | **Placeholder** — boundary for M5-01+ Part/Hisaab/Customer records. Not functional in M5-00. | none |
| Hisab | `navHisabItem` | **Placeholder** — farmer's hisab / account lookup boundary for M5-01+. Not functional in M5-00. | none |
| Settings | `shellSettingsButton` (app bar) | Farm currency (moved from Farm Tools; locked once transactions exist) and language selection | none |

Settings lives outside bottom navigation: it is reached via a top app-bar action so the bottom nav stays limited to the three primary farm-facing destinations. Back from Settings returns to the previously selected primary destination.

## Navigation / back-stack rules

1. Bottom navigation switches the visible primary destination directly. Selecting the already-active destination re-renders it (no-op refresh).
2. System **Back**:
   - If the transaction editor is open and dirty on **Home** → show the existing **Discard / Keep editing** dialog first (unchanged M4 behavior), then Back again dismisses the editor.
   - If the current destination is **Settings** → return to the previously selected primary destination (sticky last-primary tracking).
   - If the current primary destination is **Hisab-Kitab** or **Hisab** → return to **Home** (single-entry back stack: home is the only persistent destination).
   - If on **Home** with no editor → default activity finish (as today).
3. **Draft/discard on destination change:** switching away from Home while the transaction editor is open and dirty prompts the same **Discard / Keep editing** dialog. "Discard" closes the editor and switches destination; "Keep editing" stays on Home.
4. Editor state survives configuration change on Home exactly as it does today (bundle save/restore). Destination selection also survives configuration change.
5. The app bar title reflects the current destination; on Home it shows the farm name when a farm exists.

## Settings destination

- **Currency:** renders the farm currency row (label, current code, Change action gated on no transactions, locked note when transactions exist) — same domain/semantics as the temporary Farm Tools row, relocated. No opinionated free-text dialog survives the move; the existing ISO free-type chooser is reused as-is for consistency with M4-05.
- **Language:** a hand-built selector with three choices:
  - **Follow device** (default) — current behavior (`PresentationLocale.presentationLocale` from resource configuration).
  - **English**
  - **Nepali**
  - Selection persists in app preferences and is applied via `AppCompatDelegate.setApplicationLocales` (AppCompat 1.7 supports per-app locales on API 26+). The M4 presentation rules are unchanged: ne → `ne-NP`, en → `en-NP`, other → keep language.
- Settings never needs a farm to exist (it is available even before the first farm is created); currency controls act only on the current farm when present and otherwise show a neutral "no farm yet" state.

## Shell layout structure

```
activity_shell.xml (vertical LinearLayout, match_parent)
├── ShellAppBar (top: title TextView id shellTitle; Settings action id shellSettingsButton)
├── contentContainer (FrameLayout, weight=1)
│   ├── Home content  (the existing activity_farm content, ids preserved)
│   ├── Hisab-Kitab placeholder screen (id hisabKitabScreen, gone)
│   ├── Hisab placeholder screen (id hisabScreen, gone)
│   └── Settings screen (id settingsScreen, gone)
└── BottomNavigation (id bottomNavigation; horizontal LinearLayout of 3 nav items:
    id navHomeItem, navHisabKitabItem, navHisabItem)
```

The current `FarmActivity` becomes the shell host: it keeps all Home view bindings and editor/backup logic, adds destination switching, the app bar, and the Settings/Hisab-Kitab rendering. Manifest unchanged (`FarmActivity` remains the launcher).

## Implementation slices

1. **Shell skeleton** — new `activity_shell.xml` wrapping existing Home content; app bar (title + Settings action) + bottom nav; destination model + switching; back-stack rules; destination + last-primary tracking survives config change. Home unchanged.
2. **Settings slice** — Settings destination reached from the app bar: currency row moved out of Farm Tools; language selector + persistence (`AppLanguagePreferences`) + `AppCompatDelegate.setApplicationLocales`; strings en+ne.
3. **Validation** — unit (language preference store, locale mapping), instrumentation (shell nav, back stack, discard-on-leave, settings currency + language), lint Debug + Release.
4. **Hisab-Kitab placeholder screen** — simple placeholder with future-boundary note.
5. **Hisab placeholder screen** — simple placeholder for the farmer hisab/account lookup boundary.

Out of scope for M5-00: actual Hisab-Kitab domain/screens (M5-01+), actual Hisab destination, settings beyond currency+language, monetized UI framework.

## Validation goals (updated as evidence lands)

- `FarmActivityWorkflowTest` (shell/nav/discard/settings cases), `FarmActivityPresentationTest`, `LocalizedResourceResolutionTest` for new strings, `FarmBackupIntegrationTest` unchanged behavior, full JVM `testDebugUnitTest` on JDK 21, lint Debug + Release.