# Kisab M6.1 — App Shell and Navigation Polish — Design Record

M6.1 closes the presentation gap left by the functional M5-00 shell. It gives every existing destination a consistent branded wrapper without changing navigation architecture, financial facts, or persistence.

> **Status: COMPLETE** on `feature/m6-1-app-shell-polish`, based on merged M6 `main`.

## Product intent

The shell should make three primary destinations obvious, show where the farmer currently is, and keep secondary Settings access available without competing with primary navigation.

## Implemented

- Original Kisab logo: an open farm ledger and sprout, stored as source SVG under `docs/brand/` and as an Android vector drawable.
- Branded top app bar with logo, single-line destination title, and accessible overflow action.
- Overflow menu with Settings as a secondary destination.
- Bottom navigation icons and labels for Home, Hisab-Kitab, and Hisab.
- Selected item background, icon tint, and label tint synchronized with the current destination.
- No bottom item selected while Settings is open; Back returns to the prior primary destination.
- Existing destination and prior-primary restoration retained across Activity recreation.
- 48dp-or-larger actions, parent content descriptions, decorative child icons removed from the accessibility tree, and localized English/Nepali menu/logo descriptions.
- Compact equal-width navigation items with single-line labels for predictable small-screen layout.

## Deliberate boundaries

- No navigation drawer: three primary destinations fit in bottom navigation and a drawer would duplicate them.
- No fragment/navigation-framework rewrite; the existing single-Activity destination model remains authoritative.
- No Material Components dependency solely for wrapper styling.
- No domain, accounting, schema, migration, backup, or data changes.
- The same vector mark is used as the current launcher icon; adaptive icon packaging can be revisited with broader release-branding assets.

## Navigation rules

1. Home, Hisab-Kitab, and Hisab are primary destinations and select their matching bottom item.
2. Settings is opened from the top overflow menu and clears bottom selection while visible.
3. Back from Settings returns to the remembered primary destination.
4. Back from Hisab-Kitab or Hisab returns Home after existing editor/Khata discard rules are satisfied.
5. Recreation restores both current and last-primary destinations, then derives selected styling from that state.

## Validation

- Existing instrumentation navigation tests now open Settings through the overflow menu.
- Navigation tests assert selected-state transitions and recreation restoration.
- Localization coverage includes the new menu and logo accessibility text.
- Full JVM tests, debug build, lint, Android-test compilation, and `git diff --check` are completion gates.

## Manual/device validation

Manual emulator/device validation is deferred per the current instruction. Before production release, visually check small-screen English and Nepali labels, TalkBack order, popup placement, and icon contrast.
