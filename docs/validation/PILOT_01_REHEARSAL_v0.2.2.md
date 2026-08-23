# PILOT-01 Protocol Rehearsal — Kisab v0.2.2 (facilitator dry-run)

**Date**: 2026-08-23 · **Device**: ZA22374XPC (Motorola Edge 60 Fusion, Android 16, API 36)
**Build**: production-signed v0.2.2 / code 5 (`sha256 6463fe86…7a94`) — the immutable pilot baseline
**Purpose**: verify every protocol task path is executable against the released build before real sessions, and capture facilitator-facing friction. This rehearsal produces no participant data and changes no product code.

## Task-path validation matrix

| Protocol task | Executable on v0.2.2? | Notes |
|---|---|---|
| T1 create farm | YES | first-run + ADD FARM flows both verified |
| T2 choose activities | YES | More → Farms → farm → CHANGE ACTIVITIES |
| T3 switch farms | YES | app-bar switcher + Farms screen |
| T4 record production | YES | session-upsert: re-entry edits today's record per product/session |
| T4N मन/पाथी/मुरी | YES | created product "Maize" with **mana** unit; produced 2 mana; unit label rendered everywhere incl. Today ("Maize: 2 mana") |
| T5 cash sale | YES* | quick-sale live equation verified ($100 sale); *requires customer creation even for cash — probe reaction |
| T6 credit sale partial | YES | verified earlier in M13 gate ($120 / $50 / remaining $70) |
| T7 final settlement | YES | FULL AMOUNT → "All settled ($0.00)", receivable tile $0 |
| T8/T9 purchases | YES | cash-with-supplier and partial-credit both complete; Activity selector present in sheet |
| T10 supply use | YES | Feed 7 kg bought → used 1 kg → "6 kg remaining"; Bought/Used counters correct across cash+credit purchases |
| T11 who owes | YES | Today attention card + Khata filters |
| T13 breakdown discovery | YES | Farm Details → "Records by activity" reachable; General row renders |
| T16 backup export | YES | SAF picker → Downloads → REPLACE flow → file written (3,243 B); success returns to Settings |

## Configuration checks executed (M9 debt)

- **Dark mode**: Appearance mode radios function; toggled Dark → restored Follow-system.
- **Landscape**: forced rotation renders shell + navigation correctly (all four destinations visible).
- **Large text persistence**: slider to maximum → force-stop → relaunch → Settings reports **24 sp persisted** (M9 saved-size item closed at mechanism level; farmer legibility still needs human eyes).
- **Nepali render**: in-app language switch to नेपाली renders Settings ("सेटिङहरू"); revert clean.
- **मन/पाथी/मुरी**: mana usable end-to-end (production entry + sale + Today headline).

## Findings for the facilitator brief (not participant data)

- **F1 — IME covers SAVE** in bottom-sheet dialogs (quick sale/purchase). Back hides the keyboard without cancelling; participants may tap dark space or lose the sheet. Observe recovery, don't teach.
- **F2 — same-position neutral button flips ADD PRODUCT ↔ DELETE** on the production dialog once a today-record exists (`FarmActivity.kt:4796` replaces BUTTON_NEUTRAL). Near-destructive adjacency with a confirm dialog as the only guard. Tag candidate P2; watch whether participants hesitate or mis-tap here.
- **F3 — duplicate product name**: OK appears inert (transient toast, dialog stays). Participants may believe the app froze. Note reactions.
- **F4 — BOUGHT requires a supplier** even for pure-cash buys; there is no "no supplier / local shop" skip. Probe how farmers handle unknown vendors.
- **F5 — supplier-present cash purchase books as a Trade**, not a cash expense: after a $30 paid-in-full feed buy, Today's Expenses stayed $0.00 while Khata showed a settled Purchase trade. Cash-outflow visibility differs by vendor-known vs vendor-unknown path. High-value comprehension probe (T13/T14 debrief).
- **F6 — reconciliation display optimism**: when Sold exceeds Produced (12 L sold vs 10 L produced), Farm Work shows "All production accounted for" because the unexplained tile only renders when unexplained > 0 (`FarmActivity.kt:4499`). Over-sales are silently folded into "accounted". Candidate P3 copy/logic refinement; observe if any participant notices.
- **Suggested backup filename carries stale legacy text** ("kisab-rc01upgradefarm…") in SAF; cosmetic P3.

## State left behind

Rehearsal artifacts (PilotFarm with Eggs+Maize, Ram settled, Local/Vikash-less purchase history, SecondFarm NPR empty) remain on the device intentionally: they give the facilitator a realistic mixed-state device and examples to point at during debriefs. Facilitator should reset to factory state per participant using in-app Reset (More → Farms → farm details) rather than uninstalling, preserving the signed v0.2.2 install.
