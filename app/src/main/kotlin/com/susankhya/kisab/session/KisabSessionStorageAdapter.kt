package com.susankhya.kisab.session

import com.susankhya.foundation.session.SessionStorage
import com.susankhya.foundation.session.StoredSession

/**
 * Adapter between [KisabSession] and the foundation [SessionStorage].
 *
 * Serializes [KisabSession] to a `key=value;key=value` payload string
 * stored as a [StoredSession] with id `kisab-session`. Deserializes
 * on read. Never exposes tokens outside this adapter boundary.
 *
 * Used by [OnlineAccountService] to persist session material
 * after successful account establishment, and by tests to verify
 * session state without involving the real keystore.
 */
class KisabSessionStorageAdapter(
    private val storage: SessionStorage
) {
    /**
     * Persists [session] to the foundation keystore.
     * Encodes sessionId, accessToken, and refreshToken as a payload string.
     */
    suspend fun save(session: KisabSession) {
        storage.save(
            StoredSession(
                id = SESSION_ID,
                payload = session.toPayload()
            )
        )
    }

    /**
     * Reads and deserializes a [KisabSession] from the foundation keystore,
     * or returns null if no session exists.
     */
    suspend fun read(): KisabSession? = storage.read()?.payload?.toKisabSession()

    /**
     * Clears any persisted session from the foundation keystore.
     * Used during account linking rollback and sign-out.
     */
    suspend fun clear() {
        storage.clear()
    }
}

/**
 * On-device session holding a session id and optional access/refresh tokens.
 *
 * Session tokens are short-lived credentials for authenticated API calls.
 * They are stored only in the foundation keystore via [KisabSessionStorageAdapter]
 * and never appear in farm backup payloads or account link metadata.
 *
 * @property sessionId the server-issued session identifier
 * @property accessToken short-lived bearer token; null until account setup
 * @property refreshToken long-lived token for refreshing [accessToken]; null if not issued
 */
data class KisabSession(
    val sessionId: String,
    val accessToken: String? = null,
    val refreshToken: String? = null
)

private const val SESSION_ID = "kisab-session"
private const val SESSION_ID_KEY = "sessionId"
private const val ACCESS_TOKEN_KEY = "accessToken"
private const val REFRESH_TOKEN_KEY = "refreshToken"

/** Serializes [KisabSession] to a `key=value;key=value` payload string. */
private fun KisabSession.toPayload(): String = listOfNotNull(
    "$SESSION_ID_KEY=$sessionId",
    accessToken?.let { "$ACCESS_TOKEN_KEY=$it" },
    refreshToken?.let { "$REFRESH_TOKEN_KEY=$it" }
).joinToString(";")

/** Deserializes a payload string back to [KisabSession], or null if invalid. */
private fun String.toKisabSession(): KisabSession? {
    val values = mutableMapOf<String, String>()
    for (part in split(";")) {
        if (part.isEmpty()) continue
        val (key, value) = part.split("=", limit = 2)
        values[key] = value
    }

    val sessionId = values[SESSION_ID_KEY] ?: return null
    return KisabSession(
        sessionId = sessionId,
        accessToken = values[ACCESS_TOKEN_KEY],
        refreshToken = values[REFRESH_TOKEN_KEY]
    )
}
