package com.susankhya.kisab.persistence

enum class BackupRejectionReason {
    INVALID_ENVELOPE,
    UNSUPPORTED_VERSION,
    TOO_LARGE,
    UNREADABLE
}

class FarmBackupException(
    val reason: BackupRejectionReason,
    message: String,
    cause: Throwable? = null
) : IllegalArgumentException(message, cause)
