package com.susankhya.kisab.domain

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class FarmSupply(
    val id: String,
    val name: String,
    val unit: ProductUnit,
    val customUnitLabel: String = ""
) {
    init {
        require(id.isNotBlank()) { "Supply id is required" }
        require(name.isNotBlank()) { "Supply name is required" }
        if (unit == ProductUnit.CUSTOM) require(customUnitLabel.isNotBlank()) { "Custom unit label is required" }
    }
}

data class SupplyPurchaseDetail(
    val transactionId: String?,
    val supplyId: String,
    val quantity: BigDecimal,
    val unit: ProductUnit,
    val customUnitLabel: String = "",
    val purchaseTradeId: String? = null
) {
    init {
        require(transactionId?.isNotBlank() == true || purchaseTradeId?.isNotBlank() == true) { "Supply purchase source is required" }
        require(transactionId == null || purchaseTradeId == null) { "Supply purchase cannot have two sources" }
        require(supplyId.isNotBlank()) { "Supply purchase supply id is required" }
        require(quantity > BigDecimal.ZERO) { "Purchased quantity must be positive" }
        require(quantity.scale() <= MAX_QUANTITY_SCALE) { "Purchased quantity has too many decimal places" }
        if (unit == ProductUnit.CUSTOM) require(customUnitLabel.isNotBlank()) { "Custom unit label is required" }
    }

    companion object { const val MAX_QUANTITY_SCALE = 3 }
}

data class SupplyUsage(
    val id: String,
    val supplyId: String,
    val quantity: BigDecimal,
    val unit: ProductUnit,
    val occurredAt: OffsetDateTime,
    val note: String = ""
) {
    init {
        require(id.isNotBlank()) { "Supply usage id is required" }
        require(supplyId.isNotBlank()) { "Supply usage supply id is required" }
        require(quantity > BigDecimal.ZERO) { "Used quantity must be positive" }
        require(quantity.scale() <= SupplyPurchaseDetail.MAX_QUANTITY_SCALE) { "Used quantity has too many decimal places" }
    }
}

data class SupplyUsageDraft(
    val supplyId: String,
    val quantity: BigDecimal,
    val unit: ProductUnit,
    val occurredAt: String,
    val note: String = ""
) {
    fun toUsage(id: String): SupplyUsage = try {
        SupplyUsage(
            id = id,
            supplyId = supplyId,
            quantity = quantity,
            unit = unit,
            occurredAt = OffsetDateTime.parse(occurredAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .withOffsetSameInstant(ZoneOffset.UTC),
            note = note.trim()
        )
    } catch (exception: RuntimeException) {
        throw IllegalArgumentException("Supply usage date/time must be valid", exception)
    }
}

fun FarmState.supplyQuantityPurchased(supplyId: String): BigDecimal =
    supplyPurchaseDetails.filter { it.supplyId == supplyId }
        .fold(BigDecimal.ZERO) { total, detail -> total.add(detail.quantity) }

fun FarmState.supplyQuantityUsed(supplyId: String): BigDecimal =
    supplyUsages.filter { it.supplyId == supplyId }
        .fold(BigDecimal.ZERO) { total, usage -> total.add(usage.quantity) }

fun FarmState.supplyQuantityAvailable(supplyId: String): BigDecimal =
    supplyQuantityPurchased(supplyId).subtract(supplyQuantityUsed(supplyId))