package com.susankhya.kisab.notifications

/**
 * App-owned push payload after untrusted map parsing.
 * Never treats payload fields as executable commands or URLs to open blindly.
 */
data class IncomingPushMessage(
    val type: NotificationType,
    val title: String,
    val body: String,
    /** Stable id for notification collapse; optional. */
    val collapseKey: String? = null
) {
    companion object {
        private const val MAX_TEXT = 200

        /**
         * Parses FCM data (or test maps). Returns null for unknown/malformed input.
         * Ignores action/url/command-like keys entirely.
         */
        fun parse(data: Map<String, String>): IncomingPushMessage? {
            val type = NotificationType.fromWire(data["type"] ?: data["notification_type"])
                ?: return null
            val title = sanitize(data["title"]).ifBlank { return null }
            val body = sanitize(data["body"] ?: data["message"]).ifBlank { return null }
            val collapse = data["collapse_key"]?.trim()?.takeIf { it.isNotEmpty() }?.take(64)
            return IncomingPushMessage(type = type, title = title, body = body, collapseKey = collapse)
        }

        private fun sanitize(raw: String?): String {
            if (raw == null) return ""
            return raw
                .replace('\u0000', ' ')
                .trim()
                .take(MAX_TEXT)
        }
    }
}
