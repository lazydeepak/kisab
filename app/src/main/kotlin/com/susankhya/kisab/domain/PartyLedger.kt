package com.susankhya.kisab.domain

import java.time.OffsetDateTime

/**
 * M5-04 Party Khata / Ledger projection.
 *
 * A Party Khata is a **view over the authoritative persisted facts** — Party →
 * Trade → Settlement — never a second accounting authority. No ledger rows are
 * stored on [FarmState]; the projection is rebuilt on every call, so editing or
 * deleting an underlying Trade or Settlement automatically changes the Khata on
 * the next render.
 *
 * ## Sign convention (single, app-wide)
 *
 * A positive balance means **the Party owes the farmer** (receivable); a
 * negative balance means **the farmer owes the Party** (payable); zero means a
 * net settled position. Therefore:
 *
 * - SALE trade → `+totalMinor` (the Party owes for what they bought)
 * - SALE settlement (PAYMENT_RECEIVED) → `-amountMinor` (money received reduces what is owed)
 * - PURCHASE trade → `-totalMinor` (the farmer owes for what was bought)
 * - PURCHASE settlement (PAYMENT_MADE) → `+amountMinor` (money paid reduces what the farmer owes)
 *
 * Running balance: `running[n] = running[n-1] + entry[n].deltaMinor`, starting
 * from zero.
 */
enum class PartyLedgerEntryType {
    SALE,
    PURCHASE,
    PAYMENT_RECEIVED,
    PAYMENT_MADE;

    /**
     * Same-instant ordering tie-break: the obligation (Trade) always appears
     * before its Settlement so the running balance never applies a payment
     * before the obligation exists (critical for M5-03's migrated opening
     * settlements, where the settlement shares the trade's timestamp).
     */
    internal fun orderPriority(): Int = when (this) {
        SALE, PURCHASE -> 0
        PAYMENT_RECEIVED, PAYMENT_MADE -> 1
    }
}

/**
 * One projected ledger row. [sourceId] references the underlying Trade (for
 * SALE/PURCHASE) or Settlement (for PAYMENT_RECEIVED/PAYMENT_MADE) — it is
 * never persisted. [tradeId] anchors a settlement row back to its trade so the
 * UI can route open-with-payment to the correct trade. [amountMinor] is the
 * unsigned magnitude; [deltaMinor] carries the sign convention above;
 * [runningBalanceMinor] is the post-event balance.
 */
data class PartyLedgerEntry(
    val sourceId: String,
    val sourceType: PartyLedgerEntryType,
    val occurredAt: OffsetDateTime,
    val amountMinor: Long,
    val deltaMinor: Long,
    val runningBalanceMinor: Long,
    val description: String,
    val tradeId: String
)

/**
 * The Party's current position, derived from outstanding trade balances.
 * [toReceiveMinor] is the sum outstanding across SALE trades, [toPayMinor]
 * across PURCHASE trades, and [netMinor] = toReceive − toPay. The net is
 * **informational only** — it never marks any individual trade settled.
 */
data class PartyLedgerSummary(
    val toReceiveMinor: Long,
    val toPayMinor: Long,
    val netMinor: Long
)

/**
 * The projection consumed by the Party Khata screen: the party, its current
 * position, and [entries] in deterministic chronological order (oldest → newest;
 * the UI may reverse purely for rendering — running balances are always
 * computed oldest → newest).
 */
data class PartyLedger(
    val party: Party,
    val summary: PartyLedgerSummary,
    val entries: List<PartyLedgerEntry>
)

/** Projects the Khata for one party; throws when the party does not exist. */
fun FarmState.partyLedger(partyId: String): PartyLedger {
    val party = parties.firstOrNull { it.id == partyId }
        ?: throw IllegalArgumentException("Party not found: $partyId")
    return buildPartyLedger(party, trades, settlements)
}

/** The Party's derived current position only (see [partyLedger]). */
fun FarmState.partyLedgerSummary(partyId: String): PartyLedgerSummary =
    partyLedger(partyId).summary

/**
 * Pure projection over the authoritative lists. Only events whose Trade
 * references the party appear; no-party cash trades are excluded (they belong
 * to the overall Hisab-Kitab history, not to any Party Khata — no synthetic
 * "Cash Party" record is created).
 *
 * **Orphan safety:** a [Settlement] always anchors to exactly one [Trade], and
 * a Khata is never built from an unmatched settlement. The validated service
 * and codec boundaries (`FarmSliceService.addSettlement`/`updateSettlement`/
 * `updateTrade`, `FarmBackupCodec.decode`, every persisted transition through
 * `FarmStateValidator.validateFarm`) all reject a settlement whose `tradeId`
 * misses the farm's trades. A settlement failing that check below can only
 * reach a projection from tampered raw bytes; rather than silently dropping a
 * money movement from the party ledger, the projection fails loudly.
 */
internal fun buildPartyLedger(party: Party, trades: List<Trade>, settlements: List<Settlement>): PartyLedger {
    val farmTradeIds = trades.mapTo(mutableSetOf()) { it.id }
    settlements.forEach { settlement ->
        require(settlement.tradeId in farmTradeIds) {
            "Settlement ${settlement.id} references missing trade ${settlement.tradeId}"
        }
    }

    val partyTrades = trades.filter { it.partyId == party.id }
    val tradeById = partyTrades.associateBy { it.id }
    val partySettlements = settlements.filter { it.tradeId in tradeById }

    data class RawEvent(
        val type: PartyLedgerEntryType,
        val sourceId: String,
        val occurredAt: OffsetDateTime,
        val deltaMinor: Long,
        val amountMinor: Long,
        val description: String,
        val tradeId: String,
        val stableKey: String
    )

    val ordered = buildList {
        partyTrades.forEach { trade ->
            val type = if (trade.type == TradeType.SALE) PartyLedgerEntryType.SALE else PartyLedgerEntryType.PURCHASE
            val delta = if (trade.type == TradeType.SALE) trade.totalMinor else -trade.totalMinor
            add(
                RawEvent(
                    type = type,
                    sourceId = trade.id,
                    occurredAt = trade.occurredAt,
                    deltaMinor = delta,
                    amountMinor = trade.totalMinor,
                    description = trade.description,
                    tradeId = trade.id,
                    stableKey = "trade:${trade.id}"
                )
            )
        }
        partySettlements.forEach { settlement ->
            val trade = tradeById[settlement.tradeId]
            if (trade != null) {
                val received = trade.type == TradeType.SALE
                val delta = if (received) -settlement.amountMinor else settlement.amountMinor
                add(
                    RawEvent(
                        type = if (received) PartyLedgerEntryType.PAYMENT_RECEIVED else PartyLedgerEntryType.PAYMENT_MADE,
                        sourceId = settlement.id,
                        occurredAt = settlement.occurredAt,
                        deltaMinor = delta,
                        amountMinor = settlement.amountMinor,
                        description = settlement.note,
                        tradeId = trade.id,
                        stableKey = "settlement:${settlement.id}"
                    )
                )
            }
        }
    }.sortedWith(
        compareBy<RawEvent> { it.occurredAt }
            .thenBy { it.type.orderPriority() }
            .thenBy { it.stableKey }
    )

    var running = 0L
    val entries = ordered.map { event ->
        running = Math.addExact(running, event.deltaMinor)
        PartyLedgerEntry(
            sourceId = event.sourceId,
            sourceType = event.type,
            occurredAt = event.occurredAt,
            amountMinor = event.amountMinor,
            deltaMinor = event.deltaMinor,
            runningBalanceMinor = running,
            description = event.description,
            tradeId = event.tradeId
        )
    }

    return PartyLedger(
        party = party,
        summary = partyLedgerSummaryOf(partyTrades, settlements),
        entries = entries
    )
}

/** Gross to-receive / to-pay and the informational net, from outstanding balances. */
internal fun partyLedgerSummaryOf(trades: List<Trade>, settlements: List<Settlement>): PartyLedgerSummary {
    var toReceive = 0L
    var toPay = 0L
    trades.forEach { trade ->
        val outstanding = settlements.outstandingMinorFor(trade)
        if (trade.type == TradeType.SALE) {
            toReceive = Math.addExact(toReceive, outstanding)
        } else {
            toPay = Math.addExact(toPay, outstanding)
        }
    }
    return PartyLedgerSummary(
        toReceiveMinor = toReceive,
        toPayMinor = toPay,
        netMinor = Math.subtractExact(toReceive, toPay)
    )
}