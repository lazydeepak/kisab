package com.susankhya.kisab.domain

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * A payment (SALE) or receipt-of-goods payment the other way (PURCHASE) — an
 * actual money movement applied against a single [Trade]. The *direction* of
 * the money is not stored here; it is derived through the trade's [Trade.type]:
 * settling a SALE means money *received from* the customer, settling a PURCHASE
 * means money *paid to* the supplier.
 *
 * Settlements are the source of truth for payment history. They never carry a
 * currency — that is owned by [FarmState.currencyCode] — nor a repeated
 * [Trade.partyId] (the relationship reaches the party through the trade, so a
 * later party correction on the trade stays consistent with history).
 *
 * Invariants (enforced by [FarmStateValidator.validateSettlement] and the
 * resulting-state validation):
 *  - [amountMinor] > 0
 *  - [tradeId] references an existing trade
 *  - the SUM of a trade's settlements never exceeds the trade's [Trade.totalMinor]
 *  - the resulting farm state keeps the outstanding-party rule satisfied
 */
data class Settlement(
    val id: String,
    val tradeId: String,
    val amountMinor: Long,
    val occurredAt: OffsetDateTime,
    val note: String
)

/**
 * Input for creating or updating a [Settlement]. Mirrors [TradeDraft]: the
 * date/time is carried as an ISO-8601 offset string and normalized to UTC on
 * construction.
 */
data class SettlementDraft(
    val tradeId: String,
    val amountMinor: Long,
    val occurredAt: String,
    val note: String = ""
) {
    fun toSettlement(id: String): Settlement = try {
        Settlement(
            id = id,
            tradeId = tradeId,
            amountMinor = amountMinor,
            occurredAt = OffsetDateTime.parse(occurredAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .withOffsetSameInstant(ZoneOffset.UTC),
            note = note.trim()
        )
    } catch (exception: RuntimeException) {
        throw IllegalArgumentException("Settlement date/time must be a valid ISO-8601 value", exception)
    }
}

/**
 * The settlement records for one trade, newest first (occurredAt descending,
 * then insertion order), matching the presentation order used for trades.
 */
fun List<Settlement>.settlementsForTrade(tradeId: String): List<Settlement> =
    filter { it.tradeId == tradeId }
        .withIndex()
        .sortedWith(
            compareByDescending<IndexedValue<Settlement>> { it.value.occurredAt }
                .thenByDescending { it.index }
        )
        .map { it.value }

/** The total amount settled against one trade. Zero when no settlement exists. */
fun List<Settlement>.paidMinorFor(tradeId: String): Long =
    filter { it.tradeId == tradeId }.fold(0L) { acc, settlement -> Math.addExact(acc, settlement.amountMinor) }

/** Money still receivable (SALE) or payable (PURCHASE). Zero when fully settled. */
fun List<Settlement>.outstandingMinorFor(trade: Trade): Long = trade.totalMinor - paidMinorFor(trade.id)

fun List<Settlement>.paymentStatusFor(trade: Trade): PaymentStatus =
    paymentStatusOf(trade.totalMinor, paidMinorFor(trade.id))

fun List<Settlement>.paymentSummaryFor(trade: Trade): TradePaymentSummary {
    val paid = paidMinorFor(trade.id)
    return TradePaymentSummary(
        paidMinor = paid,
        outstandingMinor = trade.totalMinor - paid,
        status = paymentStatusOf(trade.totalMinor, paid)
    )
}