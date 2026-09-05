# Kisab Product Charter

## Purpose
Kisab is the first product-facing application in the Susankhya ecosystem to make farm operations manageable in low-connectivity environments. Its role is to help a farm operator record simple on-farm facts, transactions, and summaries without introducing product logic into the shared foundation.

## Non-goals for v1
- Build a full accounting engine.
- Support broad reporting, analytics, or multi-tenant administration.
- Introduce reusable foundation abstractions for farm operations.
- Replace the foundation with product-specific infrastructure.

## Primary user
A smallholder or farm operator who needs to record a farm, one or more livestock/crop entries, and a simple transaction history while offline or with intermittent connectivity.
## First journey

1. Create a farm.
2. Add an inventory or livestock/crop entry.
3. Record a transaction.
4. View the farm summary.

## Current scope and architecture

The initial journey above is the historical starting slice. Current Android code also supports farm activities/production, parties, sales/purchases, settlements, Khata, financial summaries, generic cash entry, calculators and farm backup/restore. This is a farm ledger, not a general accounting or multi-tenant administration platform.

- Kotlin Android UI and presentation coordinate product-owned domain services and projections.
- Local farm persistence and versioned migrations preserve offline records; backup validation happens before replacement. Farm backups exclude identity/session secrets.
- Party → Trade → Settlement facts drive balances and Khata; calculators are temporary projections, not a second accounting authority.
- Offline LocalUser identity, non-secret AccountLink metadata and secure foundation session storage are separate. Login is not required for farm work.
- HTTPS pilot updates verify APK digests before Android's installer enforces signer continuity. Push routing and account exchange have client foundations; live FCM/backend/provider integration remain deferred.

Current status and unfinished work live only in [CURRENT](../CURRENT.md); scoped milestone and validation records preserve history. The [README index](../../README.md) routes to architecture contracts and decisions. [ADR-0002](../decisions/ADR-0002-post-v0.2.0-multiplatform-direction.md) remains accepted but frozen; account client contracts do not establish a shared backend or sync implementation.

## Product boundary
Kisab owns farm concepts, user journeys, offline persistence strategy, and product-specific presentation. It reuses the shared foundation only for technical concerns such as secure session persistence.

## Foundation reuse
Kisab uses the published foundation artifact for app-session storage and platform integration. It will not request the foundation to own farm, crop, livestock, transaction, or accounting semantics.
