# Kisab M3 Offline Farm Backup & Restore

## Status
Kisab M3 is implemented. It adds a fully offline backup/restore flow for a single farm using Android's Storage Access Framework and a versioned backup envelope that carries only farm-domain state.

## Scope
Kisab M3 covers:
- single-farm export and restore;
- a versioned backup envelope with schema version, deterministic UTC export time, and the current farm payload;
- Android document-picker-based backup file selection and creation without filesystem permissions;
- full validation before any state replacement;
- overwrite confirmation and atomic/synchronous persistence of successful restores.

## Architecture choices
- The farm model and transaction invariants remain product-owned and unchanged.
- Backup files carry only the current farm state; they never include session credentials, device keys, preferences, or unrelated application state.
- The file-access boundary is kept thin and Android-specific so the codec and domain logic remain JVM-testable.
- Backup import validates the entire envelope and farm snapshot before current state is replaced.
