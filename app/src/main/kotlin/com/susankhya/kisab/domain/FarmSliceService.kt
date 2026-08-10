package com.susankhya.kisab.domain

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

interface FarmStore {
    fun loadFarm(farmId: String): FarmState?
    fun saveFarm(farm: FarmState)
    fun setCurrentFarmId(farmId: String)
    fun currentFarmId(): String?
    fun clear()
}

class InMemoryFarmStore : FarmStore {
    private val farms = linkedMapOf<String, FarmState>()
    private var currentFarmId: String? = null

    override fun loadFarm(farmId: String): FarmState? = farms[farmId]

    override fun saveFarm(farm: FarmState) {
        farms[farm.id] = farm
        currentFarmId = farm.id
    }

    override fun setCurrentFarmId(farmId: String) {
        currentFarmId = farmId
    }

    override fun currentFarmId(): String? = currentFarmId

    override fun clear() {
        farms.clear()
        currentFarmId = null
    }
}

class FarmSliceService(private val store: FarmStore = InMemoryFarmStore()) {
    fun createFarm(name: String, currencyCode: String = FarmState.DEFAULT_CURRENCY_CODE): FarmState {
        require(name.isNotBlank()) { "Farm name is required" }
        require(currencyCode.matches(CURRENCY_CODE_PATTERN)) { "Farm currency must be a 3-letter ISO code" }
        val farm = FarmState(id = "farm-${UUID.randomUUID()}", name = name, currencyCode = currencyCode.uppercase())
        store.saveFarm(farm)
        store.setCurrentFarmId(farm.id)
        return farm
    }

    fun loadFarm(farmId: String): FarmState? = store.loadFarm(farmId)

    fun currentFarmId(): String? = store.currentFarmId()

    fun addEntry(farmId: String, entry: FarmEntry) {
        val farm = getFarm(farmId)
        require(entry.label.isNotBlank()) { "Entry label is required" }
        require(entry.quantity > 0) { "Entry quantity must be positive" }
        val updated = farm.copy(entries = (farm.entries + entry).toMutableList())
        store.saveFarm(updated)
    }

    fun createTransaction(farmId: String, draft: FarmTransactionDraft): FarmTransaction {
        val farm = getFarm(farmId)
        val transaction = draft.toTransaction(newTransactionId())
        FarmStateValidator.validateTransaction(transaction)
        val updated = farm.copy(transactions = (farm.transactions + transaction).toMutableList())
        store.saveFarm(updated)
        return transaction
    }

    fun updateTransaction(farmId: String, transactionId: String, draft: FarmTransactionDraft): FarmTransaction {
        val farm = getFarm(farmId)
        val index = farm.transactions.indexOfFirst { it.id == transactionId }
        require(index >= 0) { "Transaction not found: $transactionId" }
        val transaction = draft.toTransaction(transactionId)
        FarmStateValidator.validateTransaction(transaction)
        val updatedTransactions = farm.transactions.toMutableList()
        updatedTransactions[index] = transaction
        val updated = farm.copy(transactions = updatedTransactions)
        store.saveFarm(updated)
        return transaction
    }

    fun deleteTransaction(farmId: String, transactionId: String) {
        val farm = getFarm(farmId)
        val updatedTransactions = farm.transactions.filterNot { it.id == transactionId }
        require(updatedTransactions.size < farm.transactions.size) { "Transaction not found: $transactionId" }
        val updated = farm.copy(transactions = updatedTransactions.toMutableList())
        store.saveFarm(updated)
    }

    fun setFarmCurrency(farmId: String, currencyCode: String) {
        val farm = getFarm(farmId)
        require(farm.transactions.isEmpty()) { "Farm currency cannot change after transactions are recorded" }
        require(currencyCode.matches(CURRENCY_CODE_PATTERN)) { "Farm currency must be a 3-letter ISO code" }
        store.saveFarm(farm.copy(currencyCode = currencyCode.trim().uppercase()))
    }

    fun transactionsNewestFirst(farmId: String): List<FarmTransaction> =
        getFarm(farmId).transactionsNewestFirst()

    fun summary(farmId: String): FarmSummary {
        val farm = getFarm(farmId)
        FarmStateValidator.validateFarm(farm)
        var balanceMinor = 0L
        for (transaction in farm.transactions) {
            val signedAmount = if (transaction.type == TransactionType.INCOME) {
                transaction.amountMinor
            } else {
                -transaction.amountMinor
            }
            balanceMinor = Math.addExact(balanceMinor, signedAmount)
        }
        return FarmSummary(
            farmId = farm.id,
            farmName = farm.name,
            entryCount = farm.entries.size,
            transactionCount = farm.transactions.size,
            balanceMinor = balanceMinor,
            currencyCode = farm.currencyCode
        )
    }

    private fun newTransactionId(): String = "tx-${UUID.randomUUID()}"

    private fun getFarm(farmId: String): FarmState =
        store.loadFarm(farmId) ?: throw IllegalArgumentException("Unknown farm: $farmId")

    companion object {
        private val CURRENCY_CODE_PATTERN = Regex("^[A-Z]{3}$")
    }

}

data class FarmState(
    val id: String,
    val name: String,
    val currencyCode: String = DEFAULT_CURRENCY_CODE,
    val entries: MutableList<FarmEntry> = mutableListOf(),
    val transactions: MutableList<FarmTransaction> = mutableListOf(),
    val schemaVersion: Int = CURRENT_FARM_SCHEMA_VERSION
) {
    companion object {
        const val DEFAULT_CURRENCY_CODE = "NPR"
        const val CURRENT_FARM_SCHEMA_VERSION = 3
    }
}

fun FarmState.transactionsNewestFirst(): List<FarmTransaction> =
    transactions.withIndex()
        .sortedWith(
            compareByDescending<IndexedValue<FarmTransaction>> { it.value.occurredAt }
                .thenByDescending { it.index }
        )
        .map { it.value }

data class FarmEntry(
    val kind: FarmEntryKind,
    val label: String,
    val quantity: Int
)

enum class FarmEntryKind {
    LIVESTOCK,
    CROP
}

enum class TransactionType {
    INCOME,
    EXPENSE
}

enum class TransactionCategory(val type: TransactionType) {
    SALES(TransactionType.INCOME),
    SERVICES(TransactionType.INCOME),
    OTHER_INCOME(TransactionType.INCOME),
    FEED(TransactionType.EXPENSE),
    SUPPLIES(TransactionType.EXPENSE),
    LABOR(TransactionType.EXPENSE),
    TRANSPORT(TransactionType.EXPENSE),
    OTHER_EXPENSE(TransactionType.EXPENSE)
}

data class FarmTransactionDraft(
    val type: TransactionType,
    val category: TransactionCategory,
    val amountMinor: Long,
    val description: String,
    val occurredAt: String
) {
    fun toTransaction(id: String): FarmTransaction = try {
        FarmTransaction(
            id = id,
            type = type,
            category = category,
            amountMinor = amountMinor,
            description = description.trim(),
            occurredAt = OffsetDateTime.parse(occurredAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .withOffsetSameInstant(ZoneOffset.UTC)
        )
    } catch (exception: RuntimeException) {
        throw IllegalArgumentException("Transaction date/time must be a valid ISO-8601 value", exception)
    }
}

data class FarmTransaction(
    val id: String,
    val type: TransactionType,
    val category: TransactionCategory,
    val amountMinor: Long,
    val description: String,
    val occurredAt: OffsetDateTime
)

data class FarmSummary(
    val farmId: String,
    val farmName: String,
    val entryCount: Int,
    val transactionCount: Int,
    val balanceMinor: Long,
    val currencyCode: String?
)
