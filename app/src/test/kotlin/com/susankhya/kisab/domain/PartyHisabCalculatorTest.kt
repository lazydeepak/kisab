package com.susankhya.kisab.domain

import com.susankhya.kisab.persistence.FarmBackupCodec
import com.susankhya.kisab.persistence.FarmPersistenceCodec
import java.time.OffsetDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PartyHisabCalculatorTest {
    private val service = FarmSliceService(InMemoryFarmStore())
    private val zone = ZoneId.of("Asia/Kathmandu")
    private val now = OffsetDateTime.parse("2024-02-20T12:00:00Z")
    private val exportedAt = OffsetDateTime.parse("2024-02-20T13:00:00Z")

    private fun createFarm(): String = service.createFarm("Demo Farm").id

    private fun addParty(farmId: String, name: String, role: PartyRole): Party =
        service.addParty(farmId, PartyDraft(name = name, role = role))

    private fun result(farmId: String, partyId: String, preset: FinancialPeriodPreset): PartyHisabResult =
        service.partyHisab(farmId, partyId, preset, now, zone)

    @Test
    fun partyWithoutFactsHasZeroActivityAndPosition() {
        val farmId = createFarm()
        val party = addParty(farmId, "Ram", PartyRole.CUSTOMER)

        val result = result(farmId, party.id, FinancialPeriodPreset.ALL_TIME)

        assertEquals(party, result.party)
        assertEquals(PartyHisabActivity(0, 0, 0, 0), result.activity)
        assertEquals(PartyHisabPosition(0, 0, 0), result.position)
    }

    @Test
    fun missingPartyIsRejected() {
        val farmId = createFarm()

        assertThrows(IllegalArgumentException::class.java) {
            result(farmId, "missing", FinancialPeriodPreset.ALL_TIME)
        }
    }

    @Test
    fun activityIsIsolatedByPartyAndPeriod() {
        val farmId = createFarm()
        val ram = addParty(farmId, "Ram", PartyRole.CUSTOMER)
        val sita = addParty(farmId, "Sita", PartyRole.CUSTOMER)
        val ramSale = service.addTrade(
            farmId,
            TradeDraft(TradeType.SALE, ram.id, 10_000, occurredAt = "2024-02-05T12:00:00Z")
        )
        service.addSettlement(
            farmId,
            SettlementDraft(ramSale.id, 3_000, occurredAt = "2024-02-10T12:00:00Z")
        )
        service.addTrade(
            farmId,
            TradeDraft(TradeType.SALE, sita.id, 99_000, occurredAt = "2024-02-06T12:00:00Z")
        )
        service.addTrade(
            farmId,
            TradeDraft(TradeType.SALE, ram.id, 5_000, occurredAt = "2024-01-05T12:00:00Z")
        )

        val result = result(farmId, ram.id, FinancialPeriodPreset.THIS_MONTH)

        assertEquals(10_000, result.activity.salesMinor)
        assertEquals(3_000, result.activity.paymentsReceivedMinor)
        assertEquals(0, result.activity.purchasesMinor)
        assertEquals(12_000, result.position.toReceiveMinor)
    }

    @Test
    fun periodIsStartInclusiveAndEndExclusive() {
        val farmId = createFarm()
        val party = addParty(farmId, "Ram", PartyRole.CUSTOMER)
        val period = FarmFinancialPeriods.periodFor(FinancialPeriodPreset.LAST_30_DAYS, now, zone)
        service.addTrade(
            farmId,
            TradeDraft(TradeType.SALE, party.id, 100, occurredAt = period.startInclusive.toString())
        )
        service.addTrade(
            farmId,
            TradeDraft(TradeType.SALE, party.id, 200, occurredAt = period.startInclusive.minusNanos(1).toString())
        )
        service.addTrade(
            farmId,
            TradeDraft(TradeType.SALE, party.id, 400, occurredAt = period.endExclusive.minusNanos(1).toString())
        )
        service.addTrade(
            farmId,
            TradeDraft(TradeType.SALE, party.id, 800, occurredAt = period.endExclusive.toString())
        )

        val result = result(farmId, party.id, FinancialPeriodPreset.LAST_30_DAYS)

        assertEquals(500, result.activity.salesMinor)
        assertEquals(700, result.position.toReceiveMinor)
    }

    @Test
    fun positionUsesAllPreCutoffFactsAndExcludesSettlementAtCutoff() {
        val farmId = createFarm()
        val party = addParty(farmId, "Ram", PartyRole.CUSTOMER)
        val period = FarmFinancialPeriods.periodFor(FinancialPeriodPreset.LAST_30_DAYS, now, zone)
        val sale = service.addTrade(
            farmId,
            TradeDraft(TradeType.SALE, party.id, 10_000, occurredAt = period.startInclusive.minusDays(10).toString())
        )
        service.addSettlement(
            farmId,
            SettlementDraft(sale.id, 2_000, occurredAt = period.startInclusive.minusDays(1).toString())
        )
        service.addSettlement(
            farmId,
            SettlementDraft(sale.id, 3_000, occurredAt = period.startInclusive.plusDays(1).toString())
        )
        service.addSettlement(
            farmId,
            SettlementDraft(sale.id, 1_000, occurredAt = period.endExclusive.toString())
        )

        val result = result(farmId, party.id, FinancialPeriodPreset.LAST_30_DAYS)

        assertEquals(0, result.activity.salesMinor)
        assertEquals(3_000, result.activity.paymentsReceivedMinor)
        assertEquals(5_000, result.position.toReceiveMinor)
    }

    @Test
    fun bothRoleKeepsSaleAndPurchaseSidesGrossAndNetInformational() {
        val farmId = createFarm()
        val party = addParty(farmId, "Co-op", PartyRole.BOTH)
        val sale = service.addTrade(
            farmId,
            TradeDraft(TradeType.SALE, party.id, 10_000, occurredAt = "2024-02-05T12:00:00Z")
        )
        val purchase = service.addTrade(
            farmId,
            TradeDraft(TradeType.PURCHASE, party.id, 8_000, occurredAt = "2024-02-06T12:00:00Z")
        )
        service.addSettlement(farmId, SettlementDraft(sale.id, 4_000, occurredAt = "2024-02-07T12:00:00Z"))
        service.addSettlement(farmId, SettlementDraft(purchase.id, 3_000, occurredAt = "2024-02-08T12:00:00Z"))

        val result = result(farmId, party.id, FinancialPeriodPreset.ALL_TIME)

        assertEquals(PartyHisabActivity(10_000, 8_000, 4_000, 3_000), result.activity)
        assertEquals(PartyHisabPosition(6_000, 5_000, 1_000), result.position)
    }

    @Test
    fun fullySettledTradesContributeActivityButNoPosition() {
        val farmId = createFarm()
        val party = addParty(farmId, "Co-op", PartyRole.BOTH)
        val sale = service.addTrade(
            farmId,
            TradeDraft(TradeType.SALE, party.id, 7_000, occurredAt = "2024-02-05T12:00:00Z")
        )
        val purchase = service.addTrade(
            farmId,
            TradeDraft(TradeType.PURCHASE, party.id, 4_000, occurredAt = "2024-02-06T12:00:00Z")
        )
        service.addSettlement(farmId, SettlementDraft(sale.id, 7_000, occurredAt = "2024-02-07T12:00:00Z"))
        service.addSettlement(farmId, SettlementDraft(purchase.id, 4_000, occurredAt = "2024-02-08T12:00:00Z"))

        val result = result(farmId, party.id, FinancialPeriodPreset.ALL_TIME)

        assertEquals(PartyHisabActivity(7_000, 4_000, 7_000, 4_000), result.activity)
        assertEquals(PartyHisabPosition(0, 0, 0), result.position)
    }

    @Test
    fun orphanSettlementIsRejectedClearly() {
        val farm = FarmState(id = "farm", name = "Farm")
        val party = Party(id = "party", name = "Ram", role = PartyRole.CUSTOMER)
        farm.parties.add(party)
        farm.settlements.add(
            Settlement(
                id = "orphan",
                tradeId = "missing",
                amountMinor = 1,
                occurredAt = now,
                note = ""
            )
        )

        assertThrows(IllegalArgumentException::class.java) {
            farm.partyHisab(party.id, FinancialPeriodPreset.ALL_TIME, now, zone)
        }
    }

    @Test
    fun activityOverflowThrowsInsteadOfWrapping() {
        val farmId = createFarm()
        val party = addParty(farmId, "Ram", PartyRole.CUSTOMER)
        service.addTrade(
            farmId,
            TradeDraft(TradeType.SALE, party.id, Long.MAX_VALUE, occurredAt = "2024-02-05T12:00:00Z")
        )
        service.addTrade(
            farmId,
            TradeDraft(TradeType.SALE, party.id, Long.MAX_VALUE, occurredAt = "2024-02-06T12:00:00Z")
        )

        assertThrows(ArithmeticException::class.java) {
            result(farmId, party.id, FinancialPeriodPreset.ALL_TIME)
        }
    }

    @Test
    fun settlementActivityOverflowThrowsInsteadOfWrapping() {
        val farmId = createFarm()
        val party = addParty(farmId, "Ram", PartyRole.CUSTOMER)
        val period = FarmFinancialPeriods.periodFor(FinancialPeriodPreset.LAST_30_DAYS, now, zone)
        val first = service.addTrade(
            farmId,
            TradeDraft(
                TradeType.SALE,
                party.id,
                Long.MAX_VALUE,
                occurredAt = period.startInclusive.minusDays(2).toString()
            )
        )
        val second = service.addTrade(
            farmId,
            TradeDraft(
                TradeType.SALE,
                party.id,
                Long.MAX_VALUE,
                occurredAt = period.startInclusive.minusDays(1).toString()
            )
        )
        service.addSettlement(
            farmId,
            SettlementDraft(first.id, Long.MAX_VALUE, occurredAt = period.startInclusive.plusDays(1).toString())
        )
        service.addSettlement(
            farmId,
            SettlementDraft(second.id, Long.MAX_VALUE, occurredAt = period.startInclusive.plusDays(2).toString())
        )

        assertThrows(ArithmeticException::class.java) {
            result(farmId, party.id, FinancialPeriodPreset.LAST_30_DAYS)
        }
    }

    @Test
    fun positionOverflowThrowsInsteadOfWrapping() {
        val farmId = createFarm()
        val party = addParty(farmId, "Ram", PartyRole.CUSTOMER)
        val period = FarmFinancialPeriods.periodFor(FinancialPeriodPreset.LAST_30_DAYS, now, zone)
        service.addTrade(
            farmId,
            TradeDraft(
                TradeType.SALE,
                party.id,
                Long.MAX_VALUE,
                occurredAt = period.startInclusive.minusDays(2).toString()
            )
        )
        service.addTrade(
            farmId,
            TradeDraft(
                TradeType.SALE,
                party.id,
                Long.MAX_VALUE,
                occurredAt = period.startInclusive.minusDays(1).toString()
            )
        )

        assertThrows(ArithmeticException::class.java) {
            result(farmId, party.id, FinancialPeriodPreset.LAST_30_DAYS)
        }
    }

    @Test
    fun projectionDoesNotMutateSchemaV6PersistenceOrBackup() {
        val farmId = createFarm()
        val party = addParty(farmId, "Ram", PartyRole.CUSTOMER)
        service.addTrade(
            farmId,
            TradeDraft(TradeType.SALE, party.id, 10_000, occurredAt = "2024-02-05T12:00:00Z")
        )
        val farm = service.loadFarm(farmId)!!
        val persistence = FarmPersistenceCodec.encode(farm)
        val backup = FarmBackupCodec.encode(farm, exportedAt)
        val decodedFarm = FarmPersistenceCodec.decode(persistence)
        val decodedBackupFarm = FarmBackupCodec.decode(backup).farm

        decodedFarm.partyHisab(party.id, FinancialPeriodPreset.ALL_TIME, now, zone)
        decodedBackupFarm.partyHisab(party.id, FinancialPeriodPreset.ALL_TIME, now, zone)

        assertEquals(7, decodedFarm.schemaVersion)
        assertEquals(persistence, FarmPersistenceCodec.encode(decodedFarm))
        assertEquals(backup, FarmBackupCodec.encode(decodedBackupFarm, exportedAt))
    }

    @Test
    fun identicalFactsProduceIdenticalResults() {
        val farmId = createFarm()
        val party = addParty(farmId, "Ram", PartyRole.CUSTOMER)
        service.addTrade(
            farmId,
            TradeDraft(TradeType.SALE, party.id, 10_000, occurredAt = "2024-02-05T12:00:00Z")
        )

        assertEquals(
            result(farmId, party.id, FinancialPeriodPreset.ALL_TIME),
            result(farmId, party.id, FinancialPeriodPreset.ALL_TIME)
        )
    }
}
