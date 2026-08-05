package com.allowance.manager.feature.calendar

import androidx.lifecycle.viewModelScope
import com.allowance.manager.core.analytics.AmAnalytics
import com.allowance.manager.core.analytics.AnalyticsHelper
import com.allowance.manager.core.common.BaseViewModel
import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.model.TransactionCategory
import com.allowance.manager.core.domain.model.TransactionType
import com.allowance.manager.core.domain.usecase.calendar.GetFirstTransactionMonthUseCase
import com.allowance.manager.core.domain.usecase.calendar.ObserveMonthTransactionsUseCase
import com.allowance.manager.core.domain.usecase.calendar.ObserveTransactionMonthsUseCase
import com.allowance.manager.core.domain.usecase.ignore.AddIgnoredAccountUseCase
import com.allowance.manager.core.domain.usecase.ignore.CountIgnorableTransactionsUseCase
import com.allowance.manager.core.domain.usecase.transaction.DeleteTransactionUseCase
import com.allowance.manager.core.domain.usecase.transaction.HideTransactionUseCase
import com.allowance.manager.core.domain.usecase.transaction.PromoteToMainUseCase
import com.allowance.manager.core.domain.usecase.transaction.UpdateTransactionUseCase
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

data class CalendarUiState(
    val month: YearMonth = YearMonth.now(),
    val transactions: List<Transaction> = emptyList(), // 검색·필터가 적용된 '노출 리스트'
    val expense: Long = 0L,                             // 노출 리스트 기준 지출 합계 (숨김 제외)
    val income: Long = 0L,                              // 노출 리스트 기준 수입 합계 (숨김 제외)
    val searchActive: Boolean = false,
    val query: String = "",
    val categoryFilter: Set<TransactionCategory> = emptySet(),
    val minMonth: YearMonth? = null,                    // 이보다 과거로는 이동 불가 (첫 내역의 달)
    val dataMonths: Set<YearMonth> = emptySet(),        // 거래 있는 달 (월 피커 활성화용)
    val isLoading: Boolean = true,
) {
    /** 다음 달(미래) 이동 가능 여부 — 이번 달까지만 */
    val canGoNext: Boolean get() = month < YearMonth.now()

    /** 이전 달 이동 가능 여부 — 첫 내역의 달까지만 (내역 없으면 제한 없음) */
    val canGoPrev: Boolean get() = minMonth == null || month > minMonth
}

private data class FilterParams(
    val query: String,
    val categories: Set<TransactionCategory>,
    val searchActive: Boolean,
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val observeMonthTransactionsUseCase: ObserveMonthTransactionsUseCase,
    private val getFirstTransactionMonthUseCase: GetFirstTransactionMonthUseCase,
    observeTransactionMonthsUseCase: ObserveTransactionMonthsUseCase,
    private val hideTransactionUseCase: HideTransactionUseCase,
    private val addIgnoredAccountUseCase: AddIgnoredAccountUseCase,
    private val countIgnorableTransactionsUseCase: CountIgnorableTransactionsUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val promoteToMainUseCase: PromoteToMainUseCase,
    private val analytics: AnalyticsHelper,
) : BaseViewModel() {

    private val month = MutableStateFlow(YearMonth.now())
    private val query = MutableStateFlow("")
    private val categoryFilter = MutableStateFlow<Set<TransactionCategory>>(emptySet())
    private val searchActive = MutableStateFlow(false)
    private val minMonth = MutableStateFlow<YearMonth?>(null)

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val monthTransactions = month.flatMapLatest { observeMonthTransactionsUseCase(it) }

    private val filterParams = combine(query, categoryFilter, searchActive) { q, c, a ->
        FilterParams(q, c, a)
    }

    // 거래 있는 달 (월 피커 활성화용)
    private val dataMonthsFlow = observeTransactionMonthsUseCase()

    init {
        // 첫 내역의 달(이전-달 이동 하한) 1회 조회
        viewModelScope.launch { minMonth.value = getFirstTransactionMonthUseCase() }

        viewModelScope.launch {
            combine(month, monthTransactions, minMonth, filterParams, dataMonthsFlow) { m, raw, min, fp, dm ->
                render(m, raw, min, fp, dm)
            }.collect { _uiState.value = it }
        }
    }

    /** 보이는 달 내역에 검색·필터를 적용하고, 노출 리스트 기준으로 지출·수입 요약을 계산 */
    private fun render(
        month: YearMonth,
        raw: List<Transaction>,
        minMonth: YearMonth?,
        fp: FilterParams,
        dataMonths: List<YearMonth>,
    ): CalendarUiState {
        val filtered = raw.filter { tx ->
            val matchCategory = fp.categories.isEmpty() || tx.category in fp.categories
            val matchQuery = fp.query.isBlank() ||
                tx.sourceName.contains(fp.query, ignoreCase = true) ||
                (tx.memo?.contains(fp.query, ignoreCase = true) == true)
            matchCategory && matchQuery
        }
        // 숨김(합계 제외) 항목은 요약에서 뺀다. 리스트에는 그대로 노출.
        val counted = filtered.filter { !it.isHidden }
        val expense = counted.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val income = counted.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        return CalendarUiState(
            month = month,
            transactions = filtered,
            expense = expense,
            income = income,
            searchActive = fp.searchActive,
            query = fp.query,
            categoryFilter = fp.categories,
            minMonth = minMonth,
            dataMonths = dataMonths.toSet(),
            isLoading = false,
        )
    }

    fun onPrevMonth() {
        analytics.logEvent(AmAnalytics.Event.CALENDAR_MONTH_CHANGE, mapOf(AmAnalytics.Param.DIRECTION to "prev"))
        if (uiState.value.canGoPrev) month.value = month.value.minusMonths(1)
    }

    fun onNextMonth() {
        analytics.logEvent(AmAnalytics.Event.CALENDAR_MONTH_CHANGE, mapOf(AmAnalytics.Param.DIRECTION to "next"))
        if (uiState.value.canGoNext) month.value = month.value.plusMonths(1)
    }

    fun onSelectMonth(target: YearMonth) {
        analytics.logEvent(AmAnalytics.Event.CALENDAR_MONTH_CHANGE, mapOf(AmAnalytics.Param.DIRECTION to "picker"))
        val max = YearMonth.now()
        val min = minMonth.value
        val clamped = when {
            target > max -> max
            min != null && target < min -> min
            else -> target
        }
        month.value = clamped
    }

    fun onToggleSearch() {
        val next = !searchActive.value
        analytics.logEvent(AmAnalytics.Event.CALENDAR_SEARCH_TOGGLE, mapOf(AmAnalytics.Param.ACTIVE to next))
        searchActive.value = next
        // 검색 해제 시 검색어·필터 초기화 → 원복
        if (!next) {
            query.value = ""
            categoryFilter.value = emptySet()
        }
    }

    fun onQueryChange(value: String) {
        query.value = value
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

    fun onIgnoreSource(tx: Transaction) {
        viewModelScope.launch { addIgnoredAccountUseCase(tx) }
    }

    suspend fun countIgnorable(tx: Transaction): Int = countIgnorableTransactionsUseCase(tx)

    fun onDelete(id: Long) {
        analytics.logEvent(AmAnalytics.Event.CALENDAR_TX_SWIPE_DELETE)
        viewModelScope.launch { deleteTransactionUseCase(id) }
    }

    fun onSaveTransaction(id: Long, memo: String, category: TransactionCategory?) {
        viewModelScope.launch { updateTransactionUseCase(id, memo, category) }
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
