package com.susankhya.kisab.domain

import java.math.BigDecimal
import java.math.MathContext

/** Pure, non-persisted calculations used by the offline Kisan toolbox. */
object KisanCalculators {
    private val mathContext = MathContext.DECIMAL128
    private val hundred = BigDecimal("100")
    private val twelve = BigDecimal("12")

    fun arithmetic(
        first: BigDecimal,
        second: BigDecimal,
        operation: ArithmeticOperation
    ): BigDecimal = when (operation) {
        ArithmeticOperation.ADD -> first.add(second, mathContext)
        ArithmeticOperation.SUBTRACT -> first.subtract(second, mathContext)
        ArithmeticOperation.MULTIPLY -> first.multiply(second, mathContext)
        ArithmeticOperation.DIVIDE -> {
            require(second.signum() != 0) { "DIVISION_BY_ZERO" }
            first.divide(second, mathContext)
        }
        ArithmeticOperation.PERCENT_OF -> first.multiply(second, mathContext).divide(hundred, mathContext)
    }.normalized()

    fun profit(cost: BigDecimal, revenue: BigDecimal): ProfitResult {
        require(cost.signum() >= 0) { "COST_NEGATIVE" }
        require(revenue.signum() >= 0) { "REVENUE_NEGATIVE" }
        val amount = revenue.subtract(cost, mathContext)
        return ProfitResult(
            amount = amount.normalized(),
            marginPercent = percentageOrNull(amount, revenue),
            markupPercent = percentageOrNull(amount, cost)
        )
    }

    fun simpleInterest(
        principal: BigDecimal,
        annualRatePercent: BigDecimal,
        months: BigDecimal
    ): SimpleInterestResult {
        require(principal.signum() >= 0) { "PRINCIPAL_NEGATIVE" }
        require(annualRatePercent.signum() >= 0) { "RATE_NEGATIVE" }
        require(months.signum() >= 0) { "MONTHS_NEGATIVE" }
        val interest = principal
            .multiply(annualRatePercent, mathContext)
            .multiply(months, mathContext)
            .divide(hundred.multiply(twelve), mathContext)
            .normalized()
        return SimpleInterestResult(
            interest = interest,
            total = principal.add(interest, mathContext).normalized()
        )
    }

    fun convertLand(value: BigDecimal, from: LandUnit, to: LandUnit): BigDecimal {
        require(value.signum() >= 0) { "LAND_VALUE_NEGATIVE" }
        if (from == to) return value.normalized()
        val squareMetres = value.multiply(from.squareMetres, mathContext)
        return squareMetres.divide(to.squareMetres, mathContext).normalized()
    }

    private fun percentageOrNull(part: BigDecimal, whole: BigDecimal): BigDecimal? =
        if (whole.signum() == 0) null
        else part.multiply(hundred, mathContext).divide(whole, mathContext).normalized()

    private fun BigDecimal.normalized(): BigDecimal = stripTrailingZeros().let {
        if (it.signum() == 0) BigDecimal.ZERO else it
    }
}

enum class ArithmeticOperation { ADD, SUBTRACT, MULTIPLY, DIVIDE, PERCENT_OF }

data class ProfitResult(
    val amount: BigDecimal,
    val marginPercent: BigDecimal?,
    val markupPercent: BigDecimal?
)

data class SimpleInterestResult(val interest: BigDecimal, val total: BigDecimal)

enum class LandUnit(val squareMetres: BigDecimal) {
    SQUARE_METRE(BigDecimal.ONE),
    ROPANI(BigDecimal("508.73704704")),
    AANA(BigDecimal("31.79606544")),
    PAISA(BigDecimal("7.94901636")),
    DAAM(BigDecimal("1.98725409")),
    BIGHA(BigDecimal("6772.631616")),
    KATTHA(BigDecimal("338.6315808")),
    DHUR(BigDecimal("16.93157904"))
}
