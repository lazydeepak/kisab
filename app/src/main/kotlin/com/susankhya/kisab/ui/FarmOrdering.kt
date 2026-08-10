package com.susankhya.kisab.ui

import com.susankhya.kisab.domain.FarmEntryKind
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

    val transactionTypes: List<TransactionType> = listOf(TransactionType.INCOME, TransactionType.EXPENSE)

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
