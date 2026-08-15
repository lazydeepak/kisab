package com.susankhya.kisab.account

import com.susankhya.kisab.domain.AccountLink
import com.susankhya.kisab.domain.AccountLinkConflictException
import com.susankhya.kisab.domain.AccountLinkService
import com.susankhya.kisab.session.KisabSession
import com.susankhya.kisab.session.KisabSessionStorageAdapter

/**
 * Provider-neutral orchestration:
 * provider credential → [AccountApi] → secure [KisabSession] → [AccountLinkService].
 *
 * Does not mutate farms, LocalUser ids, or upload/download farm data.
 *
 * ## Ordering
 * 1. Backend [AccountApi.establishAccount]
 * 2. Validate response fields
 * 3. Pre-check AccountLink conflict (different account → fail, no session write)
 * 4. Persist session via secure foundation storage
 * 5. Persist AccountLink
 * 6. If step 5 fails after step 4, clear the session just written and fail
 *
 * No secrets are logged.
 */
class OnlineAccountService(
    private val accountApi: AccountApi,
    private val sessionStorage: KisabSessionStorageAdapter,
    private val accountLinkService: AccountLinkService
) {

    /**
     * Exchanges [credential] for a Kisab account + session and links [localUserId].
     * Fully offline-safe when not called; failures leave farms/LocalUser intact.
     */
    suspend fun establish(
        localUserId: String,
        credential: ProviderCredential
    ): OnlineAccountResult {
        require(localUserId.isNotBlank()) { "localUserId is required" }

        val response = try {
            accountApi.establishAccount(
                EstablishAccountRequest(
                    credential = credential,
                    localUserId = localUserId
                )
            )
        } catch (e: AccountApiException) {
            return OnlineAccountResult.Failure(
                reason = e.kind.toOnlineReason(),
                detail = e.message
            )
        } catch (_: Exception) {
            return OnlineAccountResult.Failure(
                reason = OnlineAccountFailureReason.TRANSPORT,
                detail = "unexpected transport failure"
            )
        }

        val validated = try {
            validateResponse(response)
        } catch (e: IllegalArgumentException) {
            return OnlineAccountResult.Failure(
                reason = OnlineAccountFailureReason.INVALID_RESPONSE,
                detail = e.message
            )
        }

        // Conflict pre-check: do not write session if link would be rejected.
        val existing = accountLinkService.linkState(localUserId)
        if (existing is AccountLink.Linked && existing.accountId != validated.accountId) {
            return OnlineAccountResult.Failure(
                reason = OnlineAccountFailureReason.ACCOUNT_LINK_CONFLICT,
                detail = "already linked to a different Kisab account"
            )
        }

        val session = KisabSession(
            sessionId = validated.sessionId,
            accessToken = validated.accessToken,
            refreshToken = validated.refreshToken
        )

        try {
            sessionStorage.save(session)
        } catch (_: Exception) {
            return OnlineAccountResult.Failure(
                reason = OnlineAccountFailureReason.SESSION_PERSISTENCE_FAILED,
                detail = "secure session write failed"
            )
        }

        val linked = try {
            accountLinkService.linkToAccount(localUserId, validated.accountId)
        } catch (e: AccountLinkConflictException) {
            // Race / unexpected: roll back session
            runCatching { sessionStorage.clear() }
            return OnlineAccountResult.Failure(
                reason = OnlineAccountFailureReason.ACCOUNT_LINK_CONFLICT,
                detail = "already linked to a different Kisab account"
            )
        } catch (_: Exception) {
            runCatching { sessionStorage.clear() }
            return OnlineAccountResult.Failure(
                reason = OnlineAccountFailureReason.ACCOUNT_LINK_PERSISTENCE_FAILED,
                detail = "account link write failed; session rolled back"
            )
        }

        return OnlineAccountResult.Success(
            accountId = linked.accountId,
            link = linked
        )
    }

    private fun validateResponse(response: EstablishAccountResponse): EstablishAccountResponse {
        // Data-class init already checks blanks; re-check trim.
        val accountId = response.accountId.trim()
        val sessionId = response.sessionId.trim()
        val access = response.accessToken.trim()
        require(accountId.isNotEmpty()) { "accountId blank after trim" }
        require(sessionId.isNotEmpty()) { "sessionId blank after trim" }
        require(access.isNotEmpty()) { "accessToken blank after trim" }
        return response.copy(
            accountId = accountId,
            sessionId = sessionId,
            accessToken = access,
            refreshToken = response.refreshToken?.trim()?.takeIf { it.isNotEmpty() }
        )
    }

    private fun AccountApiFailureKind.toOnlineReason(): OnlineAccountFailureReason = when (this) {
        AccountApiFailureKind.TRANSPORT -> OnlineAccountFailureReason.TRANSPORT
        AccountApiFailureKind.INVALID_CREDENTIAL -> OnlineAccountFailureReason.INVALID_CREDENTIAL
        AccountApiFailureKind.SERVER_REJECTED -> OnlineAccountFailureReason.SERVER_REJECTED
        AccountApiFailureKind.INVALID_RESPONSE -> OnlineAccountFailureReason.INVALID_RESPONSE
    }
}
