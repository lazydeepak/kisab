# Kisab M1 Offline Farm Management

## Status
Kisab M1 is complete. It extends the M0 product slice into a launchable Android experience that can create, reopen, and update one locally stored farm without introducing farm-domain semantics into the foundation.

## Scope
Kisab M1 covers:
- a launcher activity for the Android application;
- a simple UI for creating a farm, adding livestock or crop entries, recording signed transactions, and viewing a summary;
- product-owned offline persistence in the app layer using shared preferences;
- deterministic domain operations that can be exercised in unit tests;
- Android integration coverage for persistence and recreation.

## Architecture choices
- The domain service remains in the product app and owns farm semantics.
- Persistence is implemented in `app/src/main/kotlin/com/susankhya/kisab/persistence/` and uses local shared preferences for offline storage.
- The presentation layer is implemented in `app/src/main/kotlin/com/susankhya/kisab/ui/` and is kept separate from domain operations.
- The existing foundation session integration remains intact through `KisabSessionApp` and the session storage adapter.

## Implementation notes
- Farm state is stored as a single persisted farm payload keyed by the current farm id.
- The domain service validates input and exposes summary calculations for deterministic automated testing.
- The app can survive process recreation because the persisted farm is reloaded from local storage on launch.
