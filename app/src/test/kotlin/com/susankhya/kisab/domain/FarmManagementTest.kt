package com.susankhya.kisab.domain

import com.susankhya.kisab.persistence.InMemoryMultiFarmBackend
import com.susankhya.kisab.persistence.MultiFarmStore
import com.susankhya.kisab.ui.DeleteFarmFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FarmManagementTest {

    private lateinit var store: MultiFarmStore
    private lateinit var service: FarmSliceService
    private lateinit var localUsers: LocalUserService

    @Before
    fun setUp() {
        store = MultiFarmStore(InMemoryMultiFarmBackend())
        service = FarmSliceService(store)
        localUsers = LocalUserService(
            InMemoryLocalUserStore(),
            clock = { 1L },
            idGenerator = { "user-fm" }
        )
    }

    @Test
    fun visibleFarmsUseIntersectionAndSkipStaleOwnership() {
        val a = service.createFarm("A")
        val b = service.createFarm("B")
        localUsers.associateFarm(a.id)
        localUsers.associateFarm(b.id)
        localUsers.associateFarm("farm-stale-missing")

        val visible = FarmManagement.visibleFarmIds(service.farmIds(), localUsers.ownedFarmIds())
        assertEquals(listOf(a.id, b.id), visible)
        assertFalse(visible.contains("farm-stale-missing"))
    }

    @Test
    fun emptyOwnershipStillShowsPersistedFarms() {
        val a = service.createFarm("A")
        val visible = FarmManagement.visibleFarmIds(service.farmIds(), emptySet())
        assertEquals(listOf(a.id), visible)
    }

    @Test
    fun addSecondFarmPreservesFirstAssociatesAndActivatesNew() {
        val first = service.createFarm("First", "NPR")
        localUsers.associateFarm(first.id)
        val second = service.createFarm("Second", "USD")
        localUsers.associateFarm(second.id)

        assertEquals(first.name, service.loadFarm(first.id)?.name)
        assertEquals("USD", service.loadFarm(second.id)?.currencyCode)
        assertEquals(second.id, service.currentFarmId())
        assertEquals(setOf(first.id, second.id), localUsers.ownedFarmIds())
    }

    @Test
    fun switchChangesCurrentOnly() {
        val a = service.createFarm("A", "NPR")
        val b = service.createFarm("B", "INR")
        localUsers.associateFarm(a.id)
        localUsers.associateFarm(b.id)
        service.createTransaction(
            a.id,
            FarmTransactionDraft(
                type = TransactionType.INCOME,
                category = TransactionCategory.SALES,
                amountMinor = 100L,
                description = "a",
                occurredAt = "2024-01-01T00:00:00Z"
            )
        )
        service.setCurrentFarmId(a.id)
        val ownedBefore = localUsers.ownedFarmIds()

        service.setCurrentFarmId(b.id)

        assertEquals(b.id, service.currentFarmId())
        assertEquals(100L, service.loadFarm(a.id)!!.transactions.first().amountMinor)
        assertTrue(service.loadFarm(b.id)!!.transactions.isEmpty())
        assertEquals(ownedBefore, localUsers.ownedFarmIds())
    }

    @Test
    fun renameAndCurrencyAffectSelectedFarmOnly() {
        val a = service.createFarm("A", "NPR")
        val b = service.createFarm("B", "INR")
        service.renameFarm(a.id, "Alpha")
        service.setFarmCurrency(a.id, "USD")
        assertEquals("Alpha", service.loadFarm(a.id)?.name)
        assertEquals("USD", service.loadFarm(a.id)?.currencyCode)
        assertEquals("B", service.loadFarm(b.id)?.name)
        assertEquals("INR", service.loadFarm(b.id)?.currencyCode)
    }

    @Test
    fun resetSelectedFarmOnly() {
        val a = service.createFarm("A")
        val b = service.createFarm("B")
        service.createTransaction(
            a.id,
            FarmTransactionDraft(
                type = TransactionType.EXPENSE,
                category = TransactionCategory.FEED,
                amountMinor = 50L,
                description = "x",
                occurredAt = "2024-01-01T00:00:00Z"
            )
        )
        service.createTransaction(
            b.id,
            FarmTransactionDraft(
                type = TransactionType.INCOME,
                category = TransactionCategory.SALES,
                amountMinor = 75L,
                description = "y",
                occurredAt = "2024-01-02T00:00:00Z"
            )
        )
        service.resetFarmData(a.id)
        assertTrue(service.loadFarm(a.id)!!.transactions.isEmpty())
        assertEquals(75L, service.loadFarm(b.id)!!.transactions.first().amountMinor)
        assertEquals("A", service.loadFarm(a.id)!!.name)
    }

    @Test
    fun deleteNonActiveLeavesActiveUnchanged() {
        val a = service.createFarm("A")
        val b = service.createFarm("B")
        localUsers.associateFarm(a.id)
        localUsers.associateFarm(b.id)
        service.setCurrentFarmId(a.id)
        val previous = service.currentFarmId()

        service.deleteFarm(b.id)
        localUsers.disassociateFarm(b.id)
        val next = FarmManagement.nextCurrentFarmIdAfterDelete(b.id, previous, service.farmIds())
        if (next != null) service.setCurrentFarmId(next)

        assertEquals(a.id, service.currentFarmId())
        assertNull(service.loadFarm(b.id))
        assertEquals(setOf(a.id), localUsers.ownedFarmIds())
    }

    @Test
    fun deleteActiveSelectsFirstRemainingFarm() {
        val a = service.createFarm("A")
        val b = service.createFarm("B")
        val c = service.createFarm("C")
        // order a,b,c — current c
        assertEquals(c.id, service.currentFarmId())
        val previous = c.id
        service.deleteFarm(c.id)
        val remaining = service.farmIds()
        val next = FarmManagement.nextCurrentFarmIdAfterDelete(c.id, previous, remaining)
        assertEquals(a.id, next)
    }

    @Test
    fun deleteFinalFarmYieldsNoFarmAndKeepsUser() {
        val a = service.createFarm("Only")
        localUsers.associateFarm(a.id)
        val userId = localUsers.currentUser()!!.userId
        service.deleteFarm(a.id)
        localUsers.disassociateFarm(a.id)
        val next = FarmManagement.nextCurrentFarmIdAfterDelete(a.id, a.id, service.farmIds())
        assertNull(next)
        assertTrue(service.farmIds().isEmpty())
        assertEquals(userId, localUsers.currentUser()?.userId)
        assertTrue(localUsers.ownedFarmIds().isEmpty())
    }

    @Test
    fun deleteFlowRequiresTypedDeleteKeyword() {
        var deleted = false
        val flow = DeleteFarmFlow { deleted = true }
        flow.begin()
        flow.proceedFromWarning(recentBackup = true)
        assertFalse(flow.canType("remove"))
        assertFalse(flow.confirm("remove"))
        assertFalse(deleted)
        assertTrue(flow.canType(" delete "))
        assertTrue(flow.confirm("DELETE"))
        assertTrue(deleted)
        assertEquals(DeleteFarmFlow.Stage.NONE, flow.stage)
    }

    @Test
    fun importNewFarmAddsWithoutWiping() {
        val existing = service.createFarm("Existing")
        localUsers.associateFarm(existing.id)
        val imported = FarmState(id = "farm-imported", name = "Imported", currencyCode = "USD")
        service.importFarm(imported)
        localUsers.associateFarm(imported.id)
        assertEquals(2, service.farmIds().size)
        assertNotNull(service.loadFarm(existing.id))
        assertEquals(imported.id, service.currentFarmId())
    }

    @Test
    fun importSameIdReplacesOnlyThatFarm() {
        val a = service.createFarm("A", "NPR")
        val b = service.createFarm("B", "INR")
        service.importFarm(FarmState(id = a.id, name = "A2", currencyCode = "NPR"))
        assertEquals("A2", service.loadFarm(a.id)?.name)
        assertEquals("B", service.loadFarm(b.id)?.name)
        assertEquals(2, service.farmIds().size)
    }
}
