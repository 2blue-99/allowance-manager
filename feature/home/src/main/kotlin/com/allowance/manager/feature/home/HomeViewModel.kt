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
import com.allowance.manager.core.domain.usecase.transaction.PromoteToMainUseCase
import com.allowance.manager.core.domain.usecase.transaction.UpdateTransactionMemoUseCase
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
    val ratio: Float = 0f,
    val isOver: Boolean = false,
    val cycleLabel: String = "",
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
    private val updateTransactionMemoUseCase: UpdateTransactionMemoUseCase,
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
                val effectiveMainOnly = mainOnly && hasAccounts
                val visible = if (effectiveMainOnly) transactions.filter { it.isMain } else transactions
                HomeUiState(
                    budget = status.budget,
                    spent = status.spent,
                    remaining = status.remaining,
                    ratio = status.ratio,
                    isOver = status.isOver,
                    cycleLabel = cycleLabel(status.cycle.nextPayday),
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

    fun onUpdateMemo(id: Long, memo: String) {
        viewModelScope.launch { updateTransactionMemoUseCase(id, memo) }
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

    private fun cycleLabel(nextPayday: LocalDate): String {
        val days = ChronoUnit.DAYS.between(LocalDate.now(), nextPayday)
        return if (days <= 0) "월급일" else "다음 월급일 D-$days"
    }
}
