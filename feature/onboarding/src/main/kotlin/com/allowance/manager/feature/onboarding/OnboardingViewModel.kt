package com.allowance.manager.feature.onboarding

import androidx.lifecycle.viewModelScope
import com.allowance.manager.core.domain.model.Account
import com.allowance.manager.core.domain.usecase.account.AddAccountUseCase
import com.allowance.manager.core.domain.usecase.budget.SetMonthlyBudgetUseCase
import com.allowance.manager.core.domain.usecase.budget.SetPaydayUseCase
import com.allowance.manager.core.domain.usecase.onboarding.SetOnboardingDoneUseCase
import com.allowance.manager.core.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val bankName: String = "",
    val accountPattern: String = "",
    val budgetInput: String = "",
    val payday: Int? = null,       // null = 미선택, 0 = 말일, 1~31 = 해당 일
    val isFinished: Boolean = false,
) {
    val budget: Long get() = budgetInput.filter { it.isDigit() }.toLongOrNull() ?: 0L

    /** 월급일·용돈·계좌(은행명+계좌패턴)가 모두 채워져야 시작 가능 */
    val canFinish: Boolean
        get() = payday != null && budget > 0 &&
            bankName.isNotBlank() && accountPattern.isNotBlank()
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val setMonthlyBudgetUseCase: SetMonthlyBudgetUseCase,
    private val setPaydayUseCase: SetPaydayUseCase,
    private val addAccountUseCase: AddAccountUseCase,
    private val setOnboardingDoneUseCase: SetOnboardingDoneUseCase,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onBankNameChange(value: String) = _uiState.update { it.copy(bankName = value) }
    fun onAccountPatternChange(value: String) = _uiState.update { it.copy(accountPattern = value) }
    fun onBudgetChange(value: String) = _uiState.update { it.copy(budgetInput = value) }
    fun onPaydayChange(day: Int) = _uiState.update { it.copy(payday = day) }

    fun finish() {
        val state = _uiState.value
        if (!state.canFinish) return
        val payday = state.payday ?: return
        viewModelScope.launch {
            if (state.accountPattern.isNotBlank()) {
                addAccountUseCase(
                    Account(
                        packageName = "",
                        bankName = state.bankName.ifBlank { "내 계좌" },
                        // 계좌번호 정규화는 AddAccountUseCase에서 일괄 처리
                        accountPattern = state.accountPattern,
                        enabled = true,
                    )
                )
            }
            setMonthlyBudgetUseCase(state.budget)
            setPaydayUseCase(payday)
            setOnboardingDoneUseCase()
            _uiState.update { it.copy(isFinished = true) }
        }
    }
}
