package com.susankhya.kisab.persistence

import com.susankhya.kisab.domain.FarmEntry
import com.susankhya.kisab.domain.FarmEntryKind
import com.susankhya.kisab.domain.FarmSliceService
import com.susankhya.kisab.domain.FarmState
import com.susankhya.kisab.domain.FarmTransactionDraft
import com.susankhya.kisab.domain.InMemoryFarmStore
import com.susankhya.kisab.domain.InMemoryLocalUserStore
import com.susankhya.kisab.domain.LocalUserService
import com.susankhya.kisab.domain.TransactionCategory
import com.susankhya.kisab.domain.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class MultiFarmStoreTest {

    @Test
    fun legacyOneFarmMigratesExactlyOncePreservingIdAndPayloadBytes() {
        val farm = FarmState(
            id = "farm-legacy-fixed-id",
            name = "Legacy Farm",
            currencyCode = "NPR",
            entries = mutableListOf(FarmEntry(FarmEntryKind.CROP, "Maize", 5))
        )
        val legacyEncoded = FarmPersistenceCodec.encode(farm)
        val backend = InMemoryMultiFarmBackend(
            mapOf(
                MultiFarmStore.KEY_CURRENT_FARM_ID to farm.id,
                MultiFarmStore.KEY_LEGACY_FARM_STATE to legacyEncoded
            )
        )
        val store = MultiFarmStore(backend)

        val loaded = store.loadFarm(farm.id)
        assertNotNull(loaded)
        assertEquals(farm.id, loaded!!.id)
        assertEquals("Legacy Farm", loaded.name)
        assertEquals("NPR", loaded.currencyCode)
        assertEquals(1, loaded.entries.size)
        assertEquals(farm.id, store.currentFarmId())
        assertEquals(listOf(farm.id), store.farmIds())

        // Exact legacy bytes preserved in payload key; legacy key removed.
        assertEquals(legacyEncoded, backend.getString(MultiFarmStore.payloadKey(farm.id)))
        assertNull(backend.getString(MultiFarmStore.KEY_LEGACY_FARM_STATE))
        assertEquals("2", backend.getString(MultiFarmStore.KEY_LAYOUT_VERSION))

        // Second startup: no duplicate, same id.
        val store2 = MultiFarmStore(backend)
        assertEquals(listOf(farm.id), store2.farmIds())
        assertEquals(farm.id, store2.loadFarm(farm.id)?.id)
        assertEquals(1, store2.farmIds().size)
    }

    @Test
    fun repeatedStartupDoesNotDuplicateFarm() {
        val farm = FarmState(id = "farm-once", name = "Once", currencyCode = "INR")
        val backend = InMemoryMultiFarmBackend(
            mapOf(
                MultiFarmStore.KEY_CURRENT_FARM_ID to farm.id,
                MultiFarmStore.KEY_LEGACY_FARM_STATE to FarmPersistenceCodec.encode(farm)
            )
        )
        repeat(5) {
            val store = MultiFarmStore(backend)
            assertEquals(listOf(farm.id), store.farmIds())
            assertEquals(farm.id, store.currentFarmId())
        }
    }

    @Test
    fun migrationPreservesAllRecordsSemantically() {
        val service = FarmSliceService(InMemoryFarmStore())
        val farm = service.createFarm("Full Farm", "USD")
        service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.INCOME,
                category = TransactionCategory.SALES,
                amountMinor = 99_00L,
                description = "sale",
                occurredAt = "2024-06-01T10:00:00+05:45"
            )
        )
        val before = service.loadFarm(farm.id)!!
        val encoded = FarmPersistenceCodec.encode(before)
        val backend = InMemoryMultiFarmBackend(
            mapOf(
                MultiFarmStore.KEY_CURRENT_FARM_ID to before.id,
                MultiFarmStore.KEY_LEGACY_FARM_STATE to encoded
            )
        )
        val after = MultiFarmStore(backend).loadFarm(before.id)!!
        assertEquals(before.id, after.id)
        assertEquals(before.name, after.name)
        assertEquals(before.currencyCode, after.currencyCode)
        assertEquals(before.transactions.size, after.transactions.size)
        assertEquals(before.transactions.first().amountMinor, after.transactions.first().amountMinor)
        assertEquals(before.transactions.first().description, after.transactions.first().description)
        assertEquals(before.schemaVersion, after.schemaVersion)
    }

    @Test
    fun saveLoadFarmAAndBIndependently() {
        val store = MultiFarmStore(InMemoryMultiFarmBackend())
        val a = FarmState(id = "farm-a", name = "A", currencyCode = "NPR")
        val b = FarmState(id = "farm-b", name = "B", currencyCode = "INR")
        store.saveFarm(a)
        store.setCurrentFarmId(a.id)
        store.saveFarm(b)
        store.setCurrentFarmId(b.id)

        assertEquals("A", store.loadFarm("farm-a")?.name)
        assertEquals("B", store.loadFarm("farm-b")?.name)
        assertEquals("farm-b", store.currentFarmId())
        assertEquals(listOf("farm-a", "farm-b"), store.farmIds())
    }

    @Test
    fun updatingADoesNotChangeB() {
        val store = MultiFarmStore(InMemoryMultiFarmBackend())
        store.saveFarm(FarmState(id = "farm-a", name = "A", currencyCode = "NPR"))
        store.saveFarm(FarmState(id = "farm-b", name = "B", currencyCode = "INR"))
        store.setCurrentFarmId("farm-a")

        store.saveFarm(
            FarmState(
                id = "farm-a",
                name = "A-renamed",
                currencyCode = "NPR",
                entries = mutableListOf(FarmEntry(FarmEntryKind.LIVESTOCK, "Cow", 2))
            )
        )

        assertEquals("A-renamed", store.loadFarm("farm-a")?.name)
        assertEquals(1, store.loadFarm("farm-a")?.entries?.size)
        assertEquals("B", store.loadFarm("farm-b")?.name)
        assertTrue(store.loadFarm("farm-b")!!.entries.isEmpty())
        assertEquals("farm-a", store.currentFarmId())
    }

    @Test
    fun switchingCurrentFarmPreservesBoth() {
        val store = MultiFarmStore(InMemoryMultiFarmBackend())
        store.saveFarm(FarmState(id = "farm-a", name = "A", currencyCode = "NPR"))
        store.saveFarm(FarmState(id = "farm-b", name = "B", currencyCode = "USD"))
        store.setCurrentFarmId("farm-a")
        store.setCurrentFarmId("farm-b")
        store.setCurrentFarmId("farm-a")

        assertEquals("farm-a", store.currentFarmId())
        assertEquals(listOf("farm-a", "farm-b"), store.farmIds())
        assertEquals("A", store.loadFarm("farm-a")?.name)
        assertEquals("B", store.loadFarm("farm-b")?.name)
    }

    @Test
    fun setCurrentFarmIdRejectsUnknownFarm() {
        val store = MultiFarmStore(InMemoryMultiFarmBackend())
        store.saveFarm(FarmState(id = "farm-a", name = "A", currencyCode = "NPR"))
        try {
            store.setCurrentFarmId("farm-missing")
            fail("expected require failure")
        } catch (_: IllegalArgumentException) {
            // expected
        }
        assertNull(store.currentFarmId())
    }

    @Test
    fun storeRecreationReloadsBoth() {
        val backend = InMemoryMultiFarmBackend()
        val store1 = MultiFarmStore(backend)
        store1.saveFarm(FarmState(id = "farm-a", name = "A", currencyCode = "NPR"))
        store1.saveFarm(FarmState(id = "farm-b", name = "B", currencyCode = "INR"))
        store1.setCurrentFarmId("farm-b")

        val store2 = MultiFarmStore(backend)
        assertEquals(listOf("farm-a", "farm-b"), store2.farmIds())
        assertEquals("farm-b", store2.currentFarmId())
        assertEquals("A", store2.loadFarm("farm-a")?.name)
        assertEquals("B", store2.loadFarm("farm-b")?.name)
    }

    @Test
    fun deleteALeavesBIntactAndClearsCurrentIfActive() {
        val store = MultiFarmStore(InMemoryMultiFarmBackend())
        store.saveFarm(FarmState(id = "farm-a", name = "A", currencyCode = "NPR"))
        store.saveFarm(FarmState(id = "farm-b", name = "B", currencyCode = "INR"))
        store.setCurrentFarmId("farm-a")
        store.deleteFarm("farm-a")

        assertNull(store.loadFarm("farm-a"))
        assertNull(store.currentFarmId())
        assertEquals(listOf("farm-b"), store.farmIds())
        assertEquals("B", store.loadFarm("farm-b")?.name)
    }

    @Test
    fun deleteNonActiveLeavesCurrentUnchanged() {
        val store = MultiFarmStore(InMemoryMultiFarmBackend())
        store.saveFarm(FarmState(id = "farm-a", name = "A", currencyCode = "NPR"))
        store.saveFarm(FarmState(id = "farm-b", name = "B", currencyCode = "INR"))
        store.setCurrentFarmId("farm-b")
        store.deleteFarm("farm-a")
        assertEquals("farm-b", store.currentFarmId())
        assertEquals(listOf("farm-b"), store.farmIds())
    }

    @Test
    fun resetAClearsOnlyA() {
        val store = MultiFarmStore(InMemoryMultiFarmBackend())
        val service = FarmSliceService(store)
        val a = service.createFarm("A", "NPR")
        val b = service.createFarm("B", "INR")
        service.createTransaction(
            a.id,
            FarmTransactionDraft(
                type = TransactionType.EXPENSE,
                category = TransactionCategory.FEED,
                amountMinor = 500L,
                description = "feed",
                occurredAt = "2024-01-01T00:00:00Z"
            )
        )
        service.createTransaction(
            b.id,
            FarmTransactionDraft(
                type = TransactionType.INCOME,
                category = TransactionCategory.SALES,
                amountMinor = 700L,
                description = "sale",
                occurredAt = "2024-01-02T00:00:00Z"
            )
        )
        val bBefore = service.loadFarm(b.id)!!
        val bEncodedBefore = FarmPersistenceCodec.encode(bBefore)

        service.resetFarmData(a.id)

        assertTrue(service.loadFarm(a.id)!!.transactions.isEmpty())
        assertEquals("A", service.loadFarm(a.id)!!.name)
        val bAfter = service.loadFarm(b.id)!!
        assertEquals(bEncodedBefore, FarmPersistenceCodec.encode(bAfter))
        assertEquals(700L, bAfter.transactions.first().amountMinor)
    }

    @Test
    fun createSecondFarmDoesNotOverwriteFirst() {
        val store = MultiFarmStore(InMemoryMultiFarmBackend())
        val service = FarmSliceService(store)
        val localUsers = LocalUserService(InMemoryLocalUserStore(), clock = { 1L }, idGenerator = { "user-1" })
        val first = service.createFarm("First", "NPR")
        localUsers.associateFarm(first.id)
        val second = service.createFarm("Second", "USD")
        localUsers.associateFarm(second.id)

        assertEquals(2, service.farmIds().size)
        assertEquals("First", service.loadFarm(first.id)?.name)
        assertEquals("Second", service.loadFarm(second.id)?.name)
        assertEquals(second.id, service.currentFarmId())
        assertEquals(setOf(first.id, second.id), localUsers.ownedFarmIds())
    }

    @Test
    fun deleteRemovesOwnershipOnlyForThatFarmAndKeepsUser() {
        val store = MultiFarmStore(InMemoryMultiFarmBackend())
        val service = FarmSliceService(store)
        val localUsers = LocalUserService(InMemoryLocalUserStore(), clock = { 1L }, idGenerator = { "user-keep" })
        val a = service.createFarm("A")
        val b = service.createFarm("B")
        localUsers.associateFarm(a.id)
        localUsers.associateFarm(b.id)
        val userId = localUsers.currentUser()!!.userId

        service.deleteFarm(a.id)
        localUsers.disassociateFarm(a.id)

        assertEquals(userId, localUsers.currentUser()?.userId)
        assertEquals(setOf(b.id), localUsers.ownedFarmIds())
        assertNull(service.loadFarm(a.id))
        assertNotNull(service.loadFarm(b.id))
    }

    @Test
    fun importExistingFarmIdReplacesOnlyThatFarm() {
        val store = MultiFarmStore(InMemoryMultiFarmBackend())
        val service = FarmSliceService(store)
        val a = service.createFarm("A", "NPR")
        val b = service.createFarm("B", "INR")
        val replacement = FarmState(
            id = a.id,
            name = "A-imported",
            currencyCode = "NPR",
            entries = mutableListOf(FarmEntry(FarmEntryKind.CROP, "Rice", 3))
        )
        service.importFarm(replacement)

        assertEquals("A-imported", service.loadFarm(a.id)?.name)
        assertEquals(1, service.loadFarm(a.id)?.entries?.size)
        assertEquals("B", service.loadFarm(b.id)?.name)
        assertEquals(a.id, service.currentFarmId())
        assertEquals(2, service.farmIds().size)
    }

    @Test
    fun importNewFarmIdDoesNotWipeExistingFarms() {
        val store = MultiFarmStore(InMemoryMultiFarmBackend())
        val service = FarmSliceService(store)
        val localUsers = LocalUserService(InMemoryLocalUserStore(), clock = { 1L }, idGenerator = { "user-1" })
        val existing = service.createFarm("Existing", "NPR")
        localUsers.associateFarm(existing.id)

        val imported = FarmState(id = "farm-from-backup", name = "Imported", currencyCode = "USD")
        service.importFarm(imported)
        localUsers.associateFarm(imported.id)

        assertEquals(2, service.farmIds().size)
        assertEquals("Existing", service.loadFarm(existing.id)?.name)
        assertEquals("Imported", service.loadFarm(imported.id)?.name)
        assertEquals(imported.id, service.currentFarmId())
        assertEquals(setOf(existing.id, imported.id), localUsers.ownedFarmIds())
    }

    @Test
    fun migrationAssociatesExistingFarmWithLocalUser() {
        val farm = FarmState(id = "farm-owned", name = "Owned", currencyCode = "NPR")
        val backend = InMemoryMultiFarmBackend(
            mapOf(
                MultiFarmStore.KEY_CURRENT_FARM_ID to farm.id,
                MultiFarmStore.KEY_LEGACY_FARM_STATE to FarmPersistenceCodec.encode(farm)
            )
        )
        val store = MultiFarmStore(backend)
        val service = FarmSliceService(store)
        val localUsers = LocalUserService(InMemoryLocalUserStore(), clock = { 42L }, idGenerator = { "user-mig" })

        localUsers.migrateExistingInstall(service.currentFarmId())

        assertEquals("user-mig", localUsers.currentUser()?.userId)
        assertEquals(setOf(farm.id), localUsers.ownedFarmIds())
        assertEquals(farm.id, service.currentFarmId())
        assertEquals("Owned", service.loadFarm(farm.id)?.name)
    }

    @Test
    fun saveFarmDoesNotImplicitlyChangeCurrent() {
        val store = MultiFarmStore(InMemoryMultiFarmBackend())
        store.saveFarm(FarmState(id = "farm-a", name = "A", currencyCode = "NPR"))
        store.saveFarm(FarmState(id = "farm-b", name = "B", currencyCode = "INR"))
        store.setCurrentFarmId("farm-b")
        store.saveFarm(FarmState(id = "farm-a", name = "A2", currencyCode = "NPR"))
        assertEquals("farm-b", store.currentFarmId())
        assertEquals("A2", store.loadFarm("farm-a")?.name)
    }

    @Test
    fun inMemoryCreateSecondFarmKeepsFirst() {
        val service = FarmSliceService(InMemoryFarmStore())
        val first = service.createFarm("First")
        val second = service.createFarm("Second")
        assertEquals(first.name, service.loadFarm(first.id)?.name)
        assertEquals(second.name, service.loadFarm(second.id)?.name)
        assertEquals(second.id, service.currentFarmId())
        assertEquals(listOf(first.id, second.id), service.farmIds())
    }

    @Test
    fun failedMigrationLeavesLegacyIntact() {
        val farm = FarmState(id = "farm-fail", name = "Fail", currencyCode = "NPR")
        val legacy = FarmPersistenceCodec.encode(farm)
        val backend = object : MultiFarmStoreBackend {
            private val data = linkedMapOf(
                MultiFarmStore.KEY_CURRENT_FARM_ID to farm.id,
                MultiFarmStore.KEY_LEGACY_FARM_STATE to legacy
            )
            var failNext = true
            override fun getString(key: String): String? = data[key]
            override fun commit(puts: Map<String, String>, removes: Set<String>): Boolean {
                if (failNext) {
                    failNext = false
                    return false
                }
                removes.forEach { data.remove(it) }
                data.putAll(puts)
                return true
            }
            override fun allKeys(): Set<String> = data.keys.toSet()
            fun snap() = data.toMap()
        }
        val store = MultiFarmStore(backend)
        try {
            store.loadFarm(farm.id)
            fail("expected migration failure")
        } catch (_: IllegalStateException) {
            // expected
        }
        val snap = backend.snap()
        assertEquals(legacy, snap[MultiFarmStore.KEY_LEGACY_FARM_STATE])
        assertFalse(snap.containsKey(MultiFarmStore.KEY_LAYOUT_VERSION))
        assertNull(snap[MultiFarmStore.payloadKey(farm.id)])
    }
}
