package com.susankhya.kisab.domain

/** Recent sale context derived from existing Trade and ProductSaleDetail facts. */
object ProductSaleHistory {
    fun recentCustomerIds(farm: FarmState): List<String> =
        farm.trades
            .asSequence()
            .filter { it.type == TradeType.SALE && it.partyId != null }
            .sortedWith(compareByDescending<Trade> { it.occurredAt }.thenByDescending { it.id })
            .mapNotNull { it.partyId }
            .distinct()
            .toList()

    fun recentProductIds(farm: FarmState): List<String> =
        saleDetailsNewestFirst(farm)
            .map { it.detail.productId }
            .distinct()

    fun latestRateForCustomerAndProduct(farm: FarmState, partyId: String, productId: String): Long? =
        saleDetailsNewestFirst(farm)
            .firstOrNull { it.trade.partyId == partyId && it.detail.productId == productId }
            ?.detail
            ?.rateMinor

    fun latestRateForProduct(farm: FarmState, productId: String): Long? =
        saleDetailsNewestFirst(farm)
            .firstOrNull { it.detail.productId == productId }
            ?.detail
            ?.rateMinor

    private fun saleDetailsNewestFirst(farm: FarmState): List<SaleDetailWithTrade> {
        val tradesById = farm.trades
            .filter { it.type == TradeType.SALE }
            .associateBy { it.id }
        return farm.productSaleDetails
            .mapNotNull { detail -> tradesById[detail.tradeId]?.let { SaleDetailWithTrade(it, detail) } }
            .sortedWith(
                compareByDescending<SaleDetailWithTrade> { it.trade.occurredAt }
                    .thenByDescending { it.trade.id }
            )
    }

    private data class SaleDetailWithTrade(
        val trade: Trade,
        val detail: ProductSaleDetail
    )
}
