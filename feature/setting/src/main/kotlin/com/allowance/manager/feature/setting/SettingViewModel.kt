package com.allowance.manager.feature.setting

import androidx.lifecycle.viewModelScope
import com.allowance.manager.core.domain.model.AlertFrequency
import com.allowance.manager.core.domain.model.BudgetAlertSetting
import com.allowance.manager.core.domain.model.DailyReminderSetting
import com.allowance.manager.core.domain.model.PaydayAlertSetting
import com.allowance.manager.core.domain.model.UserType
import com.allowance.manager.core.domain.usecase.alert.GetBudgetAlertSettingUseCase
import com.allowance.manager.core.domain.usecase.alert.GetDailyReminderSettingUseCase
import com.allowance.manager.core.domain.usecase.alert.GetPaydayAlertSettingUseCase
import com.allowance.manager.core.domain.usecase.alert.SetPaydayAlertSettingUseCase
import com.allowance.manager.core.domain.usecase.alert.SetBudgetAlertSettingUseCase
import com.allowance.manager.core.domain.usecase.alert.SetDailyReminderSettingUseCase
import com.allowance.manager.core.domain.usecase.budget.GetMonthlyBudgetUseCase
import com.allowance.manager.core.domain.usecase.budget.GetPaydayUseCase
import com.allowance.manager.core.domain.usecase.budget.GetUserTypeUseCase
import com.allowance.manager.core.domain.usecase.budget.SetMonthlyBudgetUseCase
import com.allowance.manager.core.domain.model.BudgetCycle
import com.allowance.manager.core.domain.usecase.budget.ChangePaydayUseCase
import com.allowance.manager.core.domain.usecase.budget.MoveCycleStartUseCase
import com.allowance.manager.core.domain.usecase.budget.ObserveCycleUseCase
import com.allowance.manager.core.domain.usecase.budget.PaydayChangePreview
import com.allowance.manager.core.domain.usecase.budget.PreviewCycleStartUseCase
import com.allowance.manager.core.domain.usecase.budget.PreviewPaydayChangeUseCase
import com.allowance.manager.core.domain.usecase.budget.SetCycleEndUseCase
import java.time.LocalDate
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
    val budgetAlert: BudgetAlertSetting = BudgetAlertSetting(),
    val dailyReminder: DailyReminderSetting = DailyReminderSetting(),
    val paydayAlert: PaydayAlertSetting = PaydayAlertSetting(),
    val cycle: BudgetCycle? = null,   // 월급일·이번 회차 다이얼로그가 쓰는 현재 사이클. 첫 프레임에선 아직 null
)

@HiltViewModel
class SettingViewModel @Inject constructor(
    getMonthlyBudgetUseCase: GetMonthlyBudgetUseCase,
    getPaydayUseCase: GetPaydayUseCase,
    getStatusBarEnabledUseCase: GetStatusBarEnabledUseCase,
    getUserTypeUseCase: GetUserTypeUseCase,
    getBudgetAlertSettingUseCase: GetBudgetAlertSettingUseCase,
    getDailyReminderSettingUseCase: GetDailyReminderSettingUseCase,
    getPaydayAlertSettingUseCase: GetPaydayAlertSettingUseCase,
    private val setMonthlyBudgetUseCase: SetMonthlyBudgetUseCase,
    private val changePaydayUseCase: ChangePaydayUseCase,
    private val setCycleEndUseCase: SetCycleEndUseCase,
    private val moveCycleStartUseCase: MoveCycleStartUseCase,
    private val previewPaydayChangeUseCase: PreviewPaydayChangeUseCase,
    private val previewCycleStartUseCase: PreviewCycleStartUseCase,
    observeCycleUseCase: ObserveCycleUseCase,
    private val setStatusBarEnabledUseCase: SetStatusBarEnabledUseCase,
    private val setUserTypeUseCase: SetUserTypeUseCase,
    private val setBudgetAlertSettingUseCase: SetBudgetAlertSettingUseCase,
    private val setDailyReminderSettingUseCase: SetDailyReminderSettingUseCase,
    private val setPaydayAlertSettingUseCase: SetPaydayAlertSettingUseCase,
    private val analytics: AnalyticsHelper,
) : BaseViewModel() {

    val uiState: StateFlow<SettingUiState> = combine(
        // 기존 4개 그룹을 먼저 묶고(combine 최대 인자 회피), 알림 설정 2개와 다시 결합
        combine(
            getMonthlyBudgetUseCase(),
            getPaydayUseCase(),
            getStatusBarEnabledUseCase(),
            getUserTypeUseCase(),
        ) { budget, payday, statusBar, userType ->
            SettingUiState(budget = budget, payday = payday, statusBarEnabled = statusBar, userType = userType)
        },
        getBudgetAlertSettingUseCase(),
        getDailyReminderSettingUseCase(),
        getPaydayAlertSettingUseCase(),
        observeCycleUseCase(),
    ) { base, budgetAlert, dailyReminder, paydayAlert, cycle ->
        base.copy(
            budgetAlert = budgetAlert,
            dailyReminder = dailyReminder,
            paydayAlert = paydayAlert,
            cycle = cycle,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingUiState())

    fun setBudget(amount: Long) {
        analytics.setUserProperty(AmAnalytics.UserProp.BUDGET_RANGE, budgetRange(amount))
        viewModelScope.launch { setMonthlyBudgetUseCase(amount) }
    }

    /**
     * 월급일 변경 — 두 다이얼로그가 같은 진입점을 쓴다.
     * - 규칙일 다이얼로그: (현재 회차 시작, 새 규칙일) → 시작은 유지, 끝만 재계산
     * - 이번 회차 다이얼로그: (실제 받은 날, 현재 규칙일) → 규칙은 유지, 시작만 이동
     */
    fun setPaydayRule(effectiveDate: LocalDate, day: Int) {
        analytics.setUserProperty(AmAnalytics.UserProp.PAYDAY, day.toString())
        viewModelScope.launch { changePaydayUseCase(effectiveDate, day) }
    }

    /** "이번 회차만 이 날 받아요" — 현재 회차 끝을 고정. 규칙일은 그대로. */
    fun setCycleEnd(end: LocalDate) {
        viewModelScope.launch { setCycleEndUseCase(end) }
    }

    /** "이번 월급일은 사실 이 날이었다" — 현재 회차 시작만 이동. 끝·예산·규칙은 그대로. */
    fun moveCycleStart(boundary: LocalDate) {
        viewModelScope.launch { moveCycleStartUseCase(boundary) }
    }

    /** 시작 이동 예고 — 저장 로직과 같은 계산 (끝 유지) */
    suspend fun previewCycleStart(boundary: LocalDate): PaydayChangePreview? =
        runCatching { previewCycleStartUseCase(boundary) }.getOrNull()

    /** 저장 전 결과 예고 — 다이얼로그가 입력이 바뀔 때마다 호출. 저장 로직과 같은 계산을 쓴다. */
    suspend fun previewPaydayChange(boundary: LocalDate, day: Int): PaydayChangePreview? =
        runCatching { previewPaydayChangeUseCase(boundary, day) }.getOrNull()

    fun setStatusBarEnabled(enabled: Boolean) {
        analytics.logEvent(AmAnalytics.Event.SETTING_STATUSBAR_TOGGLE, mapOf(AmAnalytics.Param.ENABLED to enabled))
        analytics.setUserProperty(AmAnalytics.UserProp.STATUSBAR_ENABLED, enabled.toString())
        viewModelScope.launch { setStatusBarEnabledUseCase(enabled) }
    }

    fun setUserType(type: UserType) {
        analytics.setUserProperty(AmAnalytics.UserProp.USER_TYPE, type.name.lowercase())
        viewModelScope.launch { setUserTypeUseCase(type) }
    }

    // ── 예산 소진 알림 ──
    fun setBudgetAlertEnabled(enabled: Boolean) {
        viewModelScope.launch {
            setBudgetAlertSettingUseCase(uiState.value.budgetAlert.copy(enabled = enabled))
        }
    }

    // 빈도 선택은 알림을 켠 것으로 간주(enabled = true)
    fun setBudgetAlertFrequency(freq: AlertFrequency) {
        viewModelScope.launch {
            setBudgetAlertSettingUseCase(BudgetAlertSetting(enabled = true, frequency = freq))
        }
    }

    // ── 가계부 관리 알림 ──
    fun setDailyReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            setDailyReminderSettingUseCase(uiState.value.dailyReminder.copy(enabled = enabled))
        }
    }

    fun setDailyReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            setDailyReminderSettingUseCase(uiState.value.dailyReminder.copy(hour = hour, minute = minute))
        }
    }

    // ── 월급일 알림 ──
    fun setPaydayAlertEnabled(enabled: Boolean) {
        viewModelScope.launch {
            setPaydayAlertSettingUseCase(uiState.value.paydayAlert.copy(enabled = enabled))
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
