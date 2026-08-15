package com.susankhya.kisab.domain

import java.math.BigDecimal
import java.math.RoundingMode

enum class ProductUnit {
    LITRE,
    KILOGRAM,
    PIECE,
    BAG,
    PACKET,
    BOTTLE,
    CUSTOM
}

data class FarmProduct(
    val id: String,
    val name: String,
    val defaultUnit: ProductUnit,
    val customUnitLabel: String = ""
) {
    init {
        require(id.isNotBlank()) { "Product id is required" }
        require(name.isNotBlank()) { "Product name is required" }
        if (defaultUnit == ProductUnit.CUSTOM) {
            require(customUnitLabel.isNotBlank()) { "Custom unit label is required" }
        }
    }

    fun unitLabel(): String = if (defaultUnit == ProductUnit.CUSTOM) customUnitLabel else defaultUnit.name
}

data class ProductSaleDetail(
    val tradeId: String,
    val productId: String,
    val quantity: BigDecimal,
    val unit: ProductUnit,
    val customUnitLabel: String = "",
    val rateMinor: Long
) {
    init {
        require(tradeId.isNotBlank()) { "Sale detail trade id is required" }
        require(productId.isNotBlank()) { "Sale detail product id is required" }
        require(quantity > BigDecimal.ZERO) { "Sale quantity must be positive" }
        require(quantity.scale() <= MAX_QUANTITY_SCALE) { "Sale quantity has too many decimal places" }
        require(rateMinor > 0L) { "Sale rate must be positive" }
        if (unit == ProductUnit.CUSTOM) {
            require(customUnitLabel.isNotBlank()) { "Custom unit label is required" }
        }
    }

    fun normalizedQuantity(): BigDecimal = quantity.stripTrailingZeros()

    fun totalMinor(): Long = quantity
        .multiply(BigDecimal.valueOf(rateMinor))
        .setScale(0, RoundingMode.HALF_UP)
        .longValueExact()

    companion object {
        const val MAX_QUANTITY_SCALE = 3
    }
}