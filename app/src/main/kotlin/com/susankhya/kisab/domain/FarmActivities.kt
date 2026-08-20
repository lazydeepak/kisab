package com.susankhya.kisab.domain

/**
 * M10 Farm Activities domain model.
 *
 * A farm can operate several activities at the same time (poultry, dairy,
 * crops, vegetables, fishery, ...). [FarmActivityType] is the **stable
 * persisted identity** for each activity. Display labels are resolved in the
 * UI layer via [com.susankhya.kisab.ui.FarmLabels]; translated labels are
 * never stored. The enum is closed today but values may be **appended** later
 * (name-based persistence makes appends backward-compatible, mirroring the
 * existing [TransactionCategory]/[ProductUnit] convention).
 *
 * [FarmActivityCatalog] is the single, centralized policy that maps an
 * activity to the existing governed options (transaction categories and farm
 * planning calculators). UI screens ask the catalog for orderings instead of
 * branching on individual activities, so future activities are added in one
 * place.
 */
enum class FarmActivityType {
    CROPS,
    VEGETABLES,
    FRUITS_ORCHARD,
    POULTRY,
    CATTLE_BUFFALO_DAIRY,
    GOAT_SHEEP,
    PIG,
    FISHERY,
    OTHER
}

/**
 * Centralized activity-aware policy.
 *
 * Everything activity-aware flows through this object so screens never branch
 * on individual activities. All orderings are deterministic and stable.
 */
object FarmActivityCatalog {

    /** Canonical user-facing order used by every picker and breakdown. */
    val displayOrder: List<FarmActivityType> = listOf(
        FarmActivityType.CROPS,
        FarmActivityType.VEGETABLES,
        FarmActivityType.FRUITS_ORCHARD,
        FarmActivityType.POULTRY,
        FarmActivityType.CATTLE_BUFFALO_DAIRY,
        FarmActivityType.GOAT_SHEEP,
        FarmActivityType.PIG,
        FarmActivityType.FISHERY,
        FarmActivityType.OTHER
    )

    private val EXPENSE_ORDER = listOf(
        TransactionCategory.FEED,
        TransactionCategory.SUPPLIES,
        TransactionCategory.LABOR,
        TransactionCategory.TRANSPORT,
        TransactionCategory.OTHER_EXPENSE
    )

    private val INCOME_ORDER = listOf(
        TransactionCategory.SALES,
        TransactionCategory.SERVICES,
        TransactionCategory.OTHER_INCOME
    )

    private val CALCULATOR_ORDER = listOf(
        FarmPlanningCalculator.SEED,
        FarmPlanningCalculator.FERTILIZER,
        FarmPlanningCalculator.FEED,
        FarmPlanningCalculator.MILK,
        FarmPlanningCalculator.CROP_YIELD
    )

    /**
     * Expense categories a farmer running [activity] is most likely to record.
     * The list stays within the existing governed category authority; the
     * activity does not invent new categories.
     */
    fun relevantExpenseCategories(activity: FarmActivityType): Set<TransactionCategory> = when (activity) {
        FarmActivityType.CROPS,
        FarmActivityType.VEGETABLES,
        FarmActivityType.FRUITS_ORCHARD ->
            setOf(
                TransactionCategory.SUPPLIES,
                TransactionCategory.LABOR,
                TransactionCategory.TRANSPORT
            )
        FarmActivityType.POULTRY,
        FarmActivityType.CATTLE_BUFFALO_DAIRY,
        FarmActivityType.GOAT_SHEEP,
        FarmActivityType.PIG,
        FarmActivityType.FISHERY ->
            setOf(
                TransactionCategory.FEED,
                TransactionCategory.SUPPLIES,
                TransactionCategory.LABOR,
                TransactionCategory.TRANSPORT
            )
        FarmActivityType.OTHER -> emptySet()
    }

    /** Income categories are farm-wide; no activity narrows income choices. */
    fun relevantIncomeCategories(activity: FarmActivityType): Set<TransactionCategory> = emptySet()

    /**
     * Farm planning calculators genuinely relevant to [activity]. Calculators
     * are never hidden from a general farm; ordering is what changes.
     */
    fun relevantCalculators(activity: FarmActivityType): Set<FarmPlanningCalculator> = when (activity) {
        FarmActivityType.CROPS,
        FarmActivityType.VEGETABLES,
        FarmActivityType.FRUITS_ORCHARD ->
            setOf(
                FarmPlanningCalculator.SEED,
                FarmPlanningCalculator.FERTILIZER,
                FarmPlanningCalculator.CROP_YIELD
            )
        FarmActivityType.CATTLE_BUFFALO_DAIRY ->
            setOf(FarmPlanningCalculator.FEED, FarmPlanningCalculator.MILK)
        FarmActivityType.POULTRY,
        FarmActivityType.GOAT_SHEEP,
        FarmActivityType.PIG,
        FarmActivityType.FISHERY ->
            setOf(FarmPlanningCalculator.FEED)
        FarmActivityType.OTHER -> emptySet()
    }

    /**
     * Category list for the transaction editor for a farm running [activities].
     *
     * The union of the activities' relevant categories is presented first (in
     * canonical order), followed by the remaining general categories. A farm
     * with no activities gets the exact historical list, so migrated farms and
     * general farms render identically to before.
     */
    fun orderedCategories(
        activities: Set<FarmActivityType>,
        type: TransactionType
    ): List<TransactionCategory> {
        val canonical = if (type == TransactionType.INCOME) INCOME_ORDER else EXPENSE_ORDER
        val relevant = activities.flatMap { activity ->
            if (type == TransactionType.INCOME) {
                relevantIncomeCategories(activity)
            } else {
                relevantExpenseCategories(activity)
            }
        }.toSet()
        return (canonical.filter { it in relevant } + canonical.filter { it !in relevant })
    }

    /**
     * Calculator list for the farm planning section for a farm running
     * [activities]. Relevant calculators are presented first; all calculators
     * remain selectable.
     */
    fun orderedCalculators(activities: Set<FarmActivityType>): List<FarmPlanningCalculator> {
        val relevant = activities.flatMap { relevantCalculators(it) }.toSet()
        return (CALCULATOR_ORDER.filter { it in relevant } + CALCULATOR_ORDER.filter { it !in relevant })
    }

    /**
     * Transaction-editor activity choices for a farm running [activities].
     * `null` represents the general/farm-wide option. [currentActivity] is a
     * transaction's existing association (possibly to a now-disabled
     * activity) and is appended so editing never silently drops it.
     */
    fun activityChoices(
        activities: Set<FarmActivityType>,
        currentActivity: FarmActivityType?
    ): List<FarmActivityType?> {
        val choices = displayOrder.filter { it in activities }.toMutableList()
        if (currentActivity != null && currentActivity !in choices) {
            choices.add(currentActivity)
        }
        return listOf(null) + choices
    }
}

/**
 * Per-activity accounting projection. [activity] of `null` means the
 * general/farm-wide transaction (no activity association).
 */
data class FarmActivityTotals(
    val activity: FarmActivityType?,
    val incomeMinor: Long,
    val expenseMinor: Long,
    val balanceMinor: Long
)

/**
 * Pure activity-level income/expense/balance over the farm's transactions.
 *
 * Sums exactly the whole-farm totals partitioned by activity: every
 * transaction belongs to exactly one bucket (`null` = general), so the buckets
 * never double-count and always reconcile with [FarmTotals]. Uses exact
 * `Long` minor-unit arithmetic via [Math.addExact]/[Math.subtractExact].
 *
 * Known activities are ordered by [FarmActivityCatalog.displayOrder]; the
 * general bucket is last. A disabled activity's historical transactions stay
 * in their bucket — disabling never removes history.
 */
fun farmActivityBreakdown(transactions: List<FarmTransaction>): List<FarmActivityTotals> {
    data class Accumulator(var income: Long = 0L, var expense: Long = 0L)

    val buckets = linkedMapOf<FarmActivityType?, Accumulator>()
    for (transaction in transactions) {
        val bucket = buckets.getOrPut(transaction.activity) { Accumulator() }
        if (transaction.type == TransactionType.INCOME) {
            bucket.income = Math.addExact(bucket.income, transaction.amountMinor)
        } else {
            bucket.expense = Math.addExact(bucket.expense, transaction.amountMinor)
        }
    }

    val orderedKeys = FarmActivityCatalog.displayOrder
        .filter { it in buckets.keys }
        .map<FarmActivityType, FarmActivityType?> { it }
        .toMutableList()
    if (null in buckets.keys) orderedKeys.add(null)

    return orderedKeys.map { activity ->
        val accumulator = buckets.getValue(activity)
        FarmActivityTotals(
            activity = activity,
            incomeMinor = accumulator.income,
            expenseMinor = accumulator.expense,
            balanceMinor = Math.subtractExact(accumulator.income, accumulator.expense)
        )
    }
}