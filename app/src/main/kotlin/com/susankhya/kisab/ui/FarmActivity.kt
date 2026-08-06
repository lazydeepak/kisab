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
import androidx.annotation.StringRes
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
import com.susankhya.kisab.persistence.FarmBackupException
import com.susankhya.kisab.persistence.FarmBackupFileAdapter
import com.susankhya.kisab.persistence.SharedPreferencesFarmStore
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class FarmActivity : AppCompatActivity() {
    private lateinit var store: SharedPreferencesFarmStore
    private lateinit var service: FarmSliceService
    internal lateinit var backupFileAdapter: FarmBackupFileAdapter

    private val moneyFormatter = MoneyFormatter()
    private val moneyInputParser = MoneyInputParser(moneyFormatter)
    private val numberFormatter = NumberFormatter()
    private val timePresentation = TimePresentation()

    private val presentationLocale: java.util.Locale
        get() = PresentationLocale.presentationLocale(resources.configuration.locales.get(0))

    private val deviceZone: ZoneId
        get() = ZoneId.systemDefault()

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
                val uri = result.data?.data
                val content = pendingExportContent
                pendingExportContent = null
                if (uri == null || content == null) {
                    showToast(R.string.toast_export_cancelled)
                    return@registerForActivityResult
                }
                try {
                    backupFileAdapter.writeText(uri.toString(), content, FarmBackupCodec.MAX_BACKUP_BYTES)
                    showToast(R.string.toast_backup_exported)
                } catch (exception: FarmBackupException) {
                    showValidationMessage(FarmUiError.fromBackupFailure(exception).resourceId)
                } catch (exception: Exception) {
                    Log.e(LOG_TAG, "export backup failed", exception)
                    showValidationMessage(FarmUiError.UNEXPECTED.resourceId)
                }
            } else {
                pendingExportContent = null
                showToast(R.string.toast_export_cancelled)
            }
        }

        openBackupDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) {
                showToast(R.string.toast_import_cancelled)
                return@registerForActivityResult
            }
            try {
                val content = backupFileAdapter.readText(uri.toString(), FarmBackupCodec.MAX_BACKUP_BYTES)
                handleImportedBackupContent(content)
            } catch (exception: FarmBackupException) {
                showValidationMessage(FarmUiError.fromBackupFailure(exception).resourceId)
            } catch (exception: Exception) {
                Log.e(LOG_TAG, "import backup failed", exception)
                showValidationMessage(FarmUiError.UNEXPECTED.resourceId)
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
            FarmOrdering.entryKinds.map { FarmLabels.entryKind(this, it) }
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        transactionTypeSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            FarmOrdering.transactionTypes.map { FarmLabels.transactionType(this, it) }
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
        if (name.isBlank()) {
            showValidationMessage(FarmUiError.FARM_NAME_REQUIRED.resourceId)
            return
        }
        try {
            service.createFarm(name)
            render()
            showToast(R.string.toast_farm_created)
        } catch (exception: Exception) {
            showUnexpectedFailure(exception, "create farm failed")
        }
    }

    private fun addEntry() {
        val farmId = currentFarmId ?: return showMissingFarmMessage()
        val label = entryLabelInput.text?.toString()?.trim().orEmpty()
        if (label.isBlank()) {
            showValidationMessage(FarmUiError.ENTRY_LABEL_REQUIRED.resourceId)
            return
        }
        val quantity = entryQuantityInput.text?.toString()?.trim()?.toIntOrNull()
        if (quantity == null || quantity <= 0) {
            showValidationMessage(FarmUiError.ENTRY_QUANTITY_POSITIVE_WHOLE.resourceId)
            return
        }
        try {
            val entry = FarmEntry(
                kind = selectedEntryKind(),
                label = label,
                quantity = quantity
            )
            service.addEntry(farmId, entry)
            entryLabelInput.setText("")
            entryQuantityInput.setText("")
            render()
            showToast(R.string.toast_entry_added)
        } catch (exception: Exception) {
            showUnexpectedFailure(exception, "add entry failed")
        }
    }

    private fun saveTransaction() {
        val farmId = currentFarmId ?: return showMissingFarmMessage()
        val currency = transactionCurrencyInput.text?.toString()?.trim()?.uppercase().orEmpty()
        if (!currency.matches(Regex("^[A-Z]{3}$"))) {
            showValidationMessage(FarmUiError.CURRENCY_ISO_THREE_LETTERS.resourceId)
            return
        }
        val farm = service.loadFarm(farmId)
        val farmCurrency = farm?.transactions
            ?.filterNot { it.id == currentTransactionId }
            ?.map { it.currency }
            ?.toSet()
            .orEmpty()
        if (farmCurrency.isNotEmpty() && currency !in farmCurrency) {
            showValidationMessage(FarmUiError.CURRENCY_MISMATCH.resourceId, farmCurrency.first())
            return
        }
        val occurredAt = transactionOccurredAtInput.text?.toString()?.trim().orEmpty()
        if (occurredAt.isBlank()) {
            showValidationMessage(FarmUiError.TRANSACTION_DATE_TIME_REQUIRED.resourceId)
            return
        }
        if (!isValidIsoOffsetDateTime(occurredAt)) {
            showValidationMessage(FarmUiError.TRANSACTION_DATE_TIME_INVALID.resourceId)
            return
        }
        val amountInput = transactionAmountInput.text?.toString().orEmpty()
        val amount = when (val result = moneyInputParser.parse(presentationLocale, currency, amountInput)) {
            is MoneyInputResult.Valid -> result.amountMinor
            MoneyInputResult.Missing -> {
                showValidationMessage(FarmUiError.AMOUNT_REQUIRED.resourceId)
                return
            }
            MoneyInputResult.NotPositive -> {
                showValidationMessage(FarmUiError.AMOUNT_NOT_POSITIVE.resourceId)
                return
            }
            MoneyInputResult.Invalid -> {
                showValidationMessage(FarmUiError.AMOUNT_INVALID.resourceId)
                return
            }
            MoneyInputResult.TooPrecise -> {
                showValidationMessage(FarmUiError.AMOUNT_TOO_PRECISE.resourceId)
                return
            }
            MoneyInputResult.TooLarge -> {
                showValidationMessage(FarmUiError.AMOUNT_TOO_LARGE.resourceId)
                return
            }
        }
        val description = transactionDescriptionInput.text?.toString()?.trim().orEmpty()
        if (description.isBlank()) {
            showValidationMessage(FarmUiError.TRANSACTION_DESCRIPTION_REQUIRED.resourceId)
            return
        }
        Log.d(LOG_TAG, "draftCurrency=$currency description=$description occurredAt=$occurredAt")
        val draft = FarmTransactionDraft(
            type = selectedTransactionType(),
            category = selectedTransactionCategory(),
            amountMinor = amount,
            currency = currency,
            description = description,
            occurredAt = occurredAt
        )
        try {
            if (currentTransactionId == null) {
                val created = service.createTransaction(farmId, draft)
                Log.d(LOG_TAG, "created transaction id=${created.id} with farmId=$farmId")
                showToast(R.string.toast_transaction_created)
            } else {
                val updated = service.updateTransaction(farmId, currentTransactionId!!, draft)
                Log.d(LOG_TAG, "updated transaction id=${updated.id} with farmId=$farmId")
                showToast(R.string.toast_transaction_updated)
            }
            clearTransactionForm()
            render()
        } catch (exception: Exception) {
            showUnexpectedFailure(exception, "save transaction failed")
        }
    }

    private fun deleteTransaction() {
        val farmId = currentFarmId ?: return showMissingFarmMessage()
        val selectedTransactionId = currentTransactionId
            ?: return showValidationMessage(FarmUiError.TRANSACTION_SELECTION_REQUIRED.resourceId)
        AlertDialog.Builder(this)
            .setTitle(string(R.string.dialog_delete_transaction_title))
            .setMessage(string(R.string.dialog_delete_transaction_message))
            .setPositiveButton(string(R.string.action_delete)) { _, _ ->
                try {
                    service.deleteTransaction(farmId, selectedTransactionId)
                    currentTransactionId = null
                    render()
                    showToast(R.string.toast_transaction_deleted)
                } catch (exception: Exception) {
                    showUnexpectedFailure(exception, "delete transaction failed")
                }
            }
            .setNegativeButton(string(R.string.action_cancel), null)
            .show()
    }

    private fun exportBackup() {
        val backupContent = createBackupContentForCurrentFarm() ?: return
        val farmId = currentFarmId ?: return showMissingFarmMessage()
        val farm = service.loadFarm(farmId) ?: return showMissingFarmMessage()
        pendingExportContent = backupContent
        val safeName = farm.name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
            .takeIf { it.isNotBlank() } ?: string(R.string.backup_filename_fallback)
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_TITLE, string(R.string.backup_filename_format, safeName))
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
        } catch (exception: FarmBackupException) {
            showValidationMessage(FarmUiError.fromBackupFailure(exception).resourceId)
        } catch (exception: Exception) {
            showUnexpectedFailure(exception, "import backup content failed")
        }
    }

    private fun showImportConfirmation(farm: FarmState) {
        val summary = buildImportedFarmSummary(farm)
        AlertDialog.Builder(this)
            .setTitle(string(R.string.dialog_replace_farm_title))
            .setMessage(string(R.string.dialog_import_backup_message_format, farm.name, summary))
            .setPositiveButton(string(R.string.action_replace_farm)) { _, _ ->
                store.saveFarm(farm)
                currentFarmId = farm.id
                render()
                showToast(R.string.toast_farm_restored)
            }
            .setNegativeButton(string(R.string.action_cancel)) { _, _ ->
                showToast(R.string.toast_import_cancelled)
            }
            .show()
    }

    private fun buildImportedFarmSummary(farm: FarmState): String {
        val balanceMinor = farm.transactions.fold(0L) { total, transaction ->
            total + if (transaction.type == TransactionType.INCOME) transaction.amountMinor else -transaction.amountMinor
        }
        val currencyCode = farm.transactions.map { it.currency }.toSet().firstOrNull() ?: "NPR"
        return string(
            R.string.imported_farm_summary_format,
            numberFormatter.format(presentationLocale, farm.entries.size),
            numberFormatter.format(presentationLocale, farm.transactions.size),
            formatMoney(currencyCode, balanceMinor)
        )
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
        Log.d(LOG_TAG, "summary=$summaryTextValue")
        summaryText.text = summaryTextValue
        entriesText.text = if (farm.entries.isEmpty()) {
            string(R.string.empty_entries)
        } else {
            farm.entries.joinToString("\n") { entry ->
                string(
                    R.string.entry_row_format,
                    FarmLabels.entryKind(this, entry.kind),
                    entry.label,
                    numberFormatter.format(presentationLocale, entry.quantity)
                )
            }
        }
        val newestFirstTransactions = service.transactionsNewestFirst(farm.id)
        transactionsText.text = if (newestFirstTransactions.isEmpty()) {
            string(R.string.empty_transactions)
        } else {
            newestFirstTransactions.joinToString("\n") { transaction ->
                string(
                    R.string.transaction_row_format,
                    timePresentation.displayDateTime(presentationLocale, deviceZone, transaction.occurredAt),
                    FarmLabels.transactionType(this, transaction.type),
                    FarmLabels.transactionCategory(this, transaction.category),
                    transaction.description,
                    formatMoney(transaction.currency, transaction.amountMinor)
                )
            }
        }

        clearTransactionForm()
        populateTransactionSelection(newestFirstTransactions)
        validationMessageText.visibility = View.GONE
    }

    private fun buildSummaryText(farm: FarmState, summary: FarmSummary): String {
        return string(
            R.string.farm_summary_format,
            farm.name,
            numberFormatter.format(presentationLocale, summary.entryCount),
            numberFormatter.format(presentationLocale, summary.transactionCount),
            formattedBalance(summary.currencyCode, summary.balanceMinor)
        )
    }

    internal fun formattedBalance(currencyCode: String?, balanceMinor: Long): String =
        formatMoney(currencyCode ?: "NPR", balanceMinor)

    internal fun formatMoney(currencyCode: String, amountMinor: Long): String =
        moneyFormatter.format(presentationLocale, currencyCode, amountMinor)

    internal fun formatCount(value: Int): String = numberFormatter.format(presentationLocale, value)

    internal fun displayTransactionTime(transaction: FarmTransaction): String =
        timePresentation.displayDateTime(presentationLocale, deviceZone, transaction.occurredAt)

    internal fun editFieldValue(transaction: FarmTransaction): String =
        timePresentation.toEditFieldValue(deviceZone, transaction.occurredAt)

    internal fun editFieldAmount(currencyCode: String, amountMinor: Long): String =
        moneyFormatter.toEditFieldValue(presentationLocale, currencyCode, amountMinor)

    private fun selectedEntryKind(): FarmEntryKind = FarmOrdering.entryKinds[entryKindSpinner.selectedItemPosition]

    private fun selectedTransactionType(): TransactionType =
        FarmOrdering.transactionTypes[transactionTypeSpinner.selectedItemPosition]

    private fun selectedTransactionCategory(): TransactionCategory =
        FarmOrdering.categoriesFor(selectedTransactionType())[transactionCategorySpinner.selectedItemPosition]

    private fun refreshCategoryChoices() {
        val categories = FarmOrdering.categoriesFor(selectedTransactionType())
        transactionCategorySpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categories.map { FarmLabels.transactionCategory(this, it) }
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun fillTransactionForm(transaction: FarmTransaction) {
        transactionTypeSpinner.setSelection(FarmOrdering.transactionTypes.indexOf(transaction.type))
        refreshCategoryChoices()
        val categoryIndex = FarmOrdering.categoriesFor(transaction.type).indexOf(transaction.category)
        transactionCategorySpinner.setSelection(categoryIndex)
        transactionAmountInput.setText(
            moneyFormatter.toEditFieldValue(presentationLocale, transaction.currency, transaction.amountMinor)
        )
        transactionCurrencyInput.setText(transaction.currency)
        transactionDescriptionInput.setText(transaction.description)
        transactionOccurredAtInput.setText(
            timePresentation.toEditFieldValue(deviceZone, transaction.occurredAt)
        )
    }

    private fun clearTransactionForm() {
        currentTransactionId = null
        transactionAmountInput.setText("")
        transactionCurrencyInput.setText(defaultCurrencyForForm())
        transactionDescriptionInput.setText("")
        transactionOccurredAtInput.setText("")
        transactionTypeSpinner.setSelection(0)
        refreshCategoryChoices()
    }

    private fun defaultCurrencyForForm(): String {
        val farm = currentFarmId?.let { service.loadFarm(it) } ?: return "NPR"
        return farm.transactions.map { it.currency }.toSet().firstOrNull() ?: "NPR"
    }

    private fun populateTransactionSelection(transactions: List<FarmTransaction>) {
        val displayValues = mutableListOf<String>()
        val ids = mutableListOf<String?>()
        displayValues.add(string(R.string.transaction_selection_create))
        ids.add(null)
        transactions.forEach { transaction ->
            displayValues.add(
                string(
                    R.string.transaction_selection_row_format,
                    transaction.id.take(12),
                    transaction.description,
                    formatMoney(transaction.currency, transaction.amountMinor)
                )
            )
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

    private fun showValidationMessage(@StringRes resId: Int, vararg formatArgs: Any) {
        val message = string(resId, *formatArgs)
        validationMessageText.text = message
        validationMessageText.visibility = View.VISIBLE
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showToast(@StringRes resId: Int) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
    }

    private fun showUnexpectedFailure(exception: Exception, contextLog: String) {
        Log.e(LOG_TAG, "$contextLog: ${exception.message}", exception)
        showValidationMessage(FarmUiError.UNEXPECTED.resourceId)
    }

    private fun showMissingFarmMessage() {
        showToast(FarmUiError.CURRENT_FARM_MISSING.resourceId)
    }

    private fun string(@StringRes resId: Int, vararg formatArgs: Any): String =
        if (formatArgs.isEmpty()) getString(resId) else getString(resId, *formatArgs)

    private fun isValidIsoOffsetDateTime(value: String): Boolean = try {
        OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        true
    } catch (exception: RuntimeException) {
        false
    }

    private companion object {
        const val LOG_TAG = "FarmActivity"
    }
}
