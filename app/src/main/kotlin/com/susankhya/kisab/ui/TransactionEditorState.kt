package com.susankhya.kisab.ui

import com.susankhya.kisab.domain.FarmActivityType
import com.susankhya.kisab.domain.TransactionCategory
import com.susankhya.kisab.domain.TransactionType
import java.time.OffsetDateTime

/**
 * Explicit UI-state contract for the inline transaction editor.
 *
 * The activity coordinates views only; every editor decision (open, dirty
 * detection, recreation, save, delete) flows through this value. Dirty means
 * the current state differs from the immutable [baseline] captured when the
 * editor opened — never "any field is non-empty", because a freshly opened
 * editor already carries defaults (type, category, current time). Currency is
 * a farm-level setting (see [FarmState.currencyCode]); the editor never owns it.
 * [activity] is an optional farm-activity association (`null` = general/farm-wide).
 */
enum class TransactionEditorMode {
    CREATE,
    EDIT
}

data class TransactionEditorState(
    val mode: TransactionEditorMode,
    val transactionId: String?,
    val type: TransactionType,
    val category: TransactionCategory,
    val activity: FarmActivityType?,
    val amountText: String,
    val description: String,
    val occurredAt: OffsetDateTime
) {
    companion object {
        fun create(
            type: TransactionType,
            occurredAt: OffsetDateTime,
            category: TransactionCategory? = null
        ): TransactionEditorState =
            TransactionEditorState(
                mode = TransactionEditorMode.CREATE,
                transactionId = null,
                type = type,
                category = category ?: FarmOrdering.categoriesFor(type).first(),
                activity = null,
                amountText = "",
                description = "",
                occurredAt = occurredAt
            )
    }
}
