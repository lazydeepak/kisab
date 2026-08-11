package com.susankhya.kisab.domain

import java.time.OffsetDateTime
import java.time.ZoneId

/** Period activity for one Party, derived from that Party's Trades and Settlements. */
data class PartyHisabActivity(
    val salesMinor: Long,
    val purchasesMinor: Long,
    val paymentsReceivedMinor: Long,
    val paymentsMadeMinor: Long
)

/** Outstanding position for one Party immediately before the period cutoff. */
data class PartyHisabPosition(
    val toReceiveMinor: Long,
    val toPayMinor: Long,
    val netMinor: Long
)

/**
 * M6 Farmer Hisab calculator result.
 *
 * This is a non-persisted reconciliation over Party -> Trade -> Settlement.
 * Activity is start-inclusive/end-exclusive for [period]; position is computed
 * from all of the Party's obligations and payments strictly before
 * [FinancialPeriod.endExclusive], independent of the period start.
 */
data class PartyHisabResult(
    val party: Party,
    val period: FinancialPeriod,
    val activity: PartyHisabActivity,
    val position: PartyHisabPosition
)

/** Pure per-party/per-period Hisab lookup; no calculated value is persisted. */
fun FarmState.partyHisab(
    partyId: String,
    preset: FinancialPeriodPreset,
    now: OffsetDateTime,
    zone: ZoneId
): PartyHisabResult {
    val party = parties.firstOrNull { it.id == partyId }
        ?: throw IllegalArgumentException("Party not found: $partyId")
    val period = FarmFinancialPeriods.periodFor(preset, now, zone)

    val farmTradeIds = trades.mapTo(mutableSetOf()) { it.id }
    settlements.forEach { settlement ->
        require(settlement.tradeId in farmTradeIds) {
            "Settlement ${settlement.id} references missing trade ${settlement.tradeId}"
        }
    }

    val partyTrades = trades.filter { it.partyId == party.id }
    val partyTradeById = partyTrades.associateBy { it.id }
    val partySettlements = settlements.filter { it.tradeId in partyTradeById }

    var sales = 0L
    var purchases = 0L
    partyTrades.forEach { trade ->
        if (trade.occurredAt.inHisabPeriod(period)) {
            if (trade.type == TradeType.SALE) {
                sales = Math.addExact(sales, trade.totalMinor)
            } else {
                purchases = Math.addExact(purchases, trade.totalMinor)
            }
        }
    }

    var received = 0L
    var made = 0L
    partySettlements.forEach { settlement ->
        if (settlement.occurredAt.inHisabPeriod(period)) {
            val trade = partyTradeById.getValue(settlement.tradeId)
            if (trade.type == TradeType.SALE) {
                received = Math.addExact(received, settlement.amountMinor)
            } else {
                made = Math.addExact(made, settlement.amountMinor)
            }
        }
    }

    var toReceive = 0L
    var toPay = 0L
    partyTrades.forEach { trade ->
        if (trade.occurredAt.isBefore(period.endExclusive)) {
            var settled = 0L
            partySettlements.forEach { settlement ->
                if (settlement.tradeId == trade.id && settlement.occurredAt.isBefore(period.endExclusive)) {
                    settled = Math.addExact(settled, settlement.amountMinor)
                }
            }
            val outstanding = Math.subtractExact(trade.totalMinor, settled)
            if (outstanding > 0L) {
                if (trade.type == TradeType.SALE) {
                    toReceive = Math.addExact(toReceive, outstanding)
                } else {
                    toPay = Math.addExact(toPay, outstanding)
                }
            }
        }
    }

    return PartyHisabResult(
        party = party,
        period = period,
        activity = PartyHisabActivity(
            salesMinor = sales,
            purchasesMinor = purchases,
            paymentsReceivedMinor = received,
            paymentsMadeMinor = made
        ),
        position = PartyHisabPosition(
            toReceiveMinor = toReceive,
            toPayMinor = toPay,
            netMinor = Math.subtractExact(toReceive, toPay)
        )
    )
}

private fun OffsetDateTime.inHisabPeriod(period: FinancialPeriod): Boolean =
    !isBefore(period.startInclusive) && isBefore(period.endExclusive)
