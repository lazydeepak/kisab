# PILOT-01 Protocol — Farmer Usability Validation for Kisab v0.2.2

> **Supersedes** the v0.2.0-draft protocol (`PILOT_01_FARMER_USABILITY_PROTOCOL.md`, retained as history).
> **Baseline build:** released **Kisab v0.2.2 / versionCode 5**, production-signed
> (`sha256 6463fe8660b6c9eb59764ad864a2581285bcbf47c6517184c2587e64398a7a94`), installed via the live OTA channel or sideload of the published release asset. The baseline is immutable for the duration of the pilot: no feature fixes between sessions (see §12 of the M14 directive).

## 1. Objective

First structured behavioral validation of Kisab with real farmers. Engineering quality is established (M12/M13); this pilot establishes:

1. whether real farmers can complete core workflows unassisted;
2. whether the Nepali/English terminology is understood (converting `Provisional` glossary entries to evidence-backed dispositions);
3. whether generic cash income/expense entry is genuinely required (the current release has no free-form cash verb; see §7);
4. whether purchase → stock → use and production → sale/explain form comprehensible mental models;
5. whether Activities/breakdown carry understandable value;
6. whether backup is discoverable and trusted;
7. closure of outstanding M9 physical-validation debt (Nepali walkthrough, dark mode, landscape, saved text-size relaunch, मन/पाथी/मुरी workflow).

## 2. Participants

- **3–5** active farmers / agri-business operators, anonymous IDs `P01…P05`.
- Useful variation where practical: dairy/livestock, poultry, vegetable/crop, mixed; age bands 20s–30s vs 40s–60s; daily-smartphone users vs casual users.
- Record only context needed to interpret behavior: farming type, smartphone confidence (low/med/high), language preference, device model/screen size. **No names, phone numbers, addresses, photos, or recordings in any repository artifact.**
- Consent: verbal script below; participant may stop at any time; no compensation data recorded here.

### Consent script (Nepali first, English fallback)

"यो एप नेपाली किसानहरूका लागि बनेको हो। म तपाईंलाई केही सामान्य काम दिन्छु, तपाईं आफैं गर्ने प्रयास गर्नुहोस् — गल्ती हुनु स्वाभाविक हो, त्यही ठीक छ। म केही नोट लिन्छु, तर तपाईंको नाम/फोटो कहिँ पनि राख्दिनँ। बीचमा कुनै पनि बेला रोक्न सक्नुहुन्छ। ठीक छ?"

## 3. Method

Task-based, minimally coached observation. For every task: state the goal in farmer terms → observe first action → note hesitations/mis-taps/backtracking verbatim → allow independent completion → only then ask what labels meant. Never pre-explain terminology. Facilitator notes go on paper/tablet per session using `PILOT_01_SESSION_TEMPLATE.md`.

**Facilitator technical notes (from the v0.2.2 rehearsal, see `PILOT_01_REHEARSAL_v0.2.2.md`):**

- On-screen keyboard can cover the SAVE button inside bottom-sheet dialogs; pressing Back once hides the keyboard without cancelling the sheet. Do not teach this preemptively — observe how participants recover.
- The quick-sale/purchase sheets re-render when payment mode changes; button positions shift. Note any double-tap hesitation.
- Device used for observation should be signed into a Google account only if Play Protect interplay is being observed deliberately; otherwise decline sign-in prompts.

## 4. Session configuration matrix

Distribute fixed configurations across sessions so no session carries all burden:

| Check | P01 | P02 | P03 | P04 (opt) |
|---|---|---|---|---|
| Language | English UI | **Nepali UI throughout** | Farmer's choice | Farmer's choice |
| Dark mode walkthrough | – | ✓ | – | ✓ |
| Landscape spot-check | ✓ | – | ✓ | – |
| Large-text session (slider high, relaunch) | – | – | ✓ | ✓ |
| मन/पाथी/मुरी production+sale scenario | ✓ | ✓ | – | – |
| Backup export to real storage | ✓ | ✓ | ✓ | ✓ |

All sessions: default text size start; large-text block performed at session end so earlier tasks stay comparable.

## 4a. Standardized starting state (mandatory between participants)

**Clean-start protocol.** Each participant begins with **no farms, products, parties, or records** and performs setup themselves (T1/T2 create everything later tasks need). No rehearsal or prior-participant state may be carried in.

**Session-start state (verified aloud before consent):**
- App: production-signed v0.2.2 / code 5, first-run screen visible ("Farm name" + CREATE FARM), zero existing farms.
- Settings at defaults: English or Nepali UI per session matrix, text size default (**24 sp** — `AppTextSize.DEFAULT_SP`; the app intentionally renders 24/16 = 1.5× authored sizes on fresh installs), appearance Follow-system, notifications as found.

**Reset procedure between participants (in order of preference):**
1. **UI-based (preferred):** More → Farms → open each farm → DELETE FARM (confirm). When the last farm is deleted the app returns to the first-run screen. Then Settings: restore language/text-size/appearance defaults manually. Verify: first-run screen with empty farm list.
2. **Reinstall (fallback if UI state is in doubt):** uninstall `com.susankhya.kisab`, reinstall the same published v0.2.2 APK (`app-release.apk` from GitHub release v0.2.2), re-grant install-unknown-apps. Never use adb shell tricks against app data.

**Task dependency map (all satisfied within a single session):**
- T5 cash sale → needs product (T4) and customer (created during T5).
- T6 credit sale → any customer from T5.
- T7 settlement → receivable created by T6.
- T8 cash purchase → supply created during T8; T9 credit purchase → supplier created during T9.
- T10 use → stock from T8/T9.
- T13 breakdown → meaningful after ≥1 activity-tagged trade (T6/T9) plus untagged entries.

**Between-session recording:** note reset method used and post-reset verification result in the session record.

## 5. Task inventory (v0.2.2)

Original tasks A–M are preserved where still valid, renumbered; new v0.2.2-specific tasks added. Prompts are farmer-facing; success criteria are facilitator-facing.

### Setup

- **T1 Create farm**: "आफ्नो फार्म यो एपमा बनाउनुहोस्।" — Expect: reaches name input, currency understanding optional. Observe: finds first-run screen affordances unaided.
- **T2 Choose activities**: "यो फार्मले के-कस्तो काम गर्छ भन्ने जनाउनुहोस् (जस्तै कुखुरा पालन)।" — Observe: discovery route; whether "activities" concept lands. *(Facilitator-only success reference, never read aloud: More → Farms → farm → CHANGE ACTIVITIES.)*
- **T3 Switch/understand farms**: existing task K. Observe: app-bar switcher vs More→Farms routes.

### Production

- **T4 Record today's production** (was B): "आज बिहान [X] उत्पादन भयो, रेकर्ड राख्नुहोस्।" Use participant's real product. Observe: Record discovery, product creation dialog, **default unit sense** (note: defaults to litre), session choice.
- **T4b Second-product & control-swap watch**: later, ask them to record a DIFFERENT product's output (e.g., "अब गाईको दूध पनि रेकर्ड गर्नुहोस्"). Silently observe: whether they find ADD PRODUCT; whether the left-button now reading DELETE causes hesitation, mis-tap, or near-miss; whether the confirmation dialog registers with them. Do NOT point out the swap before or during; debrief only afterward ("यो बटनले के गर्छ जस्तो लाग्यो?").
- **T4N Nepali units variant**: same but quantity phrased in मन/पाथी (e.g., "२ पाथी अन्न उत्पादन भयो") or मुरी for stores. Observe: unit spinner discovery; whether मन/पाथी/मुरी labels are found and chosen over litre/kg.

### Sales

- **T5 Cash sale** (was C simplified): "उत्पादन बेच्नुभयो, पूरा पैसा अहिले आयो।" — Observe: SELL path, customer requirement handling (**v0.2.2 requires creating a customer even for cash sales — note reactions**), live equation comprehension, Paid-in-full selection.
- **T6 Credit sale partial** (was C): "ग्राहकलाई बेच्नुभयो; अहिले आधा पैसा मात्र आयो, बाँकी पछि।" — Observe: Credit vs Some-money-received choice, received-now entry, remaining display.
- **T7 Final settlement** (was E): "बाँकी रकम आज पूरा आयो, हिसाब चुक्ता गर्नुहोस्।" — Observe: Khata → party → RECEIVED MONEY route; resulting Settled state comprehension.

### Purchases & supplies

- **T8 Cash purchase** (was F adapted): "फार्मले चाहिने [दाना/मल] किन्नुभयो, नगद तिर्नुभयो।" — Observe: supply creation, unit/BAG selection, stock increment awareness. **Probe (do not characterize as defect):** supplier is requested even for full-cash buys — record whether that makes sense to the participant; afterward ask where they EXPECT this purchase to appear (Expenses? Khata? both?) and check the Today tiles with them silently noted.
- **T9 Credit purchase partial**: "उधारोमा किन्नुभयो; अहिले अलिकति तिर्नुभयो।" — Observe: supplier creation, Activity selector visibility/reaction (record verbatim interpretation), payable creation.
- **T10 Supply use** (was G): "अघि किनेको [इनपुट] आज प्रयोग भयो।" — Observe: USED path from Record or Farm Work; remaining-stock mental model (was H: "अब कति बाँकी छ?").

### Money needing attention

- **T11 Find who owes** (was D/I): Observe Today attention card vs Khata filter routes; directional confusion (लिन बाँकी vs तिर्न बाँकी) counts recorded.

### Activities

- **T12 Activity attribution probe** (during T6/T9): after save, ask "Activity ले के बुझ्यो तपाईंलाई? किन छान्नुभयो?" — Record whether noticed unprompted, interpretation, match with expectation.
- **T13 Breakdown discovery**: "कुन कामबाट कति आम्दानी/खर्च भयो हेर्न चाहनुहुन्छ भने कहाँ जानुहुन्छ?" — Do NOT say "breakdown". Record route attempted; then show Farm Details breakdown and ask interpretation of General vs named rows.

### Generic-cash gap probe — §7 of directive (critical)

- **T14 Misc cash expense**: "गाडी भाडा/बिजुली/कामदार ज्याला तिर्नुभयो — यो हिसाब राख्नुहोस्।"
- **T15 Misc cash income**: "एउटा आम्दानी आयो जुन उत्पादन बिक्रीसँग जोडिँदैन (जस्तै गोठको मल बिक्री) — रेकर्ड गर्नुहोस्।"

For each: do NOT hint anything is missing. Record: first attempted path; whether they succeed; whether they force it into SELL/BOUGHT/settlement; abandon; expected wording/action. Expected dead-ends (facilitator reference, not to share): PAID MONEY requires a supplier; RECEIVED MONEY requires customers; no generic expense/income verb exists anywhere. This evidence directly gates M15.

### Data safety

- **T16 Protect records before phone change** (was L, deepened): "फोन फेर्नुअघि आफ्नो हिसाब सुरक्षित गर्नुहोस्।" No mention of backup word. Observe: route found (More→BACKUP/RESTORE vs Settings), SAF picker reaction, destination/file naming behavior, whether they believe data is protected afterward ("अब के सुरक्षित भयो जस्तो लाग्छ?" — record reasoning). Complete one real export to Downloads; never simulate loss with irreplaceable data.

### Distribution awareness (passive)

- **T17 Update/expiry awareness** (observation only): if an expiry banner/update prompt appears naturally during the session, capture reaction; otherwise skip. Never force.

## 6. Post-session interview (all sessions)

1. "सबैभन्दा सजिलो के लाग्यो? सबैभन्दा गाह्रो?"
2. "दैनिक काममा यो एप चलाउनुहुन्छ कि झन्झट लाग्छ?"
3. Terminology probes (below) — only now may facilitator explain meanings.
4. "कुनै काम यो एपले सम्हाल्न सक्दैन जस्तो लाग्यो?"

## 7. Terminology evaluation (evidence states)

For each term record: shown wording · participant interpretation · observed consequence · evidence quote · disposition. States: **SUPPORTED / PROBLEMATIC / INCONCLUSIVE / REVISED**. A single accepting participant does not make SUPPORTED — require either ≥2 concordant interpretations or one consequential misread. Priority list (financial/action-critical first):

लिन बाँकी · तिर्न बाँकी · चुक्ता · नखुलेको (unexplained production) · बाँकी रकम · कारोबार/लेनदेन · गतिविधि (Activity) · उत्पादन · प्रयोग गरेँ · बाँकी स्टक/remaining · ब्याकअप/पुनर्स्थापना · श्रेणी values (दाना, सामग्री, श्रम…) · मन/पाथी/मुरी labels.

Results append to `docs/localization/NEPALI_TERMINOLOGY.md` with per-term evidence; statuses move off `Provisional` only per the rules above.

## 8. Severity classification

- **P0** data loss / accounting corruption / security failure / unrecoverable state.
- **P1** core farmer task cannot reasonably be completed or seriously misunderstood (candidate: generic-cash gap if probe confirms).
- **P2** works but substantial confusion, wasted effort, repeated mistakes (e.g., keyboard-covering-SAVE recovery, forced customer creation for pure-cash sales).
- **P3** polish/cosmetic.

Each finding tagged: `product-defect` | `comprehension` | `android-unfamiliarity` | `needs-more-evidence`. Hesitation alone is not a defect.

## 9. Completion threshold (mirrors M14 gate)

≥3 consented sessions · core scenarios attempted · T14/T15 executed for every participant · terminology table filled with evidence states · findings triaged P0–P3 · M9 debt items closed-or-rescheduled · aggregated results in `PILOT_01_RESULTS_v0.2.2.md` with ranked M15 candidates.

Three sessions = initial evidence, not universal validation; glossary mass-conversion to Confirmed is explicitly forbidden by this protocol.
