# Kisab Nepali Terminology Glossary

Governed glossary for user-facing Kisab terminology. Terms proposed here are **provisional** until confirmed with real users through the M4 pilot; they must not be presented as final in any released build. Every entry maps to a resource key governed by the M4 localization rules, and every Nepali key requires an English default counterpart.

## Status legend

- **Status**: `Provisional` (proposed, awaiting real-user confirmation), `Candidate` (refined after review), `Confirmed` (confirmed by pilot evidence).
- **Final decision**: `Pending` until the M4 pilot provides evidence.

## Glossary

| Internal concept | English UI term | Proposed Nepali term | Alternative wording | Context | Status | Pilot feedback | Final decision |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Farm | Farm | फार्म / खेत (farm / khet) | खेती व्यवसाय | The single organisational unit Kisab manages | Provisional |  | Pending |
| Farmer | Farmer | किसान (kisan) | कृषक (krishak) | Person using Kisab | Provisional |  | Pending |
| Crop | Crop | बाली (bali) | बाली प्रकार | Crop-type entry on a farm | Provisional |  | Pending |
| Livestock | Livestock | पशुधन (pashudhan) | पशुपालन | Livestock-type entry on a farm | Provisional |  | Pending |
| Transaction | Transaction | लेनदेन (lenaden) | कारोबार (karobar) | A recorded income or expense event | Provisional |  | Pending |
| Income | Income | आम्दानी (aamdani) | आय (aaya) | Money received | Provisional |  | Pending |
| Expense | Expense | खर्च (kharcha) | खर्च रकम | Money spent | Provisional |  | Pending |
| Balance | Balance | बाँकी रकम (baki rakam) | मौजुदात रकम | Current remaining total | Provisional |  | Pending |
| Amount | Amount | रकम (rakam) | राशि | Numeric money value | Provisional |  | Pending |
| Category | Category | श्रेणी (shreni) | वर्ग | Transaction category constrained by type | Provisional |  | Pending |
| Date | Date | मिति (miti) | तारिख | Calendar date | Provisional |  | Pending |
| Time | Time | समय (samay) | बेला | Clock time | Provisional |  | Pending |
| Note | Note | टिप्पणी (tippani) | नोट | Optional free-text note on a transaction | Provisional |  | Pending |
| Add | Add | थप्नुहोस् (thapnuhos) | थप गर्नुहोस् | Create a new entry | Provisional |  | Pending |
| Edit | Edit | सम्पादन गर्नुहोस् (sampadan garnuhos) | सुधार गर्नुहोस् | Modify an existing entry | Provisional |  | Pending |
| Delete | Delete | मेटाउनुहोस् (mataunuhos) | हटाउनुहोस् | Remove an entry (destructive) | Provisional |  | Pending |
| Confirm | Confirm | पुष्टि गर्नुहोस् (pushti garnuhos) | निश्चित गर्नुहोस् | Accept a destructive or final action | Provisional |  | Pending |
| Cancel | Cancel | रद्द गर्नुहोस् (radd garnuhos) | छोड्नुहोस् | Abandon an action | Provisional |  | Pending |
| Backup | Backup | ब्याकअप (byakap) | जगेडा प्रति (jageda prati) | Exported farm file for safekeeping | Provisional |  | Pending |
| Export | Export | निर्यात (niryat) | बाहिर पठाउनुहोस् | Save a backup file | Provisional |  | Pending |
| Import | Import | आयात (aayat) | भित्र ल्याउनुहोस् | Load a backup file | Provisional |  | Pending |
| Restore | Restore | पुनर्स्थापना (punasthapana) | फिर्ता ल्याउनुहोस् | Replace current state from a backup | Provisional |  | Pending |
| Overwrite | Overwrite | ओभरराइट (overwrite) | अधिलेखन | Replacing existing data (destructive) | Provisional |  | Pending |
| Invalid file | Invalid file | अमान्य फाइल (amanaya phaila) | मान्य नभएको फाइल | Rejected backup file | Provisional |  | Pending |
| Currency | Currency | मुद्रा (mudra) | — | Money unit of account | Provisional |  | Pending |
| Nepali rupee | Nepali rupee | नेपाली रुपैयाँ (Nepali rupaiya) | रु. | NPR currency display | Provisional |  | Pending |

## Rules

- Terms are approved only after pilot evidence; unconfirmed terms stay `Provisional`.
- Where the loanword (e.g. `ब्याकअप`) is more immediately understood than a constructed equivalent, the glossary records both and the pilot decides.
- Every glossary term maps to a single resource key used by all screens.
- Never ship a new user-facing term without an entry in this glossary.
