package com.susankhya.kisab.domain

import java.time.format.DateTimeFormatter

object FarmStateValidator {
    private val DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    private val CURRENCY_CODE_PATTERN = Regex("^[A-Z]{3}$")

    fun validateTransaction(transaction: FarmTransaction) {
        require(transaction.id.isNotBlank()) { "Transaction id is required" }
        require(transaction.description.isNotBlank()) { "Transaction description is required" }
        require(transaction.amountMinor > 0) { "Transaction amount must be positive" }
        require(transaction.category.type == transaction.type) { "Transaction category is invalid for the selected type" }
        require(transaction.occurredAt.format(DATE_TIME_FORMATTER).isNotBlank()) { "Transaction date/time is required" }
    }

    fun validateFarm(farm: FarmState) {
        require(farm.id.isNotBlank()) { "Farm id is required" }
        require(farm.name.isNotBlank()) { "Farm name is required" }
        require(farm.currencyCode.matches(CURRENCY_CODE_PATTERN)) { "Farm currency must be a 3-letter ISO code" }
        farm.entries.forEach { entry ->
            require(entry.label.isNotBlank()) { "Entry label is required" }
            require(entry.quantity > 0) { "Entry quantity must be positive" }
        }
        farm.transactions.forEach(::validateTransaction)
        val transactionIds = farm.transactions.map { it.id }
        require(transactionIds.size == transactionIds.toSet().size) { "Transaction IDs must be unique" }
        farm.parties.forEach(::validateParty)
        val partyIds = farm.parties.map { it.id }
        require(partyIds.size == partyIds.toSet().size) { "Party IDs must be unique" }
        // Trades validate against the resulting farm (which includes its settled
        // amounts as derived state), then settlements validate the monetary facts.
        farm.trades.forEach { validateTrade(farm, it) }
        val tradeIds = farm.trades.map { it.id }
        require(tradeIds.size == tradeIds.toSet().size) { "Trade IDs must be unique" }
        farm.settlements.forEach { validateSettlement(farm, it) }
        val settlementIds = farm.settlements.map { it.id }
        require(settlementIds.size == settlementIds.toSet().size) { "Settlement IDs must be unique" }
    }

    fun validateParty(party: Party) {
        require(party.id.isNotBlank()) { "Party id is required" }
        require(party.name.isNotBlank()) { "Party name is required" }
    }

    fun validateTrade(farm: FarmState, trade: Trade) {
        require(trade.id.isNotBlank()) { "Trade id is required" }
        require(trade.totalMinor > 0) { "Trade total must be positive" }
        require(trade.occurredAt.format(DATE_TIME_FORMATTER).isNotBlank()) { "Trade date/time is required" }
        val paidMinor = farm.settlements.paidMinorFor(trade.id)
        require(paidMinor <= trade.totalMinor) { "Trade total cannot be less than the settled amount" }
        if (paidMinor < trade.totalMinor) {
            require(!trade.partyId.isNullOrBlank()) { "Partially paid or unpaid trades require a party" }
        }
        val party = trade.partyId?.let { id -> farm.parties.firstOrNull { it.id == id } }
        if (trade.partyId != null) {
            requireNotNull(party) { "Trade party not found: ${trade.partyId}" }
        }
        party?.let {
            require(it.role.compatibleWith(trade.type)) {
                "Trade party role is incompatible with the trade type"
            }
        }
    }

    fun validateSettlement(farm: FarmState, settlement: Settlement) {
        require(settlement.id.isNotBlank()) { "Settlement id is required" }
        require(settlement.amountMinor > 0) { "Settlement amount must be positive" }
        require(settlement.tradeId.isNotBlank()) { "Settlement trade id is required" }
        val trade = farm.trades.firstOrNull { it.id == settlement.tradeId }
        requireNotNull(trade) { "Settlement trade not found: ${settlement.tradeId}" }
        require(settlement.occurredAt.format(DATE_TIME_FORMATTER).isNotBlank()) { "Settlement date/time is required" }
        val paidMinor = farm.settlements.paidMinorFor(settlement.tradeId)
        require(paidMinor <= trade.totalMinor) { "Settlement amount cannot exceed the remaining balance" }
    }
}
