package com.susankhya.kisab.persistence

import com.susankhya.kisab.domain.FarmEntry
import com.susankhya.kisab.domain.FarmEntryKind
import com.susankhya.kisab.domain.FarmState
import com.susankhya.kisab.domain.FarmTransaction
import com.susankhya.kisab.domain.TransactionCategory
import com.susankhya.kisab.domain.TransactionType
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
 * Versioned, delimited persistence codec for a single farm.
 *
 * Schema 2 stored a currency code on every transaction record. Schema 3 moves
 * currency ownership to the farm level ([FarmState.currencyCode]) and drops the
 * per-transaction currency field. Schema-2 and legacy payloads still decode and
 * upgrade to schema 3; the backup envelope ([FarmBackupCodec]) is unchanged
 * because it wraps this versioned payload.
 */
object FarmPersistenceCodec {
    const val CURRENT_SCHEMA_VERSION = 3

    private const val FIELD_SEPARATOR = "\u001F"
    private const val RECORD_SEPARATOR = "\u001E"
    private const val TRANSACTION_FIELD_SEPARATOR = "\u001D"
    private const val LEGACY_FIELD_SEPARATOR = "|"
    private const val LEGACY_ENTRY_SEPARATOR = "::"
    private const val LEGACY_TRANSACTION_SEPARATOR = ";;"

    private const val LEGACY_CURRENCY_CODE = "USD"
    private const val EMPTY_FARM_CURRENCY_CODE = "NPR"

    fun encode(farm: FarmState): String = buildString {
        append(CURRENT_SCHEMA_VERSION)
        append(FIELD_SEPARATOR)
        append(farm.id)
        append(FIELD_SEPARATOR)
        append(farm.name)
        append(FIELD_SEPARATOR)
        append(farm.entries.joinToString(RECORD_SEPARATOR) { entry -> "${entry.kind.name}:${entry.label}:${entry.quantity}" })
        append(FIELD_SEPARATOR)
        append(farm.currencyCode)
        append(FIELD_SEPARATOR)
        append(farm.transactions.joinToString(RECORD_SEPARATOR) { transaction -> listOf(
            transaction.id,
            transaction.type.name,
            transaction.category.name,
            transaction.amountMinor.toString(),
            transaction.description,
            transaction.occurredAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        ).joinToString(TRANSACTION_FIELD_SEPARATOR) })
    }

    fun decode(encoded: String): FarmState = decodeOrNull(encoded)
        ?: throw IllegalArgumentException("Invalid persisted farm data")

    fun decodeOrNull(encoded: String): FarmState? {
        return try {
            val legacyParts = encoded.split(Regex.escape(LEGACY_FIELD_SEPARATOR).toRegex())
            if (legacyParts.size == 4 && !legacyParts[0].startsWith("2") && !legacyParts[0].startsWith("3")) {
                decodeLegacy(legacyParts)
            } else {
                val fields = encoded.split(FIELD_SEPARATOR)
                require(fields.size >= 5) { "Invalid persisted farm data" }

                val version = fields[0].toIntOrNull() ?: return decodeLegacy(legacyParts)
                require(version <= CURRENT_SCHEMA_VERSION) { "Unsupported schema version: $version" }

                when (version) {
                    2 -> decodeSchema2(fields)
                    3 -> decodeSchema3(fields)
                    else -> null
                }
            }
        } catch (exception: RuntimeException) {
            null
        }
    }

    private fun decodeEntries(encoded: String): MutableList<FarmEntry> =
        encoded.takeIf { it.isNotBlank() }?.split(RECORD_SEPARATOR)?.filter { it.isNotBlank() }?.map { entry ->
            val (kind, label, quantity) = entry.split(":", limit = 3)
            FarmEntry(FarmEntryKind.valueOf(kind), label, quantity.toInt())
        }?.toMutableList() ?: mutableListOf()

    private fun decodeSchema3(fields: List<String>): FarmState {
        require(fields.size >= 6) { "Invalid persisted farm data" }
        val currencyCode = fields[4].ifBlank { EMPTY_FARM_CURRENCY_CODE }
        return FarmState(
            id = fields[1],
            name = fields[2],
            currencyCode = currencyCode,
            entries = decodeEntries(fields[3]),
            transactions = decodeSchema3Transactions(fields[5]),
            schemaVersion = CURRENT_SCHEMA_VERSION
        )
    }

    private fun decodeSchema3Transactions(encoded: String): MutableList<FarmTransaction> =
        encoded.takeIf { it.isNotBlank() }?.split(RECORD_SEPARATOR)?.filter { it.isNotBlank() }?.map { transaction ->
            val parts = transaction.split(TRANSACTION_FIELD_SEPARATOR)
            require(parts.size == 6) { "Invalid transaction payload" }
            parseTransaction(
                id = parts[0],
                type = parts[1],
                category = parts[2],
                amountMinor = parts[3],
                description = parts[4],
                occurredAt = parts[5]
            )
        }?.toMutableList() ?: mutableListOf()

    private fun decodeSchema2(fields: List<String>): FarmState {
        require(fields.size >= 5) { "schema 2 persisted farm data" }
        val transactionsEncoded = fields[4]
        var currencyCode: String? = null
        val transactions = transactionsEncoded.takeIf { it.isNotBlank() }?.split(RECORD_SEPARATOR)?.filter { it.isNotBlank() }?.map { transaction ->
            val parts = transaction.split(TRANSACTION_FIELD_SEPARATOR)
            require(parts.size == 7) { "Invalid transaction payload" }
            if (currencyCode == null) {
                currencyCode = parts[4].trim().uppercase()
            }
            parseTransaction(
                id = parts[0],
                type = parts[1],
                category = parts[2],
                amountMinor = parts[3],
                description = parts[5],
                occurredAt = parts[6]
            )
        }?.toMutableList() ?: mutableListOf()
        return FarmState(
            id = fields[1],
            name = fields[2],
            currencyCode = currencyCode ?: EMPTY_FARM_CURRENCY_CODE,
            entries = decodeEntries(fields[3]),
            transactions = transactions,
            schemaVersion = CURRENT_SCHEMA_VERSION
        )
    }

    private fun parseTransaction(
        id: String,
        type: String,
        category: String,
        amountMinor: String,
        description: String,
        occurredAt: String
    ): FarmTransaction {
        val parsed = FarmTransaction(
            id = id,
            type = TransactionType.valueOf(type),
            category = TransactionCategory.valueOf(category),
            amountMinor = amountMinor.toLong(),
            description = description,
            occurredAt = OffsetDateTime.parse(occurredAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        )
        require(parsed.id.isNotBlank()) { "Invalid transaction payload" }
        require(parsed.description.isNotBlank()) { "Invalid transaction payload" }
        require(parsed.amountMinor > 0) { "Invalid transaction payload" }
        require(parsed.category.type == parsed.type) { "Invalid transaction payload" }
        return parsed
    }

    private fun decodeLegacy(parts: List<String>): FarmState? {
        return try {
            require(parts.size == 4) { "Invalid persisted farm data" }

            val entries = parts[2].takeIf { it.isNotBlank() }?.split(LEGACY_ENTRY_SEPARATOR)?.filter { it.isNotBlank() }?.map { entry ->
                val (kind, label, quantity) = entry.split(":", limit = 3)
                FarmEntry(FarmEntryKind.valueOf(kind), label, quantity.toInt())
            }?.toMutableList() ?: mutableListOf()

            val legacyTransactions = parts[3].takeIf { it.isNotBlank() }?.split(LEGACY_TRANSACTION_SEPARATOR)?.filter { it.isNotBlank() }?.mapIndexed { index, transaction ->
                val (description, amount) = transaction.split(":", limit = 2)
                val signedAmount = amount.toLong()
                val type = if (signedAmount < 0) TransactionType.INCOME else TransactionType.EXPENSE
                val category = if (type == TransactionType.INCOME) TransactionCategory.OTHER_INCOME else TransactionCategory.OTHER_EXPENSE
                FarmTransaction(
                    id = "tx-migrated-$index",
                    type = type,
                    category = category,
                    amountMinor = kotlin.math.abs(signedAmount),
                    description = description,
                    occurredAt = OffsetDateTime.parse("2024-01-01T00:00:00Z", DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                )
            }?.toMutableList() ?: mutableListOf()

            val currencyCode = if (legacyTransactions.isEmpty()) EMPTY_FARM_CURRENCY_CODE else LEGACY_CURRENCY_CODE
            FarmState(
                id = parts[0],
                name = parts[1],
                currencyCode = currencyCode,
                entries = entries,
                transactions = legacyTransactions,
                schemaVersion = CURRENT_SCHEMA_VERSION
            )
        } catch (exception: IllegalArgumentException) {
            null
        }
    }
}