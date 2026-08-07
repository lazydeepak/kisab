# Kisab M4-04 — Physical-Device and Guided Farmer-Pilot Plan

Plan for the **physical-device and guided farmer-pilot validation** workstream (M4-04). M4-04 executes the physical-device validation matrix and a small guided pilot on representative devices and farmer scenarios, capturing evidence for the M4 exit gate. It is a validation and evidence workstream: no product features are introduced and defects found during the pilot are recorded for M4-05, not silently fixed here.

> **Status:** IN PROGRESS — protocol prepared; candidate identified; execution evidence recorded as it is collected.
>
> **Fidelity rule:** every claim below marked "verified"/"passed" reflects a session actually performed on a physical device. No result is inferred from an automated test alone; no reaction is invented for a session that did not occur.

## Objective

- Execute the physical-device validation matrix on the Pixel 7a reference device and at least one older/lower-resource physical Android device.
- Verify a genuine upgrade from the published production-signed `v0.1.0` APK to a production-signed pilot candidate with the same signer and a higher version code.
- Run approximately three guided sessions (real users or realistic facilitated scenarios) and record task timing, terminology confusion, category gaps, backup discoverability, and defect severity.
- Produce the evidence required for the M4 exit gate without modifying the `v0.1.0` published release, the signing architecture, or the release workflow.

## Candidate identity

The M4-04 pilot candidate:

| Field | Value |
| --- | --- |
| versionName | `0.2.0-pilot.1` |
| versionCode | `2` |
| applicationId | `com.susankhya.kisab` (unchanged) |
| minSdk / targetSdk | 26 / 36 |
| Foundation dependency | `com.susankhya.foundation:foundation-session-android:0.1.1` |
| Persistence schema | 2 |
| Backup envelope schema | 1 |
| Signer | existing Kisab production certificate (same as published `v0.1.0`) |

Version sequence:

```text
v0.1.0          versionCode 1 — published release
0.2.0-pilot.1   versionCode 2 — private pilot candidate (this workstream)
v0.2.0          versionCode 3 or higher — M4-05 final candidate
```

The pilot candidate:

- is not a production release;
- must not receive an annotated tag;
- must not create a GitHub release;
- must not be uploaded to an app store;
- must not modify or replace the published `v0.1.0` release;
- is signed with the existing Kisab production certificate for the real upgrade test.

## Devices

Required physical targets:

1. Pixel 7a reference device.
2. At least one older or lower-resource Android device.

API 26 and API 36 emulators are supplemental only. An emulator does not satisfy the older/lower-resource physical-device requirement. If a required physical device is unavailable, the corresponding gate is recorded as blocked rather than inferred.

## Locales

- English (`en`) locale — required.
- Nepali (`ne`) locale — required.
- Additional locales are recorded if encountered but are not a completion gate.

## Technical validation matrix

Executed on the Pixel 7a and on the second physical device where available:

- clean installation;
- upgrade over published `v0.1.0` where applicable (requires the production-signed pilot APK);
- farm creation;
- income creation;
- expense creation;
- transaction edit;
- transaction delete and confirmation;
- balance / income / expense verification;
- date and time picker;
- force-stop and cold relaunch;
- full device restart;
- backup export;
- invalid backup rejection with current farm preserved;
- valid restore;
- immediate activity recreation after restore;
- English locale;
- Nepali locale;
- increased font scale;
- screen rotation/recreation with a dirty draft;
- Farm tools expansion;
- multi-day realistic transaction data.

Task duration and usability are recorded even when a task technically passes. No task is marked passed solely because an automated test exists.

## Upgrade procedure

Prerequisite: a production-signed pilot APK (`versionCode 2`, same signer certificate SHA-256 as published `v0.1.0`:

`92a578e8cedad6ea86d2dc27663a3279f07a70794627a280f877ab30b1f89cff`).

### Prepare v0.1.0 state

1. Verify the downloaded v0.1.0 APK SHA-256 (`990c100980c469c9411fb7dc66747d0286a3c8020f7d0c8acca949b7e43bd7bc`).
2. Verify its signing certificate.
3. Install the published v0.1.0 APK.
4. Create a disposable farm.
5. Add realistic entries and transactions spanning multiple dates.
6. Force-stop and cold relaunch.
7. Restart the device and relaunch.
8. Export a backup.
9. Record the farm totals, transaction IDs, values and backup SHA-256.

### Upgrade to the pilot candidate

```bash
adb install -r <pilot-release-apk>
```

Do not clear application data. Verify:

- installation succeeds as an upgrade;
- farm identity remains stable;
- entries remain intact;
- transaction IDs remain stable;
- amounts remain exact;
- timestamps remain exact;
- USD or other existing currency remains unchanged;
- totals remain correct;
- backup export still works;
- old backup restores correctly;
- M4-03 overview and quick-entry flow work;
- no migration, parsing or startup error occurs.

Record before/after evidence.

### If production signing inputs are unavailable

- do not use the debug APK for the upgrade claim;
- mark the upgrade test `BLOCKED — PRODUCTION-SIGNED PILOT APK REQUIRED`;
- continue the non-upgrade physical matrix where possible;
- do not claim M4-04 complete.

## Guided scenarios

Approximately three sessions using representative users or realistic facilitated scenarios. Every session records `participant type` as `REAL USER` or `FACILITATOR SCENARIO`. Reactions are observed, never invented. Human comprehension claims require observed human evidence; a facilitator-driven run records what the software shows and does, and does not claim comprehension by an absent user.

- **Scenario A — Dairy/livestock farm.** Locale priority: Nepali. Record milk sale as `SALES`, feed expense as `FEED`, worker payment as `LABOR`; review balance; edit an incorrect amount; delete a duplicate transaction; force-stop and reopen. Observe meaning of income/expense, understanding of Sales/Feed/Labor, amount entry, balance interpretation, and comfort with Nepali terminology.
- **Scenario B — Crop/vegetable farm.** Use multi-day data. Record vegetable sale as `SALES`, seeds/fertilizer as `SUPPLIES`, transport or another uncovered cost using the nearest available category; change transaction date; inspect recent transactions; export a backup. Record category gaps rather than adding categories during M4-04.
- **Scenario C — Mixed farm and recovery.** Create livestock and crop entries; record several days of income and expenses; restart the device; attempt invalid backup import; confirm current farm remains; restore a valid backup; verify totals and recent transactions; switch between English and Nepali where practical. Observe backup discoverability, overwrite confirmation clarity, terminology changes between locales, and confidence that data was preserved.

## Participant consent

- Each session requires explicit, informed consent before data entry.
- Consent is recorded per session (consent recorded: yes/no) in the checklist session record.
- No analytics, cloud telemetry, or personal data is collected.

## Privacy rules

Do not store:

- participant names;
- phone numbers;
- addresses;
- real financial records;
- photos of participants;
- analytics or telemetry.

Use disposable, clearly labeled sample data.

## Observation method

- Facilitator observes and times each task; timing starts at task instruction and stops at completed observable result.
- Help requested, confusion points, incorrect assumptions, category gaps, amount/date friction, and backup discoverability are recorded per task.
- Evidence is preserved (screenshots/logs where available) and hashed where appropriate; APKs, keystores, passwords, raw participant data and large screen recordings are never committed.

## Defect severity

| Severity | Meaning |
| --- | --- |
| BLOCKING | Data loss, backup corruption, incorrect monetary values, transaction identity corruption, unrecoverable crash loop, destructive action without confirmation, or terminology causing the opposite transaction type. Stops the pilot. |
| MAJOR | Workflow-blocking but recoverable; significant mis-interpretation risk. |
| MINOR | Cosmetic or low-impact usability issue. |
| OBSERVATION | Wording preference or non-blocking observation. |

Wording preferences are not classified blocking unless they prevent task completion or create a materially incorrect interpretation.

## Stop conditions

Stop the pilot immediately for:

- data loss;
- backup corruption;
- incorrect monetary values;
- transaction identity corruption;
- unrecoverable crash loop;
- destructive action without confirmation;
- terminology that causes users to record the opposite transaction type.

Minor layout or wording issues do not automatically stop unrelated sessions.

## M4-04 completion gate

M4-04 is complete only when:

- Pixel 7a physical matrix is complete;
- older/lower-resource physical-device matrix is complete;
- upgrade from production v0.1.0 is verified with the same signer;
- English locale is verified physically;
- Nepali locale is verified physically;
- realistic multi-day data is exercised;
- approximately three guided users/scenarios are documented;
- all blocking, major and terminology-critical findings are registered;
- no evidence is fabricated;
- all validation documents are complete;
- unresolved defects are handed explicitly to M4-05.

M4-04 completion does not mean defects are fixed. If blocking defects remain, status may be `M4-04 — VALIDATION COMPLETE, M4-05 CORRECTIONS REQUIRED`. It must not claim the application is release-ready.
