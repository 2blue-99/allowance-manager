package com.allowance.manager.core.domain.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import java.time.ZoneId

/**
 * 오늘 날짜를 방출하고, 다음 자정(+5초)마다 새 날짜를 방출한다.
 *
 * 사이클·오늘 지표 UseCase는 '오늘'을 구독 시점에 고정하므로, 오래 사는 구독(상태바 서비스·
 * 앱 전역 관찰)은 `todayTicks().flatMapLatest { useCase(it) }`로 날짜가 바뀔 때 다시 구독해야
 * 수급일이 지나도 지난 사이클에 머물지 않는다.
 * (기기 절전 중엔 자정 깨움이 늦어질 수 있다 — 깨어나는 즉시 새 날짜가 방출된다)
 */
fun todayTicks(zone: ZoneId = ZoneId.systemDefault()): Flow<LocalDate> = flow {
    while (true) {
        val today = LocalDate.now(zone)
        emit(today)
        val nextMidnight = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        delay((nextMidnight - System.currentTimeMillis()).coerceAtLeast(0L) + MIDNIGHT_SLACK_MS)
    }
}.distinctUntilChanged()

private const val MIDNIGHT_SLACK_MS = 5_000L
