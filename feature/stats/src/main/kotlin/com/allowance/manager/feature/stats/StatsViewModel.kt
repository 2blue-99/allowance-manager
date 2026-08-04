package com.allowance.manager.feature.stats

import androidx.lifecycle.viewModelScope
import com.allowance.manager.core.analytics.AnalyticsHelper
import com.allowance.manager.core.common.BaseViewModel
import com.allowance.manager.core.domain.model.MonthlyBudget
import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.model.TransactionCategory
import com.allowance.manager.core.domain.model.TransactionType
import com.allowance.manager.core.domain.usecase.budget.ObserveBudgetHistoryUseCase
import com.allowance.manager.core.domain.usecase.calendar.GetFirstTransactionMonthUseCase
import com.allowance.manager.core.domain.usecase.calendar.ObserveMonthTransactionsUseCase
import com.allowance.manager.core.domain.usecase.stats.GetMonthlyExpenseTotalsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

private const val WINDOW_SIZE = 6   // 화면에 보이는 개월 수
private const val PAGE_STEP = 3     // 좌우 이동 단위

data class MonthBar(
    val yearMonth: YearMonth,
    val label: String,        // "7월"
    val expense: Long,
    val budget: Long,         // 그 달 용돈(이월) → 계단식 점선 레벨
    val isOver: Boolean,      // 지출 > 용돈 → 빨강, 이하 → 초록
    val isSelected: Boolean,
)

data class MonthSummary(
    val budget: Long = 0L,
    val expense: Long = 0L,
    val income: Long = 0L,
) {
    val over: Long get() = (expense - budget).coerceAtLeast(0L)  // 초과액 (0 이하면 0)
    val isOver: Boolean get() = budget > 0 && expense > budget
}

data class CategorySlice(
    val category: TransactionCategory?,   // null = 미분류
    val amount: Long,
    val ratio: Float,                     // 그 달 총지출 대비 비율
)

data class StatsUiState(
    val window: List<MonthBar> = emptyList(),
    val selected: YearMonth = YearMonth.now(),
    val summary: MonthSummary = MonthSummary(),
    val categories: List<CategorySlice> = emptyList(),
    val canOlder: Boolean = false,
    val canNewer: Boolean = false,
    val isLoading: Boolean = true,
) {
    val rangeLabel: String
        get() = window.firstOrNull()?.let { first ->
            val last = window.last().yearMonth
            "${first.yearMonth.year}년 ${first.yearMonth.monthValue}월 – ${last.monthValue}월"
        } ?: ""
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    getMonthlyExpenseTotalsUseCase: GetMonthlyExpenseTotalsUseCase,
    observeBudgetHistoryUseCase: ObserveBudgetHistoryUseCase,
    private val observeMonthTransactionsUseCase: ObserveMonthTransactionsUseCase,
    private val getFirstTransactionMonthUseCase: GetFirstTransactionMonthUseCase,
    private val analytics: AnalyticsHelper,
) : BaseViewModel() {

    private val windowEnd = MutableStateFlow(YearMonth.now())   // 창의 오른쪽 끝(최신)
    private val selected = MutableStateFlow(YearMonth.now())
    private val minMonth = MutableStateFlow<YearMonth?>(null)

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val selectedTransactions = selected.flatMapLatest { observeMonthTransactionsUseCase(it) }

    // 월별 지출 합계 맵 (달력 월, 무시 제외)
    private val expenseByMonth = getMonthlyExpenseTotalsUseCase()

    init {
        viewModelScope.launch { minMonth.value = getFirstTransactionMonthUseCase() }

        viewModelScope.launch {
            combine(
                windowEnd,
                selected,
                minMonth,
                expenseByMonth,
                observeBudgetHistoryUseCase(),
            ) { end, sel, min, totals, history ->
                Partial(end, sel, min, totals.associate { it.yearMonth to it.total }, history)
            }.combine(selectedTransactions) { p, tx ->
                render(p, tx)
            }.collect { _uiState.value = it }
        }
    }

    private data class Partial(
        val windowEnd: YearMonth,
        val selected: YearMonth,
        val minMonth: YearMonth?,
        val expenseByMonth: Map<YearMonth, Long>,
        val history: List<MonthlyBudget>,
    )

    /** 그 달의 용돈 = effectiveMonth <= month 중 최신값(이월). 이력은 오름차순. */
    private fun budgetFor(month: YearMonth, history: List<MonthlyBudget>): Long =
        history.lastOrNull { !it.effectiveMonth.isAfter(month) }?.amount ?: 0L

    private fun render(p: Partial, selectedTx: List<Transaction>): StatsUiState {
        val months = (0 until WINDOW_SIZE).map { p.windowEnd.minusMonths((WINDOW_SIZE - 1 - it).toLong()) }
        val window = months.map { ym ->
            val expense = p.expenseByMonth[ym] ?: 0L
            val budget = budgetFor(ym, p.history)
            MonthBar(
                yearMonth = ym,
                label = "${ym.monthValue}월",
                expense = expense,
                budget = budget,
                isOver = budget > 0 && expense > budget,
                isSelected = ym == p.selected,
            )
        }

        // 선택 달 요약·카테고리 (집계 대상 = 메인/수동 · 무시 아님)
        val counted = selectedTx.filter { it.isCounted && !it.isHidden }
        val expenseTx = counted.filter { it.type == TransactionType.EXPENSE }
        val totalExpense = expenseTx.sumOf { it.amount }
        val income = counted.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val budget = budgetFor(p.selected, p.history)

        val categories = expenseTx
            .groupBy { it.category ?: TransactionCategory.ETC }   // 미지정은 기타로 합산
            .map { (cat, list) -> cat to list.sumOf { it.amount } }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { (cat, amount) ->
                CategorySlice(
                    category = cat,
                    amount = amount,
                    ratio = if (totalExpense > 0) amount.toFloat() / totalExpense else 0f,
                )
            }

        val minMonth = p.minMonth
        return StatsUiState(
            window = window,
            selected = p.selected,
            summary = MonthSummary(budget = budget, expense = totalExpense, income = income),
            categories = categories,
            canOlder = minMonth == null || months.first().isAfter(minMonth),
            canNewer = p.windowEnd.isBefore(YearMonth.now()),
            isLoading = false,
        )
    }

    fun onSelectMonth(month: YearMonth) {
        analytics.logEvent("stats_month_select")
        selected.value = month
    }

    fun onOlder() {
        analytics.logEvent("stats_window_move", mapOf("direction" to "older"))
        val min = minMonth.value
        var end = windowEnd.value.minusMonths(PAGE_STEP.toLong())
        // 창의 왼쪽 끝이 첫 거래 달보다 과거로 못 가게 clamp
        if (min != null && end.minusMonths((WINDOW_SIZE - 1).toLong()).isBefore(min)) {
            end = min.plusMonths((WINDOW_SIZE - 1).toLong())
        }
        windowEnd.value = end
        clampSelectedIntoWindow(end)
    }

    fun onNewer() {
        analytics.logEvent("stats_window_move", mapOf("direction" to "newer"))
        val now = YearMonth.now()
        val end = minOf(windowEnd.value.plusMonths(PAGE_STEP.toLong()), now)
        windowEnd.value = end
        clampSelectedIntoWindow(end)
    }

    /** 창을 옮긴 뒤 선택 달이 창 밖이면 창의 최신 달로 옮겨 항상 보이게 한다. */
    private fun clampSelectedIntoWindow(end: YearMonth) {
        val start = end.minusMonths((WINDOW_SIZE - 1).toLong())
        val sel = selected.value
        if (sel.isBefore(start) || sel.isAfter(end)) selected.value = end
    }
}
