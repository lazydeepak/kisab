package com.susankhya.kisab.persistence

import com.susankhya.kisab.domain.FarmActivityType
import com.susankhya.kisab.domain.FarmSliceService
import com.susankhya.kisab.domain.FarmState
import com.susankhya.kisab.domain.FarmTransaction
import com.susankhya.kisab.domain.InMemoryFarmStore
import com.susankhya.kisab.domain.TransactionCategory
import com.susankhya.kisab.domain.TransactionType
import com.susankhya.kisab.domain.TradeType
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun migratedSchema12EncodesBackAsCurrentSchema() {
        val payload = "12${field}farm-s12b${field}Legacy Farm${field}${field}NPR${field}${field}${field}${field}${field}${field}${field}${field}${field}${field}${field}${field}"
        val decoded = FarmPersistenceCodec.decode(payload)
        val reEncoded = FarmPersistenceCodec.encode(decoded)

        assert(reEncoded.startsWith("${FarmPersistenceCodec.CURRENT_SCHEMA_VERSION}${field}"))
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

    @Test
    fun schema14RoundTripPreservesTradeActivityAndSettlementAttribution() {
        val service = FarmSliceService(InMemoryFarmStore())
        val farm = service.createFarm(
            "Poultry Farm",
            activities = listOf(FarmActivityType.POULTRY, FarmActivityType.CROPS)
        )
        service.addParty(farm.id, com.susankhya.kisab.domain.PartyDraft(name = "Ram", role = com.susankhya.kisab.domain.PartyRole.CUSTOMER))
        service.addProduct(farm.id, "Eggs", com.susankhya.kisab.domain.ProductUnit.PIECE)
        service.addProductSale(
            farm.id, service.parties(farm.id).single().id, service.products(farm.id).single().id,
            quantity = BigDecimal("10"), rateMinor = 500,
            initialPaymentMinor = 2000, occurredAt = "2024-06-01T12:00:00Z",
            activity = FarmActivityType.POULTRY
        )
        service.recordCustomerPayment(farm.id, service.parties(farm.id).single().id, 1000, "2024-06-15T12:00:00Z")

        val encoded = FarmPersistenceCodec.encode(service.loadFarm(farm.id)!!)
        val decoded = FarmPersistenceCodec.decode(encoded)

        assertEquals(14, decoded.schemaVersion)
        val trade = decoded.trades.single()
        assertEquals(FarmActivityType.POULTRY, trade.activity)
        assertEquals(5000L, trade.totalMinor)

        val breakdown = service.farmActivityBreakdown(decoded.id)
        val poultry = breakdown.first { it.activity == FarmActivityType.POULTRY }
        assertEquals(5000L, poultry.grossSalesMinor)
        assertEquals(3000L, poultry.paymentsReceivedMinor)
        assertEquals(1, breakdown.size)
    }

    @Test
    fun schema13PayloadSixPartTradesDecodeAsGeneralAndReEncodeAsSchema14() {
        val trade = listOf(
            "t-1",
            TradeType.SALE.name,
            "party-1",
            "5000",
            "Legacy sale",
            "2024-03-04T12:00:00Z"
        ).joinToString(tsep)
        val payload = listOf(
            "13",
            "farm-s13d",
            "Legacy Farm",
            "",
            "NPR",
            "",
            "",
            trade,
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

        assertNotNull("schema-13 payload with 6-part trades should decode", farm)
        assertEquals(1, farm!!.trades.size)
        assertEquals(5000L, farm.trades[0].totalMinor)
        assertEquals(TradeType.SALE, farm.trades[0].type)
        assertNull("legacy trade has no activity, so it is general", farm.trades[0].activity)

        val reEncoded = FarmPersistenceCodec.encode(farm)
        assertTrue("re-encode upgrades to schema 14", reEncoded.startsWith("14${field}"))
        val reEncodedTrades = reEncoded.split(field)[7]
        val reEncodedTradeParts = reEncodedTrades.split(record).single().split(tsep)
        assertEquals("re-encode writes the trailing activity part", 7, reEncodedTradeParts.size)
        assertEquals("legacy trade re-encodes with a blank activity (General)", "", reEncodedTradeParts[6])

        val decodedAgain = FarmPersistenceCodec.decode(reEncoded)
        assertNull(decodedAgain.trades[0].activity)
        assertEquals(5000L, decodedAgain.trades[0].totalMinor)
    }

    @Test
    fun schema14PayloadSevenPartTradeDecodesActivity() {
        val trade = listOf(
            "t-2",
            TradeType.PURCHASE.name,
            "party-2",
            "3000",
            "Feed",
            "2024-06-10T12:00:00Z",
            FarmActivityType.CROPS.name
        ).joinToString(tsep)
        val payload = listOf(
            "14",
            "farm-s14",
            "Crops Farm",
            "",
            "NPR",
            "",
            "",
            trade,
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

        assertNotNull("schema-14 payload should decode", farm)
        assertEquals(14, farm!!.schemaVersion)
        val decodedTrade = farm.trades.single()
        assertEquals(FarmActivityType.CROPS, decodedTrade.activity)
        assertEquals(3000L, decodedTrade.totalMinor)
    }

    @Test
    fun schema14TradeWithUnknownActivityValueFailsDecode() {
        val trade = listOf(
            "t-3",
            TradeType.SALE.name,
            "party-3",
            "1000",
            "Sale",
            "2024-06-11T12:00:00Z",
            "NOT_A_REAL_ACTIVITY"
        ).joinToString(tsep)
        val payload = listOf(
            "14",
            "farm-s14b",
            "Broken Farm",
            "",
            "NPR",
            "",
            "",
            trade,
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

        assertNull(
            "unknown trade activity value should fail decode, mirroring transaction categories",
            FarmPersistenceCodec.decodeOrNull(payload)
        )
    }

    @Test
    fun backupRoundTripPreservesTradeActivity() {
        val service = FarmSliceService(InMemoryFarmStore())
        val farm = service.createFarm(
            "Poultry Farm",
            activities = listOf(FarmActivityType.POULTRY)
        )
        service.addParty(farm.id, com.susankhya.kisab.domain.PartyDraft(name = "Sita", role = com.susankhya.kisab.domain.PartyRole.SUPPLIER))
        service.addSupply(farm.id, "Feed", com.susankhya.kisab.domain.ProductUnit.KILOGRAM)
        service.addSupplierPurchase(
            farm.id, service.parties(farm.id).single().id, service.supplies(farm.id).single().id,
            quantity = BigDecimal("10"), unit = com.susankhya.kisab.domain.ProductUnit.KILOGRAM,
            amountMinor = 3000, initialPaymentMinor = 1000,
            occurredAt = "2024-06-20T12:00:00Z", description = "Feed",
            activity = FarmActivityType.POULTRY
        )

        val encoded = FarmBackupCodec.encode(service.loadFarm(farm.id)!!, exportedAt = OffsetDateTime.parse("2024-07-01T00:00:00Z"))
        val envelope = FarmBackupCodec.decode(encoded)

        assertEquals(14, envelope.farm.schemaVersion)
        val trade = envelope.farm.trades.single()
        assertEquals(FarmActivityType.POULTRY, trade.activity)
        assertEquals(3000L, trade.totalMinor)

        val reEncoded = FarmBackupCodec.encode(envelope.farm, exportedAt = OffsetDateTime.parse("2024-07-01T00:00:00Z"))
        assertEquals("backup round trip is byte-stable", encoded, reEncoded)
    }
}