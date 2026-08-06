package com.allowance.manager.feature.stats

import androidx.lifecycle.viewModelScope
import com.allowance.manager.core.analytics.AmAnalytics
import com.allowance.manager.core.analytics.AnalyticsHelper
import com.allowance.manager.core.common.BaseViewModel
import com.allowance.manager.core.domain.model.MonthlyBudget
import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.model.TransactionCategory
import com.allowance.manager.core.domain.model.TransactionType
import com.allowance.manager.core.domain.model.UserType
import com.allowance.manager.core.domain.usecase.budget.GetUserTypeUseCase
import com.allowance.manager.core.domain.usecase.budget.ObserveBudgetHistoryUseCase
import com.allowance.manager.core.domain.usecase.calendar.GetFirstTransactionMonthUseCase
import com.allowance.manager.core.domain.usecase.calendar.ObserveMonthTransactionsUseCase
import com.allowance.manager.core.domain.usecase.calendar.ObserveTransactionMonthsUseCase
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
private const val PAGE_STEP = 6     // 좌우 이동 단위 = 한 화면(6개월)씩 통째로 넘김

data class MonthBar(
    val yearMonth: YearMonth,
    val label: String,        // "7월"
    val expense: Long,
    val budget: Long,         // 그 달 용돈(이월) → 계단식 점선 레벨
    val isOver: Boolean,      // 지출 > 용돈 → 빨강, 이하 → 초록
    val isSelected: Boolean,
    val exists: Boolean,      // 첫 거래월 ~ 이번달 범위 안(=실제 존재하는 월). 이전 달 막대는 선택 불가
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
    val userType: UserType = UserType.Default,
    val headerMonth: YearMonth = YearMonth.now(),   // 헤더 표시·피커 기준 = 6개월 창의 최신월(windowEnd)
    val minMonth: YearMonth? = null,                // 피커 하한 = 가장 과거 내역의 달
    val dataMonths: Set<YearMonth> = emptySet(),    // 거래 있는 달 (월 피커 활성화용)
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
    getUserTypeUseCase: GetUserTypeUseCase,
    observeTransactionMonthsUseCase: ObserveTransactionMonthsUseCase,
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

    // 거래 있는 달 (월 피커 활성화용)
    private val dataMonthsFlow = observeTransactionMonthsUseCase()

    init {
        viewModelScope.launch { minMonth.value = getFirstTransactionMonthUseCase() }

        viewModelScope.launch {
            val partialFlow = combine(
                windowEnd,
                selected,
                minMonth,
                expenseByMonth,
                observeBudgetHistoryUseCase(),
            ) { end, sel, min, totals, history ->
                Partial(end, sel, min, totals.associate { it.yearMonth to it.total }, history)
            }
            combine(partialFlow, selectedTransactions, getUserTypeUseCase(), dataMonthsFlow) { p, tx, userType, dm ->
                render(p, tx, userType, dm)
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

    private fun render(p: Partial, selectedTx: List<Transaction>, userType: UserType, dataMonths: List<YearMonth>): StatsUiState {
        val now = YearMonth.now()
        // 존재하는 월의 하한 = 첫 거래월(없으면 이번달). 이보다 과거 or 미래 달은 실제 존재하지 않음.
        val lowerBound = p.minMonth ?: now
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
                exists = !ym.isBefore(lowerBound) && !ym.isAfter(now),
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
            userType = userType,
            headerMonth = p.windowEnd,
            minMonth = minMonth,
            dataMonths = dataMonths.toSet(),
        )
    }

    fun onSelectMonth(month: YearMonth) {
        analytics.logEvent(AmAnalytics.Event.STATS_MONTH_SELECT)
        selected.value = month
    }

    fun onOlder() {
        analytics.logEvent(AmAnalytics.Event.STATS_WINDOW_MOVE, mapOf(AmAnalytics.Param.DIRECTION to "older"))
        val min = minMonth.value
        var end = windowEnd.value.minusMonths(PAGE_STEP.toLong())
        // 창의 왼쪽 끝이 첫 거래 달보다 과거로 못 가게 clamp
        if (min != null && end.minusMonths((WINDOW_SIZE - 1).toLong()).isBefore(min)) {
            end = min.plusMonths((WINDOW_SIZE - 1).toLong())
        }
        windowEnd.value = end
        // 창 이동 시 선택월을 새 창의 최신월로 맞춤 → 헤더와 항상 일치, 왕복해도 동일 달 복귀
        selected.value = end
    }

    fun onNewer() {
        analytics.logEvent(AmAnalytics.Event.STATS_WINDOW_MOVE, mapOf(AmAnalytics.Param.DIRECTION to "newer"))
        val now = YearMonth.now()
        val end = minOf(windowEnd.value.plusMonths(PAGE_STEP.toLong()), now)
        windowEnd.value = end
        // 창 이동 시 선택월을 새 창의 최신월로 맞춤 → 헤더와 항상 일치, 왕복해도 동일 달 복귀
        selected.value = end
    }

    /** 월 피커로 창 점프 — 창의 최신월(windowEnd)을 고른 달로. [minMonth, 이번달] 및 창 크기로 clamp. */
    fun onPickMonth(target: YearMonth) {
        analytics.logEvent(AmAnalytics.Event.STATS_WINDOW_MOVE, mapOf(AmAnalytics.Param.DIRECTION to "picker"))
        val now = YearMonth.now()
        val min = minMonth.value
        var end = if (target.isAfter(now)) now else target
        // 창의 첫 달이 가장 과거 내역 달보다 과거로 가지 않게 clamp
        if (min != null && end.minusMonths((WINDOW_SIZE - 1).toLong()).isBefore(min)) {
            end = min.plusMonths((WINDOW_SIZE - 1).toLong())
        }
        windowEnd.value = end
        // 창 이동 시 선택월을 새 창의 최신월로 맞춤 → 헤더와 항상 일치, 왕복해도 동일 달 복귀
        selected.value = end
    }

}
