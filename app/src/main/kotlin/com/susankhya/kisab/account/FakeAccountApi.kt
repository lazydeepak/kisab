package com.susankhya.kisab.account

/**
 * In-memory [AccountApi] for unit tests. Never used for production traffic.
 * Does not log credentials.
 */
class FakeAccountApi(
    private val handler: suspend (EstablishAccountRequest) -> EstablishAccountResponse
) : AccountApi {

    var lastRequest: EstablishAccountRequest? = null
        private set

    override suspend fun establishAccount(request: EstablishAccountRequest): EstablishAccountResponse {
        lastRequest = request
        return handler(request)
    }

    companion object {
        fun success(
            accountId: String = "account-test-1",
            sessionId: String = "session-test-1",
            accessToken: String = "access-test-1",
            refreshToken: String? = "refresh-test-1"
        ): FakeAccountApi = FakeAccountApi {
            EstablishAccountResponse(
                accountId = accountId,
                sessionId = sessionId,
                accessToken = accessToken,
                refreshToken = refreshToken
            )
        }

        fun failing(kind: AccountApiFailureKind, message: String = kind.name): FakeAccountApi =
            FakeAccountApi { throw AccountApiException(kind, message) }
    }
}
