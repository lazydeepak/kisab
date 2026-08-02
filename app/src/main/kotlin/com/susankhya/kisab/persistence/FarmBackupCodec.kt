package com.susankhya.kisab.persistence

import com.susankhya.kisab.domain.FarmState
import com.susankhya.kisab.domain.FarmStateValidator
import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Base64

object FarmBackupCodec {
    const val CURRENT_SCHEMA_VERSION = 1
    const val MAX_BACKUP_BYTES = 256 * 1024

    private const val FIELD_SEPARATOR = "\u001F"
    private val UTC_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC)

    fun encode(farm: FarmState, exportedAt: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC)): String {
        val farmPayload = Base64.getEncoder().encodeToString(FarmPersistenceCodec.encode(farm).toByteArray(StandardCharsets.UTF_8))
        val exportedAtText = exportedAt.withOffsetSameInstant(ZoneOffset.UTC).format(UTC_FORMATTER)
        val encoded = listOf(CURRENT_SCHEMA_VERSION.toString(), exportedAtText, farmPayload).joinToString(FIELD_SEPARATOR)
        require(encoded.toByteArray(StandardCharsets.UTF_8).size <= MAX_BACKUP_BYTES) { "Backup file is too large" }
        return encoded
    }

    fun decode(encoded: String): FarmBackupEnvelope {
        return try {
            decodeInternal(encoded)
        } catch (exception: RuntimeException) {
            throw IllegalArgumentException(exception.message ?: "Invalid backup envelope", exception)
        }
    }

    fun decodeOrNull(encoded: String): FarmBackupEnvelope? {
        return try {
            decodeInternal(encoded)
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun decodeInternal(encoded: String): FarmBackupEnvelope {
        require(encoded.isNotBlank()) { "Invalid backup envelope" }
        require(encoded.toByteArray(StandardCharsets.UTF_8).size <= MAX_BACKUP_BYTES) { "Backup file is too large" }
        val parts = encoded.split(FIELD_SEPARATOR)
        require(parts.size == 3) { "Invalid backup envelope" }
        val version = parts[0].toIntOrNull() ?: throw IllegalArgumentException("Invalid backup envelope")
        require(version == CURRENT_SCHEMA_VERSION) { "Unsupported backup version: $version" }
        val exportedAt = OffsetDateTime.parse(parts[1], UTC_FORMATTER)
        val farmPayload = String(Base64.getDecoder().decode(parts[2]), StandardCharsets.UTF_8)
        val farm = FarmPersistenceCodec.decode(farmPayload)
        FarmStateValidator.validateFarm(farm)
        return FarmBackupEnvelope(schemaVersion = version, exportedAt = exportedAt, farm = farm)
    }
}

data class FarmBackupEnvelope(
    val schemaVersion: Int,
    val exportedAt: OffsetDateTime,
    val farm: FarmState
)
