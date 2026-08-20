package com.susankhya.kisab.persistence

import com.susankhya.kisab.domain.FarmActivityType
import com.susankhya.kisab.domain.FarmSliceService
import com.susankhya.kisab.domain.FarmState
import com.susankhya.kisab.domain.FarmTransaction
import com.susankhya.kisab.domain.InMemoryFarmStore
import com.susankhya.kisab.domain.TransactionCategory
import com.susankhya.kisab.domain.TransactionType
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FarmActivityPersistenceTest {
    private val field = "\u001F"
    private val record = "\u001E"
    private val tsep = "\u001D"

    @Test
    fun schema13RoundTripPreservesActivitiesAndTransactionAssociations() {
        val service = FarmSliceService(InMemoryFarmStore())
        val farm = service.createFarm(
            "Mixed Farm",
            activities = listOf(FarmActivityType.POULTRY, FarmActivityType.CROPS)
        )
        service.createTransaction(
            farm.id,
            com.susankhya.kisab.domain.FarmTransactionDraft(
                type = TransactionType.EXPENSE,
                category = TransactionCategory.FEED,
                amountMinor = 1200,
                description = "Broiler feed",
                occurredAt = "2024-02-01T12:00:00Z",
                activity = FarmActivityType.POULTRY
            )
        )
        service.createTransaction(
            farm.id,
            com.susankhya.kisab.domain.FarmTransactionDraft(
                type = TransactionType.INCOME,
                category = TransactionCategory.SALES,
                amountMinor = 5000,
                description = "Seed sale",
                occurredAt = "2024-02-02T12:00:00Z",
                activity = FarmActivityType.CROPS
            )
        )

        val encoded = FarmPersistenceCodec.encode(service.loadFarm(farm.id)!!)
        val decoded = FarmPersistenceCodec.decode(encoded)

        assertEquals(FarmActivityType.CROPS, decoded.activities[0])
        assertEquals(FarmActivityType.POULTRY, decoded.activities[1])
        assertEquals(FarmActivityType.POULTRY, decoded.transactions[0].activity)
        assertEquals(FarmActivityType.CROPS, decoded.transactions[1].activity)
        assertEquals(1200L, decoded.transactions[0].amountMinor)
        assertEquals(5000L, decoded.transactions[1].amountMinor)
    }

    @Test
    fun schema13RoundTripPreservesDisabledActivities() {
        val service = FarmSliceService(InMemoryFarmStore())
        val farm = service.createFarm(
            "Poultry Farm",
            activities = listOf(FarmActivityType.POULTRY, FarmActivityType.GOAT_SHEEP)
        )
        service.createTransaction(
            farm.id,
            com.susankhya.kisab.domain.FarmTransactionDraft(
                type = TransactionType.EXPENSE,
                category = TransactionCategory.FEED,
                amountMinor = 900,
                description = "Goat feed",
                occurredAt = "2024-02-03T12:00:00Z",
                activity = FarmActivityType.GOAT_SHEEP
            )
        )
        service.setFarmActivities(farm.id, setOf(FarmActivityType.POULTRY))

        val loaded = service.loadFarm(farm.id)!!
        assertEquals(listOf(FarmActivityType.POULTRY), loaded.activities)
        assertEquals(listOf(FarmActivityType.GOAT_SHEEP), loaded.disabledActivities)

        val decoded = FarmPersistenceCodec.decode(FarmPersistenceCodec.encode(loaded))
        assertEquals(listOf(FarmActivityType.POULTRY), decoded.activities)
        assertEquals(listOf(FarmActivityType.GOAT_SHEEP), decoded.disabledActivities)
        assertEquals(FarmActivityType.GOAT_SHEEP, decoded.transactions[0].activity)
    }

    @Test
    fun schema12PayloadMigratesToEmptyActivitiesAndNullAssociations() {
        val payload = "12${field}farm-s12${field}Legacy Farm${field}${field}NPR${field}${field}${field}${field}${field}${field}${field}${field}${field}${field}${field}${field}"

        val farm = FarmPersistenceCodec.decodeOrNull(payload)

        assertNotNull("schema-12 payload should decode", farm)
        assertEquals("Legacy Farm", farm!!.name)
        assertEquals(FarmState.CURRENT_FARM_SCHEMA_VERSION, farm.schemaVersion)
        assertEquals(emptyList<FarmActivityType>(), farm.activities)
        assertEquals(emptyList<FarmActivityType>(), farm.disabledActivities)
        assertEquals(0, farm.transactions.size)
    }

    @Test
    fun schema13PayloadDecodesSevenPartTransactionsAndActivityFields() {
        val occurredAt = "2024-03-01T12:00:00Z"
        val transaction = listOf(
            "tx-1",
            TransactionType.EXPENSE.name,
            TransactionCategory.FEED.name,
            "1500",
            "Layer feed",
            occurredAt,
            FarmActivityType.POULTRY.name
        ).joinToString(tsep)
        val payload = listOf(
            "13",
            "farm-s13",
            "Poultry Farm",
            "",
            "NPR",
            transaction,
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            FarmActivityType.POULTRY.name,
            FarmActivityType.GOAT_SHEEP.name
        ).joinToString(field)

        val farm = FarmPersistenceCodec.decodeOrNull(payload)

        assertNotNull("schema-13 payload should decode", farm)
        assertEquals("Poultry Farm", farm!!.name)
        assertEquals(listOf(FarmActivityType.POULTRY), farm.activities)
        assertEquals(listOf(FarmActivityType.GOAT_SHEEP), farm.disabledActivities)
        assertEquals(1, farm.transactions.size)
        assertEquals(FarmActivityType.POULTRY, farm.transactions[0].activity)
        assertEquals(1500L, farm.transactions[0].amountMinor)
    }

    @Test
    fun schema13PayloadAcceptsSixPartTransactionRecordsAsGeneral() {
        val transaction = listOf(
            "tx-6",
            TransactionType.INCOME.name,
            TransactionCategory.SALES.name,
            "7000",
            "Mixed produce",
            "2024-03-02T12:00:00Z"
        ).joinToString(tsep)
        val payload = listOf(
            "13",
            "farm-s13b",
            "Mixed Farm",
            "",
            "NPR",
            transaction,
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            ""
        ).joinToString(field)

        val farm = FarmPersistenceCodec.decodeOrNull(payload)

        assertNotNull("6-part transaction should decode", farm)
        assertEquals(1, farm!!.transactions.size)
        assertNull(farm.transactions[0].activity)
    }

    @Test
    fun migratedSchema12EncodesBackAsSchema13() {
        val payload = "12${field}farm-s12b${field}Legacy Farm${field}${field}NPR${field}${field}${field}${field}${field}${field}${field}${field}${field}${field}${field}${field}"
        val decoded = FarmPersistenceCodec.decode(payload)
        val reEncoded = FarmPersistenceCodec.encode(decoded)

        assert(reEncoded.startsWith("13${field}"))
        val decodedAgain = FarmPersistenceCodec.decode(reEncoded)
        assertEquals(decoded.id, decodedAgain.id)
        assertEquals(decoded.name, decodedAgain.name)
        assertEquals(emptyList<FarmActivityType>(), decodedAgain.activities)
    }

    @Test
    fun schema13TransactionWithUnknownActivityValueFailsDecodeLikeUnknownCategory() {
        val transaction = listOf(
            "tx-7",
            TransactionType.EXPENSE.name,
            TransactionCategory.SUPPLIES.name,
            "300",
            "Netting",
            "2024-03-03T12:00:00Z",
            "NOT_A_REAL_ACTIVITY"
        ).joinToString(tsep)
        val payload = listOf(
            "13",
            "farm-s13c",
            "Fishery Farm",
            "",
            "NPR",
            transaction,
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            ""
        ).joinToString(field)

        val farm = FarmPersistenceCodec.decodeOrNull(payload)

        assertNull("unknown activity value should fail decode, mirroring unknown categories", farm)
    }

    @Test
    fun farmActivityBreakdownSurvivesCodecRoundTrip() {
        val service = FarmSliceService(InMemoryFarmStore())
        val farm = service.createFarm(
            "Dairy & Crops",
            activities = listOf(FarmActivityType.CATTLE_BUFFALO_DAIRY, FarmActivityType.CROPS)
        )
        service.createTransaction(
            farm.id,
            com.susankhya.kisab.domain.FarmTransactionDraft(
                type = TransactionType.INCOME,
                category = TransactionCategory.SALES,
                amountMinor = 4000,
                description = "Milk",
                occurredAt = "2024-04-01T12:00:00Z",
                activity = FarmActivityType.CATTLE_BUFFALO_DAIRY
            )
        )
        val decoded = FarmPersistenceCodec.decode(FarmPersistenceCodec.encode(service.loadFarm(farm.id)!!))

        val breakdown = service.farmActivityBreakdown(decoded.id)
        assertEquals(1, breakdown.size)
        assertEquals(FarmActivityType.CATTLE_BUFFALO_DAIRY, breakdown[0].activity)
        assertEquals(4000L, breakdown[0].incomeMinor)
    }
}