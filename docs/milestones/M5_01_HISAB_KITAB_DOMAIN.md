# Kisab M5-01 — Hisab-Kitab Domain Foundation: Parties — Design Record

Defines the first Hisab-Kitab slice: the **Party** domain model and its persistence evolution. A party is the person or business the farmer deals with (a buyer of produce, a supplier of inputs). M5-01 establishes the domain foundation and the party list/detail boundary that M5-02+ will build on with hisab/khata records and Udhar (credit) tracking. No Udhar/Sale/Purchase records are added in this slice unless the model design requires them.

> **Status: COMPLETE** on `feature/m5-01-hisab-kitab` (from merged M5-00 `main`). This document is the deliberate design conversation required by the charter before implementation.

**Implemented (2026-08-10):** `PartyRole`, `Party`, `PartyDraft` in `Farms.kt` with validator rules; `FarmState.parties` → schema v4 codec with v3-and-below migration and backup round-trip; `FarmSliceService` party CRUD (`addParty`/`updateParty`/`deleteParty`/`party`; `PartyOrdering`: CUSTOMER before SUPPLIER before OTHER); party list + detail/form in the Hisab-Kitab destination with back-button boundary, dirty-discard protection, and en+ne strings.

## Working principles

- **Minimal dependency budget.** Continues the AppCompat view discipline; no new libraries.
- **Parties are named business counterparts, not a menu.** A party has a name, a role that captures which side of the farm's business they sit on (buyer vs supplier), and optional contact/notes. The model deliberately avoids over-structuring: contact is free text (a phone number in practice), notes are free text.
- **Role semantics are the seed of future Udhar.** `CUSTOMER` parties will later hold money *receivable*; `SUPPLIER` parties will hold money *payable*. M5-01 stores the role only — no balances, no ledger — but the enum is the stable anchor M5-02+ keys on.
- **Persistence evolution, not a rewrite.** Farm schema advances from v3 to v4 by appending a `parties` list. Existing v1–v3 payloads and v1 backup envelopes keep decoding (empty parties) — the same compatibility discipline as M4-05 F001.
- **Single source of truth stays.** `FarmSliceService` and `FarmStore` remain the domain/persistence seams; the UI never touches the codec directly.

## Party model

```kotlin
enum class PartyRole { CUSTOMER, SUPPLIER, OTHER }

data class Party(
    val id: String,
    val name: String,
    val role: PartyRole,
    val contact: String = "",   // free text, e.g. phone number
    val notes: String = ""      // free text
)

data class PartyDraft(
    val name: String,
    val role: PartyRole,
    val contact: String = "",
    val notes: String = ""
)
```

### Field semantics

| Field | Required | Rule |
| --- | --- | --- |
| `id` | yes (service-assigned) | unique within a farm; `party-<uuid>` |
| `name` | yes | non-blank after trim; the display identity |
| `role` | yes | one of `CUSTOMER`, `SUPPLIER`, `OTHER` |
| `contact` | no | free text; blank stored as `""` |
| `notes` | no | free text; blank stored as `""` |

### Role semantics

- **CUSTOMER** (ग्राहक) — buys farm produce from the farmer. Future: money the farmer is owed.
- **SUPPLIER** (आपूर्तिकर्ता) — sells inputs (feed, supplies, labor?) to the farmer. Future: money the farmer owes.
- **OTHER** — a party whose future hisab may be mixed or undetermined; never blocks a future balance model.

Roles are not categories of transaction in this slice; they describe the counterpart. M5-02+ decides how `CUSTOMER` vs `SUPPLIER` roles constrain hisab records.

## Service operations (FarmSliceService)

- `addParty(farmId, draft): Party` — validates, assigns id, appends, persists.
- `updateParty(farmId, partyId, draft): Party` — replaces fields in place; id unchanged.
- `deleteParty(farmId, partyId)` — removes; safe while no hisab references exist (M5-01 has none).
- `parties(farmId): List<Party>` — stable display order (by name, then insertion order for ties).
- `party(farmId, partyId): Party?` — direct lookup (list/detail boundary).

Validation (`FarmStateValidator`): name non-blank; party ids unique per farm. Contact/notes accept any string. Deleting or renaming a party is unconstrained because nothing references parties yet.

## Persistence evolution (schema v4)

`FarmState` gains `parties: MutableList<Party> = mutableListOf()` and `CURRENT_FARM_SCHEMA_VERSION` becomes **4**.

`FarmPersistenceCodec` encoding (schema 4) appends a parties field:

```
4 \u001F <id> \u001F <name> \u001F <entries> \u001F <currency> \u001F <transactions> \u001F <parties>
```

Each party encodes as `id \u001D role \u001D name \u001D contact \u001D notes` (transaction field separator reused).

Decode rules:
- **Schema 4** — reads all 7 fields; parties decoded from field 6.
- **Schema 3 and below** — decode exactly as today, then set `parties = []`. Upgrade is implicit: the next `saveFarm` rewrites at v4.
- **Backup envelope unchanged.** `FarmBackupCodec` (envelope schema 1) wraps `FarmPersistenceCodec`, so parties serialize through existing backups automatically; old backups with no parties restore an empty list.

`FarmStateValidator.validateFarm` additionally validates each party and requires unique party ids.

## Party list/detail boundary

The Hisab-Kitab destination (currently a placeholder screen) becomes the **party list**:

- **List view** — party name + role label per row, ordered by name; empty state ("No parties yet"); an **Add party** action.
- **Detail/edit view** — a form over `PartyDraft` (name, role chooser, contact, notes) with **Save** / **Cancel**; **Delete** is available when editing an existing party.
- **Navigation** — the list and the form live inside the Hisab-Kitab destination (not new shell destinations). Opening a party / adding a party swaps the Hisab-Kitab content between list and form. Back from the form returns to the list; the shell's existing back handling for the Hisab-Kitab destination continues to return to Home when already on the list.
- **Discard semantics** — a dirty form prompts the existing Discard/Keep-editing dialog before leaving (consistent with the transaction editor), but since the form is inside the Hisab-Kitab destination, this is governed by a form-level dirty check, not the shell editor.

Out of scope for M5-01: hisab/khata records, Udhar balances, Sale/Purchase linking, party balances, search/filter.

## Localization

New user-facing terms follow the M4 glossary rules (every Nepali key requires an English default) and are added to `docs/localization/NEPALI_TERMINOLOGY.md` before shipping: Party (पार्टी), Customer (ग्राहक), Supplier (आपूर्तिकर्ता), Contact (सम्पर्क), Notes (टिप्पणी — already governed), Add party, Save/Cancel/Delete (already governed).

## Implementation slices

1. **Domain** — `PartyRole`, `Party`, `PartyDraft`, validator rules.
2. **Persistence** — `FarmState.parties`, schema v4 codec with v3-and-below migration, backup round-trip.
3. **Service** — party CRUD + listing on `FarmSliceService`.
4. **UI** — party list + detail/edit form in the Hisab-Kitab destination, en+ne strings, list/detail boundary + back behavior.
5. **Validation** — JVM unit tests (service, codec migration, validator), instrumentation for the list/detail flow, lint Debug + Release.

## Validation goals (updated as evidence lands)

- `FarmSliceServiceTest` party CRUD + validation; `FarmPersistenceCodec` v3→v4 migration + backup round-trip; `FarmActivityWorkflowTest` party list/detail cases (with `LocalizedResourceResolutionTest` party-string coverage); full JVM `testDebugUnitTest` on JDK 21; lint Debug + Release.

**Evidence:** `FarmSliceServiceTest` (34 tests incl. party CRUD, migration, backup) PASS; androidTest compiles (`assembleDebugAndroidTest` SUCCESS); lint SUCCESS. Full JVM suite retains the single known pre-existing baseline failure `TimePresentationTest#displaysStoredInstantInDeviceZoneWithoutUtcLiteral` (JDK-17 locale/Timezone, unrelated). Timed Android tests on the API-26 emulator remain supplemental per M4-04 disposition (not a completion gate).
