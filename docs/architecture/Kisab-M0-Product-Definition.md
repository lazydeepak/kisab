# Kisab M0 Product Definition

## Status
Kisab M0 is complete. It established the product-owned farm-domain boundary and the initial vertical slice that M1 builds on.

## Scope
Kisab M0 defines the first product-owned slice that can be built and validated without expanding the foundation.

## Primary user and journey
- Primary user: a farm operator managing a single farm.
- Journey: create a farm, add a livestock or crop entry, record one transaction, and view a summary.

## V1 domain boundary
Kisab M0 covers:
- farm identity and basic farm metadata;
- one inventory or livestock/crop entry type;
- a simple transaction ledger with a net balance;
- local state that can be persisted offline.

Kisab M0 does not cover:
- multi-farm administration;
- advanced reporting or analytics;
- complex accounting rules;
- cross-device sync or multi-user collaboration.

## Offline-first data strategy
- Persist farm state locally in the app.
- Keep write operations simple and deterministic.
- Treat local persistence as the source of truth for the first slice.
- Keep foundation usage limited to session and app-state persistence that is not domain-specific.

## Product-owned architecture decisions
- The farm domain remains in the product app.
- The app may use the foundation for session persistence and platform integration only.
- Product UI and domain logic are owned by Kisab, not the foundation.

## First vertical slice
The first slice is a small domain service that supports:
- creating a farm;
- adding an entry;
- recording a transaction;
- generating a summary with the current balance.
