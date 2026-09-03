package com.allowance.manager.core.domain.usecase.budget

import com.allowance.manager.core.domain.model.BudgetCycle
import com.allowance.manager.core.domain.model.PaydayRule
import com.allowance.manager.core.domain.repository.DataStoreRepository
import com.allowance.manager.core.domain.repository.PaydayRepository
import com.allowance.manager.core.domain.repository.RemoteConfigRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/** [date] 시점에 유효한 월급일 규칙 관찰. 이력이 아직 없으면 null. */
class ObservePaydayRuleUseCase @Inject constructor(
    private val paydayRepository: PaydayRepository,
) {
    operator fun invoke(date: LocalDate = LocalDate.now()): Flow<PaydayRule?> =
        paydayRepository.observeRuleAt(date)
}

/** 전체 월급일 이력(오래된 → 최신). 사이클 목록·피커 계산용. */
class ObservePaydayHistoryUseCase @Inject constructor(
    private val paydayRepository: PaydayRepository,
) {
    operator fun invoke(): Flow<List<PaydayRule>> = paydayRepository.observeHistory()
}

/**
 * 온보딩·초기 설정에서 첫 규칙을 심는다.
 *
 * 경계는 **오늘이 속한 사이클의 시작일**로 잡는다. 첫 거래는 온보딩 이후에 쌓이므로
 * 이보다 과거를 조회할 일이 없고, 이력이 짧게 유지돼 사이클 계산도 가볍다.
 */
class InitPaydayRuleUseCase @Inject constructor(
    private val paydayRepository: PaydayRepository,
    private val dataStoreRepository: DataStoreRepository,
    private val remoteConfigRepository: RemoteConfigRepository,
) {
    suspend operator fun invoke(payday: Int, today: LocalDate = LocalDate.now()) {
        val cycle = BudgetCycle.of(payday, today, remoteConfigRepository.getHolidays())
        paydayRepository.setRule(cycle.start, payday)
        dataStoreRepository.setPayday(payday)
    }
}

/**
 * 월급일 변경 적용 — 시트에서 **규칙일**과 **이번에 받은 날(경계)** 을 함께 받는다.
 *
 * 이력을 이렇게 정리한다.
 * 1. 아직 오지 않은 예정 이력은 일어나지 않은 일이므로 걷어낸다
 *    (남기면 9/15·9/18처럼 짧은 사이클이 줄줄이 생긴다)
 * 2. 경계를 옮겼으면 현재 사이클의 기존 줄을 지운다
 *    (안 지우면 8/22로 당길 때 8.22~8.24 같은 조각 사이클이 끼어든다)
 * 3. 새 경계에 규칙일을 기록
 *
 * 경계를 그대로 두면 규칙일만 갱신되어, 시작일(이미 받은 날)은 유지되고 끝만 다시 계산된다.
 */
class ChangePaydayUseCase @Inject constructor(
    private val paydayRepository: PaydayRepository,
    private val dataStoreRepository: DataStoreRepository,
    private val getCycleUseCase: GetCycleUseCase,
) {
    suspend operator fun invoke(boundary: LocalDate, payday: Int, today: LocalDate = LocalDate.now()) {
        val current = getCycleUseCase(today)
        paydayRepository.removeRulesAfter(today)
        if (current.start != boundary) paydayRepository.removeRule(current.start)
        paydayRepository.setRule(boundary, payday)
        dataStoreRepository.setPayday(payday)
    }
}
