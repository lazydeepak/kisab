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
| Balance | Balance | बाँकी रकम (baki rakam) | मौजुदात रकम | Current remaining total | Provisional | Rendered correctly in Nepali digits (`बाँकी रकम: १,२५०.५०`); no ambiguity observed in facilitator scenario (M4-04-01) | Pending |
| Amount | Amount | रकम (rakam) | राशि | Numeric money value | Provisional |  | Pending |
| Category | Category | श्रेणी (shreni) | वर्ग | Transaction category constrained by type | Provisional |  | Pending |
| Date | Date | मिति (miti) | तारिख | Calendar date | Provisional |  | Pending |
| Time | Time | समय (samay) | बेला | Clock time | Provisional |  | Pending |
| Note | Note | टिप्पणी (tippani) | नोट | Optional free-text note on a transaction | Provisional |  | Pending |
| Add | Add | थप्नुहोस् (thapnuhos) | थप गर्नुहोस् | Create a new entry | Provisional |  | Pending |
| Edit | Edit | सम्पादन गर्नुहोस् (sampadan garnuhos) | सुधार गर्नुहोस् | Modify an existing entry | Provisional |  | Pending |
| Delete | Delete | मेटाउनुहोस् (mataunuhos) | हटाउनुहोस् | Remove an entry (destructive) | Provisional | Rendered in Nepali in delete-confirmation flow (M4-04-01) | Pending |
| Confirm | Confirm | पुष्टि गर्नुहोस् (pushti garnuhos) | निश्चित गर्नुहोस् | Accept a destructive or final action | Provisional |  | Pending |
| Cancel | Cancel | रद्द गर्नुहोस् (radd garnuhos) | छोड्नुहोस् | Abandon an action | Provisional | Rendered in Nepali in delete-confirmation flow (M4-04-01) | Pending |
| Backup | Backup | ब्याकअप (byakap) | जगेडा प्रति (jageda prati) | Exported farm file for safekeeping | Provisional |  | Pending |
| Export | Export | निर्यात (niryat) | बाहिर पठाउनुहोस् | Save a backup file | Provisional |  | Pending |
| Import | Import | आयात (aayat) | भित्र ल्याउनुहोस् | Load a backup file | Provisional |  | Pending |
| Restore | Restore | पुनर्स्थापना (punasthapana) | फिर्ता ल्याउनुहोस् | Replace current state from a backup | Provisional |  | Pending |
| Overwrite | Overwrite | ओभरराइट (overwrite) | अधिलेखन | Replacing existing data (destructive) | Provisional |  | Pending |
| Invalid file | Invalid file | अमान्य फाइल (amanaya phaila) | मान्य नभएको फाइल | Rejected backup file | Provisional |  | Pending |
| Currency | Currency | मुद्रा (mudra) | — | Money unit of account | Provisional |  | Pending |
| Nepali rupee | Nepali rupee | नेपाली रुपैयाँ (Nepali rupaiya) | रु. | NPR currency display | Provisional |  | Pending |
| Entry | Entry | प्रविष्टि (pravisti) | — | An item (livestock/crop) recorded on a farm | Provisional |  | Pending |
| Name | Name | नाम (nam) | — | The label of a farm, entry, or field | Provisional |  | Pending |
| Quantity | Quantity | परिमाण (parimana) | सङ्ख्या | Count/number of an entry | Provisional |  | Pending |
| Description | Description | विवरण (bibaran) | टिप्पणी | Free-text note on a transaction | Provisional |  | Pending |
| Create | Create | सिर्जना गर्नुहोस् (sirjana garnuhos) | बनाउनुहोस् | Create a new farm or transaction | Provisional |  | Pending |
| Save | Save | बचत गर्नुहोस् (bachat garnuhos) | सुरक्षित गर्नुहोस् | Persist a new or edited transaction | Provisional |  | Pending |
| Update | Update | अद्यावधिक गर्नुहोस् (adyabadhik garnuhos) | — | Save changes to an existing transaction | Provisional |  | Pending |
| Replace | Replace | प्रतिस्थापन गर्नुहोस् (pratisthapan garnuhos) | ओभरराइट | Replace current farm from a backup (destructive) | Provisional |  | Pending |
| Select | Select | चयन गर्नुहोस् (chayan garnuhos) | छान्नुहोस् | Choose an item from a list | Provisional |  | Pending |
| Sales | Sales | बिक्री (bikri) | — | Income transaction category | Provisional | Rendered in Nepali; used for milk and vegetable sales in facilitator scenarios (M4-04-01/02) | Pending |
| Services | Services | सेवा (sewa) | — | Income transaction category | Provisional |  | Pending |
| Other income | Other income | अन्य आम्दानी (anya aamdani) | — | Catch-all income category | Provisional |  | Pending |
| Feed | Feed | दाना (dana) | चारा | Expense transaction category (animal fodder) | Provisional | Rendered in Nepali; used for feed expense (M4-04-01) | Pending |
| Supplies | Supplies | सामग्री (samagri) | — | Expense transaction category | Provisional | Rendered in Nepali; used for seeds/fertilizer (M4-04-02) | Pending |
| Labor | Labor | श्रम (shram) | ज्याला | Expense transaction category | Provisional | Rendered in Nepali; used for worker payment (M4-04-01) | Pending |
| Other expense | Other expense | अन्य खर्च (anya kharcha) | — | Catch-all expense category | Provisional | Rendered in Nepali; used for transport in Scenario B because no transport category exists (M4-04-D001) | Pending |
| Count | Count | सङ्ख्या (sankhya) | गणना | Number of entries or transactions | Provisional |  | Pending |
| None yet | None yet | कुनै ... छैन (kunai ... chaina) | खाली | No entries/transactions recorded yet | Provisional |  | Pending |
| Required | Required | आवश्यक छ (awashyak cha) | चाहिन्छ | A field must be provided | Provisional |  | Pending |
| Valid | Valid | मान्य (manye) | सही | A value conforms to its format | Provisional |  | Pending |
| Error | Error | त्रुटि (truti) | गल्ती | Problem message | Provisional |  | Pending |
| Unexpected error | Unexpected error | अप्रत्याशित त्रुटि (apratyashit truti) | — | Unknown/internal failure message | Provisional |  | Pending |
| Local time | Local time | स्थानीय समय (sthaaniya samay) | देशको समय | Device-timezone wall-clock time for timestamps | Provisional |  | Pending |
| Decimal | Decimal | दशमलव (dashamalav) | दशांश | Fractional digits after the decimal separator | Provisional |  | Pending |
| Positive | Positive | धनात्मक (dhanatmak) | धन | Greater than zero | Provisional |  | Pending |
| Record | Record income / expense | अभिलेख गर्नुहोस् (abhiiekh garnuhos) | टिप्नुहोस् | Quick action that opens the transaction editor for income/expense | Provisional |  | Pending |
| Overview | Overview | अवलोकन (awalokan) | सारांश | Balance/income/expense summary at the top of the main screen | Provisional |  | Pending |
| Recent | Recent | हालैका (halaiika) | पछिल्ला | Recent transactions list (newest first) | Provisional |  | Pending |
| Farm tools | Farm tools | फार्म उपकरणहरू (farm upakaranharu) | — | Collapsed section with farm summary, entries, and backup | Provisional |  | Pending |
| Show | Show | देखाउनुहोस् (dekhaunuhos) | खोल्नुहोस् | Expand a collapsed section | Provisional |  | Pending |
| Hide | Hide | लुकाउनुहोस् (lukauunuhos) | बन्द गर्नुहोस् | Collapse an expanded section | Provisional |  | Pending |
| Discard | Discard | त्याग्नुहोस् (tyagnuhos) | छोड्नुहोस् | Abandon unsaved transaction changes | Provisional |  | Pending |
| Keep editing | Keep editing | सम्पादन जारी राख्नुहोस् (sampadan jari rakhunuhos) | — | Continue editing instead of discarding | Provisional |  | Pending |
| Today | Today | आज (aaj) | — | The current day, shown for a default timestamp | Provisional |  | Pending |
| New transaction | New transaction | नयाँ लेनदेन (naya lenaden) | — | Editor title when creating a transaction | Provisional |  | Pending |
| Change | Change | परिवर्तन गर्नुहोस् (parivartan garnuhos) | बदल्नुहोस् | Modify the date/time or currency in the editor | Provisional |  | Pending |

## Rules

- Terms are approved only after pilot evidence; unconfirmed terms stay `Provisional`.
- Where the loanword (e.g. `ब्याकअप`) is more immediately understood than a constructed equivalent, the glossary records both and the pilot decides.
- Every glossary term maps to a single resource key used by all screens.
- Never ship a new user-facing term without an entry in this glossary.
