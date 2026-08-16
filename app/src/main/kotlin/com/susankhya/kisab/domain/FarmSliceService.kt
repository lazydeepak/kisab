package com.susankhya.kisab.domain

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

interface FarmStore {
    fun loadFarm(farmId: String): FarmState?
    fun saveFarm(farm: FarmState)
    fun setCurrentFarmId(farmId: String)
    fun currentFarmId(): String?
    fun clear()
    fun deleteFarm(farmId: String)

    /** Stable local farm ids in insertion order. Empty when none. */
    fun farmIds(): List<String>
}

class InMemoryFarmStore : FarmStore {
    private val farms = linkedMapOf<String, FarmState>()
    private var currentFarmId: String? = null

    override fun loadFarm(farmId: String): FarmState? = farms[farmId]

    override fun saveFarm(farm: FarmState) {
        farms[farm.id] = farm
    }

    override fun setCurrentFarmId(farmId: String) {
        require(farmId in farms) { "Unknown farm: $farmId" }
        currentFarmId = farmId
    }

    override fun currentFarmId(): String? =
        currentFarmId?.takeIf { it in farms }

    override fun clear() {
        farms.clear()
        currentFarmId = null
    }

    override fun deleteFarm(farmId: String) {
        farms.remove(farmId)
        if (currentFarmId == farmId) currentFarmId = null
    }

    override fun farmIds(): List<String> = farms.keys.toList()
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

    fun setCurrentFarmId(farmId: String) {
        store.setCurrentFarmId(farmId)
    }

    /** Locally persisted farm ids (insertion order). Foundation for Farm Management. */
    fun farmIds(): List<String> = store.farmIds()

    /**
     * Persists [farm] without deleting other farms. Same id replaces that farm only;
     * a new id is added. Makes [farm] the current farm.
     */
    fun importFarm(farm: FarmState): FarmState {
        FarmStateValidator.validateFarm(farm)
        store.saveFarm(farm)
        store.setCurrentFarmId(farm.id)
        return farm
    }

    /**
    * Clears every operational/accounting record the farm owns (entries,
    * transactions, parties, trades, settlements and sale details) while
    * preserving the farm's identity, name, currency, schema version and
    * reusable product catalog. App-local preferences are owned outside
    * [FarmStore] and are untouched.
     */
    fun resetFarmData(farmId: String) {
        val farm = getFarm(farmId)
        val reset = farm.copy(
            entries = mutableListOf(),
            transactions = mutableListOf(),
            parties = mutableListOf(),
            trades = mutableListOf(),
            settlements = mutableListOf(),
            productSaleDetails = mutableListOf(),
            supplyPurchaseDetails = mutableListOf(),
            supplyUsages = mutableListOf(),
            productionRecords = mutableListOf(),
            productionAllocations = mutableListOf()
        )
        FarmStateValidator.validateFarm(reset)
        store.saveFarm(reset)
    }

    fun addProduct(farmId: String, name: String, unit: ProductUnit, customUnitLabel: String = ""): FarmProduct {
        val farm = getFarm(farmId)
        val product = FarmProduct(
            id = "product-${UUID.randomUUID()}",
            name = name.trim(),
            defaultUnit = unit,
            customUnitLabel = customUnitLabel.trim()
        )
        require(farm.products.none { it.name.equals(product.name, ignoreCase = true) }) {
            "A product with this name already exists"
        }
        val updated = farm.copy(products = (farm.products + product).toMutableList())
        FarmStateValidator.validateFarm(updated)
        store.saveFarm(updated)
        return product
    }

    fun products(farmId: String): List<FarmProduct> =
        getFarm(farmId).products.sortedBy { it.name.lowercase() }

    fun product(farmId: String, productId: String): FarmProduct? =
        getFarm(farmId).products.firstOrNull { it.id == productId }

    fun addSupply(farmId: String, name: String, unit: ProductUnit, customUnitLabel: String = ""): FarmSupply {
        val farm = getFarm(farmId)
        val supply = FarmSupply("supply-${UUID.randomUUID()}", name.trim(), unit, customUnitLabel.trim())
        require(farm.supplies.none { it.name.equals(supply.name, ignoreCase = true) }) {
            "A supply with this name already exists"
        }
        val updated = farm.copy(supplies = (farm.supplies + supply).toMutableList())
        FarmStateValidator.validateFarm(updated)
        store.saveFarm(updated)
        return supply
    }

    fun supplies(farmId: String): List<FarmSupply> = getFarm(farmId).supplies.sortedBy { it.name.lowercase() }

    fun supply(farmId: String, supplyId: String): FarmSupply? = getFarm(farmId).supplies.firstOrNull { it.id == supplyId }

    fun supplyAvailable(farmId: String, supplyId: String): BigDecimal = getFarm(farmId).supplyQuantityAvailable(supplyId)

    fun addSupplyPurchase(
        farmId: String,
        supplyId: String,
        quantity: BigDecimal,
        unit: ProductUnit,
        amountMinor: Long,
        category: TransactionCategory,
        occurredAt: String,
        description: String
    ): FarmTransaction {
        require(category.type == TransactionType.EXPENSE) { "Supply purchase must be an expense category" }
        val farm = getFarm(farmId)
        val supply = farm.supplies.firstOrNull { it.id == supplyId }
            ?: throw IllegalArgumentException("Supply not found: $supplyId")
        require(supply.unit == unit) { "Supply unit does not match" }
        val transaction = FarmTransactionDraft(
            type = TransactionType.EXPENSE,
            category = category,
            amountMinor = amountMinor,
            description = description.trim().ifBlank { supply.name },
            occurredAt = occurredAt
        ).toTransaction("tx-${UUID.randomUUID()}")
        val detail = SupplyPurchaseDetail(transaction.id, supply.id, quantity, unit, supply.customUnitLabel)
        val updated = farm.copy(
            transactions = (farm.transactions + transaction).toMutableList(),
            supplyPurchaseDetails = (farm.supplyPurchaseDetails + detail).toMutableList()
        )
        FarmStateValidator.validateFarm(updated)
        store.saveFarm(updated)
        return transaction
    }

    fun addSupplierPurchase(
        farmId: String,
        supplierId: String,
        supplyId: String,
        quantity: BigDecimal,
        unit: ProductUnit,
        amountMinor: Long,
        initialPaymentMinor: Long?,
        occurredAt: String,
        description: String
    ): Trade {
        val farm = getFarm(farmId)
        val supplier = farm.parties.firstOrNull { it.id == supplierId }
            ?: throw IllegalArgumentException("Supplier not found: $supplierId")
        require(supplier.role.compatibleWith(TradeType.PURCHASE)) { "Party is not supplier-compatible" }
        val supply = farm.supplies.firstOrNull { it.id == supplyId }
            ?: throw IllegalArgumentException("Supply not found: $supplyId")
        require(supply.unit == unit) { "Supply unit does not match" }
        require(amountMinor > 0) { "Purchase amount must be positive" }
        if (initialPaymentMinor != null) require(initialPaymentMinor in 0..amountMinor) { "Initial payment is out of range" }
        val trade = TradeDraft(TradeType.PURCHASE, supplierId, amountMinor, description.ifBlank { supply.name }, occurredAt)
            .toTrade("trade-${UUID.randomUUID()}")
        val settlement = initialPaymentMinor?.takeIf { it > 0 }?.let { amount ->
            Settlement("settlement-${UUID.randomUUID()}", trade.id, amount, trade.occurredAt, "", true)
        }
        val detail = SupplyPurchaseDetail(null, supply.id, quantity, unit, supply.customUnitLabel, trade.id)
        val updated = farm.copy(
            trades = (farm.trades + trade).toMutableList(),
            settlements = if (settlement == null) farm.settlements else (farm.settlements + settlement).toMutableList(),
            supplyPurchaseDetails = (farm.supplyPurchaseDetails + detail).toMutableList()
        )
        FarmStateValidator.validateFarm(updated)
        store.saveFarm(updated)
        return trade
    }

    fun recordSupplierPayment(farmId: String, supplierId: String, amountMinor: Long, occurredAt: String): List<Settlement> {
        require(amountMinor > 0) { "Payment amount must be positive" }
        val farm = getFarm(farmId)
        val supplier = farm.parties.firstOrNull { it.id == supplierId }
            ?: throw IllegalArgumentException("Supplier not found: $supplierId")
        require(supplier.role.compatibleWith(TradeType.PURCHASE)) { "Party is not supplier-compatible" }
        val eligible = farm.trades.filter { it.partyId == supplierId && it.type == TradeType.PURCHASE }
            .filter { farm.settlements.outstandingMinorFor(it) > 0 }
            .sortedWith(compareBy<Trade> { it.occurredAt }.thenBy { it.id })
        val outstanding = eligible.fold(0L) { total, trade -> Math.addExact(total, farm.settlements.outstandingMinorFor(trade)) }
        require(outstanding > 0) { "No outstanding supplier balance" }
        require(amountMinor <= outstanding) { "Payment exceeds supplier balance" }
        val time = SettlementDraft(eligible.first().id, amountMinor, occurredAt).toSettlement("time").occurredAt
        var remaining = amountMinor
        val allocations = eligible.mapNotNull { trade ->
            if (remaining == 0L) return@mapNotNull null
            val amount = minOf(remaining, farm.settlements.outstandingMinorFor(trade))
            remaining -= amount
            Settlement("settlement-${UUID.randomUUID()}", trade.id, amount, time, "", false)
        }
        val updated = farm.copy(settlements = (farm.settlements + allocations).toMutableList())
        FarmStateValidator.validateFarm(updated)
        store.saveFarm(updated)
        return allocations
    }

    fun addSupplyUsage(farmId: String, draft: SupplyUsageDraft): SupplyUsage {
        val farm = getFarm(farmId)
        val supply = farm.supplies.firstOrNull { it.id == draft.supplyId }
            ?: throw IllegalArgumentException("Supply not found: ${draft.supplyId}")
        require(supply.unit == draft.unit) { "Supply unit does not match" }
        require(draft.quantity <= farm.supplyQuantityAvailable(supply.id)) {
            "Usage exceeds available supply"
        }
        val usage = draft.toUsage("supply-usage-${UUID.randomUUID()}")
        val updated = farm.copy(supplyUsages = (farm.supplyUsages + usage).toMutableList())
        FarmStateValidator.validateFarm(updated)
        store.saveFarm(updated)
        return usage
    }

    fun addProductionRecord(farmId: String, draft: ProductionRecordDraft, zone: ZoneId): ProductionRecord {
        val farm = getFarm(farmId)
        val product = farm.products.firstOrNull { it.id == draft.productId }
            ?: throw IllegalArgumentException("Production product not found: ${draft.productId}")
        require(product.defaultUnit == draft.unit) { "Production unit does not match product" }
        val candidate = draft.toRecord("production-${UUID.randomUUID()}")
        val localDate = candidate.occurredAt.atZoneSameInstant(zone).toLocalDate()
        val existing = farm.productionRecords.firstOrNull {
            it.productId == candidate.productId &&
                it.session == candidate.session &&
                it.session != ProductionSession.OTHER &&
                it.occurredAt.atZoneSameInstant(zone).toLocalDate() == localDate
        }
        val records = farm.productionRecords.toMutableList()
        if (existing == null) records += candidate
        else records[records.indexOf(existing)] = candidate.copy(id = existing.id)
        val updated = farm.copy(productionRecords = records)
        FarmStateValidator.validateFarm(updated)
        store.saveFarm(updated)
        return records.first { it.id == candidate.id || it.id == existing?.id }
    }

    fun updateProductionRecord(farmId: String, recordId: String, draft: ProductionRecordDraft): ProductionRecord {
        val farm = getFarm(farmId)
        require(farm.productionRecords.any { it.id == recordId }) { "Production record not found: $recordId" }
        val record = draft.toRecord(recordId)
        val updatedRecords = farm.productionRecords.toMutableList()
        updatedRecords[updatedRecords.indexOfFirst { it.id == recordId }] = record
        val updated = farm.copy(productionRecords = updatedRecords)
        FarmStateValidator.validateFarm(updated)
        store.saveFarm(updated)
        return record
    }

    fun deleteProductionRecord(farmId: String, recordId: String) {
        val farm = getFarm(farmId)
        val updated = farm.productionRecords.filterNot { it.id == recordId }.toMutableList()
        require(updated.size < farm.productionRecords.size) { "Production record not found: $recordId" }
        store.saveFarm(farm.copy(productionRecords = updated))
    }

    fun productionForDay(farmId: String, date: java.time.LocalDate, zone: ZoneId): List<ProductionRecord> =
        getFarm(farmId).productionForDay(date, zone)

    fun productionReconciliation(farmId: String, productId: String, date: java.time.LocalDate, zone: ZoneId): ProductionReconciliation =
        getFarm(farmId).productionReconciliation(productId, date, zone)

    fun addProductionAllocation(farmId: String, draft: ProductionAllocationDraft, zone: ZoneId): ProductionAllocation {
        val farm = getFarm(farmId)
        val product = farm.products.firstOrNull { it.id == draft.productId }
            ?: throw IllegalArgumentException("Allocation product not found: ${draft.productId}")
        require(product.defaultUnit == draft.unit) { "Allocation unit does not match product" }
        val allocation = draft.toAllocation("allocation-${UUID.randomUUID()}")
        if (allocation.type != ProductionAllocationType.OTHER) {
            val reconciliation = farm.productionReconciliation(allocation.productId, allocation.occurredAt.atZoneSameInstant(zone).toLocalDate(), zone)
            require(allocation.quantity <= reconciliation.unexplained) { "Allocation exceeds unexplained production" }
        }
        val updated = farm.copy(productionAllocations = (farm.productionAllocations + allocation).toMutableList())
        FarmStateValidator.validateFarm(updated)
        store.saveFarm(updated)
        return allocation
    }

    fun updateProductionAllocation(farmId: String, allocationId: String, draft: ProductionAllocationDraft, zone: ZoneId): ProductionAllocation {
        val farm = getFarm(farmId)
        require(farm.productionAllocations.any { it.id == allocationId }) { "Allocation not found: $allocationId" }
        val allocation = draft.toAllocation(allocationId)
        val without = farm.copy(productionAllocations = farm.productionAllocations.filterNot { it.id == allocationId }.toMutableList())
        if (allocation.type != ProductionAllocationType.OTHER) {
            val reconciliation = without.productionReconciliation(allocation.productId, allocation.occurredAt.atZoneSameInstant(zone).toLocalDate(), zone)
            require(allocation.quantity <= reconciliation.unexplained) { "Allocation exceeds unexplained production" }
        }
        val updated = without.copy(productionAllocations = (without.productionAllocations + allocation).toMutableList())
        FarmStateValidator.validateFarm(updated)
        store.saveFarm(updated)
        return allocation
    }

    fun deleteProductionAllocation(farmId: String, allocationId: String) {
        val farm = getFarm(farmId)
        val updated = farm.productionAllocations.filterNot { it.id == allocationId }.toMutableList()
        require(updated.size < farm.productionAllocations.size) { "Allocation not found: $allocationId" }
        store.saveFarm(farm.copy(productionAllocations = updated))
    }

    fun addProductSale(
        farmId: String,
        partyId: String?,
        productId: String,
        quantity: BigDecimal,
        rateMinor: Long,
        initialPaymentMinor: Long?,
        occurredAt: String
    ): Trade {
        val farm = getFarm(farmId)
        val product = farm.products.firstOrNull { it.id == productId }
            ?: throw IllegalArgumentException("Product not found: $productId")
        val detail = ProductSaleDetail(
            tradeId = "pending",
            productId = product.id,
            quantity = quantity,
            unit = product.defaultUnit,
            customUnitLabel = product.customUnitLabel,
            rateMinor = rateMinor
        )
        val totalMinor = detail.totalMinor()
        val trade = TradeDraft(
            type = TradeType.SALE,
            partyId = partyId,
            totalMinor = totalMinor,
            description = product.name,
            occurredAt = occurredAt
        ).toTrade("trade-${UUID.randomUUID()}")
        if (initialPaymentMinor != null && initialPaymentMinor < 0) {
            throw IllegalArgumentException("Initial payment cannot be negative")
        }
        if (initialPaymentMinor != null && initialPaymentMinor > totalMinor) {
            throw IllegalArgumentException("Initial payment cannot exceed the total")
        }
        val openingSettlement = initialPaymentMinor?.takeIf { it > 0 }?.let { amount ->
            Settlement(
                id = "settlement-${UUID.randomUUID()}",
                tradeId = trade.id,
                amountMinor = amount,
                occurredAt = trade.occurredAt,
                note = "",
                isInitialPayment = true
            )
        }
        val updated = farm.copy(
            trades = (farm.trades + trade).toMutableList(),
            settlements = if (openingSettlement == null) {
                farm.settlements
            } else {
                (farm.settlements + openingSettlement).toMutableList()
            },
            productSaleDetails = (farm.productSaleDetails + detail.copy(tradeId = trade.id)).toMutableList()
        )
        FarmStateValidator.validateFarm(updated)
        store.saveFarm(updated)
        return trade
    }

    /**
     * Permanently removes the farm and everything it owns. After this call the
     * farm can no longer be loaded and the current farm id, if it pointed at
     * this farm, is cleared so the app returns to its initial no-farm state.
     * Externally exported backup files are never touched here.
     */
    fun deleteFarm(farmId: String) {
        getFarm(farmId)
        store.deleteFarm(farmId)
    }

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
        require(currencyCode.matches(CURRENCY_CODE_PATTERN)) { "Farm currency must be a 3-letter ISO code" }
        store.saveFarm(farm.copy(currencyCode = currencyCode.trim().uppercase()))
    }

    /**
     * Renames the farm in place. The trimmed name replaces the existing one
     * while the farm id, currency, every record and the schema version are
     * preserved — this is an in-place update, not a new farm.
     */
    fun renameFarm(farmId: String, name: String): FarmState {
        val farm = getFarm(farmId)
        val trimmed = name.trim()
        require(trimmed.isNotBlank()) { "Farm name is required" }
        val renamed = farm.copy(name = trimmed)
        store.saveFarm(renamed)
        return renamed
    }

    fun addParty(farmId: String, draft: PartyDraft): Party {
        val farm = getFarm(farmId)
        require(draft.name.isNotBlank()) { "Party name is required" }
        val party = Party(id = "party-${UUID.randomUUID()}", name = draft.name.trim(), role = draft.role, contact = draft.contact.trim(), notes = draft.notes.trim())
        val updated = farm.copy(parties = (farm.parties + party).toMutableList())
        store.saveFarm(updated)
        return party
    }

    fun updateParty(farmId: String, partyId: String, draft: PartyDraft): Party {
        val farm = getFarm(farmId)
        require(draft.name.isNotBlank()) { "Party name is required" }
        val index = farm.parties.indexOfFirst { it.id == partyId }
        require(index >= 0) { "Party not found: $partyId" }
        val referencedTypes = farm.trades.filter { it.partyId == partyId }.map { it.type }.distinct()
        val incompatibleType = referencedTypes.firstOrNull { !draft.role.compatibleWith(it) }
        require(incompatibleType == null) {
            "Party role cannot change while sales or purchases reference this party"
        }
        val party = farm.parties[index].copy(name = draft.name.trim(), role = draft.role, contact = draft.contact.trim(), notes = draft.notes.trim())
        val updatedParties = farm.parties.toMutableList()
        updatedParties[index] = party
        store.saveFarm(farm.copy(parties = updatedParties))
        return party
    }

    fun deleteParty(farmId: String, partyId: String) {
        val farm = getFarm(farmId)
        val referenced = farm.trades.any { it.partyId == partyId }
        require(!referenced) { "Party cannot be deleted while sales or purchases reference it" }
        val updatedParties = farm.parties.filterNot { it.id == partyId }
        require(updatedParties.size < farm.parties.size) { "Party not found: $partyId" }
        store.saveFarm(farm.copy(parties = updatedParties.toMutableList()))
    }

    fun addTrade(farmId: String, draft: TradeDraft): Trade =
        addTradeWithInitialSettlement(farmId, draft, initialSettlementMinor = null)

    /**
     * Creates a trade and, optionally, its first settlement in one persisted
     * state transition. Passing [initialSettlementMinor] (defaults to the full
     * total when null is NOT passed; see [addTrade] for the no-payment form)
     * records the money as of the trade's own [Trade.occurredAt], matching the
     * simple M5-02 behavior of "paid at the time of this trade".
     */
    fun addTradeWithInitialSettlement(farmId: String, draft: TradeDraft, initialSettlementMinor: Long?): Trade {
        val farm = getFarm(farmId)
        val trade = draft.toTrade("trade-${UUID.randomUUID()}")
        val openingSettlement = initialSettlementMinor?.takeIf { it > 0 }?.let { amount ->
            Settlement(
                id = "settlement-${UUID.randomUUID()}",
                tradeId = trade.id,
                amountMinor = amount,
                occurredAt = trade.occurredAt,
                note = "",
                isInitialPayment = true
            )
        }
        if (initialSettlementMinor != null && initialSettlementMinor < 0) {
            throw IllegalArgumentException("Trade initial payment amount cannot be negative")
        }
        if (openingSettlement != null && initialSettlementMinor!! > trade.totalMinor) {
            throw IllegalArgumentException("Trade initial payment cannot exceed the total")
        }
        val updated = farm.copy(
            trades = (farm.trades + trade).toMutableList(),
            settlements = if (openingSettlement != null) {
                (farm.settlements + openingSettlement).toMutableList()
            } else {
                farm.settlements
            }
        )
        FarmStateValidator.validateFarm(updated)
        store.saveFarm(updated)
        return trade
    }

    fun updateTrade(farmId: String, tradeId: String, draft: TradeDraft): Trade {
        val farm = getFarm(farmId)
        val index = farm.trades.indexOfFirst { it.id == tradeId }
        require(index >= 0) { "Trade not found: $tradeId" }
        val trade = draft.toTrade(tradeId)
        val updatedTrades = farm.trades.toMutableList()
        updatedTrades[index] = trade
        val updated = farm.copy(trades = updatedTrades)
        FarmStateValidator.validateFarm(updated)
        store.saveFarm(updated)
        return trade
    }

    fun deleteTrade(farmId: String, tradeId: String) {
        val farm = getFarm(farmId)
        require(farm.settlements.none { it.tradeId == tradeId }) {
            "Trade cannot be deleted while payment records exist"
        }
        val updatedTrades = farm.trades.filterNot { it.id == tradeId }
        require(updatedTrades.size < farm.trades.size) { "Trade not found: $tradeId" }
        store.saveFarm(farm.copy(trades = updatedTrades.toMutableList()))
    }

    fun trade(farmId: String, tradeId: String): Trade? = getFarm(farmId).trades.firstOrNull { it.id == tradeId }

    fun trades(farmId: String): List<Trade> = getFarm(farmId).tradesNewestFirst()

    fun addSettlement(farmId: String, draft: SettlementDraft): Settlement {
        val farm = getFarm(farmId)
        require(farm.trades.any { it.id == draft.tradeId }) { "Settlement trade not found: ${draft.tradeId}" }
        val settlement = draft.toSettlement("settlement-${UUID.randomUUID()}")
        val updated = farm.copy(settlements = (farm.settlements + settlement).toMutableList())
        FarmStateValidator.validateFarm(updated)
        store.saveFarm(updated)
        return settlement
    }

    fun recordCustomerPayment(
        farmId: String,
        partyId: String,
        amountMinor: Long,
        occurredAt: String,
        note: String = ""
    ): List<Settlement> {
        require(amountMinor > 0) { "Payment amount must be positive" }
        val farm = getFarm(farmId)
        val party = farm.parties.firstOrNull { it.id == partyId }
            ?: throw IllegalArgumentException("Payment party not found: $partyId")
        require(party.role.compatibleWith(TradeType.SALE)) {
            "Payment party is not a customer"
        }
        val eligible = farm.trades
            .filter { it.partyId == partyId && it.type == TradeType.SALE }
            .filter { farm.settlements.outstandingMinorFor(it) > 0L }
            .sortedWith(compareBy<Trade> { it.occurredAt }.thenBy { it.id })
        val totalOutstanding = eligible.fold(0L) { total, trade ->
            Math.addExact(total, farm.settlements.outstandingMinorFor(trade))
        }
        require(totalOutstanding > 0L) { "No outstanding balance for this customer" }
        require(amountMinor <= totalOutstanding) { "Payment exceeds the outstanding balance" }
        var remaining = amountMinor
        val paymentTime = SettlementDraft(
            tradeId = eligible.first().id,
            amountMinor = amountMinor,
            occurredAt = occurredAt,
            note = note
        ).toSettlement("validation").occurredAt
        val newSettlements = eligible.mapNotNull { trade ->
            if (remaining == 0L) return@mapNotNull null
            val allocation = minOf(remaining, farm.settlements.outstandingMinorFor(trade))
            remaining -= allocation
            Settlement(
                id = "settlement-${UUID.randomUUID()}",
                tradeId = trade.id,
                amountMinor = allocation,
                occurredAt = paymentTime,
                note = note.trim()
            )
        }
        val updated = farm.copy(settlements = (farm.settlements + newSettlements).toMutableList())
        FarmStateValidator.validateFarm(updated)
        store.saveFarm(updated)
        return newSettlements
    }

    fun updateSettlement(farmId: String, settlementId: String, draft: SettlementDraft): Settlement {
        val farm = getFarm(farmId)
        val index = farm.settlements.indexOfFirst { it.id == settlementId }
        require(index >= 0) { "Settlement not found: $settlementId" }
        val settlement = draft.toSettlement(settlementId)
        val updatedSettlements = farm.settlements.toMutableList()
        updatedSettlements[index] = settlement
        val updated = farm.copy(settlements = updatedSettlements)
        FarmStateValidator.validateFarm(updated)
        store.saveFarm(updated)
        return settlement
    }

    fun deleteSettlement(farmId: String, settlementId: String) {
        val farm = getFarm(farmId)
        val updatedSettlements = farm.settlements.filterNot { it.id == settlementId }
        require(updatedSettlements.size < farm.settlements.size) { "Settlement not found: $settlementId" }
        FarmStateValidator.validateFarm(farm.copy(settlements = updatedSettlements.toMutableList()))
        store.saveFarm(farm.copy(settlements = updatedSettlements.toMutableList()))
    }

    fun settlement(farmId: String, settlementId: String): Settlement? =
        getFarm(farmId).settlements.firstOrNull { it.id == settlementId }

    fun settlements(farmId: String): List<Settlement> = getFarm(farmId).settlementsNewestFirst()

    fun settlementsForTrade(farmId: String, tradeId: String): List<Settlement> =
        getFarm(farmId).settlements.settlementsForTrade(tradeId)

    fun tradePaymentSummary(farmId: String, trade: Trade): TradePaymentSummary =
        getFarm(farmId).settlements.paymentSummaryFor(trade)

    fun parties(farmId: String): List<Party> {
        val farm = getFarm(farmId)
        return farm.parties.withIndex()
            .sortedWith(
                compareBy<IndexedValue<Party>> { it.value.name.lowercase() }
                    .thenBy { it.index }
            )
            .map { it.value }
    }

    fun party(farmId: String, partyId: String): Party? = getFarm(farmId).parties.firstOrNull { it.id == partyId }

    /**
     * M5-04: the derived Party Khata projection for one party. Purely computed
     * from the persisted Party → Trade → Settlement facts on every call; never
     * persisted itself. Throws when the party does not exist rather than
     * fabricating one.
     */
    fun partyLedger(farmId: String, partyId: String): PartyLedger =
        getFarm(farmId).partyLedger(partyId)

    /** The Party's derived current position only (see [partyLedger]). */
    fun partyLedgerSummary(farmId: String, partyId: String): PartyLedgerSummary =
        getFarm(farmId).partyLedgerSummary(partyId)

    /**
     * M5-05: the derived Farm Financial Overview for the whole farm. Purely
     * computed from the persisted facts on every call with a stable clock and
     * zone; never persisted itself. See [FarmFinancialOverview].
     */
    fun farmFinancialOverview(
        farmId: String,
        preset: FinancialPeriodPreset,
        now: java.time.OffsetDateTime,
        zone: java.time.ZoneId
    ): FarmFinancialOverview = getFarm(farmId).financialOverview(preset, now, zone)

    /** M6: non-persisted per-party/per-period Hisab reconciliation. */
    fun partyHisab(
        farmId: String,
        partyId: String,
        preset: FinancialPeriodPreset,
        now: java.time.OffsetDateTime,
        zone: java.time.ZoneId
    ): PartyHisabResult = getFarm(farmId).partyHisab(partyId, preset, now, zone)

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
    val parties: MutableList<Party> = mutableListOf(),
    val trades: MutableList<Trade> = mutableListOf(),
    val settlements: MutableList<Settlement> = mutableListOf(),
    val products: MutableList<FarmProduct> = mutableListOf(),
    val productSaleDetails: MutableList<ProductSaleDetail> = mutableListOf(),
    val supplies: MutableList<FarmSupply> = mutableListOf(),
    val supplyPurchaseDetails: MutableList<SupplyPurchaseDetail> = mutableListOf(),
    val supplyUsages: MutableList<SupplyUsage> = mutableListOf(),
    val productionRecords: MutableList<ProductionRecord> = mutableListOf(),
    val productionAllocations: MutableList<ProductionAllocation> = mutableListOf(),
    val schemaVersion: Int = CURRENT_FARM_SCHEMA_VERSION
) {
    /**
     * Whether the farm already holds monetary records (transactions, trades or
     * settlements). Changing the display currency for such a farm requires
     * confirmation, because amounts are kept unchanged and are never converted.
     */
    fun hasMonetaryRecords(): Boolean =
        transactions.isNotEmpty() || trades.isNotEmpty() || settlements.isNotEmpty()

    companion object {
        const val DEFAULT_CURRENCY_CODE = "NPR"
        const val CURRENT_FARM_SCHEMA_VERSION = 12
    }
}

fun FarmState.transactionsNewestFirst(): List<FarmTransaction> =
    transactions.withIndex()
        .sortedWith(
            compareByDescending<IndexedValue<FarmTransaction>> { it.value.occurredAt }
                .thenByDescending { it.index }
        )
        .map { it.value }

fun FarmState.tradesNewestFirst(): List<Trade> =
    trades.withIndex()
        .sortedWith(
            compareByDescending<IndexedValue<Trade>> { it.value.occurredAt }
                .thenByDescending { it.index }
        )
        .map { it.value }

fun FarmState.settlementsNewestFirst(): List<Settlement> =
    settlements.withIndex()
        .sortedWith(
            compareByDescending<IndexedValue<Settlement>> { it.value.occurredAt }
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
