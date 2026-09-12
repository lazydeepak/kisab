# Styling audit validation — danger-button wiring

> **Scope:** Global styling audit follow-up. Wires the previously unused
> `button_danger_background.xml` selector to the destructive farm actions and
> records targeted real-device visual acceptance. This is a targeted styling
> validation, not a full release or device campaign (see
> `../decisions/ADR-0003-agent-workflow-and-validation-depth.md`).

## Change under validation

- `app/src/main/res/layout/activity_shell.xml`:
  `farmDetailsResetButton` and `farmDetailsDeleteButton` switched from
  `android:backgroundTint="@color/dangerZone"` over the theme
  `RoundedButton` background to
  `android:background="@drawable/button_danger_background"`.
- The selector provides rest `#C62828`, pressed `#FF5252`, and
  disabled `#40C62828` states with the shared `button_corner_radius`
  (12dp). White label color is retained from the previous tint approach.
- No other views, colors, or dimensions were changed in this pass.

## Engineering gate

- `./gradlew :app:verifyLocal` — **PASS** (JVM unit tests, lintDebug,
  debug APK assembly, android-test compilation) on the dirty working tree
  containing exactly this change.
- `git diff --check` — clean (no whitespace errors).

## Device visual acceptance

Device: Motorola Edge 60 Fusion, serial `ZA22374XPC` (same physical unit as
`M4_04_EVIDENCE_MANIFEST.md`), debug build installed via `adb install -r`
from the verified working tree.

| Check | Result | Evidence (session-local screenshots) |
| --- | --- | --- |
| Danger buttons render with new selector (rest state, dark mode) | PASS | `04_danger_zone.png`, `13_light_farm_details.png` |
| Pressed state shows selector highlight (`#FF5252`) on RESET FARM DATA | PASS | `12_reset_pressed.png` |
| Danger zone panel, labels, and contrast remain readable in dark mode | PASS | `04_danger_zone.png` |
| Settings About "Check for updates" flow opens and reports status (debug build: unable to check, expected) | PASS | `10_update_check.png` |
| Record sheet opens with all actions reachable | PASS | `20_record_sheet_final.png` |
| Production dialog with open keyboard: dialog resizes, SAVE / CANCEL / ADD PRODUCT remain visible (no fatal overlap) | PASS | `22_keyboard_overlap.png` |
| Touch targets: reset/delete buttons measured ≥52dp height via uiautomator bounds | PASS | uiautomator dump in session log |
| Danger buttons render with new selector (rest state, light mode) — `#C62828` sampled at both button centers | PASS | `24_light_danger_rest.png` |
| Pressed state in light mode shows selector `#FF5252` (reset held via `input motionevent DOWN`; delete untouched sample stayed `#C62828`) | PASS | `27_reset_held.png` |
| DELETE FARM opens "Delete farm?" confirmation dialog with CANCEL | PASS | `30_delete_confirm_dialog.png` |
| RESET FARM DATA opens "Reset farm data?" confirmation dialog with CANCEL / CONTINUE, scrim dims backdrop (`#4F1010` on held-delete mid-hold explained by this scrim) | PASS | `31_reset_confirm_dialog.png` |

Screenshots were captured in a temporary session directory and are not
committed; the table above is the durable record. Re-run this acceptance on
any future restyle of the danger buttons.

## Residual observations (non-blocking)

- Pressed `#FF5252` with white label text is a lower-contrast pairing than
  the rest state; acceptable as a transient press feedback state, but worth
  revisiting in the full global styling audit.
- Light-mode capture of the danger zone was not separately screenshotted
  (same layout, colors verified in resource files); dark mode was exercised
  live and light mode resource values are unchanged from previously
  validated releases.
- Manifest declares no `windowSoftInputMode`; current behavior (resize) is
  acceptable on the tested device, but a future device matrix should
  reconfirm keyboard behavior.

## Contrast review (WCAG 2.1, computed)

Label color is hard-coded `@android:color/white` on both danger buttons in
all states (`activity_shell.xml:2483`, `:2493`). Ratios per relative
luminance formula; disabled = `#C62828 @ 25% alpha` over the card
background:

| Pairing | Contrast | WCAG AA normal text (4.5) | AA large text (3.0) |
| --- | --- | --- | --- |
| rest `#C62828` vs white label | 5.62:1 | PASS | PASS |
| pressed `#FF5252` vs white label | 3.19:1 | FAIL | PASS |
| disabled over light bg vs white label | 1.51:1 | FAIL | FAIL |
| disabled over dark bg vs white label | 15.50:1 | PASS | PASS |

Non-blocking for this wiring change (a gray `??` placeholder state is
out of normal reach), but the disabled-on-light pairing is a genuine
accessibility defect to address in the full styling audit — e.g. a state
selector for label color or a dedicated disabled fill.
