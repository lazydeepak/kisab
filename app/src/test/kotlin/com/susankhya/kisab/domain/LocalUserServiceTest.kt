package com.susankhya.kisab.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for [LocalUserService] verifying stable identity generation,
 * farm association, and migration safety.
 *
 * Confirms that a [LocalUser] is generated exactly once per device,
 * that farm operations never regenerate the user id, and that
 * farm resets/deletes preserve local identity.
 */
class LocalUserServiceTest {

    private lateinit var userStore: InMemoryLocalUserStore
    private lateinit var farmStore: InMemoryFarmStore
    private lateinit var farmService: FarmSliceService
    private lateinit var localUserService: LocalUserService
    private var nextId = 0
    private var now = 1_700_000_000_000L

    @Before
    fun setUp() {
        userStore = InMemoryLocalUserStore()
        farmStore = InMemoryFarmStore()
        farmService = FarmSliceService(farmStore)
        nextId = 0
        now = 1_700_000_000_000L
        localUserService = LocalUserService(
            store = userStore,
            clock = { now },
            idGenerator = { "user-test-${++nextId}" }
        )
    }

    /** First launch creates exactly one LocalUser with a stable id. */
    @Test
    fun firstLaunchGeneratesOneLocalUser() {
        assertNull(localUserService.currentUser())
        val user = localUserService.ensureLocalUser()
        assertEquals("user-test-1", user.userId)
        assertEquals(now, user.createdAtMillis)
        assertEquals(user, localUserService.currentUser())
    }

    /** Repeated loads return the same LocalUser id; id is never regenerated. */
    @Test
    fun repeatedLoadsReturnTheSameUserId() {
        val first = localUserService.ensureLocalUser()
        now += 60_000L
        val second = localUserService.ensureLocalUser()
        val third = localUserService.currentUser()
        assertEquals(first.userId, second.userId)
        assertEquals(first.userId, third?.userId)
        assertEquals(1, nextId)
    }

    /** Existing farms are associated with the generated user on first launch. */
    @Test
    fun existingFarmIsAssociatedWithGeneratedUser() {
        val farm = farmService.createFarm("Existing Farm", "NPR")
        localUserService.migrateExistingInstall(farm.id)
        val user = localUserService.currentUser()
        assertNotNull(user)
        assertEquals(setOf(farm.id), localUserService.ownedFarmIds())
        assertEquals(farm.id, farmService.currentFarmId())
        assertEquals("Existing Farm", farmService.loadFarm(farm.id)?.name)
    }

    /** Migration preserves all farm records and the farm id. */
    @Test
    fun migrationPreservesFarmIdAndAllRecords() {
        val farm = farmService.createFarm("Records Farm", "INR")
        farmService.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.INCOME,
                category = TransactionCategory.SALES,
                amountMinor = 12_500L,
                description = "sale",
                occurredAt = "2024-01-15T10:00:00+05:45"
            )
        )
        val party = farmService.addParty(farm.id, PartyDraft(name = "Buyer", role = PartyRole.CUSTOMER, contact = "", notes = ""))
        val before = farmService.loadFarm(farm.id)!!
        val farmIdBefore = before.id
        val txCount = before.transactions.size
        val partyCount = before.parties.size

        localUserService.migrateExistingInstall(farmIdBefore)

        val after = farmService.loadFarm(farmIdBefore)!!
        assertEquals(farmIdBefore, after.id)
        assertEquals("Records Farm", after.name)
        assertEquals("INR", after.currencyCode)
        assertEquals(txCount, after.transactions.size)
        assertEquals(partyCount, after.parties.size)
        assertEquals(12_500L, after.transactions.first().amountMinor)
        assertEquals(party.id, after.parties.first().id)
        assertEquals(setOf(farmIdBefore), localUserService.ownedFarmIds())
    }

    /** Resetting farm data preserves LocalUser identity. */
    @Test
    fun resetFarmDataPreservesLocalUserIdentity() {
        val farm = farmService.createFarm("Reset Me")
        localUserService.associateFarm(farm.id)
        val userId = localUserService.ensureLocalUser().userId
        farmService.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.EXPENSE,
                category = TransactionCategory.OTHER_EXPENSE,
                amountMinor = 100L,
                description = "seed",
                occurredAt = "2024-02-01T08:00:00+05:45"
            )
        )

        farmService.resetFarmData(farm.id)

        assertEquals(userId, localUserService.currentUser()?.userId)
        assertEquals(setOf(farm.id), localUserService.ownedFarmIds())
        val reset = farmService.loadFarm(farm.id)!!
        assertEquals(farm.id, reset.id)
        assertTrue(reset.transactions.isEmpty())
        assertEquals("Reset Me", reset.name)
    }

    /** Deleting a farm preserves LocalUser identity. */
    @Test
    fun deleteFarmPreservesLocalUserIdentity() {
        val farm = farmService.createFarm("Delete Me")
        localUserService.associateFarm(farm.id)
        val userId = localUserService.ensureLocalUser().userId

        farmService.deleteFarm(farm.id)
        localUserService.disassociateFarm(farm.id)

        assertEquals(userId, localUserService.currentUser()?.userId)
        assertTrue(localUserService.ownedFarmIds().isEmpty())
        assertNull(farmService.loadFarm(farm.id))
        assertNull(farmService.currentFarmId())
    }

    /** A device with no farms still has a valid LocalUser. */
    @Test
    fun noFarmStillLeavesAValidLocalUser() {
        localUserService.migrateExistingInstall(currentFarmId = null)
        val user = localUserService.currentUser()
        assertNotNull(user)
        assertTrue(user!!.userId.startsWith("user-"))
        assertTrue(localUserService.ownedFarmIds().isEmpty())
    }

    /** LocalUser id is stable across store recreations with the same backing. */
    @Test
    fun localUserIdIsNotRegeneratedAfterStoreRecreationWithSameBacking() {
        val shared = InMemoryLocalUserStore()
        val firstService = LocalUserService(shared, clock = { 10L }, idGenerator = { "user-stable" })
        val first = firstService.ensureLocalUser()
        firstService.associateFarm("farm-abc")

        val secondService = LocalUserService(shared, clock = { 99L }, idGenerator = { "user-other" })
        val second = secondService.ensureLocalUser()
        assertEquals(first.userId, second.userId)
        assertEquals(setOf("farm-abc"), secondService.ownedFarmIds())
        assertNotEquals("user-other", second.userId)
    }

    /** Associating the same farm twice is idempotent. */
    @Test
    fun associateFarmIsIdempotent() {
        localUserService.ensureLocalUser()
        localUserService.associateFarm("farm-1")
        localUserService.associateFarm("farm-1")
        assertEquals(setOf("farm-1"), localUserService.ownedFarmIds())
    }

    /** Migration with an existing farm is idempotent. */
    @Test
    fun migrateExistingInstallIsIdempotent() {
        val farm = farmService.createFarm("Once")
        localUserService.migrateExistingInstall(farm.id)
        val userId = localUserService.currentUser()!!.userId
        localUserService.migrateExistingInstall(farm.id)
        assertEquals(userId, localUserService.currentUser()!!.userId)
        assertEquals(setOf(farm.id), localUserService.ownedFarmIds())
        assertEquals(1, nextId)
    }
}
