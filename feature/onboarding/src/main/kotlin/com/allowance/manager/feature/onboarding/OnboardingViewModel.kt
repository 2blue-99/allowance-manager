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
import com.allowance.manager.core.domain.usecase.budget.InitPaydayRuleUseCase
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

    /** 월급일·용돈·계좌(은행명+계좌패턴)가 모두 채워져야 정보 스텝을 넘어갈 수 있다 */
    val canFinish: Boolean
        get() = payday != null && budget > 0 &&
            bankName.isNotBlank() && accountPattern.isNotBlank()
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val setMonthlyBudgetUseCase: SetMonthlyBudgetUseCase,
    private val initPaydayRuleUseCase: InitPaydayRuleUseCase,
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

    /** 정보 저장 중복 방지 — 정보 스텝 '다음'은 한 번만 저장한다 */
    private var infoSaved = false

    /**
     * 정보 스텝(월급일·용돈·계좌) 확정 — **알림 권한을 켜기 전에** 저장한다.
     *
     * 권한이 켜지는 순간부터 리스너가 거래를 기록하기 시작하므로, 그 전에 첫 사이클 행이
     * 있어야 모든 거래가 사이클 안에 떨어진다. (사이클 행 없이 거래 먼저 = 유령 거래)
     */
    fun confirmInfo() {
        val state = _uiState.value
        val payday = state.payday ?: return
        if (!state.canFinish || infoSaved) return
        infoSaved = true
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
            // ⚠️ 순서 주의: 월급일이 먼저다 — 첫 사이클 행을 심어야 예산을 그 행에 쓸 수 있다.
            initPaydayRuleUseCase(payday)
            setMonthlyBudgetUseCase(state.budget)
        }
    }

    /** 마지막 스텝(서브 알림 설정) 완료 — 알림 설정 저장 + 온보딩 종료 표식 + 계측 */
    fun finish() {
        val state = _uiState.value
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
            // 혹시 정보 스텝 저장이 안 지나갔으면(비정상 경로) 여기서 한 번 더 보장
            if (!infoSaved) confirmInfo()
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
