package com.allowance.manager.feature.setting

import androidx.lifecycle.viewModelScope
import com.allowance.manager.core.domain.model.UserType
import com.allowance.manager.core.domain.usecase.budget.GetMonthlyBudgetUseCase
import com.allowance.manager.core.domain.usecase.budget.GetPaydayUseCase
import com.allowance.manager.core.domain.usecase.budget.GetUserTypeUseCase
import com.allowance.manager.core.domain.usecase.budget.SetMonthlyBudgetUseCase
import com.allowance.manager.core.domain.usecase.budget.SetPaydayUseCase
import com.allowance.manager.core.domain.usecase.budget.SetUserTypeUseCase
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
    val userType: UserType = UserType.Default,
)

@HiltViewModel
class SettingViewModel @Inject constructor(
    getMonthlyBudgetUseCase: GetMonthlyBudgetUseCase,
    getPaydayUseCase: GetPaydayUseCase,
    getStatusBarEnabledUseCase: GetStatusBarEnabledUseCase,
    getUserTypeUseCase: GetUserTypeUseCase,
    private val setMonthlyBudgetUseCase: SetMonthlyBudgetUseCase,
    private val setPaydayUseCase: SetPaydayUseCase,
    private val setStatusBarEnabledUseCase: SetStatusBarEnabledUseCase,
    private val setUserTypeUseCase: SetUserTypeUseCase,
) : BaseViewModel() {

    val uiState: StateFlow<SettingUiState> = combine(
        getMonthlyBudgetUseCase(),
        getPaydayUseCase(),
        getStatusBarEnabledUseCase(),
        getUserTypeUseCase(),
    ) { budget, payday, statusBar, userType ->
        SettingUiState(budget = budget, payday = payday, statusBarEnabled = statusBar, userType = userType)
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

    fun setUserType(type: UserType) {
        viewModelScope.launch { setUserTypeUseCase(type) }
    }
}
