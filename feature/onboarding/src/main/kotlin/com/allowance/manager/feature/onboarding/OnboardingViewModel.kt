package com.allowance.manager.feature.onboarding

import androidx.lifecycle.viewModelScope
import com.allowance.manager.core.domain.model.Account
import com.allowance.manager.core.domain.model.AlertFrequency
import com.allowance.manager.core.domain.model.BudgetAlertSetting
import com.allowance.manager.core.domain.model.DailyReminderSetting
import com.allowance.manager.core.domain.model.UserType
import com.allowance.manager.core.domain.usecase.account.AddAccountUseCase
import com.allowance.manager.core.domain.usecase.alert.SetBudgetAlertSettingUseCase
import com.allowance.manager.core.domain.usecase.alert.SetDailyReminderSettingUseCase
import com.allowance.manager.core.domain.usecase.budget.SetUserTypeUseCase
import com.allowance.manager.core.domain.usecase.budget.SetMonthlyBudgetUseCase
import com.allowance.manager.core.domain.model.PaydayRule
import com.allowance.manager.core.domain.usecase.budget.SetPaydayRuleUseCase
import com.allowance.manager.core.domain.usecase.onboarding.SetOnboardingDoneUseCase
import com.allowance.manager.core.analytics.AmAnalytics
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
    val paydayInput: String = "",  // 월급일 직접입력 텍스트(칩과 분리, 자유 편집용)
    // 알림 설정(마지막 스텝) — 기본 켜짐/중간/오후 9시
    val budgetAlertEnabled: Boolean = true,
    val budgetAlertFrequency: AlertFrequency = AlertFrequency.Default,
    val dailyReminderEnabled: Boolean = true,
    val reminderHour: Int = 21,
    val reminderMinute: Int = 0,
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
    private val setPaydayRuleUseCase: SetPaydayRuleUseCase,
    private val addAccountUseCase: AddAccountUseCase,
    private val setUserTypeUseCase: SetUserTypeUseCase,
    private val setOnboardingDoneUseCase: SetOnboardingDoneUseCase,
    private val setBudgetAlertSettingUseCase: SetBudgetAlertSettingUseCase,
    private val setDailyReminderSettingUseCase: SetDailyReminderSettingUseCase,
    private val analytics: AnalyticsHelper,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onBankNameChange(value: String) = _uiState.update { it.copy(bankName = value) }
    fun onAccountPatternChange(value: String) = _uiState.update { it.copy(accountPattern = value) }
    fun onBudgetChange(value: String) = _uiState.update { it.copy(budgetInput = value) }

    /** 직접 입력: 텍스트를 그대로 보유(자유 편집 가능) + payday 재계산 → 이전 칩 선택은 자동 해제. */
    fun onPaydayInputChange(text: String) {
        val digits = text.filter { it.isDigit() }.take(2)
        val day = digits.toIntOrNull()?.takeIf { it in 1..31 }
        _uiState.update { it.copy(paydayInput = digits, payday = day) }
    }

    /** 칩 선택: payday와 입력 텍스트를 함께 세팅(말일=0은 텍스트 비움). */
    fun onPaydaySelect(day: Int) = _uiState.update {
        it.copy(payday = day, paydayInput = if (day in 1..31) day.toString() else "")
    }
    fun onUserTypeChange(type: UserType) = _uiState.update { it.copy(userType = type) }

    // ── 알림 설정 스텝 핸들러 ──
    fun onBudgetAlertToggle(enabled: Boolean) = _uiState.update { it.copy(budgetAlertEnabled = enabled) }
    fun onBudgetAlertFrequencyChange(freq: AlertFrequency) =
        _uiState.update { it.copy(budgetAlertFrequency = freq) }
    fun onDailyReminderToggle(enabled: Boolean) = _uiState.update { it.copy(dailyReminderEnabled = enabled) }
    fun onReminderTimeChange(hour: Int, minute: Int) =
        _uiState.update { it.copy(reminderHour = hour, reminderMinute = minute) }

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
            AmAnalytics.Event.ONBOARDING_COMPLETE,
            mapOf(
                AmAnalytics.Param.PAYDAY to payday,
                AmAnalytics.Param.BUDGET to state.budget,
                AmAnalytics.Param.BANK to state.bankName,
            ),
        )
        analytics.setUserProperty(AmAnalytics.UserProp.USER_TYPE, state.userType.name.lowercase())
        analytics.setUserProperty(AmAnalytics.UserProp.MAIN_BANK, state.bankName)
        analytics.setUserProperty(AmAnalytics.UserProp.HAS_MAIN_ACCOUNT, "true")
        analytics.setUserProperty(AmAnalytics.UserProp.BUDGET_RANGE, budgetRange(state.budget))
        analytics.setUserProperty(AmAnalytics.UserProp.PAYDAY, payday.toString())
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
            // 규칙일 + "언제부터"를 함께 기록 — 이력의 첫 줄이 여기서 생긴다.
            // 첫 거래는 온보딩 이후에 쌓이므로 시작 경계는 하한(FLOOR)이면 충분하다.
            setPaydayRuleUseCase(PaydayRule.FLOOR, payday)
            setBudgetAlertSettingUseCase(
                BudgetAlertSetting(
                    enabled = state.budgetAlertEnabled,
                    frequency = state.budgetAlertFrequency,
                )
            )
            setDailyReminderSettingUseCase(
                DailyReminderSetting(
                    enabled = state.dailyReminderEnabled,
                    hour = state.reminderHour,
                    minute = state.reminderMinute,
                )
            )
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
