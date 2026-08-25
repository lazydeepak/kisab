package com.susankhya.kisab.domain

import com.susankhya.kisab.persistence.FarmPersistenceCodec
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionTest {
    private val zone = ZoneId.of("Asia/Kathmandu")

    @Test
    fun morningAndEveningMilkTotalWithoutFinancialMutation() {
        val service = FarmSliceService()
        val farm = service.createFarm("Farm")
        val milk = service.addProduct(farm.id, "Milk", ProductUnit.LITRE)
        val before = service.farmFinancialOverview(farm.id, FinancialPeriodPreset.ALL_TIME, OffsetDateTime.parse("2026-08-16T20:00:00Z"), zone)

        service.addProductionRecord(farm.id, ProductionRecordDraft(milk.id, BigDecimal("38"), ProductUnit.LITRE, "2026-08-16T02:15:00Z", ProductionSession.MORNING), zone)
        service.addProductionRecord(farm.id, ProductionRecordDraft(milk.id, BigDecimal("31"), ProductUnit.LITRE, "2026-08-16T12:15:00Z", ProductionSession.EVENING), zone)
        val records = service.productionForDay(farm.id, java.time.LocalDate.of(2026, 8, 16), zone)

        assertEquals(BigDecimal("69"), records.map { it.quantity }.fold(BigDecimal.ZERO, BigDecimal::add))
        val after = service.farmFinancialOverview(farm.id, FinancialPeriodPreset.ALL_TIME, OffsetDateTime.parse("2026-08-16T20:00:00Z"), zone)
        assertEquals(before.cashTotals, after.cashTotals)
        assertEquals(before.tradeTotals, after.tradeTotals)
        assertEquals(before.currentPosition, after.currentPosition)
    }

    @Test
    fun morningDuplicateUpdatesExistingRecordAndOtherAllowsMultiple() {
        val service = FarmSliceService()
        val farm = service.createFarm("Farm")
        val eggs = service.addProduct(farm.id, "Eggs", ProductUnit.PIECE)
        val first = service.addProductionRecord(farm.id, ProductionRecordDraft(eggs.id, BigDecimal("620"), ProductUnit.PIECE, "2026-08-16T02:00:00Z", ProductionSession.MORNING), zone)
        val replaced = service.addProductionRecord(farm.id, ProductionRecordDraft(eggs.id, BigDecimal("600"), ProductUnit.PIECE, "2026-08-16T03:00:00Z", ProductionSession.MORNING), zone)
        service.addProductionRecord(farm.id, ProductionRecordDraft(eggs.id, BigDecimal("20"), ProductUnit.PIECE, "2026-08-16T04:00:00Z", ProductionSession.OTHER), zone)

        assertEquals(first.id, replaced.id)
        assertEquals(2, service.loadFarm(farm.id)!!.productionRecords.size)
        assertEquals(BigDecimal("620"), service.productionForDay(farm.id, java.time.LocalDate.of(2026, 8, 16), zone).totalQuantityFor(eggs.id))
    }

    @Test
    fun editDeleteBackupAndResetKeepProductButClearProduction() {
        val service = FarmSliceService()
        val farm = service.createFarm("Farm")
        val tomato = service.addProduct(farm.id, "Tomato", ProductUnit.KILOGRAM)
        val record = service.addProductionRecord(farm.id, ProductionRecordDraft(tomato.id, BigDecimal("85"), ProductUnit.KILOGRAM, "2026-08-16T08:00:00Z", ProductionSession.OTHER), zone)
        service.updateProductionRecord(farm.id, record.id, ProductionRecordDraft(tomato.id, BigDecimal("90"), ProductUnit.KILOGRAM, "2026-08-16T08:00:00Z", ProductionSession.OTHER))
        val decoded = FarmPersistenceCodec.decode(FarmPersistenceCodec.encode(service.loadFarm(farm.id)!!))
        assertEquals(BigDecimal("90"), decoded.productionRecords.single().quantity)
        service.deleteProductionRecord(farm.id, record.id)
        service.addProductionRecord(farm.id, ProductionRecordDraft(tomato.id, BigDecimal("1"), ProductUnit.KILOGRAM, "2026-08-16T08:00:00Z"), zone)
        service.resetFarmData(farm.id)
        assertTrue(service.loadFarm(farm.id)!!.productionRecords.isEmpty())
        assertEquals(1, service.products(farm.id).size)
    }
}
