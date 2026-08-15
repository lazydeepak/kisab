# Kisab Account Linking Foundation

Short contract between offline identity and a future Kisab online account.
No network, provider SDKs, or sync behavior in this milestone.

## Four distinct concepts

| Concept | Role | Persistence |
|--------|------|-------------|
| **LocalUser** | Offline ownership identity (`user-<uuid>`). Owns local farms. | `kisab_local_user` |
| **Account link** | Whether this LocalUser is linked to a Kisab online account. | `kisab_account_link` (non-secret metadata only) |
| **Auth provider** | Future Google / Apple / email-OTP that *authenticates* a person. | Not stored as Kisab identity |
| **Session** | Temporary access/refresh credentials for API calls. | Foundation keystore session storage (`KisabSession`) |

Do not merge these. Losing a session must not delete LocalUser, farms, or the account-link record.

## Stable identity rules

- Kisab owns account identity (`account-…` on the server later).
- Providers never become the primary Kisab account id.
- Linking must not replace `LocalUser.userId` or farm ids.
- App works fully offline with **unlinked** default; no login required to launch.

## Intended later link flow (not implemented here)

1. Device already has LocalUser + zero or more farms.
2. User chooses future “Sign in / Sync”.
3. Provider authenticates the person.
4. Kisab backend finds or creates a Kisab Online Account; returns account id + session.
5. Client calls local `AccountLinkService.linkToAccount(localUserId, accountId)`.
6. Farm ids stay the same; sync later maps/uploads/merges server farms.

## Link rules (local)

- Default: unlinked.
- Link same `accountId` twice: **idempotent**.
- Link a **different** `accountId` while already linked: **rejected** (`AccountLinkConflictException`). No silent switch.
- Unlink / sign-out product policy deferred; farms must not depend on a valid session.

## Explicitly deferred

- Existing online account that already has farms + local farms → merge/conflict UI and sync.
- Premium, storage quota, ads, push, devices, notification prefs (account may own these later).
- Server ownership/membership roles replacing local ownership.
- Provider SDKs and networking.

## Farm backup boundary

Farm export remains farm-only (`FarmBackupCodec`). It must not embed LocalUser id, Kisab account id, provider ids, tokens, or subscription state.

## Code entry points

- `domain/AccountLink.kt` — sealed link state
- `domain/AccountLinkStore.kt` / `AccountLinkService.kt`
- `persistence/SharedPreferencesAccountLinkStore.kt`
- Session remains `session/KisabSessionStorageAdapter.kt`
