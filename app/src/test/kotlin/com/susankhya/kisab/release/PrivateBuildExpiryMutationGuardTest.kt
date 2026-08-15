package com.susankhya.kisab.release

import com.susankhya.kisab.domain.FarmSliceService
import com.susankhya.kisab.domain.FarmTransactionDraft
import com.susankhya.kisab.domain.InMemoryLocalUserStore
import com.susankhya.kisab.domain.LocalUserService
import com.susankhya.kisab.domain.TransactionCategory
import com.susankhya.kisab.domain.TransactionType
import com.susankhya.kisab.persistence.FarmBackupCodec
import com.susankhya.kisab.persistence.FarmPersistenceCodec
import com.susankhya.kisab.persistence.InMemoryMultiFarmBackend
import com.susankhya.kisab.persistence.MultiFarmStore
import com.susankhya.kisab.ui.Clock
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Domain-level proof that expired private builds still allow read/backup while
 * product UI blocks mutations. Farm payloads are unchanged by evaluating expiry.
 */
class PrivateBuildExpiryMutationGuardTest {

    private val day = TimeUnit.DAYS.toMillis(1)
    private val expiresAt = 3_000_000_000_000L
    private lateinit var farms: FarmSliceService
    private lateinit var users: LocalUserService
    private lateinit var backend: InMemoryMultiFarmBackend

    @Before
    fun setUp() {
        backend = InMemoryMultiFarmBackend()
        farms = FarmSliceService(MultiFarmStore(backend))
        users = LocalUserService(InMemoryLocalUserStore(), clock = { 1L }, idGenerator = { "user-exp" })
    }

    private fun expiredGate(): PrivateBuildExpiryGate {
        val store = object : PrivateBuildClockStore {
            private var g: Long? = null
            override fun greatestObservedEpochMillis() = g
            override fun recordObservedEpochMillis(epochMillis: Long) {
                if (g == null || epochMillis > g!!) g = epochMillis
            }
        }
        return PrivateBuildExpiryGate(
            enabled = true,
            expiresAtEpochMillis = expiresAt,
            deviceClock = Clock { expiresAt + day },
            clockStore = store
        )
    }

    @Test
    fun expiredSnapshotBlocksMutationsAllowsBackupAndView() {
        val snap = expiredGate().snapshot()
        assertEquals(PrivateBuildAccessStage.EXPIRED, snap.stage)
        assertFalse(snap.mutationsAllowed)
        assertFalse(snap.importAllowed)
        assertTrue(snap.backupAllowed)
        assertTrue(snap.viewAllowed)
    }

    @Test
    fun evaluatingExpiryDoesNotModifyFarmPayloadsOrLocalUser() {
        val user = users.ensureLocalUser()
        val farm = farms.createFarm("Keep", "NPR")
        users.associateFarm(farm.id)
        farms.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.INCOME,
                category = TransactionCategory.SALES,
                amountMinor = 1234L,
                description = "seed",
                occurredAt = "2024-01-01T00:00:00Z"
            )
        )
        val beforePayload = FarmPersistenceCodec.encode(farms.loadFarm(farm.id)!!)
        val beforeUser = users.currentUser()!!.userId
        val beforeOwned = users.ownedFarmIds()

        repeat(3) { expiredGate().snapshot() }

        assertEquals(beforePayload, FarmPersistenceCodec.encode(farms.loadFarm(farm.id)!!))
        assertEquals(beforeUser, users.currentUser()?.userId)
        assertEquals(beforeOwned, users.ownedFarmIds())
        assertNotNull(FarmBackupCodec.encode(farms.loadFarm(farm.id)!!))
    }

    @Test
    fun expiredStillAllowsReadListAndSwitchSemantics() {
        val a = farms.createFarm("A")
        val b = farms.createFarm("B")
        farms.setCurrentFarmId(a.id)
        val gate = expiredGate()
        assertFalse(gate.mutationsAllowed())
        // Read/switch are store operations that do not create business records
        assertEquals("A", farms.loadFarm(a.id)?.name)
        assertEquals(listOf(a.id, b.id), farms.farmIds())
        farms.setCurrentFarmId(b.id)
        assertEquals(b.id, farms.currentFarmId())
        assertTrue(gate.backupAllowed())
    }

    @Test
    fun productGuardWouldBlockImportWhenExpired() {
        // Mirrors UI: importAllowed false when expired
        assertFalse(expiredGate().importAllowed())
        assertTrue(
            PrivateBuildExpiryPolicy.evaluate(true, expiresAt, expiresAt - 30 * day).importAllowed
        )
    }
}
