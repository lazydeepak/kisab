package com.susankhya.kisab.ui

import com.susankhya.kisab.domain.PaymentStatus
import com.susankhya.kisab.domain.TradeType
import java.time.OffsetDateTime

/**
 * Explicit UI-state contract for the inline Sale/Purchase editor.
 *
 * Mirrors [TransactionEditorState]: the activity coordinates views only; every
 * editor decision (open, dirty detection, recreation, save, delete) flows
 * through this value. The trade [TradeType] is fixed for the editor lifetime —
 * a Sale stays a Sale; there is no redundant type radio. Payment is captured as
 * a user-facing [PaymentStatus] plus a free-text [paidText] for the PARTIAL
 * case, and converted to `paidMinor` only when the trade is saved. Trade type,
 * payment status and amounts never live in the view layer alone.
 */
enum class TradeEditorMode {
    CREATE,
    EDIT
}

data class TradeEditorState(
    val mode: TradeEditorMode,
    val tradeId: String?,
    val type: TradeType,
    val partyId: String?,
    val totalText: String,
    val paidStatus: PaymentStatus,
    val paidText: String,
    val description: String,
    val occurredAt: OffsetDateTime
) {
    companion object {
        fun create(type: TradeType, occurredAt: OffsetDateTime): TradeEditorState =
            TradeEditorState(
                mode = TradeEditorMode.CREATE,
                tradeId = null,
                type = type,
                partyId = null,
                totalText = "",
                paidStatus = PaymentStatus.PAID,
                paidText = "",
                description = "",
                occurredAt = occurredAt
            )
    }
}