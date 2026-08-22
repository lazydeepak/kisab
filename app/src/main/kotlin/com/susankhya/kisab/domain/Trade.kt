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
 * expense. Receiving or paying the money is modeled separately as [Settlement]
 * records (a first-class payment history), so partially paid sales never appear
 * as full cash income on the Home screen.
 */
enum class TradeType {
    SALE,
    PURCHASE
}

/**
 * A trade between the farm and a party, carrying only what was *owed*. The
 * identity ([id]) is stable and is the anchor for [Settlement] records
 * (receipts/payments against a single trade).
 *
 * Monetary facts are stored only as exact minor-unit totals ([totalMinor]);
 * payment status and outstanding amount are always derived from the trade's
 * [Settlement] records ([Settlement.paidMinorFor], [Settlement.outstandingMinorFor],
 * [Settlement.paymentSummaryFor]) — never persisted on the trade itself.
 * Currency is owned by [FarmState.currencyCode], never by the trade.
 *
 * The party is optional only when the trade is fully settled (a cash
 * transaction); a trade with money still receivable/payable must name the
 * party who owes or is owed.
 *
 * Invariants (enforced by [FarmStateValidator.validateTrade]):
 *  - [totalMinor] > 0
 *  - the sum of the trade's settlements never exceeds [totalMinor]
 *  - outstanding > 0 requires a [partyId]
 *  - a linked party must exist and its role must be compatible with [type]
 *
 * [activity] is the optional M10/M11 farm-activity association (`null` =
 * general/farm-wide). Activity ownership lives on the trade — the single
 * financial authority for a trade flow — and is never duplicated: settlements
 * and trade projections derive it from the trade (mirroring how settlements
 * reach the party through the trade rather than repeating [partyId]).
 */
data class Trade(
    val id: String,
    val type: TradeType,
    val partyId: String?,
    val totalMinor: Long,
    val description: String,
    val occurredAt: OffsetDateTime,
    val activity: FarmActivityType? = null
)

/**
 * Input for creating or updating a [Trade]. Mirrors [FarmTransactionDraft]:
 * the date/time is carried as an ISO-8601 offset string and normalized to UTC
 * on construction. Payment on a new trade is expressed through a separate
 * initial [Settlement] (see [FarmSliceService.addTrade]).
 */
data class TradeDraft(
    val type: TradeType,
    val partyId: String?,
    val totalMinor: Long,
    val description: String = "",
    val occurredAt: String,
    /** Optional activity association; `null` is a general/farm-wide trade. */
    val activity: FarmActivityType? = null
) {
    fun toTrade(id: String): Trade = try {
        Trade(
            id = id,
            type = type,
            partyId = partyId?.takeIf { it.isNotBlank() },
            totalMinor = totalMinor,
            description = description.trim(),
            occurredAt = OffsetDateTime.parse(occurredAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .withOffsetSameInstant(ZoneOffset.UTC),
            activity = activity
        )
    } catch (exception: RuntimeException) {
        throw IllegalArgumentException("Trade date/time must be a valid ISO-8601 value", exception)
    }
}

/**
 * Payment status derived from monetary facts, never persisted independently.
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

/**
 * A read-only projection of a trade's settlement state, derived from its
 * [Settlement] records. Never persisted independently.
 */
data class TradePaymentSummary(
    val paidMinor: Long,
    val outstandingMinor: Long,
    val status: PaymentStatus
)

/** True when no counterparty has been named and no money remains to settle. */
fun Trade.isCashTrade(settlements: List<Settlement>): Boolean =
    partyId.isNullOrBlank() && settlements.outstandingMinorFor(this) == 0L