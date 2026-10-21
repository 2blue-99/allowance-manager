package com.allowance.manager.core.data.util

import com.allowance.manager.core.domain.util.TodayTicker
import com.allowance.manager.core.domain.util.todayTicks
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

/** 실제 시계 기반 — 오늘을 방출하고 자정마다 새 날짜를 방출 */
class SystemTodayTicker @Inject constructor() : TodayTicker {
    override fun ticks(): Flow<LocalDate> = todayTicks()
}
