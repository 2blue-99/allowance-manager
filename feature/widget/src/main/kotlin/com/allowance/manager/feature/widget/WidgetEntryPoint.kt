package com.allowance.manager.feature.widget

import android.content.Context
import android.content.Intent
import com.allowance.manager.core.domain.usecase.budget.GetUserTypeUseCase
import com.allowance.manager.core.domain.usecase.budget.ObserveBudgetStatusUseCase
import com.allowance.manager.core.domain.usecase.budget.ObserveDailyCoachUseCase
import com.allowance.manager.core.domain.usecase.calendar.ObserveMonthlyLedgerUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * 위젯(Glance)에서 UseCase를 얻기 위한 Hilt 엔트리포인트. 4종 위젯이 공유한다.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface WidgetEntryPoint {
    fun observeBudgetStatusUseCase(): ObserveBudgetStatusUseCase
    fun getUserTypeUseCase(): GetUserTypeUseCase
    fun observeDailyCoachUseCase(): ObserveDailyCoachUseCase
    fun observeMonthlyLedgerUseCase(): ObserveMonthlyLedgerUseCase
}

internal fun Context.widgetEntryPoint(): WidgetEntryPoint =
    EntryPointAccessors.fromApplication(this, WidgetEntryPoint::class.java)

/** 위젯 탭 → 앱 실행 인텐트. */
internal fun Context.launchAppIntent(): Intent =
    packageManager.getLaunchIntentForPackage(packageName) ?: Intent()
