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
import com.allowance.manager.core.analytics.AmAnalytics
import com.allowance.manager.core.analytics.AnalyticsHelper
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
    private val analytics: AnalyticsHelper,
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
        analytics.setUserProperty(AmAnalytics.UserProp.BUDGET_RANGE, budgetRange(amount))
        viewModelScope.launch { setMonthlyBudgetUseCase(amount) }
    }

    fun setPayday(day: Int) {
        analytics.setUserProperty(AmAnalytics.UserProp.PAYDAY, day.toString())
        viewModelScope.launch { setPaydayUseCase(day) }
    }

    fun setStatusBarEnabled(enabled: Boolean) {
        analytics.logEvent(AmAnalytics.Event.SETTING_STATUSBAR_TOGGLE, mapOf(AmAnalytics.Param.ENABLED to enabled))
        analytics.setUserProperty(AmAnalytics.UserProp.STATUSBAR_ENABLED, enabled.toString())
        viewModelScope.launch { setStatusBarEnabledUseCase(enabled) }
    }

    fun setUserType(type: UserType) {
        analytics.setUserProperty(AmAnalytics.UserProp.USER_TYPE, type.name.lowercase())
        viewModelScope.launch { setUserTypeUseCase(type) }
    }
}

/** 생활비 원본 금액 → 세그먼트용 구간(민감도 완화). */
private fun budgetRange(won: Long): String = when {
    won < 300_000 -> "<30만"
    won < 500_000 -> "30~50만"
    won < 1_000_000 -> "50~100만"
    else -> "100만+"
}
