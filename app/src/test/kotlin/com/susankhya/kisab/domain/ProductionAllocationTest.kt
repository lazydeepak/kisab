package com.susankhya.kisab.domain

import com.susankhya.kisab.persistence.FarmPersistenceCodec
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionAllocationTest {
    private val zone = ZoneId.of("Asia/Kathmandu")
    private val day = java.time.LocalDate.of(2026, 8, 16)

    @Test
    fun milkProductionSalesAndAllocationsLeaveOneLitreUnexplained() {
        val service = FarmSliceService()
        val farm = service.createFarm("Farm")
        val milk = service.addProduct(farm.id, "Milk", ProductUnit.LITRE)
        service.addProductionRecord(farm.id, ProductionRecordDraft(milk.id, BigDecimal("69"), ProductUnit.LITRE, "2026-08-16T02:00:00Z"), zone)
        val customer = service.addParty(farm.id, PartyDraft("Ram", PartyRole.CUSTOMER))
        service.addProductSale(farm.id, customer.id, milk.id, BigDecimal("57"), 90, null, "2026-08-16T08:00:00Z")
        service.addProductionAllocation(farm.id, ProductionAllocationDraft(milk.id, BigDecimal("2"), ProductUnit.LITRE, "2026-08-16T10:00:00Z", ProductionAllocationType.HOME_USE), zone)
        service.addProductionAllocation(farm.id, ProductionAllocationDraft(milk.id, BigDecimal("6"), ProductUnit.LITRE, "2026-08-16T11:00:00Z", ProductionAllocationType.PROCESSING), zone)
        service.addProductionAllocation(farm.id, ProductionAllocationDraft(milk.id, BigDecimal("3"), ProductUnit.LITRE, "2026-08-16T12:00:00Z", ProductionAllocationType.ANIMAL_FEED), zone)

        val result = service.productionReconciliation(farm.id, milk.id, day, zone)
        assertEquals(BigDecimal("69"), result.produced)
        assertEquals(BigDecimal("57"), result.sold)
        assertEquals(BigDecimal("1"), result.unexplained)
    }

    @Test
    fun overAllocationIsRejectedAndNegativeSalesRemainVisible() {
        val service = FarmSliceService()
        val farm = service.createFarm("Farm")
        val milk = service.addProduct(farm.id, "Milk", ProductUnit.LITRE)
        service.addProductionRecord(farm.id, ProductionRecordDraft(milk.id, BigDecimal("5"), ProductUnit.LITRE, "2026-08-16T02:00:00Z"), zone)
        val before = FarmPersistenceCodec.encode(service.loadFarm(farm.id)!!)
        val rejected = runCatching {
            service.addProductionAllocation(farm.id, ProductionAllocationDraft(milk.id, BigDecimal("6"), ProductUnit.LITRE, "2026-08-16T10:00:00Z", ProductionAllocationType.HOME_USE), zone)
        }
        assertTrue(rejected.isFailure)
        assertEquals(before, FarmPersistenceCodec.encode(service.loadFarm(farm.id)!!))
        val customer = service.addParty(farm.id, PartyDraft("Ram", PartyRole.CUSTOMER))
        service.addProductSale(farm.id, customer.id, milk.id, BigDecimal("7"), 90, null, "2026-08-16T08:00:00Z")
        assertTrue(service.productionReconciliation(farm.id, milk.id, day, zone).isInconsistent)
    }

    @Test
    fun genericEggsAndUnitMismatchAreNotFalselyReconciled() {
        val service = FarmSliceService()
        val farm = service.createFarm("Farm")
        val eggs = service.addProduct(farm.id, "Eggs", ProductUnit.PIECE)
        service.addProductionRecord(farm.id, ProductionRecordDraft(eggs.id, BigDecimal("620"), ProductUnit.PIECE, "2026-08-16T04:00:00Z"), zone)
        service.addProductionAllocation(farm.id, ProductionAllocationDraft(eggs.id, BigDecimal("20"), ProductUnit.PIECE, "2026-08-16T05:00:00Z", ProductionAllocationType.HOME_USE), zone)
        assertEquals(BigDecimal("600"), service.productionReconciliation(farm.id, eggs.id, day, zone).unexplained)
        val mismatch = ProductionRecord("mismatch", eggs.id, BigDecimal("1"), ProductUnit.KILOGRAM, OffsetDateTime.parse("2026-08-16T06:00:00Z"))
        val raw = service.loadFarm(farm.id)!!.copy(productionRecords = mutableListOf(mismatch))
        val result = raw.productionReconciliation(eggs.id, day, zone)
        assertTrue(result.unitMismatch)
    }

    @Test
    fun editDeleteBackupResetAndFinancialIsolationWork() {
        val service = FarmSliceService()
        val farm = service.createFarm("Farm")
        val tomato = service.addProduct(farm.id, "Tomato", ProductUnit.KILOGRAM)
        service.addProductionRecord(farm.id, ProductionRecordDraft(tomato.id, BigDecimal("10"), ProductUnit.KILOGRAM, "2026-08-16T06:00:00Z"), zone)
        val allocation = service.addProductionAllocation(farm.id, ProductionAllocationDraft(tomato.id, BigDecimal("2"), ProductUnit.KILOGRAM, "2026-08-16T07:00:00Z", ProductionAllocationType.WASTE), zone)
        service.updateProductionAllocation(farm.id, allocation.id, ProductionAllocationDraft(tomato.id, BigDecimal("3"), ProductUnit.KILOGRAM, "2026-08-16T07:00:00Z", ProductionAllocationType.WASTE), zone)
        val decoded = FarmPersistenceCodec.decode(FarmPersistenceCodec.encode(service.loadFarm(farm.id)!!))
        assertEquals(BigDecimal("3"), decoded.productionAllocations.single().quantity)
        service.deleteProductionAllocation(farm.id, allocation.id)
        service.resetFarmData(farm.id)
        assertTrue(service.loadFarm(farm.id)!!.productionAllocations.isEmpty())
        assertTrue(service.products(farm.id).size == 1)
    }
}
