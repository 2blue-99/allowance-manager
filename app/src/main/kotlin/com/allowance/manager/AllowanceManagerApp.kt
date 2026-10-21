package com.allowance.manager

import android.app.Application
import com.allowance.manager.core.domain.usecase.alert.GetBudgetAlertSettingUseCase
import com.allowance.manager.core.domain.usecase.alert.GetDailyReminderSettingUseCase
import com.allowance.manager.core.domain.usecase.alert.GetPaydayAlertSettingUseCase
import com.allowance.manager.core.domain.usecase.budget.GetUserTypeUseCase
import com.allowance.manager.core.domain.usecase.budget.ObserveBudgetStatusUseCase
import com.allowance.manager.core.domain.usecase.budget.ObserveDailyCoachUseCase
import com.allowance.manager.core.domain.usecase.calendar.ObserveMonthlyLedgerUseCase
import com.allowance.manager.service.BudgetAlertNotifier
import com.allowance.manager.service.DailyReminderScheduler
import com.allowance.manager.service.PaydayAlarmScheduler
import com.allowance.manager.core.domain.util.todayTicks
import com.allowance.manager.feature.widget.refreshBalanceWidget
import com.allowance.manager.feature.widget.scheduleWidgetDayRefresh
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class AllowanceManagerApp : Application() {

    @Inject lateinit var observeBudgetStatusUseCase: ObserveBudgetStatusUseCase
    @Inject lateinit var observeDailyCoachUseCase: ObserveDailyCoachUseCase
    @Inject lateinit var observeMonthlyLedgerUseCase: ObserveMonthlyLedgerUseCase
    @Inject lateinit var getBudgetAlertSettingUseCase: GetBudgetAlertSettingUseCase
    @Inject lateinit var getUserTypeUseCase: GetUserTypeUseCase
    @Inject lateinit var budgetAlertNotifier: BudgetAlertNotifier
    @Inject lateinit var getDailyReminderSettingUseCase: GetDailyReminderSettingUseCase
    @Inject lateinit var dailyReminderScheduler: DailyReminderScheduler
    @Inject lateinit var getPaydayAlertSettingUseCase: GetPaydayAlertSettingUseCase
    @Inject lateinit var paydayAlarmScheduler: PaydayAlarmScheduler

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // 릴리즈에선 로그 트리를 심지 않는다 — 리스너가 찍는 알림 본문·금액·계좌가 로그캣에 남지 않도록.
        // (트리가 없으면 Timber 호출은 no-op → 앱 동작엔 영향 없음)
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        observeAndRefreshWidget()
        observeBudgetAlert()
        observeDailyReminder()
        observePaydayAlert()
    }

    /**
     * 가계부 관리 알림 설정을 관찰해 매일 알람을 예약/해제.
     * 앱 시작·설정 변경 시 자동 반영(부팅 후 재예약은 BootReceiver 담당).
     */
    private fun observeDailyReminder() {
        appScope.launch {
            getDailyReminderSettingUseCase()
                .distinctUntilChanged()
                .collect { setting ->
                    runCatching {
                        if (setting.enabled) dailyReminderScheduler.schedule(setting.hour, setting.minute)
                        else dailyReminderScheduler.cancel()
                    }
                }
        }
    }

    /**
     * 월급일 알림 설정을 관찰해 매일 알람을 예약/해제.
     * 알람은 매일 오후 9시 1개뿐이고, 그날 알릴지는 리시버가 판정한다 → 월급일·조정값이 바뀌어도 재예약이 없다.
     */
    private fun observePaydayAlert() {
        appScope.launch {
            getPaydayAlertSettingUseCase()
                .distinctUntilChanged()
                .collect {
                    // 월급일 알림 기능 일시 중지 — 설정과 무관하게 항상 해제(기존 사용자의 기존 예약도 취소).
                    // 복구 시: if (it.enabled) paydayAlarmScheduler.schedule() else paydayAlarmScheduler.cancel()
                    runCatching { paydayAlarmScheduler.cancel() }
                }
        }
    }

    /**
     * 이번달 예산 현황 변화를 받아 예산 소진 알림(임계값 관통)을 발송.
     * 위젯 갱신과 같은 흐름(ObserveBudgetStatusUseCase)을 공유해 홈·위젯과 값이 항상 일치한다.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeBudgetAlert() {
        appScope.launch {
            combine(
                // 자정마다 재구독 — 상태바 서비스가 프로세스를 살려두면 지난 사이클에 머무는 것 방지
                todayTicks().flatMapLatest { observeBudgetStatusUseCase(it) },
                getBudgetAlertSettingUseCase(),
                getUserTypeUseCase(),
            ) { status, setting, userType -> Triple(status, setting, userType) }
                .collect { (status, setting, userType) ->
                    runCatching { budgetAlertNotifier.onBudgetStatus(status, setting, userType) }
                }
        }
    }

    /**
     * 위젯 4종이 보여주는 값(예산 현황·오늘 코치·가계부)이 바뀔 때마다 위젯을 갱신.
     * 거래 저장·무시·삭제·등록/해제·예산/수급일 변경 등 모든 데이터 변화를 한곳에서 커버.
     *
     * - 지출·예산만 보면 수입·'가계부만' 내역 변화를 놓친다 → 위젯이 쓰는 UseCase 결과 전체를 본다.
     * - UseCase의 '오늘'은 구독 시점에 고정되므로 자정마다 다시 구독한다 (사이클 넘어감·오늘 지표 반영).
     * - 앱 프로세스가 없을 때의 날짜 변경은 위젯 모듈의 자정 알람이 담당.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeAndRefreshWidget() {
        runCatching { scheduleWidgetDayRefresh(this) }
        appScope.launch {
            todayTicks()
                .flatMapLatest { today ->
                    combine(
                        observeBudgetStatusUseCase(today),
                        observeDailyCoachUseCase(today),
                        observeMonthlyLedgerUseCase(today),
                    ) { status, coach, ledger -> Triple(status, coach, ledger) }
                }
                .distinctUntilChanged()
                .collect {
                    runCatching { refreshBalanceWidget(this@AllowanceManagerApp) }
                }
        }
    }
}
