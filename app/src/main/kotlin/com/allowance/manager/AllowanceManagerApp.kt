package com.allowance.manager

import android.app.Application
import com.allowance.manager.core.domain.usecase.alert.GetBudgetAlertSettingUseCase
import com.allowance.manager.core.domain.usecase.alert.GetDailyReminderSettingUseCase
import com.allowance.manager.core.domain.usecase.budget.GetUserTypeUseCase
import com.allowance.manager.core.domain.usecase.budget.ObserveBudgetStatusUseCase
import com.allowance.manager.service.BudgetAlertNotifier
import com.allowance.manager.service.DailyReminderScheduler
import com.allowance.manager.feature.widget.refreshBalanceWidget
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class AllowanceManagerApp : Application() {

    @Inject lateinit var observeBudgetStatusUseCase: ObserveBudgetStatusUseCase
    @Inject lateinit var getBudgetAlertSettingUseCase: GetBudgetAlertSettingUseCase
    @Inject lateinit var getUserTypeUseCase: GetUserTypeUseCase
    @Inject lateinit var budgetAlertNotifier: BudgetAlertNotifier
    @Inject lateinit var getDailyReminderSettingUseCase: GetDailyReminderSettingUseCase
    @Inject lateinit var dailyReminderScheduler: DailyReminderScheduler

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        observeAndRefreshWidget()
        observeBudgetAlert()
        observeDailyReminder()
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
     * 이번달 예산 현황 변화를 받아 예산 소진 알림(임계값 관통)을 발송.
     * 위젯 갱신과 같은 흐름(ObserveBudgetStatusUseCase)을 공유해 홈·위젯과 값이 항상 일치한다.
     */
    private fun observeBudgetAlert() {
        appScope.launch {
            combine(
                observeBudgetStatusUseCase(),
                getBudgetAlertSettingUseCase(),
                getUserTypeUseCase(),
            ) { status, setting, userType -> Triple(status, setting, userType) }
                .collect { (status, setting, userType) ->
                    runCatching { budgetAlertNotifier.onBudgetStatus(status, setting, userType) }
                }
        }
    }

    /**
     * 이번달 지출/예산이 바뀔 때마다 위젯을 갱신.
     * 거래 저장·무시·삭제·예산/수급일 변경 등 모든 데이터 변화를 한곳에서 커버.
     */
    private fun observeAndRefreshWidget() {
        appScope.launch {
            observeBudgetStatusUseCase()
                .map { it.spent to it.budget }
                .distinctUntilChanged()
                .collect {
                    runCatching { refreshBalanceWidget(this@AllowanceManagerApp) }
                }
        }
    }
}
