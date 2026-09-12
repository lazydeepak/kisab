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

## Danger label contrast rework (2026-09-12)

Implemented to close the pressed and disabled contrast defects above:

- Added `res/color/button_text_danger.xml` selector:
  disabled `#757575`, pressed `#212121`, default white.
- Added `textDangerPressed`/`textDangerDisabled` to
  `values/colors.xml` (theme-independent, no night variant needed).
- Both danger buttons now use `android:textColor="@color/button_text_danger"`
  instead of `@android:color/white`.

Updated contrast (computed):

| Pairing | Contrast | WCAG AA normal text (4.5) | AA large text (3.0) |
| --- | --- | --- | --- |
| rest `#C62828` vs white label | 5.62:1 | PASS | PASS |
| pressed `#FF5252` vs `#212121` label | 5.05:1 | PASS | PASS |
| disabled (light fill `#F1C9C9`) vs `#757575` label | 3.06:1 | EXEMPT* | EXEMPT* |
| disabled (dark fill `#3F1818`) vs `#757575` label | 3.36:1 | EXEMPT* | EXEMPT* |

*Disabled is an inactive UI component and is exempt from the WCAG 1.4.3
contrast requirement; the dimmed label is a deliberate visual affordance.

Gate and device verification for the rework:

- `./gradlew :app:verifyLocal` — **PASS** on the reworked tree.
- Device pixel sampling (same Moto Edge 60 Fusion) on the reinstalled
  debug APK, light and dark mode: rest shows white glyphs on `#C62828`;
  held-press shows `#212121` glyphs on `#FF5252`
  (`42_light_newlabel_rest.png`, `43_light_newlabel_pressed.png`,
  `45_dark_newlabel_pressed.png`). Disabled state is not reachable at
  runtime in normal flow; enforced by the selector.

## Full-surface styling sweep (2026-09-12)

Method: uiautomator bounds analysis (touch targets < 48dp, offscreen
nodes) plus pixel sampling, screen by screen on the same device, in light
and dark mode and at font scale 1.0/1.3. Screens covered: Today/Home,
Khata, Farm Work, More, Hisab, Farms, Settings, About dialog, Farm Details
(top + danger-zone scroll position), Add Farm, Record action sheet, Add
Party dialog, Production dialog, and Reset/Delete confirmation dialogs.
Danger selector re-verified on the merged build: dark-mode rest `#C62828`
and held pressed `#FF5252` sampled at both button centers
(`40_dark_danger_rest.png`, `41_dark_reset_held.png`).

| Check | Result |
| --- | --- |
| Touch targets: every clickable node ≥48dp height across all covered surfaces (light and dark) | PASS |
| Offscreen content: no node extends past display bounds on any covered surface (light/dark, font 1.0 and 1.3) | PASS |
| Text scaling 1.3: no truncation or horizontal overflow; only expected multi-line wrapping on headers; touch targets unaffected | PASS |
| System-bar insets: status-bar top and nav-bar bottom padding applied via `setOnApplyWindowInsetsListener`; content sits inside decor in both modes | PASS |
| Keyboard overlap, Production dialog: SAVE/CANCEL/ADD PRODUCT visible with IME open (IME frame top 1671px @ 1220x2712/450dpi) | PASS |
| Keyboard overlap, Add Party dialog: ADD PARTY (y 909–1044) fully above IME top (1671) | PASS |
| Keyboard overlap, Add Farm screen: CREATE FARM (y 1618–1764) dips below IME top (1671) — reachable via the ScrollView form but not instantly visible; minor finding | MINOR |
| Hardcoded small paddings (4/6/8/9dp) bypass the `spacing_*` scale on a few icon/utility views in `activity_shell.xml` | MINOR (stylistic drift) |
| Hardcoded `@android:color/white` label on both danger buttons in all states (drives the two contrast notes above) | KNOWN |

Sweep screenshots `23_`–`41_` in the session screenshot directory are the
durable evidence set for the checks above. Address the two MINOR findings
and the contrast defects (pressed label 3.19:1, disabled-over-light
1.51:1) in the remaining global-styling-audit workstream.
