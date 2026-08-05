package com.susankhya.kisab.ui

import android.content.Context
import com.susankhya.kisab.R
import com.susankhya.kisab.domain.FarmEntryKind
import com.susankhya.kisab.domain.TransactionCategory
import com.susankhya.kisab.domain.TransactionType

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

    fun transactionTypeRes(type: TransactionType): Int = when (type) {
        TransactionType.INCOME -> R.string.transaction_type_income
        TransactionType.EXPENSE -> R.string.transaction_type_expense
    }

    fun transactionCategoryRes(category: TransactionCategory): Int = when (category) {
        TransactionCategory.SALES -> R.string.transaction_category_sales
        TransactionCategory.SERVICES -> R.string.transaction_category_services
        TransactionCategory.OTHER_INCOME -> R.string.transaction_category_other_income
        TransactionCategory.FEED -> R.string.transaction_category_feed
        TransactionCategory.SUPPLIES -> R.string.transaction_category_supplies
        TransactionCategory.LABOR -> R.string.transaction_category_labor
        TransactionCategory.OTHER_EXPENSE -> R.string.transaction_category_other_expense
    }

    fun entryKind(context: Context, kind: FarmEntryKind): String = context.getString(entryKindRes(kind))

    fun transactionType(context: Context, type: TransactionType): String = context.getString(transactionTypeRes(type))

    fun transactionCategory(context: Context, category: TransactionCategory): String =
        context.getString(transactionCategoryRes(category))
}
