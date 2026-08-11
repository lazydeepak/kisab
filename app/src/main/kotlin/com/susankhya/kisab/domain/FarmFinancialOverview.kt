package com.susankhya.kisab.domain

import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * M5-05 Farm Financial Overview.
 *
 * A **non-persisted derived read model** built on every call from the
 * authoritative persisted facts — [FarmState.transactions] (Home cash),
 * [FarmState.trades], [FarmState.settlements], and [FarmState.parties]. Nothing
 * is stored, migrated, or mutated by projecting it, so it can never go stale:
 * editing or deleting any underlying fact automatically changes the next
 * overview. The overview is a view over the facts, never a second accounting
 * authority, and it deliberately **never claims profit** — cash income/expense
 * (Home) and trade/payment flows (Hisab-Kitab) are reported as separate
 * activity totals and are never combined into a single figure.
 *
 * ## Home separation is preserved
 *
 * Settlements are payment history, not Home cash events: a paid sale never adds
 * income to the cash totals (unchanged since M5-02/M5-03). Cash activity comes
 * only from [FarmTransaction] records; trade activity comes only from trades
 * and their settlements. The "net position" (receivable − payable) is
 * **informational only** and never settles any individual trade.
 *
 * ## Periods
 *
 * Periods are inclusive-start / exclusive-end intervals ([FinancialPeriod])
 * computed purely from a stable clock ([OffsetDateTime] `now`) and zone input,
 * so presets are reproducible in tests and across renders.
 *
 * ## Arithmetic
 *
 * All money aggregation uses exact minor-unit `Long` arithmetic via
 * [Math.addExact]/[Math.subtractExact] and throws [ArithmeticException] on
 * overflow rather than silently wrapping.
 *
 * ## Orphan safety
 *
 * Every [Settlement] must anchor to an existing [Trade]. The validated service
 * and codec boundaries already reject orphans; the projection re-asserts the
 * invariant loudly rather than silently dropping a money movement (mirrors the
 * M5-04 Party Khata guard).
 */
enum class FinancialPeriodPreset {
    THIS_MONTH,
    LAST_30_DAYS,
    ALL_TIME
}

/**
 * An inclusive-start / exclusive-end period over absolute instants. [preset]
 * is kept so the UI can render the selector without re-deriving bounds.
 */
data class FinancialPeriod(
    val preset: FinancialPeriodPreset,
    val startInclusive: OffsetDateTime,
    val endExclusive: OffsetDateTime
)

/** Period Home cash activity (FarmTransaction only). [netMinor] = income − expense. */
data class FinancialCashTotals(
    val incomeMinor: Long,
    val expenseMinor: Long,
    val netMinor: Long
)

/** Period Hisab-Kitab activity. Payments carry the direction of their trade. */
data class FinancialTradeTotals(
    val grossSalesMinor: Long,
    val grossPurchasesMinor: Long,
    val paymentsReceivedMinor: Long,
    val paymentsMadeMinor: Long
)

/**
 * The farm's position **as of the end of the period** ([FinancialPeriod.endExclusive]):
 * obligations (trades) that existed before the cutoff, net of settlements that
 * were recorded before the cutoff. [netMinor] = receivable − payable and is
 * informational only — it never settles a trade.
 *
 * The cutoff is **exclusive**: a trade or settlement at exactly [endExclusive]
 * is not counted. The UI therefore labels the position "As of" the last
 * *included* instant (`endExclusive.minusNanos(1)`) rather than the excluded
 * boundary itself, so the displayed timestamp never misrepresents an excluded
 * instant as included.
 */
data class FinancialPosition(
    val receivableMinor: Long,
    val payableMinor: Long,
    val netMinor: Long
)

/**
 * One month of the compact monthly trend. Rows cover a continuous month range
 * (zero-filled months are shown honestly), newest months capped to
 * [FarmFinancialPeriods.MAX_TREND_ROWS], ordered oldest → newest.
 */
data class FinancialTrendRow(
    val year: Int,
    val month: Int,
    val cashIncomeMinor: Long,
    val cashExpenseMinor: Long,
    val salesMinor: Long,
    val purchasesMinor: Long,
    val paymentsReceivedMinor: Long,
    val paymentsMadeMinor: Long
)

/** The complete non-persisted overview read model. */
data class FarmFinancialOverview(
    val period: FinancialPeriod,
    val cashTotals: FinancialCashTotals,
    val tradeTotals: FinancialTradeTotals,
    val currentPosition: FinancialPosition,
    val monthlyTrend: List<FinancialTrendRow>
)

/** Pure, deterministic derivation of [FinancialPeriod] bounds from a clock + zone. */
object FarmFinancialPeriods {
    /** Maximum number of monthly trend rows (keeps the trend compact, no chart). */
    const val MAX_TREND_ROWS = 12

    /** All-time anchor: year 1 UTC is before every realistic persisted event. */
    private val ALL_TIME_START: OffsetDateTime =
        OffsetDateTime.of(1, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)

    fun periodFor(preset: FinancialPeriodPreset, now: OffsetDateTime, zone: ZoneId): FinancialPeriod =
        when (preset) {
            FinancialPeriodPreset.THIS_MONTH -> {
                val start = now.atZoneSameInstant(zone)
                    .toLocalDate()
                    .withDayOfMonth(1)
                    .atStartOfDay(zone)
                    .toOffsetDateTime()
                FinancialPeriod(preset, start, start.plusMonths(1))
            }
            FinancialPeriodPreset.LAST_30_DAYS -> FinancialPeriod(preset, now.minusDays(30), now)
            FinancialPeriodPreset.ALL_TIME -> FinancialPeriod(preset, ALL_TIME_START, now)
        }
}

/** Pure overview projection over a [FarmState]; see [FarmFinancialOverview]. */
fun FarmState.financialOverview(
    preset: FinancialPeriodPreset,
    now: OffsetDateTime,
    zone: ZoneId
): FarmFinancialOverview = buildFarmFinancialOverview(this, preset, now, zone)

/** Pure overview projection; see [FarmFinancialOverview]. */
internal fun buildFarmFinancialOverview(
    farm: FarmState,
    preset: FinancialPeriodPreset,
    now: OffsetDateTime,
    zone: ZoneId
): FarmFinancialOverview {
    val period = FarmFinancialPeriods.periodFor(preset, now, zone)

    val farmTradeIds = farm.trades.mapTo(mutableSetOf()) { it.id }
    farm.settlements.forEach { settlement ->
        require(settlement.tradeId in farmTradeIds) {
            "Settlement ${settlement.id} references missing trade ${settlement.tradeId}"
        }
    }
    val tradeById = farm.trades.associateBy { it.id }

    var cashIncome = 0L
    var cashExpense = 0L
    var grossSales = 0L
    var grossPurchases = 0L
    var paymentsReceived = 0L
    var paymentsMade = 0L

    val monthly = linkedMapOf<YearMonth, TrendAccumulator>()

    fun monthOf(occurredAt: OffsetDateTime): YearMonth {
        val localDate = occurredAt.atZoneSameInstant(zone).toLocalDate()
        return YearMonth.of(localDate.year, localDate.monthValue)
    }

    fun trend(month: YearMonth): TrendAccumulator = monthly.getOrPut(month) { TrendAccumulator() }

    farm.transactions.forEach { transaction ->
        if (inPeriod(transaction.occurredAt, period)) {
            val accumulator = trend(monthOf(transaction.occurredAt))
            if (transaction.type == TransactionType.INCOME) {
                cashIncome = Math.addExact(cashIncome, transaction.amountMinor)
                accumulator.cashIncome = Math.addExact(accumulator.cashIncome, transaction.amountMinor)
            } else {
                cashExpense = Math.addExact(cashExpense, transaction.amountMinor)
                accumulator.cashExpense = Math.addExact(accumulator.cashExpense, transaction.amountMinor)
            }
        }
    }

    farm.trades.forEach { trade ->
        if (inPeriod(trade.occurredAt, period)) {
            val accumulator = trend(monthOf(trade.occurredAt))
            if (trade.type == TradeType.SALE) {
                grossSales = Math.addExact(grossSales, trade.totalMinor)
                accumulator.sales = Math.addExact(accumulator.sales, trade.totalMinor)
            } else {
                grossPurchases = Math.addExact(grossPurchases, trade.totalMinor)
                accumulator.purchases = Math.addExact(accumulator.purchases, trade.totalMinor)
            }
        }
    }

    farm.settlements.forEach { settlement ->
        if (inPeriod(settlement.occurredAt, period)) {
            val trade = tradeById.getValue(settlement.tradeId)
            val accumulator = trend(monthOf(settlement.occurredAt))
            if (trade.type == TradeType.SALE) {
                paymentsReceived = Math.addExact(paymentsReceived, settlement.amountMinor)
                accumulator.received = Math.addExact(accumulator.received, settlement.amountMinor)
            } else {
                paymentsMade = Math.addExact(paymentsMade, settlement.amountMinor)
                accumulator.made = Math.addExact(accumulator.made, settlement.amountMinor)
            }
        }
    }

    val cashNet = Math.subtractExact(cashIncome, cashExpense)

    var receivable = 0L
    var payable = 0L
    farm.trades.forEach { trade ->
        if (trade.occurredAt.isBefore(period.endExclusive)) {
            var paid = 0L
            farm.settlements.forEach { settlement ->
                if (settlement.tradeId == trade.id && settlement.occurredAt.isBefore(period.endExclusive)) {
                    paid = Math.addExact(paid, settlement.amountMinor)
                }
            }
            val outstanding = Math.subtractExact(trade.totalMinor, paid)
            if (outstanding > 0) {
                if (trade.type == TradeType.SALE) {
                    receivable = Math.addExact(receivable, outstanding)
                } else {
                    payable = Math.addExact(payable, outstanding)
                }
            }
        }
    }
    val positionNet = Math.subtractExact(receivable, payable)

    return FarmFinancialOverview(
        period = period,
        cashTotals = FinancialCashTotals(cashIncome, cashExpense, cashNet),
        tradeTotals = FinancialTradeTotals(grossSales, grossPurchases, paymentsReceived, paymentsMade),
        currentPosition = FinancialPosition(receivable, payable, positionNet),
        monthlyTrend = buildTrendRows(monthly)
    )
}

private fun inPeriod(occurredAt: OffsetDateTime, period: FinancialPeriod): Boolean =
    !occurredAt.isBefore(period.startInclusive) && occurredAt.isBefore(period.endExclusive)

/** Zero-filled continuous month range, newest months capped, oldest → newest. */
private fun buildTrendRows(monthly: Map<YearMonth, TrendAccumulator>): List<FinancialTrendRow> {
    if (monthly.isEmpty()) return emptyList()
    val present = monthly.keys.sorted()
    val continuous = generateSequence(present.first()) { it.plusMonths(1) }
        .takeWhile { !it.isAfter(present.last()) }
        .toList()
    return continuous.takeLast(FarmFinancialPeriods.MAX_TREND_ROWS).map { month ->
        val accumulator = monthly[month] ?: TrendAccumulator()
        FinancialTrendRow(
            year = month.year,
            month = month.monthValue,
            cashIncomeMinor = accumulator.cashIncome,
            cashExpenseMinor = accumulator.cashExpense,
            salesMinor = accumulator.sales,
            purchasesMinor = accumulator.purchases,
            paymentsReceivedMinor = accumulator.received,
            paymentsMadeMinor = accumulator.made
        )
    }
}

private class TrendAccumulator {
    var cashIncome = 0L
    var cashExpense = 0L
    var sales = 0L
    var purchases = 0L
    var received = 0L
    var made = 0L
}
