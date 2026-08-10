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
    }

    fun validateParty(party: Party) {
        require(party.id.isNotBlank()) { "Party id is required" }
        require(party.name.isNotBlank()) { "Party name is required" }
    }
}
