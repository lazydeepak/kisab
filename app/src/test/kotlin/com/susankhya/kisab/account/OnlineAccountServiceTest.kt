package com.susankhya.kisab.account

import com.susankhya.foundation.session.InMemorySessionStorage
import com.susankhya.foundation.session.SessionStorage
import com.susankhya.foundation.session.StoredSession
import com.susankhya.kisab.domain.AccountLink
import com.susankhya.kisab.domain.AccountLinkService
import com.susankhya.kisab.domain.AccountLinkStore
import com.susankhya.kisab.domain.FarmSliceService
import com.susankhya.kisab.domain.FarmTransactionDraft
import com.susankhya.kisab.domain.InMemoryAccountLinkStore
import com.susankhya.kisab.domain.InMemoryLocalUserStore
import com.susankhya.kisab.domain.LocalUserService
import com.susankhya.kisab.domain.TransactionCategory
import com.susankhya.kisab.domain.TransactionType
import com.susankhya.kisab.persistence.InMemoryMultiFarmBackend
import com.susankhya.kisab.persistence.MultiFarmStore
import com.susankhya.kisab.session.KisabSessionStorageAdapter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for [OnlineAccountService] covering the full account establishment
 * lifecycle: success, idempotent re-linking, conflict rejection, and
 * rollback semantics when session or link persistence fails.
 *
 * Each test verifies that farm data and LocalUser identity are never
 * mutated by account operations, and that session/link state is
 * consistent after every outcome.
 */
class OnlineAccountServiceTest {

    private lateinit var users: LocalUserService
    private lateinit var farms: FarmSliceService
    private lateinit var linkStore: InMemoryAccountLinkStore
    private lateinit var links: AccountLinkService
    private lateinit var sessionStorage: KisabSessionStorageAdapter
    private lateinit var foundationStorage: InMemorySessionStorage

    private val credential = ProviderCredential(AuthProvider.GOOGLE, "fake-google-assertion")

    @Before
    fun setUp() {
        users = LocalUserService(
            InMemoryLocalUserStore(),
            clock = { 1L },
            idGenerator = { "user-online-test" }
        )
        farms = FarmSliceService(MultiFarmStore(InMemoryMultiFarmBackend()))
        linkStore = InMemoryAccountLinkStore()
        links = AccountLinkService(linkStore, clock = { 10L })
        foundationStorage = InMemorySessionStorage()
        sessionStorage = KisabSessionStorageAdapter(foundationStorage)
    }

    private fun service(api: AccountApi) = OnlineAccountService(api, sessionStorage, links)

    /** Verifies session + link are persisted and farms/LocalUser are untouched. */
    @Test
    fun successStoresSessionAndCreatesAccountLinkWithoutTouchingFarms() = runTest {
        val user = users.ensureLocalUser()
        val farm = farms.createFarm("A", "NPR")
        users.associateFarm(farm.id)
        farms.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.INCOME,
                category = TransactionCategory.SALES,
                amountMinor = 500L,
                description = "keep",
                occurredAt = "2024-01-01T00:00:00Z"
            )
        )
        val farmIdsBefore = farms.farmIds()
        val ownedBefore = users.ownedFarmIds()
        val userIdBefore = user.userId

        val result = service(FakeAccountApi.success(accountId = "account-ok-1")).establish(
            localUserId = user.userId,
            credential = credential
        )

        assertTrue(result is OnlineAccountResult.Success)
        val success = result as OnlineAccountResult.Success
        assertEquals("account-ok-1", success.accountId)
        assertEquals("account-ok-1", success.link.accountId)
        assertEquals(userIdBefore, success.link.localUserId)

        val session = sessionStorage.read()
        assertNotNull(session)
        assertEquals("session-test-1", session!!.sessionId)
        assertEquals("access-test-1", session.accessToken)
        assertEquals("refresh-test-1", session.refreshToken)

        assertEquals(userIdBefore, users.currentUser()?.userId)
        assertEquals(farmIdsBefore, farms.farmIds())
        assertEquals(ownedBefore, users.ownedFarmIds())
        assertEquals(500L, farms.loadFarm(farm.id)!!.transactions.first().amountMinor)
        assertTrue(links.isLinked(user.userId))
    }

    /** Re-linking the same account is idempotent and refreshes the session. */
    @Test
    fun sameAccountLinkIsIdempotentAndRefreshesSession() = runTest {
        val user = users.ensureLocalUser()
        val first = service(FakeAccountApi.success(accountId = "account-same")).establish(
            user.userId,
            credential
        ) as OnlineAccountResult.Success
        val linkedAt = first.link.linkedAtMillis

        val secondApi = FakeAccountApi.success(
            accountId = "account-same",
            sessionId = "session-test-2",
            accessToken = "access-test-2",
            refreshToken = "refresh-test-2"
        )
        val second = service(secondApi).establish(user.userId, credential)
        assertTrue(second is OnlineAccountResult.Success)
        val s2 = second as OnlineAccountResult.Success
        assertEquals("account-same", s2.accountId)
        assertEquals(linkedAt, s2.link.linkedAtMillis)

        val session = sessionStorage.read()!!
        assertEquals("session-test-2", session.sessionId)
        assertEquals("access-test-2", session.accessToken)
    }

    /** Linking a different account rejects without corrupting session or farms. */
    @Test
    fun differentAccountConflictDoesNotCorruptSessionOrFarms() = runTest {
        val user = users.ensureLocalUser()
        val farm = farms.createFarm("Keep")
        users.associateFarm(farm.id)
        service(FakeAccountApi.success(accountId = "account-one")).establish(user.userId, credential)
        val sessionAfterFirst = sessionStorage.read()!!

        val result = service(FakeAccountApi.success(accountId = "account-two")).establish(
            user.userId,
            credential
        )
        assertTrue(result is OnlineAccountResult.Failure)
        val failure = result as OnlineAccountResult.Failure
        assertEquals(OnlineAccountFailureReason.ACCOUNT_LINK_CONFLICT, failure.reason)

        assertEquals("account-one", (links.linkState(user.userId) as AccountLink.Linked).accountId)
        // Pre-check rejects before session overwrite — original session remains.
        assertEquals(sessionAfterFirst.sessionId, sessionStorage.read()?.sessionId)
        assertEquals(farm.id, farms.loadFarm(farm.id)?.id)
    }

    /** Transport failures leave no link or session behind. */
    @Test
    fun backendTransportFailureCreatesNoLinkOrSession() = runTest {
        val user = users.ensureLocalUser()
        val farm = farms.createFarm("X")
        users.associateFarm(farm.id)

        val result = service(FakeAccountApi.failing(AccountApiFailureKind.TRANSPORT)).establish(
            user.userId,
            credential
        )
        assertEquals(
            OnlineAccountFailureReason.TRANSPORT,
            (result as OnlineAccountResult.Failure).reason
        )
        assertFalse(links.isLinked(user.userId))
        assertNull(sessionStorage.read())
        assertEquals(farm.id, farms.loadFarm(farm.id)?.id)
    }

    /** Invalid credential failures leave no link or session behind. */
    @Test
    fun invalidCredentialFailureCreatesNoLinkOrSession() = runTest {
        val user = users.ensureLocalUser()
        val result = service(
            FakeAccountApi.failing(AccountApiFailureKind.INVALID_CREDENTIAL, "bad credential")
        ).establish(user.userId, credential)
        assertEquals(
            OnlineAccountFailureReason.INVALID_CREDENTIAL,
            (result as OnlineAccountResult.Failure).reason
        )
        assertNull(sessionStorage.read())
        assertFalse(links.isLinked(user.userId))
    }

    /** Session write failure prevents AccountLink from being created. */
    @Test
    fun secureSessionWriteFailureCreatesNoAccountLink() = runTest {
        val user = users.ensureLocalUser()
        val failingSessions = KisabSessionStorageAdapter(FailingSaveSessionStorage())
        val svc = OnlineAccountService(
            FakeAccountApi.success(),
            failingSessions,
            links
        )
        val result = svc.establish(user.userId, credential)
        assertEquals(
            OnlineAccountFailureReason.SESSION_PERSISTENCE_FAILED,
            (result as OnlineAccountResult.Failure).reason
        )
        assertFalse(links.isLinked(user.userId))
        assertNull(failingSessions.read())
    }

    /** Account link write failure rolls back the session written earlier. */
    @Test
    fun accountLinkPersistenceFailureRollsBackSession() = runTest {
        val user = users.ensureLocalUser()
        val flakyLinks = AccountLinkService(FlakySaveAccountLinkStore(), clock = { 1L })
        val svc = OnlineAccountService(
            FakeAccountApi.success(accountId = "account-rollback"),
            sessionStorage,
            flakyLinks
        )
        val result = svc.establish(user.userId, credential)
        assertEquals(
            OnlineAccountFailureReason.ACCOUNT_LINK_PERSISTENCE_FAILED,
            (result as OnlineAccountResult.Failure).reason
        )
        assertNull(sessionStorage.read())
        assertFalse(flakyLinks.isLinked(user.userId))
    }

    /** Session and link data survive recreation of service instances. */
    @Test
    fun sessionAndLinkSurviveIndependentStoreRecreation() = runTest {
        val user = users.ensureLocalUser()
        service(FakeAccountApi.success(accountId = "account-recreate")).establish(
            user.userId,
            credential
        )

        val sessionAgain = KisabSessionStorageAdapter(foundationStorage)
        val linksAgain = AccountLinkService(linkStore, clock = { 99L })
        assertEquals("session-test-1", sessionAgain.read()?.sessionId)
        assertEquals("account-recreate", linksAgain.linkState(user.userId).accountIdOrNull)
    }

    /** Clearing session does not affect link or farm data. */
    @Test
    fun clearingSessionLeavesLinkAndFarmsIntact() = runTest {
        val user = users.ensureLocalUser()
        val farm = farms.createFarm("Stay")
        users.associateFarm(farm.id)
        service(FakeAccountApi.success(accountId = "account-stay")).establish(user.userId, credential)

        sessionStorage.clear()
        assertNull(sessionStorage.read())
        assertTrue(links.isLinked(user.userId))
        assertEquals("account-stay", links.linkState(user.userId).accountIdOrNull)
        assertEquals(farm.id, farms.loadFarm(farm.id)?.id)
        assertEquals(user.userId, users.currentUser()?.userId)
    }

    /** Server rejection leaves no link or session behind. */
    @Test
    fun serverRejectedCreatesNoLinkOrSession() = runTest {
        val user = users.ensureLocalUser()
        val result = service(FakeAccountApi.failing(AccountApiFailureKind.SERVER_REJECTED))
            .establish(user.userId, credential)
        assertEquals(
            OnlineAccountFailureReason.SERVER_REJECTED,
            (result as OnlineAccountResult.Failure).reason
        )
        assertNull(sessionStorage.read())
        assertFalse(links.isLinked(user.userId))
    }

    /** Email OTP exchange + [OnlineAccountService.persistEstablishment] completes the sign-in. */
    @Test
    fun emailOtpExchangePersistsSessionAndLinkWithoutTouchingFarms() = runTest {
        val user = users.ensureLocalUser()
        val farm = farms.createFarm("Email Farm", "NPR")
        users.associateFarm(farm.id)
        val farmIdsBefore = farms.farmIds()

        val api = FakeAccountApi(
            handler = { throw AssertionError("no establishAccount expected on the OTP path") }
        )
        val requested = api.requestEmailOtp("farmer@example.com")
        val response = api.verifyEmailOtpAndEstablish(requested.requestId, "123456", user.userId)

        val result = service(api).persistEstablishment(user.userId, response)
        assertTrue(result is OnlineAccountResult.Success)
        val success = result as OnlineAccountResult.Success
        assertEquals("account-farmer@example.com", success.accountId)

        val session = sessionStorage.read()
        assertNotNull(session)
        assertTrue(session!!.sessionId.startsWith("session-otp-"))
        assertTrue(links.isLinked(user.userId))
        assertEquals("account-farmer@example.com", links.linkState(user.userId).accountIdOrNull)
        assertEquals(farmIdsBefore, farms.farmIds())
        assertEquals(user.userId, users.currentUser()?.userId)
    }

    /** OTP link conflict rejects exactly like provider credential linking. */
    @Test
    fun emailOtpConflictRejectsWithoutCorruptingSessionOrFarms() = runTest {
        val user = users.ensureLocalUser()
        service(FakeAccountApi.success(accountId = "account-one")).establish(user.userId, credential)

        val api = FakeAccountApi(
            handler = { throw AssertionError("no establishAccount expected on the OTP path") }
        )
        val requested = api.requestEmailOtp("other@example.com")
        val response = api.verifyEmailOtpAndEstablish(requested.requestId, "123456", user.userId)

        val result = service(api).persistEstablishment(user.userId, response)
        assertEquals(
            OnlineAccountFailureReason.ACCOUNT_LINK_CONFLICT,
            (result as OnlineAccountResult.Failure).reason
        )
        assertEquals("account-one", links.linkState(user.userId).accountIdOrNull)
        assertNotNull(sessionStorage.read())
    }

    /** Session storage that fails on save; read/clear succeed. */
    private class FailingSaveSessionStorage : SessionStorage {
        override suspend fun save(session: StoredSession) {
            throw IllegalStateException("keystore unavailable")
        }

        override suspend fun read(): StoredSession? = null

        override suspend fun clear() = Unit
    }

    /** AccountLinkStore that fails on save. */
    private class FlakySaveAccountLinkStore : AccountLinkStore {
        private val inner = InMemoryAccountLinkStore()
        override fun load(localUserId: String): AccountLink? = inner.load(localUserId)
        override fun save(link: AccountLink) {
            throw IllegalStateException("prefs commit failed")
        }
        override fun clear(localUserId: String) = inner.clear(localUserId)
    }
}
