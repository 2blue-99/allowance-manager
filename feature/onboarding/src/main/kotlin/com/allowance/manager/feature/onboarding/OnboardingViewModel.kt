package com.allowance.manager.feature.onboarding

import androidx.lifecycle.viewModelScope
import com.allowance.manager.core.domain.model.Account
import com.allowance.manager.core.domain.usecase.account.AddAccountUseCase
import com.allowance.manager.core.domain.usecase.budget.SetMonthlyBudgetUseCase
import com.allowance.manager.core.domain.usecase.budget.SetPaydayUseCase
import com.allowance.manager.core.domain.usecase.onboarding.SetOnboardingDoneUseCase
import com.allowance.manager.core.ui.BaseViewModel
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
    val payday: Int = 25,          // 0 = 말일
    val isFinished: Boolean = false,
) {
    val budget: Long get() = budgetInput.filter { it.isDigit() }.toLongOrNull() ?: 0L
    val canFinish: Boolean get() = budget > 0
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
        viewModelScope.launch {
            if (state.accountPattern.isNotBlank()) {
                addAccountUseCase(
                    Account(
                        packageName = "",
                        bankName = state.bankName.ifBlank { "내 계좌" },
                        accountPattern = state.accountPattern.trim(),
                        enabled = true,
                    )
                )
            }
            setMonthlyBudgetUseCase(state.budget)
            setPaydayUseCase(state.payday)
            setOnboardingDoneUseCase()
            _uiState.update { it.copy(isFinished = true) }
        }
    }
}
