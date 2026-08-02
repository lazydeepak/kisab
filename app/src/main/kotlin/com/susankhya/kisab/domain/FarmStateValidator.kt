package com.susankhya.kisab.domain

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

object FarmStateValidator {
    private val DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    fun validateTransaction(transaction: FarmTransaction) {
        require(transaction.id.isNotBlank()) { "Transaction id is required" }
        require(transaction.description.isNotBlank()) { "Transaction description is required" }
        require(transaction.amountMinor > 0) { "Transaction amount must be positive" }
        require(transaction.currency.matches(Regex("^[A-Z]{3}$"))) { "Currency must be a 3-letter ISO code" }
        require(transaction.category.type == transaction.type) { "Transaction category is invalid for the selected type" }
        require(transaction.occurredAt.format(DATE_TIME_FORMATTER).isNotBlank()) { "Transaction date/time is required" }
    }

    fun validateFarm(farm: FarmState) {
        require(farm.id.isNotBlank()) { "Farm id is required" }
        require(farm.name.isNotBlank()) { "Farm name is required" }
        farm.entries.forEach { entry ->
            require(entry.label.isNotBlank()) { "Entry label is required" }
            require(entry.quantity > 0) { "Entry quantity must be positive" }
        }
        farm.transactions.forEach(::validateTransaction)
        val currencies = farm.transactions.map { it.currency }.toSet()
        require(currencies.size <= 1) { "Transactions use multiple currencies" }
    }
}
