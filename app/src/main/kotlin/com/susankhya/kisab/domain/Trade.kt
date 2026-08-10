package com.susankhya.kisab.domain

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * The two sides of a Hisab-Kitab trade. A SALE is a business event in which the
 * farm sells its produce (money the farm is owed); a PURCHASE is a business
 * event in which the farm buys inputs (money the farm owes).
 *
 * A trade is deliberately NOT a Home cashflow event: recording a SALE does not
 * add income to the farm balance, and recording a PURCHASE does not add
 * expense. Receiving or paying the money is a separate event that M5-03 will
 * model as settlements. This boundary keeps partially paid sales from
 * appearing as full cash income on the Home screen.
 */
enum class TradeType {
    SALE,
    PURCHASE
}

/**
 * A trade between the farm and a party. The identity ([id]) is stable and is
 * the future anchor for M5-03 settlement records (receipts/payments against a
 * single trade).
 *
 * Monetary facts are stored only as exact minor-unit totals ([totalMinor] and
 * [paidMinor]); the payment status and outstanding amount are always derived
 * from those two numbers. Currency is owned by [FarmState.currencyCode], never
 * by the trade.
 *
 * The party is optional only when the trade is fully paid (a cash transaction);
 * a trade with money still receivable/payable must name the party who owes or
 * is owed.
 *
 * Invariants (enforced by [FarmStateValidator.validateTrade]):
 *  - [totalMinor] > 0
 *  - 0 <= [paidMinor] <= [totalMinor]
 *  - [paidMinor] < [totalMinor] requires a [partyId]
 *  - a linked party must exist and its role must be compatible with [type]
 */
data class Trade(
    val id: String,
    val type: TradeType,
    val partyId: String?,
    val totalMinor: Long,
    val paidMinor: Long,
    val description: String,
    val occurredAt: OffsetDateTime
)

/**
 * Input for creating or updating a [Trade]. Mirrors [FarmTransactionDraft]:
 * the date/time is carried as an ISO-8601 offset string and normalized to UTC
 * on construction.
 */
data class TradeDraft(
    val type: TradeType,
    val partyId: String?,
    val totalMinor: Long,
    val paidMinor: Long,
    val description: String = "",
    val occurredAt: String
) {
    fun toTrade(id: String): Trade = try {
        Trade(
            id = id,
            type = type,
            partyId = partyId?.takeIf { it.isNotBlank() },
            totalMinor = totalMinor,
            paidMinor = paidMinor,
            description = description.trim(),
            occurredAt = OffsetDateTime.parse(occurredAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .withOffsetSameInstant(ZoneOffset.UTC)
        )
    } catch (exception: RuntimeException) {
        throw IllegalArgumentException("Trade date/time must be a valid ISO-8601 value", exception)
    }
}

/**
 * Payment status derived from the monetary facts, never persisted independently.
 */
enum class PaymentStatus {
    UNPAID,
    PARTIAL,
    PAID
}

/** [PARTIAL] when 0 < paid < total, [PAID] when fully settled, otherwise [UNPAID]. */
fun paymentStatusOf(totalMinor: Long, paidMinor: Long): PaymentStatus = when {
    paidMinor <= 0 -> PaymentStatus.UNPAID
    paidMinor >= totalMinor -> PaymentStatus.PAID
    else -> PaymentStatus.PARTIAL
}

fun Trade.paymentStatus(): PaymentStatus = paymentStatusOf(totalMinor, paidMinor)

/** Money still receivable (SALE) or payable (PURCHASE). Zero when fully paid. */
fun Trade.outstandingMinor(): Long = totalMinor - paidMinor

/** True when no counterparty has been named and no money remains to settle. */
fun Trade.isCashTrade(): Boolean = partyId.isNullOrBlank() && outstandingMinor() == 0L