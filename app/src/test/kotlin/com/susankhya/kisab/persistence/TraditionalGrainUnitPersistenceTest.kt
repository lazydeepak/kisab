package com.susankhya.kisab.persistence

import com.susankhya.kisab.domain.FarmSliceService
import com.susankhya.kisab.domain.ProductUnit
import com.susankhya.kisab.domain.ProductionRecordDraft
import com.susankhya.kisab.domain.ProductionSession
import com.susankhya.kisab.domain.SupplyUsageDraft
import com.susankhya.kisab.domain.TransactionCategory
import java.math.BigDecimal
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Milestone M9: the Nepali traditional grain units (mana / pathi / muri) are
 * first-class governed ProductUnit values. They must persist through the
 * schema-12 codec and the v1 backup envelope exactly like the existing units,
 * and must remain valid through production/supply records that use them.
 */
class TraditionalGrainUnitPersistenceTest {

    @Test
    fun grainUnitProductsAndSuppliesRoundTripThroughSchema12() {
        val service = FarmSliceService()
        val farm = service.createFarm("Grain Farm")
        val paddy = service.addProduct(farm.id, "Paddy", ProductUnit.MANA)
        val wheat = service.addProduct(farm.id, "Wheat", ProductUnit.MURI)
        val seed = service.addSupply(farm.id, "Seed", ProductUnit.PATHI)

        service.addSupplyPurchase(
            farm.id, seed.id, BigDecimal("20"), ProductUnit.PATHI, 40_000,
            TransactionCategory.SUPPLIES, "2026-08-16T08:00:00Z", "Seed purchase"
        )
        service.addSupplyUsage(
            farm.id, SupplyUsageDraft(seed.id, BigDecimal("5"), ProductUnit.PATHI, "2026-08-16T18:00:00Z")
        )
        service.addProductionRecord(
            farm.id,
            ProductionRecordDraft(
                productId = paddy.id,
                quantity = BigDecimal("12"),
                unit = ProductUnit.MANA,
                session = ProductionSession.MORNING,
                occurredAt = "2026-08-16T06:00:00Z"
            ),
            ZoneId.of("Asia/Kathmandu")
        )

        val decoded = FarmPersistenceCodec.decode(FarmPersistenceCodec.encode(service.loadFarm(farm.id)!!))

        assertEquals(ProductUnit.MANA, decoded.products.first { it.id == paddy.id }.defaultUnit)
        assertEquals(ProductUnit.MURI, decoded.products.first { it.id == wheat.id }.defaultUnit)
        assertEquals(ProductUnit.PATHI, decoded.supplies.single().unit)
        assertEquals(ProductUnit.PATHI, decoded.supplyPurchaseDetails.single().unit)
        assertEquals(ProductUnit.PATHI, decoded.supplyUsages.single().unit)
        assertEquals(ProductUnit.MANA, decoded.productionRecords.single().unit)
        assertEquals(BigDecimal("15"), service.supplyAvailable(farm.id, seed.id))
    }

    @Test
    fun grainUnitFarmSurvivesBackupEnvelopeRoundTrip() {
        val service = FarmSliceService()
        val farm = service.createFarm("Backup Grain Farm")
        service.addProduct(farm.id, "Mustard", ProductUnit.PATHI)
        service.addSupply(farm.id, "Fertilizer", ProductUnit.MANA)

        val envelope = FarmBackupCodec.decode(FarmBackupCodec.encode(service.loadFarm(farm.id)!!))
        assertEquals("Backup Grain Farm", envelope.farm.name)
        assertEquals(ProductUnit.PATHI, envelope.farm.products.single().defaultUnit)
        assertEquals(ProductUnit.MANA, envelope.farm.supplies.single().unit)
    }
}