package com.susankhya.kisab.domain

import com.susankhya.kisab.persistence.FarmBackupCodec
import com.susankhya.kisab.persistence.FarmPersistenceCodec
import com.susankhya.kisab.persistence.readTextWithLimit
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
        val farm = service.createFarm("Demo Farm", currencyCode = "USD")
        service.addEntry(farm.id, FarmEntry(FarmEntryKind.LIVESTOCK, "Goat", 3))
        val expense = service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.EXPENSE,
                category = TransactionCategory.FEED,
                amountMinor = 5000,
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
    fun transactionsRequireDescriptionAmountAndCategory() {
        val farm = service.createFarm("Demo Farm")

        try {
            service.createTransaction(
                farm.id,
                FarmTransactionDraft(
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.FEED,
                    amountMinor = 0,
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
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.SALES,
                    amountMinor = 1000,
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
    fun setFarmCurrencyRequiresThreeLetterIsoCode() {
        val farm = service.createFarm("Demo Farm")

        try {
            service.setFarmCurrency(farm.id, "Dollar")
            fail("Expected IllegalArgumentException")
        } catch (exception: IllegalArgumentException) {
            assertEquals("Farm currency must be a 3-letter ISO code", exception.message)
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
                    category = TransactionCategory.SALES,
                    amountMinor = 1000,
                    description = "Bad category",
                    occurredAt = "2024-01-01T12:00:00Z"
                )
            )
            fail("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            val persistedFarm = service.loadFarm(farm.id)
            assertEquals(0, persistedFarm?.transactions?.size)
        }

        try {
            service.setFarmCurrency(farm.id, "Dollar")
            fail("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            val persistedFarm = service.loadFarm(farm.id)
            assertEquals("NPR", persistedFarm?.currencyCode)
        }
    }

    @Test
    fun farmCurrencyDefaultsToNprAndCanBeChangedBeforeFirstTransaction() {
        val farm = service.createFarm("Demo Farm")
        assertEquals("NPR", farm.currencyCode)

        service.setFarmCurrency(farm.id, "USD")
        assertEquals("USD", service.loadFarm(farm.id)?.currencyCode)

        service.setFarmCurrency(farm.id, "EUR")
        assertEquals("EUR", service.loadFarm(farm.id)?.currencyCode)
    }

    @Test
    fun farmCurrencyCanChangeAfterTransactionsKeepingAmountsUnchanged() {
        val farm = service.createFarm("Demo Farm")
        val transaction = service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.INCOME,
                category = TransactionCategory.SALES,
                amountMinor = 1000,
                description = "Sale",
                occurredAt = "2024-01-01T12:00:00Z"
            )
        )

        service.setFarmCurrency(farm.id, "USD")

        val updated = service.loadFarm(farm.id)!!
        assertEquals("USD", updated.currencyCode)
        assertEquals(transaction, updated.transactions.single())
        assertEquals(1000, updated.transactions.single().amountMinor)
    }

    @Test
    fun farmCurrencyCanChangeAfterTradeAndSettlementKeepingAmountsUnchanged() {
        val farm = service.createFarm("Demo Farm")
        service.addParty(farm.id, PartyDraft(name = "Feed Store", role = PartyRole.SUPPLIER))
        val trade = service.addTradeWithInitialSettlement(
            farm.id,
            TradeDraft(
                type = TradeType.PURCHASE,
                partyId = service.parties(farm.id).single().id,
                totalMinor = 2500,
                description = "Feed stock",
                occurredAt = "2024-01-01T12:00:00Z"
            ),
            initialSettlementMinor = 2500
        )

        service.setFarmCurrency(farm.id, "JPY")

        val updated = service.loadFarm(farm.id)!!
        assertEquals("JPY", updated.currencyCode)
        assertEquals(trade, updated.trades.single())
        assertEquals(2500, updated.trades.single().totalMinor)
        assertEquals(2500, updated.settlements.single().amountMinor)
    }

    @Test
    fun partyCrudPersistsAndOrdersByName() {
        val farm = service.createFarm("Demo Farm")
        val supplier = service.addParty(
            farm.id,
            PartyDraft(name = "Feed Store", role = PartyRole.SUPPLIER, contact = "9800000001")
        )
        val customer = service.addParty(
            farm.id,
            PartyDraft(name = "market buyer", role = PartyRole.CUSTOMER, notes = "Buys eggs weekly")
        )

        val ordered = service.parties(farm.id)
        assertEquals(listOf(supplier.id, customer.id), ordered.map { it.id })

        service.updateParty(
            farm.id,
            customer.id,
            PartyDraft(name = "Market Buyer", role = PartyRole.CUSTOMER, contact = "9800000002", notes = "City market")
        )
        val updated = service.party(farm.id, customer.id)
        assertEquals("Market Buyer", updated?.name)
        assertEquals("9800000002", updated?.contact)
        assertEquals("City market", updated?.notes)
        assertEquals(customer.id, updated?.id)

        service.deleteParty(farm.id, supplier.id)
        assertNull(service.party(farm.id, supplier.id))
        assertEquals(listOf(customer.id), service.parties(farm.id).map { it.id })
    }

    @Test
    fun addPartyTrimsAndRequiresName() {
        val farm = service.createFarm("Demo Farm")

        val party = service.addParty(farm.id, PartyDraft(name = "  Ram  ", role = PartyRole.OTHER))
        assertEquals("Ram", party.name)

        try {
            service.addParty(farm.id, PartyDraft(name = "   ", role = PartyRole.CUSTOMER))
            fail("Expected IllegalArgumentException")
        } catch (exception: IllegalArgumentException) {
            assertEquals("Party name is required", exception.message)
        }
        assertEquals(1, service.parties(farm.id).size)
    }

    @Test
    fun updatePartyRejectsBlankNameAndMissingParty() {
        val farm = service.createFarm("Demo Farm")
        val party = service.addParty(farm.id, PartyDraft(name = "Ram", role = PartyRole.CUSTOMER))

        try {
            service.updateParty(farm.id, party.id, PartyDraft(name = " ", role = PartyRole.SUPPLIER))
            fail("Expected IllegalArgumentException")
        } catch (exception: IllegalArgumentException) {
            assertEquals("Party name is required", exception.message)
        }

        try {
            service.updateParty(farm.id, "missing", PartyDraft(name = "Sita", role = PartyRole.CUSTOMER))
            fail("Expected IllegalArgumentException")
        } catch (exception: IllegalArgumentException) {
            assertEquals("Party not found: missing", exception.message)
        }
    }

    @Test
    fun deletePartyRejectsMissingParty() {
        val farm = service.createFarm("Demo Farm")
        try {
            service.deleteParty(farm.id, "missing")
            fail("Expected IllegalArgumentException")
        } catch (exception: IllegalArgumentException) {
            assertEquals("Party not found: missing", exception.message)
        }
    }

    @Test
    fun partyCrudRoundTripsThroughPersistenceAndBackup() {
        val farm = service.createFarm("Demo Farm")
        service.addParty(
            farm.id,
            PartyDraft(name = "Feed Store", role = PartyRole.SUPPLIER, contact = "9800000001", notes = "Corner shop")
        )

        val encoded = FarmPersistenceCodec.encode(service.loadFarm(farm.id)!!)
        val reloaded = FarmPersistenceCodec.decode(encoded)
        assertEquals(14, reloaded.schemaVersion)
        assertEquals(1, reloaded.parties.size)
        assertEquals("Feed Store", reloaded.parties.single().name)
        assertEquals(PartyRole.SUPPLIER, reloaded.parties.single().role)
        assertEquals("9800000001", reloaded.parties.single().contact)
        assertEquals("Corner shop", reloaded.parties.single().notes)

        val envelope = FarmBackupCodec.decode(FarmBackupCodec.encode(service.loadFarm(farm.id)!!))
        assertEquals(1, envelope.farm.parties.size)
        assertEquals("Feed Store", envelope.farm.parties.single().name)
    }

    @Test
    fun tradeCrudOrdersNewestFirstAndKeepsStableId() {
        val farm = service.createFarm("Demo Farm")
        val party = service.addParty(farm.id, PartyDraft(name = "Dairy", role = PartyRole.CUSTOMER))
        val first = service.addTradeWithInitialSettlement(
            farm.id,
            TradeDraft(
                type = TradeType.SALE,
                partyId = party.id,
                totalMinor = 30000,
                occurredAt = "2024-01-01T12:00:00Z"
            ),
            initialSettlementMinor = 30000
        )
        val second = service.addTradeWithInitialSettlement(
            farm.id,
            TradeDraft(
                type = TradeType.SALE,
                partyId = party.id,
                totalMinor = 5000,
                occurredAt = "2024-02-01T12:00:00Z"
            ),
            initialSettlementMinor = 2000
        )

        assertEquals(listOf(second.id, first.id), service.trades(farm.id).map { it.id })

        val updated = service.updateTrade(
            farm.id,
            second.id,
            TradeDraft(
                type = TradeType.SALE,
                partyId = party.id,
                totalMinor = 6000,
                occurredAt = "2024-02-01T12:00:00Z"
            )
        )
        assertEquals(second.id, updated.id)
        assertEquals(6000, service.trade(farm.id, second.id)!!.totalMinor)

        val firstSettlement = service.settlementsForTrade(farm.id, first.id).single()
        service.deleteSettlement(farm.id, firstSettlement.id)
        service.deleteTrade(farm.id, first.id)
        assertEquals(listOf(second.id), service.trades(farm.id).map { it.id })
    }

    @Test
    fun addTradeRejectsInvalidAmounts() {
        val farm = service.createFarm("Demo Farm")
        val party = service.addParty(farm.id, PartyDraft(name = "Dairy", role = PartyRole.CUSTOMER))

        try {
            service.addTrade(
                farm.id,
                TradeDraft(
                    type = TradeType.SALE,
                    partyId = party.id,
                    totalMinor = 0,
                    occurredAt = "2024-01-01T12:00:00Z"
                )
            )
            fail("Expected IllegalArgumentException")
        } catch (exception: IllegalArgumentException) {
            assertEquals("Trade total must be positive", exception.message)
        }

        try {
            service.addTradeWithInitialSettlement(
                farm.id,
                TradeDraft(
                    type = TradeType.SALE,
                    partyId = party.id,
                    totalMinor = 5000,
                    occurredAt = "2024-01-01T12:00:00Z"
                ),
                initialSettlementMinor = -1
            )
            fail("Expected IllegalArgumentException")
        } catch (exception: IllegalArgumentException) {
            assertEquals("Trade initial payment amount cannot be negative", exception.message)
        }

        try {
            service.addTradeWithInitialSettlement(
                farm.id,
                TradeDraft(
                    type = TradeType.SALE,
                    partyId = party.id,
                    totalMinor = 5000,
                    occurredAt = "2024-01-01T12:00:00Z"
                ),
                initialSettlementMinor = 6000
            )
            fail("Expected IllegalArgumentException")
        } catch (exception: IllegalArgumentException) {
            assertEquals("Trade initial payment cannot exceed the total", exception.message)
        }

        assertEquals(0, service.trades(farm.id).size)
    }

    @Test
    fun unpaidOrPartialTradeRequiresParty() {
        val farm = service.createFarm("Demo Farm")

        try {
            service.addTradeWithInitialSettlement(
                farm.id,
                TradeDraft(
                    type = TradeType.SALE,
                    partyId = null,
                    totalMinor = 5000,
                    occurredAt = "2024-01-01T12:00:00Z"
                ),
                initialSettlementMinor = 2000
            )
            fail("Expected IllegalArgumentException")
        } catch (exception: IllegalArgumentException) {
            assertEquals("Partially paid or unpaid trades require a party", exception.message)
        }

        assertEquals(0, service.trades(farm.id).size)
    }

    @Test
    fun fullyPaidTradeDoesNotRequireParty() {
        val farm = service.createFarm("Demo Farm")
        val trade = service.addTradeWithInitialSettlement(
            farm.id,
            TradeDraft(
                type = TradeType.SALE,
                partyId = null,
                totalMinor = 5000,
                occurredAt = "2024-01-01T12:00:00Z"
            ),
            initialSettlementMinor = 5000
        )

        assertEquals(5000, trade.totalMinor)
        assertEquals(1, service.trades(farm.id).size)
        assertEquals(1, service.settlementsForTrade(farm.id, trade.id).size)
    }

    @Test
    fun tradeRejectsMissingOrIncompatibleParty() {
        val farm = service.createFarm("Demo Farm")
        val supplier = service.addParty(farm.id, PartyDraft(name = "Feed Store", role = PartyRole.SUPPLIER))
        val customer = service.addParty(farm.id, PartyDraft(name = "Dairy", role = PartyRole.CUSTOMER))

        try {
            service.addTrade(
                farm.id,
                TradeDraft(
                    type = TradeType.PURCHASE,
                    partyId = "missing",
                    totalMinor = 5000,
                    occurredAt = "2024-01-01T12:00:00Z"
                )
            )
            fail("Expected IllegalArgumentException")
        } catch (exception: IllegalArgumentException) {
            assertEquals("Trade party not found: missing", exception.message)
        }

        try {
            service.addTradeWithInitialSettlement(
                farm.id,
                TradeDraft(
                    type = TradeType.SALE,
                    partyId = supplier.id,
                    totalMinor = 5000,
                    occurredAt = "2024-01-01T12:00:00Z"
                ),
                initialSettlementMinor = 5000
            )
            fail("Expected IllegalArgumentException")
        } catch (exception: IllegalArgumentException) {
            assertEquals("Trade party role is incompatible with the trade type", exception.message)
        }

        service.addTradeWithInitialSettlement(
            farm.id,
            TradeDraft(
                type = TradeType.PURCHASE,
                partyId = supplier.id,
                totalMinor = 5000,
                occurredAt = "2024-01-01T12:00:00Z"
            ),
            initialSettlementMinor = 5000
        )
        service.addTradeWithInitialSettlement(
            farm.id,
            TradeDraft(
                type = TradeType.SALE,
                partyId = customer.id,
                totalMinor = 5000,
                occurredAt = "2024-01-01T12:00:00Z"
            ),
            initialSettlementMinor = 5000
        )
    }

    @Test
    fun bothPartyRoleIsCompatibleWithBothTradeTypes() {
        val farm = service.createFarm("Demo Farm")
        val both = service.addParty(farm.id, PartyDraft(name = "Co-op", role = PartyRole.BOTH))

        service.addTradeWithInitialSettlement(
            farm.id,
            TradeDraft(
                type = TradeType.SALE,
                partyId = both.id,
                totalMinor = 3000,
                occurredAt = "2024-01-01T12:00:00Z"
            ),
            initialSettlementMinor = 3000
        )
        service.addTradeWithInitialSettlement(
            farm.id,
            TradeDraft(
                type = TradeType.PURCHASE,
                partyId = both.id,
                totalMinor = 2000,
                occurredAt = "2024-01-02T12:00:00Z"
            ),
            initialSettlementMinor = 2000
        )

        assertEquals(2, service.trades(farm.id).size)
    }

    @Test
    fun deletePartyBlockedWhenReferencedByTrade() {
        val farm = service.createFarm("Demo Farm")
        val party = service.addParty(farm.id, PartyDraft(name = "Dairy", role = PartyRole.CUSTOMER))
        service.addTrade(
            farm.id,
            TradeDraft(
                type = TradeType.SALE,
                partyId = party.id,
                totalMinor = 5000,
                occurredAt = "2024-01-01T12:00:00Z"
            )
        )

        try {
            service.deleteParty(farm.id, party.id)
            fail("Expected IllegalArgumentException")
        } catch (exception: IllegalArgumentException) {
            assertEquals("Party cannot be deleted while sales or purchases reference it", exception.message)
        }

        assertEquals(1, service.parties(farm.id).size)
    }

    @Test
    fun updatePartyRoleChangeBlockedByReferencedTrades() {
        val farm = service.createFarm("Demo Farm")
        val party = service.addParty(farm.id, PartyDraft(name = "Dairy", role = PartyRole.CUSTOMER))
        service.addTrade(
            farm.id,
            TradeDraft(
                type = TradeType.SALE,
                partyId = party.id,
                totalMinor = 5000,
                occurredAt = "2024-01-01T12:00:00Z"
            )
        )

        try {
            service.updateParty(farm.id, party.id, PartyDraft(name = "Dairy", role = PartyRole.SUPPLIER))
            fail("Expected IllegalArgumentException")
        } catch (exception: IllegalArgumentException) {
            assertEquals("Party role cannot change while sales or purchases reference this party", exception.message)
        }

        val widened = service.updateParty(farm.id, party.id, PartyDraft(name = "Dairy Co-op", role = PartyRole.BOTH))
        assertEquals(PartyRole.BOTH, widened.role)

        service.addTradeWithInitialSettlement(
            farm.id,
            TradeDraft(
                type = TradeType.PURCHASE,
                partyId = party.id,
                totalMinor = 5000,
                occurredAt = "2024-01-02T12:00:00Z"
            ),
            initialSettlementMinor = 5000
        )
    }

    @Test
    fun schema4FarmMigratesWithEmptyTradesList() {
        val schema4 = "4\u001Ffarm-s4\u001FFarm S4\u001FLIVESTOCK:Goat:2\u001FNPR\u001F\u001F"

        val farm = FarmPersistenceCodec.decode(schema4)

        assertEquals(14, farm.schemaVersion)
        assertEquals(0, farm.trades.size)
        assertEquals(0, farm.parties.size)
    }

    @Test
    fun tradeRoundTripsThroughPersistenceAndBackup() {
        val farm = service.createFarm("Demo Farm")
        val party = service.addParty(farm.id, PartyDraft(name = "Dairy", role = PartyRole.CUSTOMER, contact = "9800000001"))
        service.addTradeWithInitialSettlement(
            farm.id,
            TradeDraft(
                type = TradeType.SALE,
                partyId = party.id,
                totalMinor = 30000,
                description = "Milk sale batch 1",
                occurredAt = "2024-01-01T12:00:00Z"
            ),
            initialSettlementMinor = 10000
        )

        val reloaded = FarmPersistenceCodec.decode(FarmPersistenceCodec.encode(service.loadFarm(farm.id)!!))
        assertEquals(14, reloaded.schemaVersion)
        val trade = reloaded.trades.single()
        assertEquals(TradeType.SALE, trade.type)
        assertEquals(party.id, trade.partyId)
        assertEquals(30000, trade.totalMinor)
        assertEquals(10000, reloaded.settlements.single().amountMinor)
        assertEquals("Milk sale batch 1", trade.description)

        val envelope = FarmBackupCodec.decode(FarmBackupCodec.encode(service.loadFarm(farm.id)!!))
        assertEquals(1, envelope.farm.trades.size)
        assertEquals(TradeType.SALE, envelope.farm.trades.single().type)
    }

    @Test
    fun tradesAreExcludedFromBalanceSummary() {
        val farm = service.createFarm("Demo Farm")
        service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.INCOME,
                category = TransactionCategory.SALES,
                amountMinor = 1000,
                description = "Egg sale",
                occurredAt = "2024-01-01T12:00:00Z"
            )
        )
        val party = service.addParty(farm.id, PartyDraft(name = "Dairy", role = PartyRole.CUSTOMER))
        service.addTradeWithInitialSettlement(
            farm.id,
            TradeDraft(
                type = TradeType.SALE,
                partyId = party.id,
                totalMinor = 30000,
                occurredAt = "2024-01-02T12:00:00Z"
            ),
            initialSettlementMinor = 10000
        )

        val summary = service.summary(farm.id)
        assertEquals(1, summary.transactionCount)
        assertEquals(1000, summary.balanceMinor)
    }

    @Test
    fun tradePaymentStatusAndOutstandingAreDerived() {
        val farm = service.createFarm("Demo Farm")
        val party = service.addParty(farm.id, PartyDraft(name = "Dairy", role = PartyRole.CUSTOMER))
        val paid = service.addTradeWithInitialSettlement(
            farm.id,
            TradeDraft(type = TradeType.SALE, partyId = party.id, totalMinor = 5000, occurredAt = "2024-01-01T12:00:00Z"),
            initialSettlementMinor = 5000
        )
        val partial = service.addTradeWithInitialSettlement(
            farm.id,
            TradeDraft(type = TradeType.SALE, partyId = party.id, totalMinor = 5000, occurredAt = "2024-01-02T12:00:00Z"),
            initialSettlementMinor = 2000
        )
        val unpaid = service.addTrade(
            farm.id,
            TradeDraft(type = TradeType.SALE, partyId = party.id, totalMinor = 5000, occurredAt = "2024-01-03T12:00:00Z")
        )

        assertEquals(PaymentStatus.PAID, service.tradePaymentSummary(farm.id, paid).status)
        assertEquals(PaymentStatus.PARTIAL, service.tradePaymentSummary(farm.id, partial).status)
        assertEquals(PaymentStatus.UNPAID, service.tradePaymentSummary(farm.id, unpaid).status)
        assertEquals(0, service.tradePaymentSummary(farm.id, paid).outstandingMinor)
        assertEquals(3000, service.tradePaymentSummary(farm.id, partial).outstandingMinor)
        assertEquals(5000, service.tradePaymentSummary(farm.id, unpaid).outstandingMinor)
        assertEquals(5000, service.tradePaymentSummary(farm.id, paid).paidMinor)
        assertEquals(2000, service.tradePaymentSummary(farm.id, partial).paidMinor)
    }

    @Test
    fun schema4FarmWithTradesRoundTripByteStable() {
        val farm = service.createFarm("Demo Farm")
        val party = service.addParty(farm.id, PartyDraft(name = "Dairy", role = PartyRole.CUSTOMER))
        val trade = service.addTradeWithInitialSettlement(
            farm.id,
            TradeDraft(type = TradeType.SALE, partyId = party.id, totalMinor = 3000, occurredAt = "2024-01-01T12:00:00Z"),
            initialSettlementMinor = 1000
        )
        val persisted = service.loadFarm(farm.id)!!
        val encoded = FarmPersistenceCodec.encode(persisted)
        val reloaded = FarmPersistenceCodec.decode(encoded)

        assertEquals(14, reloaded.schemaVersion)
        assertEquals(1, reloaded.parties.size)
        assertEquals(1, reloaded.trades.size)
        assertEquals(trade.id, reloaded.trades.single().id)
        assertEquals(party.id, reloaded.trades.single().partyId)
    }

    @Test
    fun schema3FarmMigratesWithEmptyPartiesList() {
        val schema3 = "3\u001Ffarm-s3\u001FFarm S3\u001FLIVESTOCK:Goat:2\u001FNPR\u001F" +
            "tx-1\u001DEXPENSE\u001DFEED\u001D1500\u001DFeed\u001D2024-01-01T12:00:00Z"

        val farm = FarmPersistenceCodec.decode(schema3)

        assertEquals(14, farm.schemaVersion)
        assertEquals("Farm S3", farm.name)
        assertEquals(1, farm.transactions.size)
        assertEquals(0, farm.parties.size)
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
        assertEquals("USD", farm.currencyCode)
        assertEquals("2024-01-01T00:00:00Z", farm.transactions[0].occurredAt.toInstant().toString())
    }

    @Test
    fun malformedPayloadsFailSafely() {
        assertNull(FarmPersistenceCodec.decodeOrNull("not-a-valid-payload"))
    }

    @Test
    fun schema2FarmMigratesCurrencyToFarmLevel() {
        val schema2 = "2\u001Ffarm-s2\u001FFarm S2\u001FLIVESTOCK:Goat:2\u001F" +
            "tx-1\u001DEXPENSE\u001DFEED\u001D1500\u001DUSD\u001DFeed\u001D2024-01-01T12:00:00Z"

        val farm = FarmPersistenceCodec.decode(schema2)

        assertEquals(14, farm.schemaVersion)
        assertEquals("USD", farm.currencyCode)
        assertEquals(1, farm.transactions.size)
        assertEquals(1500, farm.transactions[0].amountMinor)
        assertEquals("Feed", farm.transactions[0].description)
        assertTrue(farm.transactions[0].occurredAt.toString().startsWith("2024-01-01"))
    }

    @Test
    fun schema2EmptyFarmDefaultsCurrencyToNpr() {
        val schema2 = "2\u001Ffarm-s2e\u001FFarm S2 Empty\u001F\u001F"

        val farm = FarmPersistenceCodec.decode(schema2)

        assertEquals(14, farm.schemaVersion)
        assertEquals("NPR", farm.currencyCode)
        assertEquals(0, farm.transactions.size)
    }

    @Test
    fun legacyFarmWithoutTransactionsDefaultsCurrencyToNpr() {
        val farm = FarmPersistenceCodec.decode("farm-legacy-empty|Legacy Farm|LIVESTOCK:Goat:2|")

        assertEquals("NPR", farm.currencyCode)
        assertEquals(0, farm.transactions.size)
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
    fun boundedTextReaderRejectsOversizedInputBeforeExhaustiveRead() {
        val input = ByteArrayInputStream("x".repeat(16).toByteArray())
        try {
            readTextWithLimit(input, 8)
            fail("Expected IllegalArgumentException")
        } catch (exception: IllegalArgumentException) {
            assertEquals("Backup file is too large", exception.message)
        }
    }

    @Test
    fun duplicateTransactionIdsAreRejected() {
        val farm = FarmState(
            id = "farm-duplicate",
            name = "Demo Farm",
            entries = mutableListOf(),
            transactions = mutableListOf(
                FarmTransaction(
                    id = "tx-dup",
                    type = TransactionType.INCOME,
                    category = TransactionCategory.SALES,
                    amountMinor = 1500,
                    description = "Sale",
                    occurredAt = java.time.OffsetDateTime.parse("2024-01-01T12:00:00Z")
                ),
                FarmTransaction(
                    id = "tx-dup",
                    type = TransactionType.INCOME,
                    category = TransactionCategory.SALES,
                    amountMinor = 2500,
                    description = "Another sale",
                    occurredAt = java.time.OffsetDateTime.parse("2024-01-02T12:00:00Z")
                )
            )
        )

        val encoded = FarmBackupCodec.encode(farm)
        assertNull(FarmBackupCodec.decodeOrNull(encoded))
    }

    @Test
    fun invalidBackupSnapshotFailsSafely() {
        val farm = FarmState(
            id = "farm-1",
            name = "Demo Farm",
            currencyCode = "Dollar",
            entries = mutableListOf(FarmEntry(FarmEntryKind.LIVESTOCK, "Goat", 2)),
            transactions = mutableListOf(
                FarmTransaction(
                    id = "tx-1",
                    type = TransactionType.INCOME,
                    category = TransactionCategory.SALES,
                    amountMinor = 1500,
                    description = "Sale",
                    occurredAt = java.time.OffsetDateTime.parse("2024-01-01T12:00:00Z")
                ),
                FarmTransaction(
                    id = "tx-2",
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.FEED,
                    amountMinor = 500,
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

    @Test
    fun transactionsRenderNewestFirstByOccurredAt() {
        val farm = service.createFarm("Demo Farm")
        service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.INCOME,
                category = TransactionCategory.SALES,
                amountMinor = 1000,
                description = "Oldest",
                occurredAt = "2024-01-01T12:00:00Z"
            )
        )
        service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.EXPENSE,
                category = TransactionCategory.FEED,
                amountMinor = 2000,
                description = "Newest",
                occurredAt = "2024-01-03T12:00:00Z"
            )
        )
        service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.INCOME,
                category = TransactionCategory.SERVICES,
                amountMinor = 3000,
                description = "Middle",
                occurredAt = "2024-01-02T12:00:00Z"
            )
        )

        val newestFirst = service.transactionsNewestFirst(farm.id)

        assertEquals(listOf("Newest", "Middle", "Oldest"), newestFirst.map { it.description })
        assertEquals(listOf("Oldest", "Newest", "Middle"), service.loadFarm(farm.id)!!.transactions.map { it.description })
    }

    @Test
    fun equalTimestampTransactionsTieBreakByLedgerOrder() {
        val farm = service.createFarm("Demo Farm")
        service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.INCOME,
                category = TransactionCategory.SALES,
                amountMinor = 1000,
                description = "First recorded",
                occurredAt = "2024-01-01T12:00:00Z"
            )
        )
        service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.INCOME,
                category = TransactionCategory.SALES,
                amountMinor = 2000,
                description = "Second recorded",
                occurredAt = "2024-01-01T12:00:00Z"
            )
        )

        val newestFirst = service.transactionsNewestFirst(farm.id)

        assertEquals(listOf("Second recorded", "First recorded"), newestFirst.map { it.description })
    }

    @Test
    fun orderingRecomputesAfterEditingTimestamp() {
        val farm = service.createFarm("Demo Farm")
        val older = service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.INCOME,
                category = TransactionCategory.SALES,
                amountMinor = 1000,
                description = "Older",
                occurredAt = "2024-01-01T12:00:00Z"
            )
        )
        service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.EXPENSE,
                category = TransactionCategory.FEED,
                amountMinor = 2000,
                description = "Newer",
                occurredAt = "2024-01-02T12:00:00Z"
            )
        )

        service.updateTransaction(
            farm.id,
            older.id,
            FarmTransactionDraft(
                type = TransactionType.INCOME,
                category = TransactionCategory.SALES,
                amountMinor = 1500,
                description = "Older",
                occurredAt = "2024-01-03T12:00:00Z"
            )
        )

        val newestFirst = service.transactionsNewestFirst(farm.id)

        assertEquals(listOf("Older", "Newer"), newestFirst.map { it.description })
        assertEquals(older.id, newestFirst.first().id)
    }

    @Test
    fun orderingPreservedAcrossPersistenceRecreation() {
        val farm = service.createFarm("Demo Farm")
        val older = service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.INCOME,
                category = TransactionCategory.SALES,
                amountMinor = 1000,
                description = "Older",
                occurredAt = "2024-01-01T12:00:00Z"
            )
        )
        val newer = service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.EXPENSE,
                category = TransactionCategory.FEED,
                amountMinor = 2000,
                description = "Newer",
                occurredAt = "2024-01-02T12:00:00Z"
            )
        )

        val encoded = FarmPersistenceCodec.encode(service.loadFarm(farm.id)!!)
        val reloaded = FarmPersistenceCodec.decode(encoded)

        assertEquals(listOf(newer.id, older.id), reloaded.transactionsNewestFirst().map { it.id })
    }

    @Test
    fun orderingPreservedAcrossLegacyMigration() {
        val farm = FarmPersistenceCodec.decode("farm-legacy|Legacy Farm|LIVESTOCK:Goat:2|Feed:1000;;Egg sale:-5000")

        val newestFirst = farm.transactionsNewestFirst()

        assertEquals(2, newestFirst.size)
        assertEquals(listOf("tx-migrated-1", "tx-migrated-0"), newestFirst.map { it.id })
    }

    @Test
    fun orderingPreservedAcrossBackupRestore() {
        val farm = service.createFarm("Demo Farm")
        val older = service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.INCOME,
                category = TransactionCategory.SALES,
                amountMinor = 1000,
                description = "Older",
                occurredAt = "2024-01-01T12:00:00Z"
            )
        )
        val newer = service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.EXPENSE,
                category = TransactionCategory.FEED,
                amountMinor = 2000,
                description = "Newer",
                occurredAt = "2024-01-02T12:00:00Z"
            )
        )

        val encoded = FarmBackupCodec.encode(service.loadFarm(farm.id)!!)
        val envelope = FarmBackupCodec.decode(encoded)

        assertEquals(listOf(newer.id, older.id), envelope.farm.transactionsNewestFirst().map { it.id })
    }

    @Test
    fun truncatedBackupEnvelopeFailsSafely() {
        val farm = service.createFarm("Demo Farm")
        val encoded = FarmBackupCodec.encode(farm)

        val truncated = encoded.dropLast(4)

        assertNull(FarmBackupCodec.decodeOrNull(truncated))
    }

    @Test
    fun backupWithNonPositiveMoneyIsRejected() {
        val farm = FarmState(
            id = "farm-bad-money",
            name = "Demo Farm",
            entries = mutableListOf(),
            transactions = mutableListOf(
                FarmTransaction(
                    id = "tx-1",
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.FEED,
                    amountMinor = 0,
                    description = "Feed",
                    occurredAt = OffsetDateTime.parse("2024-01-01T12:00:00Z")
                )
            )
        )

        val encoded = FarmBackupCodec.encode(farm)

        assertNull(FarmBackupCodec.decodeOrNull(encoded))
    }

    @Test
    fun backupWithInvalidCategoryForTypeIsRejected() {
        val farm = FarmState(
            id = "farm-bad-category",
            name = "Demo Farm",
            entries = mutableListOf(),
            transactions = mutableListOf(
                FarmTransaction(
                    id = "tx-1",
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.SALES,
                    amountMinor = 1000,
                    description = "Bad category",
                    occurredAt = OffsetDateTime.parse("2024-01-01T12:00:00Z")
                )
            )
        )

        val encoded = FarmBackupCodec.encode(farm)

        assertNull(FarmBackupCodec.decodeOrNull(encoded))
    }

    @Test
    fun backupWithMalformedTransactionTimestampIsRejected() {
        val malformedPayload = "2\u001Ffarm-1\u001FDemo Farm\u001F\u001Ftx-1\u001DINCOME\u001DSALES\u001D1000\u001DUSD\u001DSale\u001Dnot-a-timestamp"
        val malformedEnvelope = "1\u001F2024-01-01T00:00:00Z\u001F" +
            Base64.getEncoder().encodeToString(malformedPayload.toByteArray(StandardCharsets.UTF_8))

        assertNull(FarmBackupCodec.decodeOrNull(malformedEnvelope))
    }

    @Test
    fun backupWithMalformedExportedAtIsRejected() {
        val malformedEnvelope = "1\u001Fnot-a-timestamp\u001FZmFybQ=="

        assertNull(FarmBackupCodec.decodeOrNull(malformedEnvelope))
    }

    @Test
    fun backupEncodeUsesProvidedExportClock() {
        val farm = service.createFarm("Demo Farm")
        val exportedAt = OffsetDateTime.of(2024, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC)

        val encoded = FarmBackupCodec.encode(farm, exportedAt)
        val envelope = FarmBackupCodec.decode(encoded)

        assertEquals(2024, envelope.exportedAt.year)
        assertEquals(6, envelope.exportedAt.monthValue)
        assertEquals(1, envelope.exportedAt.dayOfMonth)
        assertEquals(12, envelope.exportedAt.hour)
        assertEquals(0, envelope.exportedAt.minute)
        assertEquals(ZoneOffset.UTC, envelope.exportedAt.offset)
    }

    @Test
    fun backupEnvelopeFormatIsByteStable() {
        val farm = service.createFarm("Demo Farm")
        service.addEntry(farm.id, FarmEntry(FarmEntryKind.LIVESTOCK, "Goat", 2))
        service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.EXPENSE,
                category = TransactionCategory.FEED,
                amountMinor = 1500,
                description = "Feed",
                occurredAt = "2024-01-01T12:00:00Z"
            )
        )
        val persisted = service.loadFarm(farm.id)!!
        val exportedAt = OffsetDateTime.of(2024, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC)
        val encoded = FarmBackupCodec.encode(persisted, exportedAt)

        val payload = Base64.getEncoder().encodeToString(
            listOf(
                "14", persisted.id, "Demo Farm", "LIVESTOCK:Goat:2", "NPR",
                "${persisted.transactions[0].id}\u001DEXPENSE\u001DFEED\u001D1500\u001DFeed\u001D2024-01-01T12:00:00Z\u001D",
                "", "", "", "", "", "", "", "", "", "", "", ""
            ).joinToString("\u001F")
                .toByteArray(StandardCharsets.UTF_8)
        )
        assertEquals("1\u001F2024-06-01T12:00:00Z\u001F$payload", encoded)
        assertEquals(ZoneOffset.UTC, FarmBackupCodec.decode(encoded).exportedAt.offset)
    }

    @Test
    fun transportExpenseTransactionRoundTripsThroughPersistenceAndBackup() {
        val farm = service.createFarm("Transport Farm")
        service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.EXPENSE,
                category = TransactionCategory.TRANSPORT,
                amountMinor = 15000,
                description = "Van hire to market",
                occurredAt = "2026-08-09T05:00:00Z"
            )
        )

        val loaded = service.loadFarm(farm.id)!!
        assertEquals(1, loaded.transactions.size)
        val transaction = loaded.transactions.first()
        assertEquals(TransactionType.EXPENSE, transaction.type)
        assertEquals(TransactionCategory.TRANSPORT, transaction.category)

        val persisted = FarmPersistenceCodec.decode(FarmPersistenceCodec.encode(loaded))
        assertEquals(TransactionCategory.TRANSPORT, persisted.transactions.first().category)

        val envelope = FarmBackupCodec.decode(FarmBackupCodec.encode(persisted))
        assertEquals(TransactionCategory.TRANSPORT, envelope.farm.transactions.first().category)
        assertEquals(15000L, envelope.farm.transactions.first().amountMinor)
    }

    @Test
    fun transportCategoryIsExpenseOnlyAndRejectedWhenTypeMismatches() {
        assertEquals(TransactionType.EXPENSE, TransactionCategory.TRANSPORT.type)

        val farm = FarmState(
            id = "farm-transport-mismatch",
            name = "Transport Farm",
            entries = mutableListOf(),
            transactions = mutableListOf(
                FarmTransaction(
                    id = "tx-1",
                    type = TransactionType.INCOME,
                    category = TransactionCategory.TRANSPORT,
                    amountMinor = 1000,
                    description = "Mismatch",
                    occurredAt = OffsetDateTime.parse("2024-01-01T12:00:00Z")
                )
            )
        )

        val encoded = FarmBackupCodec.encode(farm)
        assertNull(FarmBackupCodec.decodeOrNull(encoded))
    }

    @Test
    fun backupExportNormalizesToUtc() {
        val farm = service.createFarm("Demo Farm")
        val exportedAt = OffsetDateTime.of(2024, 6, 1, 12, 0, 0, 0, ZoneOffset.ofHours(5))

        val encoded = FarmBackupCodec.encode(farm, exportedAt)
        val envelope = FarmBackupCodec.decode(encoded)

        assertEquals("2024-06-01T07:00Z", envelope.exportedAt.toString())
        assertEquals(ZoneOffset.UTC, envelope.exportedAt.offset)
    }

    @Test
    fun postUpgradeNewTransactionAppearsInBackupExport() {
        // Simulate v0.1.0 farm with pre-existing transactions (3 txns, 1 entry)
        val farm = service.createFarm("MotoUpgradeFarm")
        service.addEntry(farm.id, FarmEntry(FarmEntryKind.LIVESTOCK, "Cow", 3))
        service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.INCOME,
                category = TransactionCategory.SALES,
                amountMinor = 120050,
                description = "Milk sale",
                occurredAt = "2026-08-05T05:48:00Z"
            )
        )
        service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.EXPENSE,
                category = TransactionCategory.FEED,
                amountMinor = 45000,
                description = "Feed purchase",
                occurredAt = "2026-08-01T12:15:00Z"
            )
        )
        service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.INCOME,
                category = TransactionCategory.SALES,
                amountMinor = 8000,
                description = "Egg sale",
                occurredAt = "2026-08-07T07:20:00Z"
            )
        )

        // Verify initial state
        var loadedFarm = service.loadFarm(farm.id)!!
        assertEquals(3, loadedFarm.transactions.size)
        assertEquals(1, loadedFarm.entries.size)

        // Simulate upgrade: just continue using the same service/store
        // (in real upgrade, the app process restarts but SharedPreferences persists)

        // Create a NEW transaction after "upgrade" (Post-upgrade milk sale)
        val newTx = service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.INCOME,
                category = TransactionCategory.SALES,
                amountMinor = 15000,
                description = "Post-upgrade milk sale",
                occurredAt = "2026-08-08T05:15:00Z"
            )
        )

        // Verify new transaction is in the farm
        loadedFarm = service.loadFarm(farm.id)!!
        assertEquals(4, loadedFarm.transactions.size)
        assertTrue(loadedFarm.transactions.any { it.id == newTx.id })
        assertTrue(loadedFarm.transactions.any { it.description == "Post-upgrade milk sale" })

        // Export backup (simulating EXPORT BACKUP button)
        val backupContent = FarmBackupCodec.encode(loadedFarm)
        val envelope = FarmBackupCodec.decode(backupContent)

        // Verify backup contains ALL 4 transactions including the new one
        assertEquals(4, envelope.farm.transactions.size)
        assertTrue("Backup must contain the post-upgrade transaction", envelope.farm.transactions.any { it.description == "Post-upgrade milk sale" })
        assertTrue("Backup must contain Milk sale", envelope.farm.transactions.any { it.description == "Milk sale" })
        assertTrue("Backup must contain Feed purchase", envelope.farm.transactions.any { it.description == "Feed purchase" })
        assertTrue("Backup must contain Egg sale", envelope.farm.transactions.any { it.description == "Egg sale" })

        // Verify entry is also in backup
        assertEquals(1, envelope.farm.entries.size)
        assertEquals("Cow", envelope.farm.entries.first().label)
        assertEquals(3, envelope.farm.entries.first().quantity)
    }

    @Test
    fun settlementCrudAddsUpdatesAndDeletes() {
        val farm = service.createFarm("Demo Farm")
        val party = service.addParty(farm.id, PartyDraft(name = "Dairy", role = PartyRole.CUSTOMER))
        val trade = service.addTrade(
            farm.id,
            TradeDraft(type = TradeType.SALE, partyId = party.id, totalMinor = 5000, occurredAt = "2024-01-01T12:00:00Z")
        )

        val first = service.addSettlement(
            farm.id,
            SettlementDraft(tradeId = trade.id, amountMinor = 2000, occurredAt = "2024-01-05T12:00:00Z", note = "Partial")
        )
        service.addSettlement(
            farm.id,
            SettlementDraft(tradeId = trade.id, amountMinor = 3000, occurredAt = "2024-01-10T12:00:00Z", note = "Final")
        )

        assertEquals(2, service.settlementsForTrade(farm.id, trade.id).size)
        assertEquals(PaymentStatus.PAID, service.tradePaymentSummary(farm.id, trade).status)

        val updated = service.updateSettlement(
            farm.id,
            first.id,
            SettlementDraft(tradeId = trade.id, amountMinor = 1000, occurredAt = "2024-01-05T12:00:00Z", note = "Correction")
        )
        assertEquals(1000, updated.amountMinor)
        assertEquals(4000, service.tradePaymentSummary(farm.id, trade).paidMinor)
        assertEquals(PaymentStatus.PARTIAL, service.tradePaymentSummary(farm.id, trade).status)

        service.deleteSettlement(farm.id, first.id)
        assertEquals(1, service.settlementsForTrade(farm.id, trade.id).size)
    }

    @Test
    fun settlementCannotExceedRemainingBalance() {
        val farm = service.createFarm("Demo Farm")
        val party = service.addParty(farm.id, PartyDraft(name = "Dairy", role = PartyRole.CUSTOMER))
        val trade = service.addTrade(
            farm.id,
            TradeDraft(type = TradeType.SALE, partyId = party.id, totalMinor = 5000, occurredAt = "2024-01-01T12:00:00Z")
        )
        service.addSettlement(
            farm.id,
            SettlementDraft(tradeId = trade.id, amountMinor = 3000, occurredAt = "2024-01-05T12:00:00Z")
        )

        try {
            service.addSettlement(
                farm.id,
                SettlementDraft(tradeId = trade.id, amountMinor = 3000, occurredAt = "2024-01-06T12:00:00Z")
            )
            fail("Expected IllegalArgumentException")
        } catch (exception: IllegalArgumentException) {
            assertEquals("Trade total cannot be less than the settled amount", exception.message)
        }

        assertEquals(3000, service.tradePaymentSummary(farm.id, trade).paidMinor)
    }

    @Test
    fun settlementHistoryOrdersNewestFirst() {
        val farm = service.createFarm("Demo Farm")
        val party = service.addParty(farm.id, PartyDraft(name = "Dairy", role = PartyRole.CUSTOMER))
        val trade = service.addTrade(
            farm.id,
            TradeDraft(type = TradeType.SALE, partyId = party.id, totalMinor = 5000, occurredAt = "2024-01-01T12:00:00Z")
        )
        val older = service.addSettlement(
            farm.id,
            SettlementDraft(tradeId = trade.id, amountMinor = 1000, occurredAt = "2024-01-02T09:00:00Z")
        )
        val newer = service.addSettlement(
            farm.id,
            SettlementDraft(tradeId = trade.id, amountMinor = 1000, occurredAt = "2024-01-03T09:00:00Z")
        )
        val middle = service.addSettlement(
            farm.id,
            SettlementDraft(tradeId = trade.id, amountMinor = 1000, occurredAt = "2024-01-02T18:00:00Z")
        )

        assertEquals(listOf(newer.id, middle.id, older.id), service.settlementsForTrade(farm.id, trade.id).map { it.id })
    }

    @Test
    fun deletingTradeWithSettlementsIsBlocked() {
        val farm = service.createFarm("Demo Farm")
        val party = service.addParty(farm.id, PartyDraft(name = "Dairy", role = PartyRole.CUSTOMER))
        val trade = service.addTradeWithInitialSettlement(
            farm.id,
            TradeDraft(type = TradeType.SALE, partyId = party.id, totalMinor = 5000, occurredAt = "2024-01-01T12:00:00Z"),
            initialSettlementMinor = 2000
        )

        try {
            service.deleteTrade(farm.id, trade.id)
            fail("Expected IllegalArgumentException")
        } catch (exception: IllegalArgumentException) {
            assertEquals("Trade cannot be deleted while payment records exist", exception.message)
        }

        assertEquals(1, service.trades(farm.id).size)
    }

    @Test
    fun schema5MigrationCreatesDeterministicOpeningSettlements() {
        val schema5 = "5\u001Ffarm-v5\u001FV5 Farm\u001F\u001FNPR\u001F\u001F" +
            "party-1\u001DSUPPLIER\u001DFeed Store\u001D\u001D\u001F" +
            "trade-1\u001DPURCHASE\u001Dparty-1\u001D10000\u001D3000\u001DFeed\u001D2024-01-01T12:00:00Z"

        val farm = FarmPersistenceCodec.decode(schema5)
        val again = FarmPersistenceCodec.decode(schema5)

        assertEquals(FarmState.CURRENT_FARM_SCHEMA_VERSION, farm.schemaVersion)
        assertEquals(1, farm.trades.size)
        assertEquals(1, farm.settlements.size)
        val settlement = farm.settlements.single()
        assertEquals("trade-1", settlement.tradeId)
        assertEquals(3000, settlement.amountMinor)
        assertEquals(farm.trades.single().occurredAt, settlement.occurredAt)
        assertEquals(settlement.id, again.settlements.single().id)
        assertEquals(7000L, farm.settlements.outstandingMinorFor(farm.trades.single()))
    }

    @Test
    fun resetFarmDataClearsAllRecordsAndKeepsFarmIdentity() {
        val farm = service.createFarm("Demo Farm", currencyCode = "NPR")
        service.addEntry(farm.id, FarmEntry(FarmEntryKind.LIVESTOCK, "Goat", 3))
        service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.EXPENSE,
                category = TransactionCategory.FEED,
                amountMinor = 5000,
                description = "Feed purchase",
                occurredAt = "2024-01-01T12:00:00Z"
            )
        )
        val party = service.addParty(farm.id, PartyDraft(name = "Dairy", role = PartyRole.CUSTOMER))
        service.addTradeWithInitialSettlement(
            farm.id,
            TradeDraft(type = TradeType.SALE, partyId = party.id, totalMinor = 5000, occurredAt = "2024-01-01T12:00:00Z"),
            initialSettlementMinor = 2000
        )

        service.resetFarmData(farm.id)

        val reset = service.loadFarm(farm.id)
        assertNotNull(reset)
        assertEquals(farm.id, reset?.id)
        assertEquals("Demo Farm", reset?.name)
        assertEquals("NPR", reset?.currencyCode)
        assertEquals(0, reset?.entries?.size)
        assertEquals(0, reset?.transactions?.size)
        assertEquals(0, reset?.parties?.size)
        assertEquals(0, reset?.trades?.size)
        assertEquals(0, reset?.settlements?.size)
        assertEquals(farm.id, service.currentFarmId())
    }

    @Test
    fun resetFarmDataRequiresAnExistingFarm() {
        try {
            service.resetFarmData("farm-missing")
            fail("Expected IllegalArgumentException")
        } catch (exception: IllegalArgumentException) {
            assertEquals("Unknown farm: farm-missing", exception.message)
        }
    }

    @Test
    fun deleteFarmRemovesFarmAndReturnsToNoFarmState() {
        val farm = service.createFarm("Demo Farm", currencyCode = "NPR")
        val party = service.addParty(farm.id, PartyDraft(name = "Dairy", role = PartyRole.CUSTOMER))
        service.addTradeWithInitialSettlement(
            farm.id,
            TradeDraft(type = TradeType.SALE, partyId = party.id, totalMinor = 5000, occurredAt = "2024-01-01T12:00:00Z"),
            initialSettlementMinor = 2000
        )

        service.deleteFarm(farm.id)

        assertNull(service.loadFarm(farm.id))
        assertNull(service.currentFarmId())
    }

    @Test
    fun deleteFarmKeepsCurrentFarmWhenDeletingAnotherFarm() {
        val first = service.createFarm("First")
        val second = service.createFarm("Second")

        service.deleteFarm(first.id)

        assertNull(service.loadFarm(first.id))
        assertEquals(second.id, service.currentFarmId())
    }

    @Test
    fun deleteFarmRequiresAnExistingFarm() {
        try {
            service.deleteFarm("farm-missing")
            fail("Expected IllegalArgumentException")
        } catch (exception: IllegalArgumentException) {
            assertEquals("Unknown farm: farm-missing", exception.message)
        }
    }

    @Test
    fun renameFarmChangesNameAndPreservesEverythingElse() {
        val farm = service.createFarm("Demo Farm", currencyCode = "NPR")
        service.addEntry(farm.id, FarmEntry(FarmEntryKind.LIVESTOCK, "Goat", 3))
        service.createTransaction(
            farm.id,
            FarmTransactionDraft(
                type = TransactionType.EXPENSE,
                category = TransactionCategory.FEED,
                amountMinor = 5000,
                description = "Feed purchase",
                occurredAt = "2024-01-01T12:00:00Z"
            )
        )

        val renamed = service.renameFarm(farm.id, "  New Name  ")

        assertEquals("New Name", renamed.name)
        val loaded = service.loadFarm(farm.id)
        assertNotNull(loaded)
        assertEquals(farm.id, loaded?.id)
        assertEquals("New Name", loaded?.name)
        assertEquals("NPR", loaded?.currencyCode)
        assertEquals(1, loaded?.entries?.size)
        assertEquals(1, loaded?.transactions?.size)
        assertEquals(farm.schemaVersion, loaded?.schemaVersion)
        assertEquals(farm.id, service.currentFarmId())
    }

    @Test
    fun renameFarmRequiresNonBlankNameAndKeepsExistingName() {
        val farm = service.createFarm("Demo Farm")

        try {
            service.renameFarm(farm.id, "   ")
            fail("Expected IllegalArgumentException")
        } catch (exception: IllegalArgumentException) {
            assertEquals("Farm name is required", exception.message)
        }

        assertEquals("Demo Farm", service.loadFarm(farm.id)?.name)
    }

    @Test
    fun renameFarmRequiresAnExistingFarm() {
        try {
            service.renameFarm("farm-missing", "New Name")
            fail("Expected IllegalArgumentException")
        } catch (exception: IllegalArgumentException) {
            assertEquals("Unknown farm: farm-missing", exception.message)
        }
    }
}
