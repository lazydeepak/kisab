package com.susankhya.kisab.domain

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductSaleHistoryTest {

    @Test
    fun recentCustomersAndProductsUseNewestSaleFirstWithDeterministicTies() {
        val farm = FarmState(
            id = "farm-1",
            name = "Farm",
            parties = mutableListOf(
                Party("party-a", "A", PartyRole.CUSTOMER),
                Party("party-b", "B", PartyRole.CUSTOMER)
            ),
            products = mutableListOf(
                FarmProduct("product-a", "A", ProductUnit.LITRE),
                FarmProduct("product-b", "B", ProductUnit.KILOGRAM)
            ),
            trades = mutableListOf(
                Trade("trade-a", TradeType.SALE, "party-a", 100, "A", java.time.OffsetDateTime.parse("2026-08-16T08:00:00Z")),
                Trade("trade-b", TradeType.SALE, "party-b", 100, "B", java.time.OffsetDateTime.parse("2026-08-16T09:00:00Z")),
                Trade("trade-c", TradeType.SALE, "party-a", 100, "A later", java.time.OffsetDateTime.parse("2026-08-16T09:00:00Z"))
            ),
            productSaleDetails = mutableListOf(
                ProductSaleDetail("trade-a", "product-a", BigDecimal("1"), ProductUnit.LITRE, rateMinor = 90),
                ProductSaleDetail("trade-b", "product-b", BigDecimal("1"), ProductUnit.KILOGRAM, rateMinor = 120),
                ProductSaleDetail("trade-c", "product-a", BigDecimal("1"), ProductUnit.LITRE, rateMinor = 95)
            )
        )

        assertEquals(listOf("party-a", "party-b"), ProductSaleHistory.recentCustomerIds(farm))
        assertEquals(listOf("product-a", "product-b"), ProductSaleHistory.recentProductIds(farm))
    }

    @Test
    fun customerProductRateWinsAndProductRateIsFallback() {
        val farm = FarmState(
            id = "farm-1",
            name = "Farm",
            parties = mutableListOf(
                Party("ram", "Ram", PartyRole.CUSTOMER),
                Party("sita", "Sita", PartyRole.CUSTOMER)
            ),
            products = mutableListOf(FarmProduct("milk", "Milk", ProductUnit.LITRE)),
            trades = mutableListOf(
                Trade("old", TradeType.SALE, "ram", 100, "Milk", java.time.OffsetDateTime.parse("2026-08-15T08:00:00Z")),
                Trade("new", TradeType.SALE, "sita", 100, "Milk", java.time.OffsetDateTime.parse("2026-08-16T08:00:00Z"))
            ),
            productSaleDetails = mutableListOf(
                ProductSaleDetail("old", "milk", BigDecimal("1"), ProductUnit.LITRE, rateMinor = 90),
                ProductSaleDetail("new", "milk", BigDecimal("1"), ProductUnit.LITRE, rateMinor = 100)
            )
        )

        assertEquals(90L, ProductSaleHistory.latestRateForCustomerAndProduct(farm, "ram", "milk"))
        assertEquals(100L, ProductSaleHistory.latestRateForProduct(farm, "milk"))
        assertNull(ProductSaleHistory.latestRateForCustomerAndProduct(farm, "ram", "missing"))
        assertNull(ProductSaleHistory.latestRateForCustomerAndProduct(farm, "missing", "milk"))
    }

    @Test
    fun legacyTradeWithoutProductDetailIsIgnored() {
        val farm = FarmState(
            id = "farm-1",
            name = "Farm",
            trades = mutableListOf(
                Trade("legacy", TradeType.SALE, "ram", 100, "Milk", java.time.OffsetDateTime.parse("2026-08-16T08:00:00Z"))
            ),
            products = mutableListOf(FarmProduct("milk", "Milk", ProductUnit.LITRE))
        )

        assertEquals(emptyList<String>(), ProductSaleHistory.recentProductIds(farm))
        assertNull(ProductSaleHistory.latestRateForProduct(farm, "milk"))
    }
}
