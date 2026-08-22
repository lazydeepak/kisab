package com.susankhya.kisab.ui

import android.content.Context
import com.susankhya.kisab.R
import com.susankhya.kisab.domain.FarmActivityType
import com.susankhya.kisab.domain.FarmEntryKind
import com.susankhya.kisab.domain.FarmPlanningCalculator
import com.susankhya.kisab.domain.FinancialPeriodPreset
import com.susankhya.kisab.domain.ArithmeticOperation
import com.susankhya.kisab.domain.LandUnit
import com.susankhya.kisab.domain.PartyRole
import com.susankhya.kisab.domain.PaymentStatus
import com.susankhya.kisab.domain.TradeType
import com.susankhya.kisab.domain.TransactionCategory
import com.susankhya.kisab.domain.TransactionType
import com.susankhya.kisab.domain.TraditionalGrainUnit

/**
 * Stable enum-to-resource mappings for user-visible labels.
 *
 * Each `when` is exhaustive over its enum, so adding a future enum value
 * without a mapping is a compile-time failure (and is also asserted by tests).
 */
object FarmLabels {
    fun entryKindRes(kind: FarmEntryKind): Int = when (kind) {
        FarmEntryKind.LIVESTOCK -> R.string.entry_kind_livestock
        FarmEntryKind.CROP -> R.string.entry_kind_crop
    }

    fun activityTypeRes(activity: FarmActivityType): Int = when (activity) {
        FarmActivityType.CROPS -> R.string.activity_crops
        FarmActivityType.VEGETABLES -> R.string.activity_vegetables
        FarmActivityType.FRUITS_ORCHARD -> R.string.activity_fruits_orchard
        FarmActivityType.POULTRY -> R.string.activity_poultry
        FarmActivityType.CATTLE_BUFFALO_DAIRY -> R.string.activity_cattle_dairy
        FarmActivityType.GOAT_SHEEP -> R.string.activity_goat_sheep
        FarmActivityType.PIG -> R.string.activity_pig
        FarmActivityType.FISHERY -> R.string.activity_fishery
        FarmActivityType.OTHER -> R.string.activity_other
    }

    fun transactionTypeRes(type: TransactionType): Int = when (type) {
        TransactionType.INCOME -> R.string.transaction_type_income
        TransactionType.EXPENSE -> R.string.transaction_type_expense
    }

    fun tradeTypeRes(type: TradeType): Int = when (type) {
        TradeType.SALE -> R.string.trade_type_sale
        TradeType.PURCHASE -> R.string.trade_type_purchase
    }

    fun paymentStatusRes(status: PaymentStatus): Int = when (status) {
        PaymentStatus.PAID -> R.string.payment_status_paid
        PaymentStatus.PARTIAL -> R.string.payment_status_partial
        PaymentStatus.UNPAID -> R.string.payment_status_unpaid
    }

    fun transactionCategoryRes(category: TransactionCategory): Int = when (category) {
        TransactionCategory.SALES -> R.string.transaction_category_sales
        TransactionCategory.SERVICES -> R.string.transaction_category_services
        TransactionCategory.OTHER_INCOME -> R.string.transaction_category_other_income
        TransactionCategory.FEED -> R.string.transaction_category_feed
        TransactionCategory.SUPPLIES -> R.string.transaction_category_supplies
        TransactionCategory.LABOR -> R.string.transaction_category_labor
        TransactionCategory.TRANSPORT -> R.string.transaction_category_transport
        TransactionCategory.OTHER_EXPENSE -> R.string.transaction_category_other_expense
    }

    fun partyRoleRes(role: PartyRole): Int = when (role) {
        PartyRole.CUSTOMER -> R.string.party_role_customer
        PartyRole.SUPPLIER -> R.string.party_role_supplier
        PartyRole.BOTH -> R.string.party_role_both
        PartyRole.OTHER -> R.string.party_role_other
    }

    fun financialPeriodPresetRes(preset: FinancialPeriodPreset): Int = when (preset) {
        FinancialPeriodPreset.THIS_MONTH -> R.string.period_this_month
        FinancialPeriodPreset.LAST_30_DAYS -> R.string.period_last_30_days
        FinancialPeriodPreset.ALL_TIME -> R.string.period_all_time
    }

    fun arithmeticOperationRes(operation: ArithmeticOperation): Int = when (operation) {
        ArithmeticOperation.ADD -> R.string.arithmetic_add
        ArithmeticOperation.SUBTRACT -> R.string.arithmetic_subtract
        ArithmeticOperation.MULTIPLY -> R.string.arithmetic_multiply
        ArithmeticOperation.DIVIDE -> R.string.arithmetic_divide
        ArithmeticOperation.PERCENT_OF -> R.string.arithmetic_percent_of
    }

    fun landUnitRes(unit: LandUnit): Int = when (unit) {
        LandUnit.SQUARE_METRE -> R.string.land_unit_square_metre
        LandUnit.ROPANI -> R.string.land_unit_ropani
        LandUnit.AANA -> R.string.land_unit_aana
        LandUnit.PAISA -> R.string.land_unit_paisa
        LandUnit.DAAM -> R.string.land_unit_daam
        LandUnit.BIGHA -> R.string.land_unit_bigha
        LandUnit.KATTHA -> R.string.land_unit_kattha
        LandUnit.DHUR -> R.string.land_unit_dhur
    }

    fun grainUnitRes(unit: TraditionalGrainUnit): Int = when (unit) {
        TraditionalGrainUnit.MANA -> R.string.grain_unit_mana
        TraditionalGrainUnit.PATHI -> R.string.grain_unit_pathi
        TraditionalGrainUnit.MURI -> R.string.grain_unit_muri
    }

    fun farmPlanningCalculatorRes(calculator: FarmPlanningCalculator): Int = when (calculator) {
        FarmPlanningCalculator.SEED -> R.string.farm_planning_calculator_seed
        FarmPlanningCalculator.FERTILIZER -> R.string.farm_planning_calculator_fertilizer
        FarmPlanningCalculator.FEED -> R.string.farm_planning_calculator_feed
        FarmPlanningCalculator.MILK -> R.string.farm_planning_calculator_milk
        FarmPlanningCalculator.CROP_YIELD -> R.string.farm_planning_calculator_crop_yield
    }

    fun entryKind(context: Context, kind: FarmEntryKind): String = context.getString(entryKindRes(kind))

    fun activityType(context: Context, activity: FarmActivityType): String =
        context.getString(activityTypeRes(activity))

    fun transactionType(context: Context, type: TransactionType): String = context.getString(transactionTypeRes(type))

    fun tradeType(context: Context, type: TradeType): String = context.getString(tradeTypeRes(type))

    fun paymentStatus(context: Context, status: PaymentStatus): String = context.getString(paymentStatusRes(status))

    fun transactionCategory(context: Context, category: TransactionCategory): String =
        context.getString(transactionCategoryRes(category))

    fun partyRole(context: Context, role: PartyRole): String = context.getString(partyRoleRes(role))

    fun financialPeriodPreset(context: Context, preset: FinancialPeriodPreset): String =
        context.getString(financialPeriodPresetRes(preset))

    fun arithmeticOperation(context: Context, operation: ArithmeticOperation): String =
        context.getString(arithmeticOperationRes(operation))

    fun landUnit(context: Context, unit: LandUnit): String = context.getString(landUnitRes(unit))

    fun grainUnit(context: Context, unit: TraditionalGrainUnit): String = context.getString(grainUnitRes(unit))

    fun farmPlanningCalculator(context: Context, calculator: FarmPlanningCalculator): String =
        context.getString(farmPlanningCalculatorRes(calculator))
}
