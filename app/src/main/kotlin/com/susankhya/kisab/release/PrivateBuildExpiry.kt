package com.susankhya.kisab.release

import com.susankhya.kisab.ui.Clock
import java.util.concurrent.TimeUnit

/**
 * Private/preview APK lifetime — not subscription licensing and not
 * server min-supported-version policy.
 *
 * Stages use whole UTC calendar days remaining until [expiresAtEpochMillis]
 * (floor of remaining millis / day). Exactly 14 days remaining → WARNING;
 * exactly 3 → CRITICAL; 0 or negative → EXPIRED.
 */
enum class PrivateBuildAccessStage {
    /** Expiry disabled or more than 14 days remaining. */
    VALID,
    /** 4–14 days remaining (inclusive of 14). */
    WARNING,
    /** 1–3 days remaining (inclusive of 3). */
    CRITICAL,
    /** At or past expiry. */
    EXPIRED
}

data class PrivateBuildExpirySnapshot(
    val enabled: Boolean,
    val stage: PrivateBuildAccessStage,
    val expiresAtEpochMillis: Long,
    /** Whole days remaining; 0 when expired or on expiry day after midnight boundary. */
    val daysRemaining: Long,
    val evaluationEpochMillis: Long
) {
    val mutationsAllowed: Boolean
        get() = !enabled || stage != PrivateBuildAccessStage.EXPIRED

    val backupAllowed: Boolean get() = true

    val viewAllowed: Boolean get() = true

    val importAllowed: Boolean get() = mutationsAllowed
}

object PrivateBuildExpiryPolicy {
    val WARNING_DAYS = 14L
    val CRITICAL_DAYS = 3L
    val DAY_MILLIS: Long = TimeUnit.DAYS.toMillis(1)

    /**
     * @param enabled compile-time switch for this APK
     * @param expiresAtEpochMillis immutable build expiry instant (epoch millis)
     * @param nowEpochMillis evaluation clock (may already incorporate last-seen floor)
     */
    fun evaluate(
        enabled: Boolean,
        expiresAtEpochMillis: Long,
        nowEpochMillis: Long
    ): PrivateBuildExpirySnapshot {
        if (!enabled || expiresAtEpochMillis <= 0L) {
            return PrivateBuildExpirySnapshot(
                enabled = false,
                stage = PrivateBuildAccessStage.VALID,
                expiresAtEpochMillis = expiresAtEpochMillis,
                daysRemaining = Long.MAX_VALUE,
                evaluationEpochMillis = nowEpochMillis
            )
        }
        val remainingMillis = expiresAtEpochMillis - nowEpochMillis
        if (remainingMillis <= 0L) {
            return PrivateBuildExpirySnapshot(
                enabled = true,
                stage = PrivateBuildAccessStage.EXPIRED,
                expiresAtEpochMillis = expiresAtEpochMillis,
                daysRemaining = 0L,
                evaluationEpochMillis = nowEpochMillis
            )
        }
        val daysRemaining = remainingMillis / DAY_MILLIS
        val stage = when {
            daysRemaining > WARNING_DAYS -> PrivateBuildAccessStage.VALID
            daysRemaining > CRITICAL_DAYS -> PrivateBuildAccessStage.WARNING
            else -> PrivateBuildAccessStage.CRITICAL
        }
        return PrivateBuildExpirySnapshot(
            enabled = true,
            stage = stage,
            expiresAtEpochMillis = expiresAtEpochMillis,
            daysRemaining = daysRemaining,
            evaluationEpochMillis = nowEpochMillis
        )
    }
}

/**
 * App-local floor for wall-clock observations so a large device-time rollback
 * cannot easily skip past a private-build expiry. Not cryptographically strong;
 * clearing app data resets the floor (build expiry instant still applies).
 */
interface PrivateBuildClockStore {
    fun greatestObservedEpochMillis(): Long?

    fun recordObservedEpochMillis(epochMillis: Long)
}

/**
 * Resolves effective "now" as max(device clock, greatest previously observed).
 */
class PrivateBuildExpiryGate(
    private val enabled: Boolean,
    private val expiresAtEpochMillis: Long,
    private val deviceClock: Clock,
    private val clockStore: PrivateBuildClockStore
) {
    fun snapshot(): PrivateBuildExpirySnapshot {
        val deviceNow = deviceClock.nowMillis()
        val floor = clockStore.greatestObservedEpochMillis() ?: Long.MIN_VALUE
        val effectiveNow = maxOf(deviceNow, floor)
        if (deviceNow > floor) {
            clockStore.recordObservedEpochMillis(deviceNow)
        }
        return PrivateBuildExpiryPolicy.evaluate(enabled, expiresAtEpochMillis, effectiveNow)
    }

    fun mutationsAllowed(): Boolean = snapshot().mutationsAllowed

    fun importAllowed(): Boolean = snapshot().importAllowed

    fun backupAllowed(): Boolean = snapshot().backupAllowed
}
