package com.susankhya.kisab.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.susankhya.kisab.R
import com.susankhya.kisab.domain.FarmEntry
import com.susankhya.kisab.domain.FarmEntryKind
import com.susankhya.kisab.domain.FarmSliceService
import com.susankhya.kisab.domain.FarmState
import com.susankhya.kisab.domain.FarmSummary
import com.susankhya.kisab.domain.FarmTransaction
import com.susankhya.kisab.domain.FarmTransactionDraft
import com.susankhya.kisab.domain.TransactionCategory
import com.susankhya.kisab.domain.TransactionType
import com.susankhya.kisab.persistence.SharedPreferencesFarmStore

class FarmActivity : AppCompatActivity() {
    private lateinit var store: SharedPreferencesFarmStore
    private lateinit var service: FarmSliceService

    private lateinit var createFarmContainer: androidx.appcompat.widget.LinearLayoutCompat
    private lateinit var farmDetailsContainer: androidx.appcompat.widget.LinearLayoutCompat
    private lateinit var farmNameInput: EditText
    private lateinit var summaryText: TextView
    private lateinit var entriesText: TextView
    private lateinit var transactionsText: TextView
    private lateinit var entryKindSpinner: Spinner
    private lateinit var entryLabelInput: EditText
    private lateinit var entryQuantityInput: EditText
    private lateinit var transactionTypeSpinner: Spinner
    private lateinit var transactionCategorySpinner: Spinner
    private lateinit var transactionAmountInput: EditText
    private lateinit var transactionCurrencyInput: EditText
    private lateinit var transactionDescriptionInput: EditText
    private lateinit var transactionOccurredAtInput: EditText
    private lateinit var transactionSelectionSpinner: Spinner
    private lateinit var validationMessageText: TextView
    private lateinit var createFarmButton: Button
    private lateinit var addEntryButton: Button
    private lateinit var saveTransactionButton: Button
    private lateinit var deleteTransactionButton: Button

    private var currentTransactionId: String? = null
    private var currentFarmId: String? = null
    private var transactionIdsForSelection: List<String?> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_farm)

        store = SharedPreferencesFarmStore(applicationContext)
        service = FarmSliceService(store)

        createFarmContainer = findViewById(R.id.createFarmContainer)
        farmDetailsContainer = findViewById(R.id.farmDetailsContainer)
        farmNameInput = findViewById(R.id.farmNameInput)
        summaryText = findViewById(R.id.summaryText)
        entriesText = findViewById(R.id.entriesText)
        transactionsText = findViewById(R.id.transactionsText)
        entryKindSpinner = findViewById(R.id.entryKindSpinner)
        entryLabelInput = findViewById(R.id.entryLabelInput)
        entryQuantityInput = findViewById(R.id.entryQuantityInput)
        transactionTypeSpinner = findViewById(R.id.transactionTypeSpinner)
        transactionCategorySpinner = findViewById(R.id.transactionCategorySpinner)
        transactionAmountInput = findViewById(R.id.transactionAmountInput)
        transactionCurrencyInput = findViewById(R.id.transactionCurrencyInput)
        transactionDescriptionInput = findViewById(R.id.transactionDescriptionInput)
        transactionOccurredAtInput = findViewById(R.id.transactionOccurredAtInput)
        transactionSelectionSpinner = findViewById(R.id.transactionSelectionSpinner)
        validationMessageText = findViewById(R.id.validationMessageText)
        createFarmButton = findViewById(R.id.createFarmButton)
        addEntryButton = findViewById(R.id.addEntryButton)
        saveTransactionButton = findViewById(R.id.saveTransactionButton)
        deleteTransactionButton = findViewById(R.id.deleteTransactionButton)

        entryKindSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            listOf("Livestock", "Crop")
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        transactionTypeSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            TransactionType.values().map { it.name.lowercase().replaceFirstChar { char -> char.titlecase() } }
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        transactionTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                refreshCategoryChoices()
            }

            override fun onNothingSelected(parent: AdapterView<*>) = Unit
        }

        transactionSelectionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedTransactionId = transactionIdsForSelection.getOrNull(position)
                currentTransactionId = selectedTransactionId
                if (selectedTransactionId == null) {
                    clearTransactionForm()
                } else {
                    currentFarmId?.let { farmId ->
                        service.loadFarm(farmId)?.transactions?.find { it.id == selectedTransactionId }?.let { transaction ->
                            fillTransactionForm(transaction)
                        }
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) = Unit
        }

        createFarmButton.setOnClickListener { createFarm() }
        addEntryButton.setOnClickListener { addEntry() }
        saveTransactionButton.setOnClickListener { saveTransaction() }
        deleteTransactionButton.setOnClickListener { deleteTransaction() }

        render()
    }

    private fun createFarm() {
        val name = farmNameInput.text?.toString()?.trim().orEmpty()
        try {
            service.createFarm(name)
            render()
            Toast.makeText(this, "Farm created", Toast.LENGTH_SHORT).show()
        } catch (exception: IllegalArgumentException) {
            showValidationMessage(exception.message.orEmpty())
        }
    }

    private fun addEntry() {
        val farmId = currentFarmId ?: return showMissingFarmMessage()
        try {
            val entry = FarmEntry(
                kind = selectedEntryKind(),
                label = entryLabelInput.text?.toString()?.trim().orEmpty(),
                quantity = entryQuantityInput.text?.toString()?.toIntOrNull() ?: throw IllegalArgumentException("Quantity must be a whole number")
            )
            service.addEntry(farmId, entry)
            entryLabelInput.setText("")
            entryQuantityInput.setText("")
            render()
            Toast.makeText(this, "Entry added", Toast.LENGTH_SHORT).show()
        } catch (exception: IllegalArgumentException) {
            showValidationMessage(exception.message.orEmpty())
        }
    }

    private fun saveTransaction() {
        val farmId = currentFarmId ?: return showMissingFarmMessage()
        try {
            val currency = transactionCurrencyInput.text?.toString()?.trim()?.uppercase()?.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("Currency must be a 3-letter ISO code")
            val occurredAt = transactionOccurredAtInput.text?.toString()?.trim()?.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("Transaction date/time is required")
            val description = transactionDescriptionInput.text?.toString()?.trim().orEmpty()
            Log.d("FarmActivity", "draftCurrency=$currency description=$description occurredAt=$occurredAt")
            val draft = FarmTransactionDraft(
                type = selectedTransactionType(),
                category = selectedTransactionCategory(),
                amountMinor = transactionAmountInput.text?.toString()?.trim()?.toLongOrNull() ?: throw IllegalArgumentException("Amount must be a whole number"),
                currency = currency,
                description = description,
                occurredAt = occurredAt
            )
            if (currentTransactionId == null) {
                val created = service.createTransaction(farmId, draft)
                Log.d("FarmActivity", "created transaction id=${created.id} with farmId=$farmId")
                Toast.makeText(this, "Transaction created", Toast.LENGTH_SHORT).show()
            } else {
                val updated = service.updateTransaction(farmId, currentTransactionId!!, draft)
                Log.d("FarmActivity", "updated transaction id=${updated.id} with farmId=$farmId")
                Toast.makeText(this, "Transaction updated", Toast.LENGTH_SHORT).show()
            }
            clearTransactionForm()
            render()
        } catch (exception: IllegalArgumentException) {
            Log.e("FarmActivity", "saveTransaction failed: ${exception.message}", exception)
            showValidationMessage(exception.message.orEmpty())
        }
    }

    private fun deleteTransaction() {
        val farmId = currentFarmId ?: return showMissingFarmMessage()
        val selectedTransactionId = currentTransactionId ?: return showValidationMessage("Select a transaction to delete")
        AlertDialog.Builder(this)
            .setTitle("Delete transaction")
            .setMessage("Delete this transaction permanently?")
            .setPositiveButton("Delete") { _, _ ->
                try {
                    service.deleteTransaction(farmId, selectedTransactionId)
                    currentTransactionId = null
                    render()
                    Toast.makeText(this, "Transaction deleted", Toast.LENGTH_SHORT).show()
                } catch (exception: IllegalArgumentException) {
                    showValidationMessage(exception.message.orEmpty())
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun render() {
        val farm = service.currentFarmId()?.let { service.loadFarm(it) }
        if (farm == null) {
            currentFarmId = null
            createFarmContainer.visibility = View.VISIBLE
            farmDetailsContainer.visibility = View.GONE
            return
        }

        currentFarmId = farm.id
        createFarmContainer.visibility = View.GONE
        farmDetailsContainer.visibility = View.VISIBLE
        renderFarm(farm)
    }

    private fun renderFarm(farm: FarmState) {
        val summary = service.summary(farm.id)
        val summaryTextValue = buildSummaryText(farm, summary)
        Log.d("FarmActivity", "summary=$summaryTextValue")
        summaryText.text = summaryTextValue
        entriesText.text = if (farm.entries.isEmpty()) {
            "No entries yet"
        } else {
            farm.entries.joinToString("\n") { entry -> "- ${entry.kind.name.lowercase()}: ${entry.label} x${entry.quantity}" }
        }
        transactionsText.text = if (farm.transactions.isEmpty()) {
            "No transactions yet"
        } else {
            farm.transactions.joinToString("\n") { transaction ->
                "- ${transaction.displayDateTime()} | ${transaction.type.name.lowercase()} | ${transaction.category.name.lowercase()} | ${transaction.description} | ${transaction.currency} ${transaction.amountMinor}"
            }
        }

        clearTransactionForm()
        populateTransactionSelection(farm.transactions)
        validationMessageText.visibility = View.GONE
    }

    private fun buildSummaryText(farm: FarmState, summary: FarmSummary): String = "Farm: ${farm.name}\n" +
        "Entry count: ${summary.entryCount}\n" +
        "Transaction count: ${summary.transactionCount}\n" +
        "Balance: ${summary.balanceMinor}${summary.currencyCode?.let { " ${it}" } ?: ""}"

    private fun selectedEntryKind(): FarmEntryKind = when (entryKindSpinner.selectedItemPosition) {
        1 -> FarmEntryKind.CROP
        else -> FarmEntryKind.LIVESTOCK
    }

    private fun selectedTransactionType(): TransactionType = when (transactionTypeSpinner.selectedItemPosition) {
        1 -> TransactionType.EXPENSE
        else -> TransactionType.INCOME
    }

    private fun selectedTransactionCategory(): TransactionCategory = when (selectedTransactionType()) {
        TransactionType.EXPENSE -> when (transactionCategorySpinner.selectedItemPosition) {
            1 -> TransactionCategory.SUPPLIES
            2 -> TransactionCategory.LABOR
            3 -> TransactionCategory.OTHER_EXPENSE
            else -> TransactionCategory.FEED
        }
        TransactionType.INCOME -> when (transactionCategorySpinner.selectedItemPosition) {
            1 -> TransactionCategory.SERVICES
            2 -> TransactionCategory.OTHER_INCOME
            else -> TransactionCategory.SALES
        }
    }

    private fun refreshCategoryChoices() {
        val categories = when (selectedTransactionType()) {
            TransactionType.EXPENSE -> listOf("Feed", "Supplies", "Labor", "Other expense")
            TransactionType.INCOME -> listOf("Sales", "Services", "Other income")
        }
        transactionCategorySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun fillTransactionForm(transaction: FarmTransaction) {
        transactionTypeSpinner.setSelection(if (transaction.type == TransactionType.EXPENSE) 1 else 0)
        refreshCategoryChoices()
        val categoryIndex = when (transaction.category) {
            TransactionCategory.SALES -> 0
            TransactionCategory.SERVICES -> 1
            TransactionCategory.OTHER_INCOME -> 2
            TransactionCategory.FEED -> 0
            TransactionCategory.SUPPLIES -> 1
            TransactionCategory.LABOR -> 2
            TransactionCategory.OTHER_EXPENSE -> 3
        }
        transactionCategorySpinner.setSelection(categoryIndex)
        transactionAmountInput.setText(transaction.amountMinor.toString())
        transactionCurrencyInput.setText(transaction.currency)
        transactionDescriptionInput.setText(transaction.description)
        transactionOccurredAtInput.setText(transaction.occurredAt.format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME))
    }

    private fun clearTransactionForm() {
        currentTransactionId = null
        transactionAmountInput.setText("")
        transactionCurrencyInput.setText("")
        transactionDescriptionInput.setText("")
        transactionOccurredAtInput.setText("")
        transactionTypeSpinner.setSelection(0)
        refreshCategoryChoices()
    }

    private fun populateTransactionSelection(transactions: List<FarmTransaction>) {
        val displayValues = mutableListOf<String>()
        val ids = mutableListOf<String?>()
        displayValues.add("Create new transaction")
        ids.add(null)
        transactions.forEach { transaction ->
            displayValues.add("${transaction.id.take(12)} • ${transaction.description} • ${transaction.currency} ${transaction.amountMinor}")
            ids.add(transaction.id)
        }
        transactionIdsForSelection = ids
        transactionSelectionSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, displayValues).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        val selectedPosition = ids.indexOfFirst { it == currentTransactionId }
        if (selectedPosition >= 0) {
            transactionSelectionSpinner.setSelection(selectedPosition)
        } else {
            transactionSelectionSpinner.setSelection(0)
        }
    }

    private fun showValidationMessage(message: String) {
        validationMessageText.text = message
        validationMessageText.visibility = View.VISIBLE
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showMissingFarmMessage() {
        Toast.makeText(this, "Create a farm first", Toast.LENGTH_SHORT).show()
    }
}
