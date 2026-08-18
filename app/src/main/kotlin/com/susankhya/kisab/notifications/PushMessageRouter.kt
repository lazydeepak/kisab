package com.susankhya.kisab.notifications

/**
 * Thin router from transport payloads to [NotificationCoordinator].
 * No farm/account mutations.
 */
class PushMessageRouter(
    private val coordinator: NotificationCoordinator
) {
    fun onDataMessage(data: Map<String, String>): Boolean {
        val message = IncomingPushMessage.parse(data) ?: return false
        return coordinator.present(message)
    }
}
