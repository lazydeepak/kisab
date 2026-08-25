# PILOT-01: Farmer Usability Test Protocol

## 1. Study Objective & Overview

This usability protocol defines the methodology for evaluating the redesigned Kisab mobile application with real farmers in Nepal. The primary goal is to observe intuitive workflow comprehension, task completion rates, navigation efficiency, and terminology clarity **without coaching or leading the participant**.

---

## 2. Participant Profile & Sample

- **Sample Size**: 3–5 active farmers or agricultural business operators.
- **Demographic Mix**:
  - Diversity in age (e.g., 20s–30s tech-fluent vs. 40s–60s experienced farmers).
  - Variety of agricultural operations (Dairy, Poultry, Vegetable/Crops, Mixed).
  - Varying digital confidence (daily smartphone users vs. casual users).
- **Environment**: Quiet, natural agricultural or home setting with test Android device running Kisab build `0.2.0` (code `3`).

---

## 3. Observer Rules for Facilitators

1. **No Leading or Coaching**: Do not tell the participant where buttons or tabs are located. Give the scenario goal and observe.
2. **Do Not Pre-Explain Terminology**: Let the participant encounter words like `लिन बाँकी`, `तिर्न बाँकी`, `नखुलेको` naturally during tasks before asking about their meaning.
3. **Encourage Think-Aloud**: Ask the farmer to verbalize what they are looking at and what they expect to happen.
4. **Intervention Threshold**: Intervene only if the participant has given up / abandoned the task after reasonable exploration, or to prevent accidental deletion of non-test data.
5. **Exact Notes**: Record the participant's exact words, mis-taps, hesitations, and facial reactions.

---

## 4. Test Scenarios & Task Inventory

| Task # | Scenario & Prompt to Farmer | Primary Action Path | Success Criteria |
|---|---|---|---|
| **Task A** | **Find the current farm**: "तपाईं अहिले कुन फार्ममा हुनुहुन्छ र मुद्रा के छ हेर्नुहोस्।" | App-bar title / switcher | Identifies active farm name and currency within 5 seconds. |
| **Task B** | **Record milk production**: "आज बिहान ३० लिटर दूध उत्पादन भयो, यो हिसाब राख्नुहोस्।" | `+` (Record) -> `उत्पादन` -> Morning -> 30 L -> Save | Enters quantity and session, saves, and sees Today production tile update. |
| **Task C** | **Sell product on partial credit**: "राम दाइलाई रु १०० का दरले २० लिटर दूध बेच्नुभयो। उहाँले रु १,००० नगद दिनुभयो, बाँकी उधारो छ।" | `+` (Record) -> `बेचेँ` -> Ram Dai -> 20 L -> Rs 100 -> Partial Rs 1,000 -> Save | Understands live equation `२० × १०० = २,००० (नगद: १,०००, बाँकी: १,०००)`. |
| **Task D** | **Check who owes money**: "अहिले कस-कसले पैसा तिर्न बाँकी छ हेर्नुहोस्।" | `खाता` (Khata) or Today attention card | Navigates to Khata `लिन बाँकी` and locates Ram Dai's NPR 1,000 balance. |
| **Task E** | **Record received debt payment**: "राम दाइले बाँकी रु १,००० ल्याएर दिनुभयो, हिसाब चुक्ता गर्नुहोस्।" | Khata -> Ram Dai -> `[पैसा पाएँ]` -> Full Amount -> Save | Records payment; observes Ram Dai's balance becoming 0 / `चुक्ता`. |
| **Task F** | **Buy supply on partial credit**: "विकास एग्रोबाट रु ५,००० को २ बोरा दाना किन्नुभयो, रु ३,००० तिर्नुभयो।" | `+` (Record) -> `किनेँ` -> Feed -> 2 bags -> Rs 5,000 -> Partial Rs 3,000 -> Save | Saves purchase and observes payable balance update. |
| **Task G** | **Record supply usage**: "फार्ममा १ बोरा दाना प्रयोग भयो, रेकर्ड राख्नुहोस्।" | `+` (Record) or Farm Work -> `प्रयोग गरेँ` -> 1 bag -> Save | Saves usage; observes remaining feed stock decrement. |
| **Task H** | **Check remaining feed stock**: "फार्ममा अब कति बोरा दाना बाँकी छ हेर्नुहोस्।" | `फार्मको काम` (Farm Work) -> Supplies | Locates remaining stock tile (`बाँकी: १ बोरा`). |
| **Task I** | **Check who the farmer owes**: "तपाईंले अरूलाई कति पैसा तिर्न बाँकी छ हेर्नुहोस्।" | `खाता` (Khata) -> `तिर्न बाँकी` | Identifies Bikash Agro NPR 2,000 payable. |
| **Task J** | **Record supplier payment**: "विकास एग्रोलाई बाँकी रु २,००० तिर्नुभयो, रेकर्ड राख्नुहोस्।" | Khata -> Bikash Agro -> `[पैसा तिरेँ]` -> Full Amount -> Save | Settle payable balance to 0. |
| **Task K** | **Switch farms**: "अर्को फार्ममा जानुहोस् र त्यहाँको हिसाब हेर्नुहोस्।" | App-bar dropdown switcher or `More` -> `Farms` | Successfully switches active farm without confusion. |
| **Task L** | **Find backup**: "तपाईंको फार्मको डाटा सुरक्षित गर्न ब्याकअप कहाँबाट गर्ने खोज्नुहोस्।" | `More` -> `ब्याकअप` / `Settings` | Navigates to backup export section in Settings. |
| **Task M** | **Terminology Comprehension**: "यी शब्दहरूले के जनाउँछन् भन्नुहोस्: १. लिन बाँकी, २. तिर्न बाँकी, ३. नखुलेको दूध" | Verbal response | Demonstrates clear understanding of directional debt and unexplained production. |

---

## 5. Usability Metrics & Observation Sheet

| Metric | Target Goal | Method of Measurement |
|---|---|---|
| **Unassisted Completion Rate** | >= 85% of tasks | YES/NO score per task without facilitator guidance. |
| **Time on Task** | < 30 seconds for quick records | Stopwatch from prompt finish to save confirmation. |
| **Directional Confusion** | 0 instances | Confusing `लिन बाँकी` (Receivable) with `तिर्न बाँकी` (Payable). |
| **Devanagari Readability** | 100% legibility | Verifying zero complaints regarding text clipping or font size. |
| **Subjective Farmer Confidence** | >= 4 / 5 rating | Post-test confidence question: "तपाईं आफैं यो चलाउन कत्तिको ढुक्क हुनुहुन्छ?" |

---

## 6. Post-Session Interview Questions

1. **Overall Impression**: "तपाईंलाई यो एप चलाउँदा कस्तो लाग्यो? कुन कुरा सबैभन्दा सजिलो लाग्यो?"
2. **Speed & Ergonomics**: "दैनिक काम गर्दा (दूध बेच्दा वा दाना किन्दा) हिसाब राख्न छिटो भयो कि झन्झटिलो?"
3. **Terminology Feedback**: "कुनै शब्द बुझ्न अप्ठ्यारो भयो कि? तपाईंको आफ्नै गाउँ-ठाउँमा यसलाई के भन्नुहुन्छ?"
4. **Missing or Confusing Items**: "कुनै ठाउँमा अल्मलिनुभयो वा नबुझिने केही थियो?"

---

## 7. Pass Criteria for Pilot Execution

- **PASS**: All critical operational workflows (Tasks B, C, D, E, F, G, H, I, J) completed unassisted by >= 80% of participants. Zero data-loss incidents or directional debt misunderstandings.
- **FOLLOW-UP REQUIRED**: Specific minor terminology nuances identified for non-blocking copy refinement.
