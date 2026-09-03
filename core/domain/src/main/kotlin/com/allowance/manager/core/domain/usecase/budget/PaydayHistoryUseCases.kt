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
 * 설정에서 월급일 규칙을 바꾼다 — **현재 사이클의 다음 경계부터** 새 규칙이 적용된다.
 *
 * 현재 사이클 이력 줄의 규칙일을 갱신하는 방식이다. 시작일(이미 받은 날)은 사실이므로 그대로 두고,
 * 그 이후 경계만 새 규칙으로 다시 계산한다.
 *
 * 미래에 새 줄을 덧붙이면 안 된다 — 기존 규칙의 예정 지급일이 그보다 먼저 와서
 * 사이에 짧은 사이클이 끼어든다. (15일 규칙 중 25일로 바꾸면 9/15~9/23짜리 사이클이 생김)
 *
 * 사용자가 "실제로는 다른 날 받았다"고 경계를 직접 지정하는 건 [SetPaydayRuleUseCase]가 맡는다.
 */
class ChangePaydayUseCase @Inject constructor(
    private val paydayRepository: PaydayRepository,
    private val dataStoreRepository: DataStoreRepository,
    private val getCycleUseCase: GetCycleUseCase,
) {
    suspend operator fun invoke(payday: Int, today: LocalDate = LocalDate.now()) {
        // 이전에 예약해둔 변경은 아직 일어나지 않은 일 → 걷어내고 새 규칙으로 대체
        paydayRepository.removeRulesAfter(today)
        // 현재 사이클 줄의 규칙일을 갱신 (같은 effectiveDate면 upsert가 덮어쓴다)
        val cycle = getCycleUseCase(today)
        paydayRepository.setRule(cycle.start, payday)
        dataStoreRepository.setPayday(payday)
    }
}

/**
 * 월급일 변경 저장 — [effectiveDate]부터 새 사이클이 시작하고, 이후 경계는 [payday]로 계산한다.
 *
 * 규칙일만 바꾸든(칩), 받은 날만 옮기든(캘린더), 둘 다 하든 이 한 곳으로 들어온다.
 * 규칙일은 DataStore에도 함께 반영해 온보딩·설정 표시와 어긋나지 않게 한다.
 */
class SetPaydayRuleUseCase @Inject constructor(
    private val paydayRepository: PaydayRepository,
    private val dataStoreRepository: DataStoreRepository,
) {
    suspend operator fun invoke(effectiveDate: LocalDate, payday: Int) {
        paydayRepository.setRule(effectiveDate, payday)
        dataStoreRepository.setPayday(payday)
    }
}
