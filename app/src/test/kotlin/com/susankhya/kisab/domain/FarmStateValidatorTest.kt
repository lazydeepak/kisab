package com.susankhya.kisab.domain

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FarmStateValidatorTest {

    private val now = OffsetDateTime.now(ZoneOffset.UTC)

    private fun validFarm(overrides: Map<String, Any?> = emptyMap()): FarmState = FarmState(
        id = overrides["id"] as? String ?: "farm-1",
        name = overrides["name"] as? String ?: "Test Farm",
        currencyCode = overrides["currencyCode"] as? String ?: "NPR",
        entries = (overrides["entries"] as? MutableList<FarmEntry>) ?: mutableListOf(),
        transactions = (overrides["transactions"] as? MutableList<FarmTransaction>) ?: mutableListOf(),
        parties = (overrides["parties"] as? MutableList<Party>) ?: mutableListOf(),
        trades = (overrides["trades"] as? MutableList<Trade>) ?: mutableListOf(),
        settlements = (overrides["settlements"] as? MutableList<Settlement>) ?: mutableListOf(),
        products = (overrides["products"] as? MutableList<FarmProduct>) ?: mutableListOf(),
        productSaleDetails = (overrides["productSaleDetails"] as? MutableList<ProductSaleDetail>) ?: mutableListOf(),
        supplies = (overrides["supplies"] as? MutableList<FarmSupply>) ?: mutableListOf(),
        supplyPurchaseDetails = (overrides["supplyPurchaseDetails"] as? MutableList<SupplyPurchaseDetail>) ?: mutableListOf(),
        supplyUsages = (overrides["supplyUsages"] as? MutableList<SupplyUsage>) ?: mutableListOf(),
        productionRecords = (overrides["productionRecords"] as? MutableList<ProductionRecord>) ?: mutableListOf(),
        productionAllocations = (overrides["productionAllocations"] as? MutableList<ProductionAllocation>) ?: mutableListOf(),
        activities = (overrides["activities"] as? MutableList<FarmActivityType>) ?: mutableListOf(),
        disabledActivities = (overrides["disabledActivities"] as? MutableList<FarmActivityType>) ?: mutableListOf(),
    )

    private fun validTransaction(id: String = "tx-1"): FarmTransaction = FarmTransaction(
        id = id,
        type = TransactionType.EXPENSE,
        category = TransactionCategory.FEED,
        amountMinor = 1000,
        description = "Feed",
        occurredAt = now
    )

    private fun validParty(id: String = "party-1", role: PartyRole = PartyRole.SUPPLIER): Party = Party(
        id = id,
        name = "Test Party",
        role = role
    )

    private fun validTrade(
        id: String = "trade-1",
        type: TradeType = TradeType.PURCHASE,
        partyId: String? = "party-1",
        totalMinor: Long = 5000
    ): Trade = Trade(
        id = id,
        type = type,
        partyId = partyId,
        totalMinor = totalMinor,
        description = "Test trade",
        occurredAt = now
    )

    private fun validSettlement(id: String = "settle-1", tradeId: String = "trade-1", amountMinor: Long = 5000): Settlement = Settlement(
        id = id,
        tradeId = tradeId,
        amountMinor = amountMinor,
        occurredAt = now,
        note = ""
    )

    private fun validProduct(id: String = "prod-1"): FarmProduct = FarmProduct(
        id = id,
        name = "Milk",
        defaultUnit = ProductUnit.LITRE
    )

    private fun validSupply(id: String = "supply-1"): FarmSupply = FarmSupply(
        id = id,
        name = "Feed",
        unit = ProductUnit.KILOGRAM
    )

    // ── validateFarm ──────────────────────────────────────────────

    @Test
    fun validFarmPasses() {
        FarmStateValidator.validateFarm(validFarm())
    }

    @Test
    fun rejectsBlankFarmId() {
        try {
            FarmStateValidator.validateFarm(validFarm(mapOf("id" to "   ")))
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Farm id is required", e.message)
        }
    }

    @Test
    fun rejectsBlankFarmName() {
        try {
            FarmStateValidator.validateFarm(validFarm(mapOf("name" to "   ")))
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Farm name is required", e.message)
        }
    }

    @Test
    fun rejectsInvalidCurrencyCode() {
        try {
            FarmStateValidator.validateFarm(validFarm(mapOf("currencyCode" to "usd")))
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Farm currency must be a 3-letter ISO code", e.message)
        }
    }

    @Test
    fun rejectsDuplicateTransactionIds() {
        val farm = validFarm(mapOf(
            "transactions" to mutableListOf(
                validTransaction(id = "dup"),
                validTransaction(id = "dup")
            )
        ))
        try {
            FarmStateValidator.validateFarm(farm)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Transaction IDs must be unique", e.message)
        }
    }

    @Test
    fun rejectsDuplicatePartyIds() {
        val farm = validFarm(mapOf(
            "parties" to mutableListOf(
                validParty(id = "dup"),
                validParty(id = "dup")
            )
        ))
        try {
            FarmStateValidator.validateFarm(farm)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Party IDs must be unique", e.message)
        }
    }

    @Test
    fun rejectsDuplicateTradeIds() {
        val party = validParty()
        val farm = validFarm(mapOf(
            "parties" to mutableListOf(party),
            "trades" to mutableListOf(
                validTrade(id = "dup"),
                validTrade(id = "dup")
            )
        ))
        try {
            FarmStateValidator.validateFarm(farm)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Trade IDs must be unique", e.message)
        }
    }

    @Test
    fun rejectsDuplicateSettlementIds() {
        val party = validParty()
        val trade = validTrade(totalMinor = 10000)
        val farm = validFarm(mapOf(
            "parties" to mutableListOf(party),
            "trades" to mutableListOf(trade),
            "settlements" to mutableListOf(
                validSettlement(id = "dup", amountMinor = 1000),
                validSettlement(id = "dup", amountMinor = 1000)
            )
        ))
        try {
            FarmStateValidator.validateFarm(farm)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Settlement IDs must be unique", e.message)
        }
    }

    @Test
    fun rejectsDuplicateProductIds() {
        val farm = validFarm(mapOf(
            "products" to mutableListOf(
                validProduct(id = "dup"),
                validProduct(id = "dup")
            )
        ))
        try {
            FarmStateValidator.validateFarm(farm)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Product IDs must be unique", e.message)
        }
    }

    @Test
    fun rejectsDuplicateSupplyIds() {
        val farm = validFarm(mapOf(
            "supplies" to mutableListOf(
                validSupply(id = "dup"),
                validSupply(id = "dup")
            )
        ))
        try {
            FarmStateValidator.validateFarm(farm)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Supply IDs must be unique", e.message)
        }
    }

    @Test
    fun rejectsDuplicateProductSaleDetailTradeIds() {
        val product = validProduct()
        val trade = validTrade(type = TradeType.SALE, partyId = "party-1")
        val party = validParty(role = PartyRole.CUSTOMER)
        val detail = ProductSaleDetail(
            tradeId = "trade-1",
            productId = "prod-1",
            quantity = BigDecimal("10"),
            unit = ProductUnit.LITRE,
            rateMinor = 500
        )
        val farm = validFarm(mapOf(
            "products" to mutableListOf(product),
            "parties" to mutableListOf(party),
            "trades" to mutableListOf(trade),
            "productSaleDetails" to mutableListOf(detail, detail.copy())
        ))
        try {
            FarmStateValidator.validateFarm(farm)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Product sale details must be unique per trade", e.message)
        }
    }

    // ── validateTransaction ───────────────────────────────────────

    @Test
    fun validTransactionPasses() {
        FarmStateValidator.validateTransaction(validTransaction())
    }

    @Test
    fun rejectsBlankTransactionId() {
        try {
            FarmStateValidator.validateTransaction(validTransaction(id = "   "))
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Transaction id is required", e.message)
        }
    }

    @Test
    fun rejectsBlankTransactionDescription() {
        try {
            FarmStateValidator.validateTransaction(
                validTransaction().copy(description = "   ")
            )
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Transaction description is required", e.message)
        }
    }

    @Test
    fun rejectsZeroTransactionAmount() {
        try {
            FarmStateValidator.validateTransaction(
                validTransaction().copy(amountMinor = 0)
            )
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Transaction amount must be positive", e.message)
        }
    }

    @Test
    fun rejectsMismatchedTransactionCategoryAndType() {
        try {
            FarmStateValidator.validateTransaction(
                validTransaction().copy(type = TransactionType.INCOME, category = TransactionCategory.FEED)
            )
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Transaction category is invalid for the selected type", e.message)
        }
    }

    // ── validateParty ─────────────────────────────────────────────

    @Test
    fun validPartyPasses() {
        FarmStateValidator.validateParty(validParty())
    }

    @Test
    fun rejectsBlankPartyId() {
        try {
            FarmStateValidator.validateParty(validParty(id = "   "))
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Party id is required", e.message)
        }
    }

    @Test
    fun rejectsBlankPartyName() {
        try {
            FarmStateValidator.validateParty(validParty().copy(name = "   "))
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Party name is required", e.message)
        }
    }

    // ── validateTrade ─────────────────────────────────────────────

    @Test
    fun validTradePasses() {
        val farm = validFarm(mapOf(
            "parties" to mutableListOf(validParty()),
            "trades" to mutableListOf(validTrade())
        ))
        FarmStateValidator.validateTrade(farm, validTrade())
    }

    @Test
    fun rejectsBlankTradeId() {
        val farm = validFarm(mapOf(
            "parties" to mutableListOf(validParty()),
            "trades" to mutableListOf(validTrade(id = "   "))
        ))
        try {
            FarmStateValidator.validateTrade(farm, validTrade(id = "   "))
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Trade id is required", e.message)
        }
    }

    @Test
    fun rejectsZeroTradeTotal() {
        val farm = validFarm(mapOf(
            "parties" to mutableListOf(validParty()),
        ))
        try {
            FarmStateValidator.validateTrade(farm, validTrade(totalMinor = 0))
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Trade total must be positive", e.message)
        }
    }

    @Test
    fun rejectsSettledAmountExceedingTradeTotal() {
        val trade = validTrade(totalMinor = 1000)
        val settlement = validSettlement(amountMinor = 2000)
        val farm = validFarm(mapOf(
            "parties" to mutableListOf(validParty()),
            "trades" to mutableListOf(trade),
            "settlements" to mutableListOf(settlement)
        ))
        try {
            FarmStateValidator.validateTrade(farm, trade)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Trade total cannot be less than the settled amount", e.message)
        }
    }

    @Test
    fun rejectsUnpaidTradeWithoutParty() {
        try {
            val farm = validFarm(mapOf(
                "trades" to mutableListOf(validTrade(partyId = null))
            ))
            FarmStateValidator.validateTrade(farm, validTrade(partyId = null))
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Partially paid or unpaid trades require a party", e.message)
        }
    }

    @Test
    fun rejectsTradeWithNonexistentParty() {
        val farm = validFarm(mapOf(
            "trades" to mutableListOf(validTrade(partyId = "nonexistent"))
        ))
        try {
            FarmStateValidator.validateTrade(farm, validTrade(partyId = "nonexistent"))
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Trade party not found: nonexistent", e.message)
        }
    }

    @Test
    fun rejectsIncompatiblePartyRoleForSale() {
        val party = validParty(role = PartyRole.SUPPLIER)
        val trade = validTrade(type = TradeType.SALE, partyId = "party-1")
        val farm = validFarm(mapOf(
            "parties" to mutableListOf(party),
            "trades" to mutableListOf(trade)
        ))
        try {
            FarmStateValidator.validateTrade(farm, trade)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Trade party role is incompatible with the trade type", e.message)
        }
    }

    @Test
    fun acceptsCompatiblePartyRoleForSale() {
        val party = validParty(role = PartyRole.CUSTOMER)
        val trade = validTrade(type = TradeType.SALE, partyId = "party-1")
        val farm = validFarm(mapOf(
            "parties" to mutableListOf(party),
            "trades" to mutableListOf(trade)
        ))
        FarmStateValidator.validateTrade(farm, trade)
    }

    @Test
    fun acceptsBothRoleForSaleAndPurchase() {
        val party = validParty(role = PartyRole.BOTH)
        val sale = validTrade(id = "sale-1", type = TradeType.SALE, partyId = "party-1")
        val purchase = validTrade(id = "purchase-1", type = TradeType.PURCHASE, partyId = "party-1")
        val farm = validFarm(mapOf(
            "parties" to mutableListOf(party),
            "trades" to mutableListOf(sale, purchase)
        ))
        FarmStateValidator.validateTrade(farm, sale)
        FarmStateValidator.validateTrade(farm, purchase)
    }

    // ── validateSettlement ────────────────────────────────────────

    @Test
    fun validSettlementPasses() {
        val trade = validTrade()
        val farm = validFarm(mapOf(
            "trades" to mutableListOf(trade)
        ))
        FarmStateValidator.validateSettlement(farm, validSettlement())
    }

    @Test
    fun rejectsBlankSettlementId() {
        val trade = validTrade()
        val farm = validFarm(mapOf("trades" to mutableListOf(trade)))
        try {
            FarmStateValidator.validateSettlement(farm, validSettlement(id = "   "))
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Settlement id is required", e.message)
        }
    }

    @Test
    fun rejectsZeroSettlementAmount() {
        val trade = validTrade()
        val farm = validFarm(mapOf("trades" to mutableListOf(trade)))
        try {
            FarmStateValidator.validateSettlement(farm, validSettlement(amountMinor = 0))
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Settlement amount must be positive", e.message)
        }
    }

    @Test
    fun rejectsBlankSettlementTradeId() {
        val trade = validTrade()
        val farm = validFarm(mapOf("trades" to mutableListOf(trade)))
        try {
            FarmStateValidator.validateSettlement(farm, validSettlement(tradeId = "   "))
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Settlement trade id is required", e.message)
        }
    }

    @Test
    fun rejectsSettlementForNonexistentTrade() {
        val farm = validFarm()
        try {
            FarmStateValidator.validateSettlement(farm, validSettlement(tradeId = "nonexistent"))
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Settlement trade not found: nonexistent", e.message)
        }
    }

    @Test
    fun rejectsSettlementExceedingTradeTotal() {
        val trade = validTrade(totalMinor = 1000)
        val settlement = validSettlement(amountMinor = 2000)
        val farm = validFarm(mapOf(
            "trades" to mutableListOf(trade),
            "settlements" to mutableListOf(settlement)
        ))
        try {
            FarmStateValidator.validateSettlement(farm, settlement)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Settlement amount cannot exceed the remaining balance", e.message)
        }
    }

    // ── validateProduct ───────────────────────────────────────────

    @Test
    fun validProductPasses() {
        FarmStateValidator.validateProduct(validProduct())
    }

    @Test
    fun rejectsBlankProductId() {
        try {
            FarmStateValidator.validateProduct(validProduct(id = "   "))
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Product id is required", e.message)
        }
    }

    @Test
    fun rejectsBlankProductName() {
        try {
            FarmStateValidator.validateProduct(validProduct().copy(name = "   "))
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Product name is required", e.message)
        }
    }

    @Test
    fun rejectsCustomUnitWithoutLabel() {
        try {
            FarmStateValidator.validateProduct(
                FarmProduct(id = "p1", name = "Item", defaultUnit = ProductUnit.CUSTOM, customUnitLabel = "   ")
            )
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Custom unit label is required", e.message)
        }
    }

    @Test
    fun acceptsCustomUnitWithLabel() {
        FarmStateValidator.validateProduct(
            FarmProduct(id = "p1", name = "Item", defaultUnit = ProductUnit.CUSTOM, customUnitLabel = "Dozen")
        )
    }

    // ── validateSupply ────────────────────────────────────────────

    @Test
    fun validSupplyPasses() {
        FarmStateValidator.validateSupply(validSupply())
    }

    @Test
    fun rejectsBlankSupplyId() {
        try {
            FarmStateValidator.validateSupply(validSupply(id = "   "))
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Supply id is required", e.message)
        }
    }

    @Test
    fun rejectsBlankSupplyName() {
        try {
            FarmStateValidator.validateSupply(validSupply().copy(name = "   "))
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Supply name is required", e.message)
        }
    }

    @Test
    fun rejectsCustomSupplyUnitWithoutLabel() {
        try {
            FarmStateValidator.validateSupply(
                FarmSupply(id = "s1", name = "Item", unit = ProductUnit.CUSTOM, customUnitLabel = "   ")
            )
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Custom unit label is required", e.message)
        }
    }

    // ── validateProductSaleDetail ─────────────────────────────────

    @Test
    fun rejectsSaleDetailForNonexistentTrade() {
        val farm = validFarm(mapOf("products" to mutableListOf(validProduct())))
        try {
            FarmStateValidator.validateProductSaleDetail(
                farm,
                ProductSaleDetail(tradeId = "nonexistent", productId = "prod-1", quantity = BigDecimal("1"), unit = ProductUnit.LITRE, rateMinor = 100)
            )
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Product sale detail trade not found: nonexistent", e.message)
        }
    }

    @Test
    fun rejectsSaleDetailOnPurchaseTrade() {
        val trade = validTrade(type = TradeType.PURCHASE)
        val farm = validFarm(mapOf(
            "products" to mutableListOf(validProduct()),
            "trades" to mutableListOf(trade)
        ))
        try {
            FarmStateValidator.validateProductSaleDetail(
                farm,
                ProductSaleDetail(tradeId = "trade-1", productId = "prod-1", quantity = BigDecimal("1"), unit = ProductUnit.LITRE, rateMinor = 100)
            )
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Product sale detail trade must be a sale", e.message)
        }
    }

    @Test
    fun rejectsSaleDetailForNonexistentProduct() {
        val trade = validTrade(type = TradeType.SALE, partyId = "party-1")
        val party = validParty(role = PartyRole.CUSTOMER)
        val farm = validFarm(mapOf(
            "parties" to mutableListOf(party),
            "trades" to mutableListOf(trade)
        ))
        try {
            FarmStateValidator.validateProductSaleDetail(
                farm,
                ProductSaleDetail(tradeId = "trade-1", productId = "nonexistent", quantity = BigDecimal("1"), unit = ProductUnit.LITRE, rateMinor = 100)
            )
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Product sale detail product not found: nonexistent", e.message)
        }
    }

    @Test
    fun rejectsSaleDetailTotalMismatch() {
        val trade = validTrade(type = TradeType.SALE, partyId = "party-1", totalMinor = 1000)
        val party = validParty(role = PartyRole.CUSTOMER)
        val farm = validFarm(mapOf(
            "products" to mutableListOf(validProduct()),
            "parties" to mutableListOf(party),
            "trades" to mutableListOf(trade)
        ))
        try {
            FarmStateValidator.validateProductSaleDetail(
                farm,
                ProductSaleDetail(tradeId = "trade-1", productId = "prod-1", quantity = BigDecimal("1"), unit = ProductUnit.LITRE, rateMinor = 100)
            )
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Product sale detail total does not match the trade", e.message)
        }
    }

    // ── validateSupplyPurchaseDetail ──────────────────────────────

    @Test
    fun rejectsPurchaseDetailForNonexistentTransaction() {
        val supply = validSupply()
        val farm = validFarm(mapOf("supplies" to mutableListOf(supply)))
        try {
            FarmStateValidator.validateSupplyPurchaseDetail(
                farm,
                SupplyPurchaseDetail(transactionId = "nonexistent", supplyId = "supply-1", quantity = BigDecimal("1"), unit = ProductUnit.KILOGRAM)
            )
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Supply purchase transaction not found: nonexistent", e.message)
        }
    }

    @Test
    fun rejectsPurchaseDetailOnIncomeTransaction() {
        val tx = FarmTransaction(id = "tx-1", type = TransactionType.INCOME, category = TransactionCategory.SALES, amountMinor = 1000, description = "Sale", occurredAt = now)
        val farm = validFarm(mapOf(
            "supplies" to mutableListOf(validSupply()),
            "transactions" to mutableListOf(tx)
        ))
        try {
            FarmStateValidator.validateSupplyPurchaseDetail(
                farm,
                SupplyPurchaseDetail(transactionId = "tx-1", supplyId = "supply-1", quantity = BigDecimal("1"), unit = ProductUnit.KILOGRAM)
            )
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Supply purchase must link to an expense", e.message)
        }
    }

    @Test
    fun rejectsPurchaseDetailForNonexistentTrade() {
        val farm = validFarm(mapOf("supplies" to mutableListOf(validSupply())))
        try {
            FarmStateValidator.validateSupplyPurchaseDetail(
                farm,
                SupplyPurchaseDetail(transactionId = null, purchaseTradeId = "nonexistent", supplyId = "supply-1", quantity = BigDecimal("1"), unit = ProductUnit.KILOGRAM)
            )
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Supply purchase trade not found: nonexistent", e.message)
        }
    }

    @Test
    fun rejectsPurchaseDetailOnSaleTrade() {
        val trade = validTrade(type = TradeType.SALE, partyId = "party-1")
        val party = validParty(role = PartyRole.CUSTOMER)
        val farm = validFarm(mapOf(
            "supplies" to mutableListOf(validSupply()),
            "parties" to mutableListOf(party),
            "trades" to mutableListOf(trade)
        ))
        try {
            FarmStateValidator.validateSupplyPurchaseDetail(
                farm,
                SupplyPurchaseDetail(transactionId = null, purchaseTradeId = "trade-1", supplyId = "supply-1", quantity = BigDecimal("1"), unit = ProductUnit.KILOGRAM)
            )
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Supply purchase must link to a purchase", e.message)
        }
    }

    @Test
    fun rejectsPurchaseDetailForNonexistentSupply() {
        val tx = validTransaction()
        val farm = validFarm(mapOf("transactions" to mutableListOf(tx)))
        try {
            FarmStateValidator.validateSupplyPurchaseDetail(
                farm,
                SupplyPurchaseDetail(transactionId = "tx-1", supplyId = "nonexistent", quantity = BigDecimal("1"), unit = ProductUnit.KILOGRAM)
            )
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Supply purchase supply not found: nonexistent", e.message)
        }
    }

    @Test
    fun rejectsPurchaseDetailUnitMismatch() {
        val tx = validTransaction()
        val supply = FarmSupply(id = "supply-1", name = "Feed", unit = ProductUnit.KILOGRAM)
        val farm = validFarm(mapOf(
            "supplies" to mutableListOf(supply),
            "transactions" to mutableListOf(tx)
        ))
        try {
            FarmStateValidator.validateSupplyPurchaseDetail(
                farm,
                SupplyPurchaseDetail(transactionId = "tx-1", supplyId = "supply-1", quantity = BigDecimal("1"), unit = ProductUnit.LITRE)
            )
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Supply purchase unit does not match", e.message)
        }
    }

    // ── Activities uniqueness ─────────────────────────────────────

    @Test
    fun rejectsDuplicateActivities() {
        val farm = validFarm(mapOf(
            "activities" to mutableListOf(FarmActivityType.CROPS, FarmActivityType.CROPS)
        ))
        try {
            FarmStateValidator.validateFarm(farm)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Farm activities must be unique", e.message)
        }
    }

    @Test
    fun rejectsDuplicateDisabledActivities() {
        val farm = validFarm(mapOf(
            "disabledActivities" to mutableListOf(FarmActivityType.CROPS, FarmActivityType.CROPS)
        ))
        try {
            FarmStateValidator.validateFarm(farm)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Disabled farm activities must be unique", e.message)
        }
    }

    @Test
    fun rejectsActivityEnabledAndDisabled() {
        val farm = validFarm(mapOf(
            "activities" to mutableListOf(FarmActivityType.CROPS),
            "disabledActivities" to mutableListOf(FarmActivityType.CROPS)
        ))
        try {
            FarmStateValidator.validateFarm(farm)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("An activity cannot be enabled and disabled at the same time", e.message)
        }
    }

    // ── Supply stock non-negative ─────────────────────────────────

    @Test
    fun rejectsNegativeSupplyStock() {
        val supply = validSupply()
        val usage = SupplyUsage(
            id = "usage-1",
            supplyId = "supply-1",
            quantity = BigDecimal("999"),
            unit = ProductUnit.KILOGRAM,
            occurredAt = now
        )
        val purchase = SupplyPurchaseDetail(
            transactionId = "tx-1",
            supplyId = "supply-1",
            quantity = BigDecimal("100"),
            unit = ProductUnit.KILOGRAM
        )
        val tx = validTransaction()
        val farm = validFarm(mapOf(
            "supplies" to mutableListOf(supply),
            "supplyUsages" to mutableListOf(usage),
            "supplyPurchaseDetails" to mutableListOf(purchase),
            "transactions" to mutableListOf(tx)
        ))
        try {
            FarmStateValidator.validateFarm(farm)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Supply stock cannot be negative"))
        }
    }
}
