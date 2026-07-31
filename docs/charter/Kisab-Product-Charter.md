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

## Product boundary
Kisab owns farm concepts, user journeys, offline persistence strategy, and product-specific presentation. It reuses the shared foundation only for technical concerns such as secure session persistence.

## Foundation reuse
Kisab will use the published foundation artifact for app-session storage and platform integration. It will not request the foundation to own farm, crop, livestock, transaction, or accounting semantics.
