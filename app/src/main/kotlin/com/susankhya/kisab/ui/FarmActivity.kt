package com.susankhya.kisab.ui

import android.content.Intent
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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import com.susankhya.kisab.persistence.AndroidStorageAccessFrameworkBackupFileAdapter
import com.susankhya.kisab.persistence.FarmBackupCodec
import com.susankhya.kisab.persistence.FarmBackupFileAdapter
import com.susankhya.kisab.persistence.SharedPreferencesFarmStore

class FarmActivity : AppCompatActivity() {
    private lateinit var store: SharedPreferencesFarmStore
    private lateinit var service: FarmSliceService
    internal lateinit var backupFileAdapter: FarmBackupFileAdapter

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
    private lateinit var exportBackupButton: Button
    private lateinit var importBackupButton: Button

    private lateinit var createBackupDocumentLauncher: ActivityResultLauncher<Intent>
    private lateinit var openBackupDocumentLauncher: ActivityResultLauncher<Array<String>>

    private var currentTransactionId: String? = null
    private var currentFarmId: String? = null
    private var transactionIdsForSelection: List<String?> = emptyList()
    private var pendingExportContent: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        store = SharedPreferencesFarmStore(applicationContext)
        service = FarmSliceService(store)
        backupFileAdapter = AndroidStorageAccessFrameworkBackupFileAdapter(applicationContext)

        createBackupDocumentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data ?: run {
                    pendingExportContent = null
                    Toast.makeText(this, "Export cancelled", Toast.LENGTH_SHORT).show()
                    return@registerForActivityResult
                }
                val content = pendingExportContent ?: run {
                    Toast.makeText(this, "Export cancelled", Toast.LENGTH_SHORT).show()
                    return@registerForActivityResult
                }
                backupFileAdapter.writeText(uri.toString(), content, FarmBackupCodec.MAX_BACKUP_BYTES)
                pendingExportContent = null
                Toast.makeText(this, "Backup exported", Toast.LENGTH_SHORT).show()
            } else {
                pendingExportContent = null
                Toast.makeText(this, "Export cancelled", Toast.LENGTH_SHORT).show()
            }
        }

        openBackupDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) {
                Toast.makeText(this, "Import cancelled", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }
            try {
                val content = backupFileAdapter.readText(uri.toString(), FarmBackupCodec.MAX_BACKUP_BYTES)
                handleImportedBackupContent(content)
            } catch (exception: IllegalArgumentException) {
                showValidationMessage(exception.message.orEmpty())
            }
        }

        setContentView(R.layout.activity_farm)

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
        exportBackupButton = findViewById(R.id.exportBackupButton)
        importBackupButton = findViewById(R.id.importBackupButton)

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
        exportBackupButton.setOnClickListener { exportBackup() }
        importBackupButton.setOnClickListener { importBackup() }

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

    private fun exportBackup() {
        val backupContent = createBackupContentForCurrentFarm() ?: return
        val farmId = currentFarmId ?: return showMissingFarmMessage()
        val farm = service.loadFarm(farmId) ?: return showMissingFarmMessage()
        pendingExportContent = backupContent
        val safeName = farm.name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').takeIf { it.isNotBlank() } ?: "farm"
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_TITLE, "kisab-$safeName.backup")
        }
        createBackupDocumentLauncher.launch(intent)
    }

    internal fun createBackupContentForCurrentFarm(): String? {
        val farmId = currentFarmId ?: return null
        val farm = service.loadFarm(farmId) ?: return null
        return FarmBackupCodec.encode(farm)
    }

    private fun importBackup() {
        openBackupDocumentLauncher.launch(arrayOf("application/octet-stream"))
    }

    internal fun handleImportedBackupContent(content: String) {
        try {
            val envelope = FarmBackupCodec.decode(content)
            showImportConfirmation(envelope.farm)
        } catch (exception: IllegalArgumentException) {
            showValidationMessage(exception.message.orEmpty())
        }
    }

    private fun showImportConfirmation(farm: FarmState) {
        val summary = buildImportedFarmSummary(farm)
        AlertDialog.Builder(this)
            .setTitle("Replace current farm?")
            .setMessage("Import backup for ${farm.name}?\n\n$summary\n\nThis will replace the current farm permanently.")
            .setPositiveButton("Replace farm") { _, _ ->
                store.saveFarm(farm)
                currentFarmId = farm.id
                render()
                Toast.makeText(this, "Farm restored", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel") { _, _ ->
                Toast.makeText(this, "Import cancelled", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun buildImportedFarmSummary(farm: FarmState): String {
        val balanceMinor = farm.transactions.fold(0L) { total, transaction ->
            total + if (transaction.type == TransactionType.INCOME) transaction.amountMinor else -transaction.amountMinor
        }
        val currencyCode = farm.transactions.map { it.currency }.toSet().firstOrNull() ?: "USD"
        return "Entries: ${farm.entries.size}\nTransactions: ${farm.transactions.size}\nBalance: ${balanceMinor} $currencyCode"
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
        val newestFirstTransactions = service.transactionsNewestFirst(farm.id)
        transactionsText.text = if (newestFirstTransactions.isEmpty()) {
            "No transactions yet"
        } else {
            newestFirstTransactions.joinToString("\n") { transaction ->
                "- ${transaction.displayDateTime()} | ${transaction.type.name.lowercase()} | ${transaction.category.name.lowercase()} | ${transaction.description} | ${transaction.currency} ${transaction.amountMinor}"
            }
        }

        clearTransactionForm()
        populateTransactionSelection(newestFirstTransactions)
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
