package com.susankhya.kisab.ui

import java.time.OffsetDateTime

/**
 * Explicit UI-state contract for the inline Settlement (payment) editor.
 *
 * Mirrors [TradeEditorState] and [TransactionEditorState]: the activity
 * coordinates views only; every editor decision (open, dirty detection,
 * recreation, save, delete) flows through this value. The form always targets
 * one trade ([tradeId]); CREATE targets a new payment for that trade, EDIT an
 * existing [Settlement] identified by [settlementId]. Money is entered as free
 * text and converted to minor units only when the settlement is saved. The
 * related Trade is loaded by id — the settlement never stores a second party or
 * currency reference.
 */
enum class SettlementEditorMode {
    CREATE,
    EDIT
}

data class SettlementEditorState(
    val mode: SettlementEditorMode,
    val tradeId: String,
    val settlementId: String?,
    val amountText: String,
    val note: String,
    val occurredAt: OffsetDateTime
) {
    companion object {
        fun create(tradeId: String, occurredAt: OffsetDateTime): SettlementEditorState =
            SettlementEditorState(
                mode = SettlementEditorMode.CREATE,
                tradeId = tradeId,
                settlementId = null,
                amountText = "",
                note = "",
                occurredAt = occurredAt
            )
    }
}