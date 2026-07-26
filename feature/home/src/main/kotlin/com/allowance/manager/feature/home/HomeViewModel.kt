package com.allowance.manager.feature.home

import androidx.lifecycle.viewModelScope
import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.usecase.account.ObserveAccountsUseCase
import com.allowance.manager.core.domain.usecase.budget.ObserveBudgetStatusUseCase
import com.allowance.manager.core.domain.usecase.setting.GetShowMainOnlyUseCase
import com.allowance.manager.core.domain.usecase.setting.SetShowMainOnlyUseCase
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
    val remaining: Long = 0L,
    val ratio: Float = 0f,              // 남은 비율
    val spentRatio: Float = 0f,         // 소진율 (지출/예산) — 메인 바 채움
    val isOver: Boolean = false,
    val dailyBudget: Long = 0L,         // 하루 사용 권장액 (남은예산 ÷ 남은일수)
    val dailyAverage: Long = 0L,        // 하루 평균 지출 (지출 ÷ 경과일수)
    val overPace: Boolean = false,      // 하루 평균 > 권장 → 과속
    val daysUntilPayday: Int = 0,       // 다음 월급일까지 D-day
    val transactions: List<Transaction> = emptyList(),
    val showMainOnly: Boolean = true,
    val canFilter: Boolean = false,     // 등록 계좌가 있어야 메인/전체 필터 의미 있음
    val isLoading: Boolean = true,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeBudgetStatusUseCase: ObserveBudgetStatusUseCase,
    observeCurrentTransactionsUseCase: ObserveCurrentTransactionsUseCase,
    observeAccountsUseCase: ObserveAccountsUseCase,
    getShowMainOnlyUseCase: GetShowMainOnlyUseCase,
    private val setShowMainOnlyUseCase: SetShowMainOnlyUseCase,
    private val ignoreTransactionUseCase: IgnoreTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val promoteToMainUseCase: PromoteToMainUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val addManualTransactionUseCase: AddManualTransactionUseCase,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                observeBudgetStatusUseCase(),
                observeCurrentTransactionsUseCase(),
                observeAccountsUseCase(),
                getShowMainOnlyUseCase(),
            ) { status, transactions, accounts, mainOnly ->
                // 등록 계좌가 없으면 감지된 전체를 보여줌(첫 사용 시 감지 확인 + 메인 등록 유도)
                val hasAccounts = accounts.isNotEmpty()
                // 토글은 항상 노출·동작 (메인 계좌 유무와 무관)
                val effectiveMainOnly = mainOnly
                val visible = if (effectiveMainOnly) transactions.filter { it.isMain || it.isManual } else transactions

                // 하루 권장액·평균·D-day: 사이클 경계와 오늘 기준으로 계산 (0일 나눗셈 방어)
                val today = LocalDate.now()
                val daysUntil = ChronoUnit.DAYS.between(today, status.cycle.nextPayday).toInt()
                val daysLeft = daysUntil.coerceAtLeast(1)
                val elapsed = (ChronoUnit.DAYS.between(status.cycle.start, today).toInt() + 1).coerceAtLeast(1)
                val dailyBudget = if (status.remaining > 0) status.remaining / daysLeft else 0L
                val dailyAverage = status.spent / elapsed
                val spentRatio = if (status.budget > 0) (status.spent.toFloat() / status.budget).coerceIn(0f, 1f) else 0f

                HomeUiState(
                    budget = status.budget,
                    spent = status.spent,
                    remaining = status.remaining,
                    ratio = status.ratio,
                    spentRatio = spentRatio,
                    isOver = status.isOver,
                    dailyBudget = dailyBudget,
                    dailyAverage = dailyAverage,
                    overPace = status.budget > 0 && dailyAverage > dailyBudget,
                    daysUntilPayday = daysUntil,
                    transactions = visible,
                    showMainOnly = effectiveMainOnly,
                    canFilter = hasAccounts,
                    isLoading = false,
                )
            }.collect { _uiState.value = it }
        }
    }

    fun onToggleMainOnly() {
        viewModelScope.launch { setShowMainOnlyUseCase(!uiState.value.showMainOnly) }
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
        val pattern = transaction.extractedAccount ?: return
        viewModelScope.launch {
            promoteToMainUseCase(
                packageName = transaction.packageName,
                bankName = transaction.sourceName,
                accountPattern = pattern,
            )
        }
    }
}
