package com.susankhya.kisab.ui

import androidx.annotation.StringRes
import com.susankhya.kisab.R
import com.susankhya.kisab.persistence.BackupRejectionReason
import com.susankhya.kisab.persistence.FarmBackupException

/**
 * Stable, localized user-action failure codes.
 *
 * The UI maps failures to these codes by operation type or by typed exception
 * reason — never by comparing English exception text. Unexpected internal
 * details are logged at the call site but only [UNEXPECTED] is displayed.
 */
enum class FarmUiError(@param:StringRes val resourceId: Int) {
    FARM_NAME_REQUIRED(R.string.error_farm_name_required),
    ENTRY_LABEL_REQUIRED(R.string.error_entry_label_required),
    ENTRY_QUANTITY_POSITIVE_WHOLE(R.string.error_entry_quantity_positive_whole),
    TRANSACTION_DESCRIPTION_REQUIRED(R.string.error_transaction_description_required),
    AMOUNT_REQUIRED(R.string.error_transaction_amount_required),
    AMOUNT_INVALID(R.string.error_transaction_amount_invalid),
    AMOUNT_NOT_POSITIVE(R.string.error_transaction_amount_positive),
    AMOUNT_TOO_PRECISE(R.string.error_transaction_amount_too_precise),
    AMOUNT_TOO_LARGE(R.string.error_transaction_amount_too_large),
    PARTY_NAME_REQUIRED(R.string.error_party_name_required),
    PARTY_HAS_TRADES(R.string.error_party_has_trades),
    PARTY_ROLE_INCOMPATIBLE(R.string.error_party_role_incompatible),
    TRADE_PARTY_REQUIRED(R.string.error_trade_party_required),
    TRADE_PAID_OUT_OF_RANGE(R.string.error_trade_paid_out_of_range),
    TRADE_TOTAL_REQUIRED(R.string.error_trade_total_required),
    TRADE_HAS_PAYMENTS(R.string.trade_has_payments_error),
    TRADE_TOTAL_BELOW_SETTLED(R.string.trade_total_below_settled_error),
    SETTLEMENT_AMOUNT_REQUIRED(R.string.error_settlement_amount_required),
    SETTLEMENT_OVER_REMAINING(R.string.error_settlement_over_remaining),
    SETTLEMENT_REQUIRES_PARTY(R.string.error_settlement_requires_party),
    BACKUP_INVALID_OR_UNSUPPORTED(R.string.error_backup_invalid_or_unsupported),
    BACKUP_TOO_LARGE_OR_UNREADABLE(R.string.error_backup_too_large_or_unreadable),
    CURRENT_FARM_MISSING(R.string.error_current_farm_missing),
    UNEXPECTED(R.string.error_unexpected);

    companion object {
        fun fromBackupFailure(exception: FarmBackupException): FarmUiError = when (exception.reason) {
            BackupRejectionReason.INVALID_ENVELOPE,
            BackupRejectionReason.UNSUPPORTED_VERSION -> BACKUP_INVALID_OR_UNSUPPORTED
            BackupRejectionReason.TOO_LARGE,
            BackupRejectionReason.UNREADABLE -> BACKUP_TOO_LARGE_OR_UNREADABLE
        }
    }
}
