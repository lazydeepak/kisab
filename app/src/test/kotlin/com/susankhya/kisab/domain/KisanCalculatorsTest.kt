package com.susankhya.kisab.domain

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class KisanCalculatorsTest {
    @Test fun arithmeticSupportsAllOperations() {
        assertDecimal("12.5", KisanCalculators.arithmetic(bd("10"), bd("2.5"), ArithmeticOperation.ADD))
        assertDecimal("7.5", KisanCalculators.arithmetic(bd("10"), bd("2.5"), ArithmeticOperation.SUBTRACT))
        assertDecimal("25", KisanCalculators.arithmetic(bd("10"), bd("2.5"), ArithmeticOperation.MULTIPLY))
        assertDecimal("4", KisanCalculators.arithmetic(bd("10"), bd("2.5"), ArithmeticOperation.DIVIDE))
        assertDecimal("25", KisanCalculators.arithmetic(bd("200"), bd("12.5"), ArithmeticOperation.PERCENT_OF))
    }

    @Test fun divisionByZeroIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            KisanCalculators.arithmetic(bd("1"), BigDecimal.ZERO, ArithmeticOperation.DIVIDE)
        }
    }

    @Test fun profitReportsProfitLossMarginAndMarkup() {
        val profit = KisanCalculators.profit(bd("800"), bd("1000"))
        assertDecimal("200", profit.amount)
        assertDecimal("20", profit.marginPercent!!)
        assertDecimal("25", profit.markupPercent!!)

        val loss = KisanCalculators.profit(bd("1000"), bd("800"))
        assertDecimal("-200", loss.amount)
        assertDecimal("-25", loss.marginPercent!!)
        assertDecimal("-20", loss.markupPercent!!)
    }

    @Test fun undefinedProfitPercentagesAreExplicit() {
        val result = KisanCalculators.profit(BigDecimal.ZERO, BigDecimal.ZERO)
        assertNull(result.marginPercent)
        assertNull(result.markupPercent)
    }

    @Test fun negativeProfitInputsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { KisanCalculators.profit(bd("-1"), bd("2")) }
        assertThrows(IllegalArgumentException::class.java) { KisanCalculators.profit(bd("1"), bd("-2")) }
    }

    @Test fun simpleInterestUsesAnnualRateAndMonths() {
        val result = KisanCalculators.simpleInterest(bd("12000"), bd("10"), bd("6"))
        assertDecimal("600", result.interest)
        assertDecimal("12600", result.total)
    }

    @Test fun negativeInterestInputsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            KisanCalculators.simpleInterest(bd("-1"), bd("1"), bd("1"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            KisanCalculators.simpleInterest(bd("1"), bd("-1"), bd("1"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            KisanCalculators.simpleInterest(bd("1"), bd("1"), bd("-1"))
        }
    }

    @Test fun hillLandConversionsUseCanonicalRelationships() {
        assertDecimal("16", KisanCalculators.convertLand(BigDecimal.ONE, LandUnit.ROPANI, LandUnit.AANA))
        assertDecimal("4", KisanCalculators.convertLand(BigDecimal.ONE, LandUnit.AANA, LandUnit.PAISA))
        assertDecimal("4", KisanCalculators.convertLand(BigDecimal.ONE, LandUnit.PAISA, LandUnit.DAAM))
    }

    @Test fun teraiLandConversionsUseCanonicalRelationships() {
        assertDecimal("20", KisanCalculators.convertLand(BigDecimal.ONE, LandUnit.BIGHA, LandUnit.KATTHA))
        assertDecimal("20", KisanCalculators.convertLand(BigDecimal.ONE, LandUnit.KATTHA, LandUnit.DHUR))
    }

    @Test fun landConversionRoundTripsAndRejectsNegativeArea() {
        val squareMetres = KisanCalculators.convertLand(bd("2.5"), LandUnit.ROPANI, LandUnit.SQUARE_METRE)
        val roundTrip = KisanCalculators.convertLand(squareMetres, LandUnit.SQUARE_METRE, LandUnit.ROPANI)
        assertDecimal("2.5", roundTrip)
        assertThrows(IllegalArgumentException::class.java) {
            KisanCalculators.convertLand(bd("-1"), LandUnit.ROPANI, LandUnit.AANA)
        }
    }

    @Test fun seedAndFertilizerCalculateQuantityAndCost() {
        val seed = KisanCalculators.seedQuantityAndCost(bd("2.5"), bd("12"), bd("80"))
        assertDecimal("30", seed.quantityKg)
        assertDecimal("2400", seed.totalCost)

        val fertilizer = KisanCalculators.fertilizerQuantityAndCost(bd("3"), bd("7.5"), bd("50"))
        assertDecimal("22.5", fertilizer.quantityKg)
        assertDecimal("1125", fertilizer.totalCost)
    }

    @Test fun feedCalculatesRequirementAndCost() {
        val result = KisanCalculators.feedRequirementAndCost(4, bd("2.5"), bd("30"), bd("45"))
        assertDecimal("300", result.totalKg)
        assertDecimal("13500", result.totalCost)
    }

    @Test fun milkCalculatesProductionAndRevenue() {
        val result = KisanCalculators.milkProductionAndRevenue(3, bd("6.5"), bd("10"), bd("90"))
        assertDecimal("195", result.totalLitres)
        assertDecimal("17550", result.revenue)
    }

    @Test fun cropYieldCalculatesTotalAndRevenue() {
        val result = KisanCalculators.cropYieldAndRevenue(bd("1.5"), bd("1200"), bd("35"))
        assertDecimal("1800", result.totalKg)
        assertDecimal("63000", result.revenue)
    }

    @Test fun farmPlanningCalculatorsAcceptZeroInputs() {
        assertDecimal("0", KisanCalculators.seedQuantityAndCost(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO).totalCost)
        assertDecimal("0", KisanCalculators.fertilizerQuantityAndCost(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO).totalCost)
        assertDecimal("0", KisanCalculators.feedRequirementAndCost(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO).totalCost)
        assertDecimal("0", KisanCalculators.milkProductionAndRevenue(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO).revenue)
        assertDecimal("0", KisanCalculators.cropYieldAndRevenue(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO).revenue)
    }

    @Test fun farmPlanningCalculatorsRejectNegativeInputs() {
        assertThrows(IllegalArgumentException::class.java) {
            KisanCalculators.seedQuantityAndCost(bd("-1"), BigDecimal.ONE, BigDecimal.ONE)
        }
        assertThrows(IllegalArgumentException::class.java) {
            KisanCalculators.fertilizerQuantityAndCost(BigDecimal.ONE, bd("-1"), BigDecimal.ONE)
        }
        assertThrows(IllegalArgumentException::class.java) {
            KisanCalculators.feedRequirementAndCost(-1, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE)
        }
        assertThrows(IllegalArgumentException::class.java) {
            KisanCalculators.milkProductionAndRevenue(1, BigDecimal.ONE, bd("-1"), BigDecimal.ONE)
        }
        assertThrows(IllegalArgumentException::class.java) {
            KisanCalculators.cropYieldAndRevenue(BigDecimal.ONE, BigDecimal.ONE, bd("-1"))
        }
    }

    private fun bd(value: String) = BigDecimal(value)
    private fun assertDecimal(expected: String, actual: BigDecimal) =
        assertEquals(0, BigDecimal(expected).compareTo(actual))
}
