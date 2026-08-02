package com.allowance.manager.feature.home

import androidx.lifecycle.viewModelScope
import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.usecase.account.ObserveAccountsUseCase
import com.allowance.manager.core.domain.usecase.budget.GetUserTypeUseCase
import com.allowance.manager.core.domain.usecase.budget.ObserveBudgetStatusUseCase
import com.allowance.manager.core.domain.model.HomeFilter
import com.allowance.manager.core.domain.model.HomeFilterChip
import com.allowance.manager.core.domain.model.UserType
import com.allowance.manager.core.domain.usecase.setting.GetHomeFilterUseCase
import com.allowance.manager.core.domain.usecase.setting.GetHomeGuideShownUseCase
import com.allowance.manager.core.domain.usecase.setting.SetHomeFilterUseCase
import com.allowance.manager.core.domain.usecase.setting.SetHomeGuideShownUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.allowance.manager.core.domain.usecase.transaction.DeleteTransactionUseCase
import com.allowance.manager.core.domain.usecase.transaction.IgnoreTransactionUseCase
import com.allowance.manager.core.domain.usecase.transaction.ObserveCurrentTransactionsUseCase
import com.allowance.manager.core.domain.usecase.transaction.AddManualTransactionUseCase
import com.allowance.manager.core.domain.usecase.transaction.PromoteToMainUseCase
import com.allowance.manager.core.domain.usecase.transaction.UpdateTransactionUseCase
import com.allowance.manager.core.domain.model.TransactionCategory
import com.allowance.manager.core.domain.model.TransactionType
import com.allowance.manager.core.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class HomeUiState(
    val budget: Long = 0L,
    val spent: Long = 0L,
    val income: Long = 0L,              // 이번 달(사이클) 수입 합계
    val remaining: Long = 0L,
    val ratio: Float = 0f,              // 남은 비율
    val spentRatio: Float = 0f,         // 소진율 (지출/예산) — 메인 바 채움
    val isOver: Boolean = false,
    val dailyBudget: Long = 0L,         // 하루 사용 권장액 (남은예산 ÷ 남은일수)
    val dailyAverage: Long = 0L,        // 하루 평균 지출 (지출 ÷ 경과일수)
    val overPace: Boolean = false,      // 하루 평균 > 권장 → 과속
    val daysUntilPayday: Int = 0,       // 다음 월급일까지 D-day
    val transactions: List<Transaction> = emptyList(),
    val filter: HomeFilter = HomeFilter.Default,  // 내역 세그먼트 필터 (메인/전체/숨김)
    val userType: UserType = UserType.Default,   // 사용자 유형 → 예산 호칭(용돈/생활비/예산)
    val isLoading: Boolean = true,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeBudgetStatusUseCase: ObserveBudgetStatusUseCase,
    observeCurrentTransactionsUseCase: ObserveCurrentTransactionsUseCase,
    observeAccountsUseCase: ObserveAccountsUseCase,
    getHomeFilterUseCase: GetHomeFilterUseCase,
    getUserTypeUseCase: GetUserTypeUseCase,
    private val setHomeFilterUseCase: SetHomeFilterUseCase,
    private val ignoreTransactionUseCase: IgnoreTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val promoteToMainUseCase: PromoteToMainUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val addManualTransactionUseCase: AddManualTransactionUseCase,
    getHomeGuideShownUseCase: GetHomeGuideShownUseCase,
    private val setHomeGuideShownUseCase: SetHomeGuideShownUseCase,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // 최초 진입 가이드 노출 여부 (아직 안 봤으면 true)
    val showGuide: StateFlow<Boolean> = getHomeGuideShownUseCase()
        .map { !it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun onGuideFinished() {
        viewModelScope.launch { setHomeGuideShownUseCase(true) }
    }

    init {
        viewModelScope.launch {
            combine(
                observeBudgetStatusUseCase(),
                observeCurrentTransactionsUseCase(),
                observeAccountsUseCase(),
                getHomeFilterUseCase(),
                getUserTypeUseCase(),
            ) { status, transactions, _, filter, userType ->
                // 멀티 토글 필터: 전체(둘 다 꺼짐) / 메인 / 숨김 / 메인+숨김
                val visible = when {
                    filter.isAll -> transactions
                    filter.showMain && filter.showHidden ->
                        transactions.filter { it.isMain || it.isManual || it.isIgnored }
                    filter.showMain -> transactions.filter { (it.isMain || it.isManual) && !it.isIgnored }
                    else -> transactions.filter { it.isIgnored }   // 숨김만
                }

                // 하루 권장액·평균·D-day: 사이클 경계와 오늘 기준으로 계산 (0일 나눗셈 방어)
                val today = LocalDate.now()
                val daysUntil = ChronoUnit.DAYS.between(today, status.cycle.nextPayday).toInt()
                val daysLeft = daysUntil.coerceAtLeast(1)
                val elapsed = (ChronoUnit.DAYS.between(status.cycle.start, today).toInt() + 1).coerceAtLeast(1)
                // 하루 권장·평균은 100원 단위로 내림 표시 (예: 13,163 → 13,100)
                val dailyBudget = if (status.remaining > 0) status.remaining / daysLeft / 100 * 100 else 0L
                val dailyAverage = status.spent / elapsed / 100 * 100
                val spentRatio = if (status.budget > 0) (status.spent.toFloat() / status.budget).coerceIn(0f, 1f) else 0f
                // 이번 달 수입 = 사이클 내 집계대상(메인·수동) · 무시 아님 · INCOME
                val income = transactions
                    .filter { it.isCounted && !it.isIgnored && it.type == TransactionType.INCOME }
                    .sumOf { it.amount }

                HomeUiState(
                    budget = status.budget,
                    spent = status.spent,
                    income = income,
                    remaining = status.remaining,
                    ratio = status.ratio,
                    spentRatio = spentRatio,
                    isOver = status.isOver,
                    dailyBudget = dailyBudget,
                    dailyAverage = dailyAverage,
                    overPace = status.budget > 0 && dailyAverage > dailyBudget,
                    daysUntilPayday = daysUntil,
                    transactions = visible,
                    filter = filter,
                    userType = userType,
                    isLoading = false,
                )
            }.collect { _uiState.value = it }
        }
    }

    /** 칩 탭 — 전체는 배타(둘 다 해제), 메인·숨김은 각각 독립 토글 */
    fun onFilterChip(chip: HomeFilterChip) {
        val cur = uiState.value.filter
        val next = when (chip) {
            HomeFilterChip.MAIN -> cur.copy(showMain = !cur.showMain)
            HomeFilterChip.HIDDEN -> cur.copy(showHidden = !cur.showHidden)
            HomeFilterChip.ALL -> HomeFilter(showMain = false, showHidden = false)
        }
        viewModelScope.launch { setHomeFilterUseCase(next) }
    }

    fun onSetIgnored(id: Long, ignored: Boolean) {
        viewModelScope.launch { ignoreTransactionUseCase(id, ignored) }
    }

    fun onDelete(id: Long) {
        viewModelScope.launch { deleteTransactionUseCase(id) }
    }

    /** 바텀시트 '저장' — 수정한 메모·분류를 upsert */
    fun onSaveTransaction(id: Long, memo: String, category: TransactionCategory?) {
        viewModelScope.launch { updateTransactionUseCase(id, memo, category) }
    }

    /** FAB — 수동 내역 추가 */
    fun onAddTransaction(
        type: TransactionType,
        amount: Long,
        sourceName: String,
        category: TransactionCategory?,
        memo: String,
    ) {
        viewModelScope.launch { addManualTransactionUseCase(type, amount, sourceName, category, memo) }
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
