# ADR-0002: Post-v0.2.0 Multiplatform Direction

## Status
Accepted, frozen

## Context
Kisab is currently an offline-first Android product. M0 through M6.4 establish the local farm, party, trade, settlement, khata, overview, and calculator workflows that make the Android app useful before any shared backend or additional client exists.

The product direction is to eventually support multiple clients, shared data, and synchronization. That direction is approved for later planning, but implementing it now would distract from the next higher-value work: finishing `v0.2.0`, then improving Android features, UI/UX, Nepali-first usability, data-entry speed, and field readiness.

## Decision
Kisab will eventually become a multi-client product with:

- Android;
- Web;
- iPhone;
- iPad;
- macOS;
- Windows.

All clients will access one authoritative backend through APIs. No client will access the authoritative database directly.

Offline-capable clients, including Android and any future suitable Apple or Windows clients, will keep a local store and synchronize with the backend. The backend remains the authoritative shared system of record, while local stores support low-connectivity use and fast field workflows.

ServBay is the approved initial local development environment for the first backend and web prototype. It is not a production hosting decision.

Future client, backend, web, sync, and contract surfaces will have component-specific CI/CD once those components exist. This ADR does not create those components or pipelines.

## Frozen work
Do not implement yet:

- shared backend;
- authoritative central database;
- API layer;
- sync engine;
- web application;
- ServBay backend/web setup;
- iPhone, iPad, macOS, or Windows clients;
- shared client SDKs;
- component-specific CI/CD for future backend, web, sync, or additional clients.

## Future contract scope
Before implementation starts, define platform-neutral contracts for:

- IDs;
- timestamps;
- revisions and versioning;
- deletion and tombstones;
- conflict detection and resolution;
- authentication and authorization;
- validation rules;
- money and currency;
- farms;
- parties;
- transactions;
- trades;
- settlements.

These contracts must be explicit enough for offline clients to sync safely without duplicate writes, lost deletes, silent conflict overwrites, or incompatible migrations.

## Reactivation condition
Reactivate this direction only after `v0.2.0` is complete and the maintainer explicitly chooses to shift focus from Android feature/UI/UX work to shared backend and multiplatform work.

Before reactivation, create a fresh milestone or implementation plan that:

- preserves the existing Android data model and user workflows unless a migration is explicitly justified;
- defines the API and sync contracts before client implementation;
- records local development setup, including ServBay;
- defines verification gates for backend, web, Android sync, and migration safety.

## First proof target
The first implementation proof is deliberately small:

```text
local backend + ServBay + web vertical slice + Android sync
```

The proof is successful when the same farm, party, trade, and settlement data can be viewed on Android and Web, edited through one surface, synchronized to the other, and verified against clear conflict/deletion/retry behavior for the supported slice.

## Current priority
After `v0.2.0`, focus stays on Android product work:

- farmer workflow coverage;
- UI/UX simplification;
- Nepali-first usability;
- faster data entry;
- clearer Hisab, Khata, receivable, and payable presentation;
- reporting and useful financial insights;
- field/device validation.

This ADR records the future architecture direction so agents can retrieve it later. It does not authorize implementation now.

## Consequences
- The multiplatform direction is durable and easy to find.
- Android feature and usability work remains the active product priority after `v0.2.0`.
- Future backend, web, sync, and additional-client work starts from an accepted boundary instead of a blank slate.
- Sync is treated as the main architectural risk and must be proven before broad client expansion.
