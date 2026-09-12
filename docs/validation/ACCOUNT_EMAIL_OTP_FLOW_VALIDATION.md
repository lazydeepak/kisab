# Email OTP Account Flow Validation — 2026-09-12

Branch: feat/email-otp-account-flow (PR #64)
Device: Moto Edge 60 Fusion ZA22374XPC (installed debug APK from assembleDebug)
Evidence: /tmp/account_signin.png (settings Account section; button visible, light mode)

## What was validated
- Contract layer (AccountApi + EmailOtpFlow + FakeAccountApi) — 638 unit tests PASS, verifyLocal BUILD SUCCESSFUL.
- Debug build demo path: FakeAccountApi.demo() → requestEmailOtp("any") with code 123456 → verify → OnlineAccountService.persistEstablishment → CONNECTED state rendered.
- UI wiring: settingsAccountEmailSignInButton visible when LOCAL_ONLY; email dialog (EditText + Continue) opens; OTP dialog opens with message, code field, resend (60 s cooldown shown), submit (Sign in), error texts (Nepali/English).
- Resend countdown updates; wrongful/expired/rate-limited codes surface the correct localized error via flowErrorStringRes.
- No secrets in UI copy (requestId / tokens never shown); OTP never persisted in EmailOtpFlow state.

## Not fully validated (need maintainer / user sign-off)
- Full end-to-end device sign-in (email dialog → OTP correct code → CONNECTED) — smoke launched; full capture of the success state not yet recorded (dialog dismiss + section update).
- Nepali translation accuracy (human bilingual review still open per ADR-0004); warning vs critical title share same Nepali text (pre-existing).
- Date/number formatting with presentationLocale / deviceZone on Nepali OTP message (placeholder %1$s only; date not present).
- Release build (unavailable service placeholder) — not device-tested; covered by contract/test.

## Acceptance
- Maintainer (user) approval needed for PR #64 merge.
- After merge: record final success-state screenshot, close ADR-0004 implementation workstream, move any open bilingual review to backlog as follow-up F1/F4.
