package com.susankhya.kisab.ui

import com.susankhya.kisab.domain.FarmEntryKind
import com.susankhya.kisab.domain.FinancialPeriodPreset
import com.susankhya.kisab.domain.PartyRole
import com.susankhya.kisab.domain.PaymentStatus
import com.susankhya.kisab.domain.TradeType
import com.susankhya.kisab.domain.TransactionCategory
import com.susankhya.kisab.domain.TransactionType

/**
 * Stable, explicit domain-value ordering for the UI spinners.
 *
 * Selections are resolved by list position over these domain objects, never by
 * parsing localized labels. Semantic ordering matches the pre-M4-01 UI.
 */
object FarmOrdering {
    val entryKinds: List<FarmEntryKind> = listOf(FarmEntryKind.LIVESTOCK, FarmEntryKind.CROP)

    val partyRoles: List<PartyRole> = listOf(PartyRole.CUSTOMER, PartyRole.SUPPLIER, PartyRole.BOTH, PartyRole.OTHER)

    val tradeTypes: List<TradeType> = listOf(TradeType.SALE, TradeType.PURCHASE)

    val paymentStatuses: List<PaymentStatus> = listOf(PaymentStatus.PAID, PaymentStatus.PARTIAL, PaymentStatus.UNPAID)

    val transactionTypes: List<TransactionType> = listOf(TransactionType.INCOME, TransactionType.EXPENSE)

    val financialPeriodPresets: List<FinancialPeriodPreset> = listOf(
        FinancialPeriodPreset.THIS_MONTH,
        FinancialPeriodPreset.LAST_30_DAYS,
        FinancialPeriodPreset.ALL_TIME
    )

    fun categoriesFor(type: TransactionType): List<TransactionCategory> = when (type) {
        TransactionType.INCOME -> listOf(
            TransactionCategory.SALES,
            TransactionCategory.SERVICES,
            TransactionCategory.OTHER_INCOME
        )
        TransactionType.EXPENSE -> listOf(
            TransactionCategory.FEED,
            TransactionCategory.SUPPLIES,
            TransactionCategory.LABOR,
            TransactionCategory.TRANSPORT,
            TransactionCategory.OTHER_EXPENSE
        )
    }
}
