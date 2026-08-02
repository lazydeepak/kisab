package com.susankhya.kisab.persistence

import com.susankhya.kisab.domain.FarmEntry
import com.susankhya.kisab.domain.FarmEntryKind
import com.susankhya.kisab.domain.FarmState
import com.susankhya.kisab.domain.FarmTransaction
import com.susankhya.kisab.domain.TransactionCategory
import com.susankhya.kisab.domain.TransactionType
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

object FarmPersistenceCodec {
    const val CURRENT_SCHEMA_VERSION = 2

    private const val FIELD_SEPARATOR = "\u001F"
    private const val RECORD_SEPARATOR = "\u001E"
    private const val TRANSACTION_FIELD_SEPARATOR = "\u001D"
    private const val LEGACY_FIELD_SEPARATOR = "|"
    private const val LEGACY_ENTRY_SEPARATOR = "::"
    private const val LEGACY_TRANSACTION_SEPARATOR = ";;"

    fun encode(farm: FarmState): String = buildString {
        append(CURRENT_SCHEMA_VERSION)
        append(FIELD_SEPARATOR)
        append(farm.id)
        append(FIELD_SEPARATOR)
        append(farm.name)
        append(FIELD_SEPARATOR)
        append(farm.entries.joinToString(RECORD_SEPARATOR) { entry -> "${entry.kind.name}:${entry.label}:${entry.quantity}" })
        append(FIELD_SEPARATOR)
        append(farm.transactions.joinToString(RECORD_SEPARATOR) { transaction -> listOf(
            transaction.id,
            transaction.type.name,
            transaction.category.name,
            transaction.amountMinor.toString(),
            transaction.currency,
            transaction.description,
            transaction.occurredAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        ).joinToString(TRANSACTION_FIELD_SEPARATOR) })
    }

    fun decode(encoded: String): FarmState = decodeOrNull(encoded)
        ?: throw IllegalArgumentException("Invalid persisted farm data")

    fun decodeOrNull(encoded: String): FarmState? {
        return try {
            val parts = encoded.split(Regex.escape(LEGACY_FIELD_SEPARATOR).toRegex())
            if (parts.size == 4 && !parts[0].startsWith("2")) {
                decodeLegacy(parts)
            } else {
                val fields = encoded.split(FIELD_SEPARATOR)
                require(fields.size >= 5) { "Invalid persisted farm data" }

                val version = fields[0].toIntOrNull() ?: return decodeLegacy(encoded.split(Regex.escape(LEGACY_FIELD_SEPARATOR).toRegex()))
                require(version == CURRENT_SCHEMA_VERSION) { "Unsupported schema version: $version" }

                val entries = fields[3].takeIf { it.isNotBlank() }?.split(RECORD_SEPARATOR)?.filter { it.isNotBlank() }?.map { entry ->
                    val (kind, label, quantity) = entry.split(":", limit = 3)
                    FarmEntry(FarmEntryKind.valueOf(kind), label, quantity.toInt())
                }?.toMutableList() ?: mutableListOf()

                val transactions = fields[4].takeIf { it.isNotBlank() }?.split(RECORD_SEPARATOR)?.filter { it.isNotBlank() }?.map { transaction ->
                    val transactionParts = transaction.split(TRANSACTION_FIELD_SEPARATOR)
                    require(transactionParts.size == 7) { "Invalid transaction payload" }
                    val id = transactionParts[0]
                    val type = transactionParts[1]
                    val category = transactionParts[2]
                    val amountMinor = transactionParts[3]
                    val currency = transactionParts[4]
                    val description = transactionParts[5]
                    val occurredAt = transactionParts[6]
                    val parsedTransaction = FarmTransaction(
                        id = id,
                        type = TransactionType.valueOf(type),
                        category = TransactionCategory.valueOf(category),
                        amountMinor = amountMinor.toLong(),
                        currency = currency.trim().uppercase(),
                        description = description,
                        occurredAt = OffsetDateTime.parse(occurredAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    )
                    require(parsedTransaction.id.isNotBlank()) { "Invalid transaction payload" }
                    require(parsedTransaction.description.isNotBlank()) { "Invalid transaction payload" }
                    require(parsedTransaction.amountMinor > 0) { "Invalid transaction payload" }
                    require(parsedTransaction.currency.matches(Regex("^[A-Z]{3}$"))) { "Invalid transaction payload" }
                    require(parsedTransaction.category.type == parsedTransaction.type) { "Invalid transaction payload" }
                    parsedTransaction
                }?.toMutableList() ?: mutableListOf()

                FarmState(id = fields[1], name = fields[2], entries = entries, transactions = transactions, schemaVersion = CURRENT_SCHEMA_VERSION)
            }
        } catch (exception: IllegalArgumentException) {
            null
        }
    }

    private fun decodeLegacy(parts: List<String>): FarmState? {
        return try {
            require(parts.size == 4) { "Invalid persisted farm data" }

            val entries = parts[2].takeIf { it.isNotBlank() }?.split(LEGACY_ENTRY_SEPARATOR)?.filter { it.isNotBlank() }?.map { entry ->
                val (kind, label, quantity) = entry.split(":", limit = 3)
                FarmEntry(FarmEntryKind.valueOf(kind), label, quantity.toInt())
            }?.toMutableList() ?: mutableListOf()

            val transactions = parts[3].takeIf { it.isNotBlank() }?.split(LEGACY_TRANSACTION_SEPARATOR)?.filter { it.isNotBlank() }?.mapIndexed { index, transaction ->
                val (description, amount) = transaction.split(":", limit = 2)
                val signedAmount = amount.toLong()
                val type = if (signedAmount < 0) TransactionType.INCOME else TransactionType.EXPENSE
                val category = if (type == TransactionType.INCOME) TransactionCategory.OTHER_INCOME else TransactionCategory.OTHER_EXPENSE
                FarmTransaction(
                    id = "tx-migrated-$index",
                    type = type,
                    category = category,
                    amountMinor = kotlin.math.abs(signedAmount),
                    currency = "USD",
                    description = description,
                    occurredAt = OffsetDateTime.parse("2024-01-01T00:00:00Z", DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                )
            }?.toMutableList() ?: mutableListOf()

            FarmState(id = parts[0], name = parts[1], entries = entries, transactions = transactions, schemaVersion = CURRENT_SCHEMA_VERSION)
        } catch (exception: IllegalArgumentException) {
            null
        }
    }
}
