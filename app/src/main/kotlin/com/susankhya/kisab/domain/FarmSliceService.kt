package com.susankhya.kisab.domain

class FarmSliceService {
    private val farms = linkedMapOf<String, FarmState>()

    fun createFarm(name: String): FarmState {
        require(name.isNotBlank()) { "Farm name is required" }
        val farm = FarmState(id = "farm-${farms.size + 1}", name = name)
        farms[farm.id] = farm
        return farm
    }

    fun addEntry(farmId: String, entry: FarmEntry) {
        val farm = getFarm(farmId)
        farm.entries.add(entry)
    }

    fun recordTransaction(farmId: String, transaction: FarmTransaction) {
        val farm = getFarm(farmId)
        farm.transactions.add(transaction)
    }

    fun summary(farmId: String): FarmSummary {
        val farm = getFarm(farmId)
        val balance = farm.transactions.sumOf { it.amount }
        return FarmSummary(
            farmId = farm.id,
            farmName = farm.name,
            entryCount = farm.entries.size,
            transactionCount = farm.transactions.size,
            balance = balance
        )
    }

    private fun getFarm(farmId: String): FarmState =
        farms[farmId] ?: throw IllegalArgumentException("Unknown farm: $farmId")
}

data class FarmState(
    val id: String,
    val name: String,
    val entries: MutableList<FarmEntry> = mutableListOf(),
    val transactions: MutableList<FarmTransaction> = mutableListOf()
)

data class FarmEntry(
    val kind: FarmEntryKind,
    val label: String,
    val quantity: Int
)

enum class FarmEntryKind {
    LIVESTOCK,
    CROP
}

data class FarmTransaction(
    val description: String,
    val amount: Int
)

data class FarmSummary(
    val farmId: String,
    val farmName: String,
    val entryCount: Int,
    val transactionCount: Int,
    val balance: Int
)
