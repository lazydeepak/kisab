package com.susankhya.kisab.domain

import com.susankhya.kisab.persistence.FarmBackupCodec
import com.susankhya.kisab.persistence.FarmPersistenceCodec
import com.susankhya.kisab.persistence.InMemoryMultiFarmBackend
import com.susankhya.kisab.persistence.MultiFarmStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class AccountLinkServiceTest {

    private lateinit var linkStore: InMemoryAccountLinkStore
    private lateinit var links: AccountLinkService
    private lateinit var users: LocalUserService
    private lateinit var farms: FarmSliceService
    private var now = 2_000_000_000_000L

    @Before
    fun setUp() {
        linkStore = InMemoryAccountLinkStore()
        now = 2_000_000_000_000L
        links = AccountLinkService(linkStore, clock = { now })
        users = LocalUserService(
            InMemoryLocalUserStore(),
            clock = { now },
            idGenerator = { "user-link-test" }
        )
        farms = FarmSliceService(MultiFarmStore(InMemoryMultiFarmBackend()))
    }

    @Test
    fun defaultLocalUserIsUnlinked() {
        val user = users.ensureLocalUser()
        val state = links.linkState(user.userId)
        assertTrue(state is AccountLink.Unlinked)
        assertFalse(links.isLinked(user.userId))
        assertNull(state.accountIdOrNull)
    }

    @Test
    fun accountLinkStateSurvivesStoreRecreation() {
        val user = users.ensureLocalUser()
        links.linkToAccount(user.userId, "account-stable-1")
        val reloaded = AccountLinkService(linkStore, clock = { now + 99_000L })
        val state = reloaded.linkState(user.userId) as AccountLink.Linked
        assertEquals("account-stable-1", state.accountId)
        assertEquals(now, state.linkedAtMillis)
    }

    @Test
    fun linkingPreservesLocalUserIdAndAllFarmOwnership() {
        val user = users.ensureLocalUser()
        val a = farms.createFarm("A", "NPR")
        val b = farms.createFarm("B", "USD")
        users.associateFarm(a.id)
        users.associateFarm(b.id)
        val userIdBefore = user.userId
        val farmIdsBefore = farms.farmIds().toSet()
        val ownedBefore = users.ownedFarmIds()

        val linked = links.linkToAccount(userIdBefore, "account-xyz")

        assertEquals(userIdBefore, linked.localUserId)
        assertEquals(userIdBefore, users.currentUser()?.userId)
        assertEquals(farmIdsBefore, farms.farmIds().toSet())
        assertEquals(ownedBefore, users.ownedFarmIds())
        assertEquals("A", farms.loadFarm(a.id)?.name)
        assertEquals("B", farms.loadFarm(b.id)?.name)
    }

    @Test
    fun linkingSameAccountTwiceIsIdempotent() {
        val user = users.ensureLocalUser()
        val first = links.linkToAccount(user.userId, "account-same")
        now += 60_000L
        val second = links.linkToAccount(user.userId, "account-same")
        assertEquals(first, second)
        assertEquals(first.linkedAtMillis, second.linkedAtMillis)
    }

    @Test
    fun linkingDifferentAccountWhileLinkedIsRejected() {
        val user = users.ensureLocalUser()
        links.linkToAccount(user.userId, "account-one")
        try {
            links.linkToAccount(user.userId, "account-two")
            fail("expected conflict")
        } catch (e: AccountLinkConflictException) {
            assertEquals(user.userId, e.localUserId)
            assertEquals("account-one", e.existingAccountId)
            assertEquals("account-two", e.attemptedAccountId)
        }
        assertEquals("account-one", (links.linkState(user.userId) as AccountLink.Linked).accountId)
    }

    @Test
    fun noAccountLinkDoesNotAffectFarmOperations() {
        val user = users.ensureLocalUser()
        assertFalse(links.isLinked(user.userId))
        val farm = farms.createFarm("Solo")
        users.associateFarm(farm.id)
        farms.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.INCOME,
                category = TransactionCategory.SALES,
                amountMinor = 250L,
                description = "sale",
                occurredAt = "2024-03-01T10:00:00Z"
            )
        )
        farms.resetFarmData(farm.id)
        assertTrue(farms.loadFarm(farm.id)!!.transactions.isEmpty())
        assertEquals(user.userId, users.currentUser()?.userId)
        assertTrue(links.linkState(user.userId) is AccountLink.Unlinked)
    }

    @Test
    fun sessionAbsenceDoesNotRemoveLinkOrFarms() {
        // Session is a separate store; clearing it is simulated by never touching it.
        val user = users.ensureLocalUser()
        val farm = farms.createFarm("Keep")
        users.associateFarm(farm.id)
        links.linkToAccount(user.userId, "account-keep")

        // "No session" — only re-query domain stores.
        assertTrue(links.isLinked(user.userId))
        assertEquals(farm.id, farms.loadFarm(farm.id)?.id)
        assertEquals(setOf(farm.id), users.ownedFarmIds())
    }

    @Test
    fun farmBackupExcludesAccountAndLocalUserIdentity() {
        val user = users.ensureLocalUser()
        links.linkToAccount(user.userId, "account-secret-should-not-appear")
        val farm = farms.createFarm("BackupFarm", "NPR")
        users.associateFarm(farm.id)
        val encoded = FarmBackupCodec.encode(farms.loadFarm(farm.id)!!)
        assertFalse(encoded.contains(user.userId))
        assertFalse(encoded.contains("account-secret-should-not-appear"))
        assertFalse(encoded.contains("user-link-test"))
        // Payload is farm-only envelope
        val envelope = FarmBackupCodec.decode(encoded)
        assertEquals(farm.id, envelope.farm.id)
        assertEquals("BackupFarm", envelope.farm.name)
    }

    @Test
    fun farmPersistencePayloadAlsoExcludesAccountIdentity() {
        val user = users.ensureLocalUser()
        links.linkToAccount(user.userId, "account-not-in-payload")
        val farm = farms.createFarm("P")
        val payload = FarmPersistenceCodec.encode(farms.loadFarm(farm.id)!!)
        assertFalse(payload.contains("account-not-in-payload"))
        assertFalse(payload.contains(user.userId))
    }

    @Test
    fun resetAndDeleteFarmDoNotAlterAccountLink() {
        val user = users.ensureLocalUser()
        links.linkToAccount(user.userId, "account-stable")
        val farm = farms.createFarm("Doomed")
        users.associateFarm(farm.id)

        farms.resetFarmData(farm.id)
        assertEquals("account-stable", links.linkState(user.userId).accountIdOrNull)

        farms.deleteFarm(farm.id)
        users.disassociateFarm(farm.id)
        assertEquals(user.userId, users.currentUser()?.userId)
        assertEquals("account-stable", (links.linkState(user.userId) as AccountLink.Linked).accountId)
        assertTrue(users.ownedFarmIds().isEmpty())
    }

    @Test
    fun blankAccountIdRejected() {
        val user = users.ensureLocalUser()
        try {
            links.linkToAccount(user.userId, "   ")
            fail("expected require")
        } catch (_: IllegalArgumentException) {
            // expected
        }
        assertFalse(links.isLinked(user.userId))
    }

    @Test
    fun linkedAccountIdIsNotProviderShapedLocalUser() {
        val user = users.ensureLocalUser()
        assertTrue(user.userId.startsWith("user-"))
        val linked = links.linkToAccount(user.userId, "account-server-abc")
        assertTrue(linked.accountId.startsWith("account-"))
        assertNotEquals(user.userId, linked.accountId)
    }
}
