package com.susankhya.kisab.ui

import android.app.DatePickerDialog
import android.app.LocaleManager
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.text.InputType
import android.text.format.DateFormat
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.susankhya.kisab.R
import com.susankhya.kisab.domain.FarmEntry
import com.susankhya.kisab.domain.FarmEntryKind
import com.susankhya.kisab.domain.FarmSliceService
import com.susankhya.kisab.domain.FarmState
import com.susankhya.kisab.domain.FarmTotals
import com.susankhya.kisab.domain.FarmTransaction
import com.susankhya.kisab.domain.FarmTransactionDraft
import com.susankhya.kisab.domain.FinancialPeriodPreset
import com.susankhya.kisab.domain.FarmPlanningCalculator
import com.susankhya.kisab.domain.ArithmeticOperation
import com.susankhya.kisab.domain.KisanCalculators
import com.susankhya.kisab.domain.LandUnit
import com.susankhya.kisab.domain.Party
import com.susankhya.kisab.domain.PartyDraft
import com.susankhya.kisab.domain.PartyLedgerEntryType
import com.susankhya.kisab.domain.PartyRole
import com.susankhya.kisab.domain.PaymentStatus
import com.susankhya.kisab.domain.Trade
import com.susankhya.kisab.domain.TradeDraft
import com.susankhya.kisab.domain.TradeType
import com.susankhya.kisab.domain.TransactionCategory
import com.susankhya.kisab.domain.TransactionType
import com.susankhya.kisab.domain.Settlement
import com.susankhya.kisab.domain.SettlementDraft
import com.susankhya.kisab.domain.TradePaymentSummary
import com.susankhya.kisab.domain.compatibleWith
import com.susankhya.kisab.domain.paymentSummaryFor
import com.susankhya.kisab.domain.transactionsNewestFirst
import com.susankhya.kisab.persistence.AndroidStorageAccessFrameworkBackupFileAdapter
import com.susankhya.kisab.persistence.FarmBackupCodec
import com.susankhya.kisab.persistence.FarmBackupException
import com.susankhya.kisab.persistence.FarmBackupFileAdapter
import com.susankhya.kisab.persistence.SharedPreferencesAppLanguagePreferences
import com.susankhya.kisab.persistence.SharedPreferencesAppTextSizePreferences
import com.susankhya.kisab.persistence.SharedPreferencesFarmStore
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.math.BigDecimal
import java.util.IdentityHashMap
import java.util.Locale

class FarmActivity : AppCompatActivity() {
    private lateinit var store: SharedPreferencesFarmStore
    private lateinit var service: FarmSliceService
    internal lateinit var backupFileAdapter: FarmBackupFileAdapter

    private enum class Destination { HOME, HISAB_KITAB, HISAB, SETTINGS }

    private val moneyFormatter = MoneyFormatter()
    private val moneyInputParser = MoneyInputParser(moneyFormatter)
    private val numberFormatter = NumberFormatter()
    private val decimalValueFormatter = DecimalValueFormatter()
    private val timePresentation = TimePresentation()

    private val presentationLocale: java.util.Locale
        get() = PresentationLocale.presentationLocale(resources.configuration.locales.get(0))

    private val deviceZone: ZoneId
        get() = ZoneId.systemDefault()

    private lateinit var scrollView: ScrollView
    private lateinit var shellRoot: LinearLayout
    private lateinit var shellAppBar: LinearLayout
    private lateinit var shellTitle: TextView
    private lateinit var shellMenuButton: ImageButton
    private lateinit var bottomNavigation: LinearLayout
    private lateinit var navHomeItem: LinearLayout
    private lateinit var navHisabKitabItem: LinearLayout
    private lateinit var navHisabItem: LinearLayout
    private lateinit var hisabKitabScreen: ScrollView
    private lateinit var hisabScreen: ScrollView
    private lateinit var settingsScreen: ScrollView

    private lateinit var hisabNoFarmText: TextView
    private lateinit var hisabNoPartiesText: TextView
    private lateinit var hisabCalculatorContainer: LinearLayout
    private lateinit var hisabPartySpinner: Spinner
    private lateinit var hisabPartyRoleText: TextView
    private lateinit var hisabPeriodSpinner: Spinner
    private lateinit var hisabSalesText: TextView
    private lateinit var hisabPurchasesText: TextView
    private lateinit var hisabPaymentsReceivedText: TextView
    private lateinit var hisabPaymentsMadeText: TextView
    private lateinit var hisabActivityEmptyText: TextView
    private lateinit var hisabPositionAsOfText: TextView
    private lateinit var hisabToReceiveText: TextView
    private lateinit var hisabToPayText: TextView
    private lateinit var hisabNetText: TextView
    private lateinit var hisabPositionEmptyText: TextView
    private lateinit var arithmeticFirstInput: EditText
    private lateinit var arithmeticOperationSpinner: Spinner
    private lateinit var arithmeticSecondInput: EditText
    private lateinit var calculateArithmeticButton: Button
    private lateinit var arithmeticResultText: TextView
    private lateinit var profitCostInput: EditText
    private lateinit var profitRevenueInput: EditText
    private lateinit var calculateProfitButton: Button
    private lateinit var profitResultText: TextView
    private lateinit var interestPrincipalInput: EditText
    private lateinit var interestRateInput: EditText
    private lateinit var interestMonthsInput: EditText
    private lateinit var calculateInterestButton: Button
    private lateinit var interestResultText: TextView
    private lateinit var landValueInput: EditText
    private lateinit var landFromUnitSpinner: Spinner
    private lateinit var landToUnitSpinner: Spinner
    private lateinit var convertLandButton: Button
    private lateinit var landResultText: TextView

    // Farm Planning
    private lateinit var farmPlanningCalculatorSpinner: Spinner
    private lateinit var seedCalculatorContainer: LinearLayout
    private lateinit var seedAreaInput: EditText
    private lateinit var seedLandUnitSpinner: Spinner
    private lateinit var seedRateInput: EditText
    private lateinit var seedPriceInput: EditText
    private lateinit var calculateSeedButton: Button
    private lateinit var seedResultText: TextView
    private lateinit var fertilizerCalculatorContainer: LinearLayout
    private lateinit var fertilizerAreaInput: EditText
    private lateinit var fertilizerLandUnitSpinner: Spinner
    private lateinit var fertilizerRateInput: EditText
    private lateinit var fertilizerPriceInput: EditText
    private lateinit var calculateFertilizerButton: Button
    private lateinit var fertilizerResultText: TextView
    private lateinit var feedCalculatorContainer: LinearLayout
    private lateinit var feedAnimalCountInput: EditText
    private lateinit var feedKgPerAnimalInput: EditText
    private lateinit var feedDaysInput: EditText
    private lateinit var feedPriceInput: EditText
    private lateinit var calculateFeedButton: Button
    private lateinit var feedResultText: TextView
    private lateinit var milkCalculatorContainer: LinearLayout
    private lateinit var milkAnimalCountInput: EditText
    private lateinit var milkLitresPerAnimalInput: EditText
    private lateinit var milkDaysInput: EditText
    private lateinit var milkPriceInput: EditText
    private lateinit var calculateMilkButton: Button
    private lateinit var milkResultText: TextView
    private lateinit var cropYieldCalculatorContainer: LinearLayout
    private lateinit var cropYieldAreaInput: EditText
    private lateinit var cropYieldLandUnitSpinner: Spinner
    private lateinit var cropYieldRateInput: EditText
    private lateinit var cropYieldPriceInput: EditText
    private lateinit var calculateCropYieldButton: Button
    private lateinit var cropYieldResultText: TextView
    private var farmPlanningCalculator: FarmPlanningCalculator = FarmPlanningCalculator.SEED
    private var farmPlanningSelectionSuppressed = false

    private var hisabPartyChoices: List<Party> = emptyList()
    private var hisabSelectedPartyId: String? = null
    private var hisabPeriodPreset: FinancialPeriodPreset = FinancialPeriodPreset.THIS_MONTH
    private var hisabSelectionSuppressed = false

    private lateinit var partiesEmptyText: TextView
    private lateinit var partiesContainer: LinearLayout
    private lateinit var addPartyButton: Button
    private lateinit var tradesSectionLabel: TextView
    private lateinit var partiesSectionLabel: TextView
    private lateinit var partyKhataContainer: androidx.appcompat.widget.LinearLayoutCompat
    private lateinit var partyKhataTitle: TextView
    private lateinit var partyKhataRoleText: TextView
    private lateinit var partyKhataToReceiveText: TextView
    private lateinit var partyKhataToPayText: TextView
    private lateinit var partyKhataNetText: TextView
    private lateinit var khataNewSaleButton: Button
    private lateinit var khataNewPurchaseButton: Button
    private lateinit var khataEditPartyButton: Button
    private lateinit var closeKhataButton: Button
    private lateinit var khataEmptyText: TextView
    private lateinit var khataEntriesContainer: LinearLayout
    private lateinit var partyEditorTitle: TextView
    private lateinit var partyNameInput: EditText
    private lateinit var partyRoleSpinner: Spinner
    private lateinit var partyContactInput: EditText
    private lateinit var partyNotesInput: EditText
    private lateinit var partyValidationMessageText: TextView
    private lateinit var savePartyButton: Button
    private lateinit var cancelPartyButton: Button
    private lateinit var deletePartyButton: Button

    private lateinit var newSaleButton: Button
    private lateinit var newPurchaseButton: Button
    private lateinit var hisabSummaryContainer: LinearLayout
    private lateinit var toReceiveText: TextView
    private lateinit var toPayText: TextView
    private lateinit var financialOverviewContainer: androidx.appcompat.widget.LinearLayoutCompat
    private lateinit var overviewPeriodSpinner: Spinner
    private lateinit var overviewCashIncomeText: TextView
    private lateinit var overviewCashExpenseText: TextView
    private lateinit var overviewCashNetText: TextView
    private lateinit var overviewCashEmptyText: TextView
    private lateinit var overviewSalesText: TextView
    private lateinit var overviewPurchasesText: TextView
    private lateinit var overviewPaymentsReceivedText: TextView
    private lateinit var overviewPaymentsMadeText: TextView
    private lateinit var overviewTradeEmptyText: TextView
    private lateinit var overviewPositionAsOfText: TextView
    private lateinit var overviewReceivableText: TextView
    private lateinit var overviewPayableText: TextView
    private lateinit var overviewNetPositionText: TextView
    private lateinit var overviewPositionEmptyText: TextView
    private lateinit var overviewTrendEmptyText: TextView
    private lateinit var overviewTrendContainer: LinearLayout
    private var overviewPeriodPreset: FinancialPeriodPreset = FinancialPeriodPreset.THIS_MONTH
    private lateinit var tradeEditorContainer: androidx.appcompat.widget.LinearLayoutCompat
    private lateinit var tradeEditorTitle: TextView
    private lateinit var tradePartySpinner: Spinner
    private lateinit var tradeTotalInput: EditText
    private lateinit var tradeStatusPaidRadio: RadioButton
    private lateinit var tradeStatusPartialRadio: RadioButton
    private lateinit var tradeStatusUnpaidRadio: RadioButton
    private lateinit var tradePaidLabel: TextView
    private lateinit var tradePaidInput: EditText
    private lateinit var tradeDescriptionInput: EditText
    private lateinit var tradeDateTimeText: TextView
    private lateinit var changeTradeDateTimeButton: Button
    private lateinit var tradeValidationMessageText: TextView
    private lateinit var saveTradeButton: Button
    private lateinit var cancelTradeButton: Button
    private lateinit var deleteTradeButton: Button
    private lateinit var tradePaidDueText: TextView
    private lateinit var managePaymentsButton: Button
    private lateinit var tradeStatusLabel: TextView
    private lateinit var tradeStatusRadioGroup: RadioGroup
    private lateinit var tradesEmptyText: TextView
    private lateinit var tradesContainer: LinearLayout

    private lateinit var settlementEditorContainer: androidx.appcompat.widget.LinearLayoutCompat
    private lateinit var settlementEditorTitle: TextView
    private lateinit var settlementTradeSummaryText: TextView
    private lateinit var settlementPaidDueText: TextView
    private lateinit var settlementEditorFormTitle: TextView
    private lateinit var settlementAmountLabel: TextView
    private lateinit var settlementAmountInput: EditText
    private lateinit var settlementDateTimeLabel: TextView
    private lateinit var settlementDateTimeText: TextView
    private lateinit var changeSettlementDateTimeButton: Button
    private lateinit var settlementNoteInput: EditText
    private lateinit var settlementValidationMessageText: TextView
    private lateinit var saveSettlementButton: Button
    private lateinit var cancelSettlementFormButton: Button
    private lateinit var deleteSettlementButton: Button
    private lateinit var settlementsHistoryLabel: TextView
    private lateinit var settlementsEmptyText: TextView
    private lateinit var settlementsContainer: LinearLayout
    private lateinit var addSettlementButton: Button
    private lateinit var doneSettlementsButton: Button

    private lateinit var settingsCurrencyText: TextView
    private lateinit var changeSettingsCurrencyButton: Button
    private lateinit var settingsCurrencyLockedText: TextView
    private lateinit var settingsNoFarmText: TextView
    private lateinit var settingsFarmNameLabel: TextView
    private lateinit var settingsFarmNameText: TextView
    private lateinit var settingsDataNoFarmText: TextView
    private lateinit var settingsExportBackupButton: Button
    private lateinit var settingsImportBackupButton: Button
    private lateinit var settingsAboutVersionText: TextView
    private lateinit var settingsAppearanceSection: TextView
    private lateinit var settingsFarmSection: TextView
    private lateinit var settingsDataSection: TextView
    private lateinit var settingsTextSizeValueText: TextView
    private lateinit var settingsTextSizeSeekBar: SeekBar
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
    private lateinit var textSizePreferences: AppTextSizePreferences
    private var languageCheckSuppressed = false
    private var textSizeChangeSuppressed = false
    private var pendingSettingsScrollToSection: View? = null
    private val originalTextSizesPx = IdentityHashMap<TextView, Float>()

    private var currentFarmId: String? = null
    private var pendingExportContent: String? = null

    private var currentDestination: Destination = Destination.HOME
    private var lastPrimaryDestination: Destination = Destination.HOME
    private var editorState: TransactionEditorState? = null
    private var editorBaseline: TransactionEditorState? = null
    private var toolsExpanded: Boolean = false
    private var syncTypeListenersSuppressed = false
    private var syncTradeStatusListener = false
    private var editingPartyId: String? = null
    private var tradeEditorState: TradeEditorState? = null
    private var tradeEditorBaseline: TradeEditorState? = null
    private var tradeParties: List<Party?> = emptyList()
    private var settlementEditorState: SettlementEditorState? = null
    private var settlementEditorBaseline: SettlementEditorState? = null
    private var settlementTargetTradeId: String? = null
    private var khataPartyId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        store = SharedPreferencesFarmStore(applicationContext)
        service = FarmSliceService(store)
        backupFileAdapter = AndroidStorageAccessFrameworkBackupFileAdapter(applicationContext)
        languagePreferences = SharedPreferencesAppLanguagePreferences(applicationContext)
        textSizePreferences = SharedPreferencesAppTextSizePreferences(applicationContext)

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
        applyShellSystemBarInsets()
        wireListeners()
        syncTextSizeSelection()
        applyAppTextSize()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (editorState != null) {
                    if (isEditorDirty()) {
                        showDiscardDialog { closeEditor() }
                    } else {
                        closeEditor()
                    }
                } else if (currentDestination == Destination.HISAB_KITAB && settlementTargetTradeId != null) {
                    if (settlementEditorState != null) {
                        if (isSettlementEditorDirty()) {
                            showDiscardDialog { cancelSettlementForm() }
                        } else {
                            cancelSettlementForm()
                        }
                    } else {
                        closeSettlementEditor()
                    }
                } else if (currentDestination == Destination.HISAB_KITAB && tradeEditorState != null) {
                    if (isTradeEditorDirty()) {
                        showDiscardDialog { closeTradeEditor() }
                    } else {
                        closeTradeEditor()
                    }
                } else if (currentDestination == Destination.HISAB_KITAB && editingPartyId != null) {
                    if (isPartyEditorDirty()) {
                        showDiscardDialog { closePartyEditor() }
                    } else {
                        closePartyEditor()
                    }
                } else if (currentDestination == Destination.HISAB_KITAB && khataPartyId != null) {
                    closePartyKhata()
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
        restoreOverviewPeriodFrom(savedInstanceState)
        restoreHisabSelectionFrom(savedInstanceState)
        restoreFarmPlanningFrom(savedInstanceState)
        render()
        showDestination(currentDestination)
        restoreEditorFrom(savedInstanceState)
        restoreTradeEditorFrom(savedInstanceState)
        restoreSettlementEditorFrom(savedInstanceState)
        restoreKhataFrom(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_DESTINATION, currentDestination.name)
        outState.putString(STATE_LAST_PRIMARY_DESTINATION, lastPrimaryDestination.name)
        outState.putBoolean(STATE_TOOLS_EXPANDED, toolsExpanded)
        outState.putString(STATE_OVERVIEW_PERIOD_PRESET, overviewPeriodPreset.name)
        outState.putString(STATE_HISAB_PARTY_ID, hisabSelectedPartyId)
        outState.putString(STATE_HISAB_PERIOD_PRESET, hisabPeriodPreset.name)
        outState.putString(STATE_FARM_PLANNING_CALCULATOR, farmPlanningCalculator.name)
        val state = currentEditorState()
        if (state != null) {
            outState.putBoolean(STATE_EDITOR_OPEN, true)
            writeEditorState(outState, STATE_EDITOR_PREFIX, state)
            editorBaseline?.let { writeEditorState(outState, STATE_EDITOR_BASELINE_PREFIX, it) }
        }
        val tradeState = currentTradeEditorState()
        if (tradeState != null) {
            outState.putBoolean(STATE_TRADE_EDITOR_OPEN, true)
            writeTradeEditorState(outState, STATE_TRADE_EDITOR_PREFIX, tradeState)
            tradeEditorBaseline?.let {
                writeTradeEditorState(outState, STATE_TRADE_EDITOR_BASELINE_PREFIX, it)
            }
        }
        settlementTargetTradeId?.let { target ->
            outState.putString(STATE_SETTLEMENT_TARGET_TRADE_ID, target)
            val settlementState = currentSettlementEditorState()
            if (settlementState != null) {
                outState.putBoolean(STATE_SETTLEMENT_EDITOR_OPEN, true)
                writeSettlementEditorState(outState, STATE_SETTLEMENT_EDITOR_PREFIX, settlementState)
                settlementEditorBaseline?.let {
                    writeSettlementEditorState(outState, STATE_SETTLEMENT_EDITOR_BASELINE_PREFIX, it)
                }
            }
        }
        khataPartyId?.let { outState.putString(STATE_KHATA_PARTY_ID, it) }
    }

    private fun restoreDestinationFrom(bundle: Bundle?) {
        val name = bundle?.getString(STATE_DESTINATION) ?: return
        val saved = runCatching { Destination.valueOf(name) }.getOrNull()
        if (saved != null) currentDestination = saved
        val primaryName = bundle?.getString(STATE_LAST_PRIMARY_DESTINATION) ?: return
        val primary = runCatching { Destination.valueOf(primaryName) }.getOrNull()
        if (primary != null) lastPrimaryDestination = primary
    }

    /**
     * Restores the selected financial-overview period across recreation. A
     * missing, unknown, or otherwise unreadable value is a no-op, leaving the
     * declaration default ([FinancialPeriodPreset.THIS_MONTH]) in place — the
     * backward-compatible default. The preset field is set *before* the Spinner
     * selection is synchronized so the selection listener (which re-renders
     * only when the preset actually changes) does not render prematurely.
     */
    private fun restoreOverviewPeriodFrom(bundle: Bundle?) {
        val name = bundle?.getString(STATE_OVERVIEW_PERIOD_PRESET) ?: return
        val saved = runCatching { FinancialPeriodPreset.valueOf(name) }.getOrNull() ?: return
        overviewPeriodPreset = saved
        val position = FarmOrdering.financialPeriodPresets.indexOf(saved)
        if (position >= 0) {
            overviewPeriodSpinner.setSelection(position)
        }
    }

    private fun restoreHisabSelectionFrom(bundle: Bundle?) {
        hisabSelectedPartyId = bundle?.getString(STATE_HISAB_PARTY_ID)
        val presetName = bundle?.getString(STATE_HISAB_PERIOD_PRESET)
        val savedPreset = presetName?.let {
            runCatching { FinancialPeriodPreset.valueOf(it) }.getOrNull()
        }
        if (savedPreset != null) hisabPeriodPreset = savedPreset
        val periodPosition = FarmOrdering.financialPeriodPresets.indexOf(hisabPeriodPreset)
        if (periodPosition >= 0) hisabPeriodSpinner.setSelection(periodPosition)
    }

    private fun restoreFarmPlanningFrom(bundle: Bundle?) {
        val name = bundle?.getString(STATE_FARM_PLANNING_CALCULATOR) ?: return
        val saved = runCatching { FarmPlanningCalculator.valueOf(name) }.getOrNull() ?: return
        farmPlanningCalculator = saved
    }

    private fun bindViews() {
        shellRoot = findViewById(R.id.shellRoot)
        shellAppBar = findViewById(R.id.shellAppBar)
        scrollView = findViewById(R.id.scrollView)
        shellTitle = findViewById(R.id.shellTitle)
        shellMenuButton = findViewById(R.id.shellMenuButton)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        navHomeItem = findViewById(R.id.navHomeItem)
        navHisabKitabItem = findViewById(R.id.navHisabKitabItem)
        navHisabItem = findViewById(R.id.navHisabItem)
        hisabKitabScreen = findViewById(R.id.hisabKitabScreen)
        hisabScreen = findViewById(R.id.hisabScreen)
        settingsScreen = findViewById(R.id.settingsScreen)
        hisabNoFarmText = findViewById(R.id.hisabNoFarmText)
        hisabNoPartiesText = findViewById(R.id.hisabNoPartiesText)
        hisabCalculatorContainer = findViewById(R.id.hisabCalculatorContainer)
        hisabPartySpinner = findViewById(R.id.hisabPartySpinner)
        hisabPartyRoleText = findViewById(R.id.hisabPartyRoleText)
        hisabPeriodSpinner = findViewById(R.id.hisabPeriodSpinner)
        hisabSalesText = findViewById(R.id.hisabSalesText)
        hisabPurchasesText = findViewById(R.id.hisabPurchasesText)
        hisabPaymentsReceivedText = findViewById(R.id.hisabPaymentsReceivedText)
        hisabPaymentsMadeText = findViewById(R.id.hisabPaymentsMadeText)
        hisabActivityEmptyText = findViewById(R.id.hisabActivityEmptyText)
        hisabPositionAsOfText = findViewById(R.id.hisabPositionAsOfText)
        hisabToReceiveText = findViewById(R.id.hisabToReceiveText)
        hisabToPayText = findViewById(R.id.hisabToPayText)
        hisabNetText = findViewById(R.id.hisabNetText)
        hisabPositionEmptyText = findViewById(R.id.hisabPositionEmptyText)
        arithmeticFirstInput = findViewById(R.id.arithmeticFirstInput)
        arithmeticOperationSpinner = findViewById(R.id.arithmeticOperationSpinner)
        arithmeticSecondInput = findViewById(R.id.arithmeticSecondInput)
        calculateArithmeticButton = findViewById(R.id.calculateArithmeticButton)
        arithmeticResultText = findViewById(R.id.arithmeticResultText)
        profitCostInput = findViewById(R.id.profitCostInput)
        profitRevenueInput = findViewById(R.id.profitRevenueInput)
        calculateProfitButton = findViewById(R.id.calculateProfitButton)
        profitResultText = findViewById(R.id.profitResultText)
        interestPrincipalInput = findViewById(R.id.interestPrincipalInput)
        interestRateInput = findViewById(R.id.interestRateInput)
        interestMonthsInput = findViewById(R.id.interestMonthsInput)
        calculateInterestButton = findViewById(R.id.calculateInterestButton)
        interestResultText = findViewById(R.id.interestResultText)
        landValueInput = findViewById(R.id.landValueInput)
        landFromUnitSpinner = findViewById(R.id.landFromUnitSpinner)
        landToUnitSpinner = findViewById(R.id.landToUnitSpinner)
        convertLandButton = findViewById(R.id.convertLandButton)
        landResultText = findViewById(R.id.landResultText)

        // Farm Planning
        farmPlanningCalculatorSpinner = findViewById(R.id.farmPlanningCalculatorSpinner)
        seedCalculatorContainer = findViewById(R.id.seedCalculatorContainer)
        seedAreaInput = findViewById(R.id.seedAreaInput)
        seedLandUnitSpinner = findViewById(R.id.seedLandUnitSpinner)
        seedRateInput = findViewById(R.id.seedRateInput)
        seedPriceInput = findViewById(R.id.seedPriceInput)
        calculateSeedButton = findViewById(R.id.calculateSeedButton)
        seedResultText = findViewById(R.id.seedResultText)
        fertilizerCalculatorContainer = findViewById(R.id.fertilizerCalculatorContainer)
        fertilizerAreaInput = findViewById(R.id.fertilizerAreaInput)
        fertilizerLandUnitSpinner = findViewById(R.id.fertilizerLandUnitSpinner)
        fertilizerRateInput = findViewById(R.id.fertilizerRateInput)
        fertilizerPriceInput = findViewById(R.id.fertilizerPriceInput)
        calculateFertilizerButton = findViewById(R.id.calculateFertilizerButton)
        fertilizerResultText = findViewById(R.id.fertilizerResultText)
        feedCalculatorContainer = findViewById(R.id.feedCalculatorContainer)
        feedAnimalCountInput = findViewById(R.id.feedAnimalCountInput)
        feedKgPerAnimalInput = findViewById(R.id.feedKgPerAnimalInput)
        feedDaysInput = findViewById(R.id.feedDaysInput)
        feedPriceInput = findViewById(R.id.feedPriceInput)
        calculateFeedButton = findViewById(R.id.calculateFeedButton)
        feedResultText = findViewById(R.id.feedResultText)
        milkCalculatorContainer = findViewById(R.id.milkCalculatorContainer)
        milkAnimalCountInput = findViewById(R.id.milkAnimalCountInput)
        milkLitresPerAnimalInput = findViewById(R.id.milkLitresPerAnimalInput)
        milkDaysInput = findViewById(R.id.milkDaysInput)
        milkPriceInput = findViewById(R.id.milkPriceInput)
        calculateMilkButton = findViewById(R.id.calculateMilkButton)
        milkResultText = findViewById(R.id.milkResultText)
        cropYieldCalculatorContainer = findViewById(R.id.cropYieldCalculatorContainer)
        cropYieldAreaInput = findViewById(R.id.cropYieldAreaInput)
        cropYieldLandUnitSpinner = findViewById(R.id.cropYieldLandUnitSpinner)
        cropYieldRateInput = findViewById(R.id.cropYieldRateInput)
        cropYieldPriceInput = findViewById(R.id.cropYieldPriceInput)
        calculateCropYieldButton = findViewById(R.id.calculateCropYieldButton)
        cropYieldResultText = findViewById(R.id.cropYieldResultText)

        arithmeticOperationSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            ArithmeticOperation.values().map { FarmLabels.arithmeticOperation(this, it) }
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        val landLabels = LandUnit.values().map { FarmLabels.landUnit(this, it) }
        landFromUnitSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            landLabels
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        landToUnitSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            landLabels
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        landToUnitSpinner.setSelection(LandUnit.values().indexOf(LandUnit.SQUARE_METRE))

        // Farm Planning spinners
        val calculatorLabels = FarmOrdering.farmPlanningCalculators.map { FarmLabels.farmPlanningCalculator(this, it) }
        farmPlanningCalculatorSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            calculatorLabels
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        farmPlanningCalculatorSpinner.setSelection(FarmOrdering.farmPlanningCalculators.indexOf(farmPlanningCalculator))
        farmPlanningCalculatorSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val calculator = FarmOrdering.farmPlanningCalculators.getOrNull(position)
                if (calculator != null && calculator != farmPlanningCalculator && !farmPlanningSelectionSuppressed) {
                    farmPlanningCalculator = calculator
                    renderFarmPlanning()
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }
        seedLandUnitSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            landLabels
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        seedLandUnitSpinner.setSelection(LandUnit.values().indexOf(LandUnit.SQUARE_METRE))
        fertilizerLandUnitSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            landLabels
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        fertilizerLandUnitSpinner.setSelection(LandUnit.values().indexOf(LandUnit.SQUARE_METRE))
        cropYieldLandUnitSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            landLabels
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        cropYieldLandUnitSpinner.setSelection(LandUnit.values().indexOf(LandUnit.SQUARE_METRE))

        hisabPeriodSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            FarmOrdering.financialPeriodPresets.map { FarmLabels.financialPeriodPreset(this, it) }
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        partiesEmptyText = findViewById(R.id.partiesEmptyText)
        partiesContainer = findViewById(R.id.partiesContainer)
        addPartyButton = findViewById(R.id.addPartyButton)
        tradesSectionLabel = findViewById(R.id.tradesSectionLabel)
        partiesSectionLabel = findViewById(R.id.partiesSectionLabel)
        partyKhataContainer = findViewById(R.id.partyKhataContainer)
        partyKhataTitle = findViewById(R.id.partyKhataTitle)
        partyKhataRoleText = findViewById(R.id.partyKhataRoleText)
        partyKhataToReceiveText = findViewById(R.id.partyKhataToReceiveText)
        partyKhataToPayText = findViewById(R.id.partyKhataToPayText)
        partyKhataNetText = findViewById(R.id.partyKhataNetText)
        khataNewSaleButton = findViewById(R.id.khataNewSaleButton)
        khataNewPurchaseButton = findViewById(R.id.khataNewPurchaseButton)
        khataEditPartyButton = findViewById(R.id.khataEditPartyButton)
        closeKhataButton = findViewById(R.id.closeKhataButton)
        khataEmptyText = findViewById(R.id.khataEmptyText)
        khataEntriesContainer = findViewById(R.id.khataEntriesContainer)
        partyEditorTitle = findViewById(R.id.partyEditorTitle)
        partyNameInput = findViewById(R.id.partyNameInput)
        partyRoleSpinner = findViewById(R.id.partyRoleSpinner)
        partyContactInput = findViewById(R.id.partyContactInput)
        partyNotesInput = findViewById(R.id.partyNotesInput)
        partyValidationMessageText = findViewById(R.id.partyValidationMessageText)
        savePartyButton = findViewById(R.id.savePartyButton)
        cancelPartyButton = findViewById(R.id.cancelPartyButton)
        deletePartyButton = findViewById(R.id.deletePartyButton)
        settingsCurrencyText = findViewById(R.id.settingsCurrencyText)
        changeSettingsCurrencyButton = findViewById(R.id.changeSettingsCurrencyButton)
        settingsCurrencyLockedText = findViewById(R.id.settingsCurrencyLockedText)
        settingsNoFarmText = findViewById(R.id.settingsNoFarmText)
        settingsFarmNameLabel = findViewById(R.id.settingsFarmNameLabel)
        settingsFarmNameText = findViewById(R.id.settingsFarmNameText)
        settingsDataNoFarmText = findViewById(R.id.settingsDataNoFarmText)
        settingsExportBackupButton = findViewById(R.id.settingsExportBackupButton)
        settingsImportBackupButton = findViewById(R.id.settingsImportBackupButton)
        settingsAboutVersionText = findViewById(R.id.settingsAboutVersionText)
        settingsAppearanceSection = findViewById(R.id.settingsAppearanceSection)
        settingsFarmSection = findViewById(R.id.settingsFarmSection)
        settingsDataSection = findViewById(R.id.settingsDataSection)
        settingsTextSizeValueText = findViewById(R.id.settingsTextSizeValueText)
        settingsTextSizeSeekBar = findViewById(R.id.settingsTextSizeSeekBar)
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

        newSaleButton = findViewById(R.id.newSaleButton)
        newPurchaseButton = findViewById(R.id.newPurchaseButton)
        toReceiveText = findViewById(R.id.toReceiveText)
        toPayText = findViewById(R.id.toPayText)
        hisabSummaryContainer = findViewById(R.id.hisabSummaryContainer)
        financialOverviewContainer = findViewById(R.id.financialOverviewContainer)
        overviewPeriodSpinner = findViewById(R.id.overviewPeriodSpinner)
        overviewCashIncomeText = findViewById(R.id.overviewCashIncomeText)
        overviewCashExpenseText = findViewById(R.id.overviewCashExpenseText)
        overviewCashNetText = findViewById(R.id.overviewCashNetText)
        overviewCashEmptyText = findViewById(R.id.overviewCashEmptyText)
        overviewSalesText = findViewById(R.id.overviewSalesText)
        overviewPurchasesText = findViewById(R.id.overviewPurchasesText)
        overviewPaymentsReceivedText = findViewById(R.id.overviewPaymentsReceivedText)
        overviewPaymentsMadeText = findViewById(R.id.overviewPaymentsMadeText)
        overviewTradeEmptyText = findViewById(R.id.overviewTradeEmptyText)
        overviewPositionAsOfText = findViewById(R.id.overviewPositionAsOfText)
        overviewReceivableText = findViewById(R.id.overviewReceivableText)
        overviewPayableText = findViewById(R.id.overviewPayableText)
        overviewNetPositionText = findViewById(R.id.overviewNetPositionText)
        overviewPositionEmptyText = findViewById(R.id.overviewPositionEmptyText)
        overviewTrendEmptyText = findViewById(R.id.overviewTrendEmptyText)
        overviewTrendContainer = findViewById(R.id.overviewTrendContainer)

        overviewPeriodSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            FarmOrdering.financialPeriodPresets.map { FarmLabels.financialPeriodPreset(this, it) }
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        tradeEditorContainer = findViewById(R.id.tradeEditorContainer)
        tradeEditorTitle = findViewById(R.id.tradeEditorTitle)
        tradePartySpinner = findViewById(R.id.tradePartySpinner)
        tradeTotalInput = findViewById(R.id.tradeTotalInput)
        tradeStatusPaidRadio = findViewById(R.id.tradeStatusPaidRadio)
        tradeStatusPartialRadio = findViewById(R.id.tradeStatusPartialRadio)
        tradeStatusUnpaidRadio = findViewById(R.id.tradeStatusUnpaidRadio)
        tradePaidLabel = findViewById(R.id.tradePaidLabel)
        tradePaidInput = findViewById(R.id.tradePaidInput)
        tradeDescriptionInput = findViewById(R.id.tradeDescriptionInput)
        tradeDateTimeText = findViewById(R.id.tradeDateTimeText)
        changeTradeDateTimeButton = findViewById(R.id.changeTradeDateTimeButton)
        tradeValidationMessageText = findViewById(R.id.tradeValidationMessageText)
        saveTradeButton = findViewById(R.id.saveTradeButton)
        cancelTradeButton = findViewById(R.id.cancelTradeButton)
        deleteTradeButton = findViewById(R.id.deleteTradeButton)
        tradePaidDueText = findViewById(R.id.tradePaidDueText)
        managePaymentsButton = findViewById(R.id.managePaymentsButton)
        tradeStatusLabel = findViewById(R.id.tradeStatusLabel)
        tradeStatusRadioGroup = findViewById(R.id.tradeStatusRadioGroup)
        tradesEmptyText = findViewById(R.id.tradesEmptyText)
        tradesContainer = findViewById(R.id.tradesContainer)
        settlementEditorContainer = findViewById(R.id.settlementEditorContainer)
        settlementEditorTitle = findViewById(R.id.settlementEditorTitle)
        settlementTradeSummaryText = findViewById(R.id.settlementTradeSummaryText)
        settlementPaidDueText = findViewById(R.id.settlementPaidDueText)
        settlementEditorFormTitle = findViewById(R.id.settlementEditorFormTitle)
        settlementAmountLabel = findViewById(R.id.settlementAmountLabel)
        settlementAmountInput = findViewById(R.id.settlementAmountInput)
        settlementDateTimeLabel = findViewById(R.id.settlementDateTimeLabel)
        settlementDateTimeText = findViewById(R.id.settlementDateTimeText)
        changeSettlementDateTimeButton = findViewById(R.id.changeSettlementDateTimeButton)
        settlementNoteInput = findViewById(R.id.settlementNoteInput)
        settlementValidationMessageText = findViewById(R.id.settlementValidationMessageText)
        saveSettlementButton = findViewById(R.id.saveSettlementButton)
        cancelSettlementFormButton = findViewById(R.id.cancelSettlementFormButton)
        deleteSettlementButton = findViewById(R.id.deleteSettlementButton)
        settlementsHistoryLabel = findViewById(R.id.settlementsHistoryLabel)
        settlementsEmptyText = findViewById(R.id.settlementsEmptyText)
        settlementsContainer = findViewById(R.id.settlementsContainer)
        addSettlementButton = findViewById(R.id.addSettlementButton)
        doneSettlementsButton = findViewById(R.id.doneSettlementsButton)
    }

    private fun applyShellSystemBarInsets() {
        val baseAppBarTop = shellAppBar.paddingTop
        val baseAppBarBottom = shellAppBar.paddingBottom
        val baseAppBarStart = shellAppBar.paddingStart
        val baseAppBarEnd = shellAppBar.paddingEnd
        val baseNavTop = bottomNavigation.paddingTop
        val baseNavBottom = bottomNavigation.paddingBottom
        val baseNavStart = bottomNavigation.paddingStart
        val baseNavEnd = bottomNavigation.paddingEnd
        ViewCompat.setOnApplyWindowInsetsListener(shellRoot) { _, insets ->
            val statusBarTopInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val navigationBarBottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            ViewCompat.setPaddingRelative(
                shellAppBar,
                baseAppBarStart,
                ShellInsets.appBarTopPadding(baseAppBarTop, statusBarTopInset),
                baseAppBarEnd,
                baseAppBarBottom,
            )
            ViewCompat.setPaddingRelative(
                bottomNavigation,
                baseNavStart,
                baseNavTop,
                baseNavEnd,
                ShellInsets.bottomNavigationBottomPadding(baseNavBottom, navigationBarBottomInset),
            )
            insets
        }
    }

    private fun wireListeners() {
        createFarmButton.setOnClickListener { createFarm() }
        addEntryButton.setOnClickListener { addEntry() }
        exportBackupButton.setOnClickListener { exportBackup() }
        importBackupButton.setOnClickListener { importBackup() }
        settingsExportBackupButton.setOnClickListener { exportBackup() }
        settingsImportBackupButton.setOnClickListener { importBackup() }
        changeSettingsCurrencyButton.setOnClickListener { showFarmCurrencyChooser() }
        settingsTextSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser || textSizeChangeSuppressed) return
                onTextSizeSelected(AppTextSize.MIN_SP + progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        navHomeItem.setOnClickListener { navigateTo(Destination.HOME) }
        navHisabKitabItem.setOnClickListener { navigateTo(Destination.HISAB_KITAB) }
        navHisabItem.setOnClickListener { navigateTo(Destination.HISAB) }
        shellMenuButton.setOnClickListener { showShellMenu() }
        calculateArithmeticButton.setOnClickListener { calculateArithmetic() }
        calculateProfitButton.setOnClickListener { calculateProfit() }
        calculateInterestButton.setOnClickListener { calculateInterest() }
        convertLandButton.setOnClickListener { convertLand() }
        calculateSeedButton.setOnClickListener { calculateSeed() }
        calculateFertilizerButton.setOnClickListener { calculateFertilizer() }
        calculateFeedButton.setOnClickListener { calculateFeed() }
        calculateMilkButton.setOnClickListener { calculateMilk() }
        calculateCropYieldButton.setOnClickListener { calculateCropYield() }

        hisabPartySpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (hisabSelectionSuppressed) return
                val selected = hisabPartyChoices.getOrNull(position)?.id ?: return
                if (selected != hisabSelectedPartyId) {
                    hisabSelectedPartyId = selected
                    renderHisabCalculator()
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }
        hisabPeriodSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val preset = FarmOrdering.financialPeriodPresets.getOrNull(position) ?: return
                if (preset != hisabPeriodPreset) {
                    hisabPeriodPreset = preset
                    renderHisabCalculator()
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }

        addPartyButton.setOnClickListener {
            confirmDiscardTradeIfNeeded {
                confirmDiscardSettlementIfNeeded { openPartyEditor(null) }
            }
        }
        savePartyButton.setOnClickListener { saveParty() }
        cancelPartyButton.setOnClickListener { closePartyEditor() }
        deletePartyButton.setOnClickListener { confirmDeleteParty() }
        closeKhataButton.setOnClickListener { closePartyKhata() }
        khataEditPartyButton.setOnClickListener {
            val partyId = khataPartyId ?: return@setOnClickListener
            partyKhataContainer.visibility = View.GONE
            openPartyEditor(partyId)
        }
        khataNewSaleButton.setOnClickListener {
            val partyId = khataPartyId ?: return@setOnClickListener
            confirmDiscardSettlementIfNeeded {
                partyKhataContainer.visibility = View.GONE
                openTradeEditorForNew(TradeType.SALE, preselectedPartyId = partyId)
            }
        }
        khataNewPurchaseButton.setOnClickListener {
            val partyId = khataPartyId ?: return@setOnClickListener
            confirmDiscardSettlementIfNeeded {
                partyKhataContainer.visibility = View.GONE
                openTradeEditorForNew(TradeType.PURCHASE, preselectedPartyId = partyId)
            }
        }

        newSaleButton.setOnClickListener { openTradeEditorForNew(TradeType.SALE) }
        newPurchaseButton.setOnClickListener { openTradeEditorForNew(TradeType.PURCHASE) }
        overviewPeriodSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val preset = FarmOrdering.financialPeriodPresets.getOrNull(position)
                if (preset != null && preset != overviewPeriodPreset) {
                    overviewPeriodPreset = preset
                    renderFinancialOverview()
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }
        saveTradeButton.setOnClickListener { saveTrade() }
        cancelTradeButton.setOnClickListener { cancelTradeEditing() }
        deleteTradeButton.setOnClickListener { confirmDeleteTrade() }
        changeTradeDateTimeButton.setOnClickListener { showTradeDateTimePickers() }
        tradeStatusPaidRadio.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) onTradePaymentStatusChanged(PaymentStatus.PAID)
        }
        tradeStatusPartialRadio.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) onTradePaymentStatusChanged(PaymentStatus.PARTIAL)
        }
        tradeStatusUnpaidRadio.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) onTradePaymentStatusChanged(PaymentStatus.UNPAID)
        }
        managePaymentsButton.setOnClickListener { openPaymentsForTradeBeingEdited() }
        addSettlementButton.setOnClickListener { newSettlementForm() }
        doneSettlementsButton.setOnClickListener { closeSettlementEditor() }
        saveSettlementButton.setOnClickListener { saveSettlement() }
        cancelSettlementFormButton.setOnClickListener { cancelSettlementEditing() }
        deleteSettlementButton.setOnClickListener { confirmDeleteSettlement() }
        changeSettlementDateTimeButton.setOnClickListener { showSettlementDateTimePickers() }

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
            scrollSettingsToPendingSection()
            return
        }
        confirmDiscardIfNeeded {
            confirmDiscardSettlementIfNeeded {
                confirmDiscardTradeIfNeeded {
                    confirmDiscardPartyIfNeeded { showDestination(destination) }
                }
            }
        }
    }

    private fun showDestination(destination: Destination) {
        currentDestination = destination
        if (destination != Destination.HISAB_KITAB && khataPartyId != null) {
            closePartyKhata()
        }
        if (destination != Destination.SETTINGS) lastPrimaryDestination = destination
        scrollView.visibility = if (destination == Destination.HOME) View.VISIBLE else View.GONE
        hisabKitabScreen.visibility = if (destination == Destination.HISAB_KITAB) View.VISIBLE else View.GONE
        hisabScreen.visibility = if (destination == Destination.HISAB) View.VISIBLE else View.GONE
        settingsScreen.visibility = if (destination == Destination.SETTINGS) View.VISIBLE else View.GONE
        updateShellTitle()
        updateShellNavigationState()
        if (destination == Destination.SETTINGS) renderSettings()
        if (destination == Destination.HISAB_KITAB) renderHisabKitab()
        if (destination == Destination.HISAB) renderHisabCalculator()
        applyAppTextSize()
        scrollSettingsToPendingSection()
    }

    private fun scrollSettingsToPendingSection() {
        val target = pendingSettingsScrollToSection ?: return
        pendingSettingsScrollToSection = null
        settingsScreen.post { settingsScreen.smoothScrollTo(0, target.top) }
    }

    private fun showShellMenu() {
        PopupMenu(this, shellMenuButton).apply {
            inflate(R.menu.menu_shell_overflow)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menuSettings -> {
                        pendingSettingsScrollToSection = null
                        navigateTo(Destination.SETTINGS)
                        true
                    }
                    R.id.menuBackupRestore -> {
                        pendingSettingsScrollToSection = settingsDataSection
                        navigateTo(Destination.SETTINGS)
                        true
                    }
                    R.id.menuFarmDetails -> {
                        pendingSettingsScrollToSection = settingsFarmSection
                        navigateTo(Destination.SETTINGS)
                        true
                    }
                    R.id.menuAbout -> {
                        showAboutDialog()
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }

    private fun updateShellNavigationState() {
        navHomeItem.isSelected = currentDestination == Destination.HOME
        navHisabKitabItem.isSelected = currentDestination == Destination.HISAB_KITAB
        navHisabItem.isSelected = currentDestination == Destination.HISAB
    }

    private fun calculateArithmetic() {
        val first = calculatorValue(arithmeticFirstInput, allowNegative = true) ?: return
        val second = calculatorValue(arithmeticSecondInput, allowNegative = true) ?: return
        val operation = ArithmeticOperation.values().getOrNull(arithmeticOperationSpinner.selectedItemPosition)
            ?: return
        if (operation == ArithmeticOperation.DIVIDE && second.signum() == 0) {
            arithmeticSecondInput.error = string(R.string.calculator_divide_zero_error)
            return
        }
        val result = KisanCalculators.arithmetic(first, second, operation)
        showCalculatorResult(
            arithmeticResultText,
            string(R.string.result_format, formatCalculatorValue(result))
        )
    }

    private fun calculateProfit() {
        val cost = calculatorValue(profitCostInput) ?: return
        val revenue = calculatorValue(profitRevenueInput) ?: return
        val result = KisanCalculators.profit(cost, revenue)
        val label = when (result.amount.signum()) {
            1 -> string(R.string.profit_label)
            -1 -> string(R.string.loss_label)
            else -> string(R.string.no_profit_loss_label)
        }
        showCalculatorResult(
            profitResultText,
            string(
                R.string.profit_result_format,
                label,
                formatCalculatorValue(result.amount.abs()),
                formatCalculatorPercent(result.marginPercent),
                formatCalculatorPercent(result.markupPercent)
            )
        )
    }

    private fun calculateInterest() {
        val principal = calculatorValue(interestPrincipalInput) ?: return
        val rate = calculatorValue(interestRateInput) ?: return
        val months = calculatorValue(interestMonthsInput) ?: return
        val result = KisanCalculators.simpleInterest(principal, rate, months)
        showCalculatorResult(
            interestResultText,
            string(
                R.string.interest_result_format,
                formatCalculatorValue(result.interest),
                formatCalculatorValue(result.total)
            )
        )
    }

    private fun convertLand() {
        val value = calculatorValue(landValueInput) ?: return
        val units = LandUnit.values()
        val from = units.getOrNull(landFromUnitSpinner.selectedItemPosition) ?: return
        val to = units.getOrNull(landToUnitSpinner.selectedItemPosition) ?: return
        val result = KisanCalculators.convertLand(value, from, to)
        showCalculatorResult(
            landResultText,
            string(
                R.string.land_result_format,
                formatCalculatorValue(value),
                FarmLabels.landUnit(this, from),
                formatCalculatorValue(result),
                FarmLabels.landUnit(this, to)
            )
        )
    }

    private fun calculateSeed() {
        val area = calculatorValue(seedAreaInput) ?: return
        val rate = calculatorValue(seedRateInput) ?: return
        val price = calculatorValue(seedPriceInput) ?: return
        LandUnit.values().getOrNull(seedLandUnitSpinner.selectedItemPosition) ?: return
        val result = KisanCalculators.seedQuantityAndCost(area, rate, price)
        showCalculatorResult(
            seedResultText,
            string(
                R.string.seed_result_format,
                formatCalculatorValue(result.quantityKg),
                formatCalculatorValue(result.totalCost)
            )
        )
    }

    private fun calculateFertilizer() {
        val area = calculatorValue(fertilizerAreaInput) ?: return
        val rate = calculatorValue(fertilizerRateInput) ?: return
        val price = calculatorValue(fertilizerPriceInput) ?: return
        LandUnit.values().getOrNull(fertilizerLandUnitSpinner.selectedItemPosition) ?: return
        val result = KisanCalculators.fertilizerQuantityAndCost(area, rate, price)
        showCalculatorResult(
            fertilizerResultText,
            string(
                R.string.fertilizer_result_format,
                formatCalculatorValue(result.quantityKg),
                formatCalculatorValue(result.totalCost)
            )
        )
    }

    private fun calculateFeed() {
        feedAnimalCountInput.error = null
        val animalCount = decimalValueFormatter.parseNonNegativeWhole(
            presentationLocale,
            feedAnimalCountInput.text.toString()
        )
        if (animalCount == null) {
            feedAnimalCountInput.error = string(R.string.calculator_input_error)
            feedAnimalCountInput.requestFocus()
            return
        }
        val rate = calculatorValue(feedKgPerAnimalInput) ?: return
        val days = calculatorValue(feedDaysInput) ?: return
        val price = calculatorValue(feedPriceInput) ?: return
        val result = KisanCalculators.feedRequirementAndCost(animalCount, rate, days, price)
        showCalculatorResult(
            feedResultText,
            string(
                R.string.feed_result_format,
                formatCalculatorValue(result.totalKg),
                formatCalculatorValue(result.totalCost)
            )
        )
    }

    private fun calculateMilk() {
        milkAnimalCountInput.error = null
        val animalCount = decimalValueFormatter.parseNonNegativeWhole(
            presentationLocale,
            milkAnimalCountInput.text.toString()
        )
        if (animalCount == null) {
            milkAnimalCountInput.error = string(R.string.calculator_input_error)
            milkAnimalCountInput.requestFocus()
            return
        }
        val rate = calculatorValue(milkLitresPerAnimalInput) ?: return
        val days = calculatorValue(milkDaysInput) ?: return
        val price = calculatorValue(milkPriceInput) ?: return
        val result = KisanCalculators.milkProductionAndRevenue(animalCount, rate, days, price)
        showCalculatorResult(
            milkResultText,
            string(
                R.string.milk_result_format,
                formatCalculatorValue(result.totalLitres),
                formatCalculatorValue(result.revenue)
            )
        )
    }

    private fun calculateCropYield() {
        val area = calculatorValue(cropYieldAreaInput) ?: return
        val rate = calculatorValue(cropYieldRateInput) ?: return
        val price = calculatorValue(cropYieldPriceInput) ?: return
        LandUnit.values().getOrNull(cropYieldLandUnitSpinner.selectedItemPosition) ?: return
        val result = KisanCalculators.cropYieldAndRevenue(area, rate, price)
        showCalculatorResult(
            cropYieldResultText,
            string(
                R.string.crop_yield_result_format,
                formatCalculatorValue(result.totalKg),
                formatCalculatorValue(result.revenue)
            )
        )
    }

    private fun renderFarmPlanning() {
        farmPlanningSelectionSuppressed = true
        farmPlanningCalculatorSpinner.setSelection(FarmOrdering.farmPlanningCalculators.indexOf(farmPlanningCalculator))
        farmPlanningSelectionSuppressed = false

        seedCalculatorContainer.visibility = if (farmPlanningCalculator == FarmPlanningCalculator.SEED) View.VISIBLE else View.GONE
        fertilizerCalculatorContainer.visibility = if (farmPlanningCalculator == FarmPlanningCalculator.FERTILIZER) View.VISIBLE else View.GONE
        feedCalculatorContainer.visibility = if (farmPlanningCalculator == FarmPlanningCalculator.FEED) View.VISIBLE else View.GONE
        milkCalculatorContainer.visibility = if (farmPlanningCalculator == FarmPlanningCalculator.MILK) View.VISIBLE else View.GONE
        cropYieldCalculatorContainer.visibility = if (farmPlanningCalculator == FarmPlanningCalculator.CROP_YIELD) View.VISIBLE else View.GONE

    }

    private fun calculatorValue(input: EditText, allowNegative: Boolean = false): BigDecimal? {
        input.error = null
        val value = decimalValueFormatter.parse(presentationLocale, input.text.toString())
        if (value == null || (!allowNegative && value.signum() < 0)) {
            input.error = string(
                if (allowNegative) R.string.calculator_number_error else R.string.calculator_input_error
            )
            input.requestFocus()
            return null
        }
        return value
    }

    private fun formatCalculatorValue(value: BigDecimal): String =
        decimalValueFormatter.format(presentationLocale, value)

    private fun formatCalculatorPercent(value: BigDecimal?): String = value?.let {
        string(R.string.percent_value_format, formatCalculatorValue(it))
    } ?: string(R.string.not_available_short)

    private fun showCalculatorResult(target: TextView, result: String) {
        target.text = result
        target.visibility = View.VISIBLE
    }

    private fun renderHisabCalculator() {
        renderFarmPlanning()
        val farmId = currentFarmId
        if (farmId == null) {
            hisabNoFarmText.visibility = View.VISIBLE
            hisabNoPartiesText.visibility = View.GONE
            hisabCalculatorContainer.visibility = View.GONE
            return
        }

        val parties = service.parties(farmId)
            .filter { it.role != PartyRole.OTHER }
            .sortedWith(compareBy<Party> { it.name.lowercase(presentationLocale) }.thenBy { it.id })
        if (parties.isEmpty()) {
            hisabPartyChoices = emptyList()
            hisabSelectedPartyId = null
            hisabNoFarmText.visibility = View.GONE
            hisabNoPartiesText.visibility = View.VISIBLE
            hisabCalculatorContainer.visibility = View.GONE
            return
        }

        hisabNoFarmText.visibility = View.GONE
        hisabNoPartiesText.visibility = View.GONE
        hisabCalculatorContainer.visibility = View.VISIBLE
        hisabPartyChoices = parties
        val selectedParty = parties.firstOrNull { it.id == hisabSelectedPartyId } ?: parties.first()
        hisabSelectedPartyId = selectedParty.id

        hisabSelectionSuppressed = true
        hisabPartySpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            parties.map { it.name }
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        hisabPartySpinner.setSelection(parties.indexOfFirst { it.id == selectedParty.id })
        hisabSelectionSuppressed = false

        val result = try {
            service.partyHisab(
                farmId = farmId,
                partyId = selectedParty.id,
                preset = hisabPeriodPreset,
                now = OffsetDateTime.now(deviceZone),
                zone = deviceZone
            )
        } catch (exception: RuntimeException) {
            showUnexpectedFailure(exception, "render party hisab")
            return
        }

        val currency = currentFarmCurrency()
        val activity = result.activity
        val position = result.position
        val hasActivity = activity.salesMinor > 0L || activity.purchasesMinor > 0L ||
            activity.paymentsReceivedMinor > 0L || activity.paymentsMadeMinor > 0L
        val hasPosition = position.toReceiveMinor > 0L || position.toPayMinor > 0L

        hisabPartyRoleText.text = string(
            R.string.hisab_party_role_format,
            FarmLabels.partyRole(this, result.party.role)
        )
        hisabSalesText.text = string(R.string.overview_sales_format, formatMoney(currency, activity.salesMinor))
        hisabPurchasesText.text = string(
            R.string.overview_purchases_format,
            formatMoney(currency, activity.purchasesMinor)
        )
        hisabPaymentsReceivedText.text = string(
            R.string.overview_payments_received_format,
            formatMoney(currency, activity.paymentsReceivedMinor)
        )
        hisabPaymentsMadeText.text = string(
            R.string.overview_payments_made_format,
            formatMoney(currency, activity.paymentsMadeMinor)
        )
        hisabSalesText.visibility = if (hasActivity) View.VISIBLE else View.GONE
        hisabPurchasesText.visibility = if (hasActivity) View.VISIBLE else View.GONE
        hisabPaymentsReceivedText.visibility = if (hasActivity) View.VISIBLE else View.GONE
        hisabPaymentsMadeText.visibility = if (hasActivity) View.VISIBLE else View.GONE
        hisabActivityEmptyText.visibility = if (hasActivity) View.GONE else View.VISIBLE

        hisabPositionAsOfText.text = string(
            R.string.overview_position_as_of_format,
            timePresentation.displayDateTime(
                presentationLocale,
                deviceZone,
                result.period.endExclusive.minusNanos(1)
            )
        )
        hisabToReceiveText.text = string(
            R.string.to_receive_summary_format,
            formatMoney(currency, position.toReceiveMinor)
        )
        hisabToPayText.text = string(
            R.string.to_pay_summary_format,
            formatMoney(currency, position.toPayMinor)
        )
        hisabNetText.text = string(
            R.string.net_position_format,
            formatMoney(currency, position.netMinor)
        )
        hisabPositionAsOfText.visibility = if (hasPosition) View.VISIBLE else View.GONE
        hisabToReceiveText.visibility = if (hasPosition) View.VISIBLE else View.GONE
        hisabToPayText.visibility = if (hasPosition) View.VISIBLE else View.GONE
        hisabNetText.visibility = if (hasPosition) View.VISIBLE else View.GONE
        hisabPositionEmptyText.visibility = if (hasPosition) View.GONE else View.VISIBLE
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

    // --- Hisab-Kitab: trades ------------------------------------------------

    private fun renderHisabKitab() {
        if (khataPartyId != null) {
            refreshKhataView()
        } else {
            updateHisabKitabChromeVisibility(false)
            renderHisabSummary()
            renderFinancialOverview()
            renderTrades()
            renderParties()
        }
    }

    private fun renderHisabSummary() {
        val farm = currentFarmId?.let { service.loadFarm(it) }
        if (farm == null) {
            hisabSummaryContainer.visibility = View.GONE
            return
        }
        hisabSummaryContainer.visibility = View.VISIBLE
        val currency = farm.currencyCode
        val toReceive = farm.trades.filter { it.type == TradeType.SALE }.fold(0L) { acc, trade ->
            Math.addExact(acc, farm.settlements.paymentSummaryFor(trade).outstandingMinor)
        }
        val toPay = farm.trades.filter { it.type == TradeType.PURCHASE }.fold(0L) { acc, trade ->
            Math.addExact(acc, farm.settlements.paymentSummaryFor(trade).outstandingMinor)
        }
        toReceiveText.text = string(R.string.to_receive_summary_format, formatMoney(currency, toReceive))
        toPayText.text = string(R.string.to_pay_summary_format, formatMoney(currency, toPay))
    }

    private fun renderFinancialOverview() {
        val farmId = currentFarmId
        if (farmId == null) {
            financialOverviewContainer.visibility = View.GONE
            return
        }
        financialOverviewContainer.visibility = View.VISIBLE
        val overview = try {
            service.farmFinancialOverview(farmId, overviewPeriodPreset, OffsetDateTime.now(deviceZone), deviceZone)
        } catch (exception: ArithmeticException) {
            Log.e(LOG_TAG, "financial overview overflow", exception)
            showValidationMessage(FarmUiError.UNEXPECTED.resourceId)
            return
        }
        val currency = currentFarmCurrency()
        val cash = overview.cashTotals
        val trade = overview.tradeTotals
        val position = overview.currentPosition
        val hasCash = cash.incomeMinor > 0 || cash.expenseMinor > 0
        val hasTrade = trade.grossSalesMinor > 0 || trade.grossPurchasesMinor > 0 ||
            trade.paymentsReceivedMinor > 0 || trade.paymentsMadeMinor > 0
        val hasPosition = position.receivableMinor > 0 || position.payableMinor > 0

        overviewCashIncomeText.text = string(R.string.overview_income_format, formatMoney(currency, cash.incomeMinor))
        overviewCashExpenseText.text = string(R.string.overview_expenses_format, formatMoney(currency, cash.expenseMinor))
        overviewCashNetText.text = string(R.string.overview_cash_net_format, formatMoney(currency, cash.netMinor))
        overviewCashIncomeText.visibility = if (hasCash) View.VISIBLE else View.GONE
        overviewCashExpenseText.visibility = if (hasCash) View.VISIBLE else View.GONE
        overviewCashNetText.visibility = if (hasCash) View.VISIBLE else View.GONE
        overviewCashEmptyText.visibility = if (hasCash) View.GONE else View.VISIBLE

        overviewSalesText.text = string(R.string.overview_sales_format, formatMoney(currency, trade.grossSalesMinor))
        overviewPurchasesText.text = string(R.string.overview_purchases_format, formatMoney(currency, trade.grossPurchasesMinor))
        overviewPaymentsReceivedText.text = string(
            R.string.overview_payments_received_format, formatMoney(currency, trade.paymentsReceivedMinor)
        )
        overviewPaymentsMadeText.text = string(R.string.overview_payments_made_format, formatMoney(currency, trade.paymentsMadeMinor))
        overviewSalesText.visibility = if (hasTrade) View.VISIBLE else View.GONE
        overviewPurchasesText.visibility = if (hasTrade) View.VISIBLE else View.GONE
        overviewPaymentsReceivedText.visibility = if (hasTrade) View.VISIBLE else View.GONE
        overviewPaymentsMadeText.visibility = if (hasTrade) View.VISIBLE else View.GONE
        overviewTradeEmptyText.visibility = if (hasTrade) View.GONE else View.VISIBLE

        // The position counts facts strictly before endExclusive; show the last
        // *included* instant so "As of" never presents the excluded boundary as
        // included. MEDIUM format drops sub-seconds, so this reads as the final
        // second of the period (e.g. "…11:59:59 PM" for a month boundary).
        overviewPositionAsOfText.text = string(
            R.string.overview_position_as_of_format,
            timePresentation.displayDateTime(
                presentationLocale,
                deviceZone,
                overview.period.endExclusive.minusNanos(1)
            )
        )
        overviewReceivableText.text = string(
            R.string.overview_receivable_format, formatMoney(currency, position.receivableMinor)
        )
        overviewPayableText.text = string(R.string.overview_payable_format, formatMoney(currency, position.payableMinor))
        overviewNetPositionText.text = string(R.string.net_position_format, formatMoney(currency, position.netMinor))
        overviewPositionAsOfText.visibility = if (hasPosition) View.VISIBLE else View.GONE
        overviewReceivableText.visibility = if (hasPosition) View.VISIBLE else View.GONE
        overviewPayableText.visibility = if (hasPosition) View.VISIBLE else View.GONE
        overviewNetPositionText.visibility = if (hasPosition) View.VISIBLE else View.GONE
        overviewPositionEmptyText.visibility = if (hasPosition) View.GONE else View.VISIBLE

        overviewTrendContainer.removeAllViews()
        val rows = overview.monthlyTrend
        overviewTrendEmptyText.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
        overviewTrendContainer.visibility = if (rows.isEmpty()) View.GONE else View.VISIBLE
        if (rows.isEmpty()) return
        rows.forEach { row ->
            val line = TextView(this)
            val monthLabel = YearMonth.of(row.year, row.month)
                .atDay(1)
                .format(DateTimeFormatter.ofPattern("MMM yyyy").withLocale(presentationLocale))
            line.text = string(
                R.string.trend_row_format,
                monthLabel,
                formatMoney(currency, row.cashIncomeMinor),
                formatMoney(currency, row.cashExpenseMinor),
                formatMoney(currency, row.salesMinor),
                formatMoney(currency, row.purchasesMinor),
                formatMoney(currency, row.paymentsReceivedMinor),
                formatMoney(currency, row.paymentsMadeMinor)
            )
            line.setPadding(0, dp(4), 0, dp(4))
            overviewTrendContainer.addView(line)
        }
    }

    private fun renderTrades() {
        val farmId = currentFarmId ?: run {
            tradesEmptyText.visibility = View.VISIBLE
            tradesContainer.removeAllViews()
            return
        }
        val farm = service.loadFarm(farmId)
        val currency = farm?.currencyCode ?: FarmState.DEFAULT_CURRENCY_CODE
        val trades = service.trades(farmId)
        val settlements = farm?.settlements.orEmpty()
        tradesEmptyText.visibility = if (trades.isEmpty()) View.VISIBLE else View.GONE
        tradesContainer.removeAllViews()
        if (trades.isEmpty()) return
        val inflater = LayoutInflater.from(this)
        trades.forEach { trade ->
            val row = inflater.inflate(R.layout.item_trade_row, tradesContainer, false) as TextView
            row.setTag(trade.id)
            val summary = settlements.paymentSummaryFor(trade)
            val statusText = if (summary.status == PaymentStatus.PAID) {
                string(R.string.trade_row_paid)
            } else {
                string(R.string.trade_row_status_due_format, formatMoney(currency, summary.outstandingMinor))
            }
            row.text = string(
                R.string.trade_row_format,
                FarmLabels.tradeType(this, trade.type),
                displayTradeCounterparty(trade),
                formatMoney(currency, trade.totalMinor),
                string(R.string.trade_row_time_format, statusText, displayTradeTime(trade))
            )
            row.contentDescription = row.text
            row.setOnClickListener {
                confirmDiscardPartyIfNeeded { openTradeEditorForTrade(trade) }
            }
            tradesContainer.addView(row)
        }
    }

    private fun displayTradeCounterparty(trade: Trade): String {
        val partyId = trade.partyId ?: return string(if (trade.type == TradeType.SALE) R.string.cash_sale_label else R.string.cash_purchase_label)
        val farm = currentFarmId?.let { service.loadFarm(it) }
        val party = farm?.parties?.firstOrNull { it.id == partyId }
        return party?.name ?: string(if (trade.type == TradeType.SALE) R.string.cash_sale_label else R.string.cash_purchase_label)
    }

    private fun openTradeEditorForNew(type: TradeType, preselectedPartyId: String? = null) {
        confirmDiscardIfNeeded {
            confirmDiscardPartyIfNeeded {
                val state = TradeEditorState.create(
                    type = type,
                    occurredAt = OffsetDateTime.now(deviceZone)
                ).copy(partyId = preselectedPartyId)
                applyTradeEditorState(state, baseline = state)
            }
        }
    }

    private fun openTradeEditorForTrade(trade: Trade) {
        confirmDiscardIfNeeded {
            confirmDiscardPartyIfNeeded {
                val summary = currentFarmId?.let { service.tradePaymentSummary(it, trade) }
                val state = TradeEditorState(
                    mode = TradeEditorMode.EDIT,
                    tradeId = trade.id,
                    type = trade.type,
                    partyId = trade.partyId,
                    totalText = moneyFormatter.toEditFieldValue(presentationLocale, currentFarmCurrency(), trade.totalMinor),
                    paidStatus = summary?.status ?: PaymentStatus.UNPAID,
                    paidText = "",
                    description = trade.description,
                    occurredAt = trade.occurredAt
                )
                applyTradeEditorState(state, baseline = state)
            }
        }
    }

    private fun applyTradeEditorState(state: TradeEditorState, baseline: TradeEditorState) {
        tradeEditorState = state
        tradeEditorBaseline = baseline
        tradeParties = buildTradePartyChoices(state.type)
        refreshTradePartySpinner(state.type, state.partyId)
        tradeEditorTitle.text = string(tradeEditorTitleRes(state))
        tradeTotalInput.setText(state.totalText)
        syncTradeStatusListener = true
        if (state.mode == TradeEditorMode.CREATE) {
            when (state.paidStatus) {
                PaymentStatus.PAID -> tradeStatusPaidRadio.isChecked = true
                PaymentStatus.PARTIAL -> tradeStatusPartialRadio.isChecked = true
                PaymentStatus.UNPAID -> tradeStatusUnpaidRadio.isChecked = true
            }
        }
        syncTradeStatusListener = false
        state.paidStatus.let { updateTradePaymentVisibility(it) }
        tradePaidInput.setText(state.paidText)
        tradeDescriptionInput.setText(state.description)
        refreshTradeEditorPaymentSection(state)
        updateTradeDateTimeDisplay()
        saveTradeButton.text = string(tradeSaveActionRes(state))
        deleteTradeButton.visibility = if (state.mode == TradeEditorMode.EDIT) View.VISIBLE else View.GONE
        tradeValidationMessageText.visibility = View.GONE
        tradeEditorContainer.visibility = View.VISIBLE
        if (state.mode == TradeEditorMode.CREATE) tradeTotalInput.requestFocus()
    }

    private fun refreshTradeEditorPaymentSection(state: TradeEditorState) {
        val editMode = state.mode == TradeEditorMode.EDIT
        tradeStatusLabel.visibility = if (editMode) View.GONE else View.VISIBLE
        tradeStatusRadioGroup.visibility = if (editMode) View.GONE else View.VISIBLE
        tradePaidLabel.visibility = if (editMode) View.GONE else View.VISIBLE
        tradePaidInput.visibility = if (editMode) View.GONE else View.VISIBLE
        tradePaidDueText.visibility = if (editMode) View.VISIBLE else View.GONE
        managePaymentsButton.visibility = if (editMode) View.VISIBLE else View.GONE
        if (editMode) {
            state.tradeId?.let { tradeId ->
                currentFarmId?.let { farmId ->
                    val trade = service.trade(farmId, tradeId)
                    if (trade != null) {
                        val summary = service.tradePaymentSummary(farmId, trade)
                        tradePaidDueText.text = tradePaidDueSummary(summary)
                    }
                }
            }
        } else {
            tradePaidDueText.text = ""
            updateTradePaymentVisibility(state.paidStatus)
        }
    }

    private fun buildTradePartyChoices(type: TradeType): List<Party?> {
        val farm = currentFarmId?.let { service.loadFarm(it) }
        val compatible = farm?.parties?.filter { it.role.compatibleWith(type) }?.sortedBy { it.name.lowercase() }.orEmpty()
        return listOf(null) + compatible
    }

    private fun refreshTradePartySpinner(type: TradeType, selectedPartyId: String?) {
        val options = tradeParties
        val labels = listOf(string(R.string.trade_party_none)) + options.drop(1).map { it!!.name }
        tradePartySpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            labels
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        val position = options.indexOfFirst { it?.id == selectedPartyId }.coerceAtLeast(0)
        tradePartySpinner.setSelection(position)
    }

    private fun selectedTradePartyId(): String? =
        tradeParties.getOrNull(tradePartySpinner.selectedItemPosition)?.id

    private fun onTradePaymentStatusChanged(status: PaymentStatus) {
        if (syncTradeStatusListener) return
        val state = tradeEditorState ?: return
        tradeEditorState = state.copy(paidStatus = status)
        updateTradePaymentVisibility(status)
    }

    private fun updateTradePaymentVisibility(status: PaymentStatus) {
        val partial = status == PaymentStatus.PARTIAL
        tradePaidLabel.visibility = if (partial) View.VISIBLE else View.GONE
        tradePaidInput.visibility = if (partial) View.VISIBLE else View.GONE
    }

    private fun selectedTradePaymentStatus(): PaymentStatus = when {
        tradeStatusPartialRadio.isChecked -> PaymentStatus.PARTIAL
        tradeStatusUnpaidRadio.isChecked -> PaymentStatus.UNPAID
        else -> PaymentStatus.PAID
    }

    private fun isTradeEditorDirty(): Boolean {
        val baseline = tradeEditorBaseline ?: return false
        val current = currentTradeEditorState() ?: return false
        return current != baseline
    }

    private fun currentTradeEditorState(): TradeEditorState? {
        val state = tradeEditorState ?: return null
        val editMode = state.mode == TradeEditorMode.EDIT
        return state.copy(
            partyId = selectedTradePartyId(),
            totalText = tradeTotalInput.text?.toString().orEmpty(),
            paidStatus = if (editMode) state.paidStatus else selectedTradePaymentStatus(),
            paidText = if (editMode) state.paidText else tradePaidInput.text?.toString().orEmpty(),
            description = tradeDescriptionInput.text?.toString().orEmpty()
        )
    }

    private fun closeTradeEditor() {
        tradeEditorState = null
        tradeEditorBaseline = null
        tradeParties = emptyList()
        tradeEditorContainer.visibility = View.GONE
        tradeValidationMessageText.visibility = View.GONE
        tradeTotalInput.setText("")
        tradePaidInput.setText("")
        tradeDescriptionInput.setText("")
        if (currentDestination == Destination.HISAB_KITAB && khataPartyId != null) {
            refreshKhataView()
        }
    }

    private fun refreshKhataView() {
        updateHisabKitabChromeVisibility(true)
        renderPartyKhata()
    }

    private fun confirmDiscardTradeIfNeeded(action: () -> Unit) {
        if (tradeEditorState != null && isTradeEditorDirty()) {
            showDiscardDialog {
                closeTradeEditor()
                action()
            }
        } else {
            action()
        }
    }

    private fun confirmDiscardSettlementIfNeeded(action: () -> Unit) {
        if (settlementEditorState != null && isSettlementEditorDirty()) {
            showDiscardDialog {
                cancelSettlementForm()
                action()
            }
        } else {
            action()
        }
    }

    private fun cancelTradeEditing() {
        if (isTradeEditorDirty()) {
            showDiscardDialog { closeTradeEditor() }
        } else {
            closeTradeEditor()
        }
    }

    private fun saveTrade() {
        val farmId = currentFarmId ?: return showMissingFarmMessage()
        val state = currentTradeEditorState() ?: return
        val total = when (val result = moneyInputParser.parse(presentationLocale, currentFarmCurrency(), state.totalText)) {
            is MoneyInputResult.Valid -> result.amountMinor
            MoneyInputResult.Missing -> return showTradeEditorError(FarmUiError.TRADE_TOTAL_REQUIRED, tradeTotalInput)
            MoneyInputResult.NotPositive -> return showTradeEditorError(FarmUiError.AMOUNT_NOT_POSITIVE, tradeTotalInput)
            MoneyInputResult.Invalid -> return showTradeEditorError(FarmUiError.AMOUNT_INVALID, tradeTotalInput)
            MoneyInputResult.TooPrecise -> return showTradeEditorError(FarmUiError.AMOUNT_TOO_PRECISE, tradeTotalInput)
            MoneyInputResult.TooLarge -> return showTradeEditorError(FarmUiError.AMOUNT_TOO_LARGE, tradeTotalInput)
        }
        val paid = when (state.paidStatus) {
            PaymentStatus.PAID -> total
            PaymentStatus.UNPAID -> 0L
            PaymentStatus.PARTIAL -> {
                when (val result = moneyInputParser.parse(presentationLocale, currentFarmCurrency(), state.paidText)) {
                    is MoneyInputResult.Valid -> when {
                        result.amountMinor <= 0 -> return showTradeEditorError(FarmUiError.TRADE_PAID_OUT_OF_RANGE, tradePaidInput)
                        result.amountMinor >= total -> return showTradeEditorError(FarmUiError.TRADE_PAID_OUT_OF_RANGE, tradePaidInput)
                        else -> result.amountMinor
                    }
                    else -> return showTradeEditorError(FarmUiError.TRADE_PAID_OUT_OF_RANGE, tradePaidInput)
                }
            }
        }
        val partyId = selectedTradePartyId()
        if (paid < total && partyId == null) {
            return showTradeEditorError(FarmUiError.TRADE_PARTY_REQUIRED, tradePartySpinner)
        }
        if (state.mode == TradeEditorMode.EDIT) {
            val tradeId = state.tradeId
            val settled = tradeId?.let { id ->
                service.trade(farmId, id)?.let { service.tradePaymentSummary(farmId, it) }
            }?.paidMinor ?: 0L
            if (settled > total) {
                return showTradeEditorError(FarmUiError.TRADE_TOTAL_BELOW_SETTLED, tradeTotalInput)
            }
        }
        val occurredAt = state.occurredAt.atZoneSameInstant(deviceZone)
            .toOffsetDateTime()
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val draft = TradeDraft(
            type = state.type,
            partyId = partyId,
            totalMinor = total,
            description = state.description,
            occurredAt = occurredAt
        )
        try {
            if (state.mode == TradeEditorMode.CREATE) {
                service.addTradeWithInitialSettlement(farmId, draft, initialSettlementMinor = paid.takeIf { it > 0 })
                showToast(R.string.toast_trade_created)
            } else {
                service.updateTrade(farmId, state.tradeId!!, draft)
                showToast(R.string.toast_trade_updated)
            }
            closeTradeEditor()
            renderHisabKitab()
        } catch (exception: Exception) {
            showUnexpectedFailure(exception, "save trade failed")
        }
    }

    private fun confirmDeleteTrade() {
        val farmId = currentFarmId ?: return showMissingFarmMessage()
        val state = currentTradeEditorState() ?: return
        if (state.mode != TradeEditorMode.EDIT) return
        val tradeId = state.tradeId ?: return
        val trade = service.trade(farmId, tradeId)
        if (trade != null && service.tradePaymentSummary(farmId, trade).paidMinor > 0L) {
            return showTradeEditorError(FarmUiError.TRADE_HAS_PAYMENTS, deleteTradeButton)
        }
        AlertDialog.Builder(this)
            .setTitle(string(R.string.dialog_delete_trade_title))
            .setMessage(string(R.string.dialog_delete_trade_message))
            .setPositiveButton(string(R.string.delete_trade_action)) { _, _ ->
                try {
                    service.deleteTrade(farmId, tradeId)
                    closeTradeEditor()
                    renderHisabKitab()
                    showToast(R.string.toast_trade_deleted)
                } catch (exception: Exception) {
                    showUnexpectedFailure(exception, "delete trade failed")
                }
            }
            .setNegativeButton(string(R.string.action_cancel), null)
            .show()
    }

    private fun showTradeDateTimePickers() {
        val zone = deviceZone
        val current = tradeEditorState?.occurredAt?.atZoneSameInstant(zone) ?: ZonedDateTime.now(zone)
        val datePicker = DatePickerDialog(
            this,
            { _, year, monthOfYear, dayOfMonth ->
                val timePicker = TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        tradeEditorState = tradeEditorState?.copy(
                            occurredAt = EditorDateTime.fromPickerValues(year, monthOfYear, dayOfMonth, hourOfDay, minute, zone)
                        )
                        updateTradeDateTimeDisplay()
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

    private fun updateTradeDateTimeDisplay() {
        val occurredAt = tradeEditorState?.occurredAt ?: return
        val now = OffsetDateTime.now()
        tradeDateTimeText.text = if (timePresentation.isToday(deviceZone, occurredAt, now)) {
            string(
                R.string.today_time_format,
                string(R.string.today_label),
                timePresentation.shortTime(presentationLocale, deviceZone, occurredAt)
            )
        } else {
            timePresentation.displayDateTime(presentationLocale, deviceZone, occurredAt)
        }
    }

    private fun tradeEditorTitleRes(state: TradeEditorState): Int = when (state.mode) {
        TradeEditorMode.CREATE -> if (state.type == TradeType.SALE) R.string.trade_editor_new_sale else R.string.trade_editor_new_purchase
        TradeEditorMode.EDIT -> if (state.type == TradeType.SALE) R.string.trade_editor_edit_sale else R.string.trade_editor_edit_purchase
    }

    private fun tradeSaveActionRes(state: TradeEditorState): Int = when (state.mode) {
        TradeEditorMode.CREATE -> if (state.type == TradeType.SALE) R.string.save_sale_action else R.string.save_purchase_action
        TradeEditorMode.EDIT -> R.string.update_trade_action
    }

    private fun showTradeEditorError(error: FarmUiError, field: View) {
        val message = string(error.resourceId)
        tradeValidationMessageText.text = message
        tradeValidationMessageText.visibility = View.VISIBLE
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        field.requestFocus()
    }

    private fun currentFarmCurrency(): String {
        val farm = currentFarmId?.let { service.loadFarm(it) }
        return farm?.currencyCode ?: FarmState.DEFAULT_CURRENCY_CODE
    }

    private fun displayTradeTime(trade: Trade): String =
        timePresentation.displayDateTime(presentationLocale, deviceZone, trade.occurredAt)

    private fun restoreTradeEditorFrom(bundle: Bundle?) {
        if (bundle == null) return
        if (!bundle.getBoolean(STATE_TRADE_EDITOR_OPEN, false)) return
        val state = readTradeEditorState(bundle, STATE_TRADE_EDITOR_PREFIX) ?: return
        val baseline = readTradeEditorState(bundle, STATE_TRADE_EDITOR_BASELINE_PREFIX) ?: state
        applyTradeEditorState(state, baseline = baseline)
    }

    private fun writeTradeEditorState(bundle: Bundle, prefix: String, state: TradeEditorState) {
        bundle.putString(prefix + STATE_TRADE_EDITOR_MODE, state.mode.name)
        bundle.putString(prefix + STATE_TRADE_EDITOR_TRADE_ID, state.tradeId)
        bundle.putString(prefix + STATE_TRADE_EDITOR_TYPE, state.type.name)
        bundle.putString(prefix + STATE_TRADE_EDITOR_PARTY_ID, state.partyId)
        bundle.putString(prefix + STATE_TRADE_EDITOR_TOTAL, state.totalText)
        bundle.putString(prefix + STATE_TRADE_EDITOR_PAID_STATUS, state.paidStatus.name)
        bundle.putString(prefix + STATE_TRADE_EDITOR_PAID, state.paidText)
        bundle.putString(prefix + STATE_TRADE_EDITOR_DESCRIPTION, state.description)
        bundle.putString(prefix + STATE_TRADE_EDITOR_OCCURRED_AT, state.occurredAt.toInstant().toString())
    }

    private fun readTradeEditorState(bundle: Bundle, prefix: String): TradeEditorState? {
        val mode = bundle.getString(prefix + STATE_TRADE_EDITOR_MODE)?.let {
            runCatching { TradeEditorMode.valueOf(it) }.getOrNull()
        } ?: return null
        val type = bundle.getString(prefix + STATE_TRADE_EDITOR_TYPE)?.let {
            runCatching { TradeType.valueOf(it) }.getOrNull()
        } ?: return null
        val paidStatus = bundle.getString(prefix + STATE_TRADE_EDITOR_PAID_STATUS)?.let {
            runCatching { PaymentStatus.valueOf(it) }.getOrNull()
        } ?: return null
        val occurredAt = bundle.getString(prefix + STATE_TRADE_EDITOR_OCCURRED_AT)?.let {
            runCatching { OffsetDateTime.parse(it) }.getOrNull()
        } ?: return null
        return TradeEditorState(
            mode = mode,
            tradeId = bundle.getString(prefix + STATE_TRADE_EDITOR_TRADE_ID),
            type = type,
            partyId = bundle.getString(prefix + STATE_TRADE_EDITOR_PARTY_ID),
            totalText = bundle.getString(prefix + STATE_TRADE_EDITOR_TOTAL).orEmpty(),
            paidStatus = paidStatus,
            paidText = bundle.getString(prefix + STATE_TRADE_EDITOR_PAID).orEmpty(),
            description = bundle.getString(prefix + STATE_TRADE_EDITOR_DESCRIPTION).orEmpty(),
            occurredAt = occurredAt
        )
    }

    // --- Hisab-Kitab: settlement (payment) editor ------------------------------

    private fun openPaymentsForTradeBeingEdited() {
        val state = tradeEditorState ?: return
        val tradeId = state.tradeId ?: return
        confirmDiscardIfNeeded {
            settlementTargetTradeId = tradeId
            tradeEditorContainer.visibility = View.GONE
            settlementEditorContainer.visibility = View.VISIBLE
            settlementEditorState = null
            settlementEditorBaseline = null
            renderSettlementEditor()
        }
    }

    private fun renderSettlementEditor() {
        val farmId = currentFarmId ?: run {
            closeSettlementEditor()
            showMissingFarmMessage()
            return
        }
        val tradeId = settlementTargetTradeId ?: return
        val trade = service.trade(farmId, tradeId)
        if (trade == null) {
            closeSettlementEditor()
            showMissingFarmMessage()
            return
        }
        val currency = currentFarmCurrency()
        val summary = service.tradePaymentSummary(farmId, trade)
        val state = settlementEditorState
        val formOpen = state != null

        settlementEditorTitle.text = string(
            if (state?.mode == SettlementEditorMode.EDIT) R.string.edit_payment_title else R.string.new_payment_title
        )
        settlementTradeSummaryText.text = string(
            R.string.settlement_trade_summary_format,
            FarmLabels.tradeType(this, trade.type),
            displayTradeCounterparty(trade),
            formatMoney(currency, trade.totalMinor)
        )
        settlementPaidDueText.text = tradePaidDueSummary(summary)

        settlementEditorFormTitle.visibility = if (formOpen) View.VISIBLE else View.GONE
        settlementEditorFormTitle.text = string(
            if (state?.mode == SettlementEditorMode.EDIT) R.string.edit_payment_title else R.string.new_payment_title
        )
        settlementAmountLabel.visibility = if (formOpen) View.VISIBLE else View.GONE
        settlementAmountInput.visibility = if (formOpen) View.VISIBLE else View.GONE
        settlementDateTimeLabel.visibility = if (formOpen) View.VISIBLE else View.GONE
        settlementDateTimeText.visibility = if (formOpen) View.VISIBLE else View.GONE
        changeSettlementDateTimeButton.visibility = if (formOpen) View.VISIBLE else View.GONE
        settlementNoteInput.visibility = if (formOpen) View.VISIBLE else View.GONE
        settlementValidationMessageText.visibility = View.GONE
        saveSettlementButton.visibility = if (formOpen) View.VISIBLE else View.GONE
        saveSettlementButton.text = string(
            if (state?.mode == SettlementEditorMode.CREATE) R.string.add_payment_action else R.string.update_payment_action
        )
        cancelSettlementFormButton.visibility = if (formOpen) View.VISIBLE else View.GONE
        deleteSettlementButton.visibility =
            if (state?.mode == SettlementEditorMode.EDIT && formOpen) View.VISIBLE else View.GONE
        if (state != null) {
            settlementAmountInput.setText(state.amountText)
            settlementNoteInput.setText(state.note)
            updateSettlementDateTimeDisplay()
        }

        val settlements = service.settlementsForTrade(farmId, tradeId)
        settlementsHistoryLabel.visibility = View.VISIBLE
        settlementsHistoryLabel.text = string(
            if (trade.type == TradeType.SALE) R.string.payments_received_label else R.string.payments_made_label
        )
        settlementsEmptyText.visibility = if (settlements.isEmpty()) View.VISIBLE else View.GONE
        settlementsContainer.removeAllViews()
        if (settlements.isNotEmpty()) {
            val inflater = LayoutInflater.from(this)
            settlements.forEach { settlement ->
                val row = inflater.inflate(R.layout.item_settlement_row, settlementsContainer, false) as TextView
                row.setTag(settlement.id)
                val timeText = displaySettlementTime(settlement)
                val detail = if (settlement.note.isBlank()) {
                    string(
                        R.string.settlement_row_detail_format,
                        formatMoney(currency, settlement.amountMinor),
                        timeText
                    )
                } else {
                    string(
                        R.string.settlement_row_detail_format,
                        formatMoney(currency, settlement.amountMinor),
                        string(R.string.settlement_row_format, settlement.note, timeText)
                    )
                }
                row.text = detail
                row.contentDescription = detail
                row.setOnClickListener {
                    if (isSettlementEditorDirty()) {
                        showDiscardDialog { editSettlementForm(settlement) }
                    } else {
                        editSettlementForm(settlement)
                    }
                }
                settlementsContainer.addView(row)
            }
        }

        addSettlementButton.visibility = if (formOpen) View.GONE else View.VISIBLE
        addSettlementButton.text = string(
            if (trade.type == TradeType.SALE) R.string.receive_payment_action else R.string.record_payment_action
        )
    }

    private fun newSettlementForm() {
        val tradeId = settlementTargetTradeId ?: return
        val state = SettlementEditorState.create(
            tradeId = tradeId,
            occurredAt = OffsetDateTime.now(deviceZone)
        )
        applySettlementEditorState(state, baseline = state)
    }

    private fun editSettlementForm(settlement: Settlement) {
        val state = SettlementEditorState(
            mode = SettlementEditorMode.EDIT,
            tradeId = settlement.tradeId,
            settlementId = settlement.id,
            amountText = moneyFormatter.toEditFieldValue(presentationLocale, currentFarmCurrency(), settlement.amountMinor),
            note = settlement.note,
            occurredAt = settlement.occurredAt
        )
        applySettlementEditorState(state, baseline = state)
    }

    private fun applySettlementEditorState(state: SettlementEditorState, baseline: SettlementEditorState) {
        settlementEditorState = state
        settlementEditorBaseline = baseline
        settlementAmountInput.setText(state.amountText)
        settlementNoteInput.setText(state.note)
        updateSettlementDateTimeDisplay()
        settlementValidationMessageText.visibility = View.GONE
        renderSettlementEditor()
    }

    private fun currentSettlementEditorState(): SettlementEditorState? {
        val state = settlementEditorState ?: return null
        return state.copy(
            amountText = settlementAmountInput.text?.toString().orEmpty(),
            note = settlementNoteInput.text?.toString().orEmpty()
        )
    }

    private fun isSettlementEditorDirty(): Boolean {
        val baseline = settlementEditorBaseline ?: return false
        val current = currentSettlementEditorState() ?: return false
        return current != baseline
    }

    private fun cancelSettlementForm() {
        settlementEditorState = null
        settlementEditorBaseline = null
        renderSettlementEditor()
    }

    private fun cancelSettlementEditing() {
        if (isSettlementEditorDirty()) {
            showDiscardDialog { cancelSettlementForm() }
        } else {
            cancelSettlementForm()
        }
    }

    private fun closeSettlementEditor() {
        settlementTargetTradeId = null
        settlementEditorState = null
        settlementEditorBaseline = null
        settlementEditorContainer.visibility = View.GONE
        settlementValidationMessageText.visibility = View.GONE
        settlementAmountInput.setText("")
        settlementNoteInput.setText("")
        if (tradeEditorState != null) {
            tradeEditorContainer.visibility = View.VISIBLE
            tradeEditorState?.let { refreshTradeEditorPaymentSection(it) }
        }
        if (currentDestination == Destination.HISAB_KITAB && khataPartyId != null && tradeEditorState == null) {
            refreshKhataView()
            return
        }
        renderHisabKitab()
    }

    private fun saveSettlement() {
        val farmId = currentFarmId ?: return showMissingFarmMessage()
        val state = currentSettlementEditorState() ?: return
        val tradeId = settlementTargetTradeId ?: return
        val trade = service.trade(farmId, tradeId)
        if (trade == null) {
            closeSettlementEditor()
            showMissingFarmMessage()
            return
        }
        val amount = when (val result = moneyInputParser.parse(presentationLocale, currentFarmCurrency(), state.amountText)) {
            is MoneyInputResult.Valid -> result.amountMinor
            MoneyInputResult.Missing -> return showSettlementEditorError(FarmUiError.SETTLEMENT_AMOUNT_REQUIRED, settlementAmountInput)
            MoneyInputResult.NotPositive -> return showSettlementEditorError(FarmUiError.SETTLEMENT_AMOUNT_REQUIRED, settlementAmountInput)
            MoneyInputResult.Invalid -> return showSettlementEditorError(FarmUiError.SETTLEMENT_AMOUNT_REQUIRED, settlementAmountInput)
            MoneyInputResult.TooPrecise -> return showSettlementEditorError(FarmUiError.SETTLEMENT_AMOUNT_REQUIRED, settlementAmountInput)
            MoneyInputResult.TooLarge -> return showSettlementEditorError(FarmUiError.SETTLEMENT_AMOUNT_REQUIRED, settlementAmountInput)
        }
        val currentPaid = service.tradePaymentSummary(farmId, trade).paidMinor
        val excludingSelf = settlementEditorState?.let { os ->
            if (os.settlementId != null) {
                currentPaid - (service.settlement(farmId, os.settlementId)?.amountMinor ?: 0L)
            } else {
                currentPaid
            }
        } ?: currentPaid
        if (excludingSelf + amount > trade.totalMinor) {
            return showSettlementEditorError(FarmUiError.SETTLEMENT_OVER_REMAINING, settlementAmountInput)
        }
        if (excludingSelf + amount < trade.totalMinor && trade.partyId == null) {
            return showSettlementEditorError(FarmUiError.SETTLEMENT_REQUIRES_PARTY, settlementAmountInput)
        }
        val occurredAt = state.occurredAt.atZoneSameInstant(deviceZone)
            .toOffsetDateTime()
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val note = state.note
        try {
            if (state.mode == SettlementEditorMode.CREATE) {
                service.addSettlement(farmId, SettlementDraft(tradeId = tradeId, amountMinor = amount, occurredAt = occurredAt, note = note))
                showToast(R.string.toast_settlement_created)
            } else {
                service.updateSettlement(farmId, state.settlementId!!, SettlementDraft(tradeId = tradeId, amountMinor = amount, occurredAt = occurredAt, note = note))
                showToast(R.string.toast_settlement_updated)
            }
            cancelSettlementForm()
            renderSettlementEditor()
        } catch (exception: Exception) {
            showUnexpectedFailure(exception, "save settlement failed")
        }
    }

    private fun confirmDeleteSettlement() {
        val farmId = currentFarmId ?: return showMissingFarmMessage()
        val state = currentSettlementEditorState() ?: return
        if (state.mode != SettlementEditorMode.EDIT) return
        val settlementId = state.settlementId ?: return
        AlertDialog.Builder(this)
            .setTitle(string(R.string.dialog_delete_payment_title))
            .setMessage(string(R.string.dialog_delete_payment_message))
            .setPositiveButton(string(R.string.delete_payment_action)) { _, _ ->
                try {
                    service.deleteSettlement(farmId, settlementId)
                    showToast(R.string.toast_settlement_deleted)
                    cancelSettlementForm()
                    renderSettlementEditor()
                } catch (exception: Exception) {
                    showUnexpectedFailure(exception, "delete settlement failed")
                }
            }
            .setNegativeButton(string(R.string.action_cancel), null)
            .show()
    }

    private fun showSettlementDateTimePickers() {
        val zone = deviceZone
        val current = settlementEditorState?.occurredAt?.atZoneSameInstant(zone) ?: ZonedDateTime.now(zone)
        val datePicker = DatePickerDialog(
            this,
            { _, year, monthOfYear, dayOfMonth ->
                val timePicker = TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        settlementEditorState = settlementEditorState?.copy(
                            occurredAt = EditorDateTime.fromPickerValues(year, monthOfYear, dayOfMonth, hourOfDay, minute, zone)
                        )
                        updateSettlementDateTimeDisplay()
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

    private fun updateSettlementDateTimeDisplay() {
        val occurredAt = settlementEditorState?.occurredAt ?: return
        val now = OffsetDateTime.now()
        settlementDateTimeText.text = if (timePresentation.isToday(deviceZone, occurredAt, now)) {
            string(
                R.string.today_time_format,
                string(R.string.today_label),
                timePresentation.shortTime(presentationLocale, deviceZone, occurredAt)
            )
        } else {
            timePresentation.displayDateTime(presentationLocale, deviceZone, occurredAt)
        }
    }

    private fun tradePaidDueSummary(summary: TradePaymentSummary): String =
        string(
            R.string.trade_paid_due_summary_format,
            formatMoney(currentFarmCurrency(), summary.paidMinor),
            formatMoney(currentFarmCurrency(), summary.outstandingMinor)
        )

    private fun displaySettlementTime(settlement: Settlement): String =
        timePresentation.displayDateTime(presentationLocale, deviceZone, settlement.occurredAt)

    private fun showSettlementEditorError(error: FarmUiError, field: View) {
        val message = string(error.resourceId)
        settlementValidationMessageText.text = message
        settlementValidationMessageText.visibility = View.VISIBLE
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        field.requestFocus()
    }

    private fun restoreSettlementEditorFrom(bundle: Bundle?) {
        if (bundle == null) return
        val target = bundle.getString(STATE_SETTLEMENT_TARGET_TRADE_ID) ?: return
        if (currentFarmId?.let { service.trade(it, target) } == null) return
        settlementTargetTradeId = target
        tradeEditorContainer.visibility = View.GONE
        settlementEditorContainer.visibility = View.VISIBLE
        val state = if (bundle.getBoolean(STATE_SETTLEMENT_EDITOR_OPEN, false)) {
            readSettlementEditorState(bundle, STATE_SETTLEMENT_EDITOR_PREFIX)
        } else {
            null
        }
        val baseline = if (state != null) {
            readSettlementEditorState(bundle, STATE_SETTLEMENT_EDITOR_BASELINE_PREFIX) ?: state
        } else {
            null
        }
        if (state != null) {
            applySettlementEditorState(state, baseline = baseline ?: state)
        } else {
            settlementEditorState = null
            settlementEditorBaseline = null
            renderSettlementEditor()
        }
    }

    private fun writeSettlementEditorState(bundle: Bundle, prefix: String, state: SettlementEditorState) {
        bundle.putString(prefix + STATE_SETTLEMENT_EDITOR_MODE, state.mode.name)
        bundle.putString(prefix + STATE_SETTLEMENT_EDITOR_TRADE_ID, state.tradeId)
        bundle.putString(prefix + STATE_SETTLEMENT_EDITOR_SETTLEMENT_ID, state.settlementId)
        bundle.putString(prefix + STATE_SETTLEMENT_EDITOR_AMOUNT, state.amountText)
        bundle.putString(prefix + STATE_SETTLEMENT_EDITOR_NOTE, state.note)
        bundle.putString(prefix + STATE_SETTLEMENT_EDITOR_OCCURRED_AT, state.occurredAt.toInstant().toString())
    }

    private fun readSettlementEditorState(bundle: Bundle, prefix: String): SettlementEditorState? {
        val mode = bundle.getString(prefix + STATE_SETTLEMENT_EDITOR_MODE)?.let {
            runCatching { SettlementEditorMode.valueOf(it) }.getOrNull()
        } ?: return null
        val tradeId = bundle.getString(prefix + STATE_SETTLEMENT_EDITOR_TRADE_ID) ?: return null
        val occurredAt = bundle.getString(prefix + STATE_SETTLEMENT_EDITOR_OCCURRED_AT)?.let {
            runCatching { OffsetDateTime.parse(it) }.getOrNull()
        } ?: return null
        return SettlementEditorState(
            mode = mode,
            tradeId = tradeId,
            settlementId = bundle.getString(prefix + STATE_SETTLEMENT_EDITOR_SETTLEMENT_ID),
            amountText = bundle.getString(prefix + STATE_SETTLEMENT_EDITOR_AMOUNT).orEmpty(),
            note = bundle.getString(prefix + STATE_SETTLEMENT_EDITOR_NOTE).orEmpty(),
            occurredAt = occurredAt
        )
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
                openPartyKhataFor(party.id)
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
            showPartyValidationMessage(FarmUiError.PARTY_NAME_REQUIRED.resourceId)
            partyNameInput.requestFocus()
            return
        }
        val role = selectedPartyRole()
        val partyId = editingPartyId
        if (partyId != null) {
            val referencedTypes = service.trades(farmId).filter { it.partyId == partyId }.map { it.type }.distinct()
            val incompatibleType = referencedTypes.firstOrNull { !role.compatibleWith(it) }
            if (incompatibleType != null) {
                showPartyValidationMessage(FarmUiError.PARTY_ROLE_INCOMPATIBLE.resourceId)
                partyRoleSpinner.requestFocus()
                return
            }
        }
        val draft = PartyDraft(
            name = name,
            role = role,
            contact = partyContactInput.text?.toString()?.trim().orEmpty(),
            notes = partyNotesInput.text?.toString()?.trim().orEmpty()
        )
        try {
            if (partyId == null) {
                service.addParty(farmId, draft)
                showToast(R.string.toast_party_saved)
            } else {
                service.updateParty(farmId, partyId, draft)
                showToast(R.string.toast_party_saved)
            }
            closePartyEditor()
            if (currentDestination == Destination.HISAB_KITAB && khataPartyId != null) {
                refreshKhataView()
            } else {
                renderParties()
            }
        } catch (exception: Exception) {
            showUnexpectedFailure(exception, "save party failed")
        }
    }

    private fun confirmDeleteParty() {
        val farmId = currentFarmId ?: return showMissingFarmMessage()
        val partyId = editingPartyId ?: return
        if (service.trades(farmId).any { it.partyId == partyId }) {
            showPartyValidationMessage(FarmUiError.PARTY_HAS_TRADES.resourceId)
            return
        }
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
        if (currentDestination == Destination.HISAB_KITAB && khataPartyId != null) {
            refreshKhataView()
        } else {
            renderParties()
        }
    }

    // --- Party Khata (M5-04) -----------------------------------------------

    private fun openPartyKhataFor(partyId: String) {
        val farmId = currentFarmId ?: return showMissingFarmMessage()
        if (service.party(farmId, partyId) == null) {
            showMissingFarmMessage()
            return
        }
        confirmDiscardTradeIfNeeded {
            confirmDiscardSettlementIfNeeded {
                if (editingPartyId != null) {
                    confirmDiscardPartyIfNeeded { showPartyKhata(partyId) }
                } else {
                    showPartyKhata(partyId)
                }
            }
        }
    }

    private fun showPartyKhata(partyId: String) {
        khataPartyId = partyId
        updateHisabKitabChromeVisibility(true)
        renderPartyKhata()
    }

    private fun renderPartyKhata() {
        val farmId = currentFarmId ?: run {
            closePartyKhata()
            return
        }
        val partyId = khataPartyId ?: return
        val party = service.party(farmId, partyId)
        if (party == null) {
            closePartyKhata()
            return
        }
        val ledger = service.partyLedger(farmId, partyId)
        val currency = currentFarmCurrency()
        partyKhataTitle.text = party.name
        partyKhataRoleText.text = FarmLabels.partyRole(this, party.role)
        partyKhataToReceiveText.text = string(
            R.string.to_receive_summary_format,
            formatMoney(currency, ledger.summary.toReceiveMinor)
        )
        partyKhataToPayText.text = string(
            R.string.to_pay_summary_format,
            formatMoney(currency, ledger.summary.toPayMinor)
        )
        partyKhataNetText.text = string(
            R.string.net_position_format,
            partyBalanceSemantics(currency, ledger.summary.netMinor)
        )

        khataNewSaleButton.visibility =
            if (party.role.compatibleWith(TradeType.SALE)) View.VISIBLE else View.GONE
        khataNewPurchaseButton.visibility =
            if (party.role.compatibleWith(TradeType.PURCHASE)) View.VISIBLE else View.GONE

        val entries = ledger.entries
        khataEmptyText.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
        khataEntriesContainer.removeAllViews()
        if (entries.isEmpty()) return

        val inflater = LayoutInflater.from(this)
        entries.asReversed().forEach { entry ->
            val row = inflater.inflate(R.layout.item_ledger_entry_row, khataEntriesContainer, false) as TextView
            row.setTag(entry.sourceId)
            val header = when (entry.sourceType) {
                PartyLedgerEntryType.SALE -> FarmLabels.tradeType(this, TradeType.SALE)
                PartyLedgerEntryType.PURCHASE -> FarmLabels.tradeType(this, TradeType.PURCHASE)
                PartyLedgerEntryType.PAYMENT_RECEIVED -> string(R.string.payment_received_label)
                PartyLedgerEntryType.PAYMENT_MADE -> string(R.string.payment_made_label)
            }
            val detail = buildString {
                append(string(R.string.ledger_entry_header_format, header, formatMoney(currency, entry.amountMinor)))
                append("\n").append(
                    string(
                        R.string.ledger_entry_balance_after_format,
                        partyBalanceSemantics(currency, entry.runningBalanceMinor)
                    )
                )
                if (entry.description.isNotBlank()) append("\n").append(entry.description)
                append("\n").append(
                    timePresentation.displayDateTime(presentationLocale, deviceZone, entry.occurredAt)
                )
            }
            row.text = detail
            row.contentDescription = detail
            row.setOnClickListener {
                when (entry.sourceType) {
                    PartyLedgerEntryType.SALE, PartyLedgerEntryType.PURCHASE -> openTradeFromKhata(entry.tradeId)
                    PartyLedgerEntryType.PAYMENT_RECEIVED, PartyLedgerEntryType.PAYMENT_MADE ->
                        openSettlementFromKhata(entry.sourceId, entry.tradeId)
                }
            }
            khataEntriesContainer.addView(row)
        }
    }

    private fun closePartyKhata() {
        khataPartyId = null
        updateHisabKitabChromeVisibility(false)
        renderHisabKitab()
    }

    private fun updateHisabKitabChromeVisibility(khataActive: Boolean) {
        val chromeVisibility = if (khataActive) View.GONE else View.VISIBLE
        newSaleButton.visibility = chromeVisibility
        newPurchaseButton.visibility = chromeVisibility
        hisabSummaryContainer.visibility = chromeVisibility
        financialOverviewContainer.visibility = chromeVisibility
        tradesSectionLabel.visibility = chromeVisibility
        tradesEmptyText.visibility = chromeVisibility
        tradesContainer.visibility = chromeVisibility
        partiesSectionLabel.visibility = chromeVisibility
        partiesEmptyText.visibility = chromeVisibility
        partiesContainer.visibility = chromeVisibility
        addPartyButton.visibility = chromeVisibility
        partyKhataContainer.visibility = if (khataActive) View.VISIBLE else View.GONE
    }

    private fun partyBalanceSemantics(currency: String, balanceMinor: Long): String = when {
        balanceMinor > 0 -> string(R.string.you_should_receive_format, formatMoney(currency, balanceMinor))
        balanceMinor < 0 -> string(R.string.you_should_pay_format, formatMoney(currency, kotlin.math.abs(balanceMinor)))
        else -> string(R.string.net_settled_label)
    }

    private fun openTradeFromKhata(tradeId: String) {
        val farmId = currentFarmId ?: return
        val trade = service.trade(farmId, tradeId) ?: return
        partyKhataContainer.visibility = View.GONE
        openTradeEditorForTrade(trade)
    }

    private fun openSettlementFromKhata(settlementId: String, tradeId: String) {
        val farmId = currentFarmId ?: return
        val settlement = service.settlement(farmId, settlementId) ?: return
        if (service.trade(farmId, tradeId) == null) return
        settlementTargetTradeId = tradeId
        partyKhataContainer.visibility = View.GONE
        tradeEditorContainer.visibility = View.GONE
        settlementEditorContainer.visibility = View.VISIBLE
        settlementEditorState = null
        settlementEditorBaseline = null
        editSettlementForm(settlement)
    }

    private fun restoreKhataFrom(bundle: Bundle?) {
        if (bundle == null) return
        val partyId = bundle.getString(STATE_KHATA_PARTY_ID) ?: return
        val farmId = currentFarmId ?: return
        if (service.party(farmId, partyId) == null) return
        khataPartyId = partyId
        updateHisabKitabChromeVisibility(true)
        val layeredEditor = tradeEditorState != null || settlementTargetTradeId != null || editingPartyId != null
        if (layeredEditor) {
            partyKhataContainer.visibility = View.GONE
        }
        renderPartyKhata()
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
        if (!visible) partyValidationMessageText.visibility = View.GONE
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
            applyAppTextSize()
            return
        }
        currentFarmId = farm.id
        createFarmContainer.visibility = View.GONE
        farmDetailsContainer.visibility = View.VISIBLE
        renderFarm(farm)
        updateShellTitle()
        if (currentDestination == Destination.SETTINGS) renderSettings()
        if (currentDestination == Destination.HISAB_KITAB) renderHisabKitab()
        if (currentDestination == Destination.HISAB) renderHisabCalculator()
        applyAppTextSize()
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
        settingsNoFarmText.text = string(
            if (farm == null) R.string.settings_no_farm_gentle else R.string.settings_no_farm_text
        )
        settingsFarmNameLabel.visibility = if (farm == null) View.GONE else View.VISIBLE
        settingsFarmNameText.text = farm?.name ?: ""
        settingsFarmNameText.visibility = if (farm == null) View.GONE else View.VISIBLE
        settingsCurrencyText.text = farm?.currencyCode ?: ""
        settingsCurrencyText.visibility = if (farm == null) View.GONE else View.VISIBLE
        changeSettingsCurrencyButton.visibility = if (canChangeCurrency) View.VISIBLE else View.GONE
        settingsCurrencyLockedText.visibility =
            if (farm != null && !canChangeCurrency) View.VISIBLE else View.GONE
        settingsDataNoFarmText.visibility = if (farm == null) View.VISIBLE else View.GONE
        settingsExportBackupButton.visibility = if (farm == null) View.GONE else View.VISIBLE
        settingsImportBackupButton.visibility = View.VISIBLE
        settingsAboutVersionText.text = string(R.string.settings_about_version_format, appVersionName())
        syncLanguageSelection()
        syncTextSizeSelection()
    }

    private fun onTextSizeSelected(textSizeSp: Int) {
        val coerced = AppTextSize.coerce(textSizeSp)
        textSizePreferences.save(coerced)
        settingsTextSizeValueText.text = string(R.string.text_size_value_format, coerced)
        applyAppTextSize()
    }

    private fun syncTextSizeSelection() {
        val selected = textSizePreferences.load()
        textSizeChangeSuppressed = true
        settingsTextSizeSeekBar.max = AppTextSize.MAX_SP - AppTextSize.MIN_SP
        settingsTextSizeSeekBar.progress = selected - AppTextSize.MIN_SP
        settingsTextSizeValueText.text = string(R.string.text_size_value_format, selected)
        textSizeChangeSuppressed = false
    }

    private fun applyAppTextSize() {
        val scale = textSizePreferences.load().toFloat() / AppTextSize.DEFAULT_SP
        applyTextScale(shellRoot, scale)
    }

    private fun applyTextScale(view: View, scale: Float) {
        if (view is TextView) {
            val original = originalTextSizesPx.getOrPut(view) { view.textSize }
            view.setTextSize(TypedValue.COMPLEX_UNIT_PX, original * scale)
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                applyTextScale(view.getChildAt(index), scale)
            }
        }
    }

    private fun appVersionName(): String {
        val versionName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0)).versionName
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0).versionName
        }
        return versionName ?: string(R.string.app_name)
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_about_title)
            .setMessage(
                string(
                    R.string.settings_about_version_format,
                    appVersionName()
                ) + "\n\n" + string(R.string.settings_about_privacy_note)
            )
            .setPositiveButton(R.string.action_done, null)
            .show()
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

    private fun showPartyValidationMessage(@StringRes resId: Int, vararg formatArgs: Any) {
        val message = string(resId, *formatArgs)
        partyValidationMessageText.text = message
        partyValidationMessageText.visibility = View.VISIBLE
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
        const val STATE_TRADE_EDITOR_OPEN = "tradeEditorOpen"
        const val STATE_TRADE_EDITOR_PREFIX = "tradeEditor"
        const val STATE_TRADE_EDITOR_BASELINE_PREFIX = "tradeEditorBaseline"
        const val STATE_TRADE_EDITOR_MODE = "Mode"
        const val STATE_TRADE_EDITOR_TRADE_ID = "TradeId"
        const val STATE_TRADE_EDITOR_TYPE = "Type"
        const val STATE_TRADE_EDITOR_PARTY_ID = "PartyId"
        const val STATE_TRADE_EDITOR_TOTAL = "Total"
        const val STATE_TRADE_EDITOR_PAID_STATUS = "PaidStatus"
        const val STATE_TRADE_EDITOR_PAID = "Paid"
        const val STATE_TRADE_EDITOR_DESCRIPTION = "Description"
        const val STATE_TRADE_EDITOR_OCCURRED_AT = "OccurredAt"
        const val STATE_SETTLEMENT_TARGET_TRADE_ID = "settlementTargetTradeId"
        const val STATE_KHATA_PARTY_ID = "khataPartyId"
        const val STATE_OVERVIEW_PERIOD_PRESET = "overviewPeriodPreset"
        const val STATE_HISAB_PARTY_ID = "hisabPartyId"
        const val STATE_HISAB_PERIOD_PRESET = "hisabPeriodPreset"
        const val STATE_SETTLEMENT_EDITOR_OPEN = "settlementEditorOpen"
        const val STATE_SETTLEMENT_EDITOR_PREFIX = "settlementEditor"
        const val STATE_SETTLEMENT_EDITOR_BASELINE_PREFIX = "settlementEditorBaseline"
        const val STATE_SETTLEMENT_EDITOR_MODE = "Mode"
        const val STATE_SETTLEMENT_EDITOR_TRADE_ID = "TradeId"
        const val STATE_SETTLEMENT_EDITOR_SETTLEMENT_ID = "SettlementId"
        const val STATE_SETTLEMENT_EDITOR_AMOUNT = "Amount"
        const val STATE_SETTLEMENT_EDITOR_NOTE = "Note"
        const val STATE_SETTLEMENT_EDITOR_OCCURRED_AT = "OccurredAt"
        const val STATE_FARM_PLANNING_CALCULATOR = "farmPlanningCalculator"
    }
}
