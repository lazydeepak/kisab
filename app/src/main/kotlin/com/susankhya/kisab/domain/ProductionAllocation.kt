package com.susankhya.kisab.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

enum class ProductionAllocationType { HOME_USE, PROCESSING, ANIMAL_FEED, WASTE, OTHER }

data class ProductionAllocation(
    val id: String,
    val productId: String,
    val quantity: BigDecimal,
    val unit: ProductUnit,
    val occurredAt: OffsetDateTime,
    val type: ProductionAllocationType,
    val note: String = ""
) {
    init {
        require(id.isNotBlank()) { "Allocation id is required" }
        require(productId.isNotBlank()) { "Allocation product id is required" }
        require(quantity > BigDecimal.ZERO) { "Allocation quantity must be positive" }
        require(quantity.scale() <= MAX_QUANTITY_SCALE) { "Allocation quantity has too many decimal places" }
    }
    companion object { const val MAX_QUANTITY_SCALE = 3 }
}

data class ProductionAllocationDraft(
    val productId: String,
    val quantity: BigDecimal,
    val unit: ProductUnit,
    val occurredAt: String,
    val type: ProductionAllocationType,
    val note: String = ""
) {
    fun toAllocation(id: String): ProductionAllocation = try {
        ProductionAllocation(id, productId, quantity, unit,
            OffsetDateTime.parse(occurredAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME).withOffsetSameInstant(ZoneOffset.UTC), type, note.trim())
    } catch (exception: RuntimeException) {
        throw IllegalArgumentException("Allocation date/time must be valid", exception)
    }
}

data class ProductionReconciliation(
    val productId: String,
    val date: LocalDate,
    val unit: ProductUnit,
    val produced: BigDecimal,
    val sold: BigDecimal,
    val allocations: Map<ProductionAllocationType, BigDecimal>,
    val unexplained: BigDecimal,
    val unitMismatch: Boolean = false
) {
    val isInconsistent: Boolean get() = unexplained < BigDecimal.ZERO
}

fun FarmState.productionReconciliation(productId: String, date: LocalDate, zone: ZoneId): ProductionReconciliation {
    val product = products.firstOrNull { it.id == productId } ?: throw IllegalArgumentException("Product not found: $productId")
    val production = productionForDay(date, zone).filter { it.productId == productId }
    val produced = production.fold(BigDecimal.ZERO) { total, record -> total.add(record.quantity) }
    val details = productSaleDetails.filter { it.productId == productId }
    val tradeById = trades.associateBy { it.id }
    val soldDetails = details.filter { detail ->
        val trade = tradeById[detail.tradeId]
        trade?.type == TradeType.SALE && trade.occurredAt.atZoneSameInstant(zone).toLocalDate() == date
    }
    val unitMismatch = production.any { it.unit != product.defaultUnit } ||
        soldDetails.any { it.detailUnit() != product.defaultUnit }
    val sold = if (unitMismatch) BigDecimal.ZERO else soldDetails.fold(BigDecimal.ZERO) { total, detail -> total.add(detail.quantity) }
    val allocations = productionAllocations.filter {
        it.productId == productId && it.occurredAt.atZoneSameInstant(zone).toLocalDate() == date && it.unit == product.defaultUnit
    }.groupBy { it.type }.mapValues { (_, values) -> values.fold(BigDecimal.ZERO) { total, value -> total.add(value.quantity) } }
    val allocated = allocations.values.fold(BigDecimal.ZERO) { total, value -> total.add(value) }
    return ProductionReconciliation(productId, date, product.defaultUnit, produced, sold, allocations, produced.subtract(sold).subtract(allocated), unitMismatch)
}

private fun ProductSaleDetail.detailUnit(): ProductUnit = unit
