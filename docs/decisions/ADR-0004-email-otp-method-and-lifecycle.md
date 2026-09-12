# ADR-0004: Email Login Method and Lifecycle (Email OTP)

## Status
Accepted (Email OTP).

## Context
`docs/CURRENT.md` requires resolving the email authentication method and
lifecycle **before** implementation of account setup / email login. The
online account contract
([Kisab-Online-Account-Session.md](../architecture/Kisab-Online-Account-Session.md))
defines the provider-neutral exchange (`ProviderCredential → AccountApi.
establishAccount → OnlineAccountService`) and the rollback/conflict rules,
but names email-OTP only as "a future option" and deliberately leaves the
method and lifecycle open. `AuthProvider` currently contains GOOGLE and
APPLE only. No real Kisab HTTP backend exists in-repo; the client defines
`AccountApi` with `FakeAccountApi` for tests.

Goals: preserve offline-first LocalUser/farm IDs, keep tokens out of farm
backups, prove the person without turning the provider credential into a
Kisab identity, and match farmer workflow (occasional internet, minimal
typing friction).

## Decision

### Authentication method: Email OTP
- Person authenticates with a 6-digit numeric OTP delivered by email.
- No password hash storage, no password-reset surface, no weak-password
  policy in scope. Email password and magic-link can be added later as
  additional provider options; they are out of scope for this decision.
- OTP is a provider credential only; it never becomes the Kisab
  `accountId`. Reuse the existing `AuthProvider.EMAIL` (new enum value) →
  `AccountApi` → `OnlineAccountService.establish` path unmodified in its
  rollback and linking behavior.

### Backend exchange (client contract addition)
Extends `AccountApi` (still no real backend in-repo; contract + fake only):

1. `requestEmailOtp(email)` → `EmailOtpRequested { requestId, expiresAtEpochMillis }`
   - Server sends the 6-digit code by email; it is never returned to the client.
   - Response carries a server-gated `requestId` used for verification.
2. `verifyEmailOtpAndEstablish(requestId, otp, localUserId?)` →
   `EstablishAccountResponse` (exact current response shape: `accountId`,
   `sessionId`, `accessToken`, optional `refreshToken`).
   - All existing error kinds apply; add `OTP_EXPIRED` and `OTP_RATE_LIMITED`
     to `AccountApiFailureKind` so the UI can differentiate expiry/cooldown
     from `INVALID_CREDENTIAL`.

### Lifecycle rules (must be implemented with the feature)
- Code + request: valid for 10 minutes from issuance; single-use.
- Resend cooldown: 60 seconds per email; button hidden/disabled until then.
- Attempt limit: 5 per request; on the 5th failed attempt the request is
  invalidated server-side and the user restarts (`requestEmailOtp`).
- Client state: OTP values never persist; the active `requestId`/tokens live
  only in memory during the verification dialog and in keystore-backed
  `KisabSessionStorageAdapter` afterwards.
- Session recovery: refresh token (when present) restores a session through
  the existing storage path; no re-verification needed until expiry.
- Sign-out: clears the `KisabSession` from secure storage. The non-secret
  `AccountLink` and all LocalUser/farm data remain; sign-out is re-linkable
  and non-destructive by design.
- Data safety: access/refresh tokens never written into farm exports or
  backups; OTP request/verification never touches FarmStore.

### Account linking conflicts
Use the existing contract behavior verbatim: different `accountId` for an
already-linked LocalUser → fail with no session write; rollback on late
link failure clears the session.

## Consequences
- Feature can be implemented and tested entirely against `FakeAccountApi`
  plus a fake email/OtpVerifier; no real backend is required for the app to
  ship the flow, and the real backend can later implement the same contract.
- UI additions: email field + verification dialog (code entry, resend,
  attempts counter, expiry countdown), account-section status wiring.
- New `OTP_EXPIRED` / `OTP_RATE_LIMITED` error kinds must be handled in the
  failure surface used by the UI.
- Follows ADR-0003: implementation only after this decision is committed;
  validation depth stays bounded (targeted checks + device acceptance) unless
  maintainer approves broader validation.

## Out of scope (unchanged)
Google/Apple SDK UI, farm upload/download/merge, premium/ads/push/cloud
storage, full sign-out product UX, password or magic-link email auth.