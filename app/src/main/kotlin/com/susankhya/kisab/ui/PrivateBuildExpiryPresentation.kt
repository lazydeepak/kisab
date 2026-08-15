package com.susankhya.kisab.ui

import com.susankhya.kisab.release.PrivateBuildAccessStage
import com.susankhya.kisab.release.PrivateBuildExpirySnapshot
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Farmer-facing copy helpers for private-build expiry. Resource ids are resolved
 * by the activity; this object only chooses which message pattern applies.
 */
object PrivateBuildExpiryPresentation {

    enum class MessageKind {
        NONE,
        WARNING_DAYS_REMAINING,
        CRITICAL_DAYS_REMAINING,
        EXPIRED_BANNER
    }

    data class UiHints(
        val kind: MessageKind,
        val daysRemaining: Long,
        val showPersistentBanner: Boolean,
        val shouldShowStartupDialog: Boolean
    )

    /**
     * @param warningAlreadyShownToday when true, WARNING stage does not open another dialog
     */
    fun uiHints(
        snapshot: PrivateBuildExpirySnapshot,
        warningAlreadyShownToday: Boolean
    ): UiHints {
        if (!snapshot.enabled) {
            return UiHints(MessageKind.NONE, snapshot.daysRemaining, false, false)
        }
        return when (snapshot.stage) {
            PrivateBuildAccessStage.VALID ->
                UiHints(MessageKind.NONE, snapshot.daysRemaining, false, false)
            PrivateBuildAccessStage.WARNING ->
                UiHints(
                    kind = MessageKind.WARNING_DAYS_REMAINING,
                    daysRemaining = snapshot.daysRemaining,
                    showPersistentBanner = false,
                    shouldShowStartupDialog = !warningAlreadyShownToday
                )
            PrivateBuildAccessStage.CRITICAL ->
                UiHints(
                    kind = MessageKind.CRITICAL_DAYS_REMAINING,
                    daysRemaining = snapshot.daysRemaining,
                    showPersistentBanner = true,
                    shouldShowStartupDialog = true
                )
            PrivateBuildAccessStage.EXPIRED ->
                UiHints(
                    kind = MessageKind.EXPIRED_BANNER,
                    daysRemaining = 0L,
                    showPersistentBanner = true,
                    shouldShowStartupDialog = true
                )
        }
    }

    fun formatExpiryDate(
        expiresAtEpochMillis: Long,
        locale: Locale,
        zone: ZoneId
    ): String {
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
        return Instant.ofEpochMilli(expiresAtEpochMillis).atZone(zone).toLocalDate().format(formatter)
    }

    /** Local calendar day key for once-per-day WARNING dialogs. */
    fun localDayKey(epochMillis: Long, zone: ZoneId): String =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate().toString()
}
