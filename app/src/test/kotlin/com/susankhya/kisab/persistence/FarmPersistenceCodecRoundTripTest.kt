package com.susankhya.kisab.persistence

import com.susankhya.kisab.domain.FarmActivityType
import com.susankhya.kisab.domain.FarmEntry
import com.susankhya.kisab.domain.FarmEntryKind
import com.susankhya.kisab.domain.FarmProduct
import com.susankhya.kisab.domain.FarmState
import com.susankhya.kisab.domain.FarmSupply
import com.susankhya.kisab.domain.FarmTransaction
import com.susankhya.kisab.domain.Party
import com.susankhya.kisab.domain.PartyRole
import com.susankhya.kisab.domain.ProductSaleDetail
import com.susankhya.kisab.domain.ProductUnit
import com.susankhya.kisab.domain.ProductionAllocation
import com.susankhya.kisab.domain.ProductionAllocationType
import com.susankhya.kisab.domain.ProductionRecord
import com.susankhya.kisab.domain.ProductionSession
import com.susankhya.kisab.domain.Settlement
import com.susankhya.kisab.domain.SupplyPurchaseDetail
import com.susankhya.kisab.domain.SupplyUsage
import com.susankhya.kisab.domain.Trade
import com.susankhya.kisab.domain.TradeType
import com.susankhya.kisab.domain.TransactionCategory
import com.susankhya.kisab.domain.TransactionType
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FarmPersistenceCodecRoundTripTest {

    private val now = OffsetDateTime.now(ZoneOffset.UTC)

    @Test
    fun emptyFarmRoundTrips() {
        val farm = FarmState(id = "f1", name = "Empty Farm", currencyCode = "NPR")
        val encoded = FarmPersistenceCodec.encode(farm)
        val decoded = FarmPersistenceCodec.decode(encoded)

        assertEquals(farm.id, decoded.id)
        assertEquals(farm.name, decoded.name)
        assertEquals(farm.currencyCode, decoded.currencyCode)
        assertTrue(decoded.entries.isEmpty())
        assertTrue(decoded.transactions.isEmpty())
        assertTrue(decoded.parties.isEmpty())
        assertTrue(decoded.trades.isEmpty())
        assertTrue(decoded.settlements.isEmpty())
        assertTrue(decoded.products.isEmpty())
        assertTrue(decoded.supplies.isEmpty())
        assertTrue(decoded.productionRecords.isEmpty())
        assertTrue(decoded.productionAllocations.isEmpty())
        assertTrue(decoded.activities.isEmpty())
        assertTrue(decoded.disabledActivities.isEmpty())
    }

    @Test
    fun farmWithEntriesRoundTrips() {
        val farm = FarmState(
            id = "f1", name = "Farm", currencyCode = "USD",
            entries = mutableListOf(
                FarmEntry(FarmEntryKind.LIVESTOCK, "Goat", 5),
                FarmEntry(FarmEntryKind.CROP, "Rice", 100)
            )
        )
        val decoded = FarmPersistenceCodec.decode(FarmPersistenceCodec.encode(farm))

        assertEquals(2, decoded.entries.size)
        assertEquals(FarmEntryKind.LIVESTOCK, decoded.entries[0].kind)
        assertEquals("Goat", decoded.entries[0].label)
        assertEquals(5, decoded.entries[0].quantity)
        assertEquals(FarmEntryKind.CROP, decoded.entries[1].kind)
        assertEquals("Rice", decoded.entries[1].label)
        assertEquals(100, decoded.entries[1].quantity)
    }

    @Test
    fun farmWithTransactionsRoundTrips() {
        val tx1 = FarmTransaction(
            id = "tx1", type = TransactionType.EXPENSE, category = TransactionCategory.FEED,
            amountMinor = 5000, description = "Chicken feed", occurredAt = now
        )
        val tx2 = FarmTransaction(
            id = "tx2", type = TransactionType.INCOME, category = TransactionCategory.SALES,
            amountMinor = 8000, description = "Egg sale", occurredAt = now, activity = FarmActivityType.POULTRY
        )
        val farm = FarmState(
            id = "f1", name = "Farm",
            transactions = mutableListOf(tx1, tx2)
        )
        val decoded = FarmPersistenceCodec.decode(FarmPersistenceCodec.encode(farm))

        assertEquals(2, decoded.transactions.size)
        assertEquals("tx1", decoded.transactions[0].id)
        assertEquals(TransactionType.EXPENSE, decoded.transactions[0].type)
        assertEquals(TransactionCategory.FEED, decoded.transactions[0].category)
        assertEquals(5000L, decoded.transactions[0].amountMinor)
        assertEquals("Chicken feed", decoded.transactions[0].description)
        assertNull(decoded.transactions[0].activity)

        assertEquals("tx2", decoded.transactions[1].id)
        assertEquals(FarmActivityType.POULTRY, decoded.transactions[1].activity)
    }

    @Test
    fun farmWithPartiesRoundTrips() {
        val party1 = Party(id = "p1", name = "Ram", role = PartyRole.CUSTOMER, contact = "9841234567", notes = "Regular buyer")
        val party2 = Party(id = "p2", name = "Shyam", role = PartyRole.SUPPLIER)
        val farm = FarmState(
            id = "f1", name = "Farm",
            parties = mutableListOf(party1, party2)
        )
        val decoded = FarmPersistenceCodec.decode(FarmPersistenceCodec.encode(farm))

        assertEquals(2, decoded.parties.size)
        assertEquals("Ram", decoded.parties[0].name)
        assertEquals(PartyRole.CUSTOMER, decoded.parties[0].role)
        assertEquals("9841234567", decoded.parties[0].contact)
        assertEquals("Regular buyer", decoded.parties[0].notes)
        assertEquals("Shyam", decoded.parties[1].name)
        assertEquals(PartyRole.SUPPLIER, decoded.parties[1].role)
        assertEquals("", decoded.parties[1].contact)
    }

    @Test
    fun farmWithTradesRoundTrips() {
        val trade1 = Trade(
            id = "t1", type = TradeType.SALE, partyId = "p1",
            totalMinor = 10000, description = "Milk sale", occurredAt = now
        )
        val trade2 = Trade(
            id = "t2", type = TradeType.PURCHASE, partyId = "p2",
            totalMinor = 5000, description = "Feed purchase", occurredAt = now, activity = FarmActivityType.POULTRY
        )
        val farm = FarmState(
            id = "f1", name = "Farm",
            trades = mutableListOf(trade1, trade2)
        )
        val decoded = FarmPersistenceCodec.decode(FarmPersistenceCodec.encode(farm))

        assertEquals(2, decoded.trades.size)
        assertEquals("t1", decoded.trades[0].id)
        assertEquals(TradeType.SALE, decoded.trades[0].type)
        assertEquals("p1", decoded.trades[0].partyId)
        assertEquals(10000L, decoded.trades[0].totalMinor)
        assertEquals("Milk sale", decoded.trades[0].description)
        assertNull(decoded.trades[0].activity)

        assertEquals("t2", decoded.trades[1].id)
        assertEquals(TradeType.PURCHASE, decoded.trades[1].type)
        assertEquals(FarmActivityType.POULTRY, decoded.trades[1].activity)
    }

    @Test
    fun farmWithSettlementsRoundTrips() {
        val settlement = Settlement(
            id = "s1", tradeId = "t1", amountMinor = 5000,
            occurredAt = now, note = "Partial payment", isInitialPayment = true
        )
        val trade = Trade(
            id = "t1", type = TradeType.SALE, partyId = "p1",
            totalMinor = 10000, description = "Sale", occurredAt = now
        )
        val farm = FarmState(
            id = "f1", name = "Farm",
            trades = mutableListOf(trade),
            settlements = mutableListOf(settlement)
        )
        val decoded = FarmPersistenceCodec.decode(FarmPersistenceCodec.encode(farm))

        assertEquals(1, decoded.settlements.size)
        assertEquals("s1", decoded.settlements[0].id)
        assertEquals("t1", decoded.settlements[0].tradeId)
        assertEquals(5000L, decoded.settlements[0].amountMinor)
        assertEquals("Partial payment", decoded.settlements[0].note)
        assertEquals(true, decoded.settlements[0].isInitialPayment)
    }

    @Test
    fun farmWithProductsRoundTrips() {
        val product1 = FarmProduct(id = "prod1", name = "Milk", defaultUnit = ProductUnit.LITRE)
        val product2 = FarmProduct(id = "prod2", name = "Eggs", defaultUnit = ProductUnit.CUSTOM, customUnitLabel = "Dozen")
        val farm = FarmState(
            id = "f1", name = "Farm",
            products = mutableListOf(product1, product2)
        )
        val decoded = FarmPersistenceCodec.decode(FarmPersistenceCodec.encode(farm))

        assertEquals(2, decoded.products.size)
        assertEquals("Milk", decoded.products[0].name)
        assertEquals(ProductUnit.LITRE, decoded.products[0].defaultUnit)
        assertEquals("Eggs", decoded.products[1].name)
        assertEquals(ProductUnit.CUSTOM, decoded.products[1].defaultUnit)
        assertEquals("Dozen", decoded.products[1].customUnitLabel)
    }

    @Test
    fun farmWithProductSaleDetailsRoundTrips() {
        val detail = ProductSaleDetail(
            tradeId = "t1", productId = "prod1",
            quantity = BigDecimal("10.5"), unit = ProductUnit.LITRE,
            rateMinor = 500
        )
        val trade = Trade(
            id = "t1", type = TradeType.SALE, partyId = "p1",
            totalMinor = 5250, description = "", occurredAt = now
        )
        val farm = FarmState(
            id = "f1", name = "Farm",
            trades = mutableListOf(trade),
            products = mutableListOf(FarmProduct(id = "prod1", name = "Milk", defaultUnit = ProductUnit.LITRE)),
            productSaleDetails = mutableListOf(detail)
        )
        val decoded = FarmPersistenceCodec.decode(FarmPersistenceCodec.encode(farm))

        assertEquals(1, decoded.productSaleDetails.size)
        val d = decoded.productSaleDetails[0]
        assertEquals("t1", d.tradeId)
        assertEquals("prod1", d.productId)
        assertEquals(BigDecimal("10.5"), d.quantity)
        assertEquals(ProductUnit.LITRE, d.unit)
        assertEquals(500L, d.rateMinor)
    }

    @Test
    fun farmWithSuppliesRoundTrips() {
        val supply = FarmSupply(id = "sup1", name = "Feed", unit = ProductUnit.BAG, customUnitLabel = "")
        val farm = FarmState(
            id = "f1", name = "Farm",
            supplies = mutableListOf(supply)
        )
        val decoded = FarmPersistenceCodec.decode(FarmPersistenceCodec.encode(farm))

        assertEquals(1, decoded.supplies.size)
        assertEquals("Feed", decoded.supplies[0].name)
        assertEquals(ProductUnit.BAG, decoded.supplies[0].unit)
    }

    @Test
    fun farmWithSupplyPurchaseDetailsRoundTrips() {
        val detail = SupplyPurchaseDetail(
            transactionId = "tx1", supplyId = "sup1",
            quantity = BigDecimal("20"), unit = ProductUnit.BAG
        )
        val tx = FarmTransaction(
            id = "tx1", type = TransactionType.EXPENSE, category = TransactionCategory.SUPPLIES,
            amountMinor = 40000, description = "Feed bags", occurredAt = now
        )
        val farm = FarmState(
            id = "f1", name = "Farm",
            transactions = mutableListOf(tx),
            supplies = mutableListOf(FarmSupply(id = "sup1", name = "Feed", unit = ProductUnit.BAG)),
            supplyPurchaseDetails = mutableListOf(detail)
        )
        val decoded = FarmPersistenceCodec.decode(FarmPersistenceCodec.encode(farm))

        assertEquals(1, decoded.supplyPurchaseDetails.size)
        val d = decoded.supplyPurchaseDetails[0]
        assertEquals("tx1", d.transactionId)
        assertEquals("sup1", d.supplyId)
        assertEquals(BigDecimal("20"), d.quantity)
        assertNull(d.purchaseTradeId)
    }

    @Test
    fun farmWithSupplyPurchaseDetailViaTradeRoundTrips() {
        val detail = SupplyPurchaseDetail(
            transactionId = null, purchaseTradeId = "t1", supplyId = "sup1",
            quantity = BigDecimal("10"), unit = ProductUnit.BAG
        )
        val trade = Trade(
            id = "t1", type = TradeType.PURCHASE, partyId = "p1",
            totalMinor = 20000, description = "Feed", occurredAt = now
        )
        val farm = FarmState(
            id = "f1", name = "Farm",
            trades = mutableListOf(trade),
            supplies = mutableListOf(FarmSupply(id = "sup1", name = "Feed", unit = ProductUnit.BAG)),
            supplyPurchaseDetails = mutableListOf(detail)
        )
        val decoded = FarmPersistenceCodec.decode(FarmPersistenceCodec.encode(farm))

        val d = decoded.supplyPurchaseDetails[0]
        assertNull(d.transactionId)
        assertEquals("t1", d.purchaseTradeId)
    }

    @Test
    fun farmWithSupplyUsagesRoundTrips() {
        val usage = SupplyUsage(
            id = "u1", supplyId = "sup1",
            quantity = BigDecimal("5"), unit = ProductUnit.BAG,
            occurredAt = now, note = "Monday feeding"
        )
        val farm = FarmState(
            id = "f1", name = "Farm",
            supplies = mutableListOf(FarmSupply(id = "sup1", name = "Feed", unit = ProductUnit.BAG)),
            supplyUsages = mutableListOf(usage)
        )
        val decoded = FarmPersistenceCodec.decode(FarmPersistenceCodec.encode(farm))

        assertEquals(1, decoded.supplyUsages.size)
        val u = decoded.supplyUsages[0]
        assertEquals("u1", u.id)
        assertEquals("sup1", u.supplyId)
        assertEquals(BigDecimal("5"), u.quantity)
        assertEquals("Monday feeding", u.note)
    }

    @Test
    fun farmWithProductionRecordsRoundTrips() {
        val record = ProductionRecord(
            id = "pr1", productId = "prod1",
            quantity = BigDecimal("50.25"), unit = ProductUnit.LITRE,
            occurredAt = now, session = ProductionSession.MORNING, note = "Morning milk"
        )
        val farm = FarmState(
            id = "f1", name = "Farm",
            products = mutableListOf(FarmProduct(id = "prod1", name = "Milk", defaultUnit = ProductUnit.LITRE)),
            productionRecords = mutableListOf(record)
        )
        val decoded = FarmPersistenceCodec.decode(FarmPersistenceCodec.encode(farm))

        assertEquals(1, decoded.productionRecords.size)
        val r = decoded.productionRecords[0]
        assertEquals("pr1", r.id)
        assertEquals("prod1", r.productId)
        assertEquals(BigDecimal("50.25"), r.quantity)
        assertEquals(ProductionSession.MORNING, r.session)
        assertEquals("Morning milk", r.note)
    }

    @Test
    fun farmWithProductionAllocationsRoundTrips() {
        val allocation = ProductionAllocation(
            id = "pa1", productId = "prod1",
            quantity = BigDecimal("10"), unit = ProductUnit.LITRE,
            occurredAt = now, type = ProductionAllocationType.HOME_USE, note = "Home consumption"
        )
        val farm = FarmState(
            id = "f1", name = "Farm",
            products = mutableListOf(FarmProduct(id = "prod1", name = "Milk", defaultUnit = ProductUnit.LITRE)),
            productionAllocations = mutableListOf(allocation)
        )
        val decoded = FarmPersistenceCodec.decode(FarmPersistenceCodec.encode(farm))

        assertEquals(1, decoded.productionAllocations.size)
        val a = decoded.productionAllocations[0]
        assertEquals("pa1", a.id)
        assertEquals(BigDecimal("10"), a.quantity)
        assertEquals(ProductionAllocationType.HOME_USE, a.type)
        assertEquals("Home consumption", a.note)
    }

    @Test
    fun farmWithActivitiesRoundTrips() {
        val farm = FarmState(
            id = "f1", name = "Farm",
            activities = mutableListOf(FarmActivityType.POULTRY, FarmActivityType.CROPS),
            disabledActivities = mutableListOf(FarmActivityType.FISHERY)
        )
        val decoded = FarmPersistenceCodec.decode(FarmPersistenceCodec.encode(farm))

        assertEquals(2, decoded.activities.size)
        assertTrue(decoded.activities.containsAll(listOf(FarmActivityType.POULTRY, FarmActivityType.CROPS)))
        assertEquals(1, decoded.disabledActivities.size)
        assertEquals(FarmActivityType.FISHERY, decoded.disabledActivities[0])
    }

    @Test
    fun fullFarmRoundTrips() {
        val farm = FarmState(
            id = "farm-full", name = "Full Farm", currencyCode = "NPR",
            entries = mutableListOf(FarmEntry(FarmEntryKind.LIVESTOCK, "Cow", 3)),
            transactions = mutableListOf(
                FarmTransaction(id = "tx1", type = TransactionType.EXPENSE, category = TransactionCategory.FEED,
                    amountMinor = 3000, description = "Hay", occurredAt = now, activity = FarmActivityType.CATTLE_BUFFALO_DAIRY)
            ),
            parties = mutableListOf(Party(id = "p1", name = "Ram", role = PartyRole.CUSTOMER)),
            trades = mutableListOf(
                Trade(id = "t1", type = TradeType.SALE, partyId = "p1", totalMinor = 15000,
                    description = "Milk sale", occurredAt = now, activity = FarmActivityType.CATTLE_BUFFALO_DAIRY)
            ),
            settlements = mutableListOf(
                Settlement(id = "s1", tradeId = "t1", amountMinor = 10000, occurredAt = now, note = "Cash", isInitialPayment = true)
            ),
            products = mutableListOf(FarmProduct(id = "prod1", name = "Milk", defaultUnit = ProductUnit.LITRE)),
            productSaleDetails = mutableListOf(
                ProductSaleDetail(tradeId = "t1", productId = "prod1", quantity = BigDecimal("50"),
                    unit = ProductUnit.LITRE, rateMinor = 300)
            ),
            supplies = mutableListOf(FarmSupply(id = "sup1", name = "Hay", unit = ProductUnit.BAG)),
            supplyPurchaseDetails = mutableListOf(
                SupplyPurchaseDetail(transactionId = "tx1", supplyId = "sup1", quantity = BigDecimal("10"),
                    unit = ProductUnit.BAG)
            ),
            supplyUsages = mutableListOf(
                SupplyUsage(id = "u1", supplyId = "sup1", quantity = BigDecimal("2"), unit = ProductUnit.BAG,
                    occurredAt = now, note = "Daily")
            ),
            productionRecords = mutableListOf(
                ProductionRecord(id = "pr1", productId = "prod1", quantity = BigDecimal("60"),
                    unit = ProductUnit.LITRE, occurredAt = now, session = ProductionSession.MORNING)
            ),
            productionAllocations = mutableListOf(
                ProductionAllocation(id = "pa1", productId = "prod1", quantity = BigDecimal("5"),
                    unit = ProductUnit.LITRE, occurredAt = now, type = ProductionAllocationType.HOME_USE)
            ),
            activities = mutableListOf(FarmActivityType.CATTLE_BUFFALO_DAIRY),
            disabledActivities = mutableListOf(FarmActivityType.FISHERY)
        )
        val decoded = FarmPersistenceCodec.decode(FarmPersistenceCodec.encode(farm))

        assertEquals(farm.id, decoded.id)
        assertEquals(farm.name, decoded.name)
        assertEquals(farm.currencyCode, decoded.currencyCode)
        assertEquals(1, decoded.entries.size)
        assertEquals(1, decoded.transactions.size)
        assertEquals(FarmActivityType.CATTLE_BUFFALO_DAIRY, decoded.transactions[0].activity)
        assertEquals(1, decoded.parties.size)
        assertEquals(1, decoded.trades.size)
        assertEquals(FarmActivityType.CATTLE_BUFFALO_DAIRY, decoded.trades[0].activity)
        assertEquals(1, decoded.settlements.size)
        assertEquals(1, decoded.products.size)
        assertEquals(1, decoded.productSaleDetails.size)
        assertEquals(1, decoded.supplies.size)
        assertEquals(1, decoded.supplyPurchaseDetails.size)
        assertEquals(1, decoded.supplyUsages.size)
        assertEquals(1, decoded.productionRecords.size)
        assertEquals(1, decoded.productionAllocations.size)
        assertEquals(1, decoded.activities.size)
        assertEquals(1, decoded.disabledActivities.size)
    }

    @Test
    fun tradeWithNullPartyIdRoundTrips() {
        val trade = Trade(
            id = "t1", type = TradeType.SALE, partyId = null,
            totalMinor = 5000, description = "Cash sale", occurredAt = now
        )
        val farm = FarmState(id = "f1", name = "Farm", trades = mutableListOf(trade))
        val decoded = FarmPersistenceCodec.decode(FarmPersistenceCodec.encode(farm))

        assertNull(decoded.trades[0].partyId)
    }

    @Test
    fun emptyFieldsDecodeToEmptyLists() {
        val encoded = "14\u001Ff1\u001FFarm\u001F\u001FNPR\u001F\u001F\u001F\u001F\u001F\u001F\u001F\u001F\u001F\u001F\u001F\u001F\u001F"
        val decoded = FarmPersistenceCodec.decode(encoded)

        assertEquals("f1", decoded.id)
        assertEquals("Farm", decoded.name)
        assertTrue(decoded.entries.isEmpty())
        assertTrue(decoded.transactions.isEmpty())
        assertTrue(decoded.parties.isEmpty())
        assertTrue(decoded.trades.isEmpty())
        assertTrue(decoded.settlements.isEmpty())
        assertTrue(decoded.products.isEmpty())
        assertTrue(decoded.supplies.isEmpty())
        assertTrue(decoded.productionRecords.isEmpty())
        assertTrue(decoded.productionAllocations.isEmpty())
    }

    @Test
    fun decodeOrNullReturnsNullForGarbage() {
        assertNull(FarmPersistenceCodec.decodeOrNull("not-a-farm-payload"))
    }

    @Test
    fun decodeOrNullReturnsNullForUnsupportedVersion() {
        assertNull(FarmPersistenceCodec.decodeOrNull("99\u001Ff1\u001FFarm\u001F\u001FNPR"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun decodeThrowsForGarbage() {
        FarmPersistenceCodec.decode("garbage")
    }

    @Test
    fun schemaVersion14IsCurrent() {
        assertEquals(14, FarmPersistenceCodec.CURRENT_SCHEMA_VERSION)
    }
}
