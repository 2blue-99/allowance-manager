package com.allowance.manager.feature.onboarding

import androidx.lifecycle.viewModelScope
import com.allowance.manager.core.domain.model.Account
import com.allowance.manager.core.domain.model.UserType
import com.allowance.manager.core.domain.usecase.account.AddAccountUseCase
import com.allowance.manager.core.domain.usecase.budget.SetUserTypeUseCase
import com.allowance.manager.core.domain.usecase.budget.SetMonthlyBudgetUseCase
import com.allowance.manager.core.domain.usecase.budget.SetPaydayUseCase
import com.allowance.manager.core.domain.usecase.onboarding.SetOnboardingDoneUseCase
import com.allowance.manager.core.analytics.AnalyticsHelper
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
    val userType: UserType = UserType.Default,   // 사용자 유형(용돈/생활비/예산)
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
    private val setUserTypeUseCase: SetUserTypeUseCase,
    private val setOnboardingDoneUseCase: SetOnboardingDoneUseCase,
    private val analytics: AnalyticsHelper,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onBankNameChange(value: String) = _uiState.update { it.copy(bankName = value) }
    fun onAccountPatternChange(value: String) = _uiState.update { it.copy(accountPattern = value) }
    fun onBudgetChange(value: String) = _uiState.update { it.copy(budgetInput = value) }
    fun onPaydayChange(day: Int) = _uiState.update { it.copy(payday = day) }
    fun onUserTypeChange(type: UserType) = _uiState.update { it.copy(userType = type) }

    /** 유형 확정(첫 단계 '다음') → DataStore 저장 후 이후 화면 문구가 이 값을 따름 */
    fun confirmUserType() {
        viewModelScope.launch { setUserTypeUseCase(_uiState.value.userType) }
    }

    fun finish() {
        val state = _uiState.value
        if (!state.canFinish) return
        val payday = state.payday ?: return
        // 온보딩 세이브 시 값 기록 + 이후 전체 이벤트 세그먼트용 User Property 세팅
        analytics.logEvent(
            "onboarding_complete",
            mapOf("payday" to payday, "budget" to state.budget, "bank" to state.bankName),
        )
        analytics.setUserProperty("user_type", state.userType.name.lowercase())
        analytics.setUserProperty("main_bank", state.bankName)
        analytics.setUserProperty("has_main_account", "true")
        analytics.setUserProperty("budget_range", budgetRange(state.budget))
        analytics.setUserProperty("payday", payday.toString())
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

/** 생활비 원본 금액 → 세그먼트용 구간(민감도 완화). */
private fun budgetRange(won: Long): String = when {
    won < 300_000 -> "<30만"
    won < 500_000 -> "30~50만"
    won < 1_000_000 -> "50~100만"
    else -> "100만+"
}
