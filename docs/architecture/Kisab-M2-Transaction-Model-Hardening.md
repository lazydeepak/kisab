# Kisab M2 Transaction Model Hardening

## Status
Kisab M2 is complete. It hardens the transaction model introduced in M1 without moving farm or transaction semantics into the foundation.

## Scope
Kisab M2 covers:
- stable transaction identifiers;
- explicit income and expense transaction types;
- governed categories constrained by transaction type;
- minor-unit money storage with explicit ISO currency codes and currency-safe summaries;
- explicit transaction timestamps and deterministic display rules;
- newest-first transaction history ordered by transaction timestamp;
- validation for transaction descriptions, amounts, currency, category, dates, farm existence, and transaction existence;
- create, edit, and delete flows with destructive-action confirmation in the Android UI;
- versioned persistence migration that preserves existing local farm data.

## Architecture choices
- The product app remains the owner of farm and transaction semantics.
- The domain service validates transactions, computes summaries, and centralizes the M2 transaction invariants.
- The persistence layer uses a versioned payload format so older M1 transaction data can be migrated into the richer M2 model without data loss.
- The UI stays presentation-oriented and focuses on collecting valid input, surfacing validation errors, and reflecting updated summaries after successful mutations.

## Implementation notes
- Transaction mutations are deterministic and validated before they are persisted.
- Summary calculations are derived from the current persisted transaction set and intentionally reject mixed-currency balances.
- Transaction history is presented newest-first using the transaction timestamp (`occurredAt`) as the primary descending key. When two transactions share the same timestamp, the one recorded later in the ledger is presented first; this tie-breaker is stable across persistence, migration, backup restore, and activity recreation because it is derived from the existing stored ledger order rather than newly assigned data.
- The persistence codec is version-aware so new fields can be introduced without breaking earlier local farms.
