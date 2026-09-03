package com.allowance.manager.feature.calendar

import androidx.lifecycle.viewModelScope
import com.allowance.manager.core.analytics.AmAnalytics
import com.allowance.manager.core.analytics.AnalyticsHelper
import com.allowance.manager.core.common.BaseViewModel
import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.model.TransactionCategory
import com.allowance.manager.core.domain.model.TransactionType
import com.allowance.manager.core.domain.model.UserType
import com.allowance.manager.core.domain.usecase.budget.GetUserTypeUseCase
import com.allowance.manager.core.domain.model.BudgetCycle
import com.allowance.manager.core.domain.usecase.budget.GetCycleUseCase
import com.allowance.manager.core.domain.usecase.calendar.GetAdjacentCycleUseCase
import com.allowance.manager.core.domain.usecase.calendar.ObserveCycleTransactionsUseCase
import com.allowance.manager.core.domain.usecase.calendar.ObserveTransactionCyclesUseCase
import com.allowance.manager.core.domain.usecase.ignore.AddIgnoredAccountUseCase
import com.allowance.manager.core.domain.usecase.ignore.CountIgnorableTransactionsUseCase
import com.allowance.manager.core.domain.usecase.transaction.DeleteTransactionUseCase
import com.allowance.manager.core.domain.usecase.transaction.HideTransactionUseCase
import com.allowance.manager.core.domain.usecase.transaction.PromoteToMainUseCase
import com.allowance.manager.core.domain.usecase.transaction.SetTxScopeUseCase
import com.allowance.manager.core.domain.usecase.transaction.UpdateTransactionUseCase
import com.allowance.manager.core.domain.model.TxScope
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

/** 내역 리스트 유형 필터 — 전체 / 지출만 / 수입만. (이체는 '전체'에서만 노출) */
enum class LedgerTypeFilter { ALL, EXPENSE, INCOME }

data class CalendarUiState(
    /** 보고 있는 사이클. 첫 로딩 중엔 null */
    val cycle: BudgetCycle? = null,
    val transactions: List<Transaction> = emptyList(), // 검색·필터가 적용된 '노출 리스트'
    val expense: Long = 0L,                             // 노출 리스트 기준 지출 합계 (숨김 제외)
    val income: Long = 0L,                              // 노출 리스트 기준 수입 합계 (숨김 제외)
    val searchActive: Boolean = false,
    val mainOnly: Boolean = false,                      // 메인 계좌만 보기 (헤더 '메인' 토글)
    val query: String = "",
    val typeFilter: LedgerTypeFilter = LedgerTypeFilter.ALL,
    val categoryFilter: Set<TransactionCategory> = emptySet(),
    val canGoPrev: Boolean = false,                     // 더 과거 사이클이 있는지
    val canGoNext: Boolean = false,                     // 현재 사이클보다 뒤인지
    val dataCycles: List<BudgetCycle> = emptyList(),    // 거래 있는 사이클 (피커 목록)
    val userType: UserType = UserType.Default,          // 자산 호칭(용돈/생활비/예산) — 행·시트 안내문
    val isLoading: Boolean = true,
)

private data class FilterParams(
    val query: String,
    val categories: Set<TransactionCategory>,
    val typeFilter: LedgerTypeFilter,
    val searchActive: Boolean,
    val mainOnly: Boolean,
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val observeCycleTransactionsUseCase: ObserveCycleTransactionsUseCase,
    private val getCycleUseCase: GetCycleUseCase,
    private val getAdjacentCycleUseCase: GetAdjacentCycleUseCase,
    observeTransactionCyclesUseCase: ObserveTransactionCyclesUseCase,
    getUserTypeUseCase: GetUserTypeUseCase,
    private val hideTransactionUseCase: HideTransactionUseCase,
    private val addIgnoredAccountUseCase: AddIgnoredAccountUseCase,
    private val countIgnorableTransactionsUseCase: CountIgnorableTransactionsUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val setTxScopeUseCase: SetTxScopeUseCase,
    private val promoteToMainUseCase: PromoteToMainUseCase,
    private val analytics: AnalyticsHelper,
) : BaseViewModel() {

    private val cycle = MutableStateFlow<BudgetCycle?>(null)

    /** 오늘이 속한 사이클의 시작일 — 미래로 못 가게 막는 상한 */
    private var todayCycleStart: java.time.LocalDate? = null
    private val query = MutableStateFlow("")
    private val categoryFilter = MutableStateFlow<Set<TransactionCategory>>(emptySet())
    private val typeFilter = MutableStateFlow(LedgerTypeFilter.ALL)
    private val searchActive = MutableStateFlow(false)
    private val mainOnly = MutableStateFlow(false)

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val cycleTransactions = cycle.flatMapLatest { c ->
        if (c == null) kotlinx.coroutines.flow.flowOf(emptyList()) else observeCycleTransactionsUseCase(c.start)
    }

    private val filterParams = combine(query, categoryFilter, typeFilter, searchActive, mainOnly) { q, c, t, a, m ->
        FilterParams(q, c, t, a, m)
    }

    // 거래 있는 사이클 (피커 목록)
    private val dataCyclesFlow = observeTransactionCyclesUseCase()

    // combine 인자 수 제한(5) 회피 — 데이터사이클 + 사용자유형을 한 플로우로 묶음
    private val dataCyclesAndType = combine(dataCyclesFlow, getUserTypeUseCase()) { dc, ut -> dc to ut }

    init {
        // 오늘이 속한 사이클로 시작 (앱을 열 때마다 항상 현재로)
        viewModelScope.launch {
            val today = getCycleUseCase()
            todayCycleStart = today.start
            cycle.value = today
        }

        viewModelScope.launch {
            combine(cycle, cycleTransactions, filterParams, dataCyclesAndType) { c, raw, fp, dcType ->
                render(c, raw, fp, dcType.first, dcType.second)
            }.collect { _uiState.value = it }
        }
    }

    /** 보이는 사이클 내역에 검색·필터를 적용하고, 노출 리스트 기준으로 지출·수입 요약을 계산 */
    private fun render(
        cycle: BudgetCycle?,
        raw: List<Transaction>,
        fp: FilterParams,
        dataCycles: List<BudgetCycle>,
        userType: UserType,
    ): CalendarUiState {
        val filtered = raw.filter { tx ->
            // 메인 토글: 켜지면 메인 계좌(accountId != null) 내역만
            val matchMain = !fp.mainOnly || tx.isMain
            val matchCategory = fp.categories.isEmpty() || tx.category in fp.categories
            // 유형 필터: 전체=모두(이체 포함), 지출/수입=해당 타입만
            val matchType = when (fp.typeFilter) {
                LedgerTypeFilter.ALL -> true
                LedgerTypeFilter.EXPENSE -> tx.type == TransactionType.EXPENSE
                LedgerTypeFilter.INCOME -> tx.type == TransactionType.INCOME
            }
            val matchQuery = fp.query.isBlank() ||
                tx.sourceName.contains(fp.query, ignoreCase = true) ||
                (tx.merchant?.contains(fp.query, ignoreCase = true) == true) ||
                (tx.memo?.contains(fp.query, ignoreCase = true) == true)
            matchMain && matchCategory && matchType && matchQuery
        }
        // 가계부 집계(BUDGET+LEDGER_ONLY)만 요약. EXCLUDED(미등록·제외)는 리스트엔 노출하되 요약에서 뺀다.
        val counted = filtered.filter { it.inLedger }
        val expense = counted.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val income = counted.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        // 이동 가능 여부: 과거는 거래가 있는 가장 오래된 사이클까지, 미래는 현재 사이클까지
        val oldest = dataCycles.minByOrNull { it.start }
        val canGoPrev = cycle != null && oldest != null && cycle.start.isAfter(oldest.start)
        val canGoNext = cycle != null && todayCycleStart?.let { cycle.start.isBefore(it) } == true

        return CalendarUiState(
            cycle = cycle,
            transactions = filtered,
            expense = expense,
            income = income,
            searchActive = fp.searchActive,
            mainOnly = fp.mainOnly,
            query = fp.query,
            typeFilter = fp.typeFilter,
            categoryFilter = fp.categories,
            canGoPrev = canGoPrev,
            canGoNext = canGoNext,
            dataCycles = dataCycles,
            userType = userType,
            isLoading = cycle == null,
        )
    }

    fun onPrevMonth() {
        analytics.logEvent(AmAnalytics.Event.CALENDAR_MONTH_CHANGE, mapOf(AmAnalytics.Param.DIRECTION to "prev"))
        moveCycle(-1)
    }

    fun onNextMonth() {
        analytics.logEvent(AmAnalytics.Event.CALENDAR_MONTH_CHANGE, mapOf(AmAnalytics.Param.DIRECTION to "next"))
        moveCycle(1)
    }

    /**
     * 통계 딥링크 — 특정 달을 열면 그 달 중간이 속한 사이클로 이동.
     * TODO(사이클 통일 5단계): 통계가 사이클 기준이 되면 사이클 시작일을 직접 받는다.
     */
    fun onSelectMonth(month: YearMonth) {
        viewModelScope.launch { cycle.value = getCycleUseCase(month.atDay(15)) }
    }

    /** 피커에서 사이클을 직접 고름 */
    fun onSelectCycle(target: BudgetCycle) {
        analytics.logEvent(AmAnalytics.Event.CALENDAR_MONTH_CHANGE, mapOf(AmAnalytics.Param.DIRECTION to "picker"))
        cycle.value = target
    }

    private fun moveCycle(offset: Int) {
        val current = cycle.value ?: return
        if (offset < 0 && !uiState.value.canGoPrev) return
        if (offset > 0 && !uiState.value.canGoNext) return
        viewModelScope.launch { cycle.value = getAdjacentCycleUseCase(current, offset) }
    }

    fun onToggleSearch() {
        val next = !searchActive.value
        analytics.logEvent(AmAnalytics.Event.CALENDAR_SEARCH_TOGGLE, mapOf(AmAnalytics.Param.ACTIVE to next))
        searchActive.value = next
        // 검색 해제 시 검색어·필터 초기화 → 원복
        if (!next) {
            query.value = ""
            categoryFilter.value = emptySet()
            typeFilter.value = LedgerTypeFilter.ALL
        }
    }

    /** 헤더 '메인' 토글 — 켜지면 메인 계좌 내역만, 해제하면 전체. (검색과 독립) */
    fun onToggleMainOnly() {
        val next = !mainOnly.value
        analytics.logEvent(AmAnalytics.Event.CALENDAR_MAIN_FILTER, mapOf(AmAnalytics.Param.ACTIVE to next))
        mainOnly.value = next
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    /** 유형 슬라이드(전체/지출/수입) 선택 */
    fun onSetTypeFilter(filter: LedgerTypeFilter) {
        typeFilter.value = filter
    }

    fun onToggleCategory(category: TransactionCategory) {
        analytics.logEvent(AmAnalytics.Event.CALENDAR_CATEGORY_FILTER, mapOf(AmAnalytics.Param.CATEGORY to category.name))
        categoryFilter.value = categoryFilter.value.let {
            if (category in it) it - category else it + category
        }
    }

    /** '전체' 선택 — 모든 분류 필터 해제 (빈 필터 = 전체가 기본값) */
    fun onClearCategoryFilter() {
        categoryFilter.value = emptySet()
    }

    // ── 내역 액션 (상세 시트 공용) ──
    fun onSetHidden(id: Long, hidden: Boolean) {
        analytics.logEvent(AmAnalytics.Event.CALENDAR_TX_SWIPE_HIDE, mapOf(AmAnalytics.Param.HIDDEN to hidden))
        viewModelScope.launch { hideTransactionUseCase(id, hidden) }
    }

    /** 상세시트 3분기 — 예산 반영 상태 지정 */
    fun onSetScope(id: Long, scope: TxScope) {
        viewModelScope.launch { setTxScopeUseCase(id, scope) }
    }

    fun onIgnoreSource(tx: Transaction) {
        viewModelScope.launch { addIgnoredAccountUseCase(tx) }
    }

    suspend fun countIgnorable(tx: Transaction): Int = countIgnorableTransactionsUseCase(tx)

    fun onDelete(id: Long) {
        analytics.logEvent(AmAnalytics.Event.CALENDAR_TX_SWIPE_DELETE)
        viewModelScope.launch { deleteTransactionUseCase(id) }
    }

    fun onSaveTransaction(
        id: Long,
        type: TransactionType,
        amount: Long,
        merchant: String,
        memo: String,
        category: TransactionCategory?,
    ) {
        viewModelScope.launch { updateTransactionUseCase(id, type, amount, merchant, memo, category) }
    }

    fun onPromoteToMain(transaction: Transaction) {
        // 계좌번호(extractedAccount)가 없으면 출처(앱) 기준으로 등록
        viewModelScope.launch {
            promoteToMainUseCase(
                packageName = transaction.packageName,
                bankName = transaction.sourceName,
                accountPattern = transaction.extractedAccount,
            )
        }
    }
}
