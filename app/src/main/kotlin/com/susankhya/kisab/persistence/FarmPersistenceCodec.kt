package com.susankhya.kisab.persistence

import com.susankhya.kisab.domain.FarmEntry
import com.susankhya.kisab.domain.FarmEntryKind
import com.susankhya.kisab.domain.FarmActivityType
import com.susankhya.kisab.domain.FarmState
import com.susankhya.kisab.domain.FarmTransaction
import com.susankhya.kisab.domain.FarmProduct
import com.susankhya.kisab.domain.Party
import com.susankhya.kisab.domain.PartyRole
import com.susankhya.kisab.domain.ProductSaleDetail
import com.susankhya.kisab.domain.ProductUnit
import com.susankhya.kisab.domain.FarmSupply
import com.susankhya.kisab.domain.SupplyPurchaseDetail
import com.susankhya.kisab.domain.SupplyUsage
import com.susankhya.kisab.domain.ProductionRecord
import com.susankhya.kisab.domain.ProductionSession
import com.susankhya.kisab.domain.ProductionAllocation
import com.susankhya.kisab.domain.ProductionAllocationType
import com.susankhya.kisab.domain.Settlement
import com.susankhya.kisab.domain.Trade
import com.susankhya.kisab.domain.TradeType
import com.susankhya.kisab.domain.TransactionCategory
import com.susankhya.kisab.domain.TransactionType
import java.nio.charset.StandardCharsets
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Versioned, delimited persistence codec for a single farm.
 *
 * Schema 2 stored a currency code on every transaction record. Schema 3 moves
 * currency ownership to the farm level ([FarmState.currencyCode]) and drops the
 * per-transaction currency field. Schema 4 appends the farm's [Party] list,
 * schema 5 its [Trade] list (with a persisted `paidMinor` aggregate). Schema 6
 * makes payments first-class [Settlement] records: the trade row drops
 * `paidMinor` (payment becomes derived state) and a settlements list is
 * appended. Schema 7 appends farm-local products and product-sale details.
 *
 * Schema 13 appends the farm's activity configuration ([FarmState.activities]
 * and [FarmState.disabledActivities]) and adds an optional activity association
 * to each transaction record. Transaction records stay backward-compatible:
 * the activity is a trailing 7th field, so schema-13 decode accepts both 6-part
 * (pre-M10) and 7-part records, and pre-M10 payloads decode with a `null`
 * activity (a general/farm-wide transaction) and an empty activity set.
 *
 * Migration v5 -> v6 preserves the historical `paidMinor` by recording **one
 * deterministic opening settlement** per v5 trade that had `paidMinor > 0`,
 * dated at the trade's own [Trade.occurredAt] (the exact historical payment
 * instant is unavailable; using the trade timestamp is an intentional,
 * deterministic approximation — never "now"). The opening settlement id is
 * derived via [UUID.nameUUIDFromBytes] so the same v5 payload always decodes
 * to the same state. A re-encoded schema-v6 payload carries native settlement
 * rows and is decoded directly, never re-migrated.
 *
 * Backup envelopes ([FarmBackupCodec]) are unchanged: they wrap this versioned
 * payload, so settlements and older schemas flow through existing backups.
 */
object FarmPersistenceCodec {
    const val CURRENT_SCHEMA_VERSION = 13

    private const val FIELD_SEPARATOR = "\u001F"
    private const val RECORD_SEPARATOR = "\u001E"
    private const val TRANSACTION_FIELD_SEPARATOR = "\u001D"
    private const val LEGACY_FIELD_SEPARATOR = "|"
    private const val LEGACY_ENTRY_SEPARATOR = "::"
    private const val LEGACY_TRANSACTION_SEPARATOR = ";;"

    private const val LEGACY_CURRENCY_CODE = "USD"
    private const val EMPTY_FARM_CURRENCY_CODE = "NPR"

    /** Namespace for deterministic v5->v6 opening-settlement ids. */
    private const val V5_OPENING_SETTLEMENT_NAMESPACE = "kisab:v5-opening-settlement:"

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
            transaction.occurredAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            transaction.activity?.name.orEmpty()
        ).joinToString(TRANSACTION_FIELD_SEPARATOR) })
        append(FIELD_SEPARATOR)
        append(farm.parties.joinToString(RECORD_SEPARATOR) { party -> listOf(
            party.id,
            party.role.name,
            party.name,
            party.contact,
            party.notes
        ).joinToString(TRANSACTION_FIELD_SEPARATOR) })
        append(FIELD_SEPARATOR)
        append(farm.trades.joinToString(RECORD_SEPARATOR) { trade -> listOf(
            trade.id,
            trade.type.name,
            trade.partyId.orEmpty(),
            trade.totalMinor.toString(),
            trade.description,
            trade.occurredAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        ).joinToString(TRANSACTION_FIELD_SEPARATOR) })
        append(FIELD_SEPARATOR)
        append(farm.settlements.joinToString(RECORD_SEPARATOR) { settlement -> listOf(
            settlement.id,
            settlement.tradeId,
            settlement.amountMinor.toString(),
            settlement.note,
            settlement.occurredAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            settlement.isInitialPayment.toString()
        ).joinToString(TRANSACTION_FIELD_SEPARATOR) })
        append(FIELD_SEPARATOR)
        append(farm.products.joinToString(RECORD_SEPARATOR) { product -> listOf(
            product.id,
            product.name,
            product.defaultUnit.name,
            product.customUnitLabel
        ).joinToString(TRANSACTION_FIELD_SEPARATOR) })
        append(FIELD_SEPARATOR)
        append(farm.productSaleDetails.joinToString(RECORD_SEPARATOR) { detail -> listOf(
            detail.tradeId,
            detail.productId,
            detail.quantity.toPlainString(),
            detail.unit.name,
            detail.customUnitLabel,
            detail.rateMinor.toString()
        ).joinToString(TRANSACTION_FIELD_SEPARATOR) })
        append(FIELD_SEPARATOR)
        append(farm.supplies.joinToString(RECORD_SEPARATOR) { supply -> listOf(
            supply.id,
            supply.name,
            supply.unit.name,
            supply.customUnitLabel
        ).joinToString(TRANSACTION_FIELD_SEPARATOR) })
        append(FIELD_SEPARATOR)
        append(farm.supplyPurchaseDetails.joinToString(RECORD_SEPARATOR) { detail -> listOf(
            detail.transactionId.orEmpty(),
            detail.supplyId,
            detail.quantity.toPlainString(),
            detail.unit.name,
            detail.customUnitLabel,
            detail.purchaseTradeId.orEmpty()
        ).joinToString(TRANSACTION_FIELD_SEPARATOR) })
        append(FIELD_SEPARATOR)
        append(farm.supplyUsages.joinToString(RECORD_SEPARATOR) { usage -> listOf(
            usage.id,
            usage.supplyId,
            usage.quantity.toPlainString(),
            usage.unit.name,
            usage.occurredAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            usage.note
        ).joinToString(TRANSACTION_FIELD_SEPARATOR) })
        append(FIELD_SEPARATOR)
        append(farm.productionRecords.joinToString(RECORD_SEPARATOR) { record -> listOf(
            record.id,
            record.productId,
            record.quantity.toPlainString(),
            record.unit.name,
            record.occurredAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            record.session.name,
            record.note
        ).joinToString(TRANSACTION_FIELD_SEPARATOR) })
        append(FIELD_SEPARATOR)
        append(farm.productionAllocations.joinToString(RECORD_SEPARATOR) { allocation -> listOf(
            allocation.id,
            allocation.productId,
            allocation.quantity.toPlainString(),
            allocation.unit.name,
            allocation.occurredAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            allocation.type.name,
            allocation.note
        ).joinToString(TRANSACTION_FIELD_SEPARATOR) })
        append(FIELD_SEPARATOR)
        append(farm.activities.joinToString(RECORD_SEPARATOR) { it.name })
        append(FIELD_SEPARATOR)
        append(farm.disabledActivities.joinToString(RECORD_SEPARATOR) { it.name })
    }

    fun decode(encoded: String): FarmState = decodeOrNull(encoded)
        ?: throw IllegalArgumentException("Invalid persisted farm data")

    fun decodeOrNull(encoded: String): FarmState? {
        return try {
            val legacyParts = encoded.split(Regex.escape(LEGACY_FIELD_SEPARATOR).toRegex())
            if (legacyParts.size == 4 && !legacyParts[0].startsWith("2") && !legacyParts[0].startsWith("3") && !legacyParts[0].startsWith("4") && !legacyParts[0].startsWith("5") && !legacyParts[0].startsWith("6") && !legacyParts[0].startsWith("7") && !legacyParts[0].startsWith("8") && !legacyParts[0].startsWith("9") && !legacyParts[0].startsWith("10") && !legacyParts[0].startsWith("11") && !legacyParts[0].startsWith("12") && !legacyParts[0].startsWith("13")) {
                decodeLegacy(legacyParts)
            } else {
                val fields = encoded.split(FIELD_SEPARATOR)
                require(fields.size >= 5) { "Invalid persisted farm data" }

                val version = fields[0].toIntOrNull() ?: return decodeLegacy(legacyParts)
                require(version <= CURRENT_SCHEMA_VERSION) { "Unsupported schema version: $version" }

                when (version) {
                    2 -> decodeSchema2(fields)
                    3 -> decodeSchema3(fields)
                    4 -> decodeSchema4(fields)
                    5 -> decodeSchema5X(fields)
                    6 -> decodeSchema6(fields)
                    7 -> decodeSchema7(fields)
                    8 -> decodeSchema8(fields)
                    9 -> decodeSchema9(fields)
                    10 -> decodeSchema10(fields)
                    11 -> decodeSchema11(fields)
                    12 -> decodeSchema12(fields)
                    13 -> decodeSchema13(fields)
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

    private fun decodeParties(encoded: String): MutableList<Party> =
        encoded.takeIf { it.isNotBlank() }?.split(RECORD_SEPARATOR)?.filter { it.isNotBlank() }?.map { party ->
            val parts = party.split(TRANSACTION_FIELD_SEPARATOR)
            require(parts.size == 5) { "Invalid party payload" }
            Party(
                id = parts[0],
                role = PartyRole.valueOf(parts[1]),
                name = parts[2],
                contact = parts[3],
                notes = parts[4]
            )
        }?.toMutableList() ?: mutableListOf()

    private fun decodeSchema4(fields: List<String>): FarmState {
        require(fields.size >= 7) { "Invalid persisted farm data" }
        val currencyCode = fields[4].ifBlank { EMPTY_FARM_CURRENCY_CODE }
        return FarmState(
            id = fields[1],
            name = fields[2],
            currencyCode = currencyCode,
            entries = decodeEntries(fields[3]),
            transactions = decodeSchema3Transactions(fields[5]),
            parties = decodeParties(fields[6]),
            schemaVersion = CURRENT_SCHEMA_VERSION
        )
    }

    private fun decodeSchema5X(fields: List<String>): FarmState {
        require(fields.size >= 8) { "Invalid persisted farm data" }
        val currencyCode = fields[4].ifBlank { EMPTY_FARM_CURRENCY_CODE }
        val schema5Trades = decodeSchema5Trades(fields[7])
        return FarmState(
            id = fields[1],
            name = fields[2],
            currencyCode = currencyCode,
            entries = decodeEntries(fields[3]),
            transactions = decodeSchema3Transactions(fields[5]),
            parties = decodeParties(fields[6]),
            trades = schema5Trades.map { it.trade }.toMutableList(),
            settlements = migrateSchema5Settlements(schema5Trades).toMutableList(),
            schemaVersion = CURRENT_SCHEMA_VERSION
        )
    }

    private fun decodeSchema6(fields: List<String>): FarmState {
        require(fields.size >= 9) { "Invalid persisted farm data" }
        val currencyCode = fields[4].ifBlank { EMPTY_FARM_CURRENCY_CODE }
        return FarmState(
            id = fields[1],
            name = fields[2],
            currencyCode = currencyCode,
            entries = decodeEntries(fields[3]),
            transactions = decodeSchema3Transactions(fields[5]),
            parties = decodeParties(fields[6]),
            trades = decodeSchema6Trades(fields[7]),
            settlements = decodeSettlements(fields[8]),
            schemaVersion = CURRENT_SCHEMA_VERSION
        )
    }

    private fun decodeSchema7(fields: List<String>): FarmState {
        require(fields.size >= 11) { "Invalid persisted farm data" }
        val currencyCode = fields[4].ifBlank { EMPTY_FARM_CURRENCY_CODE }
        return FarmState(
            id = fields[1],
            name = fields[2],
            currencyCode = currencyCode,
            entries = decodeEntries(fields[3]),
            transactions = decodeSchema3Transactions(fields[5]),
            parties = decodeParties(fields[6]),
            trades = decodeSchema6Trades(fields[7]),
            settlements = decodeSettlements(fields[8]),
            products = decodeProducts(fields[9]),
            productSaleDetails = decodeProductSaleDetails(fields[10]),
            schemaVersion = CURRENT_SCHEMA_VERSION
        )
    }

    private fun decodeSchema8(fields: List<String>): FarmState {
        require(fields.size >= 14) { "Invalid persisted farm data" }
        val base = decodeSchema7(fields.take(11))
        return base.copy(
            supplies = decodeSupplies(fields[11]),
            supplyPurchaseDetails = decodeSupplyPurchaseDetails(fields[12]),
            supplyUsages = decodeSupplyUsages(fields[13]),
            schemaVersion = CURRENT_SCHEMA_VERSION
        )
    }

    private fun decodeSchema9(fields: List<String>): FarmState =
        decodeSchema8(fields.take(14)).copy(
            productionRecords = decodeProductionRecords(fields[14]),
            schemaVersion = CURRENT_SCHEMA_VERSION
        )

    private fun decodeSchema10(fields: List<String>): FarmState =
        decodeSchema9(fields.take(15)).copy(
            productionAllocations = decodeProductionAllocations(fields[15]),
            schemaVersion = CURRENT_SCHEMA_VERSION
        )

    private fun decodeSchema11(fields: List<String>): FarmState {
        val baseFields = fields.take(15).toMutableList()
        baseFields[8] = fields[8].takeIf { it.isNotBlank() }?.split(RECORD_SEPARATOR)?.joinToString(RECORD_SEPARATOR) { value ->
            value.split(TRANSACTION_FIELD_SEPARATOR).take(5).joinToString(TRANSACTION_FIELD_SEPARATOR)
        }.orEmpty()
        return decodeSchema9(baseFields).copy(
            settlements = decodeSettlements(fields[8], hasInitialPaymentMarker = true),
            productionAllocations = decodeProductionAllocations(fields[15]),
            schemaVersion = CURRENT_SCHEMA_VERSION
        )
    }

    private fun decodeSchema12(fields: List<String>): FarmState {
        val baseFields = fields.take(16).toMutableList()
        // The back-compat schema-11 decode below would truncate schema-12 supply
        // purchase records to 5 parts and drop purchaseTradeId, producing details
        // with no source. Supply purchase details are fully re-decoded from the
        // original 6-part payload below, so keep them out of the intermediate pass.
        baseFields[12] = ""
        return decodeSchema11(baseFields).copy(
            supplyPurchaseDetails = decodeSupplyPurchaseDetails(fields[12], hasPurchaseTradeLink = true),
            schemaVersion = CURRENT_SCHEMA_VERSION
        )
    }

    private fun decodeSchema13(fields: List<String>): FarmState {
        require(fields.size >= 18) { "Invalid persisted farm data" }
        val baseFields = fields.take(16).toMutableList()
        // Schema-13 transactions carry an optional trailing activity field; the
        // back-compat decode only accepts the legacy 6-part form, so transactions
        // are fully re-decoded below and kept out of the intermediate pass.
        baseFields[5] = ""
        return decodeSchema12(baseFields).copy(
            transactions = decodeSchema13Transactions(fields[5]),
            activities = decodeActivities(fields[16]),
            disabledActivities = decodeActivities(fields[17]),
            schemaVersion = CURRENT_SCHEMA_VERSION
        )
    }

    private fun decodeActivities(encoded: String): MutableList<FarmActivityType> =
        encoded.takeIf { it.isNotBlank() }?.split(RECORD_SEPARATOR)?.filter { it.isNotBlank() }?.map { value ->
            FarmActivityType.valueOf(value)
        }?.toMutableList() ?: mutableListOf()

    private fun decodeProductionAllocations(encoded: String): MutableList<ProductionAllocation> =
        encoded.takeIf { it.isNotBlank() }?.split(RECORD_SEPARATOR)?.filter { it.isNotBlank() }?.map { value ->
            val parts = value.split(TRANSACTION_FIELD_SEPARATOR)
            require(parts.size == 7) { "Invalid production allocation payload" }
            ProductionAllocation(
                id = parts[0],
                productId = parts[1],
                quantity = BigDecimal(parts[2]),
                unit = ProductUnit.valueOf(parts[3]),
                occurredAt = OffsetDateTime.parse(parts[4], DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                type = ProductionAllocationType.valueOf(parts[5]),
                note = parts[6]
            )
        }?.toMutableList() ?: mutableListOf()

    private fun decodeProductionRecords(encoded: String): MutableList<ProductionRecord> =
        encoded.takeIf { it.isNotBlank() }?.split(RECORD_SEPARATOR)?.filter { it.isNotBlank() }?.map { value ->
            val parts = value.split(TRANSACTION_FIELD_SEPARATOR)
            require(parts.size == 7) { "Invalid production payload" }
            ProductionRecord(
                id = parts[0],
                productId = parts[1],
                quantity = BigDecimal(parts[2]),
                unit = ProductUnit.valueOf(parts[3]),
                occurredAt = OffsetDateTime.parse(parts[4], DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                session = ProductionSession.valueOf(parts[5]),
                note = parts[6]
            )
        }?.toMutableList() ?: mutableListOf()

    private fun decodeSupplies(encoded: String): MutableList<FarmSupply> =
        encoded.takeIf { it.isNotBlank() }?.split(RECORD_SEPARATOR)?.filter { it.isNotBlank() }?.map { value ->
            val parts = value.split(TRANSACTION_FIELD_SEPARATOR)
            require(parts.size == 4) { "Invalid supply payload" }
            FarmSupply(parts[0], parts[1], ProductUnit.valueOf(parts[2]), parts[3])
        }?.toMutableList() ?: mutableListOf()

    private fun decodeSupplyPurchaseDetails(encoded: String, hasPurchaseTradeLink: Boolean = false): MutableList<SupplyPurchaseDetail> =
        encoded.takeIf { it.isNotBlank() }?.split(RECORD_SEPARATOR)?.filter { it.isNotBlank() }?.map { value ->
            val parts = value.split(TRANSACTION_FIELD_SEPARATOR)
            require(parts.size == if (hasPurchaseTradeLink) 6 else 5) { "Invalid supply purchase payload" }
            SupplyPurchaseDetail(
                parts[0].takeIf { it.isNotBlank() }, parts[1], BigDecimal(parts[2]), ProductUnit.valueOf(parts[3]), parts[4],
                if (hasPurchaseTradeLink) parts[5].takeIf { it.isNotBlank() } else null
            )
        }?.toMutableList() ?: mutableListOf()

    private fun decodeSupplyUsages(encoded: String): MutableList<SupplyUsage> =
        encoded.takeIf { it.isNotBlank() }?.split(RECORD_SEPARATOR)?.filter { it.isNotBlank() }?.map { value ->
            val parts = value.split(TRANSACTION_FIELD_SEPARATOR)
            require(parts.size == 6) { "Invalid supply usage payload" }
            SupplyUsage(
                id = parts[0],
                supplyId = parts[1],
                quantity = BigDecimal(parts[2]),
                unit = ProductUnit.valueOf(parts[3]),
                occurredAt = java.time.OffsetDateTime.parse(parts[4], DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                note = parts[5]
            )
        }?.toMutableList() ?: mutableListOf()

    private fun decodeProducts(encoded: String): MutableList<FarmProduct> =
        encoded.takeIf { it.isNotBlank() }?.split(RECORD_SEPARATOR)?.filter { it.isNotBlank() }?.map { product ->
            val parts = product.split(TRANSACTION_FIELD_SEPARATOR)
            require(parts.size == 4) { "Invalid product payload" }
            FarmProduct(
                id = parts[0],
                name = parts[1],
                defaultUnit = ProductUnit.valueOf(parts[2]),
                customUnitLabel = parts[3]
            )
        }?.toMutableList() ?: mutableListOf()

    private fun decodeProductSaleDetails(encoded: String): MutableList<ProductSaleDetail> =
        encoded.takeIf { it.isNotBlank() }?.split(RECORD_SEPARATOR)?.filter { it.isNotBlank() }?.map { detail ->
            val parts = detail.split(TRANSACTION_FIELD_SEPARATOR)
            require(parts.size == 6) { "Invalid product sale detail payload" }
            ProductSaleDetail(
                tradeId = parts[0],
                productId = parts[1],
                quantity = BigDecimal(parts[2]),
                unit = ProductUnit.valueOf(parts[3]),
                customUnitLabel = parts[4],
                rateMinor = parts[5].toLong()
            )
        }?.toMutableList() ?: mutableListOf()

    /**
     * V5 trades carry a `paidMinor` aggregate field, which M5-03 removes from
     * the trade row. The paid amount is returned so the migration can turn it
     * into an opening settlement.
     */
    private data class Schema5Trade(val trade: Trade, val paidMinor: Long)

    private fun decodeSchema5Trades(encoded: String): List<Schema5Trade> =
        encoded.takeIf { it.isNotBlank() }?.split(RECORD_SEPARATOR)?.filter { it.isNotBlank() }?.map { trade ->
            val parts = trade.split(TRANSACTION_FIELD_SEPARATOR)
            require(parts.size == 7) { "Invalid trade payload" }
            val paidMinor = parts[4].toLong()
            val totalMinor = parts[3].toLong()
            require(totalMinor > 0) { "Invalid trade payload" }
            require(paidMinor in 0..totalMinor) { "Invalid trade payload" }
            val parsed = Trade(
                id = parts[0],
                type = TradeType.valueOf(parts[1]),
                partyId = parts[2].takeIf { it.isNotBlank() },
                totalMinor = totalMinor,
                description = parts[5],
                occurredAt = OffsetDateTime.parse(parts[6], DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            )
            require(parsed.id.isNotBlank()) { "Invalid trade payload" }
            Schema5Trade(parsed, paidMinor)
        } ?: emptyList()

    private fun decodeSchema6Trades(encoded: String): MutableList<Trade> =
        encoded.takeIf { it.isNotBlank() }?.split(RECORD_SEPARATOR)?.filter { it.isNotBlank() }?.map { trade ->
            val parts = trade.split(TRANSACTION_FIELD_SEPARATOR)
            require(parts.size == 6) { "Invalid trade payload" }
            val totalMinor = parts[3].toLong()
            require(totalMinor > 0) { "Invalid trade payload" }
            val parsed = Trade(
                id = parts[0],
                type = TradeType.valueOf(parts[1]),
                partyId = parts[2].takeIf { it.isNotBlank() },
                totalMinor = totalMinor,
                description = parts[4],
                occurredAt = OffsetDateTime.parse(parts[5], DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            )
            require(parsed.id.isNotBlank()) { "Invalid trade payload" }
            parsed
        }?.toMutableList() ?: mutableListOf()

    private fun decodeSettlements(encoded: String, hasInitialPaymentMarker: Boolean = false): MutableList<Settlement> =
        encoded.takeIf { it.isNotBlank() }?.split(RECORD_SEPARATOR)?.filter { it.isNotBlank() }?.map { settlement ->
            val parts = settlement.split(TRANSACTION_FIELD_SEPARATOR)
            require(parts.size == if (hasInitialPaymentMarker) 6 else 5) { "Invalid settlement payload" }
            val amountMinor = parts[2].toLong()
            require(amountMinor > 0) { "Invalid settlement payload" }
            val parsed = Settlement(
                id = parts[0],
                tradeId = parts[1],
                amountMinor = amountMinor,
                note = parts[3],
                occurredAt = OffsetDateTime.parse(parts[4], DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                isInitialPayment = hasInitialPaymentMarker && parts[5].toBooleanStrict()
            )
            require(parsed.id.isNotBlank()) { "Invalid settlement payload" }
            parsed
        }?.toMutableList() ?: mutableListOf()

    /** Deterministic opening-settlement id for a v5 trade. Never a random UUID. */
    private fun v5OpeningSettlementId(tradeId: String): String =
        UUID.nameUUIDFromBytes((V5_OPENING_SETTLEMENT_NAMESPACE + tradeId).toByteArray(StandardCharsets.UTF_8)).toString()

    /**
     * Converts v5 paid aggregates into opening settlements. Each v5 trade with
     * `paidMinor > 0` yields exactly one settlement dated at the trade's own
     * [Trade.occurredAt] (a deliberate migration approximation; the true payment
     * date is unknowable and "now" would be non-deterministic).
     */
    private fun migrateSchema5Settlements(trades: List<Schema5Trade>): List<Settlement> =
        trades.filter { it.paidMinor > 0 }.map { schema5Trade ->
            Settlement(
                id = v5OpeningSettlementId(schema5Trade.trade.id),
                tradeId = schema5Trade.trade.id,
                amountMinor = schema5Trade.paidMinor,
                occurredAt = schema5Trade.trade.occurredAt,
                note = ""
            )
        }

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
            require(parts.size == 6 || parts.size == 7) { "Invalid transaction payload" }
            parseTransaction(
                id = parts[0],
                type = parts[1],
                category = parts[2],
                amountMinor = parts[3],
                description = parts[4],
                occurredAt = parts[5],
                activity = parts.getOrNull(6)?.takeIf { it.isNotBlank() }?.let { FarmActivityType.valueOf(it) }
            )
        }?.toMutableList() ?: mutableListOf()

    private fun decodeSchema13Transactions(encoded: String): MutableList<FarmTransaction> =
        decodeSchema3Transactions(encoded)

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
        occurredAt: String,
        activity: FarmActivityType? = null
    ): FarmTransaction {
        val parsed = FarmTransaction(
            id = id,
            type = TransactionType.valueOf(type),
            category = TransactionCategory.valueOf(category),
            amountMinor = amountMinor.toLong(),
            description = description,
            occurredAt = OffsetDateTime.parse(occurredAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            activity = activity
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