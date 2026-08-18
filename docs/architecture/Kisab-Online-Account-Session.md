# Kisab Online Account + Secure Session Contract

Provider-neutral layer between future Google/Apple SDKs and existing
`AccountLink` / `KisabSession`. **No real Kisab HTTP backend exists in-repo yet**;
the client defines `AccountApi` and tests use `FakeAccountApi`.

## Flow

```text
Provider SDK (future)
  → ProviderCredential (GOOGLE|APPLE + opaque assertion)
  → AccountApi.establishAccount(localUserId?, credential)
  → EstablishAccountResponse { accountId, sessionId, accessToken, refreshToken? }
  → OnlineAccountService
       1. call API
       2. validate response
       3. pre-check AccountLink conflict (different account → fail, no session write)
       4. KisabSessionStorageAdapter.save (foundation keystore)
       5. AccountLinkService.linkToAccount
       6. on link failure after 4 → sessionStorage.clear() and fail
  → OnlineAccountResult.Success | Failure
```

## Separation

| Layer | Responsibility |
|-------|----------------|
| LocalUser | Offline identity; owns local farms |
| AccountLink | LocalUser ↔ Kisab `accountId` (prefs, non-secret) |
| KisabSession | access/refresh tokens (secure session storage only) |
| Auth provider | Proves person; never becomes account id |
| AccountApi | Backend exchange contract |

## Request / response (client contract)

**Request:** `provider`, opaque `assertion`, optional `localUserId` (client context only).

**Success response:** `accountId`, `sessionId`, `accessToken`, optional `refreshToken`.

**Errors (AccountApiFailureKind):** TRANSPORT, INVALID_CREDENTIAL, SERVER_REJECTED, INVALID_RESPONSE.

## Rollback

- Session write fails → no AccountLink.
- AccountLink fails after session write → clear session; return failure.
- Never leave “session without intended link” as Success.

## Explicitly out of scope

- Google/Apple SDK/UI
- Farm upload/download/merge
- Premium, ads, push, cloud storage
- Full sign-out product UX (session clear is available for tests/orchestration)

## Future provider integration

Provider code should only: obtain credential → `OnlineAccountService.establish` → handle `OnlineAccountResult`.
No changes required to LocalUser, FarmStore, or farm IDs.
