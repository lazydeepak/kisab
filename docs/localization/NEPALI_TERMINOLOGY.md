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
| Transport | Transport | यातायात (yatayat) | ढुवानी | Expense transaction category (haulage/market transport) | Provisional | Rendered in Nepali; new category added for M4-05-D001 | Pending |
| Other expense | Other expense | अन्य खर्च (anya kharcha) | — | Catch-all expense category | Provisional | Rendered in Nepali; no longer the fallback for transport since M4-05-D001 | Pending |
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
| Party | Party | पार्टी (parti) | — | A business counterpart of the farm (buyer or supplier) | Provisional |  | Pending |
| Customer | Customer | ग्राहक (grahak) | — | Party role: buys farm produce from the farmer | Provisional |  | Pending |
| Supplier | Supplier | आपूर्तिकर्ता (aapurtikarta) | विक्रेता (bikreta) | Party role: sells inputs to the farmer | Provisional |  | Pending |
| Contact | Contact | सम्पर्क (sampark) | फोन नम्बर (phone number) | Free-text contact detail for a party (e.g. phone number) | Provisional |  | Pending |
| Add party | Add party | पार्टी थप्नुहोस् (parti thapnuhos) | — | Action that opens the party editor for a new party | Provisional |  | Pending |
| Parties | Parties | पार्टीहरू (partiharū) | — | List of parties on the Hisab-Kitab destination | Provisional |  | Pending |
| Sale | Sale | बिक्री (bikri) | — | Trade type: produce sold by the farmer to a customer or for cash | Provisional | Reuses the M4 governed term for the income category | Pending |
| Purchase | Purchase | खरिद (kharid) | — | Trade type: inputs bought by the farmer from a supplier or for cash | Provisional |  | Pending |
| New sale | New sale | नयाँ बिक्री (naya bikri) | — | Action/editor title for a new sale | Provisional |  | Pending |
| New purchase | New purchase | नयाँ खरिद (naya kharid) | — | Action/editor title for a new purchase | Provisional |  | Pending |
| Total amount | Total amount | कुल रकम (kul rakam) | — | Full monetary value of a sale or purchase | Provisional |  | Pending |
| Payment | Payment | भुक्तानी (bhuktani) | — | The state of payment for a sale/purchase (paid/partial/unpaid) | Provisional |  | Pending |
| Paid | Paid | भुक्तानी भयो (bhuktani bhayo) | — | Fully paid | Provisional |  | Pending |
| Partially paid | Partially paid | आंशिक भुक्तानी (aanshik bhuktani) | — | Paid more than zero but less than the total | Provisional |  | Pending |
| Unpaid | Unpaid | भुक्तानी बाँकी (bhuktani banki) | — | Nothing paid yet; amount still owed | Provisional |  | Pending |
| Amount paid | Amount paid | भुक्तानी गरिएको रकम (bhuktani garieko rakam) | — | Money received (sale) or handed over (purchase) so far | Provisional |  | Pending |
| Amount due | Amount due | बाँकी रकम (banki rakam) | — | Outstanding balance on a trade | Provisional |  | Pending |
| To receive | To receive | प्राप्त गर्न बाँकी (prapat garna banki) | — | Outstanding money the farmer is owed (sale balances) | Provisional |  | Pending |
| To pay | To pay | तिर्न बाँकी (tirna banki) | — | Outstanding money the farmer owes (purchase balances) | Provisional |  | Pending |
| Cash sale | Cash sale | नगद बिक्री (nagad bikri) | — | Fully paid sale with no party linked | Provisional |  | Pending |
| Cash purchase | Cash purchase | नगद खरिद (nagad kharid) | — | Fully paid purchase with no party linked | Provisional |  | Pending |
| Customer and supplier | Customer and supplier | ग्राहक र आपूर्तिकर्ता दुवै (grahak ra aapurtikarta duvai) | — | Party role valid on both sides of the farm's business | Provisional |  | Pending |
| Recent sales and purchases | Recent sales and purchases | हालैका बिक्री र खरिदहरू (halaiika bikri ra kharidharu) | — | Recent trades section on the Hisab-Kitab destination | Provisional |  | Pending |
| Payments | Payments | भुक्तानीहरू (bhuktani-haru) | — | Button that opens a trade's payment records | Provisional | M5-03: per-trade settlement ledger view | Pending |
| Receive payment | Receive payment | भुक्तानी लिनुहोस् (bhuktani linuhos) | — | Action adding a payment to a sale (money received) | Provisional | M5-03 | Pending |
| Record payment | Record payment | भुक्तानी रेकर्ड गर्नुहोस् (bhuktani record garnuhos) | — | Action adding a payment to a purchase (money paid) | Provisional | M5-03 | Pending |
| Add payment | Add payment | भुक्तानी थप्नुहोस् (bhuktani thapnuhos) | — | Save action for a new payment record | Provisional | M5-03 | Pending |
| New payment | New payment | नयाँ भुक्तानी (naya bhuktani) | — | Title for the new-payment form | Provisional | M5-03 | Pending |
| Edit payment | Edit payment | भुक्तानी सम्पादन गर्नुहोस् (bhuktani sampadan garnuhos) | — | Title for the edit-payment form | Provisional | M5-03 | Pending |
| Update payment | Update payment | भुक्तानी अद्यावधिक गर्नुहोस् (bhuktani adyavadhik garnuhos) | — | Save action for editing an existing payment record | Provisional | M5-03 | Pending |
| Delete payment | Delete payment | भुक्तानी मेटाउनुहोस् (bhuktani metaunuhos) | — | Removes a payment record permanently | Provisional | M5-03 | Pending |
| Payment amount | Payment amount | भुक्तानी रकम (bhuktani rakam) | — | Money amount of one payment record | Provisional | M5-03 | Pending |
| Payment note | Payment note | टिप्पणी (tippani) | — | Optional free-text note on a payment record | Provisional | M5-03 | Pending |
| Payments received | Payments received | प्राप्त भुक्तानीहरू (prapat bhuktani-haru) | — | History heading for a sale's money received | Provisional | M5-03 | Pending |
| Payments made | Payments made | गरिएका भुक्तानीहरू (garieka bhuktani-haru) | — | History heading for a purchase's money paid | Provisional | M5-03 | Pending |
| No payments recorded yet | No payments recorded yet | अहिलेसम्म कुनै भुक्तानी रेकर्ड छैन (ahilesamma kunai bhuktani record chaina) | — | Empty state for a trade with no payment records | Provisional | M5-03 | Pending |
| Khata | Khata / Party Khata | खाता (khata) | पार्टीको हिसाबकिताब | Per-party ledger screen on the Hisab-Kitab destination | Provisional | M5-04 | Pending |
| Net position | Net position | कुल स्थिति (kul sthiti) | खुद स्थिति | Informational to-receive minus to-pay for a party | Provisional | M5-04 | Pending |
| You should receive | You should receive … | तपाईंले … प्राप्त गर्नुपर्ने (tapaile … prapat garnuparne) | — | Positive party balance: money owed to the farmer | Provisional | M5-04 | Pending |
| You should pay | You should pay … | तपाईंले … तिर्नुपर्ने (tapaile … tirnuparne) | — | Negative party balance: money the farmer owes | Provisional | M5-04 | Pending |
| Settled | Settled | मिलान भयो (milan bhayo) | बराबर भयो | Zero party/net balance | Provisional | M5-04 | Pending |
| Payment received | Payment received | भुक्तानी प्राप्त भयो (bhuktani prapat bhayo) | — | Khata row: a payment received from a customer (sale settlement) | Provisional | M5-04 | Pending |
| Payment made | Payment made | भुक्तानी गरियो (bhuktani gariyo) | — | Khata row: a payment made to a supplier (purchase settlement) | Provisional | M5-04 | Pending |
| Balance after | Balance after | पछि (pachhi) / पछिको बाँकी | — | Khata running balance after a row's event | Provisional | M5-04 | Pending |
| Financial overview | Financial overview | वित्तीय अवलोकन (wittiya awalokan) | — | Farm-wide financial summary section on Hisab-Kitab | Provisional | M5-05 | Pending |
| Period | Period | अवधि (awadhi) | अवधि छनोट | Time window selector for the financial overview | Provisional | M5-05 | Pending |
| This month | This month | यो महिना (yo mahina) | — | Preset: current calendar month | Provisional | M5-05 | Pending |
| Last 30 days | Last 30 days | पछिल्लो ३० दिन (pachhilo 30 din) | विगत ३० दिन | Preset: rolling 30-day window | Provisional | M5-05 | Pending |
| All time | All time | सबै समय (sabai samay) | — | Preset: entire recorded history | Provisional | M5-05 | Pending |
| Cash activity | Cash activity | नगद गतिविधि (nagad gatibidhi) | — | Overview section: Home income/expense in the period | Provisional | M5-05 | Pending |
| Trade and payments | Trade and payments | बिक्री/खरिद र भुक्तानी (bikri/kharid ra bhuktani) | — | Overview section: sales, purchases, and payment flows | Provisional | M5-05 | Pending |
| Current position | Current position | हालको स्थिति (halako sthiti) | — | Overview section: outstanding receivable/payable as of period end | Provisional | M5-05 | Pending |
| Monthly trend | Monthly trend | मासिक प्रवृत्ति (masik prawritti) | मासिक विवरण | Overview section: per-month activity rows | Provisional | M5-05 | Pending |
| Net | Net | खुद (khud) | — | Cash income minus expense in the period | Provisional | M5-05 | Pending |
| Receivable | Receivable | प्राप्त गर्न बाँकी (prapat garna banki) | — | Outstanding money the farmer is owed as of period end | Provisional | M5-05 | Pending |
| Payable | Payable | तिर्न बाँकी (tirna banki) | — | Outstanding money the farmer owes as of period end | Provisional | M5-05 | Pending |
| As of | As of | सम्मको स्थिति (sammako sthiti) | — | Position timestamp: as of the end of the period | Provisional | M5-05 | Pending |
| No cash activity empty | No cash income or expenses in this period. | यस अवधिमा कुनै नगद आम्दानी वा खर्च छैन। (yas awadhimaa kunai nagad aamdani wa kharcha chaina) | — | Empty state: no Home income/expense in the selected period | Provisional | M5-05 | Pending |
| No trade activity empty | No sales, purchases, or payments in this period. | यस अवधिमा कुनै बिक्री, खरिद वा भुक्तानी छैन। (yas awadhimaa kunai bikri, kharid wa bhuktani chaina) | — | Empty state: no trades/settlements in the selected period | Provisional | M5-05 | Pending |
| No position empty | No money owed or owing at the end of this period. | यस अवधिको अन्त्यमा प्राप्त गर्नुपर्ने वा तिर्नुपर्ने कुनै रकम छैन। (yas awadhiko antyamaa prapat garnuparne wa tirnuparne kunai rakam chaina) | — | Empty state: nothing outstanding at period end | Provisional | M5-05 | Pending |
| No trend empty | No monthly activity in this period yet. | यस अवधिमा अहिलेसम्म कुनै मासिक कारोबार छैन। (yas awadhimaa ahilesamma kunai masik karobar chaina) | — | Empty state: no monthly activity yet | Provisional | M5-05 | Pending |
| Party Hisab calculator | Party Hisab calculator | पार्टी हिसाब गणना (party hisab ganana) | पार्टी हिसाब | Hisab destination title for per-party reconciliation | Provisional | M6 | Pending |
| Activity in this period | Activity in this period | यस अवधिको कारोबार (yas awadhiko karobar) | अवधिको हिसाब | Selected Party's sales, purchases, and payments in the period | Provisional | M6 | Pending |
| Position at period end | Position at period end | अवधिको अन्त्यको स्थिति (awadhiko antyako sthiti) | अन्तिम बाँकी | Gross receivable/payable position immediately before the period cutoff | Provisional | M6 | Pending |
| No farm for Hisab | Create or restore a farm to calculate Party Hisab. | पार्टी हिसाब गणना गर्न फार्म बनाउनुहोस् वा पुनर्स्थापना गर्नुहोस्। | — | Hisab empty state when no farm is open | Provisional | M6 | Pending |
| No parties for Hisab | Add a customer or supplier in Hisab-Kitab to calculate their Hisab. | हिसाब गणना गर्न हिसाब-किताबमा ग्राहक वा आपूर्तिकर्ता थप्नुहोस्। | — | Hisab empty state when the farm has no parties | Provisional | M6 | Pending |
| Nothing due with Party | Nothing is due between you and this party at the period end. | अवधिको अन्त्यमा तपाईं र यस पार्टीबीच कुनै रकम बाँकी छैन। | — | Party Hisab zero-position guidance | Provisional | M6 | Pending |
| Kisab logo | Kisab logo | किसाबको लोगो (Kisabko logo) | किसाब चिन्ह | Accessibility description for the branded ledger-and-sprout mark | Provisional | M6.1 | Pending |
| More options | More options | थप विकल्पहरू (thap wikalpaharu) | मेनु | Accessibility description for the top-app-bar overflow menu | Provisional | M6.1 | Pending |
| Kisan calculator tools | Kisan calculator tools | किसान गणना उपकरणहरू (kisan ganana upakaranharu) | किसान टुल्स | Offline calculator toolbox heading | Provisional | M6.3 | Pending |
| Money arithmetic | Money arithmetic | रकम गणना (rakam ganana) | हिसाब गर्ने | General addition, subtraction, multiplication, division, and percentage section | Provisional | M6.3 | Pending |
| Profit | Profit | नाफा (nafa) | — | Positive sale amount minus total cost | Provisional | M6.3 | Pending |
| Loss | Loss | घाटा (ghata) | नोक्सानी | Negative sale amount minus total cost, displayed as an absolute loss amount | Provisional | M6.3 | Pending |
| Margin | Margin | मार्जिन (marjin) | बिक्री नाफा प्रतिशत | Profit as a percentage of sale amount | Provisional | M6.3 | Pending |
| Markup | Markup | मार्कअप (markup) | लागत नाफा प्रतिशत | Profit as a percentage of total cost | Provisional | M6.3 | Pending |
| Simple interest | Simple interest | साधारण ब्याज (sadharan byaj) | — | Non-compounding interest over principal, annual rate, and months | Provisional | M6.3 | Pending |
| Principal | Principal | साँवा रकम (sanwa rakam) | मूलधन | Starting loan amount for simple-interest calculation | Provisional | M6.3 | Pending |
| Nepali land converter | Nepali land converter | नेपाली जग्गा रूपान्तरण (nepali jagga rupantaran) | जग्गा हिसाब | Hill, Terai, and square-metre land-unit conversion | Provisional | M6.3 | Pending |
| Square metre | Square metre | वर्ग मिटर (warga mitar) | — | Metric base unit for land conversion | Provisional | M6.3 | Pending |
| Farm planning | Farm planning | फार्म योजना (farm yojana) | खेती योजना | Temporary input calculations using farmer-provided rates | Provisional | M6.4 | Pending |
| Seed rate | Seed rate | बिउ दर (biu dar) | बीउ दर | Kilograms of seed entered per selected land unit | Provisional | M6.4 | Pending |
| Fertilizer rate | Fertilizer rate | खाद दर (khad dar) | मल दर | Kilograms of fertilizer entered per selected land unit | Provisional | M6.4 | Pending |
| Feed requirement | Feed requirement | खाना आवश्यकता (khana awashyakata) | दाना आवश्यकता | Total animal feed calculated for a period | Provisional | M6.4 | Pending |
| Milk production | Milk production | दुध उत्पादन (dudh utpadan) | दूध उत्पादन | Total litres calculated for milking animals and days | Provisional | M6.4 | Pending |
| Crop yield | Crop yield | बाली उपज (bali upaj) | उत्पादन | Expected kilograms calculated from area and farmer-entered yield rate | Provisional | M6.4 | Pending |
| Revenue | Revenue | राजस्व (rajaswa) | आम्दानी | Quantity multiplied by farmer-entered selling price | Provisional | M6.4 | Pending |

## Rules

- Terms are approved only after pilot evidence; unconfirmed terms stay `Provisional`.
- Where the loanword (e.g. `ब्याकअप`) is more immediately understood than a constructed equivalent, the glossary records both and the pilot decides.
- Every glossary term maps to a single resource key used by all screens.
- Never ship a new user-facing term without an entry in this glossary.
