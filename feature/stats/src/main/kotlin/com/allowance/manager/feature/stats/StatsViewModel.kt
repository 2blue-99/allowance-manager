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
import com.allowance.manager.core.domain.model.BudgetCycle
import com.allowance.manager.core.domain.model.lastDay
import com.allowance.manager.core.domain.usecase.budget.GetCycleUseCase
import com.allowance.manager.core.domain.usecase.calendar.GetAdjacentCycleUseCase
import com.allowance.manager.core.domain.usecase.calendar.ObserveCycleTransactionsUseCase
import com.allowance.manager.core.domain.usecase.calendar.ObserveTransactionCyclesUseCase
import com.allowance.manager.core.domain.usecase.stats.ObserveCycleExpenseTotalsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

private const val WINDOW_SIZE = 6   // 화면에 보이는 사이클 수
private const val PAGE_STEP = 6     // 좌우 이동 단위 = 한 화면씩 통째로 넘김

data class MonthBar(
    val cycle: BudgetCycle,
    val label: String,        // "8.25" — 사이클 시작일
    val expense: Long,
    val budget: Long,         // 그 사이클 용돈(이월) → 계단식 점선 레벨
    val isOver: Boolean,      // 지출 > 용돈 → 빨강, 이하 → 초록
    val isSelected: Boolean,
    val exists: Boolean,      // 거래가 있는 사이클. 없으면 막대 선택 불가
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
    val selected: BudgetCycle? = null,
    val summary: MonthSummary = MonthSummary(),
    val categories: List<CategorySlice> = emptyList(),
    val canOlder: Boolean = false,
    val canNewer: Boolean = false,
    val isLoading: Boolean = true,
    val userType: UserType = UserType.Default,
    val dataCycles: List<BudgetCycle> = emptyList(), // 거래 있는 사이클 (피커 활성화용)
) {
    /** 창 전체 범위 — "26. 4. 25 – 26. 9. 24" */
    val rangeLabel: String
        get() {
            val first = window.firstOrNull() ?: return ""
            val last = window.last().cycle.lastDay
            return "${first.cycle.start.year % 100}. ${first.cycle.start.monthValue}. ${first.cycle.start.dayOfMonth}" +
                " – ${last.year % 100}. ${last.monthValue}. ${last.dayOfMonth}"
        }
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    observeCycleExpenseTotalsUseCase: ObserveCycleExpenseTotalsUseCase,
    observeBudgetHistoryUseCase: ObserveBudgetHistoryUseCase,
    getUserTypeUseCase: GetUserTypeUseCase,
    observeTransactionCyclesUseCase: ObserveTransactionCyclesUseCase,
    private val observeCycleTransactionsUseCase: ObserveCycleTransactionsUseCase,
    private val getCycleUseCase: GetCycleUseCase,
    private val getAdjacentCycleUseCase: GetAdjacentCycleUseCase,
    private val analytics: AnalyticsHelper,
) : BaseViewModel() {

    private val windowEnd = MutableStateFlow<BudgetCycle?>(null)   // 창의 오른쪽 끝(최신)
    private val selected = MutableStateFlow<BudgetCycle?>(null)

    /** 창을 이룰 사이클들 — windowEnd에서 과거로 되짚어 만든다 */
    private val window = MutableStateFlow<List<BudgetCycle>>(emptyList())

    /** 오늘 사이클 — 미래로 못 가게 막는 상한 */
    private var todayCycle: BudgetCycle? = null

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val selectedTransactions = selected.flatMapLatest { c ->
        if (c == null) kotlinx.coroutines.flow.flowOf(emptyList()) else observeCycleTransactionsUseCase(c.start)
    }

    // 사이클별 지출 합계 맵 — GROUP BY cycle_start 한 방
    private val expenseByCycle = observeCycleExpenseTotalsUseCase()

    // 거래 있는 사이클 (피커 활성화용)
    private val dataCyclesFlow = observeTransactionCyclesUseCase()

    init {
        viewModelScope.launch {
            val today = getCycleUseCase()
            todayCycle = today
            selected.value = today
            moveWindowTo(today)
        }

        viewModelScope.launch {
            val partialFlow = combine(
                window,
                selected,
                expenseByCycle,
                observeBudgetHistoryUseCase(),
            ) { win, sel, totals, history -> Partial(win, sel, totals, history) }
            combine(partialFlow, selectedTransactions, getUserTypeUseCase(), dataCyclesFlow) { p, tx, userType, dc ->
                render(p, tx, userType, dc)
            }.collect { _uiState.value = it }
        }
    }

    /** [end]를 오른쪽 끝으로 삼아 과거로 [WINDOW_SIZE]개를 채운다. 더 못 가면 있는 만큼만. */
    private suspend fun moveWindowTo(end: BudgetCycle) {
        windowEnd.value = end
        val list = mutableListOf(end)
        repeat(WINDOW_SIZE - 1) {
            val prev = getAdjacentCycleUseCase(list.first(), -1)
            if (prev.start == list.first().start) return@repeat
            list.add(0, prev)
        }
        window.value = list
    }

    private data class Partial(
        val window: List<BudgetCycle>,
        val selected: BudgetCycle?,
        val expenseByCycle: Map<LocalDate, Long>,
        val history: List<MonthlyBudget>,
    )

    /** 그 사이클의 용돈 = effectiveCycle <= 사이클 시작일 중 최신값(이월). 이력은 오름차순. */
    private fun budgetFor(cycleStart: LocalDate, history: List<MonthlyBudget>): Long =
        history.lastOrNull { !it.effectiveCycle.isAfter(cycleStart) }?.amount ?: 0L

    private fun render(p: Partial, selectedTx: List<Transaction>, userType: UserType, dataCycles: List<BudgetCycle>): StatsUiState {
        val dataStarts = dataCycles.map { it.start }.toSet()
        val window = p.window.map { cycle ->
            val expense = p.expenseByCycle[cycle.start] ?: 0L
            val budget = budgetFor(cycle.start, p.history)
            MonthBar(
                cycle = cycle,
                label = "${cycle.start.monthValue}.${cycle.start.dayOfMonth}",
                expense = expense,
                budget = budget,
                isOver = budget > 0 && expense > budget,
                isSelected = cycle.start == p.selected?.start,
                // 거래가 있거나 현재 사이클이면 선택 가능
                exists = cycle.start in dataStarts || cycle.start == todayCycle?.start,
            )
        }

        // 선택 달 요약·카테고리 (가계부 집계 = BUDGET+LEDGER_ONLY, EXCLUDED 제외)
        val counted = selectedTx.filter { it.inLedger }
        val expenseTx = counted.filter { it.type == TransactionType.EXPENSE }
        val totalExpense = expenseTx.sumOf { it.amount }
        val income = counted.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val budget = p.selected?.let { budgetFor(it.start, p.history) } ?: 0L

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

        // 이동 가능 여부: 과거는 거래가 있는 가장 오래된 사이클까지, 미래는 오늘 사이클까지
        val oldest = dataCycles.minByOrNull { it.start }
        val first = p.window.firstOrNull()
        return StatsUiState(
            window = window,
            selected = p.selected,
            summary = MonthSummary(budget = budget, expense = totalExpense, income = income),
            categories = categories,
            canOlder = first != null && oldest != null && first.start.isAfter(oldest.start),
            canNewer = windowEnd.value?.let { end -> todayCycle?.let { end.start.isBefore(it.start) } } == true,
            isLoading = p.selected == null,
            userType = userType,
            dataCycles = dataCycles,
        )
    }

    /** 막대를 탭해 사이클 선택 */
    fun onSelectCycle(cycle: BudgetCycle) {
        analytics.logEvent(AmAnalytics.Event.STATS_MONTH_SELECT)
        selected.value = cycle
    }

    fun onOlder() {
        analytics.logEvent(AmAnalytics.Event.STATS_WINDOW_MOVE, mapOf(AmAnalytics.Param.DIRECTION to "older"))
        moveWindow(-PAGE_STEP)
    }

    fun onNewer() {
        analytics.logEvent(AmAnalytics.Event.STATS_WINDOW_MOVE, mapOf(AmAnalytics.Param.DIRECTION to "newer"))
        moveWindow(PAGE_STEP)
    }

    /** 피커로 창 점프 — 고른 사이클을 창의 오른쪽 끝으로. */
    fun onPickCycle(target: BudgetCycle) {
        analytics.logEvent(AmAnalytics.Event.STATS_WINDOW_MOVE, mapOf(AmAnalytics.Param.DIRECTION to "picker"))
        viewModelScope.launch {
            // 오늘 사이클보다 미래로는 못 간다
            val today = todayCycle
            val end = if (today != null && target.start.isAfter(today.start)) today else target
            moveWindowTo(end)
            selected.value = end
        }
    }

    /**
     * 창을 [offset]칸 옮긴다. 창 이동 시 선택 사이클을 새 창의 최신으로 맞춰
     * 헤더와 항상 일치하고, 왕복해도 같은 자리로 돌아온다.
     */
    private fun moveWindow(offset: Int) {
        val end = windowEnd.value ?: return
        if (offset < 0 && !uiState.value.canOlder) return
        if (offset > 0 && !uiState.value.canNewer) return
        viewModelScope.launch {
            var target = getAdjacentCycleUseCase(end, offset)
            // 미래로 넘칠 땐 오늘 사이클에서 멈춘다
            todayCycle?.let { if (target.start.isAfter(it.start)) target = it }
            moveWindowTo(target)
            selected.value = target
        }
    }
}
