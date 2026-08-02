package com.susankhya.kisab.domain

import com.susankhya.kisab.persistence.FarmBackupCodec
import com.susankhya.kisab.persistence.FarmPersistenceCodec
import java.time.format.DateTimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class FarmSliceServiceTest {
    private lateinit var service: FarmSliceService

    @Before
    fun setUp() {
        service = FarmSliceService(InMemoryFarmStore())
    }

    @Test
    fun createsFarmAddsEntryAndTransactionsAndSummarizesBalance() {
        val farm = service.createFarm("Demo Farm")
        service.addEntry(farm.id, FarmEntry(FarmEntryKind.LIVESTOCK, "Goat", 3))
        val expense = service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.EXPENSE,
                category = TransactionCategory.FEED,
                amountMinor = 5000,
                currency = "USD",
                description = "Feed purchase",
                occurredAt = "2024-01-01T12:00:00Z"
            )
        )
        service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.INCOME,
                category = TransactionCategory.SALES,
                amountMinor = 8000,
                currency = "USD",
                description = "Egg sale",
                occurredAt = "2024-01-02T12:00:00Z"
            )
        )

        val summary = service.summary(farm.id)

        assertEquals(1, summary.entryCount)
        assertEquals(2, summary.transactionCount)
        assertEquals(3000, summary.balanceMinor)
        assertEquals("USD", summary.currencyCode)
        assertEquals(expense.id, service.loadFarm(farm.id)?.transactions?.first()?.id)
    }

    @Test
    fun createFarmRequiresAName() {
        try {
            service.createFarm("   ")
            fail("Expected IllegalArgumentException")
        } catch (exception: IllegalArgumentException) {
            assertEquals("Farm name is required", exception.message)
        }
    }

    @Test
    fun addEntryRequiresNonBlankLabelAndPositiveQuantity() {
        val farm = service.createFarm("Demo Farm")

        try {
            service.addEntry(farm.id, FarmEntry(FarmEntryKind.CROP, "   ", 1))
            fail("Expected IllegalArgumentException")
        } catch (exception: IllegalArgumentException) {
            assertEquals("Entry label is required", exception.message)
        }

        try {
            service.addEntry(farm.id, FarmEntry(FarmEntryKind.CROP, "Wheat", 0))
            fail("Expected IllegalArgumentException")
        } catch (exception: IllegalArgumentException) {
            assertEquals("Entry quantity must be positive", exception.message)
        }
    }

    @Test
    fun transactionsRequireDescriptionAmountCategoryAndCurrency() {
        val farm = service.createFarm("Demo Farm")

        try {
            service.createTransaction(
                farm.id,
                FarmTransactionDraft(
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.FEED,
                    amountMinor = 0,
                    currency = "USD",
                    description = "   ",
                    occurredAt = "2024-01-01T12:00:00Z"
                )
            )
            fail("Expected IllegalArgumentException")
        } catch (exception: IllegalArgumentException) {
            assertEquals("Transaction description is required", exception.message)
        }

        try {
            service.createTransaction(
                farm.id,
                FarmTransactionDraft(
                    type = TransactionType.INCOME,
                    category = TransactionCategory.SALES,
                    amountMinor = 1000,
                    currency = "US",
                    description = "Sale",
                    occurredAt = "2024-01-01T12:00:00Z"
                )
            )
            fail("Expected IllegalArgumentException")
        } catch (exception: IllegalArgumentException) {
            assertEquals("Currency must be a 3-letter ISO code", exception.message)
        }

        try {
            service.createTransaction(
                farm.id,
                FarmTransactionDraft(
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.SALES,
                    amountMinor = 1000,
                    currency = "USD",
                    description = "Bad category",
                    occurredAt = "2024-01-01T12:00:00Z"
                )
            )
            fail("Expected IllegalArgumentException")
        } catch (exception: IllegalArgumentException) {
            assertEquals("Transaction category is invalid for the selected type", exception.message)
        }
    }

    @Test
    fun updateAndDeleteTransactionsRecalculateSummary() {
        val farm = service.createFarm("Demo Farm")
        val created = service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.INCOME,
                category = TransactionCategory.SALES,
                amountMinor = 1000,
                currency = "USD",
                description = "Seed sale",
                occurredAt = "2024-01-01T12:00:00Z"
            )
        )
        service.updateTransaction(
            farm.id,
            created.id,
            FarmTransactionDraft(
                type = TransactionType.INCOME,
                category = TransactionCategory.SERVICES,
                amountMinor = 2500,
                currency = "USD",
                description = "Service fee",
                occurredAt = "2024-01-01T12:00:00Z"
            )
        )
        service.deleteTransaction(farm.id, created.id)

        val summary = service.summary(farm.id)
        assertEquals(0, summary.transactionCount)
        assertEquals(0, summary.balanceMinor)
    }

    @Test
    fun invalidCreateDoesNotMutateFarmState() {
        val farm = service.createFarm("Demo Farm")

        try {
            service.createTransaction(
                farm.id,
                FarmTransactionDraft(
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.FEED,
                    amountMinor = 1000,
                    currency = "US",
                    description = "Bad currency",
                    occurredAt = "2024-01-01T12:00:00Z"
                )
            )
            fail("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            val persistedFarm = service.loadFarm(farm.id)
            assertEquals(0, persistedFarm?.transactions?.size)
        }
    }

    @Test
    fun summaryRejectsMixedCurrencyTransactions() {
        val farm = service.createFarm("Demo Farm")
        service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.INCOME,
                category = TransactionCategory.SALES,
                amountMinor = 1000,
                currency = "USD",
                description = "Sale",
                occurredAt = "2024-01-01T12:00:00Z"
            )
        )
        service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.EXPENSE,
                category = TransactionCategory.FEED,
                amountMinor = 500,
                currency = "EUR",
                description = "Feed",
                occurredAt = "2024-01-02T12:00:00Z"
            )
        )

        try {
            service.summary(farm.id)
            fail("Expected IllegalArgumentException")
        } catch (exception: IllegalArgumentException) {
            assertEquals("Transactions use multiple currencies", exception.message)
        }
    }

    @Test
    fun transactionIdsStayStableAcrossPersistenceAndUpdate() {
        val farm = service.createFarm("Demo Farm")
        val created = service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.INCOME,
                category = TransactionCategory.SALES,
                amountMinor = 1000,
                currency = "USD",
                description = "Seed sale",
                occurredAt = "2024-01-01T12:00:00Z"
            )
        )

        val updated = service.updateTransaction(
            farm.id,
            created.id,
            FarmTransactionDraft(
                type = TransactionType.INCOME,
                category = TransactionCategory.SERVICES,
                amountMinor = 2500,
                currency = "USD",
                description = "Service fee",
                occurredAt = "2024-01-01T12:00:00Z"
            )
        )
        val encoded = FarmPersistenceCodec.encode(service.loadFarm(farm.id)!!)
        val reloaded = FarmPersistenceCodec.decode(encoded)

        assertEquals(created.id, updated.id)
        assertEquals(created.id, reloaded.transactions.single().id)
        assertEquals("2024-01-01T12:00:00Z", reloaded.transactions.single().occurredAt.toInstant().toString())
    }

    @Test
    fun legacyMigrationProducesStableTransactionIdsAndUtcTimes() {
        val farm = FarmPersistenceCodec.decode("farm-legacy|Legacy Farm|LIVESTOCK:Goat:2|Feed:1000;;Egg sale:-5000")

        assertEquals(2, farm.transactions.size)
        assertEquals("tx-migrated-0", farm.transactions[0].id)
        assertEquals("tx-migrated-1", farm.transactions[1].id)
        assertEquals(TransactionType.EXPENSE, farm.transactions[0].type)
        assertEquals(TransactionType.INCOME, farm.transactions[1].type)
        assertEquals("USD", farm.transactions[0].currency)
        assertEquals("2024-01-01T00:00:00Z", farm.transactions[0].occurredAt.toInstant().toString())
    }

    @Test
    fun malformedPayloadsFailSafely() {
        assertNull(FarmPersistenceCodec.decodeOrNull("not-a-valid-payload"))
    }

    @Test
    fun unknownFarmThrowsAndDoesNotCreateState() {
        try {
            service.summary("missing")
            fail("Expected IllegalArgumentException")
        } catch (exception: IllegalArgumentException) {
            assertEquals("Unknown farm: missing", exception.message)
        }

        assertNull(service.loadFarm("missing"))
    }

    @Test
    fun backupEnvelopeRoundTripsFarmState() {
        val farm = service.createFarm("Demo Farm")
        service.addEntry(farm.id, FarmEntry(FarmEntryKind.LIVESTOCK, "Goat", 2))
        service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.EXPENSE,
                category = TransactionCategory.FEED,
                amountMinor = 1500,
                currency = "USD",
                description = "Feed",
                occurredAt = "2024-01-01T12:00:00Z"
            )
        )

        val encoded = FarmBackupCodec.encode(service.loadFarm(farm.id)!!)
        val envelope = FarmBackupCodec.decode(encoded)

        assertEquals(1, envelope.schemaVersion)
        assertEquals(farm.id, envelope.farm.id)
        assertEquals(1, envelope.farm.entries.size)
        assertEquals(1, envelope.farm.transactions.size)
    }

    @Test
    fun malformedBackupEnvelopeFailsSafely() {
        assertNull(FarmBackupCodec.decodeOrNull("not-a-valid-payload"))
    }

    @Test
    fun unsupportedBackupVersionFailsWithSpecificMessage() {
        try {
            FarmBackupCodec.decode("2\u001F2024-01-01T00:00:00Z\u001Ffarm")
            fail("Expected IllegalArgumentException")
        } catch (exception: IllegalArgumentException) {
            assertEquals("Unsupported backup version: 2", exception.message)
        }
    }

    @Test
    fun oversizedBackupEnvelopeFailsSafely() {
        val oversized = "1\u001F2024-01-01T00:00:00Z\u001F" + "x".repeat(FarmBackupCodec.MAX_BACKUP_BYTES + 1)
        assertNull(FarmBackupCodec.decodeOrNull(oversized))
    }

    @Test
    fun invalidBackupSnapshotFailsSafely() {
        val farm = FarmState(
            id = "farm-1",
            name = "Demo Farm",
            entries = mutableListOf(FarmEntry(FarmEntryKind.LIVESTOCK, "Goat", 2)),
            transactions = mutableListOf(
                FarmTransaction(
                    id = "tx-1",
                    type = TransactionType.INCOME,
                    category = TransactionCategory.SALES,
                    amountMinor = 1500,
                    currency = "USD",
                    description = "Sale",
                    occurredAt = java.time.OffsetDateTime.parse("2024-01-01T12:00:00Z")
                ),
                FarmTransaction(
                    id = "tx-2",
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.FEED,
                    amountMinor = 500,
                    currency = "EUR",
                    description = "Feed",
                    occurredAt = java.time.OffsetDateTime.parse("2024-01-02T12:00:00Z")
                )
            )
        )

        val encoded = FarmBackupCodec.encode(farm)
        assertNull(FarmBackupCodec.decodeOrNull(encoded))
    }

    @Test
    fun currentFarmIdTracksMostRecentFarm() {
        val firstFarm = service.createFarm("First")
        assertNotNull(service.currentFarmId())
        val secondFarm = service.createFarm("Second")

        assertEquals(secondFarm.id, service.currentFarmId())
        assertEquals(secondFarm.id, service.loadFarm(secondFarm.id)?.id)
        assertEquals(firstFarm.name, service.loadFarm(firstFarm.id)?.name)
    }
}
