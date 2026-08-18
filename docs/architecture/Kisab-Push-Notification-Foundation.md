# Kisab Push Notification Foundation

Client-side infrastructure for operational push notifications.
**No Firebase project config is committed** (`google-services.json` absent).

## What this milestone provides

| Piece | Role |
|-------|------|
| `POST_NOTIFICATIONS` | Declared; runtime request from Settings on API 33+ |
| Settings → Notifications | Shows On/Off; Enable / Open system settings |
| Channels | `kisab_updates`, `kisab_reminders` (IMPORTANCE_DEFAULT) |
| Preferences | App-local APP_UPDATES / BACKUP_REMINDERS toggles |
| `IncomingPushMessage` | Parses untrusted maps; unknown types ignored |
| `NotificationCoordinator` | Sole `NotificationManager` poster + deep links |
| `PushTokenStore` | Optional local FCM token file (not in backups) |

## What is still required for live FCM

1. Create a Firebase Android app for `com.susankhya.kisab`.
2. Add `app/google-services.json` **locally/CI secret** (do not commit secrets).
3. Apply Google Services Gradle plugin and `firebase-messaging` dependency.
4. Register a `FirebaseMessagingService` that:
   - `onNewToken` → `PushTokenStore.saveToken`
   - `onMessageReceived` → `PushMessageRouter.onDataMessage(remoteMessage.data)`
5. Later backend: register token + optional AccountLink account id + install id.

Until then, Kisab runs fully without Play services/FCM.

## Security

Push payloads must not reset/delete farms, import data, change currency,
run transactions, link accounts, or install APKs. Taps open normal UI only:

- `APP_UPDATE` → About / update-info surface  
- `BACKUP_REMINDER` → Settings → Data  
- `GENERAL_OPERATIONAL_NOTICE` → Settings → Notifications  

## Permission UX

- API &lt; 33: treated as granted (no runtime prompt).
- API 33+: request only from Settings → Enable notifications.
- Denial: app continues; no farm/backup/expiry impact.
