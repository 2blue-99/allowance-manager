package com.allowance.manager.feature.setting

import androidx.lifecycle.viewModelScope
import com.allowance.manager.core.domain.usecase.budget.GetMonthlyBudgetUseCase
import com.allowance.manager.core.domain.usecase.budget.GetPaydayUseCase
import com.allowance.manager.core.domain.usecase.budget.SetMonthlyBudgetUseCase
import com.allowance.manager.core.domain.usecase.budget.SetPaydayUseCase
import com.allowance.manager.core.domain.usecase.setting.GetStatusBarEnabledUseCase
import com.allowance.manager.core.domain.usecase.setting.SetStatusBarEnabledUseCase
import com.allowance.manager.core.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingUiState(
    val budget: Long = 0L,
    val payday: Int = 25,       // 0 = 말일
    val statusBarEnabled: Boolean = true,
)

@HiltViewModel
class SettingViewModel @Inject constructor(
    getMonthlyBudgetUseCase: GetMonthlyBudgetUseCase,
    getPaydayUseCase: GetPaydayUseCase,
    getStatusBarEnabledUseCase: GetStatusBarEnabledUseCase,
    private val setMonthlyBudgetUseCase: SetMonthlyBudgetUseCase,
    private val setPaydayUseCase: SetPaydayUseCase,
    private val setStatusBarEnabledUseCase: SetStatusBarEnabledUseCase,
) : BaseViewModel() {

    val uiState: StateFlow<SettingUiState> = combine(
        getMonthlyBudgetUseCase(),
        getPaydayUseCase(),
        getStatusBarEnabledUseCase(),
    ) { budget, payday, statusBar ->
        SettingUiState(budget = budget, payday = payday, statusBarEnabled = statusBar)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingUiState())

    fun setBudget(amount: Long) {
        viewModelScope.launch { setMonthlyBudgetUseCase(amount) }
    }

    fun setPayday(day: Int) {
        viewModelScope.launch { setPaydayUseCase(day) }
    }

    fun setStatusBarEnabled(enabled: Boolean) {
        viewModelScope.launch { setStatusBarEnabledUseCase(enabled) }
    }
}
