package com.allowance.manager

import android.app.Application
import com.allowance.manager.core.domain.usecase.budget.ObserveBudgetStatusUseCase
import com.allowance.manager.feature.widget.refreshBalanceWidget
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class AllowanceManagerApp : Application() {

    @Inject lateinit var observeBudgetStatusUseCase: ObserveBudgetStatusUseCase

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        observeAndRefreshWidget()
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
