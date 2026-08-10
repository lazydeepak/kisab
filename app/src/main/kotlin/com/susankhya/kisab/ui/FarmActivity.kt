package com.susankhya.kisab.ui

import android.app.DatePickerDialog
import android.app.LocaleManager
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.text.InputType
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.RequiresApi
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.os.LocaleListCompat
import com.susankhya.kisab.R
import com.susankhya.kisab.domain.FarmEntry
import com.susankhya.kisab.domain.FarmEntryKind
import com.susankhya.kisab.domain.FarmSliceService
import com.susankhya.kisab.domain.FarmState
import com.susankhya.kisab.domain.FarmTotals
import com.susankhya.kisab.domain.FarmTransaction
import com.susankhya.kisab.domain.FarmTransactionDraft
import com.susankhya.kisab.domain.PartyDraft
import com.susankhya.kisab.domain.PartyRole
import com.susankhya.kisab.domain.TransactionCategory
import com.susankhya.kisab.domain.TransactionType
import com.susankhya.kisab.domain.transactionsNewestFirst
import com.susankhya.kisab.persistence.AndroidStorageAccessFrameworkBackupFileAdapter
import com.susankhya.kisab.persistence.FarmBackupCodec
import com.susankhya.kisab.persistence.FarmBackupException
import com.susankhya.kisab.persistence.FarmBackupFileAdapter
import com.susankhya.kisab.persistence.SharedPreferencesAppLanguagePreferences
import com.susankhya.kisab.persistence.SharedPreferencesFarmStore
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class FarmActivity : AppCompatActivity() {
    private lateinit var store: SharedPreferencesFarmStore
    private lateinit var service: FarmSliceService
    internal lateinit var backupFileAdapter: FarmBackupFileAdapter

    private enum class Destination { HOME, HISAB_KITAB, HISAB, SETTINGS }

    private val moneyFormatter = MoneyFormatter()
    private val moneyInputParser = MoneyInputParser(moneyFormatter)
    private val numberFormatter = NumberFormatter()
    private val timePresentation = TimePresentation()

    private val presentationLocale: java.util.Locale
        get() = PresentationLocale.presentationLocale(resources.configuration.locales.get(0))

    private val deviceZone: ZoneId
        get() = ZoneId.systemDefault()

    private lateinit var scrollView: ScrollView
    private lateinit var shellTitle: TextView
    private lateinit var shellSettingsButton: Button
    private lateinit var navHomeItem: LinearLayout
    private lateinit var navHisabKitabItem: LinearLayout
    private lateinit var navHisabItem: LinearLayout
    private lateinit var hisabKitabScreen: ScrollView
    private lateinit var hisabScreen: ScrollView
    private lateinit var settingsScreen: ScrollView

    private lateinit var partiesEmptyText: TextView
    private lateinit var partiesContainer: LinearLayout
    private lateinit var addPartyButton: Button
    private lateinit var partyEditorTitle: TextView
    private lateinit var partyNameInput: EditText
    private lateinit var partyRoleSpinner: Spinner
    private lateinit var partyContactInput: EditText
    private lateinit var partyNotesInput: EditText
    private lateinit var savePartyButton: Button
    private lateinit var cancelPartyButton: Button
    private lateinit var deletePartyButton: Button

    private lateinit var settingsCurrencyText: TextView
    private lateinit var changeSettingsCurrencyButton: Button
    private lateinit var settingsCurrencyLockedText: TextView
    private lateinit var settingsNoFarmText: TextView
    private lateinit var languageFollowDeviceRadio: RadioButton
    private lateinit var languageEnglishRadio: RadioButton
    private lateinit var languageNepaliRadio: RadioButton

    private lateinit var createFarmContainer: androidx.appcompat.widget.LinearLayoutCompat
    private lateinit var farmDetailsContainer: androidx.appcompat.widget.LinearLayoutCompat
    private lateinit var farmNameInput: EditText
    private lateinit var createFarmButton: Button
    private lateinit var farmNameText: TextView
    private lateinit var balanceText: TextView
    private lateinit var incomeText: TextView
    private lateinit var expensesText: TextView
    private lateinit var firstActionPrompt: TextView
    private lateinit var recordIncomeButton: Button
    private lateinit var recordExpenseButton: Button
    private lateinit var transactionEditorContainer: LinearLayoutCompat
    private lateinit var transactionEditorTitle: TextView
    private lateinit var transactionTypeIncomeRadio: RadioButton
    private lateinit var transactionTypeExpenseRadio: RadioButton
    private lateinit var transactionCategorySpinner: Spinner
    private lateinit var transactionAmountInput: EditText
    private lateinit var transactionDescriptionInput: EditText
    private lateinit var transactionDateTimeText: TextView
    private lateinit var changeDateTimeButton: Button
    private lateinit var validationMessageText: TextView
    private lateinit var saveTransactionButton: Button
    private lateinit var cancelTransactionButton: Button
    private lateinit var deleteTransactionButton: Button
    private lateinit var recentTransactionsTitle: TextView
    private lateinit var recentTransactionsContainer: LinearLayout
    private lateinit var farmToolsToggleButton: Button
    private lateinit var farmToolsContainer: LinearLayoutCompat
    private lateinit var summaryText: TextView
    private lateinit var entriesText: TextView
    private lateinit var entryKindSpinner: Spinner
    private lateinit var entryLabelInput: EditText
    private lateinit var entryQuantityInput: EditText
    private lateinit var addEntryButton: Button
    private lateinit var exportBackupButton: Button
    private lateinit var importBackupButton: Button

    private lateinit var createBackupDocumentLauncher: ActivityResultLauncher<Intent>
    private lateinit var openBackupDocumentLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var languagePreferences: AppLanguagePreferences
    private var languageCheckSuppressed = false

    private var currentFarmId: String? = null
    private var pendingExportContent: String? = null

    private var currentDestination: Destination = Destination.HOME
    private var lastPrimaryDestination: Destination = Destination.HOME
    private var editorState: TransactionEditorState? = null
    private var editorBaseline: TransactionEditorState? = null
    private var toolsExpanded: Boolean = false
    private var syncTypeListenersSuppressed = false
    private var editingPartyId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        store = SharedPreferencesFarmStore(applicationContext)
        service = FarmSliceService(store)
        backupFileAdapter = AndroidStorageAccessFrameworkBackupFileAdapter(applicationContext)
        languagePreferences = SharedPreferencesAppLanguagePreferences(applicationContext)

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

        setContentView(R.layout.activity_shell)
        bindViews()
        wireListeners()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (editorState != null) {
                    if (isEditorDirty()) {
                        showDiscardDialog { closeEditor() }
                    } else {
                        closeEditor()
                    }
                } else if (currentDestination == Destination.HISAB_KITAB && editingPartyId != null) {
                    if (isPartyEditorDirty()) {
                        showDiscardDialog { closePartyEditor() }
                    } else {
                        closePartyEditor()
                    }
                } else if (currentDestination == Destination.SETTINGS) {
                    showDestination(lastPrimaryDestination)
                } else if (currentDestination != Destination.HOME) {
                    showDestination(Destination.HOME)
                } else {
                    finish()
                }
            }
        })

        restoreDestinationFrom(savedInstanceState)
        render()
        showDestination(currentDestination)
        restoreEditorFrom(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_DESTINATION, currentDestination.name)
        outState.putString(STATE_LAST_PRIMARY_DESTINATION, lastPrimaryDestination.name)
        outState.putBoolean(STATE_TOOLS_EXPANDED, toolsExpanded)
        val state = currentEditorState() ?: return
        val baseline = editorBaseline
        outState.putBoolean(STATE_EDITOR_OPEN, true)
        writeEditorState(outState, STATE_EDITOR_PREFIX, state)
        if (baseline != null) {
            writeEditorState(outState, STATE_EDITOR_BASELINE_PREFIX, baseline)
        }
    }

    private fun restoreDestinationFrom(bundle: Bundle?) {
        val name = bundle?.getString(STATE_DESTINATION) ?: return
        val saved = runCatching { Destination.valueOf(name) }.getOrNull()
        if (saved != null) currentDestination = saved
        val primaryName = bundle?.getString(STATE_LAST_PRIMARY_DESTINATION) ?: return
        val primary = runCatching { Destination.valueOf(primaryName) }.getOrNull()
        if (primary != null) lastPrimaryDestination = primary
    }

    private fun bindViews() {
        scrollView = findViewById(R.id.scrollView)
        shellTitle = findViewById(R.id.shellTitle)
        shellSettingsButton = findViewById(R.id.shellSettingsButton)
        navHomeItem = findViewById(R.id.navHomeItem)
        navHisabKitabItem = findViewById(R.id.navHisabKitabItem)
        navHisabItem = findViewById(R.id.navHisabItem)
        hisabKitabScreen = findViewById(R.id.hisabKitabScreen)
        hisabScreen = findViewById(R.id.hisabScreen)
        settingsScreen = findViewById(R.id.settingsScreen)
        partiesEmptyText = findViewById(R.id.partiesEmptyText)
        partiesContainer = findViewById(R.id.partiesContainer)
        addPartyButton = findViewById(R.id.addPartyButton)
        partyEditorTitle = findViewById(R.id.partyEditorTitle)
        partyNameInput = findViewById(R.id.partyNameInput)
        partyRoleSpinner = findViewById(R.id.partyRoleSpinner)
        partyContactInput = findViewById(R.id.partyContactInput)
        partyNotesInput = findViewById(R.id.partyNotesInput)
        savePartyButton = findViewById(R.id.savePartyButton)
        cancelPartyButton = findViewById(R.id.cancelPartyButton)
        deletePartyButton = findViewById(R.id.deletePartyButton)
        settingsCurrencyText = findViewById(R.id.settingsCurrencyText)
        changeSettingsCurrencyButton = findViewById(R.id.changeSettingsCurrencyButton)
        settingsCurrencyLockedText = findViewById(R.id.settingsCurrencyLockedText)
        settingsNoFarmText = findViewById(R.id.settingsNoFarmText)
        languageFollowDeviceRadio = findViewById(R.id.languageFollowDeviceRadio)
        languageEnglishRadio = findViewById(R.id.languageEnglishRadio)
        languageNepaliRadio = findViewById(R.id.languageNepaliRadio)
        createFarmContainer = findViewById(R.id.createFarmContainer)
        farmDetailsContainer = findViewById(R.id.farmDetailsContainer)
        farmNameInput = findViewById(R.id.farmNameInput)
        createFarmButton = findViewById(R.id.createFarmButton)
        farmNameText = findViewById(R.id.farmNameText)
        balanceText = findViewById(R.id.balanceText)
        incomeText = findViewById(R.id.incomeText)
        expensesText = findViewById(R.id.expensesText)
        firstActionPrompt = findViewById(R.id.firstActionPrompt)
        recordIncomeButton = findViewById(R.id.recordIncomeButton)
        recordExpenseButton = findViewById(R.id.recordExpenseButton)
        transactionEditorContainer = findViewById(R.id.transactionEditorContainer)
        transactionEditorTitle = findViewById(R.id.transactionEditorTitle)
        transactionTypeIncomeRadio = findViewById(R.id.transactionTypeIncomeRadio)
        transactionTypeExpenseRadio = findViewById(R.id.transactionTypeExpenseRadio)
        transactionCategorySpinner = findViewById(R.id.transactionCategorySpinner)
        transactionAmountInput = findViewById(R.id.transactionAmountInput)
        transactionDescriptionInput = findViewById(R.id.transactionDescriptionInput)
        transactionDateTimeText = findViewById(R.id.transactionDateTimeText)
        changeDateTimeButton = findViewById(R.id.changeDateTimeButton)
        validationMessageText = findViewById(R.id.validationMessageText)
        saveTransactionButton = findViewById(R.id.saveTransactionButton)
        cancelTransactionButton = findViewById(R.id.cancelTransactionButton)
        deleteTransactionButton = findViewById(R.id.deleteTransactionButton)
        recentTransactionsTitle = findViewById(R.id.recentTransactionsTitle)
        recentTransactionsContainer = findViewById(R.id.recentTransactionsContainer)
        farmToolsToggleButton = findViewById(R.id.farmToolsToggleButton)
        farmToolsContainer = findViewById(R.id.farmToolsContainer)
        summaryText = findViewById(R.id.summaryText)
        entriesText = findViewById(R.id.entriesText)
        entryKindSpinner = findViewById(R.id.entryKindSpinner)
        entryLabelInput = findViewById(R.id.entryLabelInput)
        entryQuantityInput = findViewById(R.id.entryQuantityInput)
        addEntryButton = findViewById(R.id.addEntryButton)
        exportBackupButton = findViewById(R.id.exportBackupButton)
        importBackupButton = findViewById(R.id.importBackupButton)

        entryKindSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            FarmOrdering.entryKinds.map { FarmLabels.entryKind(this, it) }
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        partyRoleSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            FarmOrdering.partyRoles.map { FarmLabels.partyRole(this, it) }
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun wireListeners() {
        createFarmButton.setOnClickListener { createFarm() }
        addEntryButton.setOnClickListener { addEntry() }
        exportBackupButton.setOnClickListener { exportBackup() }
        importBackupButton.setOnClickListener { importBackup() }
        changeSettingsCurrencyButton.setOnClickListener { showFarmCurrencyChooser() }

        navHomeItem.setOnClickListener { navigateTo(Destination.HOME) }
        navHisabKitabItem.setOnClickListener { navigateTo(Destination.HISAB_KITAB) }
        navHisabItem.setOnClickListener { navigateTo(Destination.HISAB) }
        shellSettingsButton.setOnClickListener { navigateTo(Destination.SETTINGS) }

        addPartyButton.setOnClickListener { openPartyEditor(null) }
        savePartyButton.setOnClickListener { saveParty() }
        cancelPartyButton.setOnClickListener { closePartyEditor() }
        deletePartyButton.setOnClickListener { confirmDeleteParty() }

        val languageRadios = listOf(
            languageFollowDeviceRadio to AppLanguage.FOLLOW_DEVICE,
            languageEnglishRadio to AppLanguage.ENGLISH,
            languageNepaliRadio to AppLanguage.NEPALI
        )
        languageRadios.forEach { (radio, language) ->
            radio.setOnClickListener {
                if (!languageCheckSuppressed) onLanguageSelected(language)
            }
        }

        recordIncomeButton.setOnClickListener {
            confirmDiscardIfNeeded { openEditorForNew(TransactionType.INCOME) }
        }
        recordExpenseButton.setOnClickListener {
            confirmDiscardIfNeeded { openEditorForNew(TransactionType.EXPENSE) }
        }
        transactionTypeIncomeRadio.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) onTransactionTypeChanged(TransactionType.INCOME)
        }
        transactionTypeExpenseRadio.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) onTransactionTypeChanged(TransactionType.EXPENSE)
        }
        saveTransactionButton.setOnClickListener { saveTransaction() }
        cancelTransactionButton.setOnClickListener { cancelEditing() }
        deleteTransactionButton.setOnClickListener { deleteTransaction() }
        changeDateTimeButton.setOnClickListener { showDateTimePickers() }
        farmToolsToggleButton.setOnClickListener { toggleFarmTools() }
    }

    // --- Shell navigation ---------------------------------------------------

    private fun navigateTo(destination: Destination) {
        if (destination == currentDestination) {
            render()
            return
        }
        confirmDiscardIfNeeded { confirmDiscardPartyIfNeeded { showDestination(destination) } }
    }

    private fun showDestination(destination: Destination) {
        currentDestination = destination
        if (destination != Destination.SETTINGS) lastPrimaryDestination = destination
        scrollView.visibility = if (destination == Destination.HOME) View.VISIBLE else View.GONE
        hisabKitabScreen.visibility = if (destination == Destination.HISAB_KITAB) View.VISIBLE else View.GONE
        hisabScreen.visibility = if (destination == Destination.HISAB) View.VISIBLE else View.GONE
        settingsScreen.visibility = if (destination == Destination.SETTINGS) View.VISIBLE else View.GONE
        updateShellTitle()
        if (destination == Destination.SETTINGS) renderSettings()
        if (destination == Destination.HISAB_KITAB) renderParties()
    }

    private fun updateShellTitle() {
        shellTitle.text = when (currentDestination) {
            Destination.HOME -> {
                val farm = currentFarmId?.let { service.loadFarm(it) }
                farm?.name ?: string(R.string.app_name)
            }
            Destination.HISAB_KITAB -> string(R.string.nav_hisab_kitab)
            Destination.HISAB -> string(R.string.nav_hisab)
            Destination.SETTINGS -> string(R.string.nav_settings)
        }
    }

    // --- Parties ------------------------------------------------------------

    private fun renderParties() {
        val farmId = currentFarmId ?: run {
            partiesEmptyText.visibility = View.VISIBLE
            partiesContainer.removeAllViews()
            return
        }
        val parties = service.parties(farmId)
        partiesEmptyText.visibility = if (parties.isEmpty()) View.VISIBLE else View.GONE
        partiesContainer.removeAllViews()
        if (parties.isEmpty()) return
        val inflater = LayoutInflater.from(this)
        parties.forEach { party ->
            val row = inflater.inflate(R.layout.item_party_row, partiesContainer, false) as TextView
            row.setTag(party.id)
            row.text = string(
                R.string.party_row_format,
                party.name,
                FarmLabels.partyRole(this, party.role)
            )
            row.contentDescription = string(
                R.string.party_row_format,
                party.name,
                FarmLabels.partyRole(this, party.role)
            )
            row.setOnClickListener {
                if (editingPartyId != null) {
                    confirmDiscardPartyIfNeeded { openPartyEditor(party.id) }
                } else {
                    openPartyEditor(party.id)
                }
            }
            partiesContainer.addView(row)
        }
    }

    private fun openPartyEditor(partyId: String?) {
        val farmId = currentFarmId ?: return showMissingFarmMessage()
        val party = partyId?.let { service.party(farmId, it) }
        editingPartyId = partyId
        partyEditorTitle.text = string(R.string.party_editor_title)
        partyNameInput.setText(party?.name ?: "")
        val roleIndex = FarmOrdering.partyRoles.indexOf(party?.role ?: PartyRole.CUSTOMER)
            .coerceAtLeast(0)
        partyRoleSpinner.setSelection(roleIndex)
        partyContactInput.setText(party?.contact ?: "")
        partyNotesInput.setText(party?.notes ?: "")
        setPartyEditorVisible(true)
        partyNameInput.requestFocus()
    }

    private fun saveParty() {
        val farmId = currentFarmId ?: return showMissingFarmMessage()
        val name = partyNameInput.text?.toString()?.trim().orEmpty()
        if (name.isBlank()) {
            showValidationMessage(FarmUiError.PARTY_NAME_REQUIRED.resourceId)
            partyNameInput.requestFocus()
            return
        }
        val draft = PartyDraft(
            name = name,
            role = selectedPartyRole(),
            contact = partyContactInput.text?.toString()?.trim().orEmpty(),
            notes = partyNotesInput.text?.toString()?.trim().orEmpty()
        )
        try {
            if (editingPartyId == null) {
                service.addParty(farmId, draft)
                showToast(R.string.toast_party_saved)
            } else {
                service.updateParty(farmId, editingPartyId!!, draft)
                showToast(R.string.toast_party_saved)
            }
            closePartyEditor()
            renderParties()
        } catch (exception: Exception) {
            showUnexpectedFailure(exception, "save party failed")
        }
    }

    private fun confirmDeleteParty() {
        val farmId = currentFarmId ?: return showMissingFarmMessage()
        val partyId = editingPartyId ?: return
        AlertDialog.Builder(this)
            .setTitle(string(R.string.dialog_delete_party_title))
            .setMessage(string(R.string.dialog_delete_party_message))
            .setPositiveButton(string(R.string.delete_party_action)) { _, _ ->
                try {
                    service.deleteParty(farmId, partyId)
                    closePartyEditor()
                    renderParties()
                    showToast(R.string.toast_party_deleted)
                } catch (exception: Exception) {
                    showUnexpectedFailure(exception, "delete party failed")
                }
            }
            .setNegativeButton(string(R.string.action_cancel), null)
            .show()
    }

    private fun closePartyEditor() {
        editingPartyId = null
        setPartyEditorVisible(false)
        renderParties()
    }

    private fun setPartyEditorVisible(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        partyEditorTitle.visibility = visibility
        partyNameInput.visibility = visibility
        val roleLabel = findViewById<TextView>(R.id.partyRoleLabel)
        roleLabel.visibility = visibility
        partyRoleSpinner.visibility = visibility
        partyContactInput.visibility = visibility
        partyNotesInput.visibility = visibility
        savePartyButton.visibility = visibility
        cancelPartyButton.visibility = visibility
        deletePartyButton.visibility = if (visible && editingPartyId != null) View.VISIBLE else View.GONE
    }

    private fun isPartyEditorDirty(): Boolean {
        if (editingPartyId == null && partyNameInput.text?.toString()?.isBlank() != false &&
            partyContactInput.text?.toString()?.isBlank() != false &&
            partyNotesInput.text?.toString()?.isBlank() != false &&
            selectedPartyRole() == PartyRole.CUSTOMER
        ) {
            return false
        }
        val farmId = currentFarmId ?: return false
        val party = editingPartyId?.let { service.party(farmId, it) }
        val baselineName = party?.name ?: ""
        val baselineRole = party?.role ?: PartyRole.CUSTOMER
        val baselineContact = party?.contact ?: ""
        val baselineNotes = party?.notes ?: ""
        return partyNameInput.text?.toString()?.trim().orEmpty() != baselineName ||
            selectedPartyRole() != baselineRole ||
            partyContactInput.text?.toString()?.trim().orEmpty() != baselineContact ||
            partyNotesInput.text?.toString()?.trim().orEmpty() != baselineNotes
    }

    private fun confirmDiscardPartyIfNeeded(action: () -> Unit) {
        if (isPartyEditorDirty()) {
            showDiscardDialog(action)
        } else {
            action()
        }
    }

    private fun selectedPartyRole(): PartyRole =
        FarmOrdering.partyRoles[partyRoleSpinner.selectedItemPosition.coerceIn(0, FarmOrdering.partyRoles.size - 1)]

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

    // --- Transaction editor -------------------------------------------------

    private fun openEditorForNew(type: TransactionType) {
        val state = TransactionEditorState.create(
            type = type,
            occurredAt = OffsetDateTime.now(deviceZone)
        )
        applyEditorState(state, baseline = state)
    }

    private fun openEditorForTransaction(transaction: FarmTransaction) {
        val state = TransactionEditorState(
            mode = TransactionEditorMode.EDIT,
            transactionId = transaction.id,
            type = transaction.type,
            category = transaction.category,
            amountText = moneyFormatter.toEditFieldValue(presentationLocale, farmCurrencyOf(transaction), transaction.amountMinor),
            description = transaction.description,
            occurredAt = transaction.occurredAt
        )
        applyEditorState(state, baseline = state)
    }

    private fun farmCurrencyOf(transaction: FarmTransaction): String {
        val farm = currentFarmId?.let { service.loadFarm(it) }
        return farm?.currencyCode ?: FarmState.DEFAULT_CURRENCY_CODE
    }

    private fun applyEditorState(state: TransactionEditorState, baseline: TransactionEditorState) {
        editorState = state
        editorBaseline = baseline
        transactionEditorTitle.text = string(
            if (state.mode == TransactionEditorMode.CREATE) R.string.transaction_editor_new_section
            else R.string.transaction_editor_edit_section
        )
        syncTypeListenersSuppressed = true
        transactionTypeIncomeRadio.isChecked = state.type == TransactionType.INCOME
        transactionTypeExpenseRadio.isChecked = state.type == TransactionType.EXPENSE
        syncTypeListenersSuppressed = false
        refreshCategoryChoices(state.type)
        val categoryIndex = FarmOrdering.categoriesFor(state.type).indexOf(state.category).coerceAtLeast(0)
        transactionCategorySpinner.setSelection(categoryIndex)
        transactionAmountInput.setText(state.amountText)
        transactionDescriptionInput.setText(state.description)
        updateDateTimeDisplay()
        saveTransactionButton.text = string(saveActionRes(state))
        deleteTransactionButton.visibility =
            if (state.mode == TransactionEditorMode.EDIT) View.VISIBLE else View.GONE
        validationMessageText.visibility = View.GONE
        transactionEditorContainer.visibility = View.VISIBLE
        if (state.mode == TransactionEditorMode.CREATE) {
            transactionAmountInput.requestFocus()
            scrollEditorIntoView()
        }
    }

    private fun closeEditor() {
        editorState = null
        editorBaseline = null
        transactionEditorContainer.visibility = View.GONE
        validationMessageText.visibility = View.GONE
        transactionAmountInput.setText("")
        transactionDescriptionInput.setText("")
    }

    private fun cancelEditing() {
        if (isEditorDirty()) {
            showDiscardDialog { closeEditor() }
        } else {
            closeEditor()
        }
    }

    private fun confirmDiscardIfNeeded(action: () -> Unit) {
        if (editorState != null && isEditorDirty()) {
            showDiscardDialog {
                closeEditor()
                action()
            }
        } else {
            action()
        }
    }

    private fun isEditorDirty(): Boolean {
        val baseline = editorBaseline ?: return false
        val current = currentEditorState() ?: return false
        return current != baseline
    }

    private fun currentEditorState(): TransactionEditorState? {
        val state = editorState ?: return null
        return state.copy(
            type = selectedTransactionType(),
            category = selectedTransactionCategory(),
            amountText = transactionAmountInput.text?.toString().orEmpty(),
            description = transactionDescriptionInput.text?.toString().orEmpty()
        )
    }

    private fun onTransactionTypeChanged(type: TransactionType) {
        if (syncTypeListenersSuppressed) return
        val state = editorState ?: return
        val categories = FarmOrdering.categoriesFor(type)
        refreshCategoryChoices(type)
        val updated = state.copy(type = type, category = categories.first())
        editorState = updated
        transactionCategorySpinner.setSelection(0)
        saveTransactionButton.text = string(saveActionRes(updated))
    }

    private fun saveTransaction() {
        val farmId = currentFarmId ?: return showMissingFarmMessage()
        val state = currentEditorState() ?: return
        val farm = service.loadFarm(farmId)
        val farmCurrency = farm?.currencyCode ?: FarmState.DEFAULT_CURRENCY_CODE
        val amount = when (val result = moneyInputParser.parse(presentationLocale, farmCurrency, state.amountText)) {
            is MoneyInputResult.Valid -> result.amountMinor
            MoneyInputResult.Missing -> return showEditorError(FarmUiError.AMOUNT_REQUIRED, transactionAmountInput)
            MoneyInputResult.NotPositive -> return showEditorError(FarmUiError.AMOUNT_NOT_POSITIVE, transactionAmountInput)
            MoneyInputResult.Invalid -> return showEditorError(FarmUiError.AMOUNT_INVALID, transactionAmountInput)
            MoneyInputResult.TooPrecise -> return showEditorError(FarmUiError.AMOUNT_TOO_PRECISE, transactionAmountInput)
            MoneyInputResult.TooLarge -> return showEditorError(FarmUiError.AMOUNT_TOO_LARGE, transactionAmountInput)
        }
        if (state.description.isBlank()) {
            showEditorError(FarmUiError.TRANSACTION_DESCRIPTION_REQUIRED, transactionDescriptionInput)
            return
        }
        val occurredAt = state.occurredAt.atZoneSameInstant(deviceZone)
            .toOffsetDateTime()
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val draft = FarmTransactionDraft(
            type = state.type,
            category = state.category,
            amountMinor = amount,
            description = state.description,
            occurredAt = occurredAt
        )
        try {
            if (state.mode == TransactionEditorMode.CREATE) {
                service.createTransaction(farmId, draft)
                showToast(R.string.toast_transaction_created)
            } else {
                service.updateTransaction(farmId, state.transactionId!!, draft)
                showToast(R.string.toast_transaction_updated)
            }
            closeEditor()
            render()
        } catch (exception: Exception) {
            showUnexpectedFailure(exception, "save transaction failed")
        }
    }

    private fun deleteTransaction() {
        val farmId = currentFarmId ?: return showMissingFarmMessage()
        val state = currentEditorState() ?: return
        if (state.mode != TransactionEditorMode.EDIT) return
        val transactionId = state.transactionId ?: return
        AlertDialog.Builder(this)
            .setTitle(string(R.string.dialog_delete_transaction_title))
            .setMessage(string(R.string.dialog_delete_transaction_message))
            .setPositiveButton(string(R.string.action_delete)) { _, _ ->
                try {
                    service.deleteTransaction(farmId, transactionId)
                    closeEditor()
                    render()
                    showToast(R.string.toast_transaction_deleted)
                } catch (exception: Exception) {
                    showUnexpectedFailure(exception, "delete transaction failed")
                }
            }
            .setNegativeButton(string(R.string.action_cancel), null)
            .show()
    }

    private fun showDateTimePickers() {
        val zone = deviceZone
        val current = editorState?.occurredAt?.atZoneSameInstant(zone) ?: ZonedDateTime.now(zone)
        val datePicker = DatePickerDialog(
            this,
            { _, year, monthOfYear, dayOfMonth ->
                val timePicker = TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        editorState = editorState?.copy(
                            occurredAt = EditorDateTime.fromPickerValues(year, monthOfYear, dayOfMonth, hourOfDay, minute, zone)
                        )
                        updateDateTimeDisplay()
                    },
                    current.hour,
                    current.minute,
                    DateFormat.is24HourFormat(this)
                )
                timePicker.show()
            },
            current.year,
            current.monthValue - 1,
            current.dayOfMonth
        )
        datePicker.show()
    }

    private fun showFarmCurrencyChooser() {
        val input = EditText(this).apply {
            id = R.id.currencyInput
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            hint = string(R.string.currency_iso_hint)
            setText(currentFarmCurrencyOrNull())
            setSelection(text?.length ?: 0)
        }
        val errorText = TextView(this).apply {
            id = R.id.currencyErrorText
            setTextColor(resources.getColor(android.R.color.holo_red_dark, theme))
            text = ""
            setPadding(0, dp(4), 0, 0)
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(input)
            addView(errorText)
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(string(R.string.currency_choice_dialog_title))
            .setView(content)
            .setPositiveButton(string(R.string.action_ok), null)
            .setNegativeButton(string(R.string.action_cancel), null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val code = input.text?.toString()?.trim()?.uppercase(Locale.US).orEmpty()
                if (code.matches(Regex("^[A-Z]{3}$"))) {
                    applyFarmCurrencyChange(code)
                    dialog.dismiss()
                } else {
                    errorText.text = string(R.string.error_currency_iso_three_letters)
                }
            }
        }
        dialog.show()
    }

    private fun applyFarmCurrencyChange(code: String) {
        val farmId = currentFarmId ?: return showMissingFarmMessage()
        try {
            service.setFarmCurrency(farmId, code)
            render()
        } catch (exception: Exception) {
            showValidationMessage(FarmUiError.CURRENCY_LOCKED.resourceId)
            Log.e(LOG_TAG, "change farm currency failed", exception)
        }
    }

    private fun currentFarmCurrencyOrNull(): String {
        val farm = currentFarmId?.let { service.loadFarm(it) }
        return farm?.currencyCode ?: FarmState.DEFAULT_CURRENCY_CODE
    }

    private fun updateDateTimeDisplay() {
        val occurredAt = editorState?.occurredAt ?: return
        val now = OffsetDateTime.now()
        transactionDateTimeText.text = if (timePresentation.isToday(deviceZone, occurredAt, now)) {
            string(
                R.string.today_time_format,
                string(R.string.today_label),
                timePresentation.shortTime(presentationLocale, deviceZone, occurredAt)
            )
        } else {
            timePresentation.displayDateTime(presentationLocale, deviceZone, occurredAt)
        }
    }

    private fun showDiscardDialog(onDiscard: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(string(R.string.discard_changes_title))
            .setMessage(string(R.string.discard_changes_message))
            .setPositiveButton(string(R.string.action_discard)) { _, _ -> onDiscard() }
            .setNegativeButton(string(R.string.action_keep_editing), null)
            .show()
    }

    private fun saveActionRes(state: TransactionEditorState): Int = when (state.mode) {
        TransactionEditorMode.CREATE -> {
            if (state.type == TransactionType.INCOME) R.string.save_income_action else R.string.save_expense_action
        }
        TransactionEditorMode.EDIT -> R.string.update_transaction_action
    }

    private fun restoreEditorFrom(bundle: Bundle?) {
        if (bundle == null) return
        toolsExpanded = bundle.getBoolean(STATE_TOOLS_EXPANDED, false)
        updateToolsExpansion()
        if (!bundle.getBoolean(STATE_EDITOR_OPEN, false)) return
        val state = readEditorState(bundle, STATE_EDITOR_PREFIX) ?: return
        val baseline = readEditorState(bundle, STATE_EDITOR_BASELINE_PREFIX) ?: state
        applyEditorState(state, baseline = baseline)
    }

    private fun writeEditorState(bundle: Bundle, prefix: String, state: TransactionEditorState) {
        bundle.putString(prefix + STATE_EDITOR_MODE, state.mode.name)
        bundle.putString(prefix + STATE_EDITOR_TRANSACTION_ID, state.transactionId)
        bundle.putString(prefix + STATE_EDITOR_TYPE, state.type.name)
        bundle.putString(prefix + STATE_EDITOR_CATEGORY, state.category.name)
        bundle.putString(prefix + STATE_EDITOR_AMOUNT, state.amountText)
        bundle.putString(prefix + STATE_EDITOR_DESCRIPTION, state.description)
        bundle.putString(prefix + STATE_EDITOR_OCCURRED_AT, state.occurredAt.toInstant().toString())
    }

    private fun readEditorState(bundle: Bundle, prefix: String): TransactionEditorState? {
        val mode = bundle.getString(prefix + STATE_EDITOR_MODE)?.let {
            runCatching { TransactionEditorMode.valueOf(it) }.getOrNull()
        } ?: return null
        val type = bundle.getString(prefix + STATE_EDITOR_TYPE)?.let {
            runCatching { TransactionType.valueOf(it) }.getOrNull()
        } ?: return null
        val category = bundle.getString(prefix + STATE_EDITOR_CATEGORY)?.let {
            runCatching { TransactionCategory.valueOf(it) }.getOrNull()
        } ?: return null
        val occurredAt = bundle.getString(prefix + STATE_EDITOR_OCCURRED_AT)?.let {
            runCatching { OffsetDateTime.parse(it) }.getOrNull()
        } ?: return null
        return TransactionEditorState(
            mode = mode,
            transactionId = bundle.getString(prefix + STATE_EDITOR_TRANSACTION_ID),
            type = type,
            category = category,
            amountText = bundle.getString(prefix + STATE_EDITOR_AMOUNT).orEmpty(),
            description = bundle.getString(prefix + STATE_EDITOR_DESCRIPTION).orEmpty(),
            occurredAt = occurredAt
        )
    }

    // --- Rendering ----------------------------------------------------------

    private fun render() {
        val farm = service.currentFarmId()?.let { service.loadFarm(it) }
        if (farm == null) {
            currentFarmId = null
            createFarmContainer.visibility = View.VISIBLE
            farmDetailsContainer.visibility = View.GONE
            updateShellTitle()
            return
        }
        currentFarmId = farm.id
        createFarmContainer.visibility = View.GONE
        farmDetailsContainer.visibility = View.VISIBLE
        renderFarm(farm)
        updateShellTitle()
        if (currentDestination == Destination.SETTINGS) renderSettings()
        if (currentDestination == Destination.HISAB_KITAB) renderParties()
    }

    private fun renderFarm(farm: FarmState) {
        farmNameText.text = farm.name
        val currency = farm.currencyCode
        val totals = try {
            FarmTotals.of(farm.transactions)
        } catch (exception: ArithmeticException) {
            Log.e(LOG_TAG, "farm totals overflow", exception)
            showValidationMessage(FarmUiError.UNEXPECTED.resourceId)
            return
        }
        balanceText.text = string(R.string.overview_balance_format, formatMoney(currency, totals.balanceMinor))
        incomeText.text = string(R.string.overview_income_format, formatMoney(currency, totals.incomeMinor))
        expensesText.text = string(R.string.overview_expenses_format, formatMoney(currency, totals.expensesMinor))
        firstActionPrompt.visibility = if (farm.transactions.isEmpty()) View.VISIBLE else View.GONE
        renderRecentTransactions(farm, currency)
        renderFarmTools(farm, currency, totals)
    }

    private fun renderRecentTransactions(farm: FarmState, currency: String) {
        val transactions = farm.transactionsNewestFirst()
        recentTransactionsContainer.removeAllViews()
        if (transactions.isEmpty()) {
            val empty = TextView(this)
            empty.text = string(R.string.empty_transactions)
            empty.setPadding(0, dp(8), 0, dp(8))
            recentTransactionsContainer.addView(empty)
            return
        }
        val inflater = LayoutInflater.from(this)
        transactions.forEach { transaction ->
            val row = inflater.inflate(R.layout.item_recent_transaction, recentTransactionsContainer, false) as TextView
            row.setTag(transaction.id)
            row.text = string(
                R.string.transaction_row_format,
                displayTransactionTime(transaction),
                FarmLabels.transactionType(this, transaction.type),
                FarmLabels.transactionCategory(this, transaction.category),
                transaction.description,
                formatMoney(currency, transaction.amountMinor)
            )
            row.contentDescription = string(
                R.string.recent_transaction_accessibility_format,
                FarmLabels.transactionType(this, transaction.type),
                transaction.description,
                formatMoney(currency, transaction.amountMinor)
            )
            row.setOnClickListener {
                confirmDiscardIfNeeded { openEditorForTransaction(transaction) }
            }
            recentTransactionsContainer.addView(row)
        }
    }

    private fun renderFarmTools(farm: FarmState, currency: String, totals: FarmTotals) {
        summaryText.text = string(
            R.string.farm_tools_summary_format,
            farm.name,
            numberFormatter.format(presentationLocale, farm.entries.size),
            formatMoney(currency, totals.balanceMinor)
        )
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
    }

    private fun renderSettings() {
        val farm = currentFarmId?.let { service.loadFarm(it) }
        val canChangeCurrency = farm != null && farm.transactions.isEmpty()
        settingsNoFarmText.visibility = if (farm == null) View.VISIBLE else View.GONE
        settingsCurrencyText.text = farm?.currencyCode ?: ""
        settingsCurrencyText.visibility = if (farm == null) View.GONE else View.VISIBLE
        changeSettingsCurrencyButton.visibility = if (canChangeCurrency) View.VISIBLE else View.GONE
        settingsCurrencyLockedText.visibility =
            if (farm != null && !canChangeCurrency) View.VISIBLE else View.GONE
        syncLanguageSelection()
    }

    private fun onLanguageSelected(language: AppLanguage) {
        if (languagePreferences.load() == language) return
        languagePreferences.save(language)
        applyAppLanguage(language)
    }

    private fun syncLanguageSelection() {
        languageCheckSuppressed = true
        val selected = languagePreferences.load()
        languageFollowDeviceRadio.isChecked = selected == AppLanguage.FOLLOW_DEVICE
        languageEnglishRadio.isChecked = selected == AppLanguage.ENGLISH
        languageNepaliRadio.isChecked = selected == AppLanguage.NEPALI
        languageCheckSuppressed = false
    }

    private fun applyAppLanguage(language: AppLanguage) {
        val tag = language.languageTag
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            applySystemLocales(tag)
        } else {
            AppCompatDelegate.setApplicationLocales(
                if (tag == null) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun applySystemLocales(tag: String?) {
        getSystemService(LocaleManager::class.java).applicationLocales =
            if (tag == null) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
    }

    private fun toggleFarmTools() {
        toolsExpanded = !toolsExpanded
        updateToolsExpansion()
    }

    private fun updateToolsExpansion() {
        farmToolsContainer.visibility = if (toolsExpanded) View.VISIBLE else View.GONE
        farmToolsToggleButton.text = string(
            if (toolsExpanded) R.string.hide_farm_tools_action else R.string.show_farm_tools_action
        )
    }

    private fun scrollEditorIntoView() {
        scrollView.post { scrollView.smoothScrollTo(0, transactionEditorContainer.top) }
    }

    // --- Backup -------------------------------------------------------------

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
                if (editorState != null && isEditorDirty()) {
                    showDiscardDialog { replaceFarmWith(farm) }
                } else {
                    closeEditor()
                    replaceFarmWith(farm)
                }
            }
            .setNegativeButton(string(R.string.action_cancel)) { _, _ ->
                showToast(R.string.toast_import_cancelled)
            }
            .show()
    }

    private fun replaceFarmWith(farm: FarmState) {
        closeEditor()
        store.saveFarm(farm)
        currentFarmId = farm.id
        render()
        showToast(R.string.toast_farm_restored)
    }

    private fun buildImportedFarmSummary(farm: FarmState): String {
        val currencyCode = farm.currencyCode
        val totals = try {
            FarmTotals.of(farm.transactions)
        } catch (exception: ArithmeticException) {
            Log.e(LOG_TAG, "imported farm totals overflow", exception)
            showValidationMessage(FarmUiError.UNEXPECTED.resourceId)
            return ""
        }
        return string(
            R.string.imported_farm_summary_format,
            numberFormatter.format(presentationLocale, farm.entries.size),
            numberFormatter.format(presentationLocale, farm.transactions.size),
            formatMoney(currencyCode, totals.balanceMinor)
        )
    }

    // --- Presentation helpers (also test seams) ------------------------------

    internal fun formattedBalance(currencyCode: String?, balanceMinor: Long): String =
        formatMoney(currencyCode ?: "NPR", balanceMinor)

    internal fun formatMoney(currencyCode: String, amountMinor: Long): String =
        moneyFormatter.format(presentationLocale, currencyCode, amountMinor)

    internal fun formatCount(value: Int): String = numberFormatter.format(presentationLocale, value)

    internal fun displayTransactionTime(transaction: FarmTransaction): String =
        timePresentation.displayDateTime(presentationLocale, deviceZone, transaction.occurredAt)

    internal fun editFieldAmount(currencyCode: String, amountMinor: Long): String =
        moneyFormatter.toEditFieldValue(presentationLocale, currencyCode, amountMinor)

    internal fun editorOccurredAtIsoForTest(): String? =
        editorState?.occurredAt?.atZoneSameInstant(deviceZone)
            ?.toOffsetDateTime()
            ?.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    internal fun pickerUses24HourView(): Boolean = DateFormat.is24HourFormat(this)

    // --- Validation and messages ---------------------------------------------

    private fun showEditorError(error: FarmUiError, field: View?, vararg formatArgs: Any) {
        showValidationMessage(error.resourceId, *formatArgs)
        field?.requestFocus()
    }

    private fun selectedEntryKind(): FarmEntryKind = FarmOrdering.entryKinds[entryKindSpinner.selectedItemPosition]

    private fun selectedTransactionType(): TransactionType =
        if (transactionTypeExpenseRadio.isChecked) TransactionType.EXPENSE else TransactionType.INCOME

    private fun selectedTransactionCategory(): TransactionCategory {
        val type = selectedTransactionType()
        val categories = FarmOrdering.categoriesFor(type)
        val position = transactionCategorySpinner.selectedItemPosition.coerceIn(0, categories.size - 1)
        return categories[position]
    }

    private fun refreshCategoryChoices(type: TransactionType) {
        val categories = FarmOrdering.categoriesFor(type)
        transactionCategorySpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categories.map { FarmLabels.transactionCategory(this, it) }
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
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

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val LOG_TAG = "FarmActivity"
        const val STATE_DESTINATION = "destination"
        const val STATE_LAST_PRIMARY_DESTINATION = "lastPrimaryDestination"
        const val STATE_EDITOR_PREFIX = "editor"
        const val STATE_EDITOR_BASELINE_PREFIX = "editorBaseline"
        const val STATE_EDITOR_OPEN = "editorOpen"
        const val STATE_EDITOR_MODE = "Mode"
        const val STATE_EDITOR_TRANSACTION_ID = "TransactionId"
        const val STATE_EDITOR_TYPE = "Type"
        const val STATE_EDITOR_CATEGORY = "Category"
        const val STATE_EDITOR_AMOUNT = "Amount"
        const val STATE_EDITOR_DESCRIPTION = "Description"
        const val STATE_EDITOR_OCCURRED_AT = "OccurredAt"
        const val STATE_TOOLS_EXPANDED = "toolsExpanded"
    }
}
