package com.susankhya.kisab.domain

import com.susankhya.kisab.persistence.FarmPersistenceCodec
import java.time.OffsetDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * M5-04 Party Khata projection tests. All asserts run against the derived
 * [PartyLedger] built from authoritative Party → Trade → Settlement facts —
 * the projection itself is never persisted.
 */
class PartyLedgerTest {
    private lateinit var service: FarmSliceService

    private companion object {
        const val T = "2024-01-01T12:00:00Z"
    }

    @Before
    fun setUp() {
        service = FarmSliceService(InMemoryFarmStore())
    }

    @Test
    fun saleOnlyProducesReceivableRunningBalance() {
        val farm = service.createFarm("Demo Farm")
        val party = service.addParty(farm.id, PartyDraft(name = "Ram", role = PartyRole.CUSTOMER))
        service.addTrade(
            farm.id,
            TradeDraft(type = TradeType.SALE, partyId = party.id, totalMinor = 10000, occurredAt = T)
        )

        val ledger = service.partyLedger(farm.id, party.id)

        assertEquals(
            listOf(PartyLedgerEntryType.SALE),
            ledger.entries.map { it.sourceType }
        )
        assertEquals(10000, ledger.entries.single().deltaMinor)
        assertEquals(10000, ledger.entries.single().runningBalanceMinor)
        assertEquals(10000, ledger.summary.toReceiveMinor)
        assertEquals(0, ledger.summary.toPayMinor)
        assertEquals(10000, ledger.summary.netMinor)
    }

    @Test
    fun salePlusReceiptBuildsCorrectRunningBalance() {
        val farm = service.createFarm("Demo Farm")
        val party = service.addParty(farm.id, PartyDraft(name = "Ram", role = PartyRole.CUSTOMER))
        val trade = service.addTrade(
            farm.id,
            TradeDraft(type = TradeType.SALE, partyId = party.id, totalMinor = 10000, occurredAt = T)
        )
        service.addSettlement(
            farm.id,
            SettlementDraft(tradeId = trade.id, amountMinor = 4000, occurredAt = "2024-01-05T12:00:00Z")
        )

        val ledger = service.partyLedger(farm.id, party.id)

        assertEquals(
            listOf(PartyLedgerEntryType.SALE, PartyLedgerEntryType.PAYMENT_RECEIVED),
            ledger.entries.map { it.sourceType }
        )
        assertEquals(listOf(10000L, -4000L), ledger.entries.map { it.deltaMinor })
        assertEquals(listOf(10000L, 6000L), ledger.entries.map { it.runningBalanceMinor })
        assertEquals(6000, ledger.summary.toReceiveMinor)
        assertEquals(0, ledger.summary.toPayMinor)
        assertEquals(6000, ledger.summary.netMinor)
    }

    @Test
    fun purchaseOnlyProducesPayableRunningBalance() {
        val farm = service.createFarm("Demo Farm")
        val party = service.addParty(farm.id, PartyDraft(name = "Seed Store", role = PartyRole.SUPPLIER))
        service.addTrade(
            farm.id,
            TradeDraft(type = TradeType.PURCHASE, partyId = party.id, totalMinor = 8000, occurredAt = T)
        )

        val ledger = service.partyLedger(farm.id, party.id)

        assertEquals(
            listOf(PartyLedgerEntryType.PURCHASE),
            ledger.entries.map { it.sourceType }
        )
        assertEquals(-8000, ledger.entries.single().deltaMinor)
        assertEquals(-8000, ledger.entries.single().runningBalanceMinor)
        assertEquals(0, ledger.summary.toReceiveMinor)
        assertEquals(8000, ledger.summary.toPayMinor)
        assertEquals(-8000, ledger.summary.netMinor)
    }

    @Test
    fun purchasePlusPaymentBuildsCorrectRunningBalance() {
        val farm = service.createFarm("Demo Farm")
        val party = service.addParty(farm.id, PartyDraft(name = "Seed Store", role = PartyRole.SUPPLIER))
        val trade = service.addTrade(
            farm.id,
            TradeDraft(type = TradeType.PURCHASE, partyId = party.id, totalMinor = 8000, occurredAt = T)
        )
        service.addSettlement(
            farm.id,
            SettlementDraft(tradeId = trade.id, amountMinor = 3000, occurredAt = "2024-01-05T12:00:00Z")
        )

        val ledger = service.partyLedger(farm.id, party.id)

        assertEquals(
            listOf(PartyLedgerEntryType.PURCHASE, PartyLedgerEntryType.PAYMENT_MADE),
            ledger.entries.map { it.sourceType }
        )
        assertEquals(listOf(-8000L, 3000L), ledger.entries.map { it.deltaMinor })
        assertEquals(listOf(-8000L, -5000L), ledger.entries.map { it.runningBalanceMinor })
        assertEquals(5000, ledger.summary.toPayMinor)
        assertEquals(-5000, ledger.summary.netMinor)
    }

    @Test
    fun bothPartyKeepsTradesSeparateAndNetsInformationally() {
        val farm = service.createFarm("Demo Farm")
        val party = service.addParty(farm.id, PartyDraft(name = "Co-op", role = PartyRole.BOTH))
        val sale = service.addTrade(
            farm.id,
            TradeDraft(type = TradeType.SALE, partyId = party.id, totalMinor = 10000, occurredAt = T)
        )
        val purchase = service.addTrade(
            farm.id,
            TradeDraft(type = TradeType.PURCHASE, partyId = party.id, totalMinor = 4000, occurredAt = "2024-01-10T12:00:00Z")
        )

        val ledger = service.partyLedger(farm.id, party.id)

        assertEquals(10000, ledger.summary.toReceiveMinor)
        assertEquals(4000, ledger.summary.toPayMinor)
        assertEquals(6000, ledger.summary.netMinor)
        assertEquals(listOf(10000L, 6000L), ledger.entries.map { it.runningBalanceMinor })

        // The informational net MUST NOT settle either trade.
        assertEquals(PaymentStatus.UNPAID, service.tradePaymentSummary(farm.id, sale).status)
        assertEquals(PaymentStatus.UNPAID, service.tradePaymentSummary(farm.id, purchase).status)
        assertEquals(10000, service.tradePaymentSummary(farm.id, sale).outstandingMinor)
        assertEquals(4000, service.tradePaymentSummary(farm.id, purchase).outstandingMinor)
    }

    @Test
    fun mixedBothPartyHistoryShowsEachSettlementIndependent() {
        val farm = service.createFarm("Demo Farm")
        val party = service.addParty(farm.id, PartyDraft(name = "Co-op", role = PartyRole.BOTH))
        val sale = service.addTrade(
            farm.id,
            TradeDraft(type = TradeType.SALE, partyId = party.id, totalMinor = 10000, occurredAt = "2024-01-01T12:00:00Z")
        )
        val purchase = service.addTrade(
            farm.id,
            TradeDraft(type = TradeType.PURCHASE, partyId = party.id, totalMinor = 3000, occurredAt = "2024-01-02T12:00:00Z")
        )
        service.addSettlement(
            farm.id,
            SettlementDraft(tradeId = sale.id, amountMinor = 5000, occurredAt = "2024-01-03T12:00:00Z")
        )
        service.addSettlement(
            farm.id,
            SettlementDraft(tradeId = purchase.id, amountMinor = 1000, occurredAt = "2024-01-04T12:00:00Z")
        )

        val ledger = service.partyLedger(farm.id, party.id)

        assertEquals(
            listOf(
                PartyLedgerEntryType.SALE,
                PartyLedgerEntryType.PURCHASE,
                PartyLedgerEntryType.PAYMENT_RECEIVED,
                PartyLedgerEntryType.PAYMENT_MADE
            ),
            ledger.entries.map { it.sourceType }
        )
        assertEquals(listOf(10000L, -3000L, -5000L, 1000L), ledger.entries.map { it.deltaMinor })
        assertEquals(listOf(10000L, 7000L, 2000L, 3000L), ledger.entries.map { it.runningBalanceMinor })
        assertEquals(5000, ledger.summary.toReceiveMinor)
        assertEquals(2000, ledger.summary.toPayMinor)
        assertEquals(3000, ledger.summary.netMinor)
        assertEquals(4, ledger.entries.size)
    }

    @Test
    fun multipleSettlementsAppearIndependently() {
        val farm = service.createFarm("Demo Farm")
        val party = service.addParty(farm.id, PartyDraft(name = "Ram", role = PartyRole.CUSTOMER))
        val trade = service.addTrade(
            farm.id,
            TradeDraft(type = TradeType.SALE, partyId = party.id, totalMinor = 5000, occurredAt = T)
        )
        service.addSettlement(
            farm.id,
            SettlementDraft(tradeId = trade.id, amountMinor = 1000, occurredAt = "2024-01-05T12:00:00Z", note = "First")
        )
        service.addSettlement(
            farm.id,
            SettlementDraft(tradeId = trade.id, amountMinor = 2000, occurredAt = "2024-01-06T12:00:00Z", note = "Second")
        )

        val ledger = service.partyLedger(farm.id, party.id)

        assertEquals(3, ledger.entries.size)
        assertEquals(
            listOf(PartyLedgerEntryType.SALE, PartyLedgerEntryType.PAYMENT_RECEIVED, PartyLedgerEntryType.PAYMENT_RECEIVED),
            ledger.entries.map { it.sourceType }
        )
        assertEquals(listOf(5000L, 4000L, 2000L), ledger.entries.map { it.runningBalanceMinor })
        assertEquals("First", ledger.entries[1].description)
        assertEquals("Second", ledger.entries[2].description)
    }

    @Test
    fun sameTimestampTradeAppearsBeforeItsOpeningSettlement() {
        val farm = service.createFarm("Demo Farm")
        val party = service.addParty(farm.id, PartyDraft(name = "Ram", role = PartyRole.CUSTOMER))
        // Mirrors M5-03's v5->v6 migration: opening settlement at the trade's own timestamp.
        val trade = service.addTrade(
            farm.id,
            TradeDraft(type = TradeType.SALE, partyId = party.id, totalMinor = 10000, occurredAt = T)
        )
        service.addSettlement(
            farm.id,
            SettlementDraft(tradeId = trade.id, amountMinor = 4000, occurredAt = T)
        )

        val ledger = service.partyLedger(farm.id, party.id)

        assertEquals(2, ledger.entries.size)
        assertEquals(PartyLedgerEntryType.SALE, ledger.entries[0].sourceType)
        assertEquals(PartyLedgerEntryType.PAYMENT_RECEIVED, ledger.entries[1].sourceType)
        assertEquals(listOf(10000L, 6000L), ledger.entries.map { it.runningBalanceMinor })
        assertEquals(6000, ledger.summary.toReceiveMinor)
    }

    @Test
    fun migratedSchema5PayloadProjectsTradeBeforeOpeningSettlement() {
        // Real migration path, not hand-built same-time objects: a schema-v5
        // payload with `paidMinor > 0` decodes through FarmPersistenceCodec into
        // a deterministic opening settlement dated at the trade's own timestamp.
        // Projecting that decoded state must place the Trade before its opening
        // Settlement and compute the correct running balance.
        val schema5 = "5\u001Ffarm-v5\u001FV5 Farm\u001F\u001FNPR\u001F\u001F" +
            "party-1\u001DCUSTOMER\u001DRam\u001D\u001D\u001F" +
            "trade-1\u001DSALE\u001Dparty-1\u001D10000\u001D4000\u001DMilk\u001D2024-01-01T12:00:00Z"

        val farm = FarmPersistenceCodec.decode(schema5)

        assertEquals(1, farm.trades.size)
        assertEquals(1, farm.settlements.size)
        assertEquals(farm.trades.single().occurredAt, farm.settlements.single().occurredAt)

        val ledger = farm.partyLedger("party-1")

        assertEquals(2, ledger.entries.size)
        assertEquals(PartyLedgerEntryType.SALE, ledger.entries[0].sourceType)
        assertEquals(PartyLedgerEntryType.PAYMENT_RECEIVED, ledger.entries[1].sourceType)
        assertEquals(listOf(10000L, -4000L), ledger.entries.map { it.deltaMinor })
        assertEquals(listOf(10000L, 6000L), ledger.entries.map { it.runningBalanceMinor })
        assertEquals(6000, ledger.summary.toReceiveMinor)
        assertEquals(0, ledger.summary.toPayMinor)
        assertEquals(6000, ledger.summary.netMinor)
    }

    @Test
    fun projectionRejectsSettlementOfMissingTradeInsteadOfSilentlyOmittingIt() {
        val t = OffsetDateTime.parse(T)
        val party = Party(id = "p1", name = "Ram", role = PartyRole.CUSTOMER)
        val trades = listOf(
            Trade(id = "t1", type = TradeType.SALE, partyId = "p1", totalMinor = 10000, description = "", occurredAt = t)
        )
        val orphanSettlement = Settlement(
            id = "s-orphan",
            tradeId = "missing-trade",
            amountMinor = 4000,
            occurredAt = t,
            note = ""
        )

        try {
            buildPartyLedger(party, trades, listOf(orphanSettlement))
            fail("Expected IllegalArgumentException for an orphan settlement")
        } catch (exception: IllegalArgumentException) {
            assertEquals(
                "Settlement s-orphan references missing trade missing-trade",
                exception.message
            )
        }
    }

    @Test
    fun orderingIsDeterministicAcrossInsertionOrderAndCallCount() {
        val t = OffsetDateTime.parse(T)
        val party = Party(id = "p1", name = "Ram", role = PartyRole.BOTH)
        val trades = listOf(
            Trade(id = "t-b", type = TradeType.SALE, partyId = "p1", totalMinor = 2000, description = "", occurredAt = t),
            Trade(id = "t-a", type = TradeType.SALE, partyId = "p1", totalMinor = 1000, description = "", occurredAt = t)
        )
        val settlements = listOf(
            Settlement(id = "s-b", tradeId = "t-b", amountMinor = 1000, occurredAt = t, note = ""),
            Settlement(id = "s-a", tradeId = "t-a", amountMinor = 500, occurredAt = t, note = "")
        )

        val forward = buildPartyLedger(party, trades, settlements)
        val reversed = buildPartyLedger(party, trades.reversed(), settlements.reversed())
        val again = buildPartyLedger(party, trades, settlements)

        assertEquals(forward.entries, again.entries)
        assertEquals(forward.entries, reversed.entries)
        // Same instant: obligations come before their own settlements.
        assertEquals(PartyLedgerEntryType.SALE, forward.entries.first().sourceType)
        assertEquals(4, forward.entries.size)
    }

    @Test
    fun cashNoPartyTradeIsExcludedFromAnyPartyKhata() {
        val farm = service.createFarm("Demo Farm")
        val party = service.addParty(farm.id, PartyDraft(name = "Ram", role = PartyRole.CUSTOMER))
        service.addTrade(
            farm.id,
            TradeDraft(type = TradeType.SALE, partyId = party.id, totalMinor = 10000, occurredAt = T)
        )
        // M5-02 cash trade: fully paid, no party.
        service.addTradeWithInitialSettlement(
            farm.id,
            TradeDraft(type = TradeType.SALE, partyId = null, totalMinor = 2000, occurredAt = "2024-01-10T12:00:00Z"),
            initialSettlementMinor = 2000
        )

        val ledger = service.partyLedger(farm.id, party.id)

        assertEquals(1, ledger.entries.size)
        assertEquals(10000, ledger.entries.single().runningBalanceMinor)
        assertEquals(10000, ledger.summary.toReceiveMinor)
        assertTrue(ledger.entries.none { it.sourceType == PartyLedgerEntryType.PAYMENT_RECEIVED })
    }

    @Test
    fun projectionReflectsUnderlyingEditsAndDeletes() {
        val farm = service.createFarm("Demo Farm")
        val party = service.addParty(farm.id, PartyDraft(name = "Ram", role = PartyRole.CUSTOMER))
        val trade = service.addTrade(
            farm.id,
            TradeDraft(type = TradeType.SALE, partyId = party.id, totalMinor = 10000, occurredAt = T)
        )

        assertEquals(10000, service.partyLedger(farm.id, party.id).summary.toReceiveMinor)

        val settlement = service.addSettlement(
            farm.id,
            SettlementDraft(tradeId = trade.id, amountMinor = 4000, occurredAt = "2024-01-05T12:00:00Z")
        )
        assertEquals(6000, service.partyLedger(farm.id, party.id).summary.toReceiveMinor)

        service.updateSettlement(
            farm.id,
            settlement.id,
            SettlementDraft(tradeId = trade.id, amountMinor = 7000, occurredAt = "2024-01-05T12:00:00Z")
        )
        assertEquals(3000, service.partyLedger(farm.id, party.id).summary.toReceiveMinor)

        service.deleteSettlement(farm.id, settlement.id)
        assertEquals(10000, service.partyLedger(farm.id, party.id).summary.toReceiveMinor)
        assertEquals(1, service.partyLedger(farm.id, party.id).entries.size)
    }

    @Test
    fun khataProjectionDoesNotChangeEncodedFarmState() {
        val farm = service.createFarm("Demo Farm")
        val party = service.addParty(farm.id, PartyDraft(name = "Ram", role = PartyRole.CUSTOMER))
        service.addTrade(
            farm.id,
            TradeDraft(type = TradeType.SALE, partyId = party.id, totalMinor = 10000, occurredAt = T)
        )

        val before = FarmPersistenceCodec.encode(service.loadFarm(farm.id)!!)

        service.partyLedger(farm.id, party.id)
        service.partyLedgerSummary(farm.id, party.id)

        val after = FarmPersistenceCodec.encode(service.loadFarm(farm.id)!!)

        assertEquals(before, after)
        assertEquals(7, FarmState.CURRENT_FARM_SCHEMA_VERSION)
        assertEquals(7, FarmPersistenceCodec.decode(after).schemaVersion)
    }

    @Test
    fun finalRunningBalanceEqualsNetPosition() {
        val farm = service.createFarm("Demo Farm")
        val party = service.addParty(farm.id, PartyDraft(name = "Co-op", role = PartyRole.BOTH))
        val sale = service.addTrade(
            farm.id,
            TradeDraft(type = TradeType.SALE, partyId = party.id, totalMinor = 10000, occurredAt = "2024-01-01T12:00:00Z")
        )
        val purchase = service.addTrade(
            farm.id,
            TradeDraft(type = TradeType.PURCHASE, partyId = party.id, totalMinor = 4000, occurredAt = "2024-01-02T12:00:00Z")
        )
        service.addSettlement(
            farm.id,
            SettlementDraft(tradeId = sale.id, amountMinor = 5000, occurredAt = "2024-01-03T12:00:00Z")
        )
        service.addSettlement(
            farm.id,
            SettlementDraft(tradeId = purchase.id, amountMinor = 3000, occurredAt = "2024-01-04T12:00:00Z")
        )

        val ledger = service.partyLedger(farm.id, party.id)

        assertEquals(ledger.summary.netMinor, ledger.entries.last().runningBalanceMinor)
        assertEquals(4000, ledger.summary.netMinor)
        assertEquals(listOf(5000L, 1000L, 4000L), listOf(
            ledger.summary.toReceiveMinor,
            ledger.summary.toPayMinor,
            ledger.summary.netMinor
        ))
    }

    @Test
    fun emptyKhataShowsNoRowsForPartyWithoutTrades() {
        val farm = service.createFarm("Demo Farm")
        val party = service.addParty(farm.id, PartyDraft(name = "Ram", role = PartyRole.CUSTOMER))

        val ledger = service.partyLedger(farm.id, party.id)

        assertTrue(ledger.entries.isEmpty())
        assertEquals(0, ledger.summary.toReceiveMinor)
        assertEquals(0, ledger.summary.toPayMinor)
        assertEquals(0, ledger.summary.netMinor)
    }

    @Test
    fun unknownPartyFailsClearly() {
        val farm = service.createFarm("Demo Farm")
        try {
            service.partyLedger(farm.id, "missing")
            fail("Expected IllegalArgumentException")
        } catch (exception: IllegalArgumentException) {
            assertEquals("Party not found: missing", exception.message)
        }
    }
}