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

    fun seedQuantityAndCost(
        area: BigDecimal,
        seedKgPerLandUnit: BigDecimal,
        pricePerKg: BigDecimal
    ): SeedFertilizerResult {
        require(area.signum() >= 0) { "AREA_NEGATIVE" }
        require(seedKgPerLandUnit.signum() >= 0) { "RATE_NEGATIVE" }
        require(pricePerKg.signum() >= 0) { "PRICE_NEGATIVE" }
        val quantityKg = area.multiply(seedKgPerLandUnit, mathContext).normalized()
        val totalCost = quantityKg.multiply(pricePerKg, mathContext).normalized()
        return SeedFertilizerResult(quantityKg = quantityKg, totalCost = totalCost)
    }

    fun fertilizerQuantityAndCost(
        area: BigDecimal,
        fertilizerKgPerLandUnit: BigDecimal,
        pricePerKg: BigDecimal
    ): SeedFertilizerResult {
        require(area.signum() >= 0) { "AREA_NEGATIVE" }
        require(fertilizerKgPerLandUnit.signum() >= 0) { "RATE_NEGATIVE" }
        require(pricePerKg.signum() >= 0) { "PRICE_NEGATIVE" }
        val quantityKg = area.multiply(fertilizerKgPerLandUnit, mathContext).normalized()
        val totalCost = quantityKg.multiply(pricePerKg, mathContext).normalized()
        return SeedFertilizerResult(quantityKg = quantityKg, totalCost = totalCost)
    }

    fun feedRequirementAndCost(
        animalCount: Int,
        kgPerAnimalPerDay: BigDecimal,
        days: BigDecimal,
        pricePerKg: BigDecimal
    ): FeedResult {
        require(animalCount >= 0) { "ANIMAL_COUNT_NEGATIVE" }
        require(kgPerAnimalPerDay.signum() >= 0) { "RATE_NEGATIVE" }
        require(days.signum() >= 0) { "DAYS_NEGATIVE" }
        require(pricePerKg.signum() >= 0) { "PRICE_NEGATIVE" }
        val animalCountDecimal = BigDecimal(animalCount)
        val totalKg = animalCountDecimal
            .multiply(kgPerAnimalPerDay, mathContext)
            .multiply(days, mathContext)
            .normalized()
        val totalCost = totalKg.multiply(pricePerKg, mathContext).normalized()
        return FeedResult(totalKg = totalKg, totalCost = totalCost)
    }

    fun milkProductionAndRevenue(
        milkingAnimalCount: Int,
        litresPerAnimalPerDay: BigDecimal,
        days: BigDecimal,
        pricePerLitre: BigDecimal
    ): MilkResult {
        require(milkingAnimalCount >= 0) { "ANIMAL_COUNT_NEGATIVE" }
        require(litresPerAnimalPerDay.signum() >= 0) { "RATE_NEGATIVE" }
        require(days.signum() >= 0) { "DAYS_NEGATIVE" }
        require(pricePerLitre.signum() >= 0) { "PRICE_NEGATIVE" }
        val animalCountDecimal = BigDecimal(milkingAnimalCount)
        val totalLitres = animalCountDecimal
            .multiply(litresPerAnimalPerDay, mathContext)
            .multiply(days, mathContext)
            .normalized()
        val revenue = totalLitres.multiply(pricePerLitre, mathContext).normalized()
        return MilkResult(totalLitres = totalLitres, revenue = revenue)
    }

    fun cropYieldAndRevenue(
        area: BigDecimal,
        expectedKgPerLandUnit: BigDecimal,
        pricePerKg: BigDecimal
    ): CropYieldResult {
        require(area.signum() >= 0) { "AREA_NEGATIVE" }
        require(expectedKgPerLandUnit.signum() >= 0) { "RATE_NEGATIVE" }
        require(pricePerKg.signum() >= 0) { "PRICE_NEGATIVE" }
        val totalKg = area.multiply(expectedKgPerLandUnit, mathContext).normalized()
        val revenue = totalKg.multiply(pricePerKg, mathContext).normalized()
        return CropYieldResult(totalKg = totalKg, revenue = revenue)
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

data class SeedFertilizerResult(
    val quantityKg: BigDecimal,
    val totalCost: BigDecimal
)

data class FeedResult(
    val totalKg: BigDecimal,
    val totalCost: BigDecimal
)

data class MilkResult(
    val totalLitres: BigDecimal,
    val revenue: BigDecimal
)

data class CropYieldResult(
    val totalKg: BigDecimal,
    val revenue: BigDecimal
)

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

enum class FarmPlanningCalculator {
    SEED,
    FERTILIZER,
    FEED,
    MILK,
    CROP_YIELD
}
