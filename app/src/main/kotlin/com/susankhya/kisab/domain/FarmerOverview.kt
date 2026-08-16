package com.susankhya.kisab.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId

data class FarmerProductionOverview(
    val productId: String,
    val name: String,
    val quantity: BigDecimal,
    val unit: ProductUnit,
    val unexplained: BigDecimal? = null,
    val inconsistent: Boolean = false
)

data class FarmerSupplyOverview(
    val supplyId: String,
    val name: String,
    val quantity: BigDecimal,
    val unit: ProductUnit
)

data class FarmerDailyOverview(
    val date: LocalDate,
    val production: List<FarmerProductionOverview>,
    val salesMinor: Long,
    val moneyReceivedMinor: Long,
    val expensesMinor: Long,
    val currentReceivableMinor: Long,
    val creditSalesMinor: Long,
    val supplies: List<FarmerSupplyOverview>
)

data class FarmerMonthlyOverview(
    val month: YearMonth,
    val production: List<FarmerProductionOverview>,
    val salesMinor: Long,
    val moneyReceivedMinor: Long,
    val expensesMinor: Long,
    val currentReceivableMinor: Long,
    val supplies: List<FarmerSupplyOverview>
)

data class FarmerOverview(
    val daily: FarmerDailyOverview,
    val monthly: FarmerMonthlyOverview
)

fun FarmState.farmerOverview(now: OffsetDateTime, zone: ZoneId): FarmerOverview {
    val date = now.atZoneSameInstant(zone).toLocalDate()
    val month = YearMonth.from(date)
    val monthStart = month.atDay(1).atStartOfDay(zone).toOffsetDateTime()
    val monthEnd = month.plusMonths(1).atDay(1).atStartOfDay(zone).toOffsetDateTime()
    fun inDay(time: OffsetDateTime) = time.atZoneSameInstant(zone).toLocalDate() == date
    fun inMonth(time: OffsetDateTime) = !time.isBefore(monthStart) && time.isBefore(monthEnd)
    val tradeById = trades.associateBy { it.id }
    fun sales(period: (OffsetDateTime) -> Boolean): Long = trades.filter { it.type == TradeType.SALE && period(it.occurredAt) }.fold(0L) { total, trade -> Math.addExact(total, trade.totalMinor) }
    fun received(period: (OffsetDateTime) -> Boolean): Long = settlements.filter { period(it.occurredAt) && tradeById[it.tradeId]?.type == TradeType.SALE }.fold(0L) { total, item -> Math.addExact(total, item.amountMinor) }
    fun expenses(period: (OffsetDateTime) -> Boolean): Long = transactions.filter { it.type == TransactionType.EXPENSE && period(it.occurredAt) }.fold(0L) { total, item -> Math.addExact(total, item.amountMinor) }
    fun creditSales(period: (OffsetDateTime) -> Boolean): Long = trades.filter { it.type == TradeType.SALE && period(it.occurredAt) }.fold(0L) { total, trade ->
        val paidAtCreation = settlements.filter { it.tradeId == trade.id && it.isInitialPayment }.fold(0L) { sum, item -> Math.addExact(sum, item.amountMinor) }
        Math.addExact(total, Math.subtractExact(trade.totalMinor, paidAtCreation))
    }
    val receivable = parties.filter { it.role.compatibleWith(TradeType.SALE) }.fold(0L) { total, party -> Math.addExact(total, partyLedgerSummary(party.id).toReceiveMinor) }
    fun production(periodDate: LocalDate, monthly: Boolean): List<FarmerProductionOverview> {
        val records = productionRecords.filter { record ->
            val local = record.occurredAt.atZoneSameInstant(zone).toLocalDate()
            if (monthly) YearMonth.from(local) == month else local == periodDate
        }
        return products.mapNotNull { product ->
            val quantity = records.filter { it.productId == product.id }.fold(BigDecimal.ZERO) { total, record -> total.add(record.quantity) }
            if (quantity <= BigDecimal.ZERO) return@mapNotNull null
            val reconciliation = if (monthly) null else productionReconciliation(product.id, periodDate, zone)
            FarmerProductionOverview(product.id, product.name, quantity, product.defaultUnit, reconciliation?.unexplained, reconciliation?.isInconsistent ?: false)
        }
    }
    fun supplies(): List<FarmerSupplyOverview> = supplies.mapNotNull { supply ->
        val quantity = supplyQuantityAvailable(supply.id)
        quantity.takeIf { it > BigDecimal.ZERO }?.let { FarmerSupplyOverview(supply.id, supply.name, it, supply.unit) }
    }
    return FarmerOverview(
        daily = FarmerDailyOverview(date, production(date, false), sales(::inDay), received(::inDay), expenses(::inDay), receivable, creditSales(::inDay), supplies()),
        monthly = FarmerMonthlyOverview(month, production(date, true), sales(::inMonth), received(::inMonth), expenses(::inMonth), receivable, supplies())
    )
}
