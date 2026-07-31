package com.susankhya.kisab.session

import com.susankhya.foundation.session.SessionStorage
import com.susankhya.foundation.session.StoredSession

class KisabSessionStorageAdapter(
    private val storage: SessionStorage
) {
    suspend fun save(session: KisabSession) {
        storage.save(
            StoredSession(
                id = SESSION_ID,
                payload = session.toPayload()
            )
        )
    }

    suspend fun read(): KisabSession? = storage.read()?.payload?.toKisabSession()

    suspend fun clear() {
        storage.clear()
    }
}

data class KisabSession(
    val sessionId: String,
    val accessToken: String? = null,
    val refreshToken: String? = null
)

private const val SESSION_ID = "kisab-session"
private const val SESSION_ID_KEY = "sessionId"
private const val ACCESS_TOKEN_KEY = "accessToken"
private const val REFRESH_TOKEN_KEY = "refreshToken"

private fun KisabSession.toPayload(): String = listOfNotNull(
    "$SESSION_ID_KEY=$sessionId",
    accessToken?.let { "$ACCESS_TOKEN_KEY=$it" },
    refreshToken?.let { "$REFRESH_TOKEN_KEY=$it" }
).joinToString(";")

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
