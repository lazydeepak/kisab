package com.susankhya.kisab.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

enum class ProductionSession {
    MORNING,
    EVENING,
    OTHER
}

data class ProductionRecord(
    val id: String,
    val productId: String,
    val quantity: BigDecimal,
    val unit: ProductUnit,
    val occurredAt: OffsetDateTime,
    val session: ProductionSession = ProductionSession.OTHER,
    val note: String = ""
) {
    init {
        require(id.isNotBlank()) { "Production id is required" }
        require(productId.isNotBlank()) { "Production product id is required" }
        require(quantity > BigDecimal.ZERO) { "Production quantity must be positive" }
        require(quantity.scale() <= MAX_QUANTITY_SCALE) { "Production quantity has too many decimal places" }
    }

    companion object { const val MAX_QUANTITY_SCALE = 3 }
}

data class ProductionRecordDraft(
    val productId: String,
    val quantity: BigDecimal,
    val unit: ProductUnit,
    val occurredAt: String,
    val session: ProductionSession = ProductionSession.OTHER,
    val note: String = ""
) {
    fun toRecord(id: String): ProductionRecord = try {
        ProductionRecord(
            id = id,
            productId = productId,
            quantity = quantity,
            unit = unit,
            occurredAt = OffsetDateTime.parse(occurredAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .withOffsetSameInstant(ZoneOffset.UTC),
            session = session,
            note = note.trim()
        )
    } catch (exception: RuntimeException) {
        throw IllegalArgumentException("Production date/time must be valid", exception)
    }
}

fun FarmState.productionForDay(date: LocalDate, zone: ZoneId): List<ProductionRecord> =
    productionRecords.filter { it.occurredAt.atZoneSameInstant(zone).toLocalDate() == date }
        .sortedWith(compareBy<ProductionRecord> { it.occurredAt }.thenBy { it.id })

fun List<ProductionRecord>.totalQuantityFor(productId: String): BigDecimal =
    filter { it.productId == productId }.fold(BigDecimal.ZERO) { total, record -> total.add(record.quantity) }
