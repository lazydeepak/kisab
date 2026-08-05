package com.susankhya.kisab

import com.susankhya.kisab.persistence.BackupRejectionReason
import com.susankhya.kisab.persistence.FarmBackupException
import com.susankhya.kisab.ui.FarmUiError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the localized error boundary: expected validation reasons map to
 * specific resources, backup rejections map by typed reason (never by parsing
 * English exception text), and unexpected failures map to the generic message.
 */
class FarmUiErrorMappingTest {

    @Test
    fun everyErrorHasADistinctResource() {
        val resourceIds = FarmUiError.values().map { it.resourceId }
        assertTrue(resourceIds.all { it != 0 })
        assertEquals(FarmUiError.values().size, resourceIds.toSet().size)
    }

    @Test
    fun invalidAndUnsupportedBackupsMapToSpecificError() {
        assertEquals(
            FarmUiError.BACKUP_INVALID_OR_UNSUPPORTED,
            FarmUiError.fromBackupFailure(FarmBackupException(BackupRejectionReason.INVALID_ENVELOPE, "x"))
        )
        assertEquals(
            FarmUiError.BACKUP_INVALID_OR_UNSUPPORTED,
            FarmUiError.fromBackupFailure(FarmBackupException(BackupRejectionReason.UNSUPPORTED_VERSION, "x"))
        )
    }

    @Test
    fun tooLargeAndUnreadableBackupsMapToSpecificError() {
        assertEquals(
            FarmUiError.BACKUP_TOO_LARGE_OR_UNREADABLE,
            FarmUiError.fromBackupFailure(FarmBackupException(BackupRejectionReason.TOO_LARGE, "x"))
        )
        assertEquals(
            FarmUiError.BACKUP_TOO_LARGE_OR_UNREADABLE,
            FarmUiError.fromBackupFailure(FarmBackupException(BackupRejectionReason.UNREADABLE, "x"))
        )
    }

    @Test
    fun unexpectedFailuresResolveToGenericMessage() {
        assertEquals(com.susankhya.kisab.R.string.error_unexpected, FarmUiError.UNEXPECTED.resourceId)
    }

    @Test
    fun backupErrorsDoNotShareTheGenericMessage() {
        assertNotEquals(FarmUiError.UNEXPECTED.resourceId, FarmUiError.BACKUP_INVALID_OR_UNSUPPORTED.resourceId)
        assertNotEquals(FarmUiError.UNEXPECTED.resourceId, FarmUiError.BACKUP_TOO_LARGE_OR_UNREADABLE.resourceId)
    }
}
