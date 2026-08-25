package com.susankhya.kisab.domain

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreditSalesMetricTest {
    private val zone = ZoneId.of("Asia/Kathmandu")
    private val now = OffsetDateTime.parse("2026-08-16T10:00:00Z")

    private fun setup(): Triple<FarmSliceService, FarmState, Party> {
        val service = FarmSliceService()
        val farm = service.createFarm("Farm")
        val party = service.addParty(farm.id, PartyDraft("Ram", PartyRole.CUSTOMER))
        return Triple(service, farm, party)
    }

    @Test
    fun fullCashCreditAndPartialSalesUseInitialPaymentOnly() {
        val (service, farm, party) = setup()
        val cash = service.addTradeWithInitialSettlement(farm.id, TradeDraft(TradeType.SALE, party.id, 1_000, occurredAt = "2026-08-16T08:00:00Z"), 1_000)
        service.addTradeWithInitialSettlement(farm.id, TradeDraft(TradeType.SALE, party.id, 2_000, occurredAt = "2026-08-16T08:05:00Z"), null)
        service.addTradeWithInitialSettlement(farm.id, TradeDraft(TradeType.SALE, party.id, 1_000, occurredAt = "2026-08-16T08:10:00Z"), 400)

        assertEquals(2_600L, service.loadFarm(farm.id)!!.farmerOverview(now, zone).daily.creditSalesMinor)
        assertTrue(service.loadFarm(farm.id)!!.settlements.first { it.tradeId == cash.id }.isInitialPayment)
    }

    @Test
    fun laterSameDayAndNextDayPaymentsDoNotChangeOriginalCreditSales() {
        val (service, farm, party) = setup()
        val sale = service.addTradeWithInitialSettlement(farm.id, TradeDraft(TradeType.SALE, party.id, 1_000, occurredAt = "2026-08-16T08:00:00Z"), 400)
        service.addSettlement(farm.id, SettlementDraft(sale.id, 200, "2026-08-16T09:00:00Z"))
        assertEquals(600L, service.loadFarm(farm.id)!!.farmerOverview(now, zone).daily.creditSalesMinor)
        service.addSettlement(farm.id, SettlementDraft(sale.id, 200, "2026-08-17T09:00:00Z"))
        assertEquals(600L, service.loadFarm(farm.id)!!.farmerOverview(now, zone).daily.creditSalesMinor)
    }

    @Test
    fun oldDebtPaidTodayDoesNotBecomeTodayCreditSales() {
        val (service, farm, party) = setup()
        val old = service.addTradeWithInitialSettlement(farm.id, TradeDraft(TradeType.SALE, party.id, 1_000, occurredAt = "2026-08-15T08:00:00Z"), null)
        service.addTradeWithInitialSettlement(farm.id, TradeDraft(TradeType.SALE, party.id, 500, occurredAt = "2026-08-16T08:00:00Z"), null)
        service.addSettlement(farm.id, SettlementDraft(old.id, 1_000, "2026-08-16T09:00:00Z"))
        assertEquals(500L, service.loadFarm(farm.id)!!.farmerOverview(now, zone).daily.creditSalesMinor)
    }

    @Test
    fun initialSettlementTimestampMayDifferFromTradeTimestamp() {
        val (service, farm, party) = setup()
        val trade = service.addTrade(farm.id, TradeDraft(TradeType.SALE, party.id, 1_000, occurredAt = "2026-08-16T08:00:00Z"))
        val current = service.loadFarm(farm.id)!!
        val marked = current.copy(settlements = mutableListOf(Settlement("opening", trade.id, 400, OffsetDateTime.parse("2026-08-16T08:00:00.123Z"), "", true)))
        service.importFarm(marked)
        assertEquals(600L, service.loadFarm(farm.id)!!.farmerOverview(now, zone).daily.creditSalesMinor)
    }

    @Test
    fun multipleSalesAndCurrentReceivableRemainSeparate() {
        val (service, farm, party) = setup()
        service.addTradeWithInitialSettlement(farm.id, TradeDraft(TradeType.SALE, party.id, 1_000, occurredAt = "2026-08-16T08:00:00Z"), 1_000)
        service.addTradeWithInitialSettlement(farm.id, TradeDraft(TradeType.SALE, party.id, 2_000, occurredAt = "2026-08-16T08:05:00Z"), 500)
        val overview = service.loadFarm(farm.id)!!.farmerOverview(now, zone).daily
        assertEquals(1_500L, overview.creditSalesMinor)
        assertEquals(1_500L, overview.currentReceivableMinor)
    }
}
