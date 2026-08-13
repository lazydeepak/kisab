# RC-01 — Final Release-Candidate Validation Record (v0.2.0)

> **Delivery of RC-01 (validation).** Executed against the frozen v0.2.0 release candidate on a physical device (Moto Edge 60 Fusion, API 36) and an API-26 emulator. Companion to `docs/release/V0.2.0_RELEASE_CHECKLIST.md`, whose dispositions this record supports. No product code was changed, no build was re-signed, no tag or release was created, and nothing was merged as a result of this record.

## Candidate identity

| Item | Value |
| --- | --- |
| Candidate commit | `53de2711cb7fabca6fb39ee13892822518b36d4f` (`main` at freeze; doc-reconcile commit `docs(release)…(#28)`) |
| APK | `kisab-rc-apk.apk`, 2,886,924 bytes, SHA-256 `650fa248f7679dfb104b298552c8b243472ae56543f40c460487d61210229452` |
| Package / version | `com.susankhya.kisab` v0.2.0 (versionCode 3) |
| Signer certificate SHA-256 | `92a578e8…1f89cff` — same production certificate as published v0.1.0 (signer continuity preserved) |
| Verification note | The RC APK used for every device run was pulled from the device after install and independently SHA-256-verified against the recorded value. The build-output copy at `app/build/outputs/apk/release/app-release.apk` is stale (`593b57fd…`, 2,655,903 bytes) and was **not** used for any validation. |

## Devices

| Environment | Role in RC-01 |
| --- | --- |
| Moto Edge 60 Fusion, serial `ZA22374XPC`, Android 16 / API 36, 1220×2712 @ 450 dpi | Primary physical device. Upgrade gate, full smoke matrix, all manual batteries. |
| Android emulator `api26` (AVD), API 26 (Android 8.0), 1080×1920 @ 160 dpi (≈ 420 dp wide) | Older-API / small-screen emulator. Empty-farm currency switch, financial-overview empty state, small-screen layout, back-nav re-observation. |

## Gate results

| Gate | Disposition | Observation summary |
| --- | --- | --- |
| RC-01A — published `v0.1.0` → candidate in-place upgrade | `PASS` | See checklist Section C log. v0.1.0-era farm `RC01UpgradeFarm` (2 entries, 6 transactions, balance −1,247,600.00 NPR) preserved through same-signer install, force-stop, cold relaunch, and full device reboot; schema migrated to v6; post-upgrade features (party CRUD, trade + settlement, Party Khata, overview period switch, Party Hisab, calculator) exercised without crash or data loss; fresh backup export decoded and reconciled. |
| RC-01B — M5-05 Financial Overview manual battery | `PASS` | Period switching (This month / Last 30 days / All time), all four sections re-render, empty states, as-of line, and Nepali rendering verified on the physical device. |
| RC-01C — M6 Party Hisab + Parties + Trades manual battery | `PASS` | Party/period switching, activity + position, recreation restore, party CRUD with role spinner, and trade create/edit/delete + payment-status transitions verified. |
| RC-01D — M6.3 Kisan toolbox manual battery | `PASS` | Money arithmetic, profit/loss, interest, land conversion, Nepali digits, error + focus behavior, and empty-farm usability verified on the physical device. |
| RC-01E — M6.1 visual/accessibility pass | `PASS` | App-shell labels, large-font layout, Nepali (Devanagari + Nepali digits) rendering, and popup placement verified. TalkBack order and icon contrast remain covered by the automated M6.4.1 suite (accepted residual — see register). |
| RC-01F — full-product smoke matrix #1–#14 (Moto) | `PASS` | All fourteen bounded smokes passed. Notable: party-delete-with-trades rule blocks with "Cannot delete a party that has sales or purchases. Remove those first."; trade-with-payments delete blocked; unpaid-trade delete confirms ("Delete this sale/purchase permanently?"); all five M6.4 calculators return exact expected results (below); backup export/import round-trip including invalid-import rejection and valid-restore replacement confirm; insets and back-navigation correct. |
| RC-01G — API-26 emulator pass | `PASS` | Empty-farm currency switch NPR→INR→NPR unlocked; financial-overview empty state renders all sections ("No cash income or expenses in this period.", "No sales, purchases, or payments in this period.", "No money owed or owing at the end of this period.", "No monthly activity in this period yet.", "No sales or purchases yet.", "No parties yet. Add the people and businesses you deal with."); small-screen layout for Home / Hisab-Kitab / Settings fits without clipping in both EN and NE; manual back-nav from Hisab-Kitab and Settings correct. |

### M6.4 calculator spot results (smoke #12)

| Calculator | Input | Expected | Observed |
| --- | --- | --- | --- |
| Seed quantity and cost | 2 × 25 × 50 (Ropani) | 50 kg / 2,500 NPR | Match; land-unit spinner (Ropani/Aana/Bigha/Kattha) switch verified |
| Fertilizer quantity and cost | 3 × 40 × 45 | 120 kg / 5,400 NPR | Match |
| Feed requirement and cost | 5 × 2 × 30 × 10 | 300 kg / 3,000 NPR | Match |
| Milk production and revenue | 2 × 5 × 10 × 60 | 100 L / 6,000 NPR | Match |
| Crop yield and revenue | 4 × 250 × 35 | 1,000 kg / 35,000 NPR | Match |

No calculator mutates farm state (persistence isolation re-verified by backup diff).

### Smoke #13 backup/restore detail

- Export: `kisab-rc01upgradefarm.backup` (SAF auto-renamed to `kisab-rc01upgradefarm (1)/(2)/(3).backup`); decoded payload: envelope schema 1, persistence schema 6, farm `RC01UpgradeFarm`, NPR, 2 entries, 6 transactions, parties Ram Kumar Sharma (CUSTOMER) and Shyam Agro (SUPPLIER), 4 trades, 5 settlements, balance −1,247,600.00, net receivable 15,000.00.
- Invalid import: garbage file and wrong-version file both rejected; farm preserved; toast "Invalid or unsupported backup" (`error_backup_invalid_or_unsupported`). Toast text is not capturable on the Moto by uiautomator (accepted evidence limitation); rejection was confirmed via farm state unchanged.
- Valid restore: confirmation dialog "Replace current farm?" / "Import backup for RC01UpgradeFarm? Entries: 2, Transactions: 6, Balance: -1,247,600.00 NPR / This will replace the current farm permanently." with CANCEL and REPLACE FARM. Cancel preserved state; replace restored full state after a 5.00 NPR perturbation expense (balance returned to −1,247,600.00, perturbation gone, all data intact).

## Residual register (accepted, kept visible)

| Severity | Item | Disposition | Note |
| --- | --- | --- | --- |
| Low | API-26 back-nav coordinate flakes (2 Espresso tests) | `ACCEPTED RESIDUAL` | Reproduced on the pre-M6.4.1 baseline; not product defects. Re-observed in RC-01G: manual back-nav from Hisab-Kitab and Settings is correct on the API-26 emulator — recorded as a data point, not a silent flip to `PASS`. |
| Low | Toast-window text capture on Moto | `ACCEPTED RESIDUAL` | uiautomator does not expose Toast windows on this device; toast-verification relied on `FarmUiError`-driven code paths and unchanged farm state. |
| Low | Icon contrast / TalkBack order not visually re-audited | `ACCEPTED RESIDUAL` | Covered by the automated M6.4.1 connected suite and string/accessibility review; model-based visual confirmation was not possible. |
| Low | Stale build-output APK | `ACCEPTED RESIDUAL` | `app/build/outputs/apk/release/app-release.apk` is stale; the exact RC artifact is preserved under the evidence path below with its SHA-256. |
| Medium | Older/lower-resource **physical** device not proven | `ACCEPTED RESIDUAL` | Carried from `M4_FIELD_VALIDATION…:32` and `M4_05_D001…:72`; acknowledged, does not block the release decision. |
| Low | Extra test artifacts left in device Download | `ACCEPTED RESIDUAL` | `kisab-invalid-*.backup` and `kisab-rc01upgradefarm (n).backup` remain in `/sdcard/Download/`; harmless, removable. |

## Evidence manifest

- Exact RC APK: `/tmp/kisab-rc01-evidence/kisab-rc-apk.apk` (SHA-256 `650fa248…229452`).
- Decoded backups: `/tmp/kisab-rc01-evidence/kisab-rc01upgradefarm-final.backup`, `…-current.backup`.
- Corrupt-import test inputs: `/tmp/kisab-rc01-evidence/kisab-invalid-garbage.backup`, `kisab-invalid-badversion.backup`.
- Device run records and uiautomator dumps: RC-01A–G session logs; checklist Section E result column.
- Supporting records: `docs/validation/V0.2.0_PILOT_CHECKLIST.md` (pilot precedent), `docs/milestones/M6_4_FARM_INPUT_CALCULATORS.md`, `docs/milestones/M6_4_1_SHELL_SYSTEM_BAR_INSETS.md`.

## Conclusion

All High gate items defined in `V0.2.0_RELEASE_CHECKLIST.md` Section G are now executed and passing on the exact candidate `53de271` (production-signed APK `650fa248…`). Residuals are Low/Medium, documented, and non-blocking. The checklist is re-issued with disposition `RELEASE READY`; the release decision is re-issued to the maintainer for gating.
